# elvis-launcher 오류 해결 이력 (TROUBLESHOOTING)

> 통합 런처(`elvis-launcher`, `:1420`) 실행 중 발생하는 프로세스 기동·중지·트레이·로그 뷰어 관련 오류 및 해결 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: Tauri Rust 명령, Vite 미들웨어 API, 데몬 제어, 로그 파일 잠금 관련 오류
- **필수 항목**: 발생 일시, 증상/에러 메시지, 근본 원인, 해결 방법, 관련 파일

---

## 점검 참고 사항

### NOTE-01: 완전 클린 환경에서 `전체 서비스 시작` 시 초기화 타이밍
- **증상 가능성**: `D:\elvis-lite\data`가 없는 최초 환경에서 `전체 서비스 시작`을 한 번에 누르면 `start_all_services_sequence()`(`lib.rs`) 및 `vite.config.ts`가 Kafka 기동 2.5초 후 MySQL, 3~4초 후 코어 백엔드를 실행하므로, MySQL 최초 초기화·스키마 적재가 끝나기 전에 `elvis-connect-sync`가 접속을 시도해 타임아웃이 발생할 수 있습니다.
- **권장 운용**: 완전 최초 기동 시에는 **인프라 미들웨어(`MySQL`, `Kafka`) 먼저 시작 → `ONLINE` 확인 → 코어 백엔드 시작** 순서를 권장합니다.

### NOTE-02: Tauri `에러로그` vs Vite 웹 모드 `에러로그` 동작 차이
- **상세 내용**: `vite.config.ts`는 `{service}-err.log` 존재 시 전용 에러 로그를 우선 참조하지만, `src-tauri/src/lib.rs`의 `open_log_window`는 `{service}.log`를 정규식 필터링(`ERROR|Exception|FATAL|...`)하는 구조입니다. (하위 배치 스크립트에서 `pause` 등으로 프로세스가 걸리면 `{service}.log`가 잠겨 후속 기동 로그가 기록되지 않을 수 있습니다.)

---

## 오류 이력

### [2026-11-24] `icons/icon.ico` 교체 후 재빌드 시 `.exe` 아이콘 미갱신
- **발생 일시**: `2026-11-24`
- **증상 / 에러 메시지**: `src-tauri/icons/icon.ico`를 공식 제품 아이콘으로 교체하고 `tauri build --no-bundle`을 재실행했으나, 생성된 `D:\elvis-lite\elvis-launcher.exe`에 이전 리소스 아이콘이 유지됨.
- **근본 원인**: `tauri_build::build()`가 `icons/icon.ico` 파일 변경을 `cargo:rerun-if-changed`로 감지하지 않아, Cargo가 기존 `target/release/build/elvis-launcher-*/out/resource.lib` 캐시를 그대로 링크함.
- **해결 방법**: `src-tauri/build.rs`에 `icons/icon.ico` 외 필수 아이콘 5종의 `cargo:rerun-if-changed` 지시어를 추가하고 기존 `target/release/build/elvis-launcher-*` 산출물을 정리한 뒤 재빌드하여 해결.
- **관련 파일**: `elvis-launcher/src-tauri/build.rs`, `elvis-launcher/src-tauri/icons/icon.ico`

### [TEMPLATE] 오류 제목
- **발생 일시**: `YYYY-MM-DD`
- **증상 / 에러 메시지**:
  ```text
  여기에 실제 콘솔/로그 에러 메시지를 기록합니다.
  ```
- **근본 원인**: 원인 분석 내용
- **해결 방법**: 적용한 조치 및 재발 방지 방안
- **관련 파일**: `elvis-launcher/...`
