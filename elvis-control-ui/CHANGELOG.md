# elvis-control-ui 변경 로그 이력 (CHANGELOG)

> ELVIS-CSMS 데스크톱 관제 센터(`:1421`, Tauri 2.0 / Vue 3)의 화면·상태관리·API 연동 변경 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: 화면/라우터 추가·수정, Pinia 스토어, API 클라이언트, Tauri/Vite 설정 변경
- **작성 항목**: 날짜, WBS/커밋 ID, 담당자, 변경 요약, 영향 범위
- **오류 해결 이력**: [TROUBLESHOOTING.md](TROUBLESHOOTING.md)에서 별도 관리

---

## 변경 이력

### [2026-11-16] 정산/충전기 뷰 및 API 클라이언트 연동 정비 (`098f3e9`)
- **과제/커밋**: `[3368]WBS-COMMON-OP-001` (`098f3e9`)
- **변경 내용**:
  - `BillingCdrView.vue`, `ChargersView.vue`, `App.vue`, `router/index.ts` 화면 및 라우팅 정비
  - `authStore.ts`, `csmsStore.ts`, `apiClient.ts` 및 Tauri `lib.rs` / `capabilities/default.json` 보완
- **영향 범위**: 관제 UI 정산/충전기 조회, 인증 상태 및 Tauri 데스크톱 실행

### [2026-11-03] 클린 로그인 UI 및 JWT 인증 연동 (`3f04370`)
- **과제/커밋**: `[3368] S36-UI-0030` (`3f04370`)
- **변경 내용**:
  - `LoginView.vue` 추가 및 `authStore.ts` / `apiClient.ts` / `router/index.ts` JWT 인증 연동
- **영향 범위**: 로그인 및 권한 기반 라우트 가드

### [2026-10-26] 풀뷰포트 충전기 그리드 및 실시간 필터 툴바 개발 (`3ee8e0d`)
- **과제/커밋**: `[3368] S35-UI-0020` (`3ee8e0d`)
- **변경 내용**:
  - `ChargersView.vue` 풀뷰포트 충전기 그리드 및 상태 필터 툴바 구현
- **영향 범위**: 충전기 실시간 관제 메인 화면
