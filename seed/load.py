#!/usr/bin/env python3
"""
Étape 2 : charge le corpus dans Metamind EN PASSANT PAR SON API (comme un vrai utilisateur).

Ce que fait le script :
  1. crée les institutions (domaine .test) et leur dotation de crédits ;
  2. crée 2 à 3 bibliothécaires fictifs par institution, activés par l'admin ;
  3. importe chaque PDF avec sa couverture, sous l'identité d'un bibliothécaire de l'institution ;
  4. répartit les documents dans des états réalistes :
       publié (~70 %), à valider (~15 %), en attente (~10 %), rejeté (~5 %) ;
     pour les publiés, valide les métadonnées avec la vérité terrain OpenAlex (= correction humaine) ;
  5. avant validation, compare la sortie du LLM à la vérité terrain -> data/evaluation_seed.csv
     (premier jeu de mesures pour le chapitre « Évaluation » du rapport).
Sortie : data/load_result.json (correspondance document Metamind <-> publication, utilisée par backdate.py)

Usage :
  export METAMIND_ADMIN_EMAIL=... METAMIND_ADMIN_PASSWORD=... METAMIND_DEMO_PASSWORD='Un-mot-de-passe-long-2026'
  python load.py --api http://localhost:8080/api/v1 [--seed 42] [--dry-run]
"""
import argparse
import csv
import difflib
import json
import os
import random
import sys
import time
from pathlib import Path

import requests

ROOT = Path(__file__).parent
DATA = ROOT / "data"

# Noms fictifs (aucun membre réel du personnel des institutions).
PRENOMS_FR = ["Camille", "Julien", "Sarah", "Thomas", "Inès", "Nicolas", "Léa", "Mehdi", "Charlotte", "Antoine", "Yasmine", "Maxime"]
NOMS_FR = ["Dubois", "Lambert", "Martin", "Peeters", "Lejeune", "Renard", "Mertens", "Collard", "Bensaïd", "Lefèvre", "Gilson", "Wauters"]
PRENOMS_NL = ["Lotte", "Pieter", "Eline", "Jonas", "Femke", "Wout", "Hanne", "Arne", "Nora", "Bram", "Lien", "Seppe"]
NOMS_NL = ["Janssens", "Maes", "Claes", "Wouters", "De Smet", "Vermeulen", "Goossens", "Van den Broeck", "Willems", "Jacobs", "Hermans", "Aerts"]

MOTIFS_REJET = [
    "Document en double : déjà déposé sous une autre version.",
    "Version éditeur sous embargo, dépôt de la version auteur demandé.",
    "Fichier incomplet : annexes manquantes.",
]


class Api:
    def __init__(self, base, dry_run=False):
        self.base = base.rstrip("/")
        self.dry = dry_run
        self.s = requests.Session()

    def call(self, method, path, token=None, expect=(200, 201, 202), **kw):
        if self.dry:
            print(f"DRY {method} {path}")
            return {}
        headers = kw.pop("headers", {})
        if token:
            headers["Authorization"] = f"Bearer {token}"
        r = self.s.request(method, f"{self.base}{path}", headers=headers, timeout=120, **kw)
        if r.status_code not in expect:
            raise RuntimeError(f"{method} {path} -> {r.status_code} {r.text[:300]}")
        return r.json() if r.content and "json" in r.headers.get("Content-Type", "") else {}

    def login(self, email, password):
        return self.call("POST", "/auth/login", json={"email": email, "password": password}).get("token")


def norm(s):
    return " ".join((s or "").lower().split())


def similarity(a, b):
    return round(difflib.SequenceMatcher(None, norm(a), norm(b)).ratio(), 3) if a and b else 0.0


def set_f1(pred, truth):
    p, t = {norm(x) for x in pred if x}, {norm(x) for x in truth if x}
    if not p or not t:
        return 0.0
    inter = len(p & t)
    if inter == 0:
        return 0.0
    prec, rec = inter / len(p), inter / len(t)
    return round(2 * prec * rec / (prec + rec), 3)


