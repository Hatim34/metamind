#!/usr/bin/env python3
"""
Étape 1 : constitue un corpus RÉEL de publications en libre accès pour la démo Metamind.

Source : OpenAlex (métadonnées CC0). Pour chaque institution de institutions.json :
  - retrouve l'institution dans OpenAlex (pays BE) ;
  - cherche des publications en libre accès, sous licence réutilisable, avec un PDF ;
  - télécharge le PDF, vérifie qu'il est valide, génère une vignette de la 1re page (couverture) ;
  - garde les métadonnées OpenAlex comme « vérité terrain » (sert à valider et à mesurer le LLM).
Option --photos : récupère une photo de campus sur Wikimedia Commons (licence libre + crédit).

Sortie : data/manifest.json, data/pdf/*.pdf, data/covers/*.jpg, data/photos/*.jpg

Usage :
  pip install -r requirements.txt
  python collect.py --mailto ton.email@exemple.be --photos --figures --hero [--allow-nc] [--only ULB,KUL]
"""
import argparse
import hashlib
import html
import json
import re
import sys
import time
from pathlib import Path

import fitz  # PyMuPDF
import requests

OPENALEX = "https://api.openalex.org"
COMMONS = "https://commons.wikimedia.org/w/api.php"
ROOT = Path(__file__).parent
DATA = ROOT / "data"

# Licences qui autorisent la rediffusion par un service (commercial) comme Metamind.
LICENCES_OK = {"cc-by", "cc-by-sa", "cc0", "public-domain"}
LICENCES_NC = {"cc-by-nc", "cc-by-nc-sa"}
TYPES = "article|preprint|dissertation|book-chapter|report|review"
MAX_PDF_BYTES = 25 * 1024 * 1024
MAX_PAGES = 400

session = requests.Session()


def get_json(url, params=None, tries=4):
    for attempt in range(tries):
        try:
            r = session.get(url, params=params, timeout=30)
            if r.status_code == 200:
                return r.json()
            if r.status_code in (429, 500, 502, 503, 504):
                time.sleep(2 ** attempt)
                continue
            r.raise_for_status()
        except requests.RequestException as exc:
            if attempt == tries - 1:
                raise
            print(f"  réseau : {exc}, nouvel essai", file=sys.stderr)
            time.sleep(2 ** attempt)
    raise RuntimeError(f"Échec après {tries} essais : {url}")


def resolve_institution(search):
    data = get_json(f"{OPENALEX}/institutions", {"search": search, "filter": "country_code:BE", "per-page": 5})
    if not data["results"]:
        raise SystemExit(f"Institution introuvable dans OpenAlex : {search}")
    inst = data["results"][0]
    print(f"  -> {inst['display_name']} | {inst['id']} | ROR {inst.get('ror')}")
    return inst


def abstract_from_index(index):
    if not index:
        return None
    positions = [(pos, word) for word, poss in index.items() for pos in poss]
    return " ".join(word for _, word in sorted(positions)).strip() or None


def pick_location(work, allow_nc):
    allowed = LICENCES_OK | (LICENCES_NC if allow_nc else set())
    locations = [work.get("best_oa_location")] + (work.get("locations") or [])
    for loc in locations:
        if loc and loc.get("pdf_url") and (loc.get("license") or "").lower() in allowed:
            return loc
    return None


def ground_truth(work, loc):
    authors = []
    for a in sorted(work.get("authorships") or [], key=lambda x: {"first": 0, "middle": 1, "last": 2}.get(x.get("author_position"), 1)):
        author = a.get("author") or {}
        orcid = (author.get("orcid") or "").replace("https://orcid.org/", "") or None
        name = author.get("display_name") or a.get("raw_author_name")
        if name:
            authors.append({"nom_complet": name[:255], "orcid": orcid,
                            "affiliations": [i.get("display_name") for i in a.get("institutions") or []]})
    topic = work.get("primary_topic") or {}
    keywords = [k.get("display_name") for k in work.get("keywords") or [] if k.get("display_name")]
    if len(keywords) < 4 and topic.get("display_name"):
        keywords.append(topic["display_name"])
    source = (loc.get("source") or {}) if loc else {}
    return {
        "titre": (work.get("title") or work.get("display_name") or "").strip(),
        "auteurs": authors[:20],
        "resume": abstract_from_index(work.get("abstract_inverted_index")),
        "mots_cles": keywords[:8],
        "date_publication": work.get("publication_date"),
        "langue": work.get("language"),
        "type_document": work.get("type"),
        "classification": (topic.get("field") or {}).get("display_name"),
        "sous_domaine": (topic.get("subfield") or {}).get("display_name"),
        "doi": (work.get("doi") or "").replace("https://doi.org/", "") or None,
        "revue": source.get("display_name"),
        "licence": loc.get("license") if loc else None,
        "url_source": loc.get("landing_page_url") if loc else None,
        "openalex_id": work.get("id"),
        "citations": work.get("cited_by_count"),
    }


