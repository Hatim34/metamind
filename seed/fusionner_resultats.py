"""Reconstitue load_result.json et reprise_correspondance.json apres un chargement en plusieurs morceaux."""
import json, re
from pathlib import Path
D = Path("data")
m = json.loads((D / "manifest.json").read_text(encoding="utf-8"))
par_titre = {}
for d in m["documents"]:
    par_titre.setdefault(d["verite_terrain"]["titre"][:70], d)
docs = []
inst = None
for ligne in (D / "load_prod.partie1.log").read_text(encoding="utf-8").splitlines():
    t = re.match(r"\[(\w+)\]", ligne)
    if t:
        inst = t.group(1)
    t = re.match(r"  #(\d+) (\S+)\s+(.*)$", ligne)
    if t:
        d = par_titre[t.group(3)[:70]]
        docs.append({"document_id": int(t.group(1)), "key": d["key"], "institution": inst, "etat": t.group(2)})
docs += json.loads((D / "load_result.partie2.json").read_text(encoding="utf-8"))["documents"]
docs.append({"document_id": 207, "key": next(d["key"] for d in m["documents"] if d["verite_terrain"]["titre"].startswith("The Relationship Between Inter-limb Asymmetry")), "institution": "VUB", "etat": "PUBLIE"})
p3 = D / "load_result.partie3.json"
if p3.exists():
    docs += json.loads(p3.read_text(encoding="utf-8"))
ids = [d["document_id"] for d in docs]
assert len(ids) == len(set(ids)), "doublon"
print(len(docs), "documents")
cles = {d["key"]: d for d in m["documents"]}
corr = {}
for d in docs:
    corr.setdefault(Path(cles[d["key"]]["pdf"]).name, []).append(d["document_id"])
(D / "reprise_correspondance.json").write_text(json.dumps(corr, ensure_ascii=False, indent=2), encoding="utf-8")
