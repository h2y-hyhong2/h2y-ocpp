-- ============================================================
-- V3: elvis_lite.tb_rfid_card & tb_auth_hist 테이블 생성
-- 과제: [3368] CSMS Lite 버전 (POC) | Sprint: S20-SYNC-0070-DE
-- 작성일: 2026-10-12 | 작성자: IT기획 파트
-- 목적: OCPP Authorize 수신 시 RFID 회원 카드 유효성 초저지연 검증(<50ms)을 위한
--       회원 카드 마스터 테이블 및 비동기 감사 이력 테이블 구축
-- ============================================================

-- 1. 회원 RFID 카드 마스터 테이블 (L3 영속 원장 및 캐시 원천 소스)
CREATE TABLE IF NOT EXISTS tb_rfid_card (
    ID                  BIGINT          NOT NULL AUTO_INCREMENT     COMMENT 'PK - RFID 카드 고유 일련번호',
    ID_TAG              VARCHAR(64)     NOT NULL                    COMMENT 'RFID 태그 식별자 (대문자 정규화, 고유키)',
    MEMBER_ID           BIGINT          NOT NULL                    COMMENT '회원 식별자 (tb_member 매핑)',
    MEMBER_NAME         VARCHAR(64)         NULL DEFAULT NULL       COMMENT '회원명 (마스킹 표시용)',
    PARENT_ID_TAG       VARCHAR(64)         NULL DEFAULT NULL       COMMENT '상위/법인 대표 태그 식별자 (선택)',
    
    CARD_STATUS         VARCHAR(32)     NOT NULL DEFAULT 'NORMAL'   COMMENT '카드 상태 (NORMAL:정상, BLOCKED:차단/정지, LOST:분실, SUSPENDED:일시정지)',
    EXPIRY_DATE         DATETIME(3)         NULL DEFAULT NULL       COMMENT '카드 유효기간 만료일시 (UTC 기준)',
    
    BALANCE             DECIMAL(12, 2)  NOT NULL DEFAULT 0.00       COMMENT '선불 잔액 또는 결제 한도 (원)',
    MAX_CONCURRENT_TX   INT             NOT NULL DEFAULT 1          COMMENT '동시 충전 허용 세션 수 (기본 1개)',
    
    IS_DELETED          TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '삭제 여부 (0:활성, 1:삭제)',
    VERSION             INT             NOT NULL DEFAULT 0          COMMENT 'JPA 낙관적 락(Optimistic Locking) 버전 컬럼',
    REG_DT              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '최초 등록 일시',
    UPDT_DT             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3)             COMMENT '최종 수정 일시',

    PRIMARY KEY (ID),

    -- 고유 제약조건: 활성 RFID 태그 중복 방지 (조회 핵심 인덱스)
    UNIQUE KEY UK_RFID_CARD_ID_TAG (ID_TAG),

    -- 회원별 카드 목록 조회 인덱스
    INDEX IX_RFID_CARD_MEMBER (MEMBER_ID, CARD_STATUS),

    -- 상위 태그 기준 하위 태그 탐색 인덱스
    INDEX IX_RFID_CARD_PARENT (PARENT_ID_TAG)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='회원 RFID 카드 마스터 원장 (Authorize 회원 인증 조회)';


-- 2. Authorize 인증 감사 이력 테이블 (Fire-and-Forget 비동기 저장)
CREATE TABLE IF NOT EXISTS tb_auth_hist (
    ID                  BIGINT          NOT NULL AUTO_INCREMENT     COMMENT 'PK - 인증 이력 일련번호',
    CHARGE_BOX_ID       VARCHAR(64)     NOT NULL                    COMMENT '요청 충전기 식별자',
    MESSAGE_ID          VARCHAR(64)     NOT NULL                    COMMENT 'OCPP MessageId (UUID/식별자)',
    ID_TAG              VARCHAR(64)     NOT NULL                    COMMENT '요청 RFID 태그 식별자',
    
    AUTH_STATUS         VARCHAR(32)     NOT NULL                    COMMENT '인증 판정 결과 (Accepted, Blocked, Expired, Invalid, ConcurrentTx)',
    PARENT_ID_TAG       VARCHAR(64)         NULL DEFAULT NULL       COMMENT '응답된 상위 태그 식별자',
    FAIL_REASON         VARCHAR(128)        NULL DEFAULT NULL       COMMENT '인증 실패/차단 상세 사유',
    
    CACHE_TIER          VARCHAR(16)     NOT NULL DEFAULT 'L2_REDIS' COMMENT '캐시 적중 계층 (L1_CAFFEINE, L2_REDIS, L3_DB, MISS)',
    LATENCY_MS          DECIMAL(8, 2)   NOT NULL DEFAULT 0.00       COMMENT '인증 판정 소요 시간 (밀리초)',
    
    REG_DT              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '인증 요청 수신 일시',

    PRIMARY KEY (ID),

    -- 충전기별 일자 인증 이력 조회 인덱스
    INDEX IX_AUTH_HIST_CHARGER_TIME (CHARGE_BOX_ID, REG_DT),

    -- 태그별 인증 이력 및 남용 탐지 인덱스
    INDEX IX_AUTH_HIST_ID_TAG_TIME (ID_TAG, REG_DT),

    -- 기간별 일괄 통계 산출 인덱스
    INDEX IX_AUTH_HIST_TIME (REG_DT)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='회원 RFID 인증 감사 이력 (비동기 적재)';
