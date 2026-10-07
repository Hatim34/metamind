#!/usr/bin/env python3
"""
Remise en etat du site en ligne apres les corrections d'octobre 2026.

Etapes, a lancer dans l'ordre (chacune peut etre relancee sans risque) :
  fichiers     rattache les PDF de data/pdf aux documents dont le fichier a ete perdu
  nettoyage    supprime les faux documents et les tests, rattache les comptes de demonstration
               a de vraies institutions et desactive les institutions fictives
  extraction   relance l'extraction (prompt v5) des documents a valider jamais arbitres
  evaluation   compare la sortie du modele aux metadonnees reelles (OpenAlex), par champ
  publication  publie les documents extraits en laissant quelques documents par institution
               dans la file pour la demonstration (voir notice_publiee pour la regle suivie)
  noms         remet en « Prenom Nom » les auteurs publies sous la forme « Nom, Prenom »
  figures      remplace la premiere page par la figure retenue de l'article (figures_retenues.txt)
  photos       place en base les photos de campus actuelles, avec leur credit
  traductions  lance la traduction de toutes les notices publiees (en arriere-plan sur le serveur)
  ajout        importe, analyse et publie les documents ajoutes par « collect.py --ajout »,
               au nom d'un bibliothecaire de leur institution (METAMIND_SEED_PASSWORD requis)

Usage :
  export METAMIND_ADMIN_EMAIL=admin@metamind.example
  export METAMIND_ADMIN_PASSWORD=...
  python reprise.py fichiers --api https://metamind-app.duckdns.org/api/v1
"""
import argparse
import csv
import io
import json
import os
import random
import sys
import time
import unicodedata
from pathlib import Path

from load import Api, charger_vocabulaires, norm, set_f1, similarity, validation_body

ROOT = Path(__file__).parent
DATA = ROOT / "data"
CORRESPONDANCE = DATA / "reprise_correspondance.json"
INSTITUTIONS_FICTIVES = {"Institution A", "Institution B"}
INSTITUTION_TESTS = "Metamind"
RATTACHEMENTS = {
    "sarah@institution-a.example": "UCLouvain",
    "jan@institution-b.example": "KU Leuven",
}


def nom_usuel(nom):
    """OpenAlex ecrit parfois « Simon, Edouard » : le catalogue affiche « Edouard Simon »."""
    if nom and nom.count(",") == 1:
        famille, prenom = (partie.strip() for partie in nom.split(","))
        return f"{prenom} {famille}".strip()
    return nom


def nom_de_famille(nom):
    """Dernier mot du nom, sans accents : « J. Hubrechts » et « Jolien Hubrechts » concordent."""
    sans_accents = unicodedata.normalize("NFD", nom_usuel(nom) or "").encode("ascii", "ignore").decode()
    mots = [m for m in sans_accents.split() if len(m.strip(".")) > 1]
    return mots[-1].lower() if mots else ""


def tous_les_documents(api, token, statut=None):
    documents, page = [], 0
    while True:
        params = {"size": 100, "page": page}
        if statut:
            params["statut"] = statut
        reponse = api.call("GET", "/documents", token, params=params)
        documents += reponse["contenu"]
        page += 1
        if page >= reponse["total_pages"]:
            return documents


def manifeste_par_pdf():
    manifeste = json.loads((DATA / "manifest.json").read_text(encoding="utf-8"))
    return {Path(d["pdf"]).name: d for d in manifeste["documents"]}


def correspondance():
    """Identifiant en ligne -> entree du manifeste, etablie par l'etape fichiers."""
    if not CORRESPONDANCE.exists():
        sys.exit("Lance d'abord l'etape fichiers : elle etablit la correspondance documents / PDF.")
    par_pdf = manifeste_par_pdf()
    resultat = {}
    for nom, ids in json.loads(CORRESPONDANCE.read_text(encoding="utf-8")).items():
        for doc_id in ids:
            if nom in par_pdf:
                resultat[doc_id] = par_pdf[nom]
    return resultat


