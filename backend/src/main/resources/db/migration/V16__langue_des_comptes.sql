-- Langue choisie a l'inscription : les emails envoyes au compte sont rediges dans cette langue.
ALTER TABLE users ADD COLUMN IF NOT EXISTS langue_preferee VARCHAR(2) NOT NULL DEFAULT 'fr';
