#!/bin/bash
set -e

echo "🚀 Generating TypeScript API client..."

pnpm exec openapi-generator-cli generate \
  -i ../../packages/apis/auth/openapi.json \
  -g typescript-fetch \
  -o ../../generated/ts/auth \
  -c ./openapi-generator-config.json

echo "✅ Done: generated/ts/auth"
