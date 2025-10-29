#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KAFKA_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ROOT_DIR="$(cd "$KAFKA_DIR/../.." && pwd)"        # -> portfolio-service/
ROOT_ENV="$ROOT_DIR/.env"

# env 로드(선택): docker compose는 --env-file로 처리하므로 여기선 파싱용만 사용
if [ -f "$ROOT_ENV" ]; then
  # shellcheck disable=SC2046
  export $(grep -v '^#' "$ROOT_ENV" | xargs)
fi
