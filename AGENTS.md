# CSMS Lite & h2y-ocpp 프로젝트 거버넌스 및 UI 규칙

## 1. ☀️ UI 기본 테마 및 디자인 표준 규칙 (절대 준수)
* **기본 라이트 테마 원칙**: 모든 UI 화면, 런처 도구, 모니터링 대시보드의 **기본(Default) 테마는 항상 라이트 테마(Light Theme)**로 구성합니다.
* **모던 & 심플 디자인**:
  * 시안/프로토타입 요소(이모지 등)를 배제하고, 깔끔한 인라인 SVG 아이콘과 정제된 타이포그래피(`Pretendard`, `JetBrains Mono`)를 사용합니다.
  * 배경은 세련된 밝은 뉴트럴 톤(`bg-slate-50`, `#F8FAFC`), 카드는 화이트(`bg-white`)와 얇은 보더(`border-slate-200`) 및 은은한 소프트 섀도우를 적용합니다.
  * 복잡한 장식을 지양하고 간결하고 직관적인 엔지니어링 대시보드 레이아웃을 유지합니다.

---

## 2. 🚫 Git 자동 커밋 금지 및 커밋 메시지 표준 규칙
* **AI 자동 커밋 금지 원칙**: AI 어시스턴트는 임의로 `git commit` 또는 `git push`를 자동 실행하지 않으며, 사용자의 명시적 지시 시에만 수동 수행합니다.
* **커밋 메시지 표준 포맷**:
  ```text
  [과제ID]WBS-ID 세부작업명
  ```
  * 기본 과제 ID: `[3368]`
  * 예: `[3368]WBS-COMMON-PP-001 백엔드 런타임 및 Gradle 멀티모듈 스캐폴딩`
  * 예: `[3368]WBS-CONTROL-PT-101-PT 데스크톱 메인 프레임 & 반응형 레이아웃 프로토타입 다중 샘플 개발`

---

## 3. ⚙️ 개발 및 빌드 실행 가이드
* 답변, 중간 과정, 결과 보고는 한국어로 작성합니다.
* 컴파일 테스트는 생략합니다.
* **테마는 라이트 기본입니다 (모든 UI 컴포넌트, 뷰어, 터미널 포함)**.

---

## 4. 🏷️ 충전 인프라 식별자 및 채번 표준 규칙 (CSMS Lite 표준)
* **충전소 ID (`RECHGST_ID` / `ST_ID`)**:
  * **6자리 숫자 체계 (`000000` ~ `999999`)**: CSMS Lite에서는 **`000000`부터 순차 채번**하여 6자리 전체 범위를 제약 없이 자유롭게 사용합니다 (예: `000000`, `000001`, `000002` 등).
  * 💡 **참고**: 기존 레거시 B2C 모바일 앱의 7자리 충전건 키패드 입력 연동 시에만 앞자리 `"00"` 고정 제약이 존재하며, CSMS Lite는 관제/현장 엣지 중심이므로 6자리 전체(100만 개)를 지원합니다.
* **충전기 ID (`RECHGR_ID` / `CP_ID`)**:
  * **2자리 숫자(`01` ~ `99`)** 0-패딩 형식을 적용합니다.
* **충전기 결합 식별자 (`chargeBoxId` / `CHARGE_BOX_ID` / `LOCAL_RECHGR_ID`)**:
  * 포맷: `{ST_ID}{CP_ID}` (총 8자리 정규화)
  * 예: 충전소 `000000` + 충전기 `01` ➔ **`00000001`**
  * 예: 충전소 `000001` + 충전기 `02` ➔ **`00000102`**
* **커넥터 ID (`CONNECTOR_ID` / `CNNCTR_ID`) 및 커넥터 기본 구성 규칙**:
  * **1자리 이상 숫자(`1`, `2`...)**를 적용합니다.
  * **샘플 충전기 커넥터 기본 수량 (절대 준수)**: 시스템 초기 시드 데이터, 가상 충전기 테스터(에뮬레이터) 및 샘플로 등록되는 충전기의 커넥터는 항상 **기본 2개 (듀얼건: 커넥터 ID 1, 2)**로 구성합니다.


* **트랜잭션 ID (`transactionId` / `TRN_ID`) - 전역 단일 시퀀스 방식 채택**:
  * **표준 타입**: `integer` (32비트 signed int, 최대값 `2,147,483,647` 이하 필수, OCPP 1.6 규격 준수).
  * **채번 주체**: **CSMS (서버)** (`StartTransaction.conf` 응답 시 발급).
  * **채번 방식 (전역 단일 시퀀스)**:
    * 충전소/충전기/커넥터 구분 없이 시스템 전체 차원에서 **단일 순차 증가 번호(1, 2, 3...)**를 발급합니다.
    * **전역 고유성(Global Uniqueness)**: `TRN_ID` 단독으로 전사 모든 충전 트랜잭션을 1:1 고유 식별합니다 (복합키 없이 단일 기본키로 활용 가능).
  * **동시성 제어 및 구현 표준**:
    * **Redis 원자적 증가(`INCR csms:seq:transaction_id`)**를 활용하여 DB 행 락(Row Lock) 병목 없이 초고속(논블로킹, <1ms) 채번.
    * 서버 기동 또는 캐시 미스 시 DB의 `MAX(TRN_ID)`를 조회하여 Redis 시퀀스 초기값 자동 동기화.
  * **오버플로우 및 수명 주기**:
    * 일일 10만 건 충전 기준 약 58년(21,474일) 동안 롤오버 없이 사용 가능하며, 21억 도달 시 안전 롤오버 처리.

