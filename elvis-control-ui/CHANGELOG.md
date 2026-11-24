# elvis-control-ui 변경 로그 이력 (CHANGELOG)

> ELVIS-CSMS 데스크톱 관제 센터(`:1421`, Tauri 2.0 / Vue 3)의 화면·상태관리·API 연동 변경 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: 화면/라우터 추가·수정, Pinia 스토어, API 클라이언트, Tauri/Vite 설정 변경
- **작성 항목**: 날짜, WBS/커밋 ID, 담당자, 변경 요약, 영향 범위
- **오류 해결 이력**: [TROUBLESHOOTING.md](TROUBLESHOOTING.md)에서 별도 관리

---

## 변경 이력

### [2026-11-24] 릴리스 실행파일 `D:\elvis-control-ui.exe` 출력 통일 및 단독 실행 경로 탐색 보강
- **과제/커밋**: `[3368]WBS-COMMON-OP-007` (데스크톱 실행파일 `D:\` 배포 통일)
- **변경 내용**:
  - `package.json`: `pnpm tauri:build` 및 `pnpm tauri:build:exe`(`--no-bundle`) 실행 시 최종 실행파일만 `D:\elvis-control-ui.exe`로 자동 복사(`../scripts/copy-exe.mjs`)하도록 구성
  - `src-tauri/src/lib.rs`: `D:\elvis-control-ui.exe` 단독 실행 시에도 `config/paths.env` 및 `scripts/`를 찾을 수 있도록 `ELVIS_WORKSPACE_DIR` 및 컴파일 시점 워크스페이스 루트 폴백 추가
- **영향 범위**: `elvis-control-ui/package.json`, `elvis-control-ui/src-tauri/src/lib.rs`

### [2026-11-20] 충전기 목록(카드형 메인) 전환, 우클릭 컨텍스트 메뉴 및 하단 롤링·롤업 바 적용
- **과제/커밋**: `[3368]WBS-COMMON-OP-007` (메인 관제 화면 전환 및 우클릭 빠른 제어 메뉴 적용)
- **변경 내용**:
  - `router/index.ts`: 기본 루트 경로(`/`)를 `SimpleDashboardView`(`충전기 목록`)로 변경하고, 기존 `ChargersView`(`충전기 관제`)를 `/chargers`로 이동, 기존 `/simple` 경로는 `/`로 리다이렉트 유지
  - `App.vue`: GNB 메뉴 순서를 `충전기 목록`(메인 `/`) → `충전기 관제`(`/chargers`) → `과금 원장`(`/billing`) 순으로 재배치하고, `ChargersView` 전용 모바일 사이드바/제어반 토글 버튼 노출 조건(`route.name === 'Chargers'`) 및 라우트 이동 시 드로어 자동 닫기 처리 추가
  - `components/ChargerContextMenu.vue` / `stores/csmsStore.ts`: `elvis-tester-ui` 스타일의 충전기 우클릭 플로팅 컨텍스트 메뉴 신규 구현 (`Teleport`, 경계 자동 보정, `ESC`/외부 클릭 닫기, 실시간 계측 2×2 요약, 커넥터 채널 전환, 기기 스펙, OCPP 원격 제어 4종, `TriggerMessage` 즉시 보고 요청, 상세 제어반/ID 복사/과금 원장 이동) 및 `sendTriggerMessage` 스토어 액션 추가
  - `SimpleDashboardView.vue` / `ChargersView.vue`: 충전기 카드·테이블 행 및 개별 커넥터 버튼에 우클릭 컨텍스트 메뉴 연동
  - `SimpleDashboardView.vue` / `data/sampleChargers.ts`: 루트 컨테이너 중복 패딩 제거(`App.vue` `<main>` 단일 패딩으로 통합), 하단 상태 바에 **수직 자동 롤링 티커**(3.5초 주기, 호버 일시정지, 이전/다음/일시정지 컨트롤, 항목 클릭 시 필터·상세 연동)와 **롤업(Roll-up) 확장 패널**(4열 실시간 운전 KPI + 전체 롤링 피드 목록, 외부 클릭/`ESC` 닫기) 구현, 디자인 샘플 데이터를 10기 → **전수 케이스 25기(5×5 그리드)**로 분리·확장(OCPP 1.6J + LS 확장 12종 전체 상태, 1/2/3/4채널, 듀얼건 동시 충전·복합 상태·오프라인·알수없음) 및 카드 본문 세부 상태(`일시중지`/`예약중`/`통신이상`/`점검중`/`운영중지`/`알수없음`) 표시 정교화
  - `tailwind.config.js` / `styles/main.css` / 전체 Vue 뷰·컴포넌트: 전역 폰트 스케일 상향(`xs` 13px ~ `3xl` 32px, 기존 9/10/11px 극소 픽셀을 11/12/13px로 +2px 일괄 상향) 및 헤더(`52px`)·카드(`min-h-[280px]`)·롤링 티커(`h-7`)·상세/우클릭 패널 치수 미세 보정
  - `styles/main.css` / `SimpleDashboardView.vue` / `ChargersView.vue`: 충전중 SoC 진행 바 내부에 GPU `transform` 기반 Shimmer(전류 흐름) 애니메이션(`.charging-bar-shimmer`, `prefers-reduced-motion` 대응) 적용
- **영향 범위**: 관제 UI 라우터(`router/index.ts`), 상단 GNB(`App.vue`), 충전기 목록(`SimpleDashboardView.vue`), 충전기 관제(`ChargersView.vue`), Pinia 스토어(`csmsStore.ts`)

### [2026-11-19] 충전기 상태(온라인/오프라인) + 커넥터별 상태 동시 표시 정비
- **과제/커밋**: `[3368]WBS-COMMON-OP-006` (충전기·커넥터 상태 역할 분리 및 통합 가시성 개선)
- **변경 내용**:
  - `csmsStore.ts`: `getChargerConnectionMeta` 헬퍼 추가 (온라인/오프라인 배지·도트 메타 공통화)
  - `ChargersView.vue`: 메인 테이블에 `충전기 상태`(`온라인`/`오프라인` 단일 배지) 컬럼을 독립 추가하여 `커넥터 상태`(`#1`, `#2`... 개별 OCPP 상태 + 출력) 컬럼과 분리 표시, 상단 헤더에 온라인/오프라인 충전기 및 전체 커넥터 수 요약 배지 추가, 우측 제어반 단말 정보 영역에 충전기 상태(`온라인`/`오프라인`) 행 추가
  - `SimpleDashboardView.vue`: 모든 충전기 카드(충전대기·충전중·준비중·충전완료·고장·연결안됨) 상단에 충전기 상태(`온라인`/`오프라인`) 배지, 하단에 전체 커넥터별 실시간 상태(`#1`, `#2`... 클릭 시 해당 커넥터 상세 드로어 연동) 공통 영역을 항상 표시하고, 상단 필터 칩에 `준비중`·`완료` 필터 및 온라인/오프라인·커넥터 수 요약, 우측 상세 드로어의 충전기 상태 섹션 보강
- **영향 범위**: 충전기 관제 화면(`ChargersView.vue`), 간편 관제 화면(`SimpleDashboardView.vue`), Pinia 스토어(`csmsStore.ts`)

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
