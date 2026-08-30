#!/usr/bin/env bash
set -Eeuo pipefail

for command in docker curl; do
  if ! command -v "${command}" >/dev/null 2>&1; then
    echo "Required command is missing: ${command}" >&2
    exit 1
  fi
done

project_suffix="${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-1}-$$"
project_name="kivo-smoke-$(printf '%s' "${project_suffix}" | tr '[:upper:]_' '[:lower:]-' | tr -cd 'a-z0-9-')"

export KIVO_IMAGE="${KIVO_IMAGE:-kivo-backend:smoke}"
export KIVO_HTTPS_PORT="${KIVO_HTTPS_PORT:-8443}"
export KIVO_PUBLIC_HOST="${KIVO_PUBLIC_HOST:-localhost}"
export KIVO_PUBLIC_BASE_URL="${KIVO_PUBLIC_BASE_URL:-https://localhost:${KIVO_HTTPS_PORT}}"
export KIVO_DB_NAME="${KIVO_DB_NAME:-kivo}"
export KIVO_DB_USERNAME="${KIVO_DB_USERNAME:-kivo}"
export KIVO_ADMIN_USERNAME="${KIVO_ADMIN_USERNAME:-smoke-admin}"
export KIVO_CORS_ALLOWED_ORIGINS="${KIVO_CORS_ALLOWED_ORIGINS:-${KIVO_PUBLIC_BASE_URL}}"
export KIVO_MINIMUM_ACTIVE_GRAPHS="${KIVO_MINIMUM_ACTIVE_GRAPHS:-0}"

secret_directory=""
ensure_secret_directory() {
  if [[ -z "${secret_directory}" ]]; then
    secret_directory="$(mktemp -d)"
  fi
}

compose_path() {
  if command -v cygpath >/dev/null 2>&1; then
    cygpath --windows "$1"
  else
    printf '%s\n' "$1"
  fi
}

if [[ -z "${KIVO_DB_PASSWORD_FILE:-}" ]]; then
  ensure_secret_directory
  secret_file="${secret_directory}/kivo_db_password"
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 32 >"${secret_file}"
  else
    printf '%s%s\n' \
      "$(tr -d '-' </proc/sys/kernel/random/uuid)" \
      "$(tr -d '-' </proc/sys/kernel/random/uuid)" >"${secret_file}"
  fi
  # Compose mounts file secrets with their host permissions. The private mktemp
  # directory remains 0700, while app UID 10001 needs read access to this file.
  chmod 0644 "${secret_file}"
  KIVO_DB_PASSWORD_FILE="$(compose_path "${secret_file}")"
  export KIVO_DB_PASSWORD_FILE
fi

if [[ -z "${KIVO_ADMIN_PASSWORD_HASH_FILE:-}" ]]; then
  ensure_secret_directory
  admin_hash_file="${secret_directory}/kivo_admin_password_hash"
  printf '%s\n' '$2a$10$jSFJWQ5.f1dUERLQRaGrAeV4lUa.Ki18LHtMLIrF2CQ/9F7nppvim' >"${admin_hash_file}"
  chmod 0644 "${admin_hash_file}"
  KIVO_ADMIN_PASSWORD_HASH_FILE="$(compose_path "${admin_hash_file}")"
  export KIVO_ADMIN_PASSWORD_HASH_FILE
fi

compose=(docker compose --project-name "${project_name}" --file compose.production.yaml)

cleanup() {
  result=$?
  trap - EXIT
  if (( result != 0 )); then
    "${compose[@]}" logs --no-color >&2 || true
  fi
  "${compose[@]}" down --volumes --remove-orphans >/dev/null 2>&1 || true
  if [[ -n "${secret_directory}" ]]; then
    rm -rf -- "${secret_directory}"
  fi
  exit "${result}"
}
trap cleanup EXIT

"${compose[@]}" config --quiet
"${compose[@]}" up --detach --wait --wait-timeout 240

app_container="$(${compose[@]} ps --quiet app)"
configured_user="$(docker inspect --format '{{.Config.User}}' "${app_container}")"
if [[ "${configured_user}" != "10001:10001" ]]; then
  echo "Application container is not configured with the expected non-root user: ${configured_user}" >&2
  exit 1
fi

app_health="$(docker inspect --format '{{.State.Health.Status}}' "${app_container}")"
if [[ "${app_health}" != "healthy" ]]; then
  echo "Application container health is ${app_health}" >&2
  exit 1
fi

readiness="$(curl --fail --silent --show-error --insecure \
  "https://localhost:${KIVO_HTTPS_PORT}/actuator/health/readiness")"
if [[ "${readiness}" != *'"status":"UP"'* ]]; then
  echo "Unexpected readiness response: ${readiness}" >&2
  exit 1
fi

expected_version="$({
  for migration in src/main/resources/db/migration/V*__*.sql; do
    basename "${migration}" | sed -E 's/^V([^_]+)__.*/\1/'
  done
} | sort --version-sort | tail -n 1)"

actual_version="$(${compose[@]} exec --no-TTY postgres \
  psql --username "${KIVO_DB_USERNAME}" --dbname "${KIVO_DB_NAME}" \
  --tuples-only --no-align \
  --command "select version from flyway_schema_history where success order by installed_rank desc limit 1")"

if [[ "${actual_version}" != "${expected_version}" ]]; then
  echo "Flyway version mismatch: expected ${expected_version}, got ${actual_version}" >&2
  exit 1
fi

failed_migrations="$(${compose[@]} exec --no-TTY postgres \
  psql --username "${KIVO_DB_USERNAME}" --dbname "${KIVO_DB_NAME}" \
  --tuples-only --no-align \
  --command "select count(*) from flyway_schema_history where not success")"

if [[ "${failed_migrations}" != "0" ]]; then
  echo "Flyway history contains ${failed_migrations} failed migration(s)" >&2
  exit 1
fi

echo "container-smoke: PASSED (non-root app, HTTPS readiness, Flyway ${actual_version})"
