# elvis-control-api 변경 로그 이력 (CHANGELOG)

> 관제 어드민 REST API·JWT 인증·실시간 SSE/WebSocket 푸시 서버(`:9082`)의 주요 변경 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: REST API 스펙, Spring Security/JWT 인증, QueryDSL 조회, Kafka 알림/하향 커맨드, 설정 변경
- **작성 항목**: 날짜, WBS/커밋 ID, 담당자, 변경 요약, 영향 범위

---

## 변경 이력

### [2026-11-25] 과금 원장(CDR) 조회를 `transaction_session` + `charge_detail_record` 실데이터 통합 조회로 전환 및 Tauri 2.0 CORS 허용
- **과제/커밋**: `[3368]WBS-COMMON-OP-008` (과금 원장 실조회 연동 및 `elvis-control-ui.exe` 프로덕션 API 연동)
- **변경 내용**:
  - `TransactionSessionEntity.java`, `ChargeDetailRecordEntity.java`, `TransactionCdrRepositoryImpl.java`: 기존 빈 테이블(`transaction_cdr`) 대신 `elvis-connect-sync`가 실제 영속화하는 `transaction_session`과 `charge_detail_record`를 QueryDSL `LEFT OUTER JOIN`으로 통합 조회해 진행 중 세션·TOU 정산 완료 원장·4대 KPI 요약을 실시간 반환
  - `SecurityConfig.java`, `WebSocketConfig.java`: Tauri 2.0 Windows WebView2 기본 Origin(`http://tauri.localhost`, `http://tauri.localhost:*`, `https://tauri.localhost:*`)을 CORS 및 `/ws-push` STOMP 엔드포인트 허용 패턴에 추가
  - `config/mysql/02_seed_data.sql`: 가상 충전기 테스터 기본 프리셋 RFID 카드 4종(`CARD-CORP-9901`, `CARD-USER-1001`, `CREDIT-IC-01`, `ROAM-ME-7701`) 시드 등록 추가
- **영향 범위**: `elvis-control-api` CDR 조회 전체(`/api/v1/cdr/**`), CORS/WebSocket 보안 설정, `config/mysql/02_seed_data.sql`

### [2026-11-19] REST API 요청/응답 공통 로깅 필터(`RestApiLoggingFilter`) 추가 및 로그 텍스트 표준화
- **과제/커밋**: `[3368]WBS-COMMON-OP-006` (관제 REST API 요청 로깅 정비 및 로그 이모지 제거)
- **변경 내용**:
  - `RestApiLoggingFilter`: `/api/**` 요청(CORS `OPTIONS` 제외)에 대해 HTTP 메서드, URI(민감 쿼리 파라미터 마스킹), 상태코드, 소요시간(ms), 인증 사용자명, 클라이언트 IP를 상태코드별 로그 레벨(2xx/3xx `INFO`, 4xx `WARN`, 5xx `ERROR`)로 출력
  - `JwtAuthenticationFilter`: 서블릿 필터 최상단 로깅 시 인증 사용자명을 참조할 수 있도록 요청 속성(`elvis.auth.username`) 기록
  - `application.yml`: `elvis.control.api-logging.enabled` 설정 추가 (기본 `true`)
  - `CommandController` / `DynamicControlController`: 로그 출력 문구 내 이모지 제거 및 순수 텍스트 태그 형식으로 통일
- **영향 범위**: 관제 REST API 전체 (`/api/**`) 및 컨트롤러 로그 출력