def etape_fichiers(api, token, args):
    deja = json.loads(CORRESPONDANCE.read_text(encoding="utf-8")) if CORRESPONDANCE.exists() else {}
    pdfs = sorted((DATA / "pdf").glob("*.pdf"))
    for i, pdf in enumerate(pdfs, 1):
        if pdf.name in deja:
            continue
        # Contenu lu en memoire : apres un incident passager, l'essai suivant doit renvoyer
        # le fichier entier, pas un flux deja consomme.
        reponse = api.call("POST", "/admin/documents/files", token,
                           files={"fichier": (pdf.name, pdf.read_bytes(), "application/pdf")})
        restaures = reponse.get("documents_restaures", [])
        if restaures:
            deja[pdf.name] = sorted(set(deja.get(pdf.name, [])) | set(restaures))
        print(f"  {i:>3}/{len(pdfs)} {pdf.name} -> {restaures or 'deja en base ou aucun document'}")
        CORRESPONDANCE.write_text(json.dumps(deja, indent=1), encoding="utf-8")


def etape_nettoyage(api, token, args):
    for doc in tous_les_documents(api, token):
        if doc["statut"] != "SUPPRIME" and (doc["institution"] in INSTITUTIONS_FICTIVES or doc["institution"] == INSTITUTION_TESTS):
            api.call("DELETE", f"/documents/{doc['id']}", token, expect=(200, 204))
            print(f"  supprime #{doc['id']} [{doc['institution']}] {doc['titre'][:70]}")

    institutions = {i["nom"]: i["id"] for i in api.call("GET", "/admin/institutions", token)}
    utilisateurs = api.call("GET", "/admin/users", token, params={"size": 100})["contenu"]
    for email, cible in RATTACHEMENTS.items():
        compte = next((u for u in utilisateurs if u.get("email") == email), None)
        if compte and cible in institutions:
            api.call("PATCH", f"/admin/users/{compte['id']}", token, json={"institution_id": institutions[cible]})
            print(f"  {email} rattache a {cible}")
    for nom in INSTITUTIONS_FICTIVES:
        if nom in institutions:
            api.call("PATCH", f"/admin/institutions/{institutions[nom]}", token, json={"actif": False})
            print(f"  institution desactivee : {nom}")


def a_extraire(api, token):
    """Documents a valider jamais arbitres : un document publie ou rejete n'est jamais retraite."""
    exclues = INSTITUTIONS_FICTIVES | {INSTITUTION_TESTS}
    for doc in tous_les_documents(api, token, statut="A_VALIDER"):
        if doc["institution"] in exclues:
            continue
        meta = api.call("GET", f"/documents/{doc['id']}/metadata", token, expect=(200, 404))
        if meta and meta.get("statut") == "REJETE":
            continue
        yield doc


def etape_extraction(api, token, args):
    with Bibliothecaire(api, token) as bibliothecaire:
        for doc in a_extraire(api, token):
            extraire(api, bibliothecaire.pour(doc["institution"]), doc)


def extraire(api, token, doc):
    debut = time.time()
    try:
        api.call("POST", f"/documents/{doc['id']}/extraction", token)
        meta = api.call("GET", f"/documents/{doc['id']}/metadata", token)
        print(f"  #{doc['id']} {time.time() - debut:4.1f}s  {len(meta.get('auteurs') or [])} auteurs, "
              f"resume {'oui' if meta.get('resume') else 'non'}  {meta.get('titre', '')[:60]}")
    except RuntimeError as erreur:
        print(f"  #{doc['id']} ECHEC {erreur}")


