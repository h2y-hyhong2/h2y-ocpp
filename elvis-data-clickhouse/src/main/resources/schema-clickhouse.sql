-- ===================================================================
-- ClickHouse MergeTree DDL 스키마 (CSMS Lite PoC v1.0)
-- 대상 모듈: elvis-data-clickhouse
-- ===================================================================

CREATE DATABASE IF NOT EXISTS elvis_poc;

CREATE TABLE IF NOT EXISTS elvis_poc.meter_value_history
(
    event_time      DateTime64(3, 'Asia/Seoul') COMMENT '계측 발생 일시',
    charge_box_id   LowCardinality(String)      COMMENT '충전기 식별자 (8자리 정규화)',
    connector_id    UInt8                       COMMENT '커넥터 번호 (1 또는 2)',
    transaction_id  Nullable(UInt32)            COMMENT '트랜잭션 ID (세션)',
    voltage         Float32                     COMMENT '전압 (V)',
    current_import  Float32                     COMMENT '전류 (A)',
    power_active    Float32                     COMMENT '유효 출력 (kW)',
    energy_active   UInt64                      COMMENT '누적 유효 전력량 (Wh)',
    soc             UInt8                       COMMENT '배터리 충전 상태 (0~100 %)',
    raw_json        String                      COMMENT '원본 JSON 페이로드'
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(event_time)
ORDER BY (charge_box_id, connector_id, event_time)
TTL toDateTime(event_time) + INTERVAL 180 DAY
SETTINGS index_granularity = 8192;
