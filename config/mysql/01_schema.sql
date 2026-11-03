-- ==============================================================================
-- ELVIS-CSMS 관제 플랫폼 MySQL 9.71+ DDL 스키마 정의서 (ISO 11179 표준)
-- 파일: config/mysql/01_schema.sql
-- 설명: ISO 11179 글로벌 명명 표준(No-Prefix, lower_snake_case, 단수형 명사)
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
-- 기존 테이블 및 뷰 정리
-- ==============================================================================
SET FOREIGN_KEY_CHECKS = 0;
DROP VIEW IF EXISTS tb_fault_history, tbl_fault_history;
DROP VIEW IF EXISTS tb_transaction_cdr, tbl_transaction_cdr;
DROP VIEW IF EXISTS tb_connector_status, tbl_connector_status;
DROP VIEW IF EXISTS tb_charger, tbl_charger;
DROP VIEW IF EXISTS tb_station, tbl_station;
DROP VIEW IF EXISTS tb_corp, tbl_corp;
DROP VIEW IF EXISTS tb_dlt_history, tbl_dlt_history;
DROP VIEW IF EXISTS tb_admin_user, tbl_admin_user;

DROP TABLE IF EXISTS dlt_history, tbl_dlt_history;
DROP TABLE IF EXISTS fault_history, tbl_fault_history;
DROP TABLE IF EXISTS transaction_cdr, tbl_transaction_cdr;
DROP TABLE IF EXISTS transaction_session, tbl_transaction_session;
DROP TABLE IF EXISTS connector_status, tbl_connector_status;
DROP TABLE IF EXISTS charger, tbl_charger;
DROP TABLE IF EXISTS station, tbl_station;
DROP TABLE IF EXISTS corp, tbl_corp;
DROP TABLE IF EXISTS admin_user, tb_admin_user, tbl_admin_user;
SET FOREIGN_KEY_CHECKS = 1;

