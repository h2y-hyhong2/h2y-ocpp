# elvis-log-viewer 변경 로그 이력 (CHANGELOG)

> ELVIS CSMS Lite 순수 Rust 네이티브 GUI 5분할 실시간 로그 뷰어(`eframe` / `egui`)의 변경 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: UI 레이아웃·기능 추가/수정, 로그 파싱·성능 개선, CLI 인자 및 `config/paths.env` 연동 변경
- **작성 항목**: 날짜, WBS/커밋 ID, 변경 요약, 영향 범위
- **오류 해결 이력**: [TROUBLESHOOTING.md](TROUBLESHOOTING.md)에서 별도 관리

---

## 변경 이력

### [2026-11-25] 개발 환경(Debug) vs Release 빌드 로그·설정 경로 자동 분기 적용
- **과제/커밋**: `[3368]WBS-COMMON-OP-010` (개발·릴리스 경로 분리)
- **변경 내용**:
  - `src/main.rs`: `find_workspace_root`·`default_log_dir`에 `#[cfg(debug_assertions)]` / `#[cfg(not(debug_assertions))]` 분기 적용 — 개발 환경에서는 워크스페이스 루트(`CARGO_MANIFEST_DIR` 부모) 및 `{workspace}\logs`를, Release 빌드에서는 실행파일 위치·`D:\elvis-lite` 및 `D:\elvis-lite\logs`를 따르도록 분리 (Release 빌드에서 개발 워크스페이스 `paths.env` 참조 제거)
  - `expand_elvis_home`: `config/paths.env`의 `ELVIS_LOG_DIR=%ELVIS_HOME%\logs` 동적 치환 지원 추가
- **영향 범위**: `elvis-log-viewer/src/main.rs`

### [2026-11-25] OCPP 파이프 구분 표준 로그 레벨 파싱 및 태그 하이라이트 지원
- **과제/커밋**: `[3368]WBS-COMMON-OP-009` (WS/SYNC 로그 방식 표준화 연계)
- **변경 내용**:
  - `src/main.rs`: `detect_log_level`에 파이프 구분 헤더(`timestamp | LEVEL | APP | ...`) 파싱 우선 순위를 추가해 본문 JSON 내 `"errorCode":"NoError"` 등에 의한 `ERROR` 오탐 방지, `classify_line_colors`에 `[RECEIVE]`/`[SEND]`/`[REQ]`/`[RES]` 강조 색상 추가
- **영향 범위**: `elvis-log-viewer/src/main.rs`

### [2026-11-25] 공통 시스템 자원 현황 바 하단 1줄 메트릭 필 레이아웃 전환
- **과제/커밋**: `[3368]WBS-COMMON-OP-008` (로그 뷰어 레이아웃 개선)
- **변경 내용**:
  - `src/main.rs`: `render_system_resource_bar`를 `TopBottomPanel::bottom("sys_resource_bar")` 1줄 메트릭 필(`render_metric_pill`: `CPU`, `RAM`, 드라이브별 `DISK` 미니 게이지 바 + 우측 `UPTIME`) 구조로 전환해 전 모드(5분할·3분할·2분할·단일·프로세스) 공통으로 하단에 고정 배치
- **영향 범위**: `elvis-log-viewer/src/main.rs`, `elvis-log-viewer/README.md`

