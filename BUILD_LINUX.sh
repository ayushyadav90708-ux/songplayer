#!/usr/bin/env bash
set -e
if command -v gradle >/dev/null 2>&1; then
  gradle build
else
  echo 'Gradle was not found. Install Gradle 8.14.3 or import the project into IntelliJ IDEA.'
  exit 1
fi
