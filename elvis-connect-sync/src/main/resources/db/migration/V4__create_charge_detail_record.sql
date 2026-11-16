-- ============================================================
-- V4: elvis_lite 요금 정책 및 충전 과금 원장(CDR) 테이블 생성 (ISO 11179)
-- 과제: [3368] CSMS Lite 버전 (POC) | Sprint: S21-SYNC-0080-DE/CO
-- ============================================================

-- 1. 요금 정책 마스터 테이블
CREATE TABLE IF NOT EXISTS pricing_policy (
    policy_id           VARCHAR(32)     NOT NULL                    COMMENT 'PK - 요금 정책 식별자 (DEFAULT_TOU, VIP_MEMBER 등)',
    policy_name         VARCHAR(64)     NOT NULL                    COMMENT '요금 정책 명칭',
    policy_type         VARCHAR(32)     NOT NULL DEFAULT 'TOU'      COMMENT '정책 유형 (TOU: 계절/시간대별, FLAT: 단일고정, DYNAMIC: 동적단가)',

    basic_fee           DECIMAL(12, 2)  NOT NULL DEFAULT 0.00       COMMENT '기본 요금 또는 충전 접속료 (원)',
    free_minutes        INT             NOT NULL DEFAULT 0          COMMENT '무료 충전 제공 시간 (분 단위)',
    penalty_per_minute  DECIMAL(12, 2)  NOT NULL DEFAULT 0.00       COMMENT '완충 후 미출차 점유 페널티 단가 (원/분)',

    is_active           TINYINT(1)      NOT NULL DEFAULT 1          COMMENT '활성 여부 (1: 활성, 0: 비활성)',
    description         VARCHAR(255)        NULL DEFAULT NULL       COMMENT '정책 상세 설명',

    reg_dt              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '등록 일시',
    updt_dt             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3)              COMMENT '수정 일시',

    PRIMARY KEY (policy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='전기차 충전 요금 정책 마스터 원장 (ISO 11179)';


-- 2. TOU 계절/요일/시간대별 단가 상세 규칙 테이블
CREATE TABLE IF NOT EXISTS pricing_tou_rule (
    id                  BIGINT          NOT NULL AUTO_INCREMENT     COMMENT 'PK - TOU 규칙 일련번호',
    policy_id           VARCHAR(32)     NOT NULL                    COMMENT 'FK - 요금 정책 식별자 (pricing_policy)',

    season_type         VARCHAR(16)     NOT NULL                    COMMENT '계절 구분 (SPRING_AUTUMN, SUMMER, WINTER)',
    day_type            VARCHAR(16)     NOT NULL                    COMMENT '요일 구분 (WEEKDAY: 주중, SATURDAY: 토요일, HOLIDAY: 일/공휴일)',
    load_type           VARCHAR(16)     NOT NULL                    COMMENT '부하 구분 (OFF_PEAK: 경부하, MID_PEAK: 중부하, ON_PEAK: 최대부하)',

    start_hour          INT             NOT NULL                    COMMENT '적용 시작 시 (0 ~ 23)',
    end_hour            INT             NOT NULL                    COMMENT '적용 종료 시 (1 ~ 24, 24는 자정)',

    unit_price          DECIMAL(12, 2)  NOT NULL                    COMMENT '전력량 단가 (원/kWh)',

    reg_dt              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '등록 일시',

    PRIMARY KEY (id),

    -- 정책별 조건 탐색 복합 인덱스
    INDEX ix_tou_rule_lookup (policy_id, season_type, day_type, start_hour, end_hour)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='TOU 계절/요일/시간대별 단가 상세 규칙 (ISO 11179)';


-- 3. 충전 과금 원장 (CDR: Charge Detail Record) 테이블
CREATE TABLE IF NOT EXISTS charge_detail_record (
    id                  BIGINT          NOT NULL AUTO_INCREMENT     COMMENT 'PK - 과금 원장 일련번호',
    cdr_no              VARCHAR(64)     NOT NULL                    COMMENT '과금 영수증 번호 (고유키: CDR-YYYYMMDD-CBID-TRNID)',

    charge_box_id       VARCHAR(50)     NOT NULL                    COMMENT '충전기 식별자 (8자리 정규화)',
    connector_id        INT             NOT NULL DEFAULT 1          COMMENT '충전 커넥터 번호 (1: C1, 2: C2)',
    transaction_id      BIGINT          NOT NULL                    COMMENT 'OCPP 거래 번호 (전역 단일 시퀀스)',
    session_id          BIGINT              NULL DEFAULT NULL       COMMENT '연관 세션 일련번호 (transaction_session.id)',

    id_tag              VARCHAR(64)     NOT NULL                    COMMENT '인증/결제 회원 태그 식별자',
    parent_id_tag       VARCHAR(64)         NULL DEFAULT NULL       COMMENT '법인/그룹 상위 대표 태그 식별자',

    start_time          DATETIME(3)     NOT NULL                    COMMENT '충전 개시 일시',
    stop_time           DATETIME(3)     NOT NULL                    COMMENT '충전 종료 일시',
    charging_seconds    BIGINT          NOT NULL DEFAULT 0          COMMENT '총 충전 소요 초 (Seconds)',

    start_meter_wh      BIGINT          NOT NULL DEFAULT 0          COMMENT '충전 시작 미터값 (Wh)',
    stop_meter_wh       BIGINT          NOT NULL DEFAULT 0          COMMENT '충전 종료 미터값 (Wh)',
    total_energy_kwh    DECIMAL(10, 3)  NOT NULL DEFAULT 0.000      COMMENT '총 충전 전력량 (kWh)',

    policy_id           VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT_TOU' COMMENT '적용된 요금 정책 식별자',

    energy_fee          DECIMAL(12, 2)  NOT NULL DEFAULT 0.00       COMMENT '전력량 요금 합계 (세전, 원)',
    basic_fee           DECIMAL(12, 2)  NOT NULL DEFAULT 0.00       COMMENT '기본 요금 및 점유 페널티 (원)',
    discount_fee        DECIMAL(12, 2)  NOT NULL DEFAULT 0.00       COMMENT '회원/제휴 할인 금액 (원)',
    vat                 DECIMAL(12, 2)  NOT NULL DEFAULT 0.00       COMMENT '부가가치세 (10%, 원단위 사사오입)',
    total_amount        DECIMAL(12, 2)  NOT NULL DEFAULT 0.00       COMMENT '최종 결제 청구 금액 (ENERGY+BASIC-DISCOUNT+VAT, 원)',

    payment_status      VARCHAR(32)     NOT NULL DEFAULT 'PENDING'  COMMENT '결제 상태 (PENDING, PAID, FAILED, CANCELLED)',
    breakdown_json      JSON                NULL DEFAULT NULL       COMMENT '시간대별(경/중/최대) 사용량 및 단가 분할 JSON',

    reg_dt              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '원장 생성 일시',
    updt_dt             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3)              COMMENT '원장 갱신 일시',

    PRIMARY KEY (id),

    -- 영수증 고유 번호 유니크 제약
    UNIQUE KEY uk_cdr_no (cdr_no),

    -- 멱등성 보장 유니크 복합 인덱스 (동일 트랜잭션 중복 과금 원천 차단)
    UNIQUE KEY uk_cdr_charge_box_trn (charge_box_id, transaction_id),

    -- 일자별 정산 및 매출 집계 인덱스
    INDEX ix_cdr_stop_time (stop_time),

    -- 회원별 충전 이력 및 과금 조회 인덱스
    INDEX ix_cdr_id_tag (id_tag),

    -- 세션 역방향 조인 인덱스
    INDEX ix_cdr_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='충전 과금 원장 (CDR: Charge Detail Record, ISO 11179)';


-- 4. 한전/환경부 표준 기본 TOU 요금 정책 및 단가 시드 데이터
INSERT INTO pricing_policy (policy_id, policy_name, policy_type, basic_fee, free_minutes, penalty_per_minute, is_active, description)
VALUES ('DEFAULT_TOU', '한전 표준 계절/시간대별 요금제 (기본)', 'TOU', 0.00, 0, 100.00, 1, '한전 계절별/시간대별 3단계 차등 요금제 (급속/완속 공통 표준)')
ON DUPLICATE KEY UPDATE policy_name = VALUES(policy_name);

-- 주중(WEEKDAY) - 봄/가을 (SPRING_AUTUMN: 3~6월, 9~10월)
INSERT INTO pricing_tou_rule (policy_id, season_type, day_type, load_type, start_hour, end_hour, unit_price) VALUES
('DEFAULT_TOU', 'SPRING_AUTUMN', 'WEEKDAY', 'OFF_PEAK', 0, 9, 195.80),
('DEFAULT_TOU', 'SPRING_AUTUMN', 'WEEKDAY', 'MID_PEAK', 9, 10, 255.70),
('DEFAULT_TOU', 'SPRING_AUTUMN', 'WEEKDAY', 'ON_PEAK', 10, 12, 298.50),
('DEFAULT_TOU', 'SPRING_AUTUMN', 'WEEKDAY', 'MID_PEAK', 12, 13, 255.70),
('DEFAULT_TOU', 'SPRING_AUTUMN', 'WEEKDAY', 'ON_PEAK', 13, 17, 298.50),
('DEFAULT_TOU', 'SPRING_AUTUMN', 'WEEKDAY', 'MID_PEAK', 17, 23, 255.70),
('DEFAULT_TOU', 'SPRING_AUTUMN', 'WEEKDAY', 'OFF_PEAK', 23, 24, 195.80);

-- 주중(WEEKDAY) - 여름 (SUMMER: 7~8월)
INSERT INTO pricing_tou_rule (policy_id, season_type, day_type, load_type, start_hour, end_hour, unit_price) VALUES
('DEFAULT_TOU', 'SUMMER', 'WEEKDAY', 'OFF_PEAK', 0, 9, 198.50),
('DEFAULT_TOU', 'SUMMER', 'WEEKDAY', 'MID_PEAK', 9, 10, 285.20),
('DEFAULT_TOU', 'SUMMER', 'WEEKDAY', 'ON_PEAK', 10, 12, 347.20),
('DEFAULT_TOU', 'SUMMER', 'WEEKDAY', 'MID_PEAK', 12, 13, 285.20),
('DEFAULT_TOU', 'SUMMER', 'WEEKDAY', 'ON_PEAK', 13, 17, 347.20),
('DEFAULT_TOU', 'SUMMER', 'WEEKDAY', 'MID_PEAK', 17, 23, 285.20),
('DEFAULT_TOU', 'SUMMER', 'WEEKDAY', 'OFF_PEAK', 23, 24, 198.50);

-- 주중(WEEKDAY) - 겨울 (WINTER: 11~2월)
INSERT INTO pricing_tou_rule (policy_id, season_type, day_type, load_type, start_hour, end_hour, unit_price) VALUES
('DEFAULT_TOU', 'WINTER', 'WEEKDAY', 'OFF_PEAK', 0, 9, 203.20),
('DEFAULT_TOU', 'WINTER', 'WEEKDAY', 'MID_PEAK', 9, 10, 278.40),
('DEFAULT_TOU', 'WINTER', 'WEEKDAY', 'ON_PEAK', 10, 12, 325.60),
('DEFAULT_TOU', 'WINTER', 'WEEKDAY', 'MID_PEAK', 12, 17, 278.40),
('DEFAULT_TOU', 'WINTER', 'WEEKDAY', 'ON_PEAK', 17, 20, 325.60),
('DEFAULT_TOU', 'WINTER', 'WEEKDAY', 'MID_PEAK', 20, 22, 278.40),
('DEFAULT_TOU', 'WINTER', 'WEEKDAY', 'ON_PEAK', 22, 23, 325.60),
('DEFAULT_TOU', 'WINTER', 'WEEKDAY', 'OFF_PEAK', 23, 24, 203.20);

-- 주말/공휴일(SATURDAY, HOLIDAY) - 전 계절 경부하/중부하 위주 완화 단가
INSERT INTO pricing_tou_rule (policy_id, season_type, day_type, load_type, start_hour, end_hour, unit_price) VALUES
('DEFAULT_TOU', 'SPRING_AUTUMN', 'SATURDAY', 'OFF_PEAK', 0, 9, 195.80),
('DEFAULT_TOU', 'SPRING_AUTUMN', 'SATURDAY', 'MID_PEAK', 9, 23, 235.00),
('DEFAULT_TOU', 'SPRING_AUTUMN', 'SATURDAY', 'OFF_PEAK', 23, 24, 195.80),
('DEFAULT_TOU', 'SPRING_AUTUMN', 'HOLIDAY', 'OFF_PEAK', 0, 24, 195.80),

('DEFAULT_TOU', 'SUMMER', 'SATURDAY', 'OFF_PEAK', 0, 9, 198.50),
('DEFAULT_TOU', 'SUMMER', 'SATURDAY', 'MID_PEAK', 9, 23, 255.00),
('DEFAULT_TOU', 'SUMMER', 'SATURDAY', 'OFF_PEAK', 23, 24, 198.50),
('DEFAULT_TOU', 'SUMMER', 'HOLIDAY', 'OFF_PEAK', 0, 24, 198.50),

('DEFAULT_TOU', 'WINTER', 'SATURDAY', 'OFF_PEAK', 0, 9, 203.20),
('DEFAULT_TOU', 'WINTER', 'SATURDAY', 'MID_PEAK', 9, 23, 260.00),
('DEFAULT_TOU', 'WINTER', 'SATURDAY', 'OFF_PEAK', 23, 24, 203.20),
('DEFAULT_TOU', 'WINTER', 'HOLIDAY', 'OFF_PEAK', 0, 24, 203.20);

-- 레거시(tb_*, tbl_*) 하위 호환 뷰 생성
CREATE OR REPLACE VIEW tb_pricing_policy AS SELECT * FROM pricing_policy;
CREATE OR REPLACE VIEW tbl_pricing_policy AS SELECT * FROM pricing_policy;
CREATE OR REPLACE VIEW tb_pricing_tou_rule AS SELECT * FROM pricing_tou_rule;
CREATE OR REPLACE VIEW tbl_pricing_tou_rule AS SELECT * FROM pricing_tou_rule;
CREATE OR REPLACE VIEW tb_charge_detail_record AS SELECT * FROM charge_detail_record;
CREATE OR REPLACE VIEW tbl_charge_detail_record AS SELECT * FROM charge_detail_record;
