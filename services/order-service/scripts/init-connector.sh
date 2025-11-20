#!/bin/bash

set -e

echo "=== Order Service Connector Auto Registration ==="
echo ""

# 설정 파일 경로
CONNECTOR_CONFIG="/config/order-outbox-connector.json"

if [ ! -f "$CONNECTOR_CONFIG" ]; then
  echo "ERROR: Config file not found: $CONNECTOR_CONFIG"
  exit 1
fi

echo "✓ Config file found"
echo ""

# Kafka Connect 준비 대기
echo "Waiting for Kafka Connect..."
for i in {1..60}; do
  if curl -s http://kafka-connect:8083/ > /dev/null 2>&1; then
    echo "✓ Kafka Connect ready"
    break
  fi

  if [ $i -eq 60 ]; then
    echo "ERROR: Kafka Connect timeout"
    exit 1
  fi

  sleep 1
done

echo ""

# Connector 존재 확인
CONNECTOR_NAME=$(jq -r '.name' "$CONNECTOR_CONFIG")
echo "Checking connector: $CONNECTOR_NAME"

if curl -s http://kafka-connect:8083/connectors 2>/dev/null | grep -q "\"$CONNECTOR_NAME\""; then
  echo "✓ Already exists"
  echo ""
  echo "Current status:"
  curl -s http://kafka-connect:8083/connectors/$CONNECTOR_NAME/status 2>/dev/null | jq '.' || echo "Failed to get status"
  exit 0
fi

echo "✗ Not found. Creating..."
echo ""

# 환경변수 치환
CONFIG_JSON=$(cat "$CONNECTOR_CONFIG" | \
  sed "s/\${POSTGRES_USER}/$POSTGRES_USER/g" | \
  sed "s/\${POSTGRES_PASSWORD}/$POSTGRES_PASSWORD/g" | \
  sed "s/\${POSTGRES_DB}/$POSTGRES_DB/g")

# Connector 생성
echo "Creating connector..."
RESPONSE=$(curl -s -X POST http://kafka-connect:8083/connectors \
  -H "Content-Type: application/json" \
  -d "$CONFIG_JSON" 2>/dev/null)

# 응답 확인
if echo "$RESPONSE" | jq -e '.name' > /dev/null 2>&1; then
  echo "✓ Created successfully"
else
  echo "ERROR: Creation failed"
  echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
  exit 1
fi

# 상태 확인 (최대 30초 대기)
echo ""
echo "Waiting for connector to start..."
for i in {1..30}; do
  sleep 1

  STATUS=$(curl -s http://kafka-connect:8083/connectors/$CONNECTOR_NAME/status 2>/dev/null)

  if [ -z "$STATUS" ]; then
    echo "  Attempt $i/30: No response yet..."
    continue
  fi

  CONNECTOR_STATE=$(echo "$STATUS" | jq -r '.connector.state' 2>/dev/null)
  TASK_STATE=$(echo "$STATUS" | jq -r '.tasks[0].state' 2>/dev/null)

  echo "  Attempt $i/30: Connector=$CONNECTOR_STATE, Task=$TASK_STATE"

  if [ "$TASK_STATE" = "RUNNING" ]; then
    echo ""
    echo "✅ Connector is RUNNING!"
    echo ""
    echo "Final status:"
    echo "$STATUS" | jq '.'
    exit 0
  fi

  if [ "$TASK_STATE" = "FAILED" ]; then
    echo ""
    echo "❌ Connector FAILED!"
    echo ""
    echo "Status:"
    echo "$STATUS" | jq '.'
    exit 1
  fi
done

# 30초 후에도 RUNNING 안되면
echo ""
echo "⚠️  Connector did not reach RUNNING state within 30 seconds"
echo ""
echo "Current status:"
curl -s http://kafka-connect:8083/connectors/$CONNECTOR_NAME/status 2>/dev/null | jq '.' || echo "Failed to get status"
echo ""
echo "Check logs: docker-compose logs kafka-connect"
exit 1