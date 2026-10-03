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

ALTER TABLE metadonnees ADD COLUMN doi VARCHAR(255);
CREATE UNIQUE INDEX uk_metadonnees_doi ON metadonnees(doi) WHERE doi IS NOT NULL;
