# elvis-connect-ws 변경 로그 이력 (CHANGELOG)

> OCPP 1.6J WebSocket 게이트웨이 모듈(`:9080`)의 주요 기능·설정·아키텍처 변경 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: OCPP WebSocket 핸들링/세션 저장/Kafka 연동/설정 변경 등 동작에 영향을 주는 변경 사항
- **작성 항목**: 날짜, WBS/커밋 ID, 담당자, 변경 요약, 영향 범위

---

## 변경 이력

### [2026-11-16] Spring Boot 4.1.2 업그레이드 및 기동 안정화 (`098f3e9`)
- **과제/커밋**: `[3368]WBS-COMMON-OP-001` (`098f3e9`)
- **변경 내용**:
  - Spring Boot 4.1.2 환경에 맞춰 `KafkaConsumerConfig`, `KafkaProducerConfig`, `WebSocketProperties` 설정 정합성 보완
  - `OcppHandshakeInterceptor`, `OcppWebSocketHandler`, `LocalSessionStore`, `GatewayMetrics` 연동 보완
  - `InboundRawEventProducer`, `OutboundCommandConsumer` 및 `OcppOutboundCommand` DTO 정비
- **영향 범위**: WebSocket 연결 수락(`/ocpp/{chargeBoxId}`), Kafka `ocpp-raw-events` / `ocpp-outbound-commands` 송수신

### [2026-11-13] Standalone Core 5대 엔진 단일화 (`0202561`)
- **과제/커밋**: `[3368] S30-DATA-0030` (`0202561`)
- **변경 내용**:
  - ClickHouse 관련 의존/연동 제거 및 Core 5대 엔진 구조로 단일화
- **영향 범위**: 전체 아키텍처 파이프라인
