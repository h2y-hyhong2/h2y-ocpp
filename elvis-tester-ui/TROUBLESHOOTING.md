# elvis-tester-ui 오류 해결 이력 (TROUBLESHOOTING)

> 가상 충전기 테스터 UI(`elvis-tester-ui`, `:1422`)의 설치·기동·WebSocket 연결 오류 및 해결 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: `pnpm install`/포트(`1422`) 충돌, Tauri/Vite 실행, `elvis-connect-ws`(`:9080`) WebSocket 연결 오류
- **필수 항목**: 발생 일시, 증상/에러 메시지, 근본 원인, 해결 방법, 관련 파일

---

## 오류 이력

### [2026-11-24] `pnpm tauri:build:exe` 실행 시 `tsconfig.json` 부재로 `vue-tsc` 실패 및 아이콘 재빌드 미반영
- **발생 일시**: `2026-11-24`
- **증상 / 에러 메시지**: `pnpm build`(`vue-tsc && vite build`) 실행 시 `tsconfig.json`이 없어 `vue-tsc`가 도움말만 출력하고 종료 코드 1로 실패함. 또한 `ChargerListPanel.vue`의 `getConnectors` 폴백 객체 타입 불일치 오류 발생.
- **근본 원인**: `elvis-tester-ui/tsconfig.json` 미생성 및 `getConnectors` 반환 타입 정의 누락, Tauri `build.rs`의 아이콘 변경 감지(`cargo:rerun-if-changed`) 미선언.
- **해결 방법**: `tsconfig.json` 생성, `ChargerListPanel.vue` `getConnectors` 반환 타입 보강, `src-tauri/build.rs`에 필수 아이콘 5종의 `cargo:rerun-if-changed` 지시어 추가.
- **관련 파일**: `elvis-tester-ui/tsconfig.json`, `elvis-tester-ui/src/components/ChargerListPanel.vue`, `elvis-tester-ui/src-tauri/build.rs`

### [TEMPLATE] 오류 제목
- **발생 일시**: `YYYY-MM-DD`
- **증상 / 에러 메시지**:
  ```text
  여기에 실제 콘솔/로그 에러 메시지를 기록합니다.
  ```
- **근본 원인**: 원인 분석 내용
- **해결 방법**: 적용한 조치 및 재발 방지 방안
- **관련 파일**: `elvis-tester-ui/...`
