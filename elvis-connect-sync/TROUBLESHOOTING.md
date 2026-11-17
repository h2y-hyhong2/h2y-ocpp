# elvis-connect-sync 오류 해결 이력 (TROUBLESHOOTING)

> Kafka 이벤트 동기화 엔진(`:9081`)의 기동·DB/Flyway 검증·컨슈머/과금 처리 오류 및 해결 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: 재발 가능성이 있는 DB 연결, Flyway/Hibernate 스키마 불일치, Kafka 역직렬화/DLT 오류
- **필수 항목**: 발생 일시, 증상/에러 메시지, 근본 원인, 해결 방법, 관련 파일

---

## 점검 참고 사항 (최초 기동 시 유의)

### NOTE-01: `01_schema.sql` 재실행 후 Flyway `V4` TOU 단가 중복 가능성
- **대상 설정**: `elvis-connect-sync/src/main/resources/application.yml` (`flyway.enabled: true`, `baseline-on-migrate: true`)
- **상세 내용**: `config/mysql/01_schema.sql`을 재실행하면 `flyway_schema_history`가 `DROP`된 뒤 테이블이 재생성됩니다. 이후 `elvis-connect-sync`가 기동하면 Flyway가 `V2`~`V6`를 재실행하며, `V4__create_charge_detail_record.sql`에 포함된 `INSERT INTO pricing_tou_rule`이 다시 수행될 수 있습니다.

### NOTE-02: `connection-timeout: 3000` (3초)과 최초 인프라 초기화 경합
- **대상 설정**: `elvis-connect-sync/src/main/resources/application.yml` (`spring.datasource.hikari.connection-timeout: 3000`)
- **상세 내용**: 완전 클린 환경에서 MySQL 최초 데이터 초기화 및 스키마 적재가 완료되기 전에 `elvis-connect-sync`가 먼저 기동하면 커넥션 타임아웃으로 실패할 수 있으므로, MySQL/Kafka `ONLINE` 상태 확인 후 기동하는 것이 안전합니다.

---

## 오류 이력

### [TEMPLATE] 오류 제목
- **발생 일시**: `YYYY-MM-DD`
- **증상 / 에러 메시지**:
  ```text
  여기에 실제 콘솔/로그 에러 메시지를 기록합니다.
  ```
- **근본 원인**: 원인 분석 내용
- **해결 방법**: 적용한 조치 및 재발 방지 방안
- **관련 파일**: `elvis-connect-sync/...`
