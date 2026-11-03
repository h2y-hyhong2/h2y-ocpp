-- ==============================================================================
-- ELVIS-CSMS 관제 플랫폼 기준정보 (Master Data) 등록 스크립트 (ISO 11179 표준)
-- 파일: config/mysql/02_seed_data.sql
-- 채번규칙: 충전소 ST_ID (000000 6자리), 충전기 CP_ID (01 2자리) -> CHARGE_BOX_ID: 00000001 (8자리)
-- 커넥터 규칙: 듀얼건 기본 2개 (커넥터 ID 1, 2)
-- ==============================================================================

USE `elvis_lite`;

-- 외래키 제약조건 잠시 해제 및 클린 초기화 (기존 데이터 전부 삭제)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE transaction_cdr;
TRUNCATE TABLE connector_status;
TRUNCATE TABLE charger;
TRUNCATE TABLE station;
TRUNCATE TABLE corp;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. 운영 법인 기준정보 등록 (corp: 1건)
INSERT INTO corp (corp_id, corp_name, short_name, tag, color_class, badge)
VALUES 
('CORP_001', '(주)LS E-Link (본사 직영)', 'LS E-Link', '직영', 'sky', '1개소');

-- 2. 거점 충전소 기준정보 등록 (station: 000000 채번 1건)
INSERT INTO station (st_id, name, corp_id, charger_count, addr, lat, lng, status, reg_dt, updt_dt)
VALUES 
('000000', '서울 강남 테헤란로 하이퍼차징 스테이션', 'CORP_001', 1, '서울특별시 강남구 테헤란로 152', 37.5000, 127.0365, 'ACTIVE', NOW(), NOW());

-- 3. 충전기 자산 마스터 기준정보 등록 (charger: 채번규칙 {st_id}{cp_id} -> 00000001 1대)
INSERT INTO charger (charge_box_id, st_id, cp_id, vendor, model, spec, protocol, use_yn)
VALUES
('00000001', '000000', '01', 'LS E-Link Power', 'ELVIS-HPC-350K', '350kW 초급속', 'OCPP 1.6-J', 'Y');

-- 4. 커넥터 실시간 관제 기준 초기 상태 등록 (듀얼건 기본 2개: 커넥터 ID 1, 2)
INSERT INTO connector_status 
(charge_box_id, connector_id, status, power_kw, voltage_v, current_a, soc_percent, battery_temp_c, car_model, user_tag, accumulated_kwh, charging_minutes, last_heartbeat)
VALUES
('00000001', 1, '연결안됨', 0.00, 0.00, 0.00, 0, 25, NULL, NULL, 0.00, 0, NOW()),
('00000001', 2, '연결안됨', 0.00, 0.00, 0.00, 0, 25, NULL, NULL, 0.00, 0, NOW());
