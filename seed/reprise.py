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

Usage :
  export METAMIND_ADMIN_EMAIL=admin@metamind.example
  export METAMIND_ADMIN_PASSWORD=...
  python reprise.py fichiers --api https://metamind-app.duckdns.org/api/v1
"""
import argparse
import csv
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
    for doc in a_extraire(api, token):
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
    d'OpenAlex : date de publication officielle, DOI, classification, et auteurs complets
    avec ORCID quand ce sont bien les memes personnes. OpenAlex sert aussi de repli pour
    tout champ que le document ne contient pas.
    """
    corps = validation_body(gt, "PUBLIC")
    corps["auteurs"] = [{**auteur, "nom_complet": nom_usuel(auteur["nom_complet"])} for auteur in corps["auteurs"]]
    if meta.get("titre"):
        corps["titre"] = meta["titre"][:500]
    if meta.get("resume"):
        corps["resume"] = meta["resume"][:5000]
    if meta.get("mots_cles"):
        corps["mots_cles"] = meta["mots_cles"][:30]
    if meta.get("langue"):
        corps["langue"] = meta["langue"]
    extraits = [a["nom_complet"] for a in meta.get("auteurs") or []]
    memes_personnes = set_f1([nom_de_famille(n) for n in extraits],
                             [nom_de_famille(a["nom_complet"]) for a in gt["auteurs"]]) >= 0.5
    if extraits and not memes_personnes:
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
    gardes = {}
    for doc in candidats:
        meta = api.call("GET", f"/documents/{doc['id']}/metadata", token)
        if meta.get("statut") != "EN_ATTENTE":
            continue
        if gardes.get(doc["institution"], 0) < args.garder:
            gardes[doc["institution"]] = gardes.get(doc["institution"], 0) + 1
            print(f"  garde dans la file #{doc['id']} [{doc['institution']}]")
            continue
        corps = notice_publiee(meta, par_id[doc["id"]]["verite_terrain"])
        publier(api, token, doc["id"], corps)
        print(f"  publie #{doc['id']} [{doc['institution']}] {corps['titre'][:70]}")


def etape_noms(api, token, args):
    """Notices deja publiees avec un auteur ecrit « Nom, Prenom » : remises en « Prenom Nom »."""
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
        api.call("PUT", f"/documents/{doc['id']}/metadata", token, json=corps)
        print(f"  #{doc['id']} {', '.join(a['nom_complet'] for a in corps['auteurs'])[:90]}")


ETAPES = {
    "fichiers": etape_fichiers,
    "nettoyage": etape_nettoyage,
    "extraction": etape_extraction,
    "evaluation": etape_evaluation,
    "publication": etape_publication,
    "noms": etape_noms,
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
