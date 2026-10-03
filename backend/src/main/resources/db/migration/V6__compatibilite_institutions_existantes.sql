-- Met a niveau les bases creees avant l'introduction de Flyway.
-- Ces colonnes existent dans V1 mais une base Render historique est baselinee en V2.
ALTER TABLE institutions ADD COLUMN IF NOT EXISTS actif BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE institutions ADD COLUMN IF NOT EXISTS credits_bienvenue_accordes BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE institutions ADD COLUMN IF NOT EXISTS achats_suspendus BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE institutions ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE institutions ADD COLUMN IF NOT EXISTS date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
