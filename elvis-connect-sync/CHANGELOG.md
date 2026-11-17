# elvis-connect-sync 변경 로그 이력 (CHANGELOG)

> Kafka 이벤트 파싱 및 MySQL 실시간 상태/세션/과금/장애 동기화 엔진(`:9081`)의 주요 변경 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: Kafka 컨슈머, OCPP 파서, JPA/QueryDSL, Flyway 마이그레이션, 과금/장애 로직 및 설정 변경
- **작성 항목**: 날짜, WBS/커밋 ID, 담당자, 변경 요약, 영향 범위

---

## 변경 이력

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
