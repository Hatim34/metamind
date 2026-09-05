# Deploiement Metamind

## Objectif

L'application Metamind doit etre accessible en ligne a l'adresse suivante :

```text
https://metamind-app.duckdns.org
```

L'API REST publique et securisee utilise la base d'URL suivante :

```text
https://metamind-app.duckdns.org/api/v1
```

## Prerequis serveur

Le serveur de production doit disposer de ces elements :

- serveur Linux accessible par SSH ;
- Docker et Docker Compose installes ;
- ports 80 et 443 ouverts ;
- domaine DuckDNS `metamind-app.duckdns.org` pointe vers l'adresse IP du serveur ;
- branche `main` du depot GitHub a jour.

## Fichiers utilises

- `docker-compose.yml` : lance PostgreSQL, le backend, le frontend et Caddy ;
- `Caddyfile` : expose l'application en HTTPS ;
- `.env.example` : modele de configuration sans secret ;
- `backend/Dockerfile` : construit l'API Spring Boot ;
- `frontend/Dockerfile` : construit l'interface Angular ;
- `database/ASSAL-Hatim-14-Dump_base_de_donnees.sql` : dump PostgreSQL avec structure et donnees.

## Configuration

Le fichier `.env` ne doit pas etre versionne. Il est cree sur le serveur a partir du modele :

```bash
cp .env.example .env
```

Variables principales :

```text
METAMIND_DOMAIN=metamind-app.duckdns.org
METAMIND_PUBLIC_URL=https://metamind-app.duckdns.org
POSTGRES_DB=metamind
POSTGRES_USER=metamind
POSTGRES_PASSWORD=valeur_secrete
METAMIND_JWT_SECRET=valeur_secrete_longue
METAMIND_LLM_PROVIDER=local
GEMINI_API_KEY=
GEMINI_MODEL=gemini-2.5-flash-lite
STRIPE_SECRET_KEY=
STRIPE_WEBHOOK_SECRET=
```

Pour activer Gemini sur le serveur, `METAMIND_LLM_PROVIDER` passe a `gemini` et `GEMINI_API_KEY` recoit une cle API conservee uniquement dans le fichier `.env`.

## Mise en ligne

Commandes a executer sur le serveur :

```bash
git clone https://github.com/Hatim34/metamind.git
cd metamind
cp .env.example .env
docker compose up -d --build
docker compose ps
```

Caddy demande automatiquement un certificat TLS Let's Encrypt pour le domaine configure. Le cadenas HTTPS est donc gere par le serveur au moment du deploiement. Le fichier `Caddyfile` ajoute aussi les en-tetes HTTP de securite : HSTS, anti-sniffing, refus d'affichage en frame, politique de referer et desactivation des permissions navigateur non utilisees.

## Verification

Controle local avant publication :

```bash
sh scripts/controle-local.sh
```

Controle distant apres publication :

```bash
sh scripts/controle-deploiement.sh https://metamind-app.duckdns.org
```

Controle du parcours API REST :

```bash
sh scripts/controle-api.sh https://metamind-app.duckdns.org/api/v1
```

Verifications manuelles utiles :

```bash
curl -i https://metamind-app.duckdns.org/api/v1/health
curl -i https://metamind-app.duckdns.org/api/v1/openapi.yaml
curl -i https://metamind-app.duckdns.org/api/v1/open-data/rss
```

## Comptes de test

Compte bibliothecaire :

```text
Email : sarah@institution-a.example
Mot de passe : 558435
```

Compte administrateur :

```text
Email : admin@metamind.example
Mot de passe : 558435
```

## Elements a remettre pour la partie deploiement

URL de l'application :

```text
https://metamind-app.duckdns.org
```

Donnees de connexion :

```text
Bibliothecaire : sarah@institution-a.example / 558435
Administrateur : admin@metamind.example / 558435
```

## Exploitation

Consulter les journaux :

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f caddy
```

Redemarrer les services :

```bash
docker compose restart
```

Arreter l'application :

```bash
docker compose down
```
