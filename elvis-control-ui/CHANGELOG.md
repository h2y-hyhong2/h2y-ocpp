# elvis-control-ui 변경 로그 이력 (CHANGELOG)

> ELVIS-CSMS 데스크톱 관제 센터(`:1421`, Tauri 2.0 / Vue 3)의 화면·상태관리·API 연동 변경 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: 화면/라우터 추가·수정, Pinia 스토어, API 클라이언트, Tauri/Vite 설정 변경
- **작성 항목**: 날짜, WBS/커밋 ID, 담당자, 변경 요약, 영향 범위
- **오류 해결 이력**: [TROUBLESHOOTING.md](TROUBLESHOOTING.md)에서 별도 관리

---

## 변경 이력

### [2026-11-19] 미연결 충전기(`connected: false`) 상태·계측치 표시 정합성 개선
- **과제/커밋**: `[3368]` (미연결 충전기 상태 정합성 개선)
- **변경 내용**:
  - `csmsStore.ts`: `ChargerItem.connected` 필드 및 `isChargerConnected` 헬퍼 추가, `connected === false`인 충전기의 커넥터·대표 상태를 `'연결안됨'`으로 보정
  - `ChargersView.vue`: 충전기 ID 좌측에 WebSocket 연결 상태 도트 표시, 우측 제어반 계측치 헤더(`실시간`/`미연결`) 및 커넥터 채널 카드(`미연결`) 문구를 실제 연결 상태에 맞게 개선
  - `csmsStore.ts`: `STATUS_MAP`에 LS 확장 영문 상태 `'Pause'`(`일시중지`), `'Maintenance'`(`점검중`) 및 `'Reserved'`(`예약중`) 매핑 추가
  - `SimpleDashboardView.vue` / `router/index.ts` / `App.vue`: Google Stitch `ELVIS-LITE` 프로젝트의 **초간편 쉬운 전기차 충전 관제 대시보드** 디자인을 `/simple`(`간편 관제`) 신규 탭으로 적용 (5열 반응형 상태별 카드 그리드, 충전소 전환 드롭다운, 수동 새로고침 버튼, 슬라이드 상세·원격제어 드로어, MySQL 실데이터 ↔ 10기 디자인 샘플 미리보기 토글)
- **영향 범위**: 충전기 관제 화면(`ChargersView.vue`), 간편 관제 화면(`SimpleDashboardView.vue`), 라우터/GNB 및 Pinia 스토어(`csmsStore.ts`)

### [2026-11-18] 충전기·커넥터 기준 실시간 상태 표시, 필터링 및 커넥터별 원격제어 반영
- **과제/커밋**: `[3368]` (커넥터 기준 관제 UI 개선)
- **변경 내용**:
  - `csmsStore.ts`: `selectedConnectorId` 상태 및 `getChargerConnectors`, `getPrimaryConnector`, `getChargerEffectiveStatus`, `getChargerTotalPowerKw` 헬퍼 추가, `sendRemoteAction`에서 `Reset` 외 제어 명령 시 선택된 `connectorId` 전달
  - `ChargersView.vue`: 테이블 행에 커넥터별 클릭 가능한 상태 배지(`#1`, `#2`...) 렌더링, 상태 필터 칩을 커넥터 건수 기준으로 집계·필터링, 우측 제어반에 커넥터 채널 선택 탭 및 선택 커넥터별 계측치·원격제어 대상 표시 추가
- **영향 범위**: 충전기 관제 메인 화면(`ChargersView.vue`) 및 Pinia 스토어(`csmsStore.ts`)

### [2026-11-17] Tauri 권한 스키마(`capabilities.json`) 동기화
- **과제/커밋**: `[3368]WBS-COMMON-OP-003`
- **변경 내용**:
  - `src-tauri/capabilities/default.json`에 대응하는 `src-tauri/gen/schemas/capabilities.json` 생성 스키마(`default` 권한 및 윈도우 제어 권한) 최신화
- **영향 범위**: Tauri 2.0 데스크톱 빌드/실행 시 권한 스키마 정합성

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
