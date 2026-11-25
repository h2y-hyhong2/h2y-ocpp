# elvis-launcher 변경 로그 이력 (CHANGELOG)

> 로컬 인프라(MySQL, Kafka) 및 코어 백엔드(WS, Sync, API) 통합 런처(`:1420`, Tauri 2.0 / Vue 3)의 변경 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: Tauri 트레이/프로세스 제어, Vite 미들웨어 API, UI 제어판 및 포트/로그 연동 변경
- **작성 항목**: 날짜, WBS/커밋 ID, 담당자, 변경 요약, 영향 범위

---

## 변경 이력

### [2026-11-25] 개발 환경(Dev/Debug) vs Release 빌드 경로 자동 분기 적용
- **과제/커밋**: `[3368]WBS-COMMON-OP-010` (개발·릴리스 경로 분리)
- **변경 내용**:
  - `src-tauri/src/lib.rs`: `#[cfg(debug_assertions)]` / `#[cfg(not(debug_assertions))]` 컴파일 타임 분기 도입 — 개발 빌드에서는 워크스페이스 루트(`CARGO_MANIFEST_DIR` 조부모)와 `settings-dev.json`, Gradle `bootRun` 모듈 인식, `elvis-log-viewer/target/{debug,release}` 우선 탐색을 적용하고, Release 빌드에서만 제품 경로(`D:\elvis-lite`)와 `settings.json`, `apps\elvis-*.jar` 필수 판정을 따르도록 분리
  - `vite.config.ts`: Vite 개발 서버(`npm run dev`)의 기본 경로를 워크스페이스 루트(`path.resolve(__dirname, '..')`), 설정 파일을 `settings-dev.json`으로 변경하고, Gradle 모듈 설치 인식 및 `elvis-log-viewer/target/*`·워크스페이스 `logs/` 폴백 적용
  - `src/App.vue`: 초기 경로 및 입력 placeholder를 개발/릴리스 동적 `defaultProductRoot`에 연동
- **영향 범위**: `elvis-launcher/src-tauri/src/lib.rs`, `elvis-launcher/vite.config.ts`, `elvis-launcher/src/App.vue`

### [2026-11-25] 제품 메인 경로(`D:\elvis-lite`) 동적 변경 및 서비스별 `미설치` 상태 판별·표시 기능 구현
- **과제/커밋**: `[3368]WBS-COMMON-OP-008` (제품 인스톨 경로 관리 및 미설치 표시)
- **변경 내용**:
  - `src-tauri/src/lib.rs`, `vite.config.ts`:
    - 제품 메인 경로 기본값을 `D:\elvis-lite`로 고정하고 `%APPDATA%\com.lselink.elvis.launcher\settings.json`에 사용자 변경 경로 영속 저장(`get_product_root`, `set_product_root`, `browse_product_root` 커맨드 및 `/api/infra/product-root*` API 추가)
    - 컴파일 시점 개발 워크스페이스 폴백을 제거하고 선택된 제품 경로 기준으로 5대 서비스 설치 여부(`binaries\mysql-9.7.1-winx64`, `binaries\kafka_2.13-4.3.1`, `apps\elvis-*.jar`, `scripts\*.bat`·`run-daemon`)를 정밀 판별해 `installed`/`missing` 정보 반환
    - 미설치 서비스 기동 차단 및 그룹 일괄 제어 시 설치된 서비스만 순차 실행, 시스템 트레이 메뉴에도 `[미설치]` 표시 및 비활성화 동기화, `elvis-log-viewer.exe`를 제품 경로 우선 탐색(미설치 시 기존 콘솔 폴백 유지)
  - `src/App.vue`, `src-tauri/tauri.conf.json`:
    - 상단 툴바의 `에러로그` 버튼을 제거하고 `로그` 단독 버튼으로 간결화
    - 라이트 테마 기준 상단 `제품 경로` 설정 바(가로 꽉 채움 경로 표시/입력창·설치 요약 배지·`경로 변경`·폴더 `찾아보기`·`기본값` 복원) 추가 및 기본 창 높이 조정(`432 -> 484`, 최소 `452`)
    - 미설치 서비스 행에 `미설치` 배지(누락 파일 툴팁 포함) 표시 및 시작/재기동 버튼 비활성화, 그룹 헤더 및 상단 `RUNNING` 배지에 설치 수 반영
