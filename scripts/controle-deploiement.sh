#!/usr/bin/env sh
set -eu

BASE_URL="${1:-https://metamind-app.duckdns.org}"
BASE_URL="${BASE_URL%/}"
HTTP_OUTPUT="/tmp/metamind-http-check.out"

check_http() {
	path="$1"
	expected="$2"
	url="$BASE_URL$path"
	status="$(curl -sS -o "$HTTP_OUTPUT" -w "%{http_code}" "$url")"

	if [ "$status" != "$expected" ]; then
		echo "Echec $url : HTTP $status au lieu de $expected"
		exit 1
	fi

	echo "OK $url"
}

check_http "/" "200"
check_http "/api/v1/health" "200"
check_http "/api/v1/openapi.yaml" "200"
check_http "/api/v1/open-data/rss" "200"

echo "Controle de deploiement termine"
