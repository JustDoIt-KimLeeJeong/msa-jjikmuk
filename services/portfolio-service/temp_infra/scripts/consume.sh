
#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/_common.sh"
TOPIC=${1:-portfolio.events}
BOOT="localhost:${KAFKA_BROKER_PORT:-19092}"
docker exec -it pf-kafka bash -lc "kafka-console-consumer --bootstrap-server $BOOT --topic $TOPIC --from-beginning"
