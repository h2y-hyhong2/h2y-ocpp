-- ============================================================
-- V1: elvis_lite.tb_connector_status 테이블 생성
-- Sprint: S18-SYNC-0030-CO | 작성일: 2026-10-01
-- 목적: OCPP StatusNotification / Heartbeat 수신 시 충전기
--       커넥터별 최신 상태를 실시간 Upsert 저장
-- ============================================================

CREATE TABLE IF NOT EXISTS tb_connector_status (
    ID                  BIGINT          NOT NULL AUTO_INCREMENT     COMMENT 'PK - 자동증가',
    CHARGE_BOX_ID       VARCHAR(64)     NOT NULL                    COMMENT '충전기 식별자 (ChargeBoxId)',
    CONNECTOR_ID        INT             NOT NULL DEFAULT 0          COMMENT '커넥터 번호 (0=전체기, 1~N=커넥터)',
    STATUS              VARCHAR(32)     NOT NULL DEFAULT 'Unknown'  COMMENT 'OCPP ChargePointStatus (Available/Occupied/Faulted 등)',
    ERROR_CODE          VARCHAR(32)         NULL DEFAULT NULL       COMMENT 'OCPP ErrorCode (NoError/OtherError 등)',
    VENDOR_ID           VARCHAR(128)        NULL DEFAULT NULL       COMMENT 'StatusNotification vendorId',
    INFO                VARCHAR(255)        NULL DEFAULT NULL       COMMENT 'StatusNotification info 메시지',
    HEARTBEAT_AT        DATETIME(3)         NULL DEFAULT NULL       COMMENT '최근 Heartbeat 수신 일시 (3ms 정밀도)',
    STATUS_AT           DATETIME(3)         NULL DEFAULT NULL       COMMENT '최근 StatusNotification 수신 일시',
    VERSION             INT             NOT NULL DEFAULT 0          COMMENT '낙관적 락 버전 컬럼',
    REG_DT              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '최초 등록 일시',
    UPDT_DT             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3)             COMMENT '최종 갱신 일시',

    PRIMARY KEY (ID),

    -- 복합 유니크 인덱스: Upsert 기준 키 (CHARGE_BOX_ID + CONNECTOR_ID)
    UNIQUE KEY UK_CONN_CHARGE_BOX_CONNECTOR (CHARGE_BOX_ID, CONNECTOR_ID),

    -- 상태별 조회 성능 인덱스 (Faulted/Occupied 필터링)
    INDEX IX_CONN_STATUS (STATUS),

    -- 타임아웃 감지용 heartbeat 갱신 일시 인덱스
    INDEX IX_CONN_HEARTBEAT_AT (HEARTBEAT_AT)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='충전기 커넥터 실시간 상태 테이블 (StatusNotification / Heartbeat 기반 Upsert)';