def etape_evaluation(api, token, args):
    par_id = correspondance()
    lignes = []
    for doc in tous_les_documents(api, token, statut="A_VALIDER"):
        entree = par_id.get(doc["id"])
        if not entree:
            continue
        meta = api.call("GET", f"/documents/{doc['id']}/metadata", token)
        if meta.get("statut") != "EN_ATTENTE":
            continue
        gt = entree["verite_terrain"]
        lignes.append({
            "document_id": doc["id"],
            "institution": entree["institution"],
            "langue": gt.get("langue"),
            "titre_similarite": similarity(meta.get("titre"), gt["titre"]),
            "auteurs_f1": set_f1([a["nom_complet"] for a in meta.get("auteurs") or []],
                                 [a["nom_complet"] for a in gt["auteurs"]]),
            "auteurs_f1_nom": set_f1([nom_de_famille(a["nom_complet"]) for a in meta.get("auteurs") or []],
                                     [nom_de_famille(a["nom_complet"]) for a in gt["auteurs"]]),
            "resume_similarite": similarity(meta.get("resume"), gt.get("resume")) if gt.get("resume") else "",
            "annee_exacte": int(str(meta.get("date_publication") or "")[:4] == str(gt.get("date_publication") or "")[:4]),
            "doi_exact": int(norm(meta.get("doi")) == norm(gt.get("doi"))) if gt.get("doi") else "",
            "langue_exacte": int(meta.get("langue") == gt.get("langue")),
            "mots_cles_f1": set_f1(meta.get("mots_cles") or [], gt.get("mots_cles") or []),
        })
    if not lignes:
        sys.exit("Aucun document extrait a evaluer.")
    chemin = DATA / "evaluation_v5.csv"
    with open(chemin, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(lignes[0].keys()))
        w.writeheader()
        w.writerows(lignes)
    print(f"{len(lignes)} documents evalues -> {chemin}")
    for champ in [c for c in lignes[0] if c not in ("document_id", "institution", "langue")]:
        valeurs = [float(l[champ]) for l in lignes if l[champ] != ""]
        print(f"  {champ:<20} moyenne {sum(valeurs) / len(valeurs):.3f}  (n={len(valeurs)})")


def notice_publiee(meta, gt):
    """
    Notice publiee, comme la composerait un bibliothecaire attentif.

    Ce qui est ecrit dans le document fait foi : titre, resume, mots-cles et langue viennent
    de l'extraction quand elle les a trouves. OpenAlex traduit parfois les titres en anglais
    et ne connait pas toujours le resume de l'auteur. Les donnees d'autorite viennent
    d'OpenAlex : date de publication officielle, DOI, classification et auteurs complets
    avec ORCID. OpenAlex sert aussi de repli pour tout champ que le document ne contient pas.
    """
    corps = validation_body(gt, "PUBLIC")
    corps["auteurs"] = [{**auteur, "nom_complet": nom_usuel(auteur["nom_complet"])} for auteur in corps["auteurs"]]
    # Pour un chapitre, le modele retient souvent le titre de l'ouvrage, souvent en capitales :
    # le titre propre au chapitre est alors celui d'OpenAlex.
    titre = meta.get("titre") or ""
    if titre and not titre.isupper() and gt.get("type_document") != "book-chapter":
        corps["titre"] = titre[:500]
    if meta.get("resume"):
        corps["resume"] = meta["resume"][:5000]
    if meta.get("mots_cles"):
        corps["mots_cles"] = meta["mots_cles"][:30]
    if meta.get("langue"):
        corps["langue"] = meta["langue"]
    # Les auteurs d'OpenAlex font autorite (noms complets, ORCID) ; l'extraction ne sert qu'a defaut.
    extraits = [a["nom_complet"] for a in meta.get("auteurs") or []]
    if not corps["auteurs"] and extraits:
        corps["auteurs"] = [{"nom_complet": n} for n in extraits[:20]]
    if not corps.get("doi") and meta.get("doi"):
        corps["doi"] = meta["doi"]
    return corps


def publier(api, token, doc_id, corps):
    """Le meme travail figure sous deux institutions : un DOI deja pris (409) est retire."""
    try:
        api.call("PUT", f"/documents/{doc_id}/metadata", token, json=corps)
        return
    except RuntimeError as erreur:
        if "409" not in str(erreur) or not corps.get("doi"):
            raise
    print(f"      DOI deja utilise, publication sans DOI : {corps['doi']}")
    api.call("PUT", f"/documents/{doc_id}/metadata", token, json={**corps, "doi": None})


