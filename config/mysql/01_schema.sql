-- ==============================================================================
-- ELVIS-CSMS 관제 플랫폼 MySQL 9.71+ DDL 스키마 정의서
-- 파일: config/mysql/01_schema.sql
-- 설명: 충전소/충전기 자산 마스터, 실시간 상태, 충전 세션 및 과금 원장 (CDR)
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS `elvis-lite`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `elvis-lite`;

-- ==============================================================================
-- 전용 애플리케이션 계정 생성 및 권한 부여
-- ==============================================================================
CREATE USER IF NOT EXISTS 'elvis'@'%' IDENTIFIED BY 'elvis1234!';
CREATE USER IF NOT EXISTS 'elvis'@'localhost' IDENTIFIED BY 'elvis1234!';
GRANT ALL PRIVILEGES ON `elvis-lite`.* TO 'elvis'@'%';
GRANT ALL PRIVILEGES ON `elvis-lite`.* TO 'elvis'@'localhost';
FLUSH PRIVILEGES;

-- ==============================================================================
-- 1. 법인/운영사 마스터 (tb_corp)
-- ==============================================================================
DROP TABLE IF EXISTS tb_transaction_session;
DROP TABLE IF EXISTS tb_connector_status;
DROP TABLE IF EXISTS tb_transaction_cdr;
DROP TABLE IF EXISTS tb_charger;
DROP TABLE IF EXISTS tb_station;
DROP TABLE IF EXISTS tb_corp;

