#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
python3 scripts/tests/test_launch.py
exec ./mvnw test "$@"
