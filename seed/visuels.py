#!/usr/bin/env python3
"""
Visuels originaux Metamind (aucun droit tiers) aux couleurs du livrable 10.

Motif : des pages de documents vues de dessus, dont certaines lignes sont « extraites »
(surlignées en teal avec un repère de champ) -> la métaphore de l'extraction de métadonnées.
Chaque institution a sa propre composition (graine = son code), donc un visuel reconnaissable
sans utiliser de logo ni de photo.

Sortie : visuels/institutions/<CODE>.jpg, visuels/accueil/hero-*.jpg, visuels/couverture-defaut.jpg
Usage : python visuels.py
"""
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).parent
OUT = ROOT / "visuels"

TEAL = (18, 120, 123)
TEAL_DARK = (11, 84, 86)
TEAL_LIGHT = (225, 240, 240)
INK = (24, 42, 43)
SLATE = (85, 103, 106)
PAPER = (245, 247, 248)
OCHRE = (154, 107, 18)
RUST = (180, 83, 27)

SS = 2  # suréchantillonnage pour l'anticrénelage


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def rotated_page(w, h, rng, theme):
    """Une page : papier, lignes de texte, quelques champs extraits."""
    pad = int(w * 0.11)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    paper, line, accent = theme["paper"], theme["line"], theme["accent"]
    d.rounded_rectangle((0, 0, w - 1, h - 1), radius=int(w * 0.02), fill=paper)
    y = pad
    # Titre (2 lignes épaisses), puis auteurs, puis paragraphes.
    blocks = [("titre", 2, 0.034), ("auteurs", 1, 0.016), ("gap", 0, 0.03)] + \
             [("texte", rng.randint(3, 6), 0.011) for _ in range(rng.randint(3, 5))]
    extracted = {"titre", "auteurs"} if rng.random() < 0.7 else {"titre"}
    for kind, n, thick in blocks:
        if kind == "gap":
            y += int(h * thick)
            continue
        th = max(2, int(h * thick))
        for i in range(n):
            if y + th > h - pad:
                break
            full = w - 2 * pad
            length = full if i < n - 1 else int(full * rng.uniform(0.35, 0.85))
            if kind == "auteurs":
                length = int(full * rng.uniform(0.4, 0.6))
            hit = kind in extracted or (kind == "texte" and rng.random() < 0.05)
            if hit:
                d.rounded_rectangle((pad - th // 2, y - th // 2, pad + length + th // 2, y + th + th // 2),
                                    radius=th // 2, fill=theme["highlight"])
                d.rectangle((pad - th, y - th // 2, pad - th + max(2, th // 3), y + th + th // 2), fill=accent)
            d.rounded_rectangle((pad, y, pad + length, y + th), radius=th // 2,
                                fill=accent if kind == "titre" and hit else line)
            y += int(th * (2.4 if kind != "texte" else 2.1))
        y += int(h * 0.028)
    return img


def composition(width, height, seed, theme, density=1.0):
    rng = random.Random(seed)
    W, H = width * SS, height * SS
    canvas = Image.new("RGB", (W, H), theme["bg"])
    d = ImageDraw.Draw(canvas)
    # Trame fine de fond (grille de catalogue).
    step = int(W / 48)
    for x in range(0, W, step):
        d.line((x, 0, x, H), fill=theme["grid"], width=SS)
    for y in range(0, H, step):
        d.line((0, y, W, y), fill=theme["grid"], width=SS)
    # Fil d'extraction : une courbe douce sous les pages, comme un flux de données.
    freq, phase, amp = rng.uniform(1.1, 1.7), rng.uniform(0, 6.28), rng.uniform(0.18, 0.28)
    pts = [(int(W * t), int(H * (0.5 + amp * math.sin(t * math.pi * freq + phase)))) for t in [i / 120 for i in range(121)]]
    d.line(pts, fill=theme["accent"], width=3 * SS, joint="curve")
    # Pages disposées en éventail, plus denses d'un côté (laisse de la place au texte de la page web).
    count = int(rng.randint(9, 13) * density)
    side = rng.choice([-1, 1])
    centers = []
    for i in range(count):
        pw = int(W * rng.uniform(0.11, 0.17))
        ph = int(pw * 1.414)
        page = rotated_page(pw, ph, rng, theme)
        angle = rng.uniform(-24, 24)
        page = page.rotate(angle, expand=True, resample=Image.BICUBIC)
        bias = rng.random() ** 0.7
        cx = int(W / 2 + side * (bias * W * 0.48) * (1 if rng.random() < 0.8 else -0.4))
        cy = int(rng.uniform(0.05, 0.95) * H)
        centers.append(cx)
        shadow = Image.new("RGBA", page.size, (0, 0, 0, 0))
        shadow.putalpha(page.split()[3].point(lambda a: int(a * theme["shadow"])))
        shadow = shadow.filter(ImageFilter.GaussianBlur(10 * SS))
        canvas.paste((0, 0, 0), (cx - page.width // 2 + 6 * SS, cy - page.height // 2 + 14 * SS), shadow)
        canvas.paste(page, (cx - page.width // 2, cy - page.height // 2), page)
    # Points de passage visibles côté vide : les « champs » sortis des documents.
    d = ImageDraw.Draw(canvas)
    heavy_right = sum(centers) / len(centers) > W / 2
    for t in ((0.08, 0.20, 0.32) if heavy_right else (0.68, 0.80, 0.92)):
        x, y = pts[int(t * 120)]
        r = 10 * SS
        d.ellipse((x - r, y - r, x + r, y + r), fill=theme["bg"], outline=theme["accent"], width=3 * SS)
    return canvas.resize((width, height), Image.LANCZOS)


LIGHT = {"bg": PAPER, "grid": mix(PAPER, TEAL_LIGHT, 0.9), "paper": (255, 255, 255, 255),
         "line": (*mix(SLATE, PAPER, 0.62), 255), "accent": (*TEAL, 255),
         "highlight": (*TEAL_LIGHT, 255), "shadow": 0.16}
DARK = {"bg": INK, "grid": mix(INK, TEAL_DARK, 0.35), "paper": (*mix(INK, (255, 255, 255), 0.07), 255),
        "line": (*mix(INK, (255, 255, 255), 0.28), 255), "accent": (*mix(TEAL, (255, 255, 255), 0.25), 255),
        "highlight": (*mix(INK, TEAL, 0.45), 255), "shadow": 0.55}
DEEP = dict(DARK, bg=TEAL_DARK, grid=mix(TEAL_DARK, TEAL, 0.35),
            paper=(*mix(TEAL_DARK, (255, 255, 255), 0.1), 255),
            highlight=(*mix(TEAL_DARK, OCHRE, 0.55), 255), accent=(*mix(OCHRE, (255, 255, 255), 0.35), 255))


def institution_theme(code):
    # Variation d'accent par institution, dans la palette du livrable 10.
    accents = {"ULB": TEAL, "UCL": TEAL_DARK, "ULG": RUST, "KUL": OCHRE, "UGE": TEAL, "VUB": RUST}
    acc = accents.get(code, TEAL)
    return dict(LIGHT, accent=(*acc, 255), highlight=(*mix(acc, (255, 255, 255), 0.82), 255))


def main():
    (OUT / "institutions").mkdir(parents=True, exist_ok=True)
    (OUT / "accueil").mkdir(parents=True, exist_ok=True)
    conf = json.loads((ROOT / "institutions.json").read_text(encoding="utf-8"))["institutions"]
    for inst in conf:
        img = composition(1600, 640, inst["code"], institution_theme(inst["code"]))
        img.save(OUT / "institutions" / f"{inst['code']}.jpg", quality=88, optimize=True)
        print("institution", inst["code"])
    for i, (theme, seed) in enumerate([(DARK, "hero-a"), (LIGHT, "hero-b"), (DEEP, "hero-c")], start=1):
        composition(2400, 1000, seed, theme, density=1.3).save(OUT / "accueil" / f"hero-{i}.jpg", quality=88, optimize=True)
        print("hero", i)
    # Couverture par défaut (document sans vignette) : une seule page, centrée.
    cover = Image.new("RGB", (900 * SS, 1200 * SS), TEAL_LIGHT)
    page = rotated_page(int(900 * SS * 0.72), int(900 * SS * 0.72 * 1.414), random.Random("cover"), LIGHT)
    cover.paste(page, ((cover.width - page.width) // 2, (cover.height - page.height) // 2), page)
    cover.resize((900, 1200), Image.LANCZOS).save(OUT / "couverture-defaut.jpg", quality=88)
    print("couverture par défaut")


if __name__ == "__main__":
    main()
