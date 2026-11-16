-- ============================================================
-- V3: elvis_lite.rfid_card & auth_hist 테이블 생성 (ISO 11179)
-- 과제: [3368] CSMS Lite 버전 (POC) | Sprint: S20-SYNC-0070-DE
-- 목적: OCPP Authorize 수신 시 RFID 회원 카드 유효성 초저지연 검증(<50ms)을 위한
--       회원 카드 마스터 테이블 및 비동기 감사 이력 테이블 구축
-- ============================================================

-- 1. 회원 RFID 카드 마스터 테이블 (L3 영속 원장 및 캐시 원천 소스)
CREATE TABLE IF NOT EXISTS rfid_card (
    id                  BIGINT          NOT NULL AUTO_INCREMENT     COMMENT 'PK - RFID 카드 고유 일련번호',
    id_tag              VARCHAR(64)     NOT NULL                    COMMENT 'RFID 태그 식별자 (대문자 정규화, 고유키)',
    member_id           BIGINT          NOT NULL                    COMMENT '회원 식별자',
    member_name         VARCHAR(64)         NULL DEFAULT NULL       COMMENT '회원명 (마스킹 표시용)',
    parent_id_tag       VARCHAR(64)         NULL DEFAULT NULL       COMMENT '상위/법인 대표 태그 식별자 (선택)',

    card_status         VARCHAR(32)     NOT NULL DEFAULT 'NORMAL'   COMMENT '카드 상태 (NORMAL:정상, BLOCKED:차단/정지, LOST:분실, SUSPENDED:일시정지)',
    expiry_date         DATETIME(3)         NULL DEFAULT NULL       COMMENT '카드 유효기간 만료일시 (UTC 기준)',

    balance             DECIMAL(12, 2)  NOT NULL DEFAULT 0.00       COMMENT '선불 잔액 또는 결제 한도 (원)',
    max_concurrent_tx   INT             NOT NULL DEFAULT 1          COMMENT '동시 충전 허용 세션 수 (기본 1개)',

    is_deleted          TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '삭제 여부 (0:활성, 1:삭제)',
    version             INT             NOT NULL DEFAULT 0          COMMENT 'JPA 낙관적 락(Optimistic Locking) 버전 컬럼',
    reg_dt              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '최초 등록 일시',
    updt_dt             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3)              COMMENT '최종 수정 일시',

    PRIMARY KEY (id),

    -- 고유 제약조건: 활성 RFID 태그 중복 방지 (조회 핵심 인덱스)
    UNIQUE KEY uk_rfid_card_id_tag (id_tag),

    -- 회원별 카드 목록 조회 인덱스
    INDEX ix_rfid_card_member (member_id, card_status),

    -- 상위 태그 기준 하위 태그 탐색 인덱스
    INDEX ix_rfid_card_parent (parent_id_tag)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='회원 RFID 카드 마스터 원장 (ISO 11179)';


-- 2. Authorize 인증 감사 이력 테이블 (Fire-and-Forget 비동기 저장)
CREATE TABLE IF NOT EXISTS auth_hist (
    id                  BIGINT          NOT NULL AUTO_INCREMENT     COMMENT 'PK - 인증 이력 일련번호',
    charge_box_id       VARCHAR(50)     NOT NULL                    COMMENT '요청 충전기 식별자',
    message_id          VARCHAR(64)     NOT NULL                    COMMENT 'OCPP MessageId (UUID/식별자)',
    id_tag              VARCHAR(64)     NOT NULL                    COMMENT '요청 RFID 태그 식별자',

    auth_status         VARCHAR(32)     NOT NULL                    COMMENT '인증 판정 결과 (Accepted, Blocked, Expired, Invalid, ConcurrentTx)',
    parent_id_tag       VARCHAR(64)         NULL DEFAULT NULL       COMMENT '응답된 상위 태그 식별자',
    fail_reason         VARCHAR(128)        NULL DEFAULT NULL       COMMENT '인증 실패/차단 상세 사유',

    cache_tier          VARCHAR(16)     NOT NULL DEFAULT 'L2_REDIS' COMMENT '캐시 적중 계층 (L1_CAFFEINE, L2_REDIS, L3_DB, MISS)',
    latency_ms          DECIMAL(8, 2)   NOT NULL DEFAULT 0.00       COMMENT '인증 판정 소요 시간 (밀리초)',

    reg_dt              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '인증 요청 수신 일시',

    PRIMARY KEY (id),

    -- 충전기별 일자 인증 이력 조회 인덱스
    INDEX ix_auth_hist_charger_time (charge_box_id, reg_dt),

    -- 태그별 인증 이력 및 남용 탐지 인덱스
    INDEX ix_auth_hist_id_tag_time (id_tag, reg_dt),

    -- 기간별 일괄 통계 산출 인덱스
    INDEX ix_auth_hist_time (reg_dt)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='회원 RFID 인증 감사 이력 (ISO 11179)';

-- 레거시(tb_*, tbl_*) 하위 호환 뷰 생성
CREATE OR REPLACE VIEW tb_rfid_card AS SELECT * FROM rfid_card;
CREATE OR REPLACE VIEW tbl_rfid_card AS SELECT * FROM rfid_card;
CREATE OR REPLACE VIEW tb_auth_hist AS SELECT * FROM auth_hist;
CREATE OR REPLACE VIEW tbl_auth_hist AS SELECT * FROM auth_hist;