---

## 5. 🧩 백엔드 쿼리 및 QueryDSL 표준 규칙 (QueryDSL Architecture Standards)

* **기술 스택별 역할 분담 및 책임 분리 원칙**:
  1. **Spring Data JPA (`JpaRepository`)**:
     * PK 기반 단건 조회 (`findById`), 엔티티 기본 영속화 (`save`, `delete`).
     * 조건이 1~2개로 매우 단순한 고정 단건/목록 조회 (`findByChargeBoxId`, `findByTrnId` 등).
  2. **QueryDSL (`JPAQueryFactory`) (실무 표준)**:
     * **다중 조건 검색 및 동적 필터링**: 관제/어드민 검색 조건(기간, 충전소, 상태, 키워드 등).
     * **복합 조인 및 화면 맞춤 DTO 프로젝션**: 엔티티 전체 로딩 대신 필요한 컬럼만 `Projections.constructor`로 직접 조회하여 N+1 및 메모리 낭비 원천 차단.
     * **페이징 및 정렬**: `Pageable`과 연동된 카운트 쿼리 최적화.
  3. **MyBatis 하이브리드 허용 기준**:
     * 대용량 정산 집계, 일괄 벌크 연산(Bulk Insert/Update), 복잡한 윈도우 함수/통계 SQL 등 순수 RDBMS 최적화가 필수적인 특수 영역에 한해 선별적 사용.

* **🚨 기형적으로 긴 JPA 쿼리 메소드명 작성 엄격 금지**:
  * `findByChargerIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(...)` 처럼 조건/정렬이 3개 이상 결합되어 메소드명이 장황해지는 JPA 메소드 선언을 금지합니다.
  * 검색 조건이 2개 이상이거나 동적 조건이 필요한 경우, 지체 없이 **QueryDSL Custom Repository(`XxxRepositoryCustom`, `XxxRepositoryImpl`)** 구조로 전환합니다.

* **QueryDSL 코딩 및 설계 표준**:
  * **`BooleanExpression` 조건절 분리 (호이스팅 및 가독성)**:
    * `where(...)` 절 내부에 null 체크를 인라인으로 작성하지 않고, 의미가 명확한 개별 private 메소드(`eqChargerId`, `betweenDate`, `containsKeyword` 등)로 분리합니다.
    * QueryDSL은 `BooleanExpression`이 `null`을 반환하면 `where` 조건절에서 자동으로 제외하므로 안전하고 깔끔한 동적 쿼리가 완성됩니다.
    ```java
    // ⭕ 표준 권장 구조
    public Page<ChargerDto> searchChargers(ChargerSearchCond cond, Pageable pageable) {
        List<ChargerDto> content = queryFactory
            .select(Projections.constructor(ChargerDto.class,
                charger.chargerId,
                charger.stationId,
                charger.status,
                charger.lastHeartbeat))
            .from(charger)
            .where(
                eqStationId(cond.getStationId()),
                eqStatus(cond.getStatus()),
                betweenCreatedAt(cond.getStartDate(), cond.getEndDate())
            )
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
        ...
    }

    private BooleanExpression eqStationId(String stationId) {
        if (!StringUtils.hasText(stationId)) {
            return null;
        }
        return charger.stationId.eq(stationId);
    }
    ```
  * **표준 패키지 및 클래스 네이밍 구조**:
    * 기본 리포지토리: `XxxRepository extends JpaRepository<Xxx, ID>, XxxRepositoryCustom`
    * 커스텀 인터페이스: `XxxRepositoryCustom`
    * 커스텀 구현체: `XxxRepositoryImpl implements XxxRepositoryCustom` (`JPAQueryFactory` 주입)
---

## 6. 🗄️ 데이터베이스 및 테이블 명명 표준 (DB Conventions)
* **기본 명명 원칙**:
  1. **소문자 스네이크 표기법 (`lower_snake_case`)**: 리눅스/윈도우 배포 환경 간의 `lower_case_table_names` 설정 충돌 방지 및 JPA Entity 기본 네이밍 전략과의 100% 호환성 보장.
  2. **단수형 명사 사용 (Singular)**: 테이블의 각 행(Row)은 엔티티 인스턴스 1건을 의미하므로 복수형(`-s`)을 금지합니다 (예: `tb_charger` O, `tb_chargers` X).
  3. **표준 기본 구조**: `[접두사]_[엔티티명]_[역할/유형]`
* **객체 유형별 표준 접두사 (Prefix)**:
  * **`tb_`**: 일반 물리 테이블 (`tb_charger`, `tb_station`, `tb_corp`, `tb_connector_status`, `tb_transaction_cdr`)
  * **`vi_`**: 뷰 (`vi_charger_summary`)
  * **`ht_`**: 이력 테이블 (`ht_connector_status`)
  * **`if_`**: 인터페이스 (`if_kme_card`)
  * **`tmp_`**: 임시/배치 테이블