def download_pdf(url, dest):
    try:
        with session.get(url, timeout=60, stream=True, allow_redirects=True) as r:
            if r.status_code != 200:
                return f"HTTP {r.status_code}"
            chunks, size = [], 0
            for chunk in r.iter_content(65536):
                size += len(chunk)
                if size > MAX_PDF_BYTES:
                    return "trop volumineux"
                chunks.append(chunk)
        content = b"".join(chunks)
        if not content.startswith(b"%PDF"):
            return "pas un PDF (page HTML ou captcha)"
        dest.write_bytes(content)
        return None
    except requests.RequestException as exc:
        return f"réseau : {exc.__class__.__name__}"


def make_cover(pdf_path, cover_path):
    """Vignette de la 1re page : la « photo » honnête d'une publication."""
    try:
        doc = fitz.open(pdf_path)
        if doc.page_count == 0 or doc.page_count > MAX_PAGES:
            return None
        text_len = len(doc[0].get_text().strip())
        page = doc[0]
        zoom = 900 / page.rect.width
        pix = page.get_pixmap(matrix=fitz.Matrix(zoom, zoom), alpha=False)
        pix.save(cover_path, jpg_quality=82)
        return {"pages": doc.page_count, "texte_page1": text_len}
    except Exception as exc:  # PDF corrompu ou chiffré
        print(f"    vignette impossible : {exc}", file=sys.stderr)
        return None


def extract_figure(pdf_path, fig_path):
    """Plus grande figure du document (graphique, photo, schéma) : visuel de carte plus parlant que la page 1.
    Autorisé par les licences retenues (CC BY, CC BY-SA, CC0), avec crédit à la publication."""
    try:
        doc = fitz.open(pdf_path)
        best = None
        for page_no in range(min(doc.page_count, 12)):
            for img in doc[page_no].get_images(full=True):
                xref, w, h = img[0], img[2], img[3]
                if w < 500 or h < 300 or not 0.6 <= w / h <= 2.4:
                    continue  # logos, icônes, bandeaux
                if not best or w * h > best[1] * best[2]:
                    best = (xref, w, h, page_no + 1)
        if not best:
            return None
        pix = fitz.Pixmap(doc, best[0])
        if pix.n - pix.alpha >= 4:  # CMYK -> RGB
            pix = fitz.Pixmap(fitz.csRGB, pix)
        if pix.width > 1400:
            pix = fitz.Pixmap(pix, 1400, int(pix.height * 1400 / pix.width))
        pix.save(fig_path, jpg_quality=85)
        return {"fichier": str(fig_path.relative_to(ROOT)), "page": best[3]}
    except Exception:
        return None


def collect_for(inst_conf, args):
    print(f"\n[{inst_conf['code']}] {inst_conf['name']}")
    inst = resolve_institution(inst_conf["openalex_search"])
    target = args.per_institution or inst_conf.get("target", 25)
    langs = inst_conf.get("langs", ["en"])
    # Répartition : ~45 % dans la langue locale, le reste en anglais.
    quotas = {}
    local = [l for l in langs if l != "en"]
    if local:
        quotas[local[0]] = max(1, round(target * 0.45))
    quotas["en"] = target - sum(quotas.values())

    records = []
    for lang, quota in quotas.items():
        got, cursor, seen_pages = 0, "*", 0
        while got < quota and cursor and seen_pages < 12:
            params = {
                "filter": ",".join([
                    f"authorships.institutions.id:{inst['id'].split('/')[-1]}",
                    "is_oa:true",
                    f"language:{lang}",
                    f"type:{TYPES}",
                    f"publication_year:{args.year_from}-{args.year_to}",
                ]),
                "sort": "publication_date:desc",
                "per-page": 50,
                "cursor": cursor,
            }
            page = get_json(f"{OPENALEX}/works", params)
            cursor = page.get("meta", {}).get("next_cursor")
            seen_pages += 1
            for work in page["results"]:
                if got >= quota:
                    break
                loc = pick_location(work, args.allow_nc)
                if not loc:
                    continue
                gt = ground_truth(work, loc)
                if not gt["titre"] or not gt["auteurs"]:
                    continue
                key = hashlib.sha1(work["id"].encode()).hexdigest()[:12]
                pdf_path = DATA / "pdf" / f"{inst_conf['code']}-{key}.pdf"
                cover_path = DATA / "covers" / f"{inst_conf['code']}-{key}.jpg"
                if not pdf_path.exists():
                    err = download_pdf(loc["pdf_url"], pdf_path)
                    if err:
                        print(f"    ignoré ({err}) : {gt['titre'][:70]}")
                        continue
                info = make_cover(pdf_path, cover_path)
                if not info:
                    pdf_path.unlink(missing_ok=True)
                    continue
                figure = extract_figure(pdf_path, DATA / "figures" / f"{inst_conf['code']}-{key}.jpg") if args.figures else None
                records.append({
                    "key": key,
                    "institution": inst_conf["code"],
                    "pdf": str(pdf_path.relative_to(ROOT)),
                    "cover": str(cover_path.relative_to(ROOT)),
                    "pdf_url": loc["pdf_url"],
                    "pages": info["pages"],
                    "scan_probable": info["texte_page1"] < 50,
                    "figure": figure,
                    "verite_terrain": gt,
                })
                got += 1
                print(f"    + [{lang}] {gt['titre'][:80]}")
                time.sleep(0.2)
        if got < quota:
            print(f"  ! {lang} : {got}/{quota} seulement (élargis --year-from ou ajoute --allow-nc)")
    return inst, records


