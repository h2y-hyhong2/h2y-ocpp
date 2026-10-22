-- ============================================================
-- V5: elvis_lite 충전기 장애 발생 및 복구 이력 원장(tbl_fault_history) 생성
-- 과제: [3368] CSMS Lite 버전 (POC) | Sprint: S23-SYNC-0060-DE/CO
-- 작성일: 2026-10-22 | 작성자: IT기획 파트
-- ============================================================

CREATE TABLE IF NOT EXISTS tbl_fault_history (
    FAULT_ID            BIGINT          NOT NULL AUTO_INCREMENT     COMMENT 'PK - 장애 고유 ID',
    CHARGE_BOX_ID       VARCHAR(50)     NOT NULL                    COMMENT 'FK - 충전기 식별자 (tbl_charger)',
    CONNECTOR_ID        INT             NOT NULL DEFAULT 1          COMMENT '커넥터 번호 (0=본체, 1~N=커넥터)',
    
    STATUS              VARCHAR(32)     NOT NULL                    COMMENT '발생 당시 상태 (Faulted 등)',
    ERROR_CODE          VARCHAR(50)     NOT NULL                    COMMENT 'OCPP 표준 에러코드 (OverVoltage, GroundFailure 등)',
    VENDOR_ERROR_CODE   VARCHAR(50)         NULL DEFAULT NULL       COMMENT '제조사 상세 에러코드',
    SEVERITY            VARCHAR(20)     NOT NULL DEFAULT 'MAJOR'    COMMENT '심각도 (CRITICAL, MAJOR, MINOR, INFO)',
    
    FAULT_MSG           VARCHAR(500)        NULL DEFAULT NULL       COMMENT '상세 장애 설명 메시지',
    ACTION_REQUIRED     VARCHAR(50)         NULL DEFAULT 'INSPECTION' COMMENT '권장 조치 가이드 (EMERGENCY_SHUTDOWN, INSPECTION, NONE)',
    
    OCCURRED_DT         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '장애 발생 일시',
    RESOLVED_DT         DATETIME(3)         NULL DEFAULT NULL       COMMENT '장애 복구 일시',
    IS_RESOLVED         CHAR(1)         NOT NULL DEFAULT 'N'        COMMENT '해결 여부 (Y: 해결완료, N: 미해결/장애중)',
    
    REG_DT              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '등록일시',
    UPDT_DT             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3)              COMMENT '수정일시',

    PRIMARY KEY (FAULT_ID),
    KEY IX_FAULT_BOX_CONN (CHARGE_BOX_ID, CONNECTOR_ID, IS_RESOLVED),
    KEY IX_FAULT_OCCURRED (OCCURRED_DT),
    KEY IX_FAULT_SEVERITY (SEVERITY)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='충전기 장애 발생 및 복구 이력 원장';

-- 레거시 호환 뷰 생성
CREATE OR REPLACE VIEW tb_fault_history AS SELECT * FROM tbl_fault_history;