def wait_status(api, token, doc_id, wanted, timeout=90):
    end = time.time() + timeout
    status = None
    while time.time() < end:
        status = api.call("GET", f"/documents/{doc_id}", token).get("statut")
        if status in wanted:
            return status
        time.sleep(2)
    return status


def ensure_institution(api, admin, inst):
    existing = api.call("GET", "/institutions", admin) or []
    for e in existing if isinstance(existing, list) else []:
        if (e.get("domaine_email") or "").lower() == inst["domain"]:
            return e["id"]
    created = api.call("POST", "/institutions", admin,
                       json={"code": inst["code"], "name": inst["name"], "emailDomain": inst["domain"]})
    return created.get("id")


def find_user_id(api, admin, email, institution_id=None):
    page_no = 0
    while True:
        params = {"page": page_no, "size": 100}
        if institution_id:
            params["institutionId"] = institution_id
        page = api.call("GET", "/admin/users", admin, params=params)
        for u in page.get("contenu", []):
            if u.get("email", "").lower() == email.lower():
                return u["id"]
        page_no += 1
        if page_no >= page.get("total_pages", 0):
            return None


def create_librarians(api, admin, inst, inst_id, password, rng, n):
    flemish = "nl" in inst.get("langs", [])
    prenoms, noms = (PRENOMS_NL, NOMS_NL) if flemish else (PRENOMS_FR, NOMS_FR)
    users = []
    for prenom, nom in zip(rng.sample(prenoms, n), rng.sample(noms, n)):
        slug = lambda x: "".join(c for c in x.lower().replace(" ", "") if c.isalnum())
        email = f"{slug(prenom)}.{slug(nom)}@{inst['domain']}"
        try:
            api.call("POST", "/auth/register", json={"firstName": prenom, "lastName": nom, "email": email,
                                                     "institution": inst["name"], "password": password})
        except RuntimeError as exc:
            if "409" not in str(exc) and "existe" not in str(exc).lower():
                raise
        uid = find_user_id(api, admin, email, inst_id)
        if uid:
            api.call("PATCH", f"/admin/users/{uid}", admin, json={"statut": "ACTIF"})
        users.append({"email": email, "prenom": prenom, "nom": nom, "id": uid})
        print(f"    bibliothécaire : {prenom} {nom} <{email}>")
    return users


