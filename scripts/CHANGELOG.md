# scripts 인프라·기동 스크립트 변경 로그 이력 (CHANGELOG)

> Windows Native(Zero-Docker) 환경의 MySQL, Kafka, 백엔드·UI 기동 및 초기화 스크립트 변경 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: DB/Kafka 초기화·기동·스키마 적재 스크립트, 데몬 실행기(`run-daemon.ps1`/`vbs`), 포트/경로 연동 로직 변경
- **작성 항목**: 날짜, WBS/커밋 ID, 담당자, 변경 요약, 영향 범위

---

## 변경 이력

### [2026-11-19] 실행 스크립트 콘솔 출력 이모지 제거 및 텍스트 표준화
- **과제/커밋**: `[3368]WBS-COMMON-OP-006` (전 모듈 로그·콘솔 텍스트 표준화)
- **변경 내용**:
  - `start-elvis-launcher.bat`, `start-ui.ps1`: Windows 콘솔 깨짐 방지를 위해 출력 메시지 내 이모지 제거 및 `[INFO]`/`[OK]` 텍스트 접두사로 통일
- **영향 범위**: 로컬 런처·UI 기동 스크립트 콘솔 출력

### [2026-11-19] 백엔드 DevTools 자동 반영용 Gradle 연속 컴파일 워처 스크립트 추가
- **과제/커밋**: `[3368]` (개발 환경 DevTools 핫 리로드 지원)
- **변경 내용**:
  - `watch-backend-classes.bat`: `elvis-connect-ws`, `elvis-connect-sync`, `elvis-control-api` 3개 모듈의 `classes --continuous` 증분 컴파일 워처 스크립트 추가
- **영향 범위**: VS Code 개발 환경에서 Java/YAML 저장 시 Spring Boot DevTools 자동 재시작 트리거

### [2026-11-17] 로컬 Gradle 9.9.1(`D:\project\lselink\ocpp-lite\gradle\gradle-9.9.1`) 및 `GRADLE_USER_HOME` 연동
- **과제/커밋**: `[3368]WBS-COMMON-OP-003` (`74be7da` 후속 정비)
- **변경 내용**:
  - `start-elvis-connect-ws.bat`, `start-elvis-connect-sync.bat`, `start-elvis-control-api.bat`, `gradlew.bat`에 `GRADLE_USER_HOME=D:\project\lselink\ocpp-lite\gradle\.gradle` 자동 지정 추가
  - `start-elvis-connect-ws.bat`, `start-elvis-connect-sync.bat`, `start-elvis-control-api.bat`가 `D:\project\lselink\ocpp-lite\gradle\gradle-9.9.1`을 1순위로 실행하도록 보완
  - 루트 `gradlew.bat`에서 `gradle-wrapper.jar` 부재 시 `D:\project\lselink\ocpp-lite\gradle\gradle-9.9.1\bin\gradle.bat`(및 `binaries\gradle-*`)로 자동 폴백되도록 수정
  - `gradle/wrapper/gradle-wrapper.properties`의 `distributionUrl`을 실제 존재하지 않던 로컬 경로(`gradle-9.8.2-all.zip`)에서 `gradle-9.9.1-bin.zip` 공식 URL로 정비
- **영향 범위**: 백엔드 3대 서비스(`ws`, `sync`, `api`) 기동 및 Gradle 실행/캐시 경로 전반

### [2026-11-17] MySQL 최초 초기화 안정화 및 Kafka 클래스패스 길이 초과 방어 (`74be7da`)
- **과제/커밋**: `[3368]WBS-COMMON-OP-002` (`74be7da`)
- **변경 내용**:
  - `init-mysql.bat`: `DATA_DIR` 상위 디렉토리(`D:\elvis-lite\data\`) 자동 생성, `%DATA_DIR%\mysql` 기준 초기화 판별, `NO_PAUSE=1` 준수, 실패 시 불완전 디렉토리 정리 및 `exit /b 1` 반환 추가
  - `start-mysql.bat`: `ELVIS_LOG_DIR` 자동 생성, `init-mysql.bat` 실패 시 즉시 중단, `my.ini` 사용 시에도 `paths.env`의 `--datadir`/`--port` 우선 적용, 최초 초기화 직후 `load-mysql-schema.bat` 자동 적재 추가
  - `load-mysql-schema.bat`: `01_schema.sql` / `02_seed_data.sql` 적재 시 `--default-character-set=utf8mb4` 명시
  - `start-kafka-kraft.bat`: Windows 8,191자 명령줄 제한 초과 방지를 위한 `kafka-run-class.bat` 와일드카드 클래스패스(`%BASE_DIR%\libs\*`) 자동 보정 및 `kafka-storage.bat` 실패 시 중단 로직 추가
- **영향 범위**: 런처 및 콘솔 기반 MySQL/Kafka 최초 세팅 및 기동 전체

### [2026-11-16] ClickHouse 스크립트 제거 및 스크립트 정비 (`098f3e9`)
- **과제/커밋**: `[3368]WBS-COMMON-OP-001` (`098f3e9`)
- **변경 내용**:
  - ClickHouse 관련 배치/쉘 스크립트 삭제 및 `check-process-status.ps1`, `create-kafka-topics.bat`, `clear-mysql-data.bat`, `run-daemon.ps1`, 백엔드 기동 스크립트 정비
- **영향 범위**: 로컬 인프라 및 코어 서비스 기동 스크립트 전반
