-- Compatibilite des bases creees avant Flyway : elles limitaient encore les
-- mouvements a ACHAT et CONSOMMATION alors que l'application utilise aussi
-- remboursement, offre de bienvenue et ajustement administratif.
ALTER TABLE mouvements_credits
    DROP CONSTRAINT IF EXISTS mouvements_credits_type_check;

ALTER TABLE mouvements_credits
    ADD CONSTRAINT mouvements_credits_type_check
    CHECK (type IN (
        'ACHAT',
        'CONSOMMATION',
        'REMBOURSEMENT',
        'AJUSTEMENT_ADMIN',
        'OFFRE_BIENVENUE'
    ));
