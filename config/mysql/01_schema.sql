-- ==============================================================================
-- ELVIS-CSMS 관제 플랫폼 MySQL 9.71+ DDL 스키마 정의서 (ISO 11179 표준)
-- 파일: config/mysql/01_schema.sql
-- 설명: ISO 11179 글로벌 명명 표준(No-Prefix, lower_snake_case, 단수형 명사)
--       elvis-control-api 및 elvis-connect-sync(Flyway V1~V6) 통합 스키마
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
-- 기존 뷰 및 테이블 정리 (구버전 tb_*, tbl_* 물리 테이블 포함 안전 정리)
-- ==============================================================================
SET FOREIGN_KEY_CHECKS = 0;

DELIMITER $$
DROP PROCEDURE IF EXISTS `sp_drop_legacy_objects`$$
CREATE PROCEDURE `sp_drop_legacy_objects`()
BEGIN
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_name VARCHAR(128);
    DECLARE v_type VARCHAR(32);
    DECLARE cur CURSOR FOR
        SELECT TABLE_NAME, TABLE_TYPE
          FROM INFORMATION_SCHEMA.TABLES
         WHERE TABLE_SCHEMA = 'elvis_lite'
           AND TABLE_NAME <> 'admin_user';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_name, v_type;
        IF v_done = 1 THEN
            LEAVE read_loop;
        END IF;

        IF v_type = 'VIEW' THEN
            SET @sql_stmt = CONCAT('DROP VIEW IF EXISTS `elvis_lite`.`', v_name, '`');
        ELSE
            SET @sql_stmt = CONCAT('DROP TABLE IF EXISTS `elvis_lite`.`', v_name, '`');
        END IF;
        PREPARE stmt FROM @sql_stmt;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

CALL sp_drop_legacy_objects();
DROP PROCEDURE IF EXISTS sp_drop_legacy_objects;

DROP TABLE IF EXISTS flyway_schema_history;
DROP TABLE IF EXISTS dlt_history;
DROP TABLE IF EXISTS fault_history;
DROP TABLE IF EXISTS charge_detail_record;
DROP TABLE IF EXISTS pricing_tou_rule;
DROP TABLE IF EXISTS pricing_policy;
DROP TABLE IF EXISTS auth_hist;
DROP TABLE IF EXISTS rfid_card;
DROP TABLE IF EXISTS transaction_session;
DROP TABLE IF EXISTS transaction_cdr;
DROP TABLE IF EXISTS connector_status;
DROP TABLE IF EXISTS charger;
DROP TABLE IF EXISTS station;
DROP TABLE IF EXISTS corp;

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
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'PK - 자동증가',
    charge_box_id   VARCHAR(50)     NOT NULL COMMENT '충전기 식별자 (ChargeBoxIdentity)',
    connector_id    INT             NOT NULL DEFAULT 1 COMMENT '커넥터/건 번호 (0=전체기, 1~N=커넥터)',
    status          VARCHAR(32)     NOT NULL DEFAULT '연결안됨' COMMENT '상태 (Available, Charging, 연결안됨 등)',
    error_code      VARCHAR(32)     NULL DEFAULT NULL COMMENT 'OCPP ErrorCode (NoError/OtherError 등)',
    vendor_id       VARCHAR(128)    NULL DEFAULT NULL COMMENT 'StatusNotification vendorId',
    info            VARCHAR(255)    NULL DEFAULT NULL COMMENT 'StatusNotification info 메시지',
    power_kw        DECIMAL(8, 2)   NOT NULL DEFAULT 0.00 COMMENT '현재 출력 전력 (kW)',
    voltage_v       DECIMAL(8, 2)   NOT NULL DEFAULT 0.00 COMMENT '현재 출력 전압 (V)',
    current_a       DECIMAL(8, 2)   NOT NULL DEFAULT 0.00 COMMENT '현재 출력 전류 (A)',
    soc_percent     INT             NOT NULL DEFAULT 0 COMMENT '차량 배터리 SoC (%)',
    battery_temp_c  INT             NOT NULL DEFAULT 25 COMMENT '배터리 온도 (°C)',
    car_model       VARCHAR(50)     NULL COMMENT '충전 중 차량 모델',
    user_tag        VARCHAR(50)     NULL COMMENT '사용자 인증 RFID/IdTag',
    accumulated_kwh DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '세션 누적 충전량 (kWh)',
    charging_minutes INT            NOT NULL DEFAULT 0 COMMENT '충전 경과 시간 (분)',
    last_heartbeat  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '최종 수신 일시 (관제 화면용)',
    heartbeat_at    DATETIME(3)     NULL DEFAULT NULL COMMENT '최근 Heartbeat 수신 일시 (3ms 정밀도)',
    status_at       DATETIME(3)     NULL DEFAULT NULL COMMENT '최근 StatusNotification 수신 일시',
    version         INT             NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전 컬럼',
    reg_dt          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '최초 등록 일시',
    updt_dt         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '최종 갱신 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_conn_charge_box_connector (charge_box_id, connector_id),
    KEY ix_conn_status (status),
    KEY ix_conn_heartbeat_at (heartbeat_at),
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
    total_kwh       DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '최종 충전량 (kWh)',
    unit_price      DECIMAL(8, 2)   NOT NULL DEFAULT 288.10 COMMENT '적용 단가 (원/kWh)',
    total_amount    DECIMAL(12, 2)  NOT NULL DEFAULT 0.00 COMMENT '최종 과금액 (원)',
    payment_status  VARCHAR(20)     NOT NULL DEFAULT 'COMPLETED' COMMENT 'COMPLETED, CHARGING, FAILED',
    stop_reason     VARCHAR(50)     NULL COMMENT '종료 사유',
    reg_dt          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    PRIMARY KEY (transaction_id),
    KEY ix_cdr_start_time (start_time),
    KEY ix_cdr_user_tag (user_tag),
    CONSTRAINT fk_cdr_charger FOREIGN KEY (charge_box_id) REFERENCES charger (charge_box_id)
) ENGINE=InnoDB AUTO_INCREMENT=98120 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='충전 과금 원장 (CDR, ISO 11179)';

