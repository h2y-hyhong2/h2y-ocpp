-- ==============================================================================
-- ELVIS-CSMS 관제 플랫폼 MySQL 9.71+ DDL 스키마 정의서
-- 파일: config/mysql/01_schema.sql
-- 설명: 충전소/충전기 자산 마스터, 실시간 상태, 충전 세션 및 과금 원장 (CDR)
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS `elvis_lite`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `elvis_lite`;

-- ==============================================================================
-- 전용 애플리케이션 계정 생성 및 권한 부여
-- ==============================================================================
CREATE USER IF NOT EXISTS 'elvis'@'%' IDENTIFIED BY 'elvis1234!';
CREATE USER IF NOT EXISTS 'elvis'@'localhost' IDENTIFIED BY 'elvis1234!';
GRANT ALL PRIVILEGES ON `elvis_lite`.* TO 'elvis'@'%';
GRANT ALL PRIVILEGES ON `elvis_lite`.* TO 'elvis'@'localhost';
FLUSH PRIVILEGES;

-- ==============================================================================
-- 기존 뷰 및 테이블 정리
-- ==============================================================================
SET FOREIGN_KEY_CHECKS = 0;
DROP VIEW IF EXISTS tb_transaction_cdr;
-- DROP VIEW IF EXISTS tb_connector_status; (Sprint 18 실시간 물리 테이블로 분리)
DROP VIEW IF EXISTS tb_charger;
DROP VIEW IF EXISTS tb_station;
DROP VIEW IF EXISTS tb_corp;

DROP TABLE IF EXISTS tbl_transaction_cdr;
DROP TABLE IF EXISTS tbl_transaction_session;
DROP TABLE IF EXISTS tbl_connector_status;
DROP TABLE IF EXISTS tbl_charger;
DROP TABLE IF EXISTS tbl_station;
DROP TABLE IF EXISTS tbl_corp;
SET FOREIGN_KEY_CHECKS = 1;

-- ==============================================================================
-- 1. 법인/운영사 마스터 (tbl_corp)
-- ==============================================================================
CREATE TABLE tbl_corp (
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
-- 2. 충전소 마스터 (tbl_station)
-- ==============================================================================
CREATE TABLE tbl_station (
    ST_ID           VARCHAR(20)     NOT NULL COMMENT '충전소 고유 ID (6자리: 000000~999999, 예: 000000)',
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
    CONSTRAINT FK_STATION_CORP FOREIGN KEY (CORP_ID) REFERENCES tbl_corp (CORP_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='충전소 마스터';

-- ==============================================================================
-- 3. 충전기 마스터 (tbl_charger)
-- ==============================================================================
CREATE TABLE tbl_charger (
    CHARGE_BOX_ID   VARCHAR(50)     NOT NULL COMMENT 'OCPP ChargeBoxIdentity ({ST_ID}{CP_ID} 8자리, 예: 00000001)',
    ST_ID           VARCHAR(20)     NOT NULL COMMENT '소속 충전소 ID (FK, 6자리)',
    CP_ID           VARCHAR(10)     NOT NULL COMMENT '충전소 내 충전기 번호 2자리 (01~99, 예: 01)',
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
    CONSTRAINT FK_CHARGER_STATION FOREIGN KEY (ST_ID) REFERENCES tbl_station (ST_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='충전기 마스터';

-- ==============================================================================
-- 4. 커넥터 실시간 관제 및 상태 원장 (tbl_connector_status)
-- ==============================================================================
CREATE TABLE tbl_connector_status (
    CHARGE_BOX_ID   VARCHAR(50)     NOT NULL COMMENT '충전기 식별자 (ChargeBoxIdentity)',
    CONNECTOR_ID    INT             NOT NULL DEFAULT 1 COMMENT '커넥터/건 번호 (0=전체기, 1~N=커넥터)',
    STATUS          VARCHAR(32)     NOT NULL DEFAULT '연결안됨' COMMENT '상태 (Available, Charging, 연결안됨 등)',
    POWER_KW        DECIMAL(8, 2)   NOT NULL DEFAULT 0.0 COMMENT '현재 출력 전력 (kW)',
    VOLTAGE_V       DECIMAL(8, 2)   NOT NULL DEFAULT 0.0 COMMENT '현재 출력 전압 (V)',
    CURRENT_A       DECIMAL(8, 2)   NOT NULL DEFAULT 0.0 COMMENT '현재 출력 전류 (A)',
    SOC_PERCENT     INT             NOT NULL DEFAULT 0 COMMENT '차량 배터리 SoC (%)',
    BATTERY_TEMP_C  INT             NOT NULL DEFAULT 25 COMMENT '배터리 온도 (°C)',
    CAR_MODEL       VARCHAR(50)     NULL COMMENT '충전 중 차량 모델',
    USER_TAG        VARCHAR(50)     NULL COMMENT '사용자 인증 RFID/IdTag',
    ACCUMULATED_KWH DECIMAL(10, 2)  NOT NULL DEFAULT 0.0 COMMENT '세션 누적 충전량 (kWh)',
    CHARGING_MINUTES INT            NOT NULL DEFAULT 0 COMMENT '충전 경과 시간 (분)',
    LAST_HEARTBEAT  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최종 수신 일시 (관제 화면용)',
    UPDT_DT         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '최종 갱신 일시',
    PRIMARY KEY (CHARGE_BOX_ID, CONNECTOR_ID),
    KEY IX_CONN_STATUS (STATUS),
    CONSTRAINT FK_CONN_CHARGER FOREIGN KEY (CHARGE_BOX_ID) REFERENCES tbl_charger (CHARGE_BOX_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='충전기 커넥터 실시간 상태 원장';

-- ==============================================================================
-- 5. 충전 세션 및 과금 원장 (tbl_transaction_cdr)
-- ==============================================================================
CREATE TABLE tbl_transaction_cdr (
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
    CONSTRAINT FK_CDR_CHARGER FOREIGN KEY (CHARGE_BOX_ID) REFERENCES tbl_charger (CHARGE_BOX_ID)
) ENGINE=InnoDB AUTO_INCREMENT=98120 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='충전 과금 원장 (CDR)';

-- ==============================================================================
-- 6. 레거시 호환 뷰 (tb_*) 생성
-- ==============================================================================
CREATE OR REPLACE VIEW tb_corp AS SELECT * FROM tbl_corp;
CREATE OR REPLACE VIEW tb_station AS SELECT * FROM tbl_station;
CREATE OR REPLACE VIEW tb_charger AS SELECT * FROM tbl_charger;
-- CREATE OR REPLACE VIEW tb_connector_status AS SELECT * FROM tbl_connector_status; (Sprint 18 실시간 물리 테이블로 분리)
CREATE OR REPLACE VIEW tb_transaction_cdr AS SELECT * FROM tbl_transaction_cdr;
