#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
exec ./mvnw -pl backend -Ppostgres-it verify "$@"
