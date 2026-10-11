#!/bin/sh
set -eu
api_url="${1:?Укажите URL API}"
if ! response="$(curl --connect-timeout 2 --max-time 5 -fsS "${api_url%/}/api/health")"; then
  printf '%s\n' "TomorrowTOU API недоступен: $api_url. Сначала успешно выполните ./scripts/docker-up.sh." >&2
  exit 1
fi
check_status=0
printf '%s' "$response" | python3 -c 'import json,sys; b=json.load(sys.stdin); sys.exit(1 if b.get("application")!="tomorrow-tou" else 0 if b.get("status")=="UP" else 2)' 2>/dev/null || check_status=$?
case "$check_status" in
  0) ;;
  2) printf '%s\n' "TomorrowTOU API ещё запускается: $api_url. Дождитесь успешного завершения docker-up.sh." >&2; exit 1 ;;
  *) printf '%s\n' "По адресу $api_url отвечает другой или старый API. Пересоберите TomorrowTOU: ./scripts/docker-up.sh. При конфликте задайте одинаковый API_PORT при запуске Docker и frontend." >&2; exit 1 ;;
esac
