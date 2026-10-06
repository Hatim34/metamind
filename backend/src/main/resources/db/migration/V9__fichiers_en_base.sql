-- Les PDF importes etaient stockes sur le disque du conteneur, recree a chaque
-- deploiement sur Render : la base gardait le chemin, le fichier avait disparu.
-- La page de validation ne pouvait plus afficher le document et le telechargement
-- public renvoyait une erreur. Les octets sont desormais conserves dans PostgreSQL,
-- comme les couvertures (V7).
--
-- Table distincte : lister les documents ne charge jamais les fichiers.
CREATE TABLE IF NOT EXISTS fichiers_documents (
    document_id BIGINT PRIMARY KEY REFERENCES documents(id) ON DELETE CASCADE,
    donnees BYTEA NOT NULL,
    type_mime VARCHAR(100) NOT NULL,
    taille_octets INTEGER NOT NULL,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