def etape_publication(api, token, args):
    # Sans les vocabulaires, langue et type de document seraient publies vides.
    charger_vocabulaires(api)
    par_id = correspondance()
    candidats = [doc for doc in tous_les_documents(api, token, statut="A_VALIDER") if doc["id"] in par_id]
    rng = random.Random(args.seed)
    rng.shuffle(candidats)
    gardes, a_publier = {}, []
    for doc in candidats:
        meta = api.call("GET", f"/documents/{doc['id']}/metadata", token)
        if meta.get("statut") != "EN_ATTENTE":
            continue
        if gardes.get(doc["institution"], 0) < args.garder:
            gardes[doc["institution"]] = gardes.get(doc["institution"], 0) + 1
            print(f"  garde dans la file #{doc['id']} [{doc['institution']}]")
            continue
        corps = notice_publiee(meta, par_id[doc["id"]]["verite_terrain"])
        a_publier.append((doc, corps))
    with Bibliothecaire(api, token) as bibliothecaire:
        for doc, corps in sorted(a_publier, key=lambda element: element[0]["institution"]):
            publier(api, bibliothecaire.pour(doc["institution"]), doc["id"], corps)
            print(f"  publie #{doc['id']} [{doc['institution']}] {corps['titre'][:70]}")


def etape_noms(api, token, args):
    """Notices deja publiees avec un auteur ecrit « Nom, Prenom » : remises en « Prenom Nom »."""
    with Bibliothecaire(api, token) as bibliothecaire:
        corriger_noms(api, token, bibliothecaire)


def corriger_noms(api, token, bibliothecaire):
    for doc in tous_les_documents(api, token, statut="PUBLIE"):
        meta = api.call("GET", f"/documents/{doc['id']}/metadata", token)
        auteurs = meta.get("auteurs") or []
        if not any(nom_usuel(a["nom_complet"]) != a["nom_complet"] for a in auteurs):
            continue
        corps = {
            "titre": meta["titre"], "resume": meta.get("resume"), "date_publication": meta.get("date_publication"),
            "classification": meta.get("classification"), "visibilite": meta["visibilite"],
            "auteurs": [{"nom_complet": nom_usuel(a["nom_complet"]), "orcid": a.get("orcid")} for a in auteurs],
            "mots_cles": meta.get("mots_cles") or [], "langue": meta.get("langue"),
            "type_document": meta.get("type_document"), "doi": meta.get("doi"),
        }
        api.call("PUT", f"/documents/{doc['id']}/metadata", bibliothecaire.pour(doc["institution"]), json=corps)
        print(f"  #{doc['id']} {', '.join(a['nom_complet'] for a in corps['auteurs'])[:90]}")


def figure_jpeg(chemin, largeur=1000):
    """Figure reduite pour le catalogue : une vignette n'a pas besoin de 1400 pixels."""
    from PIL import Image
    image = Image.open(chemin).convert("RGB")
    if image.width > largeur:
        image = image.resize((largeur, round(image.height * largeur / image.width)))
    tampon = io.BytesIO()
    image.save(tampon, "JPEG", quality=82, optimize=True)
    return tampon.getvalue()


def etape_figures(api, token, args):
    """La vignette devient la figure la plus parlante de l'article, choisie a la main."""
    retenues = [ligne.strip() for ligne in (ROOT / "figures_retenues.txt").read_text(encoding="utf-8").splitlines()
                if ligne.strip() and not ligne.startswith("#")]
    correspondances = json.loads(CORRESPONDANCE.read_text(encoding="utf-8")) if CORRESPONDANCE.exists() else {}
    for nom in retenues:
        ids = correspondances.get(nom.replace(".jpg", ".pdf"), [])
        if not ids:
            print(f"  {nom} : aucun document en ligne")
            continue
        contenu = figure_jpeg(DATA / "figures" / nom)
        for doc_id in ids:
            api.call("PUT", f"/admin/documents/{doc_id}/image", token,
                     files={"image": (nom, contenu, "image/jpeg")})
            print(f"  #{doc_id} <- {nom}")


INSTITUTION_PAR_CODE = {"ULB": "Université libre de Bruxelles", "UCL": "UCLouvain", "ULG": "Université de Liège",
                        "KUL": "KU Leuven", "UGE": "Universiteit Gent", "VUB": "Vrije Universiteit Brussel"}
