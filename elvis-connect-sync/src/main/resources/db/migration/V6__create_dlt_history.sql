-- ============================================================
-- V6: elvis_lite DLT 실패 메시지 격리 및 재처리 원장(tbl_dlt_history) 생성
-- 과제: [3368] CSMS Lite 버전 (POC) | Sprint: S24-SYNC-0100-DE/CO
-- 작성일: 2026-10-22 | 작성자: IT기획 파트
-- ============================================================

CREATE TABLE IF NOT EXISTS tbl_dlt_history (
    DLT_ID              VARCHAR(64)     NOT NULL                    COMMENT 'PK - DLT 고유 식별자 (예: DLT-20261022-xxxx)',
    CHARGE_BOX_ID       VARCHAR(50)         NULL DEFAULT NULL       COMMENT '충전기 ID (파싱 실패 시 NULL 가능)',
    
    SOURCE_TOPIC        VARCHAR(100)    NOT NULL DEFAULT 'ocpp-raw-events' COMMENT '원본 인바운드 토픽',
    SOURCE_PARTITION    INT             NOT NULL DEFAULT 0          COMMENT '원본 토픽 파티션 번호',
    SOURCE_OFFSET       BIGINT          NOT NULL DEFAULT 0          COMMENT '원본 토픽 오프셋 번호',
    
    ERROR_CATEGORY      VARCHAR(30)     NOT NULL DEFAULT 'NON_RETRYABLE' COMMENT '오류 분류 (NON_RETRYABLE, TRANSIENT, BUSINESS_LOGIC)',
    EXCEPTION_CLASS     VARCHAR(200)        NULL DEFAULT NULL       COMMENT '발생 예외 클래스명',
    ERROR_MESSAGE       VARCHAR(1000)       NULL DEFAULT NULL       COMMENT '오류 메시지 요약',
    RAW_PAYLOAD         LONGTEXT        NOT NULL                    COMMENT '원본 수신 전문 (OCPP JSON)',
    
    RETRY_COUNT         INT             NOT NULL DEFAULT 0          COMMENT '재시도 횟수',
    STATUS              VARCHAR(20)     NOT NULL DEFAULT 'PENDING'  COMMENT '재처리 상태 (PENDING, REPLAYED, DISCARDED, FAILED)',
    
    FAILED_DT           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '최초 실패 일시',
    REPLAYED_DT         DATETIME(3)         NULL DEFAULT NULL       COMMENT '재처리 완료 일시',
    
    REG_DT              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '등록일시',
    UPDT_DT             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3)              COMMENT '수정일시',

    PRIMARY KEY (DLT_ID),
    KEY IX_DLT_BOX (CHARGE_BOX_ID),
    KEY IX_DLT_STATUS (STATUS),
    KEY IX_DLT_FAILED (FAILED_DT)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Dead Letter Topic 실패 메시지 격리 및 재처리 원장';

-- 레거시 호환 뷰 생성
CREATE OR REPLACE VIEW tb_dlt_history AS SELECT * FROM tbl_dlt_history;
