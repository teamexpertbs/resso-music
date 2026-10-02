#!/usr/bin/env bash
set -euo pipefail

: "${ANDROID_HOME:=/opt/android-sdk}"
: "${JAVA_HOME:=/usr/lib/jvm/java-21-openjdk-amd64}"

if [[ ! -d "${ANDROID_HOME}/platforms/android-36" ]]; then
  echo "Android SDK platform android-36 is missing under ${ANDROID_HOME}" >&2
  exit 1
fi

if [[ ! -x "${GRADLE_HOME:-/opt/gradle}/bin/gradle" ]]; then
  echo "Gradle is missing at ${GRADLE_HOME:-/opt/gradle}" >&2
  exit 1
fi
