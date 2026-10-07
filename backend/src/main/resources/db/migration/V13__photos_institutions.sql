-- Photo d'illustration de chaque institution, geree par l'administrateur.
-- Elle etait figee dans le code du site : une nouvelle institution n'en avait pas et
-- l'administrateur ne pouvait pas la changer. Le credit est obligatoire pour les licences
-- Creative Commons (auteur, licence, source).
CREATE TABLE IF NOT EXISTS photos_institutions (
    institution_id BIGINT PRIMARY KEY REFERENCES institutions(id) ON DELETE CASCADE,
    donnees BYTEA NOT NULL,
    type_mime VARCHAR(100) NOT NULL,
    taille_octets INTEGER NOT NULL,
    credit VARCHAR(300),
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