- **영향 범위**: `elvis-launcher/src-tauri/src/lib.rs`, `elvis-launcher/vite.config.ts`, `elvis-launcher/src/App.vue`, `elvis-launcher/src-tauri/tauri.conf.json`

### [2026-11-24] 전용 제품 아이콘 적용 및 `D:\elvis-lite\elvis-launcher.exe` 실행파일 배포 구성
- **과제/커밋**: `[3368]WBS-COMMON-OP-007` (4대 데스크톱 제품 아이콘 적용 및 `D:\elvis-lite` 실행파일 배포)
- **변경 내용**:
  - `src-tauri/icons/*`, `src-tauri/build.rs`, `public/elvis_lite_light.svg`, `index.html`, `src/App.vue`: 루트 `icons/elvis_lite_light.svg` 공식 제품 아이콘 기준으로 필수 Tauri 아이콘 5종(`32x32`, `128x128`, `128x128@2x`, `icon.icns`, `icon.ico`)·파비콘·상단 헤더 로고 일괄 적용, 미사용 레거시 아이콘 정리 및 `build.rs`에 `cargo:rerun-if-changed=icons/*` 추가(아이콘 교체 시 Windows 리소스 자동 재컴파일 보장)
  - `tsconfig.json`, `src-tauri/src/lib.rs`, `vite.config.ts`: `vue-tsc` 빌드용 `tsconfig.json` 추가 및 `elvis-log-viewer.exe` 우선 탐색 경로를 `D:\elvis-lite\elvis-log-viewer.exe`로 통일
- **영향 범위**: `elvis-launcher/src-tauri/icons/*`, `elvis-launcher/src-tauri/build.rs`, `elvis-launcher/tsconfig.json`, `elvis-launcher/src-tauri/src/lib.rs`, `elvis-launcher/vite.config.ts`

### [2026-11-24] 릴리스 실행파일 `D:\elvis-launcher.exe` 출력 통일 및 단독 실행 경로 탐색 보강
- **과제/커밋**: `[3368]WBS-COMMON-OP-007` (데스크톱 실행파일 `D:\` 배포 통일)
- **변경 내용**:
  - `package.json`: `pnpm tauri:build` 및 `pnpm tauri:build:exe`(`--no-bundle`) 실행 시 최종 실행파일만 `D:\elvis-launcher.exe`로 자동 복사(`../scripts/copy-exe.mjs`)하도록 구성
  - `src-tauri/src/lib.rs`: `D:\elvis-launcher.exe` 단독 실행 시에도 `config/paths.env`와 `scripts/`를 정상 탐색하도록 `ELVIS_WORKSPACE_DIR` 및 컴파일 시점 워크스페이스 루트 폴백 추가, `find_log_viewer_binary()`에서 `D:\elvis-log-viewer.exe` 최우선 탐색 추가
  - `vite.config.ts`: `elvis-log-viewer.exe` 탐색 후보 3곳 모두 `D:\elvis-log-viewer.exe`를 1순위로 추가
- **영향 범위**: `elvis-launcher/package.json`, `elvis-launcher/src-tauri/src/lib.rs`, `elvis-launcher/vite.config.ts`

### [2026-11-23] 상단 헤더 `줄바꿈` 토글 버튼 제거
- **과제/커밋**: `[3368]WBS-COMMON-OP-007` (런처 상단 툴바 간결화)
- **변경 내용**:
  - `src/App.vue`: `elvis-log-viewer` 내부에서 실시간 줄바꿈 토글을 직접 지원함에 따라 런처 상단 헤더의 중복 `줄바꿈 ON/OFF` 버튼 및 관련 상태(`logWordWrap`, `toggleLogWordWrap`)를 제거하고, 로그 실행 시 기본값(`wordWrap: false` / `wrap=0`, 가로 스크롤)으로 전달하도록 정리
- **영향 범위**: `elvis-launcher/src/App.vue`

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
