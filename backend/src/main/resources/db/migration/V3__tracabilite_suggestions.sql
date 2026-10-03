ALTER TABLE suggestions_metadonnees
    ADD COLUMN preuve TEXT,
    ADD COLUMN signaux TEXT,
    ADD COLUMN segments TEXT,
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'LLM',
    ADD COLUMN url_source TEXT,
    ADD COLUMN distance_edition NUMERIC(4,3);

CREATE INDEX idx_suggestions_source ON suggestions_metadonnees(source);
