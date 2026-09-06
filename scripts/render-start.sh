#!/usr/bin/env sh
set -eu

if [ -n "${METAMIND_DATABASE_URL:-}" ]; then
	case "$METAMIND_DATABASE_URL" in
		postgres://*|postgresql://*)
			stripped="${METAMIND_DATABASE_URL#*://}"
			stripped="${stripped##*@}"
			host_port="${stripped%%/*}"
			database_name="${stripped#*/}"
			database_name="${database_name%%\?*}"
			case "$host_port" in
				*:*)
					host="${host_port%:*}"
					port="${host_port##*:}"
					;;
				*)
					host="$host_port"
					port="5432"
					;;
			esac
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