-- ==============================================================================
-- 1. 법인 및 운영사 마스터 (corp)
-- ==============================================================================
CREATE TABLE corp (
    corp_id         VARCHAR(20)     NOT NULL COMMENT '법인 식별코드 (예: CORP_001)',
    corp_name       VARCHAR(100)    NOT NULL COMMENT '정식 법인명',
    short_name      VARCHAR(50)     NOT NULL COMMENT '축약 법인명 (배지/태그용)',
    tag             VARCHAR(20)     NOT NULL COMMENT '구분 태그 (직영, 공공, CPO, 운수)',
    color_class     VARCHAR(20)     NOT NULL DEFAULT 'sky' COMMENT 'UI 강조 색상 테마',
    badge           VARCHAR(50)     NULL COMMENT '거점 규모 배지',
    use_yn          CHAR(1)         NOT NULL DEFAULT 'Y' COMMENT '사용 여부 (Y/N)',
    reg_dt          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    updt_dt         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (corp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='법인 및 운영사 마스터 (ISO 11179)';

-- ==============================================================================
-- 2. 거점 충전소 마스터 (station)
-- ==============================================================================
CREATE TABLE station (
    st_id           VARCHAR(20)     NOT NULL COMMENT '충전소 고유 ID (6자리: 000000~999999, 예: 000000)',
    name            VARCHAR(150)    NOT NULL COMMENT '충전소 명칭',
    corp_id         VARCHAR(20)     NOT NULL COMMENT '소속 법인 ID (FK)',
    charger_count   INT             NOT NULL DEFAULT 0 COMMENT '충전기 수량',
    addr            VARCHAR(250)    NULL COMMENT '소재지 주소',
    lat             DECIMAL(10, 7)  NULL COMMENT '위도',
    lng             DECIMAL(10, 7)  NULL COMMENT '경도',
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '상태 (ACTIVE, INSPECTING, CLOSED)',
    reg_dt          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    updt_dt         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (st_id),
    KEY ix_station_corp_id (corp_id),
    CONSTRAINT fk_station_corp FOREIGN KEY (corp_id) REFERENCES corp (corp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='거점 충전소 마스터 (ISO 11179)';

-- ==============================================================================
-- 3. 충전기 자산 마스터 (charger)
-- ==============================================================================
CREATE TABLE charger (
    charge_box_id   VARCHAR(50)     NOT NULL COMMENT 'OCPP ChargeBoxIdentity ({st_id}{cp_id} 8자리, 예: 00000001)',
    st_id           VARCHAR(20)     NOT NULL COMMENT '소속 충전소 ID (FK, 6자리)',
    cp_id           VARCHAR(10)     NOT NULL COMMENT '충전소 내 충전기 번호 2자리 (01~99, 예: 01)',
    vendor          VARCHAR(50)     NOT NULL COMMENT '제조사 (LS E-Link Power, SK Signet 등)',
    model           VARCHAR(50)     NOT NULL COMMENT '모델명 (ELVIS-HPC-350K 등)',
    spec            VARCHAR(50)     NOT NULL COMMENT '정격 사양 (350kW 초급속, 200kW 급속)',
    protocol        VARCHAR(30)     NOT NULL DEFAULT 'OCPP 1.6-J' COMMENT '프로토콜 버전',
    firmware_ver    VARCHAR(50)     NULL COMMENT '펌웨어 버전',
    use_yn          CHAR(1)         NOT NULL DEFAULT 'Y' COMMENT '사용 여부 (Y/N)',
    reg_dt          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    updt_dt         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (charge_box_id),
    KEY ix_charger_st_id (st_id),
    CONSTRAINT fk_charger_station FOREIGN KEY (st_id) REFERENCES station (st_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='충전기 마스터 (ISO 11179)';

-- ==============================================================================
-- 4. 커넥터 실시간 관제 및 상태 원장 (connector_status)
-- ==============================================================================
CREATE TABLE connector_status (
    charge_box_id   VARCHAR(50)     NOT NULL COMMENT '충전기 식별자 (ChargeBoxIdentity)',
    connector_id    INT             NOT NULL DEFAULT 1 COMMENT '커넥터/건 번호 (0=전체기, 1~N=커넥터)',
    status          VARCHAR(32)     NOT NULL DEFAULT '연결안됨' COMMENT '상태 (Available, Charging, 연결안됨 등)',
    power_kw        DECIMAL(8, 2)   NOT NULL DEFAULT 0.0 COMMENT '현재 출력 전력 (kW)',
    voltage_v       DECIMAL(8, 2)   NOT NULL DEFAULT 0.0 COMMENT '현재 출력 전압 (V)',
    current_a       DECIMAL(8, 2)   NOT NULL DEFAULT 0.0 COMMENT '현재 출력 전류 (A)',
    soc_percent     INT             NOT NULL DEFAULT 0 COMMENT '차량 배터리 SoC (%)',
    battery_temp_c  INT             NOT NULL DEFAULT 25 COMMENT '배터리 온도 (°C)',
    car_model       VARCHAR(50)     NULL COMMENT '충전 중 차량 모델',
    user_tag        VARCHAR(50)     NULL COMMENT '사용자 인증 RFID/IdTag',
    accumulated_kwh DECIMAL(10, 2)  NOT NULL DEFAULT 0.0 COMMENT '세션 누적 충전량 (kWh)',
    charging_minutes INT            NOT NULL DEFAULT 0 COMMENT '충전 경과 시간 (분)',
    last_heartbeat  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최종 수신 일시 (관제 화면용)',
    updt_dt         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '최종 갱신 일시',
    PRIMARY KEY (charge_box_id, connector_id),
    KEY ix_conn_status (status),
    CONSTRAINT fk_conn_charger FOREIGN KEY (charge_box_id) REFERENCES charger (charge_box_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='충전기 커넥터 실시간 상태 원장 (ISO 11179)';

-- ==============================================================================
-- 5. 충전 세션 및 과금 원장 (transaction_cdr)
-- ==============================================================================
CREATE TABLE transaction_cdr (
    transaction_id  BIGINT AUTO_INCREMENT NOT NULL COMMENT '정산 트랜잭션 고유번호 (PK)',
    charge_box_id   VARCHAR(50)     NOT NULL COMMENT '충전기 ID',
    connector_id    INT             NOT NULL DEFAULT 1 COMMENT '커넥터 번호',
    user_tag        VARCHAR(50)     NOT NULL COMMENT '회원 RFID / IdTag',
    start_time      DATETIME        NOT NULL COMMENT '충전 시작 일시',
    stop_time       DATETIME        NULL COMMENT '충전 종료 일시',
    meter_start_wh  BIGINT          NOT NULL DEFAULT 0 COMMENT '시작 미터값 (Wh)',
    meter_stop_wh   BIGINT          NULL COMMENT '종료 미터값 (Wh)',
    total_kwh       DECIMAL(10, 2)  NOT NULL DEFAULT 0.0 COMMENT '최종 충전량 (kWh)',
    unit_price      DECIMAL(8, 2)   NOT NULL DEFAULT 288.1 COMMENT '적용 단가 (원/kWh)',
    total_amount    DECIMAL(12, 2)  NOT NULL DEFAULT 0.0 COMMENT '최종 과금액 (원)',
    payment_status  VARCHAR(20)     NOT NULL DEFAULT 'COMPLETED' COMMENT 'COMPLETED, CHARGING, FAILED',
    stop_reason     VARCHAR(50)     NULL COMMENT '종료 사유',
    reg_dt          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    PRIMARY KEY (transaction_id),
    KEY ix_cdr_start_time (start_time),
    KEY ix_cdr_user_tag (user_tag),
    CONSTRAINT fk_cdr_charger FOREIGN KEY (charge_box_id) REFERENCES charger (charge_box_id)
) ENGINE=InnoDB AUTO_INCREMENT=98120 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='충전 과금 원장 (CDR, ISO 11179)';

-- ==============================================================================
-- 6. 충전기 장애 발생 및 복구 이력 원장 (fault_history)
-- ==============================================================================
CREATE TABLE fault_history (
    fault_id            BIGINT AUTO_INCREMENT NOT NULL COMMENT '장애 고유 ID (PK)',
    charge_box_id       VARCHAR(50)     NOT NULL COMMENT '충전기 ID (FK)',
    connector_id        INT             NOT NULL DEFAULT 1 COMMENT '커넥터 번호 (0=본체, 1~N=커넥터)',
    status              VARCHAR(32)     NOT NULL COMMENT '발생 당시 상태 (Faulted 등)',
    error_code          VARCHAR(50)     NOT NULL COMMENT 'OCPP 표준 에러코드 (OverVoltage, GroundFailure 등)',
    vendor_error_code   VARCHAR(50)     NULL COMMENT '제조사 상세 에러코드',
    severity            VARCHAR(20)     NOT NULL DEFAULT 'MAJOR' COMMENT '심각도 (CRITICAL, MAJOR, MINOR, INFO)',
    fault_msg           VARCHAR(500)    NULL COMMENT '상세 장애 설명 메시지',
    occurred_dt         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '장애 발생 일시',
    resolved_dt         DATETIME        NULL COMMENT '장애 복구 일시',
    is_resolved         CHAR(1)         NOT NULL DEFAULT 'N' COMMENT '해결 여부 (Y/N)',
    reg_dt              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    updt_dt             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (fault_id),
    KEY ix_fault_box_conn (charge_box_id, connector_id, is_resolved),
    KEY ix_fault_occurred (occurred_dt),
    KEY ix_fault_severity (severity),
    CONSTRAINT fk_fault_charger FOREIGN KEY (charge_box_id) REFERENCES charger (charge_box_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='충전기 장애 발생 및 복구 이력 원장 (ISO 11179)';

-- ==============================================================================
-- 7. Dead Letter Topic (DLT) 실패 메시지 격리 및 재처리 원장 (dlt_history)
-- ==============================================================================
CREATE TABLE dlt_history (
    dlt_id              VARCHAR(64)     NOT NULL COMMENT 'DLT 고유 식별자 (PK - 예: DLT-20261022-xxxx)',
    charge_box_id       VARCHAR(50)     NULL COMMENT '충전기 ID (파싱 실패 시 null 가능)',
    source_topic        VARCHAR(100)    NOT NULL DEFAULT 'ocpp-raw-events' COMMENT '원본 인바운드 토픽',
    source_partition    INT             NOT NULL DEFAULT 0 COMMENT '원본 토픽 파티션',
    source_offset       BIGINT          NOT NULL DEFAULT 0 COMMENT '원본 토픽 오프셋',
    error_category      VARCHAR(30)     NOT NULL DEFAULT 'NON_RETRYABLE' COMMENT '오류 분류 (NON_RETRYABLE, TRANSIENT, BUSINESS_LOGIC)',
    exception_class     VARCHAR(200)    NULL COMMENT '발생 예외 클래스명',
    error_message       VARCHAR(1000)   NULL COMMENT '오류 메시지 요약',
    raw_payload         LONGTEXT        NOT NULL COMMENT '원본 OCPP 수신 전문',
    retry_count         INT             NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '재처리 상태 (PENDING, REPLAYED, DISCARDED, FAILED)',
    failed_dt           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '최초 실패 일시',
    replayed_dt         DATETIME(3)     NULL COMMENT '재처리 완료 일시',
    reg_dt              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '등록일시',
    updt_dt             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정일시',
    PRIMARY KEY (dlt_id),
    KEY ix_dlt_box (charge_box_id),
    KEY ix_dlt_status (status),
    KEY ix_dlt_failed (failed_dt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dead Letter Topic 실패 메시지 격리 및 재처리 원장 (ISO 11179)';

-- ==============================================================================
-- 8. 관리자 및 운영자 계정 테이블 (admin_user)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS admin_user (
    admin_id        BIGINT AUTO_INCREMENT COMMENT '관리자 고유 PK',
    username        VARCHAR(50)     NOT NULL COMMENT '로그인 계정 ID (admin, operator 등)',
    password        VARCHAR(255)    NOT NULL COMMENT 'BCrypt 암호화 해시 비밀번호',
    name            VARCHAR(50)     NOT NULL COMMENT '관리자 성명/직함',
    role            VARCHAR(30)     NOT NULL COMMENT '접근 권한 (ROLE_ADMIN, ROLE_OPERATOR, ROLE_VIEWER)',
    enabled         TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '계정 활성 여부 (1: 사용, 0: 차단)',
    last_login_at   DATETIME        NULL COMMENT '최종 로그인 일시',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (admin_id),
    UNIQUE KEY uq_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='관리자 계정 원장 (ISO 11179)';

-- ==============================================================================
-- 9. 레거시(tb_*, tbl_*) 100% 하위 호환 뷰 생성 (과도기 안전장치)
-- ==============================================================================
CREATE OR REPLACE VIEW tb_corp AS SELECT * FROM corp;
CREATE OR REPLACE VIEW tbl_corp AS SELECT * FROM corp;

CREATE OR REPLACE VIEW tb_station AS SELECT * FROM station;
CREATE OR REPLACE VIEW tbl_station AS SELECT * FROM station;

CREATE OR REPLACE VIEW tb_charger AS SELECT * FROM charger;
CREATE OR REPLACE VIEW tbl_charger AS SELECT * FROM charger;

CREATE OR REPLACE VIEW tb_connector_status AS SELECT * FROM connector_status;
CREATE OR REPLACE VIEW tbl_connector_status AS SELECT * FROM connector_status;

CREATE OR REPLACE VIEW tb_transaction_cdr AS SELECT * FROM transaction_cdr;
CREATE OR REPLACE VIEW tbl_transaction_cdr AS SELECT * FROM transaction_cdr;

CREATE OR REPLACE VIEW tb_fault_history AS SELECT * FROM fault_history;
CREATE OR REPLACE VIEW tbl_fault_history AS SELECT * FROM fault_history;

CREATE OR REPLACE VIEW tb_dlt_history AS SELECT * FROM dlt_history;
CREATE OR REPLACE VIEW tbl_dlt_history AS SELECT * FROM dlt_history;

CREATE OR REPLACE VIEW tb_admin_user AS SELECT * FROM admin_user;
CREATE OR REPLACE VIEW tbl_admin_user AS SELECT * FROM admin_user;