def validation_body(gt, visibility):
    date = gt.get("date_publication")
    return {
        "titre": gt["titre"][:500],
        "resume": (gt.get("resume") or "")[:5000] or None,
        "date_publication": date,
        "classification": (gt.get("classification") or "")[:255] or None,
        "visibilite": visibility,
        "auteurs": [{"nom_complet": a["nom_complet"], "orcid": a.get("orcid")} for a in gt["auteurs"][:20]],
        "mots_cles": gt.get("mots_cles", [])[:30],
        "langue": gt.get("langue"),
        "type_document": gt.get("type_document"),
        "doi": gt.get("doi"),
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--api", default="http://localhost:8080/api/v1")
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--publish", type=float, default=0.70)
    ap.add_argument("--to-validate", type=float, default=0.15)
    ap.add_argument("--pending", type=float, default=0.10)
    ap.add_argument("--private", type=float, default=0.15, help="part des documents publiés en visibilité INSTITUTION")
    ap.add_argument("--limit", type=int, help="nombre max de documents (test rapide)")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    admin_email = os.environ.get("METAMIND_ADMIN_EMAIL")
    admin_pwd = os.environ.get("METAMIND_ADMIN_PASSWORD")
    demo_pwd = os.environ.get("METAMIND_DEMO_PASSWORD")
    if not (admin_email and admin_pwd and demo_pwd) or len(demo_pwd) < 12:
        sys.exit("Définis METAMIND_ADMIN_EMAIL, METAMIND_ADMIN_PASSWORD et METAMIND_DEMO_PASSWORD (12 caractères min).")

    manifest = json.loads((DATA / "manifest.json").read_text(encoding="utf-8"))
    docs = manifest["documents"][: args.limit] if args.limit else manifest["documents"]
    rng = random.Random(args.seed)
    api = Api(args.api, args.dry_run)
    admin = api.login(admin_email, admin_pwd)

    result = {"institutions": {}, "documents": []}
    for inst in manifest["institutions"]:
        print(f"\n[{inst['code']}] {inst['name']}")
        inst_id = ensure_institution(api, admin, inst)
        users = create_librarians(api, admin, inst, inst_id, demo_pwd, rng, rng.choice([2, 3]))
        n_docs = sum(1 for d in docs if d["institution"] == inst["code"])
        dotation = n_docs + rng.choice([40, 60, 85])
        api.call("POST", f"/admin/institutions/{inst_id}/credits/adjustments", admin,
                 json={"amount": dotation, "reason": "Dotation initiale (jeu de démonstration)"})
        tokens = {u["email"]: api.login(u["email"], demo_pwd) for u in users}
        result["institutions"][inst["code"]] = {"id": inst_id, "utilisateurs": users, "dotation": dotation}

        eval_rows = []
        for d in [d for d in docs if d["institution"] == inst["code"]]:
            gt = d["verite_terrain"]
            user = rng.choice(users)
            token = tokens[user["email"]]
            roll = rng.random()
            if roll < args.pending:
                target = "EN_ATTENTE"
            elif roll < args.pending + args.to_validate:
                target = "A_VALIDER"
            elif roll < args.pending + args.to_validate + args.publish:
                target = "PUBLIE"
            else:
                target = "REJETE"
            visibility = "INSTITUTION" if target == "PUBLIE" and rng.random() < args.private else "PUBLIC"

            with open(ROOT / d["pdf"], "rb") as pdf, open(ROOT / d["cover"], "rb") as cover:
                created = api.call("POST", "/documents", token, files={
                    "fichier": (Path(d["pdf"]).name, pdf, "application/pdf"),
                    "image": (Path(d["cover"]).name, cover, "image/jpeg"),
                }, data={"visibilite": visibility})
            doc_id = created.get("id")
            wait_status(api, token, doc_id, {"EN_ATTENTE", "A_VALIDER"})
            print(f"  #{doc_id} {target:<10} {gt['titre'][:70]}")

            if target != "EN_ATTENTE":
                api.call("POST", f"/documents/{doc_id}/extraction", token)
                wait_status(api, token, doc_id, {"A_VALIDER"})
                llm = api.call("GET", f"/documents/{doc_id}/metadata", token)
                eval_rows.append({
                    "document_id": doc_id, "institution": inst["code"], "langue": gt.get("langue"),
                    "type": gt.get("type_document"),
                    "titre_similarite": similarity(llm.get("titre"), gt["titre"]),
                    "auteurs_f1": set_f1([a.get("nom_complet") for a in llm.get("auteurs") or []],
                                         [a["nom_complet"] for a in gt["auteurs"]]),
                    "annee_exacte": int(str(llm.get("date_publication") or "")[:4] == str(gt.get("date_publication") or "")[:4]),
                    "mots_cles_f1": set_f1(llm.get("mots_cles") or [], gt.get("mots_cles") or []),
                    "resume_similarite": similarity(llm.get("resume"), gt.get("resume")),
                    "classification_exacte": int(norm(llm.get("classification")) == norm(gt.get("classification"))),
                })
                if target == "PUBLIE":
                    api.call("PUT", f"/documents/{doc_id}/metadata", token, json=validation_body(gt, visibility))
                elif target == "REJETE":
                    api.call("POST", f"/documents/{doc_id}/metadata/rejet", token,
                             json={"motif": rng.choice(MOTIFS_REJET)})

            result["documents"].append({"document_id": doc_id, "key": d["key"], "institution": inst["code"],
                                        "etat": target, "par": user["email"],
                                        "date_publication": gt.get("date_publication")})

        if eval_rows:
            path = DATA / "evaluation_seed.csv"
            new = not path.exists()
            with open(path, "a", newline="", encoding="utf-8") as f:
                w = csv.DictWriter(f, fieldnames=list(eval_rows[0].keys()))
                if new:
                    w.writeheader()
                w.writerows(eval_rows)

    (DATA / "load_result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\nTerminé : {len(result['documents'])} documents. Lance ensuite : python backdate.py > data/backdate.sql")


if __name__ == "__main__":
    main()
