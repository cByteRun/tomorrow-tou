#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
. ./scripts/api-env.sh
./scripts/check-api.sh "$TOU_API_URL"
printf '%s\n' "JavaFX → $TOU_API_URL"
exec ./mvnw -pl frontend javafx:run "$@"
