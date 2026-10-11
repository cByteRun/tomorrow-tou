#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
. ./scripts/api-env.sh
if ! docker compose up --build -d "$@"; then
  printf '%s\n' "Docker не запущен успешно. JavaFX пока не запускайте. Если порт занят, укажите свободный порт в .env: API_PORT=18081, затем повторите docker-up.sh и frontend.sh. Существующие сервисы и данные автоматически не удаляются." >&2
  exit 1
fi
attempt=0
until ./scripts/check-api.sh "http://localhost:$API_PORT" >/dev/null 2>&1; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 60 ]; then
    docker compose logs --tail=80 backend
    ./scripts/check-api.sh "http://localhost:$API_PORT"
    exit 1
  fi
  sleep 2
done
printf '%s\n' "TomorrowTOU API готов: http://localhost:$API_PORT/api" 'Запуск JavaFX: ./scripts/frontend.sh'
