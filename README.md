# Metamind

Metamind est une plateforme web destinée aux dépôts institutionnels académiques, aux bibliothèques universitaires et aux chercheurs.

## Structure

- `backend/` : API Spring Boot
- `frontend/` : application Angular
- `database/` : dump PostgreSQL de la base de donnees
- `docs/` : documentation technique du projet

## Documentation API

Le contrat OpenAPI est disponible dans `docs/openapi.yaml`. Il decrit les endpoints REST exposes en version `/api/v1`.
Une fois l'application lancee, le meme contrat est disponible en ligne sur `/api/v1/openapi.yaml`.
Les exports Open Data publics sont disponibles via `/api/v1/open-data/rss` et `/api/v1/open-data/publications/{id}/dublin-core`.

## Documentation de deploiement

- Guide : `docs/deploiement.md`
- Controle local : `scripts/controle-local.sh`
- Controle API : `scripts/controle-api.sh`
- Controle distant : `scripts/controle-deploiement.sh`
- URL de production : `https://metamind-app.duckdns.org`

## Securite API

L'authentification utilise un jeton JWT signe. Apres connexion, le frontend envoie le header `Authorization: Bearer <token>` sur les routes protegees : profil, credits, extraction, publications privees et administration.

Comptes de test :

- Bibliothecaire : `sarah@institution-a.example` / `558435`
- Administrateur : `admin@metamind.example` / `558435`

## Validation locale

```bash
cd backend
mvn test
cd ../frontend
npm run build
npm test -- --watch=false --browsers=ChromeHeadless
cd ..
sh scripts/controle-api.sh http://localhost:8080/api/v1
```

## Deploiement

Le deploiement prevu utilise Docker Compose avec PostgreSQL, le backend Spring Boot, le frontend Angular servi par Nginx et Caddy pour HTTPS.

Fichiers principaux :

- `docker-compose.yml`
- `Caddyfile`
- `.env.example`
- `backend/Dockerfile`
- `frontend/Dockerfile`
- `frontend/nginx.conf`

Commandes serveur :

```bash
cp .env.example .env
docker compose up -d --build
docker compose ps
```

La variable `METAMIND_DOMAIN` doit rester sur `metamind-app.duckdns.org` pour le deploiement final. Le fichier `.env` contient les secrets de production et ne doit pas etre versionne.

L'extraction de metadonnees utilise le fournisseur local par defaut. Gemini peut etre active en production avec `METAMIND_LLM_PROVIDER=gemini` et une cle `GEMINI_API_KEY` stockee uniquement dans `.env`.

## Deploiement Render

Render peut lancer Metamind avec un seul service Docker. Dans cette configuration, le backend Spring Boot sert aussi les fichiers Angular generes dans le jar. Le service utilise le port fourni par Render via `PORT`, une base PostgreSQL managée et une seule origine HTTPS.

Fichiers ajoutes pour Render :

- `Dockerfile`
- `render.yaml`
- `scripts/render-start.sh`

Sur l'offre gratuite Render, le stockage local du service web est ephemere : les fichiers importes peuvent etre perdus lors d'un redeploiement ou d'un redemarrage. Les donnees relationnelles restent dans PostgreSQL. Pour conserver durablement les fichiers importes, il faut passer le service web sur une offre compatible avec Render Disk, monter un disque persistant et définir `METAMIND_DOCUMENTS_DIR=/app/storage/documents`.
