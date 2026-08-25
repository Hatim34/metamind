#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
COMPOSE_CHECK_FILE="/tmp/metamind-compose-check.yml"

echo "Controle backend"
cd "$ROOT_DIR/backend"
mvn test

echo "Controle frontend"
cd "$ROOT_DIR/frontend"
npm test -- --watch=false --browsers=ChromeHeadless
npm run build -- --progress=false

echo "Controle Docker Compose"
cd "$ROOT_DIR"
docker compose --env-file .env.example config > "$COMPOSE_CHECK_FILE"

echo "Controle local termine"
