#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/_common.sh"
cd "$KAFKA_DIR"
docker compose --env-file "$ROOT_ENV" -f ./compose.yml down -v
