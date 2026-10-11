#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
. ./scripts/api-env.sh
./scripts/docker-up.sh
./scripts/check-api.sh "$TOU_API_URL"
./mvnw -pl backend -Ppostgres-it verify
if [ -n "${DISPLAY:-}" ]; then
  exec ./mvnw -pl frontend -Pui-e2e verify
fi
if command -v xvfb-run >/dev/null 2>&1; then
  exec xvfb-run -a ./mvnw -pl frontend -Pui-e2e verify
fi
printf '%s\n' 'JavaFX E2E requires DISPLAY or xvfb-run. API tests completed; UI tests were not run.' >&2
exit 1