### [2026-11-24] 전용 제품 아이콘 추가 (`exe` 리소스 임베딩 및 창·작업표시줄 아이콘) 및 `D:\elvis-lite` 배포 통일
- **과제/커밋**: `[3368]WBS-COMMON-OP-007` (4대 데스크톱 제품 아이콘 적용 및 `D:\elvis-lite` 실행파일 배포)
- **변경 내용**:
  - `icons/icon.ico`, `icons/icon_64.rgba`: 루트 `icons/elvis_lite_log_light.svg` 공식 제품 아이콘 기준으로 필수 멀티 해상도 `.ico`(16~256px) 및 `64x64` RGBA 아이콘만 유지·적용
  - `build.rs`: Windows SDK `rc.exe`로 `icons/icon.ico`를 `elvis-log-viewer.exe` 실행파일 리소스에 자동 임베딩 (외부 Cargo 의존성 추가 없음)
  - `src/main.rs`: `egui::ViewportBuilder::with_icon(load_app_icon())` 적용으로 앱 타이틀바 및 Windows 작업표시줄 아이콘 표시
  - `build-release.bat`: 실행파일 배포 출력 경로를 `D:\elvis-lite\elvis-log-viewer.exe`로 변경 및 Windows `cmd.exe` CRLF 개행 적용
- **영향 범위**: `elvis-log-viewer/build.rs`, `elvis-log-viewer/build-release.bat`, `elvis-log-viewer/src/main.rs`, `elvis-log-viewer/icons/*`

### [2026-11-24] 각 로그 패널 내부 좌·우 `에러 분할` 토글 기능 추가
- **과제/커밋**: `[3368]WBS-COMMON-OP-007` (패널별 전체 로그 | 에러·경고 로그 좌·우 양분 뷰 추가)
- **변경 내용**:
  - `src/main.rs`: 상단 툴바에 **`에러 분할`** 토글 버튼(실시간 에러·경고 건수 표시, CLI `--type split` 지원) 및 각 패널 헤더의 클릭 가능한 `에러 N` 배지 추가
  - `에러 분할` 활성화 시 모든 로그 모드(5분할·3분할·2분할·단일·확대)에서 각 서비스 패널 내부를 **좌측 `전체 로그` | 우측 `에러 · 경고`**로 50:50 좌·우 양분 표시하며, 중앙 드래그 스플리터로 패널별 비율 조절·`비율 초기화`·우측 에러 로그 전용 `복사` 지원
  - `PaneState::refresh`를 개선해 기본 로그(`{key}.log`) 내 `ERROR`/`WARN` 라인과 전용 stderr 파일(`{key}-err.log`)을 동시에 증분 수집하여 모드 전환 지연(0ms) 제거 및 에러 누락 방지
- **영향 범위**: `elvis-log-viewer/src/main.rs`, `elvis-log-viewer/README.md`

