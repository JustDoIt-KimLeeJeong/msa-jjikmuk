#!/bin/bash
set -e

echo "=== Debezium Connector 등록 ==="

# .env 파일 로드
if [ -f .env ]; then
    echo "✅ .env 파일 로드 중..."
    export $(grep -v '^#' .env | xargs)
else
    echo "❌ .env 파일을 찾을 수 없습니다."
    exit 1
fi

# 필수 환경변수 확인
if [ -z "$POSTGRES_USER" ] || [ -z "$POSTGRES_PASSWORD" ] || [ -z "$POSTGRES_DB" ]; then
    echo "❌ 필수 환경변수가 설정되지 않았습니다."
    echo "POSTGRES_USER: $POSTGRES_USER"
    echo "POSTGRES_DB: $POSTGRES_DB"
    exit 1
fi

echo "Database: $POSTGRES_DB"
echo "User: $POSTGRES_USER"

# Kafka Connect 준비 대기
echo ""
echo "Kafka Connect 준비 확인 중..."
for i in {1..30}; do
  if curl -s http://localhost:8083/ > /dev/null 2>&1; then
    echo "✅ Kafka Connect 준비 완료"
    break
  fi
  if [ $i -eq 30 ]; then
    echo "❌ Kafka Connect가 준비되지 않았습니다."
    exit 1
  fi
  echo "대기 중... ($i/30)"
  sleep 2
done

# 기존 Connector 삭제 (있다면)
echo ""
echo "기존 Connector 삭제 시도..."
curl -X DELETE http://localhost:8083/connectors/order-outbox-connector 2>/dev/null || echo "기존 Connector 없음"

sleep 2

# 환경변수 치환하여 Connector 등록
echo ""
echo "Connector 등록 중..."
cat debezium-connector.json | \
  sed "s/\${POSTGRES_USER}/$POSTGRES_USER/g" | \
  sed "s/\${POSTGRES_PASSWORD}/$POSTGRES_PASSWORD/g" | \
  sed "s/\${POSTGRES_DB}/$POSTGRES_DB/g" | \
  curl -X POST http://localhost:8083/connectors \
    -H "Content-Type: application/json" \
    -d @-

# 상태 확인
echo ""
echo "=== Connector 상태 확인 ==="
sleep 2
curl http://localhost:8083/connectors/order-outbox-connector/status | jq

echo ""
echo "✅ Connector 등록 완료!"