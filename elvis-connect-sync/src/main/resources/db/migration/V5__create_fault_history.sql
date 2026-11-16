-- ============================================================
-- V5: elvis_lite 충전기 장애 발생 및 복구 이력 원장(fault_history) 생성 (ISO 11179)
-- 과제: [3368] CSMS Lite 버전 (POC) | Sprint: S23-SYNC-0060-DE/CO
-- ============================================================

CREATE TABLE IF NOT EXISTS fault_history (
    fault_id            BIGINT          NOT NULL AUTO_INCREMENT     COMMENT 'PK - 장애 고유 ID',
    charge_box_id       VARCHAR(50)     NOT NULL                    COMMENT 'FK - 충전기 식별자 (charger)',
    connector_id        INT             NOT NULL DEFAULT 1          COMMENT '커넥터 번호 (0=본체, 1~N=커넥터)',

    status              VARCHAR(32)     NOT NULL                    COMMENT '발생 당시 상태 (Faulted 등)',
    error_code          VARCHAR(50)     NOT NULL                    COMMENT 'OCPP 표준 에러코드 (OverVoltage, GroundFailure 등)',
    vendor_error_code   VARCHAR(50)         NULL DEFAULT NULL       COMMENT '제조사 상세 에러코드',
    severity            VARCHAR(20)     NOT NULL DEFAULT 'MAJOR'    COMMENT '심각도 (CRITICAL, MAJOR, MINOR, INFO)',

    fault_msg           VARCHAR(500)        NULL DEFAULT NULL       COMMENT '상세 장애 설명 메시지',
    action_required     VARCHAR(50)         NULL DEFAULT 'INSPECTION' COMMENT '권장 조치 가이드 (EMERGENCY_SHUTDOWN, INSPECTION, NONE)',

    occurred_dt         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '장애 발생 일시',
    resolved_dt         DATETIME(3)         NULL DEFAULT NULL       COMMENT '장애 복구 일시',
    is_resolved         CHAR(1)         NOT NULL DEFAULT 'N'        COMMENT '해결 여부 (Y: 해결완료, N: 미해결/장애중)',

    reg_dt              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '등록일시',
    updt_dt             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3)              COMMENT '수정일시',

    PRIMARY KEY (fault_id),
    KEY ix_fault_box_conn (charge_box_id, connector_id, is_resolved),
    KEY ix_fault_occurred (occurred_dt),
    KEY ix_fault_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='충전기 장애 발생 및 복구 이력 원장 (ISO 11179)';

-- 레거시(tb_*, tbl_*) 하위 호환 뷰 생성
CREATE OR REPLACE VIEW tb_fault_history AS SELECT * FROM fault_history;
CREATE OR REPLACE VIEW tbl_fault_history AS SELECT * FROM fault_history;
