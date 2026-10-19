-- ============================================================
-- V4: elvis_lite 요금 정책 및 충전 과금 원장(CDR) 테이블 생성
-- 과제: [3368] CSMS Lite 버전 (POC) | Sprint: S21-SYNC-0080-DE/CO
-- 작성일: 2026-10-19 | 작성자: IT기획 파트
-- ============================================================

-- 1. 요금 정책 마스터 테이블
CREATE TABLE IF NOT EXISTS tb_pricing_policy (
    POLICY_ID           VARCHAR(32)     NOT NULL                    COMMENT 'PK - 요금 정책 식별자 (DEFAULT_TOU, VIP_MEMBER 등)',
    POLICY_NAME         VARCHAR(64)     NOT NULL                    COMMENT '요금 정책 명칭',
    POLICY_TYPE         VARCHAR(32)     NOT NULL DEFAULT 'TOU'      COMMENT '정책 유형 (TOU: 계절/시간대별, FLAT: 단일고정, DYNAMIC: 동적단가)',
    
    BASIC_FEE           DECIMAL(12, 2)  NOT NULL DEFAULT 0.00       COMMENT '기본 요금 또는 충전 접속료 (원)',
    FREE_MINUTES        INT             NOT NULL DEFAULT 0          COMMENT '무료 충전 제공 시간 (분 단위)',
    PENALTY_PER_MINUTE  DECIMAL(12, 2)  NOT NULL DEFAULT 0.00       COMMENT '완충 후 미출차 점유 페널티 단가 (원/분)',
    
    IS_ACTIVE           TINYINT(1)      NOT NULL DEFAULT 1          COMMENT '활성 여부 (1: 활성, 0: 비활성)',
    DESCRIPTION         VARCHAR(255)        NULL DEFAULT NULL       COMMENT '정책 상세 설명',
    
    REG_DT              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '등록 일시',
    UPDT_DT             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3)             COMMENT '수정 일시',

    PRIMARY KEY (POLICY_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='전기차 충전 요금 정책 마스터 원장';


-- 2. TOU 계절/요일/시간대별 단가 상세 규칙 테이블
CREATE TABLE IF NOT EXISTS tb_pricing_tou_rule (
    ID                  BIGINT          NOT NULL AUTO_INCREMENT     COMMENT 'PK - TOU 규칙 일련번호',
    POLICY_ID           VARCHAR(32)     NOT NULL                    COMMENT 'FK - 요금 정책 식별자 (tb_pricing_policy)',
    
    SEASON_TYPE         VARCHAR(16)     NOT NULL                    COMMENT '계절 구분 (SPRING_AUTUMN, SUMMER, WINTER)',
    DAY_TYPE            VARCHAR(16)     NOT NULL                    COMMENT '요일 구분 (WEEKDAY: 주중, SATURDAY: 토요일, HOLIDAY: 일/공휴일)',
    LOAD_TYPE           VARCHAR(16)     NOT NULL                    COMMENT '부하 구분 (OFF_PEAK: 경부하, MID_PEAK: 중부하, ON_PEAK: 최대부하)',
    
    START_HOUR          INT             NOT NULL                    COMMENT '적용 시작 시 (0 ~ 23)',
    END_HOUR            INT             NOT NULL                    COMMENT '적용 종료 시 (1 ~ 24, 24는 자정)',
    
    UNIT_PRICE          DECIMAL(12, 2)  NOT NULL                    COMMENT '전력량 단가 (원/kWh)',
    
    REG_DT              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '등록 일시',

    PRIMARY KEY (ID),
    
    -- 정책별 조건 탐색 복합 인덱스
    INDEX IX_TOU_RULE_LOOKUP (POLICY_ID, SEASON_TYPE, DAY_TYPE, START_HOUR, END_HOUR)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='TOU 계절/요일/시간대별 단가 상세 규칙';


-- 3. 충전 과금 원장 (CDR: Charge Detail Record) 테이블
CREATE TABLE IF NOT EXISTS tb_charge_detail_record (
    ID                  BIGINT          NOT NULL AUTO_INCREMENT     COMMENT 'PK - 과금 원장 일련번호',
    CDR_NO              VARCHAR(48)     NOT NULL                    COMMENT '과금 영수증 번호 (고유키: CDR-YYYYMMDD-CBID-TRNID)',
    
    CHARGE_BOX_ID       VARCHAR(64)     NOT NULL                    COMMENT '충전기 식별자 (8자리 정규화)',
    CONNECTOR_ID        INT             NOT NULL DEFAULT 1          COMMENT '충전 커넥터 번호 (1: C1, 2: C2)',
    TRANSACTION_ID      BIGINT          NOT NULL                    COMMENT 'OCPP 거래 번호 (전역 단일 시퀀스)',
    SESSION_ID          BIGINT              NULL DEFAULT NULL       COMMENT '연관 세션 일련번호 (tb_transaction_session.ID)',
    
    ID_TAG              VARCHAR(64)     NOT NULL                    COMMENT '인증/결제 회원 태그 식별자',
    PARENT_ID_TAG       VARCHAR(64)         NULL DEFAULT NULL       COMMENT '법인/그룹 상위 대표 태그 식별자',
    
    START_TIME          DATETIME(3)     NOT NULL                    COMMENT '충전 개시 일시',
    STOP_TIME           DATETIME(3)     NOT NULL                    COMMENT '충전 종료 일시',
    CHARGING_SECONDS    BIGINT          NOT NULL DEFAULT 0          COMMENT '총 충전 소요 초 (Seconds)',
    
    START_METER_WH      BIGINT          NOT NULL DEFAULT 0          COMMENT '충전 시작 미터값 (Wh)',
    STOP_METER_WH       BIGINT          NOT NULL DEFAULT 0          COMMENT '충전 종료 미터값 (Wh)',
    TOTAL_ENERGY_KWH    DECIMAL(10, 3)  NOT NULL DEFAULT 0.000      COMMENT '총 충전 전력량 (kWh)',
    
    POLICY_ID           VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT_TOU' COMMENT '적용된 요금 정책 식별자',
    
    ENERGY_FEE          DECIMAL(12, 2)  NOT NULL DEFAULT 0.00       COMMENT '전력량 요금 합계 (세전, 원)',
    BASIC_FEE           DECIMAL(12, 2)  NOT NULL DEFAULT 0.00       COMMENT '기본 요금 및 점유 페널티 (원)',
    DISCOUNT_FEE        DECIMAL(12, 2)  NOT NULL DEFAULT 0.00       COMMENT '회원/제휴 할인 금액 (원)',
    VAT                 DECIMAL(12, 2)  NOT NULL DEFAULT 0.00       COMMENT '부가가치세 (10%, 원단위 사사오입)',
    TOTAL_AMOUNT        DECIMAL(12, 2)  NOT NULL DEFAULT 0.00       COMMENT '최종 결제 청구 금액 (ENERGY+BASIC-DISCOUNT+VAT, 원)',
    
    PAYMENT_STATUS      VARCHAR(32)     NOT NULL DEFAULT 'PENDING'  COMMENT '결제 상태 (PENDING, PAID, FAILED, CANCELLED)',
    BREAKDOWN_JSON      JSON                NULL DEFAULT NULL       COMMENT '시간대별(경/중/최대) 사용량 및 단가 분할 JSON',
    
    REG_DT              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '원장 생성 일시',
    UPDT_DT             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3)             COMMENT '원장 갱신 일시',

    PRIMARY KEY (ID),
    
    -- 영수증 고유 번호 유니크 제약
    UNIQUE KEY UK_CDR_NO (CDR_NO),
    
    -- 멱등성 보장 유니크 복합 인덱스 (동일 트랜잭션 중복 과금 원천 차단)
    UNIQUE KEY UK_CDR_CHARGE_BOX_TRN (CHARGE_BOX_ID, TRANSACTION_ID),
    
    -- 일자별 정산 및 매출 집계 인덱스
    INDEX IX_CDR_STOP_TIME (STOP_TIME),
    
    -- 회원별 충전 이력 및 과금 조회 인덱스
    INDEX IX_CDR_ID_TAG (ID_TAG),
    
    -- 세션 역방향 조인 인덱스
    INDEX IX_CDR_SESSION_ID (SESSION_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='충전 과금 원장 (CDR: Charge Detail Record)';


-- 4. 한전/환경부 표준 기본 TOU 요금 정책 및 단가 시드 데이터
INSERT INTO tb_pricing_policy (POLICY_ID, POLICY_NAME, POLICY_TYPE, BASIC_FEE, FREE_MINUTES, PENALTY_PER_MINUTE, IS_ACTIVE, DESCRIPTION)
VALUES ('DEFAULT_TOU', '한전 표준 계절/시간대별 요금제 (기본)', 'TOU', 0.00, 0, 100.00, 1, '한전 계절별/시간대별 3단계 차등 요금제 (급속/완속 공통 표준)')
ON DUPLICATE KEY UPDATE POLICY_NAME = VALUES(POLICY_NAME);

-- 주중(WEEKDAY) - 봄/가을 (SPRING_AUTUMN: 3~6월, 9~10월)
-- 경부하: 23:00~09:00 (195.80원) / 중부하: 09:00~10:00, 12:00~13:00, 17:00~23:00 (255.70원) / 최대부하: 10:00~12:00, 13:00~17:00 (298.50원)
INSERT INTO tb_pricing_tou_rule (POLICY_ID, SEASON_TYPE, DAY_TYPE, LOAD_TYPE, START_HOUR, END_HOUR, UNIT_PRICE) VALUES
('DEFAULT_TOU', 'SPRING_AUTUMN', 'WEEKDAY', 'OFF_PEAK', 0, 9, 195.80),
('DEFAULT_TOU', 'SPRING_AUTUMN', 'WEEKDAY', 'MID_PEAK', 9, 10, 255.70),
('DEFAULT_TOU', 'SPRING_AUTUMN', 'WEEKDAY', 'ON_PEAK', 10, 12, 298.50),
('DEFAULT_TOU', 'SPRING_AUTUMN', 'WEEKDAY', 'MID_PEAK', 12, 13, 255.70),
('DEFAULT_TOU', 'SPRING_AUTUMN', 'WEEKDAY', 'ON_PEAK', 13, 17, 298.50),
('DEFAULT_TOU', 'SPRING_AUTUMN', 'WEEKDAY', 'MID_PEAK', 17, 23, 255.70),
('DEFAULT_TOU', 'SPRING_AUTUMN', 'WEEKDAY', 'OFF_PEAK', 23, 24, 195.80);

-- 주중(WEEKDAY) - 여름 (SUMMER: 7~8월)
-- 경부하: 23:00~09:00 (198.50원) / 중부하: 09:00~10:00, 12:00~13:00, 17:00~23:00 (285.20원) / 최대부하: 10:00~12:00, 13:00~17:00 (347.20원)
INSERT INTO tb_pricing_tou_rule (POLICY_ID, SEASON_TYPE, DAY_TYPE, LOAD_TYPE, START_HOUR, END_HOUR, UNIT_PRICE) VALUES
('DEFAULT_TOU', 'SUMMER', 'WEEKDAY', 'OFF_PEAK', 0, 9, 198.50),
('DEFAULT_TOU', 'SUMMER', 'WEEKDAY', 'MID_PEAK', 9, 10, 285.20),
('DEFAULT_TOU', 'SUMMER', 'WEEKDAY', 'ON_PEAK', 10, 12, 347.20),
('DEFAULT_TOU', 'SUMMER', 'WEEKDAY', 'MID_PEAK', 12, 13, 285.20),
('DEFAULT_TOU', 'SUMMER', 'WEEKDAY', 'ON_PEAK', 13, 17, 347.20),
('DEFAULT_TOU', 'SUMMER', 'WEEKDAY', 'MID_PEAK', 17, 23, 285.20),
('DEFAULT_TOU', 'SUMMER', 'WEEKDAY', 'OFF_PEAK', 23, 24, 198.50);

-- 주중(WEEKDAY) - 겨울 (WINTER: 11~2월)
-- 경부하: 23:00~09:00 (203.20원) / 중부하: 09:00~10:00, 12:00~17:00, 20:00~22:00 (278.40원) / 최대부하: 10:00~12:00, 17:00~20:00, 22:00~23:00 (325.60원)
INSERT INTO tb_pricing_tou_rule (POLICY_ID, SEASON_TYPE, DAY_TYPE, LOAD_TYPE, START_HOUR, END_HOUR, UNIT_PRICE) VALUES
('DEFAULT_TOU', 'WINTER', 'WEEKDAY', 'OFF_PEAK', 0, 9, 203.20),
('DEFAULT_TOU', 'WINTER', 'WEEKDAY', 'MID_PEAK', 9, 10, 278.40),
('DEFAULT_TOU', 'WINTER', 'WEEKDAY', 'ON_PEAK', 10, 12, 325.60),
('DEFAULT_TOU', 'WINTER', 'WEEKDAY', 'MID_PEAK', 12, 17, 278.40),
('DEFAULT_TOU', 'WINTER', 'WEEKDAY', 'ON_PEAK', 17, 20, 325.60),
('DEFAULT_TOU', 'WINTER', 'WEEKDAY', 'MID_PEAK', 20, 22, 278.40),
('DEFAULT_TOU', 'WINTER', 'WEEKDAY', 'ON_PEAK', 22, 23, 325.60),
('DEFAULT_TOU', 'WINTER', 'WEEKDAY', 'OFF_PEAK', 23, 24, 203.20);

-- 주말/공휴일(SATURDAY, HOLIDAY) - 전 계절 경부하/중부하 위주 완화 단가
INSERT INTO tb_pricing_tou_rule (POLICY_ID, SEASON_TYPE, DAY_TYPE, LOAD_TYPE, START_HOUR, END_HOUR, UNIT_PRICE) VALUES
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
