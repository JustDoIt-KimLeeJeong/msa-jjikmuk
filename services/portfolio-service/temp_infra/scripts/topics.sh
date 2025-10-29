#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/_common.sh"
cd "$KAFKA_DIR"

BOOT="localhost:${KAFKA_BROKER_PORT:-19092}"

jq -c '.[]' topics.json | while read -r t; do
  NAME=$(echo "$t" | jq -r '.name')
  PARTS=$(echo "$t" | jq -r '.partitions')
  REPL=$(echo "$t" | jq -r '.replication')
  CONFIGS=$(echo "$t" | jq -r '.configs // {}')
  CFG=""
  for row in $(echo "$CONFIGS" | jq -r 'to_entries[]? | @base64'); do
    _jq(){ echo "$row" | base64 -d | jq -r "$1"; }
    CFG="$CFG --config $(_jq .key)=$(_jq .value)"
  done
  echo ">> create topic: $NAME"
  docker exec -it pf-kafka kafka-topics \
    --bootstrap-server "$BOOT" \
    --create --if-not-exists \
    --topic "$NAME" \
    --partitions "$PARTS" \
    --replication-factor "$REPL" \
    $CFG
done

docker exec -it pf-kafka kafka-topics --bootstrap-server "$BOOT" --list
