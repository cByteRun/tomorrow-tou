#!/bin/sh
set -eu
BASE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
VERSION=3.9.11
if [ -z "${JAVA_HOME:-}" ] && ! command -v java >/dev/null 2>&1 && [ -x "$HOME/.cache/tou-java-tools/jdk-21/bin/java" ]; then
 JAVA_HOME="$HOME/.cache/tou-java-tools/jdk-21"
 export JAVA_HOME
fi
# Prefer an installed Maven; otherwise download the pinned distribution.
if command -v mvn >/dev/null 2>&1; then exec mvn "$@"; fi
for CANDIDATE in "$HOME/.cache/tou-java-tools/maven/bin/mvn" "$HOME/.m2/wrapper/dists/apache-maven-$VERSION"/*/bin/mvn; do
 if [ -x "$CANDIDATE" ]; then exec "$CANDIDATE" "$@"; fi
done
CACHE="${MAVEN_USER_HOME:-$HOME/.m2}/wrapper/dists/apache-maven-$VERSION/tou"
if [ ! -x "$CACHE/apache-maven-$VERSION/bin/mvn" ]; then
 mkdir -p "$CACHE"
 URL=$(sed -n 's/^distributionUrl=//p' "$BASE/.mvn/wrapper/maven-wrapper.properties")
 command -v curl >/dev/null 2>&1 || { echo "Install curl or Maven $VERSION" >&2; exit 1; }
 curl --fail --location --retry 3 "$URL" -o "$CACHE/maven.zip"
 unzip -q "$CACHE/maven.zip" -d "$CACHE"
 rm "$CACHE/maven.zip"
fi
exec "$CACHE/apache-maven-$VERSION/bin/mvn" "$@"
