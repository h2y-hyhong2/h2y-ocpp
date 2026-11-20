# elvis-launcher 변경 로그 이력 (CHANGELOG)

> 로컬 인프라(MySQL, Kafka) 및 코어 백엔드(WS, Sync, API) 통합 런처(`:1420`, Tauri 2.0 / Vue 3)의 변경 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: Tauri 트레이/프로세스 제어, Vite 미들웨어 API, UI 제어판 및 포트/로그 연동 변경
- **작성 항목**: 날짜, WBS/커밋 ID, 담당자, 변경 요약, 영향 범위

---

## 변경 이력

### [2026-11-20] 런처 UI 간결화·상태 애니메이션 및 독립 로그·프로세스 뷰어 연동
- **과제/커밋**: `[3368]WBS-COMMON-OP-007` (런처 UI 정비 및 `elvis-log-viewer` 연동)
- **변경 내용**:
  - `src/style.css`: GPU `transform` 기반 `.launcher-live-ring` 이중 파동 도트 애니메이션 추가 (`prefers-reduced-motion` 대응 포함)
  - `src/App.vue`: 상단 `RUNNING` 배지 및 5대 서비스(MySQL, Kafka, WS, Sync, API) 각 행의 상태 도트에 실행중(`ONLINE`) 이중 파동 애니메이션 적용
  - `tailwind.config.js` / `src/App.vue` / `src-tauri/tauri.conf.json`: 전역 폰트 크기 +2px 일괄 상향(`text-xs` 14px, `9/10/10.5/11px` → `11/12/12.5/13px`), 5대 서비스 상태 텍스트(`ONLINE`/`OFFLINE`, `min-w-[56px] text-right`) 및 `Port` 배지(`min-w-[82px]`)를 우측 버튼군 좌측으로 세로 일렬 정렬, 중복 `로그`/`에러로그`/`프로세스` 버튼을 그룹 헤더·개별 행에서 제거하고 최상단 헤더(`로그 | 에러로그 · 프로세스`)로 일원화, 5대 서비스 모든 행에서 `시작`·`종료`·`재기동` 3버튼을 항상 표시(불가 상태 비활성화)하고 그룹 헤더와 동일 폭(`min-w-[54px]`)으로 세로 정렬, 창 폭(`700×432`, 최소 `660×400`) 최적화
  - `src-tauri/src/lib.rs` / `vite.config.ts` / `src/App.vue`: 독립 Rust 네이티브 GUI 모듈(`elvis-log-viewer/target/{release,debug}/elvis-log-viewer.exe`) 자동 탐색·실행 연동 — 일괄 분할(`all`/`core`/`infra`), 개별 프로세스 `로그`/`에러`(`--service <svc>`), 그룹 **`프로세스` 진단 창**(`--mode process --target infra|core|all`) 모두 네이티브 뷰어로 우선 실행 (`elvis-log-viewer` 미빌드 시 기존 Windows Terminal / PowerShell / `check-status.bat` 자동 폴백), 상단 `줄바꿈 OFF/ON` 토글 및 그룹 명칭(`미들웨어` / `코어 엔진`) 간결화
- **영향 범위**: 런처 UI 전역 타이포그래피, 상태 도트 및 창 크기(`App.vue`, `style.css`, `tailwind.config.js`, `tauri.conf.json`)

### [2026-11-17] 5대 서비스 기준 런처 컴팩트 레이아웃 및 창 크기 최적화
- **과제/커밋**: `[3368]WBS-COMMON-OP-003`
- **변경 내용**:
  - `elvis-data`(확장 기능) 제거 이후 남아있던 세로 여백을 축소하고, `src-tauri/tauri.conf.json` 기본 창 크기(`700x550`, 최소 `620x500` → `640x388`, 최소 `590x360`)를 5대 서비스 구성에 맞게 축소
  - `src/App.vue`에서 `h-screen`/`flex-1` 강제 세로 확장 대신 콘텐츠 밀착형 패딩(`py-2`, `max-w-[640px]`)으로 전환해 데스크톱·웹 모드 모두에서 컴팩트하게 표시되도록 개선
- **영향 범위**: 런처 메인 창 크기 및 5대 서비스 목록 레이아웃

### [2026-11-16] 런처 프로세스 제어 및 UI 안정화 (`098f3e9`)
- **과제/커밋**: `[3368]WBS-COMMON-OP-001` (`098f3e9`)
- **변경 내용**:
  - `src-tauri/src/lib.rs`, `src/App.vue`, `vite.config.ts` 서비스 제어·재기동 상태 추적 및 로그 콘솔 연동 보완
- **영향 범위**: 트레이 메뉴, 서비스 시작/중지/재기동 제어, 실시간 로그/에러 콘솔

### [2026-10-23] WMI 기반 무창 독립 데몬 기동 및 프로세스 진단 분리 (`715d138`)
- **과제/커밋**: `[3368]` (`715d138`)
- **변경 내용**:
  - 런처 종료 후에도 인프라/백엔드 데몬이 유지되도록 WMI(`Win32_Process.Create`) 기반 무창 독립 기동 및 프로세스 진단 창 분리
- **영향 범위**: 전체 서비스 백그라운드 실행 수명 주기
