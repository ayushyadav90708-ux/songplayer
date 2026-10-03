#!/usr/bin/env sh
set -eu
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
GRADLE_VERSION=8.10.2
CACHE="${HOME:-.}/.gradle/note-block-songs-bootstrap/$GRADLE_VERSION"
DIST="$CACHE/gradle-$GRADLE_VERSION/bin/gradle"
if [ ! -x "$DIST" ]; then
  mkdir -p "$CACHE"
  ARCHIVE="$CACHE/gradle.zip"
  if command -v curl >/dev/null 2>&1; then curl -L --fail --retry 2 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ARCHIVE"; else wget -O "$ARCHIVE" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"; fi
  unzip -q -o "$ARCHIVE" -d "$CACHE"
fi
exec "$DIST" "$@"
