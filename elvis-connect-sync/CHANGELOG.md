# elvis-connect-sync 변경 로그 이력 (CHANGELOG)

> Kafka 이벤트 파싱 및 MySQL 실시간 상태/세션/과금/장애 동기화 엔진(`:9081`)의 주요 변경 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: Kafka 컨슈머, OCPP 파서, JPA/QueryDSL, Flyway 마이그레이션, 과금/장애 로직 및 설정 변경
- **작성 항목**: 날짜, WBS/커밋 ID, 담당자, 변경 요약, 영향 범위

---

## 변경 이력

### [2026-11-25] `evcs_ocpp_rest` 표준 로그 포맷·MDC(`charger`, `msgId`)·파일 분리 적용
- **과제/커밋**: `[3368]WBS-COMMON-OP-009` (WS/SYNC 로그 방식 표준화)
- **변경 내용**:
  - `logback-spring.xml`: `evcs_ocpp_rest`와 동일한 파이프 구분 고정 폭 패턴(`%d{yyyy-MM-dd HH:mm:ss.SSS} | %-5level | OCPP-SYNC | %-18.-18thread | %-36.36logger{36} | %-8.-8X{charger:--} | %-36.-36X{msgId:--} | %msg%n`) 및 4종 어펜더(`CONSOLE`, `FILE`(ERROR 제외, `ocpp-sync.log`), `ERROR_FILE`(ERROR 전용, `sync-err.log`), `KAFKA_FILE`(`sync-kafka.log`, `additivity="false"` 분리))·롤링 정책(100MB/7일 `.gz`) 적용
  - `util/LogFormatUtils.java`, `filter/MdcMsgIdFilter.java`: 29자 Action 패딩(`padAction`), OCPP `msgId` 추출, `MdcScope` 및 HTTP 필터(`jakarta.servlet`) 추가
  - `OcppRawEventConsumer`, `OcppResponseSender`: MDC(`charger`, `msgId`) 바인딩 및 비동기 Kafka 콜백 MDC 전파, `[REQ] CALL`, `[RES] CALL` (`elapsed={}ms`), `[RES] CALLRESULT`, `[RES] CALLERROR`, `[Kafka] SEND`/`SEND FAIL` 표준 로그 포맷 적용
  - `application.yml`, `config/connect-sync/application.yml`: `logging.file.name`/`error-file.name`/`kafka-file.name` 및 Kafka/Redis 로거 `WARN` 레벨 설정
- **영향 범위**: `elvis-connect-sync` 콘솔·파일·에러·Kafka 로그 출력 및 `D:\elvis-lite\logs\` 파일 구조

### [2026-11-19] 로그 출력 이모지 제거 및 텍스트 형식 표준화
- **과제/커밋**: `[3368]WBS-COMMON-OP-006` (전 모듈 로그 텍스트 표준화)
- **변경 내용**:
  - `TransactionSequenceService`: 경고 로그 문구 내 이모지 제거 및 순수 텍스트 형식으로 통일
- **영향 범위**: 동기화 엔진 콘솔·파일 로그 출력

### [2026-11-19] 단말 연결 종료 이벤트 반영 및 Heartbeat 만료 자동 오프라인 전환
- **과제/커밋**: `[3368]` (미연결 충전기 상태 정합성 개선)
- **변경 내용**:
  - `OcppRawEventEnvelope` / `OcppRawEventConsumer`: `DISCONNECTED` 시스템 이벤트 분기 처리 추가
  - `ConnectorStatusRepository` / `ConnectorStatusService`: 연결 종료 시 충전기 전체 커넥터를 `'연결안됨'` 상태로 전환 및 순시 계측치(`power_kw`, `voltage_v`, `current_a`, `soc_percent`, `charging_minutes`) 초기화, Heartbeat 타임아웃(기본 `120초`, `30초` 주기) 초과 만료 단말 자동 오프라인 전환 스케줄러 추가
  - `ElvisConnectSyncApplication`: `@EnableScheduling` 활성화 및 `application.yml`에 `elvis.sync.connection` 설정 추가
  - `build.gradle` / `application.yml`: `spring-boot-devtools`(`developmentOnly`) 추가 및 `spring.devtools.restart` 활성화(`livereload.enabled: false`)
  - `OcppResponseSender` / `OcppRawEventConsumer` / `DataTransferService` / `MeterValuesService` / `ConnectorStatusRepository`: OCPP Phase 1 최적화 — 미구현 액션 `CALLERROR(NotImplemented)` 회신, `DataTransfer` 라우터(`UnknownMessageId` 기본 응답), 표준 `MeterValues` 및 LS `DataTransfer(MeterValues)` 실시간 계측치 단일 Upsert(`upsertTelemetry`) 반영, 대기/종료 상태 복귀 시 순시 출력·전압·전류 0 초기화, Heartbeat 만료 안전망 `3900초`(65분) 상향
- **영향 범위**: `connector_status` 실시간 상태 동기화 파이프라인, OCPP RPC 응답 및 계측 처리, 로컬 개발 자동 재시작

### [2026-11-16] Spring Boot 4.1.2 업그레이드 및 QueryDSL·Flyway 정비 (`098f3e9`)
- **과제/커밋**: `[3368]WBS-COMMON-OP-001` (`098f3e9`)
- **변경 내용**:
  - `QuerydslConfig` 도입 및 `FaultHistoryRepositoryCustom/Impl`, `PricingTouRuleRepositoryCustom/Impl`, `TransactionSessionRepositoryCustom/Impl` QueryDSL Custom Repository 패턴 적용
  - Flyway 마이그레이션(`V1`~`V6`) 및 도메인 엔티티(`ConnectorStatus`, `TransactionSession`, `RfidCard`, `AuthHist`, `PricingPolicy`, `PricingTouRule`, `ChargeDetailRecord`, `FaultHistory`, `DltHistory`) ISO 11179 스키마 정합성 정비
  - `OcppRawEventConsumer`, `OcppMessageParser`, `TransactionSequenceService`, `TouTimeSlicer`, `TouRatingServiceImpl`, `FaultDetectionServiceImpl`, `DltRecoveryServiceImpl` 보완
- **영향 범위**: `elvis_lite` MySQL 스키마(`ddl-auto: validate`), Flyway V1~V6, Kafka 이벤트 동기화 파이프라인

### [2026-11-13] Standalone Core 5대 엔진 단일화 (`0202561`)
- **과제/커밋**: `[3368] S30-DATA-0030` (`0202561`)
- **변경 내용**:
  - ClickHouse 시계열 저장소 제거 및 MySQL/Kafka 중심 동기화 구조 단일화
- **영향 범위**: 데이터 적재 및 이력 저장 경로
