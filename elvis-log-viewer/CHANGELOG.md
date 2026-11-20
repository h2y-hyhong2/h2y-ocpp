# elvis-log-viewer 변경 로그 이력 (CHANGELOG)

> ELVIS CSMS Lite 순수 Rust 네이티브 GUI 5분할 실시간 로그 뷰어(`eframe` / `egui`)의 변경 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: UI 레이아웃·기능 추가/수정, 로그 파싱·성능 개선, CLI 인자 및 `config/paths.env` 연동 변경
- **작성 항목**: 날짜, WBS/커밋 ID, 변경 요약, 영향 범위
- **오류 해결 이력**: [TROUBLESHOOTING.md](TROUBLESHOOTING.md)에서 별도 관리

---

## 변경 이력

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
- **영향 범위**: `elvis-log-viewer` 신규 독립 모듈 및 `elvis-launcher` 연동
