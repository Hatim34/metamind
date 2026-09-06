#!/usr/bin/env sh
set -eu

if [ -n "${METAMIND_DATABASE_URL:-}" ]; then
	case "$METAMIND_DATABASE_URL" in
		postgres://*)
			database_url="${METAMIND_DATABASE_URL#postgres://}"
			without_credentials="${database_url#*@}"
			host_port="${without_credentials%%/*}"
			database_name="${without_credentials#*/}"
			database_name="${database_name%%\?*}"
			host="${host_port%:*}"
			port="${host_port##*:}"
			export METAMIND_DATABASE_URL="jdbc:postgresql://${host}:${port}/${database_name}"
			;;
		postgresql://*)
			database_url="${METAMIND_DATABASE_URL#postgresql://}"
			without_credentials="${database_url#*@}"
			host_port="${without_credentials%%/*}"
			database_name="${without_credentials#*/}"
			database_name="${database_name%%\?*}"
			host="${host_port%:*}"
			port="${host_port##*:}"
			export METAMIND_DATABASE_URL="jdbc:postgresql://${host}:${port}/${database_name}"
			;;
	esac
fi

if [ -n "${METAMIND_PUBLIC_URL:-}" ]; then
	case "$METAMIND_PUBLIC_URL" in
		http://*|https://*) ;;
		*) export METAMIND_PUBLIC_URL="https://${METAMIND_PUBLIC_URL}" ;;
	esac
fi

exec java ${JAVA_OPTS:-} -jar /app/app.jar