### [2026-11-19] WS 게이트웨이 활성 세션 교차 검증을 통한 미연결 충전기 상태 보정
- **과제/커밋**: `[3368]` (미연결 충전기 상태 정합성 개선)
- **변경 내용**:
  - `WsSessionClient`: `elvis-connect-ws`의 `/internal/sessions`를 짧은 TTL 캐시(기본 1초)로 조회하여 실시간 활성 연결 충전기 목록 확인 (게이트웨이 미응답 시 0건 처리)
  - `ChargerService` / `ChargerResponseDto`: `connected` 필드 추가 및 활성 WebSocket 세션이 없는 충전기는 DB 잔여 상태와 무관하게 대표/커넥터 상태를 `'연결안됨'`, 순시 출력/전압/전류를 `0`으로 반환하도록 보정
  - `application.yml`: `elvis.control.ws-gateway` 연동 설정 추가
  - `AssetApiServiceTest`: 미연결 충전기 `'연결안됨'` 보정 단위 테스트 추가
  - `build.gradle` / `application.yml`: `spring-boot-devtools`(`developmentOnly`) 추가 및 `spring.devtools.restart` 활성화(`livereload.enabled: false`)
  - `ChargerService`: LS 커스텀 영문 상태(`Pause`, `Maintenance`) 우선순위 및 요약 통계 매핑 추가
- **영향 범위**: 충전기 목록/상세/페이징/요약 REST API (`/api/v1/chargers/**`), 로컬 개발 자동 재시작

### [2026-11-18] 충전기·커넥터 기준 상태 산출 및 커넥터 단위 원격제어 지원
- **과제/커밋**: `[3368]` (커넥터 기준 관제 정합성 개선)
- **변경 내용**:
  - `ChargerService`: `connectorId=0`(본체)과 실제 커넥터(`>=1`)를 분리하여 `connectors` 목록을 구성하고, 대표 상태를 우선순위(`고장 > 충전중 > 준비중/일시중지 > 충전완료/예약중 > 점검중 > 충전대기 > 연결안됨`) 기반으로 산출하며 최신 하트비트 반영
  - `ChargerService` / `ChargerController`: `/api/v1/chargers/{chargeBoxId}/remote-command` 요청에서 선택적 `connectorId`를 지원하여 커넥터별 원격 제어(`RemoteStartTransaction`, `RemoteStopTransaction`, `UnlockConnector`) 가능하도록 개선 (`Reset`은 본체 전체 적용)
  - `ChargerRepositoryImpl`: 상태 필터링(`eqStatus`) 시 실제 커넥터(`connectorId > 0`) 중 해당 상태를 가진 건이 하나라도 존재하면 매칭되도록 QueryDSL 조건 보완
  - `AssetApiServiceTest`: 0번 본체 레코드 분리, 우선순위 대표 상태, 특정 커넥터 대상 원격제어 단위 테스트 추가
- **영향 범위**: 충전기 목록/상세/요약 API 및 원격 제어 엔드포인트

### [2026-11-17] JWT 인증 필터 수정 (`0eb3c33`)
- **과제/커밋**: `0eb3c33`
- **변경 내용**:
  - `JwtAuthenticationFilter.java` 필터 처리 보완
- **영향 범위**: API 요청 인증 필터 체인

### [2026-11-16] Spring Boot 4.1.2 업그레이드 및 QueryDSL·보안 핸들러 정비 (`098f3e9`)
- **과제/커밋**: `[3368]WBS-COMMON-OP-001` (`098f3e9`)
- **변경 내용**:
  - `QuerydslConfig` 및 `TransactionCdrRepositoryCustom/Impl` 등 QueryDSL 조회 구조 정비
  - `AdminSeedInitializer`, `JwtAccessDeniedHandler`, `JwtTokenProvider`, `GlobalExceptionHandler` 보완
  - `CdrServiceImpl`, `ChargerService`, `StationService`, `DynamicControlServiceImpl`, `OutboundCommandServiceImpl` 정비
- **영향 범위**: 관리자 시드 계정(`admin`, `operator`, `viewer`), CDR/충전기/충전소 API 및 원격 제어

### [2026-11-03] 관리자 계정 DB 영속화 및 JWT 인증 연동 (`3f04370`)
- **과제/커밋**: `[3368] S36-UI-0030` (`3f04370`)
- **변경 내용**:
  - `AdminUser` 엔티티 및 `AdminUserRepository` 추가, `AuthServiceImpl` DB 연동 및 ISO 11179 명명 표준 반영
- **영향 범위**: 로그인 인증 및 `admin_user` 테이블
