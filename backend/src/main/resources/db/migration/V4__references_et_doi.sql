INSERT INTO langues (code, libelle) VALUES
    ('fr', 'Francais'),
    ('nl', 'Neerlandais'),
    ('en', 'Anglais')
ON CONFLICT (code) DO NOTHING;

INSERT INTO types_documents (code, libelle) VALUES
    ('article', 'Article'),
    ('these', 'These'),
    ('memoire', 'Memoire'),
    ('rapport', 'Rapport'),
    ('chapitre', 'Chapitre'),
    ('communication', 'Communication'),
    ('preprint', 'Prepublication'),
    ('autre', 'Autre')
ON CONFLICT (code) DO NOTHING;

-- Idempotent : Hibernate a deja pu ajouter cette colonne nullable sur une base historique.
ALTER TABLE metadonnees ADD COLUMN IF NOT EXISTS doi VARCHAR(255);
CREATE UNIQUE INDEX IF NOT EXISTS uk_metadonnees_doi ON metadonnees(doi) WHERE doi IS NOT NULL;
