# elvis-tester-ui 변경 로그 이력 (CHANGELOG)

> 가상 충전기(EVSE) 에뮬레이터 및 WireTap 패킷 시뮬레이터 UI(`:1422`, Tauri 2.0 / Vue 3)의 변경 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: OCPP 시뮬레이션 컴포저블, 가상 충전기 제어 패널, 검증 스위트, UI/설정 변경
- **작성 항목**: 날짜, WBS/커밋 ID, 담당자, 변경 요약, 영향 범위

---

## 변경 이력

### [2026-11-16] 가상 충전기 시뮬레이터 및 검증 스위트 정비 (`098f3e9`)
- **과제/커밋**: `[3368]WBS-COMMON-OP-001` (`098f3e9`)
- **변경 내용**:
  - `useVirtualCharger.ts`, `ActionControlPanel.vue`, `AddChargerModal.vue`, `ChargerListPanel.vue`, `ChargerMasterModal.vue`, `TaskVerificationSuite.vue` 정비
- **영향 범위**: 가상 충전기 등록/연결, OCPP 1.6 시나리오 테스트 및 검증 스위트
