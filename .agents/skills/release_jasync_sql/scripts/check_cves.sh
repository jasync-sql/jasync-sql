#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
PROPS_FILE="${1:-${REPO_ROOT}/gradle.properties}"

if [[ ! -f "${PROPS_FILE}" ]]; then
  echo "ERROR: gradle.properties not found at ${PROPS_FILE}" >&2
  exit 1
fi

prop() {
  local key="$1"
  local val
  val="$(grep -E "^${key}=" "${PROPS_FILE}" | head -n 1 | cut -d'=' -f2- | tr -d '[:space:]')"
  if [[ -z "${val}" ]]; then
    echo "ERROR: Missing required property '${key}' in ${PROPS_FILE}" >&2
    exit 1
  fi
  printf '%s' "${val}"
}

NETTY_VERSION="$(prop NETTY_VERSION)"
KOTLIN_VERSION="$(prop KOTLIN_VERSION)"
KOTLIN_COROUTINES_VERSION="$(prop KOTLIN_COROUTINES_VERSION)"
SL4J_VERSION="$(prop SL4J_VERSION)"
KOTLIN_LOGGING_VERSION="$(prop KOTLIN_LOGGING_VERSION)"
JODA_VERSION="$(prop JODA_VERSION)"
JODA_CONVERT_VERSION="$(prop JODA_CONVERT_VERSION)"
SCRAM_CLIENT_VERSION="$(prop SCRAM_CLIENT_VERSION)"
THREETEN_EXTRA="$(prop THREETEN_EXTRA)"
JTS_VERSION="$(prop JTS_VERSION)"
R2DBC_SPI_VERSION="$(prop R2DBC_SPI_VERSION)"
REACTOR_CORE_VERSION="$(prop REACTOR_CORE_VERSION)"
REACTOR_KOTLIN_EXTENSION="$(prop REACTOR_KOTLIN_EXTENSION)"

AFFECTS="$(IFS=,; echo "\
io.netty:netty-common@${NETTY_VERSION},\
io.netty:netty-buffer@${NETTY_VERSION},\
io.netty:netty-resolver@${NETTY_VERSION},\
io.netty:netty-codec@${NETTY_VERSION},\
io.netty:netty-transport@${NETTY_VERSION},\
io.netty:netty-handler@${NETTY_VERSION},\
io.netty:netty-transport-native-unix-common@${NETTY_VERSION},\
org.jetbrains.kotlin:kotlin-stdlib@${KOTLIN_VERSION},\
org.jetbrains.kotlin:kotlin-stdlib-jdk8@${KOTLIN_VERSION},\
org.jetbrains.kotlin:kotlin-reflect@${KOTLIN_VERSION},\
org.jetbrains.kotlinx:kotlinx-coroutines-core@${KOTLIN_COROUTINES_VERSION},\
org.jetbrains.kotlinx:kotlinx-coroutines-jdk8@${KOTLIN_COROUTINES_VERSION},\
org.slf4j:slf4j-api@${SL4J_VERSION},\
io.github.oshai:kotlin-logging-jvm@${KOTLIN_LOGGING_VERSION},\
joda-time:joda-time@${JODA_VERSION},\
org.joda:joda-convert@${JODA_CONVERT_VERSION},\
com.ongres.scram:client@${SCRAM_CLIENT_VERSION},\
org.threeten:threeten-extra@${THREETEN_EXTRA},\
org.locationtech.jts:jts-core@${JTS_VERSION},\
io.r2dbc:r2dbc-spi@${R2DBC_SPI_VERSION},\
io.projectreactor:reactor-core@${REACTOR_CORE_VERSION},\
io.projectreactor.kotlin:reactor-kotlin-extensions@${REACTOR_KOTLIN_EXTENSION}")"

RESULT="$(gh api "advisories?ecosystem=maven&per_page=100&affects=${AFFECTS}" \
  --jq 'map({ghsa_id, cve_id, severity, summary, vulnerabilities: [.vulnerabilities[] | {package: .package.name, vulnerable_version_range, first_patched_version}]})')"

if [[ "${RESULT}" != "[]" ]]; then
  echo "ERROR: Open security advisories found in published runtime dependencies:" >&2
  echo "${RESULT}" >&2
  exit 1
fi

echo "OK: 0 open security advisories across published runtime dependencies."
