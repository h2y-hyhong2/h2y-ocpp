# elvis-connect-ws 변경 로그 이력 (CHANGELOG)

> OCPP 1.6J WebSocket 게이트웨이 모듈(`:9080`)의 주요 기능·설정·아키텍처 변경 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: OCPP WebSocket 핸들링/세션 저장/Kafka 연동/설정 변경 등 동작에 영향을 주는 변경 사항
- **작성 항목**: 날짜, WBS/커밋 ID, 담당자, 변경 요약, 영향 범위

---

## 변경 이력

### [2026-11-25] `evcs_ocpp_ws` 표준 로그 포맷·MDC(`charger`, `msgId`)·파일 분리 적용
- **과제/커밋**: `[3368]WBS-COMMON-OP-009` (WS/SYNC 로그 방식 표준화)
- **변경 내용**:
  - `logback-spring.xml`: `evcs_ocpp_ws`와 동일한 파이프 구분 고정 폭 패턴(`%d{yyyy-MM-dd HH:mm:ss.SSS} | %-5level | OCPP-WS   | %-18.-18thread | %-36.36logger{36} | %-8.-8X{charger:--} | %-36.-36X{msgId:--} | %msg%n`) 및 4종 어펜더(`CONSOLE`, `FILE`(ERROR 제외, `ocpp-ws.log`), `ERROR_FILE`(ERROR 전용, `ws-err.log`), `KAFKA_FILE`(`ws-kafka.log`, `additivity="false"` 분리))·롤링 정책(100MB/7일 `.gz`) 적용
  - `util/LogFormatUtils.java`, `filter/MdcMsgIdFilter.java`: 29자 Action 패딩(`padAction`), OCPP 배열 프레임 `msgId` 추출, URI `charger` 추출 및 `try-with-resources` 기반 `MdcScope`·HTTP 필터(`jakarta.servlet`) 추가
  - `OcppWebSocketHandler`, `OcppHandshakeInterceptor`, `OutboundCommandConsumer`, `InboundRawEventProducer`: MDC(`charger`, `msgId`) 바인딩 및 `[RECEIVE]`, `[SEND]   `, `[Kafka] SEND`/`SEND FAIL`, `[WebSocket][OCPP1.6]` 표준 로그 태그로 통일
  - `application.yml`, `config/connect-ws/application.yml`: `logging.file.name`/`error-file.name`/`kafka-file.name` 및 `org.apache.kafka`/`org.springframework.kafka` `WARN` 레벨 설정
- **영향 범위**: `elvis-connect-ws` 콘솔·파일·에러·Kafka 로그 출력 및 `D:\elvis-lite\logs\` 파일 구조

### [2026-11-20] 활성 세션 현황 로그 개행 문자 제거 (1줄 출력 표준화)
- **과제/커밋**: `[3368]WBS-COMMON-OP-007` (로그 뷰어 1로그 1줄 가독성 개선)
- **변경 내용**:
  - `LocalSessionStore#logActiveSessions`: 세션 현황 로그의 `%n` 개행 문자를 제거하고 다중 세션 시 ` | ` 구분자로 연결된 단일 라인 포맷으로 변경
- **영향 범위**: `elvis-connect-ws` 세션 주기 모니터링 및 등록/해제 로그 출력

### [2026-11-19] 로그 출력 이모지 제거 및 텍스트 형식 표준화
- **과제/커밋**: `[3368]WBS-COMMON-OP-006` (전 모듈 로그 텍스트 표준화)
- **변경 내용**:
  - `OcppWebSocketHandler`, `InboundRawEventProducer`, `LocalSessionStore`: 로그 출력 문구 내 이모지 제거 및 순수 텍스트(`[모듈 태그]` + 메시지) 형식으로 통일
- **영향 범위**: WebSocket 게이트웨이 콘솔·파일 로그 출력

### [2026-11-19] 단말 연결 종료(DISCONNECTED) 이벤트 발행 및 활성 세션 조회 내부 API 추가
- **과제/커밋**: `[3368]` (미연결 충전기 상태 정합성 개선)
- **변경 내용**:
  - `OcppRawEventEnvelope` / `InboundRawEventProducer`: `eventType`(`OCPP_MESSAGE` / `DISCONNECTED`) 및 `sendDisconnectEvent` 추가 (동일 `chargeBoxId` 파티션 키로 순서 보장)
  - `OcppWebSocketHandler` / `LocalSessionStore`: 소켓 종료(`afterConnectionClosed`) 및 유휴 타임아웃 정리(`cleanupIdleSessions`) 시 중복 없는 단일 `DISCONNECTED` 이벤트 발행, `getActiveChargeBoxIds()` 추가
  - `InternalSessionController`: 관제 API에서 실시간 활성 연결 충전기 목록을 조회하는 `GET /internal/sessions` 내부 엔드포인트 추가
  - `build.gradle` / `application.yml`: `spring-boot-devtools`(`developmentOnly`) 추가 및 `spring.devtools.restart` 활성화(`livereload.enabled: false`로 다중 모듈 포트 충돌 방지)
  - `OcppWebSocketHandler` / `LocalSessionStore` / `WebSocketConfig` / `WebSocketProperties`: OCPP Phase 1 최적화 — RFC 6455 WebSocket Ping/Pong 헬스체크(`ping-interval-seconds: 30`, `handlePongMessage`) 분리, `idle-timeout-seconds: 360` 및 `buffer-size-bytes: 65536`(64KB) 상향
- **영향 범위**: WebSocket 세션 생명주기 관리, Kafka `ocpp-raw-events` 이벤트 스키마, 내부 세션 조회 API, 로컬 개발 자동 재시작

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
