-- ==============================================================================
-- ELVIS-CSMS 관제 플랫폼 초기 시드 데이터 (MySQL DML)
-- 파일: config/mysql/02_seed_data.sql
-- ==============================================================================

USE `elvis-lite`;

-- 1. 4대 법인 데이터 등록
INSERT INTO tb_corp (CORP_ID, CORP_NAME, SHORT_NAME, TAG, COLOR_CLASS, BADGE)
VALUES 
('CORP_001', '(주)LS E-Link (본사 직영)', 'LS E-Link', '직영', 'sky', '1개소 (1기)'),
('CORP_002', '서울에너지공사 (공공제휴)', '서울에너지', '공공', 'emerald', '공공제휴'),
('CORP_003', '한국모빌리티네트웍스 (민간CPO)', '모빌리티N', 'CPO', 'indigo', '민간CPO'),
('CORP_004', '경기운수/물류 하이퍼차징', '경기운수', '운수', 'amber', '운수/물류')
ON DUPLICATE KEY UPDATE CORP_NAME=VALUES(CORP_NAME);

-- 2. 거점 충전소 등록 (6자리 표준 ST_ID: 000000 - 1개소 1기 운영)
INSERT INTO tb_station (ST_ID, NAME, CORP_ID, CHARGER_COUNT, ADDR)
VALUES
('000000', '서울 강남 테헤란로 하이퍼차징 스테이션', 'CORP_001', 1, '서울특별시 강남구 테헤란로 152')
ON DUPLICATE KEY UPDATE NAME=VALUES(NAME), CHARGER_COUNT=VALUES(CHARGER_COUNT);

-- 3. 충전기 마스터 등록 (표준 8자리 CHARGE_BOX_ID: {ST_ID}{CP_ID} - 단일 충전기 00000001)
INSERT INTO tb_charger (CHARGE_BOX_ID, ST_ID, CP_ID, VENDOR, MODEL, SPEC, PROTOCOL)
VALUES
('00000001', '000000', '01', 'LS E-Link Power', 'ELVIS-HPC-350K', '350kW 초급속', 'OCPP 1.6-J')
ON DUPLICATE KEY UPDATE VENDOR=VALUES(VENDOR), MODEL=VALUES(MODEL), SPEC=VALUES(SPEC);

-- 4. 커넥터 실시간 관제 상태 등록
INSERT INTO tb_connector_status 
(CHARGE_BOX_ID, CONNECTOR_ID, STATUS, POWER_KW, VOLTAGE_V, CURRENT_A, SOC_PERCENT, BATTERY_TEMP_C, CAR_MODEL, USER_TAG, ACCUMULATED_KWH, CHARGING_MINUTES)
VALUES
('00000001', 1, '충전대기', 0.0, 0.0, 0.0, 0, 24, NULL, NULL, 0.0, 0)
ON DUPLICATE KEY UPDATE STATUS=VALUES(STATUS);

-- 5. 과금 원장(CDR) 시드 데이터 등록
INSERT INTO tb_transaction_cdr 
(TRANSACTION_ID, CHARGE_BOX_ID, CONNECTOR_ID, USER_TAG, START_TIME, STOP_TIME, METER_START_WH, METER_STOP_WH, TOTAL_KWH, UNIT_PRICE, TOTAL_AMOUNT, PAYMENT_STATUS, STOP_REASON)
VALUES
(1, '00000001', 1, 'RFID-192840', '2026-08-20 14:02:00', '2026-08-20 14:45:00', 502000, 550200, 48.20, 288.1, 13886, 'COMPLETED', 'Local')
ON DUPLICATE KEY UPDATE TOTAL_AMOUNT=VALUES(TOTAL_AMOUNT);