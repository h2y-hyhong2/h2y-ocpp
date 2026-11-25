# elvis-control-api 오류 해결 이력 (TROUBLESHOOTING)

> 관제 REST API 서버(`:9082`)의 빌드·기동·JWT 보안·DB/Kafka 연동 오류 및 해결 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: 재발 가능성이 있는 Spring Security/JWT, JPA/QueryDSL, DB 접속, Kafka 연동 오류
- **필수 항목**: 발생 일시, 증상/에러 메시지, 근본 원인, 해결 방법, 관련 파일

---

## 오류 이력

### ISSUE-01: `elvis-control-ui.exe` 단독 실행 시 API 요청이 `403 Invalid CORS request`로 차단됨
- **발생 일시**: `2026-11-25`
- **증상 / 에러 메시지**:
  ```text
  OPTIONS http://127.0.0.1:9082/api/v1/auth/login (Origin: http://tauri.localhost) -> 403 Forbidden (Invalid CORS request)
  ```
- **근본 원인**:
  - Tauri 2.0 Windows WebView2 프로덕션 앱의 기본 `Origin` 헤더는 `http://tauri.localhost`이지만, `SecurityConfig` 및 `WebSocketConfig`의 `allowedOriginPatterns`에는 `tauri://localhost`와 `https://tauri.localhost`만 등록되어 있어 CORS 필터에서 모든 요청이 차단됨.
- **해결 방법**:
  - `SecurityConfig.corsConfigurationSource()` 및 `WebSocketConfig.registerStompEndpoints()`에 `http://tauri.localhost`, `http://tauri.localhost:*`, `https://tauri.localhost:*` 패턴을 추가하고 `elvis-control-api.jar`를 재배포함.
- **관련 파일**: `elvis-control-api/src/main/java/com/lselink/elvis/control/config/SecurityConfig.java`, `elvis-control-api/src/main/java/com/lselink/elvis/control/config/WebSocketConfig.java`

---

### [TEMPLATE] 오류 제목
- **발생 일시**: `YYYY-MM-DD`
- **증상 / 에러 메시지**:
  ```text
  여기에 실제 콘솔/로그 에러 메시지를 기록합니다.
  ```
- **근본 원인**: 원인 분석 내용
- **해결 방법**: 적용한 조치 및 재발 방지 방안
- **관련 파일**: `elvis-control-api/...`
