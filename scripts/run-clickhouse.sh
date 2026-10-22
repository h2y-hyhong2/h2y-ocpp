#!/bin/bash
CH_HTTP_PORT=${1:-8123}
CH_TCP_PORT=${2:-9000}
LOG_DIR="/mnt/d/elvis-lite/logs"
mkdir -p "$LOG_DIR"
BIN_DIR="/mnt/d/project/lselink/ocpp-lite/git/h2y-ocpp/binaries/clickhouse-v26.7.34.3-stable"

cd "$BIN_DIR" || exit 1
chmod +x clickhouse

# ClickHouse 공식 log-file 및 errorlog-file 인자로 D:\elvis-lite\logs에 직접 기록
exec ./clickhouse server \
  --log-file="$LOG_DIR/clickhouse.log" \
  --errorlog-file="$LOG_DIR/clickhouse-err.log" \
  -- \
  --listen_host=0.0.0.0 \
  --http_port="$CH_HTTP_PORT" \
  --tcp_port="$CH_TCP_PORT"
