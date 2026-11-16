-- ============================================================
-- V2: elvis_lite.transaction_session 테이블 생성 (ISO 11179)
-- 과제: [3368] CSMS Lite 버전 (POC) | Sprint: S19-SYNC-0040-DE
-- 목적: OCPP StartTransaction / StopTransaction 수신 시
--       충전 세션 생명주기 및 전력량 계측 데이터를 원자적으로 관리
-- ============================================================

CREATE TABLE IF NOT EXISTS transaction_session (
    id                  BIGINT          NOT NULL AUTO_INCREMENT     COMMENT 'PK - 세션 내부 고유 일련번호',
    charge_box_id       VARCHAR(50)     NOT NULL                    COMMENT '충전기 식별자 (ChargeBoxIdentity)',
    connector_id        INT             NOT NULL DEFAULT 1          COMMENT '충전 건/커넥터 번호 (1~N)',
    transaction_id      BIGINT          NOT NULL                    COMMENT 'OCPP 거래 번호 (CSMS 채번 TransactionId)',
    id_tag              VARCHAR(64)     NOT NULL                    COMMENT '회원 인증 식별자 (RFID / IdTag)',
    parent_id_tag       VARCHAR(64)         NULL DEFAULT NULL       COMMENT '상위/법인 회원 식별자 (ParentIdTag)',

    status              VARCHAR(32)     NOT NULL DEFAULT 'STARTED'  COMMENT '세션 상태 (STARTED, CHARGING, STOPPED, COMPLETED, ABORTED, FORCE_CLOSED)',

    meter_start         BIGINT          NOT NULL DEFAULT 0          COMMENT '충전 시작 미터값 (Wh)',
    meter_stop          BIGINT              NULL DEFAULT NULL       COMMENT '충전 종료 미터값 (Wh)',
    total_energy_kwh    DECIMAL(10, 3)  NOT NULL DEFAULT 0.000      COMMENT '최종 충전 전력량 (kWh, (meter_stop-meter_start)/1000)',

    start_time          DATETIME(3)     NOT NULL                    COMMENT '충전 시작 일시 (단말 타임스탬프 기준)',
    stop_time           DATETIME(3)         NULL DEFAULT NULL       COMMENT '충전 종료 일시 (단말 타임스탬프 기준)',
    charging_minutes    INT             NOT NULL DEFAULT 0          COMMENT '총 충전 경과 시간 (분 단위)',

    stop_reason         VARCHAR(64)         NULL DEFAULT NULL       COMMENT 'OCPP 종료 사유 (EmergencyStop, EVDisconnected 등 11종)',

    version             INT             NOT NULL DEFAULT 0          COMMENT 'JPA 낙관적 락(Optimistic Locking) 버전 컬럼',
    reg_dt              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '최초 세션 생성 일시',
    updt_dt             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3)              COMMENT '최종 세션 갱신 일시',

    PRIMARY KEY (id),

    -- 1. 멱등성 보장 복합 유니크 인덱스 (동일 충전기 내 중복 transaction_id 인입 원천 방어)
    UNIQUE KEY uk_session_charge_box_trn (charge_box_id, transaction_id),

    -- 2. 활성 세션 상태 조회 최적화 인덱스 (커넥터별 현재 충전중 세션 조회)
    INDEX ix_session_charger_conn_status (charge_box_id, connector_id, status),

    -- 3. 일자별/기간별 정산 조회 인덱스
    INDEX ix_session_start_time (start_time),

    -- 4. 회원별 충전 이력 조회 인덱스
    INDEX ix_session_id_tag (id_tag)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='충전 거래 세션 관리 원장 (ISO 11179)';

-- 레거시(tb_*, tbl_*) 하위 호환 뷰 생성
CREATE OR REPLACE VIEW tb_transaction_session AS SELECT * FROM transaction_session;
CREATE OR REPLACE VIEW tbl_transaction_session AS SELECT * FROM transaction_session;