-- ==============================================================================
-- 6. 충전 거래 세션 관리 원장 (transaction_session)
-- ==============================================================================
CREATE TABLE transaction_session (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'PK - 세션 내부 고유 일련번호',
    charge_box_id       VARCHAR(50)     NOT NULL COMMENT '충전기 식별자 (ChargeBoxIdentity)',
    connector_id        INT             NOT NULL DEFAULT 1 COMMENT '충전 건/커넥터 번호 (1~N)',
    transaction_id      BIGINT          NOT NULL COMMENT 'OCPP 거래 번호 (TransactionId)',
    id_tag              VARCHAR(64)     NOT NULL COMMENT '회원 인증 식별자 (RFID / IdTag)',
    parent_id_tag       VARCHAR(64)     NULL DEFAULT NULL COMMENT '상위/법인 회원 식별자 (ParentIdTag)',
    status              VARCHAR(32)     NOT NULL DEFAULT 'STARTED' COMMENT '세션 상태 (STARTED, CHARGING, STOPPED, COMPLETED, ABORTED, FORCE_CLOSED)',
    meter_start         BIGINT          NOT NULL DEFAULT 0 COMMENT '충전 시작 미터값 (Wh)',
    meter_stop          BIGINT          NULL DEFAULT NULL COMMENT '충전 종료 미터값 (Wh)',
    total_energy_kwh    DECIMAL(10, 3)  NOT NULL DEFAULT 0.000 COMMENT '최종 충전 전력량 (kWh, (meterStop-meterStart)/1000)',
    start_time          DATETIME(3)     NOT NULL COMMENT '충전 시작 일시 (단말 타임스탬프 기준)',
    stop_time           DATETIME(3)     NULL DEFAULT NULL COMMENT '충전 종료 일시 (단말 타임스탬프 기준)',
    charging_minutes    INT             NOT NULL DEFAULT 0 COMMENT '총 충전 경과 시간 (분 단위)',
    stop_reason         VARCHAR(64)     NULL DEFAULT NULL COMMENT 'OCPP 종료 사유 (EmergencyStop, EVDisconnected 등)',
    version             INT             NOT NULL DEFAULT 0 COMMENT 'JPA 낙관적 락 버전 컬럼',
    reg_dt              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '최초 세션 생성 일시',
    updt_dt             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '최종 세션 갱신 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_charge_box_trn (charge_box_id, transaction_id),
    KEY ix_session_charger_conn_status (charge_box_id, connector_id, status),
    KEY ix_session_start_time (start_time),
    KEY ix_session_id_tag (id_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='충전 거래 세션 관리 원장 (ISO 11179)';

-- ==============================================================================
-- 7. 회원 RFID 카드 마스터 원장 (rfid_card)
-- ==============================================================================
CREATE TABLE rfid_card (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'PK - RFID 카드 고유 일련번호',
    id_tag              VARCHAR(64)     NOT NULL COMMENT 'RFID 태그 식별자 (대문자 정규화, 고유키)',
    member_id           BIGINT          NOT NULL COMMENT '회원 식별자',
    member_name         VARCHAR(64)     NULL DEFAULT NULL COMMENT '회원명 (마스킹 표시용)',
    parent_id_tag       VARCHAR(64)     NULL DEFAULT NULL COMMENT '상위/법인 대표 태그 식별자 (선택)',
    card_status         VARCHAR(32)     NOT NULL DEFAULT 'NORMAL' COMMENT '카드 상태 (NORMAL, BLOCKED, LOST, SUSPENDED)',
    expiry_date         DATETIME(3)     NULL DEFAULT NULL COMMENT '카드 유효기간 만료일시 (UTC 기준)',
    balance             DECIMAL(12, 2)  NOT NULL DEFAULT 0.00 COMMENT '선불 잔액 또는 결제 한도 (원)',
    max_concurrent_tx   INT             NOT NULL DEFAULT 1 COMMENT '동시 충전 허용 세션 수 (기본 1개)',
    is_deleted          TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '삭제 여부 (0:활성, 1:삭제)',
    version             INT             NOT NULL DEFAULT 0 COMMENT 'JPA 낙관적 락 버전 컬럼',
    reg_dt              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '최초 등록 일시',
    updt_dt             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '최종 수정 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rfid_card_id_tag (id_tag),
    KEY ix_rfid_card_member (member_id, card_status),
    KEY ix_rfid_card_parent (parent_id_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 RFID 카드 마스터 원장 (ISO 11179)';

-- ==============================================================================
-- 8. 회원 RFID 인증 감사 이력 (auth_hist)
-- ==============================================================================
CREATE TABLE auth_hist (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'PK - 인증 이력 일련번호',
    charge_box_id       VARCHAR(50)     NOT NULL COMMENT '요청 충전기 식별자',
    message_id          VARCHAR(64)     NOT NULL COMMENT 'OCPP MessageId (UUID/식별자)',
    id_tag              VARCHAR(64)     NOT NULL COMMENT '요청 RFID 태그 식별자',
    auth_status         VARCHAR(32)     NOT NULL COMMENT '인증 판정 결과 (Accepted, Blocked, Expired, Invalid, ConcurrentTx)',
    parent_id_tag       VARCHAR(64)     NULL DEFAULT NULL COMMENT '응답된 상위 태그 식별자',
    fail_reason         VARCHAR(128)    NULL DEFAULT NULL COMMENT '인증 실패/차단 상세 사유',
    cache_tier          VARCHAR(16)     NOT NULL DEFAULT 'L2_REDIS' COMMENT '캐시 적중 계층 (L1_CAFFEINE, L2_REDIS, L3_DB, MISS)',
    latency_ms          DECIMAL(8, 2)   NOT NULL DEFAULT 0.00 COMMENT '인증 판정 소요 시간 (밀리초)',
    reg_dt              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '인증 요청 수신 일시',
    PRIMARY KEY (id),
    KEY ix_auth_hist_charger_time (charge_box_id, reg_dt),
    KEY ix_auth_hist_id_tag_time (id_tag, reg_dt),
    KEY ix_auth_hist_time (reg_dt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 RFID 인증 감사 이력 (ISO 11179)';

-- ==============================================================================
-- 9. 요금 정책 마스터 원장 (pricing_policy)
-- ==============================================================================
CREATE TABLE pricing_policy (
    policy_id           VARCHAR(32)     NOT NULL COMMENT 'PK - 요금 정책 식별자 (DEFAULT_TOU, VIP_MEMBER 등)',
    policy_name         VARCHAR(64)     NOT NULL COMMENT '요금 정책 명칭',
    policy_type         VARCHAR(32)     NOT NULL DEFAULT 'TOU' COMMENT '정책 유형 (TOU, FLAT, DYNAMIC)',
    basic_fee           DECIMAL(12, 2)  NOT NULL DEFAULT 0.00 COMMENT '기본 요금 또는 충전 접속료 (원)',
    free_minutes        INT             NOT NULL DEFAULT 0 COMMENT '무료 충전 제공 시간 (분 단위)',
    penalty_per_minute  DECIMAL(12, 2)  NOT NULL DEFAULT 0.00 COMMENT '완충 후 미출차 점유 페널티 단가 (원/분)',
    is_active           TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '활성 여부 (1: 활성, 0: 비활성)',
    description         VARCHAR(255)    NULL DEFAULT NULL COMMENT '정책 상세 설명',
    reg_dt              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '등록 일시',
    updt_dt             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 일시',
    PRIMARY KEY (policy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='전기차 충전 요금 정책 마스터 원장 (ISO 11179)';

-- ==============================================================================
-- 10. TOU 계절/요일/시간대별 단가 상세 규칙 (pricing_tou_rule)
-- ==============================================================================
CREATE TABLE pricing_tou_rule (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'PK - TOU 규칙 일련번호',
    policy_id           VARCHAR(32)     NOT NULL COMMENT 'FK - 요금 정책 식별자 (pricing_policy)',
    season_type         VARCHAR(16)     NOT NULL COMMENT '계절 구분 (SPRING_AUTUMN, SUMMER, WINTER)',
    day_type            VARCHAR(16)     NOT NULL COMMENT '요일 구분 (WEEKDAY, SATURDAY, HOLIDAY)',
    load_type           VARCHAR(16)     NOT NULL COMMENT '부하 구분 (OFF_PEAK, MID_PEAK, ON_PEAK)',
    start_hour          INT             NOT NULL COMMENT '적용 시작 시 (0 ~ 23)',
    end_hour            INT             NOT NULL COMMENT '적용 종료 시 (1 ~ 24, 24는 자정)',
    unit_price          DECIMAL(12, 2)  NOT NULL COMMENT '전력량 단가 (원/kWh)',
    reg_dt              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '등록 일시',
    PRIMARY KEY (id),
    KEY ix_tou_rule_lookup (policy_id, season_type, day_type, start_hour, end_hour)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TOU 계절/요일/시간대별 단가 상세 규칙 (ISO 11179)';

-- ==============================================================================
-- 11. 충전 상세 과금 원장 (charge_detail_record)
-- ==============================================================================
CREATE TABLE charge_detail_record (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'PK - 과금 원장 일련번호',
    cdr_no              VARCHAR(64)     NOT NULL COMMENT '과금 영수증 번호 (고유키: CDR-YYYYMMDD-CBID-TRNID)',
    charge_box_id       VARCHAR(50)     NOT NULL COMMENT '충전기 식별자 (8자리 정규화)',
    connector_id        INT             NOT NULL DEFAULT 1 COMMENT '충전 커넥터 번호 (1: C1, 2: C2)',
    transaction_id      BIGINT          NOT NULL COMMENT 'OCPP 거래 번호 (전역 단일 시퀀스)',
    session_id          BIGINT          NULL DEFAULT NULL COMMENT '연관 세션 일련번호 (transaction_session.id)',
    id_tag              VARCHAR(64)     NOT NULL COMMENT '인증/결제 회원 태그 식별자',
    parent_id_tag       VARCHAR(64)     NULL DEFAULT NULL COMMENT '법인/그룹 상위 대표 태그 식별자',
    start_time          DATETIME(3)     NOT NULL COMMENT '충전 개시 일시',
    stop_time           DATETIME(3)     NOT NULL COMMENT '충전 종료 일시',
    charging_seconds    BIGINT          NOT NULL DEFAULT 0 COMMENT '총 충전 소요 초 (Seconds)',
    start_meter_wh      BIGINT          NOT NULL DEFAULT 0 COMMENT '충전 시작 미터값 (Wh)',
    stop_meter_wh       BIGINT          NOT NULL DEFAULT 0 COMMENT '충전 종료 미터값 (Wh)',
    total_energy_kwh    DECIMAL(10, 3)  NOT NULL DEFAULT 0.000 COMMENT '총 충전 전력량 (kWh)',
    policy_id           VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT_TOU' COMMENT '적용된 요금 정책 식별자',
    energy_fee          DECIMAL(12, 2)  NOT NULL DEFAULT 0.00 COMMENT '전력량 요금 합계 (세전, 원)',
    basic_fee           DECIMAL(12, 2)  NOT NULL DEFAULT 0.00 COMMENT '기본 요금 및 점유 페널티 (원)',
    discount_fee        DECIMAL(12, 2)  NOT NULL DEFAULT 0.00 COMMENT '회원/제휴 할인 금액 (원)',
    vat                 DECIMAL(12, 2)  NOT NULL DEFAULT 0.00 COMMENT '부가가치세 (10%, 원단위 사사오입)',
    total_amount        DECIMAL(12, 2)  NOT NULL DEFAULT 0.00 COMMENT '최종 결제 청구 금액 (원)',
    payment_status      VARCHAR(32)     NOT NULL DEFAULT 'PENDING' COMMENT '결제 상태 (PENDING, PAID, FAILED, CANCELLED)',
    breakdown_json      JSON            NULL DEFAULT NULL COMMENT '시간대별(경/중/최대) 사용량 및 단가 분할 JSON',
    reg_dt              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '원장 생성 일시',
    updt_dt             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '원장 갱신 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cdr_no (cdr_no),
    UNIQUE KEY uk_cdr_charge_box_trn (charge_box_id, transaction_id),
    KEY ix_cdr_stop_time (stop_time),
    KEY ix_cdr_id_tag (id_tag),
    KEY ix_cdr_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='충전 상세 과금 원장 (CDR, ISO 11179)';

-- ==============================================================================
-- 12. 충전기 장애 발생 및 복구 이력 원장 (fault_history)
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
    action_required     VARCHAR(50)     NULL DEFAULT 'INSPECTION' COMMENT '권장 조치 가이드 (EMERGENCY_SHUTDOWN, INSPECTION, NONE)',
    occurred_dt         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '장애 발생 일시',
    resolved_dt         DATETIME(3)     NULL COMMENT '장애 복구 일시',
    is_resolved         CHAR(1)         NOT NULL DEFAULT 'N' COMMENT '해결 여부 (Y/N)',
    reg_dt              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '등록일시',
    updt_dt             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정일시',
    PRIMARY KEY (fault_id),
    KEY ix_fault_box_conn (charge_box_id, connector_id, is_resolved),
    KEY ix_fault_occurred (occurred_dt),
    KEY ix_fault_severity (severity),
    CONSTRAINT fk_fault_charger FOREIGN KEY (charge_box_id) REFERENCES charger (charge_box_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='충전기 장애 발생 및 복구 이력 원장 (ISO 11179)';

-- ==============================================================================
-- 13. Dead Letter Topic (DLT) 실패 메시지 격리 및 재처리 원장 (dlt_history)
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
-- 14. 관리자 및 운영자 계정 테이블 (admin_user)
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
-- 15. 레거시(tb_*, tbl_*) 100% 하위 호환 뷰 생성 (과도기 안전장치)
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

CREATE OR REPLACE VIEW tb_transaction_session AS SELECT * FROM transaction_session;
CREATE OR REPLACE VIEW tbl_transaction_session AS SELECT * FROM transaction_session;

CREATE OR REPLACE VIEW tb_rfid_card AS SELECT * FROM rfid_card;
CREATE OR REPLACE VIEW tbl_rfid_card AS SELECT * FROM rfid_card;

CREATE OR REPLACE VIEW tb_auth_hist AS SELECT * FROM auth_hist;
CREATE OR REPLACE VIEW tbl_auth_hist AS SELECT * FROM auth_hist;

CREATE OR REPLACE VIEW tb_pricing_policy AS SELECT * FROM pricing_policy;
CREATE OR REPLACE VIEW tbl_pricing_policy AS SELECT * FROM pricing_policy;

CREATE OR REPLACE VIEW tb_pricing_tou_rule AS SELECT * FROM pricing_tou_rule;
CREATE OR REPLACE VIEW tbl_pricing_tou_rule AS SELECT * FROM pricing_tou_rule;

CREATE OR REPLACE VIEW tb_charge_detail_record AS SELECT * FROM charge_detail_record;
CREATE OR REPLACE VIEW tbl_charge_detail_record AS SELECT * FROM charge_detail_record;

CREATE OR REPLACE VIEW tb_fault_history AS SELECT * FROM fault_history;
CREATE OR REPLACE VIEW tbl_fault_history AS SELECT * FROM fault_history;

CREATE OR REPLACE VIEW tb_dlt_history AS SELECT * FROM dlt_history;
CREATE OR REPLACE VIEW tbl_dlt_history AS SELECT * FROM dlt_history;

CREATE OR REPLACE VIEW tb_admin_user AS SELECT * FROM admin_user;
CREATE OR REPLACE VIEW tbl_admin_user AS SELECT * FROM admin_user;
