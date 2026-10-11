#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
exec ./mvnw -pl frontend clean compile "$@"