BIBLIOTHECAIRE = "sarah@institution-a.example"


class Bibliothecaire:
    """
    Seul un bibliothecaire importe, analyse et valide (cahier des charges B3, B5, B6) :
    le compte de demonstration est rattache a l'institution du document le temps de l'action,
    puis retrouve l'UCLouvain.
    """

    def __init__(self, api, jeton_admin):
        mot_de_passe = os.environ.get("METAMIND_SEED_PASSWORD")
        if not mot_de_passe:
            sys.exit("Definis METAMIND_SEED_PASSWORD (mot de passe des comptes de demonstration).")
        self.api, self.admin, self.mot_de_passe = api, jeton_admin, mot_de_passe
        self.institutions = {i["nom"]: i["id"] for i in api.call("GET", "/admin/institutions", jeton_admin)}
        self.compte = next(u for u in api.call("GET", "/admin/users", jeton_admin, params={"size": 100})["contenu"]
                           if u["email"] == BIBLIOTHECAIRE)
        self.actuelle, self.jeton = None, None

    def pour(self, institution):
        if institution != self.actuelle:
            self.api.call("PATCH", f"/admin/users/{self.compte['id']}", self.admin,
                          json={"institution_id": self.institutions[institution]})
            self.jeton, self.actuelle = self.api.login(BIBLIOTHECAIRE, self.mot_de_passe), institution
        return self.jeton

    def __enter__(self):
        return self

    def __exit__(self, *erreur):
        self.api.call("PATCH", f"/admin/users/{self.compte['id']}", self.admin,
                      json={"institution_id": self.institutions["UCLouvain"]})
        print(f"  {BIBLIOTHECAIRE} de retour a l'UCLouvain")


def attendre_texte(api, token, doc_id, limite=120):
    for _ in range(limite):
        doc = api.call("GET", f"/publications/{doc_id}", token)
        # Avant la migration V11, un texte lu faisait passer le document directement "a valider".
        if doc.get("texte_pret") or doc["statut"] in ("ECHEC", "A_VALIDER"):
            return doc
        time.sleep(1)
    return None


def etape_ajout(api, token, args):
    """
    Le document doit appartenir a son universite : il est importe par un bibliothecaire
    rattache temporairement a celle-ci, puis le compte retrouve son institution d'origine.
    """
    mot_de_passe = os.environ.get("METAMIND_SEED_PASSWORD")
    if not mot_de_passe:
        sys.exit("Definis METAMIND_SEED_PASSWORD (mot de passe des comptes de demonstration).")
    charger_vocabulaires(api)
    cles = set(json.loads((DATA / "ajout.json").read_text(encoding="utf-8")))
    manifeste = json.loads((DATA / "manifest.json").read_text(encoding="utf-8"))
    documents = [d for d in manifeste["documents"] if d["key"] in cles]
    retenues = set((ROOT / "figures_retenues.txt").read_text(encoding="utf-8").split())
    correspondances = json.loads(CORRESPONDANCE.read_text(encoding="utf-8")) if CORRESPONDANCE.exists() else {}
    institutions = {i["nom"]: i["id"] for i in api.call("GET", "/admin/institutions", token)}
    compte = next(u for u in api.call("GET", "/admin/users", token, params={"size": 100})["contenu"] if u["email"] == BIBLIOTHECAIRE)
    try:
        for code in sorted({d["institution"] for d in documents}):
            api.call("PATCH", f"/admin/users/{compte['id']}", token, json={"institution_id": institutions[INSTITUTION_PAR_CODE[code]]})
            jeton = api.login(BIBLIOTHECAIRE, mot_de_passe)
            for d in [d for d in documents if d["institution"] == code]:
                pdf = Path(d["pdf"]).name
                if pdf in correspondances:
                    continue
                figure = f"{code}-{d['key']}.jpg"
                image = figure_jpeg(DATA / "figures" / figure) if figure in retenues else (ROOT / d["cover"]).read_bytes()
                cree = api.call("POST", "/documents", jeton, files={
                    "fichier": (pdf, (ROOT / d["pdf"]).read_bytes(), "application/pdf"),
                    "image": (figure, image, "image/jpeg"),
                }, data={"visibilite": "PUBLIC"})
                correspondances[pdf] = [cree["id"]]
                CORRESPONDANCE.write_text(json.dumps(correspondances, indent=1), encoding="utf-8")
                doc = attendre_texte(api, jeton, cree["id"])
                if not doc or doc["statut"] == "ECHEC":
                    print(f"  #{cree['id']} fichier illisible : {d['verite_terrain']['titre'][:60]}")
                    continue
                try:
                    api.call("POST", f"/documents/{cree['id']}/extraction", jeton)
                except RuntimeError as erreur:
                    print(f"  #{cree['id']} analyse impossible ({erreur})")
                    continue
                meta = api.call("GET", f"/documents/{cree['id']}/metadata", jeton)
                corps = notice_publiee(meta, d["verite_terrain"])
                publier(api, jeton, cree["id"], corps)
                print(f"  publie #{cree['id']} [{code}] {corps['titre'][:70]}")
    finally:
        api.call("PATCH", f"/admin/users/{compte['id']}", token, json={"institution_id": institutions["UCLouvain"]})
        print(f"  {BIBLIOTHECAIRE} de retour a l'UCLouvain")


