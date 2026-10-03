#!/usr/bin/env python3
"""
Étape 3 : rend l'historique crédible. Tout ce que load.py a créé porte la date du jour ;
ce script génère un SQL qui étale l'activité sur les N derniers mois :
  - imports en semaine, heures de bureau, volume croissant (adoption progressive) ;
  - extraction quelques minutes après l'import, validation 2 h à 6 jours plus tard ;
  - mouvements de crédits et journaux d'audit alignés sur ces dates ;
  - institutions créées quelques semaines avant leur premier import.

Usage :
  python backdate.py --months 8 > data/backdate.sql
  docker compose exec -T db psql -U metamind -d metamind < data/backdate.sql
"""
import argparse
import json
import random
from datetime import datetime, timedelta
from pathlib import Path

DATA = Path(__file__).parent / "data"


def working_time(rng, start, end):
    """Date aléatoire, pondérée vers la fin de la période, en semaine entre 8 h et 18 h."""
    span = (end - start).total_seconds()
    while True:
        t = start + timedelta(seconds=span * (rng.random() ** 0.6))  # plus d'activité récente
        if t.weekday() < 5:
            return t.replace(hour=rng.randint(8, 17), minute=rng.randint(0, 59), second=rng.randint(0, 59))


def next_working(t, rng, min_h, max_h):
    t = t + timedelta(hours=rng.uniform(min_h, max_h))
    while t.weekday() >= 5 or not 8 <= t.hour < 18:
        t = (t + timedelta(days=1)).replace(hour=rng.randint(8, 11), minute=rng.randint(0, 59))
    return t


def ts(t):
    return t.strftime("'%Y-%m-%d %H:%M:%S'")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--months", type=int, default=8)
    ap.add_argument("--seed", type=int, default=7)
    args = ap.parse_args()

    rng = random.Random(args.seed)
    result = json.loads((DATA / "load_result.json").read_text(encoding="utf-8"))
    now = datetime.now().replace(microsecond=0)
    start = now - timedelta(days=30 * args.months)

    out = ["-- Généré par backdate.py : étale le jeu de démonstration dans le temps.", "BEGIN;"]
    first_import = {}
    for d in result["documents"]:
        doc_id = d["document_id"]
        t_import = working_time(rng, start + timedelta(days=21), now - timedelta(hours=2))
        first_import[d["institution"]] = min(first_import.get(d["institution"], t_import), t_import)
        t_gen = t_import + timedelta(minutes=rng.randint(2, 45))
        t_end = t_gen + timedelta(seconds=rng.randint(4, 40))
        t_val = next_working(t_end, rng, 2, 140)
        if t_val > now:
            t_val = now - timedelta(minutes=rng.randint(10, 300))
        out.append(f"-- document {doc_id} ({d['etat']})")
        out.append(f"UPDATE enrichissements SET date_debut = {ts(t_gen)}, date_fin = {ts(t_end)} WHERE document_id = {doc_id};")
        out.append(f"UPDATE metadonnees SET date_generation = {ts(t_gen)} WHERE document_id = {doc_id};")
        if d["etat"] in ("PUBLIE", "REJETE"):
            out.append(f"UPDATE metadonnees SET date_validation = {ts(t_val)} WHERE document_id = {doc_id} AND date_validation IS NOT NULL;")
        out.append(
            "UPDATE mouvements_credits SET date_mouvement = " + ts(t_end) +
            f" WHERE enrichissement_id IN (SELECT id FROM enrichissements WHERE document_id = {doc_id});")
        # Journaux liés au document : import puis validation.
        out.append(
            f"UPDATE logs_audit SET date_creation = CASE WHEN action ILIKE '%VALID%' OR action ILIKE '%REJET%' "
            f"OR action ILIKE '%METADON%' THEN {ts(t_val)} ELSE {ts(t_import)} END "
            f"WHERE entite_id = {doc_id} AND type_entite ILIKE ANY (ARRAY['%document%', '%metadon%', '%publication%']);")

    for code, info in result["institutions"].items():
        created = first_import.get(code, now) - timedelta(days=rng.randint(10, 30))
        out.append(f"UPDATE institutions SET date_creation = {ts(created)} WHERE id = {info['id']};")
        out.append(
            f"UPDATE mouvements_credits SET date_mouvement = {ts(created + timedelta(days=1, hours=2))} "
            f"WHERE institution_id = {info['id']} AND enrichissement_id IS NULL AND description ILIKE '%dotation%';")

    # Journaux restants créés pendant le chargement (connexions, comptes) : répartis sur la période.
    out.append(
        f"UPDATE logs_audit SET date_creation = NOW() - (random() * INTERVAL '{30 * args.months} days') "
        "WHERE date_creation > NOW() - INTERVAL '1 day';")
    out.append("COMMIT;")
    print("\n".join(out))


if __name__ == "__main__":
    main()
