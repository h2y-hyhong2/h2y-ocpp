-- ============================================================
-- V1: elvis_lite.connector_status 테이블 생성 (ISO 11179)
-- Sprint: S18-SYNC-0030-CO
-- 목적: OCPP StatusNotification / Heartbeat 수신 시 충전기
--       커넥터별 최신 상태를 실시간 Upsert 저장
-- ============================================================

CREATE TABLE IF NOT EXISTS connector_status (
    id                  BIGINT          NOT NULL AUTO_INCREMENT     COMMENT 'PK - 자동증가',
    charge_box_id       VARCHAR(50)     NOT NULL                    COMMENT '충전기 식별자 (ChargeBoxId, 8자리)',
    connector_id        INT             NOT NULL DEFAULT 1          COMMENT '커넥터 번호 (0=전체기, 1~N=커넥터)',
    status              VARCHAR(32)     NOT NULL DEFAULT 'Unknown'  COMMENT 'OCPP ChargePointStatus (Available/Occupied/Faulted 등)',
    error_code          VARCHAR(32)         NULL DEFAULT NULL       COMMENT 'OCPP ErrorCode (NoError/OtherError 등)',
    vendor_id           VARCHAR(128)        NULL DEFAULT NULL       COMMENT 'StatusNotification vendorId',
    info                VARCHAR(255)        NULL DEFAULT NULL       COMMENT 'StatusNotification info 메시지',
    power_kw            DECIMAL(8, 2)   NOT NULL DEFAULT 0.00       COMMENT '현재 출력 전력 (kW)',
    voltage_v           DECIMAL(8, 2)   NOT NULL DEFAULT 0.00       COMMENT '현재 출력 전압 (V)',
    current_a           DECIMAL(8, 2)   NOT NULL DEFAULT 0.00       COMMENT '현재 출력 전류 (A)',
    soc_percent         INT             NOT NULL DEFAULT 0          COMMENT '차량 배터리 SoC (%)',
    battery_temp_c      INT             NOT NULL DEFAULT 25         COMMENT '배터리 온도 (°C)',
    car_model           VARCHAR(50)         NULL DEFAULT NULL       COMMENT '충전 중 차량 모델',
    user_tag            VARCHAR(50)         NULL DEFAULT NULL       COMMENT '사용자 인증 RFID/IdTag',
    accumulated_kwh     DECIMAL(10, 2)  NOT NULL DEFAULT 0.00       COMMENT '세션 누적 충전량 (kWh)',
    charging_minutes    INT             NOT NULL DEFAULT 0          COMMENT '충전 경과 시간 (분)',
    last_heartbeat      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '최종 수신 일시 (관제 화면용)',
    heartbeat_at        DATETIME(3)         NULL DEFAULT NULL       COMMENT '최근 Heartbeat 수신 일시 (3ms 정밀도)',
    status_at           DATETIME(3)         NULL DEFAULT NULL       COMMENT '최근 StatusNotification 수신 일시',
    version             INT             NOT NULL DEFAULT 0          COMMENT '낙관적 락 버전 컬럼',
    reg_dt              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '최초 등록 일시',
    updt_dt             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3)              COMMENT '최종 갱신 일시',

    PRIMARY KEY (id),

    -- 복합 유니크 인덱스: Upsert 기준 키 (charge_box_id + connector_id)
    UNIQUE KEY uk_conn_charge_box_connector (charge_box_id, connector_id),

    -- 상태별 조회 성능 인덱스 (Faulted/Occupied 필터링)
    INDEX ix_conn_status (status),

    -- 타임아웃 감지용 heartbeat 갱신 일시 인덱스
    INDEX ix_conn_heartbeat_at (heartbeat_at)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='충전기 커넥터 실시간 상태 원장 (ISO 11179)';

-- 레거시(tb_*, tbl_*) 하위 호환 뷰 생성
CREATE OR REPLACE VIEW tb_connector_status AS SELECT * FROM connector_status;
CREATE OR REPLACE VIEW tbl_connector_status AS SELECT * FROM connector_status;
