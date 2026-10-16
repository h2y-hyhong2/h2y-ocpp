-- ==============================================================================
-- ELVIS-CSMS 관제 플랫폼 기준정보 (Master Data) 등록 스크립트
-- 파일: config/mysql/02_seed_data.sql
-- 채번규칙: 충전소 ST_ID (000000 6자리), 충전기 CP_ID (01 2자리) -> CHARGE_BOX_ID: 00000001 (8자리)
-- ==============================================================================

USE `elvis_lite`;

-- 외래키 제약조건 잠시 해제 및 클린 초기화 (기존 데이터 전부 삭제)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE tbl_transaction_cdr;
TRUNCATE TABLE tbl_connector_status;
TRUNCATE TABLE tbl_charger;
TRUNCATE TABLE tbl_station;
TRUNCATE TABLE tbl_corp;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. 운영 법인 기준정보 등록 (tbl_corp: 1건)
INSERT INTO tbl_corp (CORP_ID, CORP_NAME, SHORT_NAME, TAG, COLOR_CLASS, BADGE)
VALUES 
('CORP_001', '(주)LS E-Link (본사 직영)', 'LS E-Link', '직영', 'sky', '1개소');

-- 2. 거점 충전소 기준정보 등록 (tbl_station: 000000 채번 1건)
INSERT INTO tbl_station (ST_ID, NAME, CORP_ID, CHARGER_COUNT, ADDR, LAT, LNG, STATUS, REG_DT, UPDT_DT)
VALUES 
('000000', '서울 강남 테헤란로 하이퍼차징 스테이션', 'CORP_001', 1, '서울특별시 강남구 테헤란로 152', 37.5000, 127.0365, 'ACTIVE', NOW(), NOW());

-- 3. 충전기 자산 마스터 기준정보 등록 (tbl_charger: 채번규칙 {ST_ID}{CP_ID} -> 00000001 1대)
INSERT INTO tbl_charger (CHARGE_BOX_ID, ST_ID, CP_ID, VENDOR, MODEL, SPEC, PROTOCOL, USE_YN)
VALUES
('00000001', '000000', '01', 'LS E-Link Power', 'ELVIS-HPC-350K', '350kW 초급속', 'OCPP 1.6-J', 'Y');

-- 4. 커넥터 실시간 관제 기준 초기 상태 등록 (충전기 1대 클린 초기화: '연결안됨', 출력 0kW, SoC 0%)
INSERT INTO tbl_connector_status 
(CHARGE_BOX_ID, CONNECTOR_ID, STATUS, POWER_KW, VOLTAGE_V, CURRENT_A, SOC_PERCENT, BATTERY_TEMP_C, CAR_MODEL, USER_TAG, ACCUMULATED_KWH, CHARGING_MINUTES, LAST_HEARTBEAT)
VALUES
('00000001', 1, '연결안됨', 0.00, 0.00, 0.00, 0, 25, NULL, NULL, 0.00, 0, NOW());

-- 5. tb_* 양방향 호환 VIEW 생성
CREATE OR REPLACE VIEW tb_corp AS SELECT * FROM tbl_corp;
CREATE OR REPLACE VIEW tb_station AS SELECT * FROM tbl_station;
CREATE OR REPLACE VIEW tb_charger AS SELECT * FROM tbl_charger;
CREATE OR REPLACE VIEW tb_connector_status AS SELECT * FROM tbl_connector_status;
CREATE OR REPLACE VIEW tb_transaction_cdr AS SELECT * FROM tbl_transaction_cdr;