"""Termine les derniers documents de la VUB quand Gemini limite le debit : pauses longues entre les essais."""
import json, random, sys, time
from pathlib import Path
import load

pwd = sys.argv[1]
api = load.Api("https://metamind-app.duckdns.org/api/v1", False)
tok = api.login("lotte.wouters@vub.demo-metamind.test", pwd)
m = json.loads(Path("data/manifest.json").read_text(encoding="utf-8"))
vub = [d for d in m["documents"] if d["institution"] == "VUB"]
rng = random.Random(7)
resultat = []

def analyser(doc_id):
    for essai in range(1, 11):
        try:
            return load.start_extraction_when_ready(api, tok, doc_id)
        except RuntimeError as erreur:
            if "502" not in str(erreur) and "503" not in str(erreur) and "429" not in str(erreur):
                raise
            print(f"    Gemini indisponible ({essai}/10), nouvel essai dans 60 s", flush=True)
            time.sleep(60)
    raise RuntimeError(f"analyse impossible pour {doc_id}")

for index, d in enumerate(vub[11:], start=11):
    gt = d["verite_terrain"]
    roll = rng.random()
    cible = "EN_ATTENTE" if roll < 0.10 else "A_VALIDER" if roll < 0.25 else "PUBLIE" if roll < 0.95 else "REJETE"
    if index == 11:
        doc_id = 208
    else:
        with open(d["pdf"], "rb") as pdf, open(d["cover"], "rb") as cover:
            doc_id = api.call("POST", "/documents", tok, files={
                "fichier": (Path(d["pdf"]).name, pdf, "application/pdf"),
                "image": (Path(d["cover"]).name, cover, "image/jpeg")}, data={"visibilite": "PUBLIC"})["id"]
        load.wait_status(api, tok, doc_id, {"EN_ATTENTE", "A_VALIDER"})
    if cible != "EN_ATTENTE":
        analyser(doc_id)
        load.wait_status(api, tok, doc_id, {"A_VALIDER"})
        if cible == "PUBLIE":
            load.publier_metadonnees(api, tok, doc_id, gt, "PUBLIC")
        elif cible == "REJETE":
            api.call("POST", f"/documents/{doc_id}/metadata/rejet", tok, json={"motif": rng.choice(load.MOTIFS_REJET)})
    print(f"  #{doc_id} {cible:<10} {gt['titre'][:70]}", flush=True)
    resultat.append({"document_id": doc_id, "key": d["key"], "institution": "VUB", "etat": cible,
                     "par": "lotte.wouters@vub.demo-metamind.test", "date_publication": gt.get("date_publication")})
    Path("data/load_result.partie3.json").write_text(json.dumps(resultat, ensure_ascii=False, indent=2), encoding="utf-8")
print("TERMINE")