CREATE TABLE tb_corp (
    CORP_ID         VARCHAR(20)     NOT NULL COMMENT '법인 식별코드 (예: CORP_001)',
    CORP_NAME       VARCHAR(100)    NOT NULL COMMENT '정식 법인명',
    SHORT_NAME      VARCHAR(50)     NOT NULL COMMENT '축약 법인명 (배지/태그용)',
    TAG             VARCHAR(20)     NOT NULL COMMENT '구분 태그 (직영, 공공, CPO, 운수)',
    COLOR_CLASS     VARCHAR(20)     NOT NULL DEFAULT 'sky' COMMENT 'UI 강조 색상 테마',
    BADGE           VARCHAR(50)     NULL COMMENT '거점 규모 배지',
    USE_YN          CHAR(1)         NOT NULL DEFAULT 'Y' COMMENT '사용 여부 (Y/N)',
    REG_DT          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    UPDT_DT         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (CORP_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='법인 및 운영사 마스터';

-- ==============================================================================
-- 2. 충전소 마스터 (tb_station)
-- ==============================================================================
CREATE TABLE tb_station (
    ST_ID           VARCHAR(20)     NOT NULL COMMENT '충전소 고유 ID (예: 10001)',
    NAME            VARCHAR(150)    NOT NULL COMMENT '충전소 명칭',
    CORP_ID         VARCHAR(20)     NOT NULL COMMENT '소속 법인 ID (FK)',
    CHARGER_COUNT   INT             NOT NULL DEFAULT 0 COMMENT '충전기 수량',
    ADDR            VARCHAR(250)    NULL COMMENT '소재지 주소',
    LAT             DECIMAL(10, 7)  NULL COMMENT '위도',
    LNG             DECIMAL(10, 7)  NULL COMMENT '경도',
    STATUS          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '상태 (ACTIVE, INSPECTING, CLOSED)',
    REG_DT          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    UPDT_DT         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (ST_ID),
    KEY IX_STATION_CORP_ID (CORP_ID),
    CONSTRAINT FK_STATION_CORP FOREIGN KEY (CORP_ID) REFERENCES tb_corp (CORP_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='충전소 마스터';

-- ==============================================================================
-- 3. 충전기 마스터 (tb_charger)
-- ==============================================================================
CREATE TABLE tb_charger (
    CHARGE_BOX_ID   VARCHAR(50)     NOT NULL COMMENT 'OCPP ChargeBoxIdentity (예: CP-001-01)',
    ST_ID           VARCHAR(20)     NOT NULL COMMENT '소속 충전소 ID (FK)',
    CP_ID           VARCHAR(10)     NOT NULL COMMENT '충전소 내 단말 순번 (예: 01)',
    VENDOR          VARCHAR(50)     NOT NULL COMMENT '제조사 (LS E-Link Power, SK Signet 등)',
    MODEL           VARCHAR(50)     NOT NULL COMMENT '모델명 (ELVIS-HPC-350K 등)',
    SPEC            VARCHAR(50)     NOT NULL COMMENT '정격 사양 (350kW 초급속, 200kW 급속)',
    PROTOCOL        VARCHAR(30)     NOT NULL DEFAULT 'OCPP 1.6-J' COMMENT '프로토콜 버전',
    FIRMWARE_VER    VARCHAR(50)     NULL COMMENT '펌웨어 버전',
    USE_YN          CHAR(1)         NOT NULL DEFAULT 'Y' COMMENT '사용 여부 (Y/N)',
    REG_DT          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    UPDT_DT         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (CHARGE_BOX_ID),
    KEY IX_CHARGER_ST_ID (ST_ID),
    CONSTRAINT FK_CHARGER_STATION FOREIGN KEY (ST_ID) REFERENCES tb_station (ST_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='충전기 마스터';

-- ==============================================================================
-- 4. 커넥터 실시간 관제 및 상태 원장 (tb_connector_status)
-- ==============================================================================
CREATE TABLE tb_connector_status (
    ID              BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'PK - 내부 고유 일련번호',
    CHARGE_BOX_ID   VARCHAR(50)     NOT NULL COMMENT '충전기 식별자 (ChargeBoxIdentity)',
    CONNECTOR_ID    INT             NOT NULL DEFAULT 1 COMMENT '커넥터/건 번호 (0=전체기, 1~N=커넥터)',
    STATUS          VARCHAR(32)     NOT NULL DEFAULT 'Available' COMMENT 'OCPP 상태 (Available, Occupied, Faulted 등)',
    ERROR_CODE      VARCHAR(32)     NULL DEFAULT NULL COMMENT 'OCPP 에러코드 (NoError, OtherError 등)',
    VENDOR_ID       VARCHAR(128)    NULL DEFAULT NULL COMMENT '제조사 에러/상태 코드 (vendorId)',
    INFO            VARCHAR(255)    NULL DEFAULT NULL COMMENT '상태 부가 정보 (info 메시지)',
    POWER_KW        DECIMAL(8, 2)   NOT NULL DEFAULT 0.0 COMMENT '현재 출력 전력 (kW)',
    VOLTAGE_V       DECIMAL(8, 2)   NOT NULL DEFAULT 0.0 COMMENT '현재 출력 전압 (V)',
    CURRENT_A       DECIMAL(8, 2)   NOT NULL DEFAULT 0.0 COMMENT '현재 출력 전류 (A)',
    SOC_PERCENT     INT             NOT NULL DEFAULT 0 COMMENT '차량 배터리 SoC (%)',
    BATTERY_TEMP_C  INT             NOT NULL DEFAULT 25 COMMENT '배터리 온도 (°C)',
    CAR_MODEL       VARCHAR(50)     NULL COMMENT '충전 중 차량 모델',
    USER_TAG        VARCHAR(50)     NULL COMMENT '사용자 인증 RFID/IdTag',
    ACCUMULATED_KWH DECIMAL(10, 2)  NOT NULL DEFAULT 0.0 COMMENT '세션 누적 충전량 (kWh)',
    CHARGING_MINUTES INT            NOT NULL DEFAULT 0 COMMENT '충전 경과 시간 (분)',
    HEARTBEAT_AT    DATETIME(3)     NULL DEFAULT NULL COMMENT '최근 Heartbeat 수신 일시 (3ms 정밀도)',
    STATUS_AT       DATETIME(3)     NULL DEFAULT NULL COMMENT '최근 StatusNotification 수신 일시',
    LAST_HEARTBEAT  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최종 수신 일시 (관제 화면용)',
    VERSION         INT             NOT NULL DEFAULT 0 COMMENT 'JPA 낙관적 락 버전 컬럼',
    REG_DT          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '최초 등록일시',
    UPDT_DT         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '최종 갱신일시',
    PRIMARY KEY (ID),
    UNIQUE KEY UK_CONN_CHARGE_BOX_CONNECTOR (CHARGE_BOX_ID, CONNECTOR_ID),
    KEY IX_CONN_STATUS (STATUS),
    KEY IX_CONN_HEARTBEAT_AT (HEARTBEAT_AT),
    CONSTRAINT FK_CONN_CHARGER FOREIGN KEY (CHARGE_BOX_ID) REFERENCES tb_charger (CHARGE_BOX_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='충전기 커넥터 실시간 상태 원장 (StatusNotification / Heartbeat 기반 Upsert)';

-- ==============================================================================
-- 5. 충전 거래 세션 관리 원장 (tb_transaction_session)
-- ==============================================================================
CREATE TABLE tb_transaction_session (
    ID                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'PK - 세션 내부 고유 일련번호',
    CHARGE_BOX_ID       VARCHAR(50)     NOT NULL COMMENT '충전기 식별자 (ChargeBoxIdentity)',
    CONNECTOR_ID        INT             NOT NULL DEFAULT 1 COMMENT '충전 건/커넥터 번호 (1~N)',
    TRANSACTION_ID      BIGINT          NOT NULL COMMENT 'OCPP 거래 번호 (단말 채번 TransactionId)',
    ID_TAG              VARCHAR(64)     NOT NULL COMMENT '회원 인증 식별자 (RFID / IdTag)',
    PARENT_ID_TAG       VARCHAR(64)     NULL DEFAULT NULL COMMENT '상위/법인 회원 식별자 (ParentIdTag)',
    STATUS              VARCHAR(32)     NOT NULL DEFAULT 'STARTED' COMMENT '세션 상태 (STARTED, CHARGING, STOPPED, COMPLETED, ABORTED, FORCE_CLOSED)',
    METER_START         BIGINT          NOT NULL DEFAULT 0 COMMENT '충전 시작 미터값 (Wh)',
    METER_STOP          BIGINT          NULL DEFAULT NULL COMMENT '충전 종료 미터값 (Wh)',
    TOTAL_ENERGY_KWH    DECIMAL(10, 3)  NOT NULL DEFAULT 0.000 COMMENT '최종 충전 전력량 (kWh, (meterStop-meterStart)/1000)',
    START_TIME          DATETIME(3)     NOT NULL COMMENT '충전 시작 일시 (단말 타임스탬프 기준)',
    STOP_TIME           DATETIME(3)     NULL DEFAULT NULL COMMENT '충전 종료 일시 (단말 타임스탬프 기준)',
    CHARGING_MINUTES    INT             NOT NULL DEFAULT 0 COMMENT '총 충전 경과 시간 (분 단위)',
    STOP_REASON         VARCHAR(64)     NULL DEFAULT NULL COMMENT 'OCPP 종료 사유 (EmergencyStop, EVDisconnected 등 11종)',
    VERSION             INT             NOT NULL DEFAULT 0 COMMENT 'JPA 낙관적 락(Optimistic Locking) 버전 컬럼',
    REG_DT              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '최초 세션 생성 일시',
    UPDT_DT             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '최종 세션 갱신 일시',
    PRIMARY KEY (ID),
    UNIQUE KEY UK_SESSION_CHARGE_BOX_TRN (CHARGE_BOX_ID, TRANSACTION_ID),
    KEY IX_SESSION_CHARGER_CONN_STATUS (CHARGE_BOX_ID, CONNECTOR_ID, STATUS),
    KEY IX_SESSION_START_TIME (START_TIME),
    KEY IX_SESSION_ID_TAG (ID_TAG),
    CONSTRAINT FK_SESSION_CHARGER FOREIGN KEY (CHARGE_BOX_ID) REFERENCES tb_charger (CHARGE_BOX_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='충전 거래 세션 관리 원장 (StartTransaction / StopTransaction 연동)';

-- ==============================================================================
-- 6. 충전 세션 및 과금 원장 (tb_transaction_cdr)
-- ==============================================================================
CREATE TABLE tb_transaction_cdr (
    TRANSACTION_ID  BIGINT AUTO_INCREMENT NOT NULL COMMENT '정산 트랜잭션 고유번호 (PK)',
    CHARGE_BOX_ID   VARCHAR(50)     NOT NULL COMMENT '충전기 ID',
    CONNECTOR_ID    INT             NOT NULL DEFAULT 1 COMMENT '커넥터 번호',
    USER_TAG        VARCHAR(50)     NOT NULL COMMENT '회원 RFID / IdTag',
    START_TIME      DATETIME        NOT NULL COMMENT '충전 시작 일시',
    STOP_TIME       DATETIME        NULL COMMENT '충전 종료 일시',
    METER_START_WH  BIGINT          NOT NULL DEFAULT 0 COMMENT '시작 미터값 (Wh)',
    METER_STOP_WH   BIGINT          NULL COMMENT '종료 미터값 (Wh)',
    TOTAL_KWH       DECIMAL(10, 2)  NOT NULL DEFAULT 0.0 COMMENT '최종 충전량 (kWh)',
    UNIT_PRICE      DECIMAL(8, 2)   NOT NULL DEFAULT 288.1 COMMENT '적용 단가 (원/kWh)',
    TOTAL_AMOUNT    DECIMAL(12, 2)  NOT NULL DEFAULT 0.0 COMMENT '최종 과금액 (원)',
    PAYMENT_STATUS  VARCHAR(20)     NOT NULL DEFAULT 'COMPLETED' COMMENT 'COMPLETED, CHARGING, FAILED',
    STOP_REASON     VARCHAR(50)     NULL COMMENT '종료 사유',
    REG_DT          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    PRIMARY KEY (TRANSACTION_ID),
    KEY IX_CDR_START_TIME (START_TIME),
    KEY IX_CDR_USER_TAG (USER_TAG),
    CONSTRAINT FK_CDR_CHARGER FOREIGN KEY (CHARGE_BOX_ID) REFERENCES tb_charger (CHARGE_BOX_ID)
) ENGINE=InnoDB AUTO_INCREMENT=98120 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='충전 과금 원장 (CDR)';
