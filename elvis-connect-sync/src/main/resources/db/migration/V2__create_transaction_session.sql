-- ============================================================
-- V2: elvis_lite.tb_transaction_session 테이블 생성
-- 과제: [3368] CSMS Lite 버전 (POC) | Sprint: S19-SYNC-0040-DE
-- 작성일: 2026-10-06 | 작성자: IT기획 파트
-- 목적: OCPP StartTransaction / StopTransaction 수신 시
--       충전 세션 생명주기 및 전력량 계측 데이터를 원자적으로 관리
-- ============================================================

CREATE TABLE IF NOT EXISTS tb_transaction_session (
    ID                  BIGINT          NOT NULL AUTO_INCREMENT     COMMENT 'PK - 세션 내부 고유 일련번호',
    CHARGE_BOX_ID       VARCHAR(64)     NOT NULL                    COMMENT '충전기 식별자 (ChargeBoxIdentity)',
    CONNECTOR_ID        INT             NOT NULL DEFAULT 1          COMMENT '충전 건/커넥터 번호 (1~N)',
    TRANSACTION_ID      BIGINT          NOT NULL                    COMMENT 'OCPP 거래 번호 (단말 채번 TransactionId)',
    ID_TAG              VARCHAR(64)     NOT NULL                    COMMENT '회원 인증 식별자 (RFID / IdTag)',
    PARENT_ID_TAG       VARCHAR(64)         NULL DEFAULT NULL       COMMENT '상위/법인 회원 식별자 (ParentIdTag)',
    
    STATUS              VARCHAR(32)     NOT NULL DEFAULT 'STARTED'  COMMENT '세션 상태 (STARTED, CHARGING, STOPPED, COMPLETED, ABORTED, FORCE_CLOSED)',
    
    METER_START         BIGINT          NOT NULL DEFAULT 0          COMMENT '충전 시작 미터값 (Wh)',
    METER_STOP          BIGINT              NULL DEFAULT NULL       COMMENT '충전 종료 미터값 (Wh)',
    TOTAL_ENERGY_KWH    DECIMAL(10, 3)  NOT NULL DEFAULT 0.000      COMMENT '최종 충전 전력량 (kWh, (meterStop-meterStart)/1000)',
    
    START_TIME          DATETIME(3)     NOT NULL                    COMMENT '충전 시작 일시 (단말 타임스탬프 기준)',
    STOP_TIME           DATETIME(3)         NULL DEFAULT NULL       COMMENT '충전 종료 일시 (단말 타임스탬프 기준)',
    CHARGING_MINUTES    INT             NOT NULL DEFAULT 0          COMMENT '총 충전 경과 시간 (분 단위)',
    
    STOP_REASON         VARCHAR(64)         NULL DEFAULT NULL       COMMENT 'OCPP 종료 사유 (EmergencyStop, EVDisconnected 등 11종)',
    
    VERSION             INT             NOT NULL DEFAULT 0          COMMENT 'JPA 낙관적 락(Optimistic Locking) 버전 컬럼',
    REG_DT              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '최초 세션 생성 일시',
    UPDT_DT             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3)             COMMENT '최종 세션 갱신 일시',

    PRIMARY KEY (ID),

    -- 1. 멱등성 보장 복합 유니크 인덱스 (동일 충전기 내 중복 TransactionId 인입 원천 방어)
    UNIQUE KEY UK_SESSION_CHARGE_BOX_TRN (CHARGE_BOX_ID, TRANSACTION_ID),

    -- 2. 활성 세션 상태 조회 최적화 인덱스 (커넥터별 현재 충전중 세션 조회)
    INDEX IX_SESSION_CHARGER_CONN_STATUS (CHARGE_BOX_ID, CONNECTOR_ID, STATUS),

    -- 3. 일자별/기간별 정산 조회 인덱스
    INDEX IX_SESSION_START_TIME (START_TIME),

    -- 4. 회원별 충전 이력 조회 인덱스
    INDEX IX_SESSION_ID_TAG (ID_TAG)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='충전 거래 세션 관리 원장 (StartTransaction / StopTransaction 연동)';