def commons_photo(inst_conf, query=None, dest_name=None):
    """Photo sous licence libre (Wikimedia Commons), avec auteur et licence pour le crédit."""
    params = {
        "action": "query", "format": "json", "generator": "search",
        "gsrsearch": query or f"{inst_conf['name']} {inst_conf['city']} campus filetype:bitmap",
        "gsrnamespace": 6, "gsrlimit": 15,
        "prop": "imageinfo", "iiprop": "url|size|extmetadata", "iiurlwidth": 1600,
    }
    data = get_json(COMMONS, params)
    pages = sorted((data.get("query") or {}).get("pages", {}).values(), key=lambda p: p.get("index", 99))
    for p in pages:
        info = (p.get("imageinfo") or [{}])[0]
        meta = info.get("extmetadata") or {}
        licence = (meta.get("LicenseShortName") or {}).get("value", "")
        if not re.search(r"CC BY|CC0|Public domain", licence, re.I) or re.search(r"NC|ND", licence):
            continue
        if info.get("width", 0) < 1200 or info.get("width", 0) <= info.get("height", 0):
            continue
        artist = re.sub(r"<[^>]+>", "", html.unescape((meta.get("Artist") or {}).get("value", ""))).strip()
        dest = DATA / "photos" / f"{dest_name or inst_conf['code']}.jpg"
        img = session.get(info["thumburl"], timeout=60)
        if img.status_code != 200:
            continue
        dest.write_bytes(img.content)
        print(f"  photo : {p['title']} ({licence}, {artist})")
        return {"fichier": str(dest.relative_to(ROOT)), "titre": p["title"], "auteur": artist,
                "licence": licence, "url_licence": (meta.get("LicenseUrl") or {}).get("value"),
                "source": info.get("descriptionurl")}
    print(f"  photo : rien de satisfaisant pour « {params['gsrsearch']} », à choisir à la main sur commons.wikimedia.org")
    return None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--mailto", required=True, help="email de contact (pool « polite » d'OpenAlex)")
    ap.add_argument("--photos", action="store_true", help="photos de campus depuis Wikimedia Commons")
    ap.add_argument("--figures", action="store_true", help="extraire la plus grande figure de chaque PDF")
    ap.add_argument("--hero", action="store_true", help="3 photos d'ambiance (bibliothèques, salles de lecture) pour l'accueil")
    ap.add_argument("--allow-nc", action="store_true", help="accepter aussi les licences CC BY-NC")
    ap.add_argument("--only", help="codes séparés par des virgules, ex. ULB,KUL")
    ap.add_argument("--per-institution", type=int, help="remplace 'target' de institutions.json")
    ap.add_argument("--year-from", type=int, default=2019)
    ap.add_argument("--year-to", type=int, default=2026)
    args = ap.parse_args()

    session.headers["User-Agent"] = f"Metamind-demo-seed/1.0 (mailto:{args.mailto})"
    session.params = {"mailto": args.mailto}
    for d in ("pdf", "covers", "photos", "figures"):
        (DATA / d).mkdir(parents=True, exist_ok=True)

    conf = json.loads((ROOT / "institutions.json").read_text(encoding="utf-8"))["institutions"]
    if args.only:
        wanted = {c.strip().upper() for c in args.only.split(",")}
        conf = [c for c in conf if c["code"] in wanted]

    manifest = {"genere_le": time.strftime("%Y-%m-%dT%H:%M:%S"), "source": "OpenAlex (CC0) + PDF en libre accès",
                "institutions": [], "documents": []}
    for inst_conf in conf:
        inst, records = collect_for(inst_conf, args)
        entry = dict(inst_conf, openalex_id=inst["id"], ror=inst.get("ror"), site_web=inst.get("homepage_url"))
        if args.photos:
            session.params = {}
            entry["photo"] = commons_photo(inst_conf)
            session.params = {"mailto": args.mailto}
        manifest["institutions"].append(entry)
        manifest["documents"].extend(records)

    if args.hero:
        session.params = {}
        queries = ["university library reading room Belgium filetype:bitmap",
                   "Leuven university library filetype:bitmap",
                   "Brussels university campus aerial filetype:bitmap"]
        manifest["accueil"] = [p for i, q in enumerate(queries)
                               if (p := commons_photo({"name": "", "city": ""}, q, f"accueil-{i + 1}"))]

    out = DATA / "manifest.json"
    out.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\n{len(manifest['documents'])} documents réels collectés -> {out}")


if __name__ == "__main__":
    main()