### [2026-11-24] 릴리스 실행파일 `D:\elvis-log-viewer.exe` 출력 통일 및 단독 실행 경로 탐색 보강
- **과제/커밋**: `[3368]WBS-COMMON-OP-007` (데스크톱 실행파일 `D:\` 배포 통일)
- **변경 내용**:
  - `build-release.bat`: `cargo build --release` 후 최종 실행파일만 `D:\elvis-log-viewer.exe`로 자동 복사하는 릴리스 빌드 스크립트 추가
  - `src/main.rs`: `D:\elvis-log-viewer.exe` 단독 실행 시에도 `config/paths.env` 및 로컬 폰트(`Pretendard`/`Inconsolata`)를 정상 로드하도록 `find_workspace_root()`에 `ELVIS_WORKSPACE_DIR` 및 컴파일 시점 워크스페이스 루트 폴백 추가
- **영향 범위**: `elvis-log-viewer/build-release.bat`, `elvis-log-viewer/src/main.rs`, `elvis-log-viewer/README.md`

### [2026-11-23] 전체 뷰 공통 상단 컴팩트 시스템 자원 바 추가
- **과제/커밋**: `[3368]WBS-COMMON-OP-007` (전 모드 공통 호스트 시스템 자원 모니터링 바 추가)
- **변경 내용**:
  - `src/main.rs`: 상단 툴바(`top_toolbar`) 바로 아래에 `TopBottomPanel::top("sys_resource_bar")`를 신설해 **전체 5분할·코어 3분할·미들웨어 2분할·개별 단일 보기·프로세스 진단** 모든 창에 공통 적용
  - 초슬림 2행 레이아웃(높이 약 43px)으로 구성: 1행 `CPU` 개별 가로 막대 + 우측 로컬 고정 디스크별 사용률/여유 배지(`C:`, `D:` 등), 2행 `RAM` 개별 가로 막대 + 우측 `UPTIME` 표시
  - 외부 크레이트 추가나 PowerShell CIM 호출 지연 없이 Windows `kernel32.dll` 네이티브 API(`GetSystemTimes`, `GlobalMemoryStatusEx`, `GetTickCount64`, `GetLogicalDrives`, `GetDriveTypeW`, `GetDiskFreeSpaceExW`)로 1초 주기 경량 수집 구현
  - 사용률 임계치별 라이트 테마 상태 컬러(`<70%` Emerald/Slate, `70~84%` Amber, `>=85%` Rose) 및 상세 호버 툴팁 적용
- **영향 범위**: `elvis-log-viewer/src/main.rs`, `elvis-log-viewer/README.md`

### [2026-11-20] 독립 Rust 네이티브 GUI 5분할 실시간 로그 뷰어 및 프로세스 진단 모듈 신규 구성
- **과제/커밋**: `[3368]WBS-COMMON-OP-007` (독립 Rust 네이티브 로그·프로세스 뷰어 모듈 신규 개발)
- **변경 내용**:
  - `Cargo.toml` / `src/main.rs`: `eframe 0.31` 기반 순수 Rust 네이티브 Windows GUI 앱(`elvis-log-viewer`) 신규 구성
  - 5분할 격자(`1:WS · 3:SYNC · 2+4:API · 5:MySQL · 6:Kafka`), 코어 3분할, 미들웨어 2분할 및 4축 마우스 드래그 스플리터 리사이즈 구현
  - 기본 `줄바꿈 OFF` 시 네이티브 가로+세로 스크롤바(`ScrollArea::both` + `TextWrapMode::Extend`) 및 `줄바꿈 ON` 실시간 토글 지원
  - 라이트 테마 기본 적용, `Pretendard`/`Inconsolata`(폴백: `맑은 고딕`/`Consolas`) 한글 폰트 자동 로드, 단독 실행 시 `config/paths.env` 자동 인식
  - 패널별 확대/복귀(`ESC`), 복사, 비우기, 자동 스크롤 추적(`최신↓`), 전체/개별 검색 필터, `ERROR`/`WARN`/`[OCPP]` 컬러 하이라이트 구현
  - 개별 프로세스 단일 보기 모드(`LogGroup::Single`, CLI `--service ws|sync|api|mysql|kafka` 지원) 및 상단 툴바 5대 서비스 원클릭 전환(`WS | SYNC | API | MySQL | Kafka`) 추가, `f32` 컴파일 경고 7건 정리
  - **프로세스 및 포트 실시간 진단 뷰 모드(`LogGroup::Process`, CLI `--mode process --target all|infra|core`) 추가**: 비동기 백그라운드 스레드로 TCP LISTEN 포트(8종) 및 `mysqld`/`java`/`node` 세부 PID·메모리 현황을 2.5초 주기로 수집하고, 행별 `로그` 바로가기·2단계 안전 `종료(PID)`·특정 포트 강제 종료 UI를 라이트 테마로 통합 구현
  - **로그 레벨 헤더 파싱 정확도 개선 (`detect_log_level`)**: 본문 문자열(`"errorCode":"NoError"`, `0 transport error` 등) 때문에 `INFO`/`DEBUG` 로그가 빨간색(`ERROR`)으로 오탐되던 문제를 수정해 Spring Boot(`---` 이전 헤더)·Kafka(`[...] LEVEL`)·MySQL(`[ERROR]`/`[Warning]`/`[System]`) 로그 레벨 토큰 기준으로 정확히 분류·필터링하도록 개선
- **영향 범위**: `elvis-log-viewer` 신규 독립 모듈 및 `elvis-launcher` 연동
