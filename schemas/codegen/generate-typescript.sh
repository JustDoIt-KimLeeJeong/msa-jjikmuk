#!/bin/bash
set -e

# 출력 디렉토리 설정 (BFF 서비스 내부)
OUTPUT_DIR="../../services/bff-service/src/generated"

echo "🚀 Generating TypeScript API clients..."

# 1. User Service
echo "📦 Generating User Service client..."
pnpm exec openapi-generator-cli generate \
  -i ../apis/user/openapi.json \
  -g typescript-axios \
  -o $OUTPUT_DIR/user \
  -c ./config/typescript.json

# 2. Order Service
echo "📦 Generating Order Service client..."
pnpm exec openapi-generator-cli generate \
  -i ../apis/order/openapi.json \
  -g typescript-axios \
  -o $OUTPUT_DIR/order \
  -c ./config/typescript.json

# 3. Portfolio Service
echo "📦 Generating Portfolio Service client..."
pnpm exec openapi-generator-cli generate \
  -i ../apis/portfolio/openapi.json \
  -g typescript-axios \
  -o $OUTPUT_DIR/portfolio \
  -c ./config/typescript.json

# 4. Market Data Service
echo "📦 Generating Market Data Service client..."
pnpm exec openapi-generator-cli generate \
  -i ../apis/market-data/openapi.json \
  -g typescript-axios \
  -o $OUTPUT_DIR/market-data \
  -c ./config/typescript.json

echo "✅ All clients generated successfully in $OUTPUT_DIR"
