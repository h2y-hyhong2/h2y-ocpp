# elvis-tester-ui 변경 로그 이력 (CHANGELOG)

> 가상 충전기(EVSE) 에뮬레이터 및 WireTap 패킷 시뮬레이터 UI(`:1422`, Tauri 2.0 / Vue 3)의 변경 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: OCPP 시뮬레이션 컴포저블, 가상 충전기 제어 패널, 검증 스위트, UI/설정 변경
- **작성 항목**: 날짜, WBS/커밋 ID, 담당자, 변경 요약, 영향 범위

---

## 변경 이력

### [2026-11-25] `elvis-control-api` JWT 인증 연동(`testerAuthFetch`) 추가
- **과제/커밋**: `[3368]WBS-COMMON-OP-008` (`elvis-tester-ui.exe` 프로덕션 서버 연동 점검)
- **변경 내용**:
  - `src/utils/testerApiAuth.ts`: `elvis-control-api(:9082)` 호출 시 읽기 전용 시드 계정(`viewer`)으로 JWT 토큰을 자동 발급·캐시하고 401 시 1회 자동 갱신하는 `testerAuthFetch` 추가
  - `src/composables/useVirtualCharger.ts`, `src/components/TaskVerificationSuite.vue`: MySQL 충전기 목록 동기화(`/api/v1/chargers`) 및 API 검증 스위트(`S26-API-0020-TE`)에서 `testerAuthFetch`를 사용하도록 수정하여 401 인증 실패 해소
- **영향 범위**: `elvis-tester-ui/src/utils/testerApiAuth.ts`, `useVirtualCharger.ts`, `TaskVerificationSuite.vue`

### [2026-11-24] 전용 제품 아이콘 적용 및 `D:\elvis-lite\elvis-tester-ui.exe` 실행파일 배포 구성
- **과제/커밋**: `[3368]WBS-COMMON-OP-007` (4대 데스크톱 제품 아이콘 적용 및 `D:\elvis-lite` 실행파일 배포)
- **변경 내용**:
  - `src-tauri/icons/*`, `src-tauri/build.rs`, `public/elvis_lite_tester_light.svg`, `index.html`, `src/components/SimpleHeader.vue`: 루트 `icons/elvis_lite_tester_light.svg` 공식 제품 아이콘 기준으로 필수 Tauri 아이콘 5종(`32x32`, `128x128`, `128x128@2x`, `icon.icns`, `icon.ico`)·파비콘·상단 헤더 로고 일괄 적용, 미사용 레거시 아이콘 정리 및 `build.rs`에 `cargo:rerun-if-changed=icons/*` 추가
  - `package.json`, `tsconfig.json`, `src/components/ChargerListPanel.vue`: `pnpm tauri:build` / `pnpm tauri:build:exe` 완료 시 `D:\elvis-lite\elvis-tester-ui.exe` 자동 복사 구성, `tsconfig.json` 추가 및 `getConnectors` 반환 타입 보완
- **영향 범위**: `elvis-tester-ui/package.json`, `elvis-tester-ui/tsconfig.json`, `elvis-tester-ui/index.html`, `elvis-tester-ui/public/elvis_lite_tester_light.svg`, `elvis-tester-ui/src/components/SimpleHeader.vue`, `elvis-tester-ui/src/components/ChargerListPanel.vue`, `elvis-tester-ui/src-tauri/icons/*`, `elvis-tester-ui/src-tauri/build.rs`

### [2026-11-18] 기동 시퀀스 듀얼 커넥터(#1, #2) 상태 전문 순차 발송 보완
- **과제/WBS**: `[3368]WBS-COMMON-OP-001`
- **변경 내용**:
  - `src/composables/useVirtualCharger.ts`의 `runBootSequence`에서 단일 `activeConnectorId`가 아닌 등록된 모든 커넥터(`#1`, `#2`)에 대해 `StatusNotification`(`Preparing` → `Available`)을 순차 발송하도록 수정
- **영향 범위**: 가상 충전기 연결 직후 자동 기동 시퀀스 및 수동 `기동 시퀀스 즉시 실행` 시 듀얼건(#1, #2) 상태 동기화

### [2026-11-16] 가상 충전기 시뮬레이터 및 검증 스위트 정비 (`098f3e9`)
- **과제/커밋**: `[3368]WBS-COMMON-OP-001` (`098f3e9`)
- **변경 내용**:
  - `useVirtualCharger.ts`, `ActionControlPanel.vue`, `AddChargerModal.vue`, `ChargerListPanel.vue`, `ChargerMasterModal.vue`, `TaskVerificationSuite.vue` 정비
- **영향 범위**: 가상 충전기 등록/연결, OCPP 1.6 시나리오 테스트 및 검증 스위트
