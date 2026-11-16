-- ============================================================
-- V6: elvis_lite DLT 실패 메시지 격리 및 재처리 원장(dlt_history) 생성 (ISO 11179)
-- 과제: [3368] CSMS Lite 버전 (POC) | Sprint: S24-SYNC-0100-DE/CO
-- ============================================================

CREATE TABLE IF NOT EXISTS dlt_history (
    dlt_id              VARCHAR(64)     NOT NULL                    COMMENT 'PK - DLT 고유 식별자 (예: DLT-20261022-xxxx)',
    charge_box_id       VARCHAR(50)         NULL DEFAULT NULL       COMMENT '충전기 ID (파싱 실패 시 NULL 가능)',

    source_topic        VARCHAR(100)    NOT NULL DEFAULT 'ocpp-raw-events' COMMENT '원본 인바운드 토픽',
    source_partition    INT             NOT NULL DEFAULT 0          COMMENT '원본 토픽 파티션 번호',
    source_offset       BIGINT          NOT NULL DEFAULT 0          COMMENT '원본 토픽 오프셋 번호',

    error_category      VARCHAR(30)     NOT NULL DEFAULT 'NON_RETRYABLE' COMMENT '오류 분류 (NON_RETRYABLE, TRANSIENT, BUSINESS_LOGIC)',
    exception_class     VARCHAR(200)        NULL DEFAULT NULL       COMMENT '발생 예외 클래스명',
    error_message       VARCHAR(1000)       NULL DEFAULT NULL       COMMENT '오류 메시지 요약',
    raw_payload         LONGTEXT        NOT NULL                    COMMENT '원본 수신 전문 (OCPP JSON)',

    retry_count         INT             NOT NULL DEFAULT 0          COMMENT '재시도 횟수',
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING'  COMMENT '재처리 상태 (PENDING, REPLAYED, DISCARDED, FAILED)',

    failed_dt           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '최초 실패 일시',
    replayed_dt         DATETIME(3)         NULL DEFAULT NULL       COMMENT '재처리 완료 일시',

    reg_dt              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '등록일시',
    updt_dt             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3)              COMMENT '수정일시',

    PRIMARY KEY (dlt_id),
    KEY ix_dlt_box (charge_box_id),
    KEY ix_dlt_status (status),
    KEY ix_dlt_failed (failed_dt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Dead Letter Topic 실패 메시지 격리 및 재처리 원장 (ISO 11179)';

-- 레거시(tb_*, tbl_*) 하위 호환 뷰 생성
CREATE OR REPLACE VIEW tb_dlt_history AS SELECT * FROM dlt_history;
CREATE OR REPLACE VIEW tbl_dlt_history AS SELECT * FROM dlt_history;
