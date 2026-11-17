# elvis-control-api 변경 로그 이력 (CHANGELOG)

> 관제 어드민 REST API·JWT 인증·실시간 SSE/WebSocket 푸시 서버(`:9082`)의 주요 변경 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: REST API 스펙, Spring Security/JWT 인증, QueryDSL 조회, Kafka 알림/하향 커맨드, 설정 변경
- **작성 항목**: 날짜, WBS/커밋 ID, 담당자, 변경 요약, 영향 범위

---

## 변경 이력

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
