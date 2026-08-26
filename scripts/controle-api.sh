#!/usr/bin/env sh
set -eu

BASE_URL="${1:-http://localhost:8080/api/v1}"
BASE_URL="${BASE_URL%/}"
TMP_DIR="$(mktemp -d)"
LOGIN_RESPONSE="$TMP_DIR/login.json"
PUBLICATION_RESPONSE="$TMP_DIR/publication.json"

cleanup() {
	rm -rf "$TMP_DIR"
}
trap cleanup EXIT

extract_json_string() {
	file="$1"
	field="$2"
	sed -n "s/.*\"$field\"[[:space:]]*:[[:space:]]*\"\\([^\"]*\\)\".*/\\1/p" "$file" | head -n 1
}

extract_json_number() {
	file="$1"
	field="$2"
	sed -n "s/.*\"$field\"[[:space:]]*:[[:space:]]*\\([0-9][0-9]*\\).*/\\1/p" "$file" | head -n 1
}

check_status() {
	method="$1"
	path="$2"
	expected="$3"
	output="$4"
	shift 4
	status="$(curl -sS -o "$output" -w "%{http_code}" -X "$method" "$BASE_URL$path" "$@")"
	if [ "$status" != "$expected" ]; then
		echo "Echec $method $path : HTTP $status au lieu de $expected"
		cat "$output"
		exit 1
	fi
	echo "OK $method $path"
}

echo "Controle API Metamind sur $BASE_URL"

check_status GET "/health" 200 "$TMP_DIR/health.json"
check_status GET "/openapi.yaml" 200 "$TMP_DIR/openapi.yaml"
check_status GET "/publications" 200 "$TMP_DIR/publications.json"

check_status POST "/auth/login" 200 "$LOGIN_RESPONSE" \
	-H "Content-Type: application/json" \
	-d '{"email":"sarah@institution-a.example","password":"558435"}'

TOKEN="$(extract_json_string "$LOGIN_RESPONSE" token)"
if [ -z "$TOKEN" ]; then
	echo "Echec authentification : jeton absent"
	cat "$LOGIN_RESPONSE"
	exit 1
fi

check_status POST "/publications" 201 "$PUBLICATION_RESPONSE" \
	-H "Content-Type: application/json" \
	-H "Authorization: Bearer $TOKEN" \
	-d '{"title":"Controle automatise des metadonnees","author":"Mina Laurent","institution":"Institution A","year":2026,"visibility":"INSTITUTION","keywords":["controle","api"]}'

PUBLICATION_ID="$(extract_json_number "$PUBLICATION_RESPONSE" id)"
if [ -z "$PUBLICATION_ID" ]; then
	echo "Echec creation publication : identifiant absent"
	cat "$PUBLICATION_RESPONSE"
	exit 1
fi

check_status PUT "/publications/$PUBLICATION_ID/status" 200 "$TMP_DIR/status.json" \
	-H "Content-Type: application/json" \
	-H "Authorization: Bearer $TOKEN" \
	-d '{"status":"PUBLIE"}'

check_status DELETE "/publications/$PUBLICATION_ID" 200 "$TMP_DIR/delete.json" \
	-H "Authorization: Bearer $TOKEN"

echo "Controle API termine"