PHOTOS_INSTITUTIONS = {
    "Université libre de Bruxelles": ("ULB.jpg", "Jndemi, CC BY 3.0, Wikimedia Commons"),
    "UCLouvain": ("UCL.jpg", "EmDee, CC BY 4.0, Wikimedia Commons"),
    "Université de Liège": ("ULG.jpg", "Marc Ryckaert, CC BY 4.0, Wikimedia Commons"),
    "KU Leuven": ("KUL.jpg", "Juhanson, CC BY-SA 3.0, Wikimedia Commons"),
    "Universiteit Gent": ("UGE.jpg", "ElienSmits, CC BY-SA 4.0, Wikimedia Commons"),
    "Vrije Universiteit Brussel": ("VUB.jpg", "Romaine, CC0, Wikimedia Commons"),
}


def etape_photos(api, token, args):
    """Les photos etaient figees dans le code du site : elles passent en base, modifiables par l'administrateur."""
    dossier = ROOT.parent / "frontend" / "public" / "institutions"
    institutions = {i["nom"]: i["id"] for i in api.call("GET", "/admin/institutions", token)}
    for nom, (fichier, credit) in PHOTOS_INSTITUTIONS.items():
        if nom not in institutions or not (dossier / fichier).exists():
            print(f"  {nom} : ignore")
            continue
        api.call("PUT", f"/institutions/{institutions[nom]}/photo", token,
                 files={"image": (fichier, (dossier / fichier).read_bytes(), "image/jpeg")}, data={"credit": credit})
        print(f"  {nom} <- {fichier}")


def etape_traductions(api, token, args):
    api.call("POST", "/admin/traductions", token, expect=(200, 202, 204))
    print("  traduction de toutes les notices publiees lancee sur le serveur (quelques minutes)")


ETAPES = {
    "fichiers": etape_fichiers,
    "nettoyage": etape_nettoyage,
    "extraction": etape_extraction,
    "evaluation": etape_evaluation,
    "publication": etape_publication,
    "noms": etape_noms,
    "figures": etape_figures,
    "ajout": etape_ajout,
    "photos": etape_photos,
    "traductions": etape_traductions,
}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("etape", choices=ETAPES)
    ap.add_argument("--api", default="https://metamind-app.duckdns.org/api/v1")
    ap.add_argument("--garder", type=int, default=3, help="documents laisses dans la file par institution")
    ap.add_argument("--seed", type=int, default=7)
    args = ap.parse_args()
    email = os.environ.get("METAMIND_ADMIN_EMAIL")
    mot_de_passe = os.environ.get("METAMIND_ADMIN_PASSWORD")
    if not (email and mot_de_passe):
        sys.exit("Definis METAMIND_ADMIN_EMAIL et METAMIND_ADMIN_PASSWORD.")
    api = Api(args.api)
    token = api.login(email, mot_de_passe)
    ETAPES[args.etape](api, token, args)


if __name__ == "__main__":
    main()
