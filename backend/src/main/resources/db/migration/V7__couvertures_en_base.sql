-- Les couvertures etaient stockees sur le disque du conteneur.
-- Sur Render (plan gratuit, sans disque persistant) ce disque est recree a chaque
-- deploiement : la base gardait le chemin, le fichier avait disparu, et le catalogue
-- n'affichait plus aucune image. Les octets sont desormais conserves dans PostgreSQL.
--
-- Table distincte et non colonne sur documents : le catalogue liste les documents
-- sans jamais charger les images.
CREATE TABLE IF NOT EXISTS couvertures_documents (
    document_id BIGINT PRIMARY KEY REFERENCES documents(id) ON DELETE CASCADE,
    donnees BYTEA NOT NULL,
    type_mime VARCHAR(100) NOT NULL,
    taille_octets INTEGER NOT NULL,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
