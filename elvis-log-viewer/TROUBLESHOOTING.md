# elvis-log-viewer 오류 해결 이력 (TROUBLESHOOTING)

> `elvis-log-viewer` 빌드·실행·폰트·로그 감시 관련 트러블슈팅 이력을 기록합니다.

---

## 트러블슈팅 이력

### [2026-11-20] 초기 모듈 구성 안내
- **빌드 방법**: `cd elvis-log-viewer && cargo build --release`
- **단독 실행**: `elvis-log-viewer\target\release\elvis-log-viewer.exe` 실행 시 상위 워크스페이스의 `config/paths.env`를 자동 탐색하여 `ELVIS_LOG_DIR` 및 5대 서비스 포트를 로드합니다.
- **런처 연동**: `elvis-launcher`의 `전체 로그 / 전체 에러` 및 그룹별 `로그 / 에러` 버튼 클릭 시 `elvis-log-viewer/target/release/elvis-log-viewer.exe`(또는 `target/debug/elvis-log-viewer.exe`)를 자동 감지하여 실행합니다.
