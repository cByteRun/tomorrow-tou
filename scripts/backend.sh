#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
. ./scripts/api-env.sh
export PORT="${PORT:-$API_PORT}"
export DEMO_SEED="${DEMO_SEED:-true}"
export DEMO_PASSWORD="${DEMO_PASSWORD:-Tomorrow21!}"
exec ./mvnw -pl backend spring-boot:run "$@"
