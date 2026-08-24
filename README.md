# Metamind

Metamind est une plateforme web destinée aux dépôts institutionnels académiques, aux bibliothèques universitaires et aux chercheurs.

## Structure

- `backend/` : API Spring Boot
- `frontend/` : application Angular
- `database/` : scripts et dumps de base de données
- `docs/` : documentation technique et livrables du projet

## Documentation API

Le contrat OpenAPI est disponible dans `docs/openapi.yaml`. Il decrit les endpoints REST exposes en version `/api/v1`.

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
