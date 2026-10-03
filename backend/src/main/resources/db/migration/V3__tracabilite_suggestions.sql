-- Idempotent : sur une base historique, Hibernate (ddl-auto=update) a deja pu creer
-- ces colonnes nullables. Flyway doit pouvoir rejouer sans tomber sur "column already exists".
ALTER TABLE suggestions_metadonnees
    ADD COLUMN IF NOT EXISTS preuve TEXT,
    ADD COLUMN IF NOT EXISTS signaux TEXT,
    ADD COLUMN IF NOT EXISTS segments TEXT,
    ADD COLUMN IF NOT EXISTS source VARCHAR(20) NOT NULL DEFAULT 'LLM',
    ADD COLUMN IF NOT EXISTS url_source TEXT,
    ADD COLUMN IF NOT EXISTS distance_edition NUMERIC(4,3);

CREATE INDEX IF NOT EXISTS idx_suggestions_source ON suggestions_metadonnees(source);
