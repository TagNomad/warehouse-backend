#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
OUTPUT="$SCRIPT_DIR/openapi.json"
PORT=18080
MAX_WAIT=60  # seconds

# 1. 启动（clean 确保 MapStruct 注解处理器正确运行，避免 IDE 编译污染）
echo "Starting application on port $PORT ..."
"$PROJECT_ROOT/mvnw" -f "$PROJECT_ROOT/pom.xml" clean spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dserver.port=$PORT" \
  --quiet &
APP_PID=$!

# 2. 等待 /v3/api-docs 可访问
echo "Waiting for http://localhost:$PORT/v3/api-docs ..."
elapsed=0
until curl -sf "http://localhost:$PORT/v3/api-docs" > /dev/null; do
  if ! kill -0 "$APP_PID" 2>/dev/null; then
    echo "ERROR: Application process exited unexpectedly." >&2
    exit 1
  fi
  if [ "$elapsed" -ge "$MAX_WAIT" ]; then
    echo "ERROR: Timed out waiting for application to start." >&2
    kill "$APP_PID" 2>/dev/null || true
    exit 1
  fi
  sleep 2
  elapsed=$((elapsed + 2))
done

# 3. 导出
echo "Exporting OpenAPI spec to $OUTPUT ..."
curl -sf "http://localhost:$PORT/v3/api-docs" -o "$OUTPUT"
echo "Done: $OUTPUT"

# 4. 关闭
kill "$APP_PID"
wait "$APP_PID" 2>/dev/null || true
