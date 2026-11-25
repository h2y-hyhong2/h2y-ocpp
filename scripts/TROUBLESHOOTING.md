# scripts 인프라·기동 스크립트 오류 해결 이력 (TROUBLESHOOTING)

> 로컬 미들웨어(MySQL, Kafka) 초기화·기동 및 백엔드/UI 실행 스크립트에서 발생한 오류 원인과 해결 이력을 관리합니다.

## 기록 규칙
- **기록 대상**: 초기화 실패, Windows 배치/명령줄 제한, 포트/로그 잠금, Gradle/JDK 경로 오류
- **필수 항목**: 발생 일시, 증상/에러 메시지, 근본 원인, 해결 방법, 관련 파일

---

## 오류 이력

### ISSUE-01: 런처에서 MySQL 최초 초기화 시 `OS errno 2` 및 로그 파일 잠금 정체
- **발생 일시**: `2026-11-17`
- **증상 / 에러 메시지**:
  ```text
  mysqld: Can't create directory 'D:\elvis-lite\data\mysql\' (OS errno 2 - No such file or directory)
  [ERROR] [MY-013236] [Server] The designated data directory D:\elvis-lite\data\mysql\ is unusable.
  [ERROR] MySQL 초기화 중 오류가 발생했습니다.
  Press any key to continue . . .
  ```
- **근본 원인**:
  1. `D:\elvis-lite\data` 상위 디렉토리가 없는 상태에서 `mysqld.exe --initialize-insecure --datadir="D:\elvis-lite\data\mysql"`이 중간 부모 디렉토리를 자동 생성하지 못해 실패함.
  2. `init-mysql.bat`이 `NO_PAUSE=1`을 확인하지 않고 무조건 `pause`를 호출해 무창 백그라운드 데몬(`wscript.exe` / `cmd.exe`)이 종료되지 않고 `D:\elvis-lite\logs\mysql.log`를 점유한 채 대기함.
  3. 최초 초기화 직후 `load-mysql-schema.bat`이 자동 실행되지 않아 `elvis_lite` DB 및 `elvis` 계정이 생성되지 않는 문제가 있었음.
- **해결 방법**:
  1. `init-mysql.bat`에서 `DATA_DIR` 상위 디렉토리 자동 생성, `NO_PAUSE=1` 분기 적용, 실패 시 `exit /b 1` 반환.
  2. `start-mysql.bat`에서 초기화 실패 시 기동 중단 및 최초 초기화 직후 `load-mysql-schema.bat` 자동 실행(UTF-8 `utf8mb4` 적용).
- **관련 파일**: `scripts/init-mysql.bat`, `scripts/start-mysql.bat`, `scripts/load-mysql-schema.bat`

---

### ISSUE-02: Apache Kafka KRaft 기동 시 `The input line is too long.` 오류
- **발생 일시**: `2026-11-17`
- **증상 / 에러 메시지**:
  ```text
  [1/2] Checking KRaft storage format...
  The input line is too long.
  The syntax of the command is incorrect.
  ```
- **근본 원인**:
  - `binaries\kafka_2.13-4.3.1\bin\windows\kafka-run-class.bat`가 `libs\` 내 108개 JAR의 절대경로를 모두 `%CLASSPATH%`에 나열(약 11,019자)하면서 Windows `cmd.exe` 최대 명령줄 길이 제한(8,191자)을 초과함.
- **해결 방법**:
  - `kafka-run-class.bat`의 `libs\*` 반복문을 Java 와일드카드 클래스패스(`call :concat "%BASE_DIR%\libs\*"`)로 수정함.
  - `binaries/*`가 `.gitignore` 제외 대상이므로, `scripts/start-kafka-kraft.bat` 기동 시에도 미패치 상태의 `kafka-run-class.bat`를 감지하면 자동으로 와일드카드 패치를 적용하도록 방어 코드를 추가함.
- **관련 파일**: `scripts/start-kafka-kraft.bat`, `binaries/kafka_2.13-4.3.1/bin/windows/kafka-run-class.bat`

---

### ISSUE-03: 신규 PC에서 `gradlew.bat` 실행 시 `gradle-wrapper.jar` 및 로컬 Gradle 경로 미존재 오류 (해결 완료)
- **발생/확인 일시**: `2026-11-17`
- **증상 / 원인**:
  - `.gitignore`의 `*.jar` 규칙으로 `gradle/wrapper/gradle-wrapper.jar`가 포함되지 않았고, `gradle-wrapper.properties`의 `distributionUrl`도 존재하지 않는 로컬 파일(`file:/D:/project/lselink/ocpp-lite/gradle/gradle-9.8.2-all.zip`)을 가리키고 있었음.
- **해결 방법**:
  - `D:\project\lselink\ocpp-lite\gradle\gradle-9.9.1` 경로 기준으로 `start-elvis-connect-ws.bat`·`start-elvis-connect-sync.bat`·`start-elvis-control-api.bat` 및 루트 `gradlew.bat`가 1순위로 실행하도록 수정(`binaries\gradle-*` 폴백 유지).
  - `gradle/wrapper/gradle-wrapper.properties`의 `distributionUrl`을 `https://services.gradle.org/distributions/gradle-9.9.1-bin.zip`으로 정비.
- **관련 파일**: `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `scripts/start-elvis-connect-ws.bat`, `scripts/start-elvis-connect-sync.bat`, `scripts/start-elvis-control-api.bat`

---

### ISSUE-04: Windows `.bat` 파일 LF 개행 시 `goto` 라벨 깨짐 및 드라이브 루트 `mkdirSync` `EPERM` 오류
- **발생 일시**: `2026-11-24`
- **증상 / 에러 메시지**:
  ```text
  'iewer' is not recognized as an internal or external command...
  Error: EPERM: operation not permitted, mkdir 'D:\'
  ```
- **근본 원인**:
  1. `chcp 65001` UTF-8 `.bat` 파일이 Unix LF(`\n`) 개행으로 저장되면 Windows `cmd.exe`의 `goto` 오프셋 계산이 어긋나 라벨 중간부터 명령으로 해석됨.
  2. `copy-exe.mjs`에서 이미 존재하는 드라이브 루트(`D:\`)에 `fs.mkdirSync(..., { recursive: true })`를 무조건 호출할 때 Windows에서 `EPERM`이 발생함.
- **해결 방법**:
  1. `scripts/build-desktop-exes.bat`, `elvis-log-viewer/build-release.bat`을 CRLF(`\r\n`, UTF-8 무BOM) 개행으로 통일.
  2. `scripts/copy-exe.mjs`에서 `if (!fs.existsSync(destDir))` 검사 후 디렉토리를 생성하고 배포 경로를 `D:\elvis-lite`로 일원화.
- **관련 파일**: `scripts/build-desktop-exes.bat`, `scripts/copy-exe.mjs`, `elvis-log-viewer/build-release.bat`

---

### ISSUE-05: 런처에서 서비스 기동 무응답 (`Win32_Process.Create code 21`) 및 MySQL `my.ini` UTF-8 BOM 파싱 실패
- **발생 일시**: `2026-11-25`
- **증상 / 에러 메시지**:
  ```text
  D:\elvis-lite\scripts\run-daemon.ps1 : FAILED: mysql failed to start with code 21
  mysqld: [ERROR] Found option without preceding group in config file D:\elvis-lite\config\mysql\my.ini at line 1.
  mysqld: [ERROR] Fatal error in defaults handling. Program aborted!
  ```
- **근본 원인**:
  1. `scripts/run-daemon.ps1`이 UTF-8(무BOM) + LF(`\n`)로 저장되면서 Windows PowerShell 5.1이 한글 주석을 CP949로 오독했고, 주석 끝 한글 바이트와 개행(`\n`)이 2바이트 문자로 합쳐져 바로 다음 줄(`$wscriptExe = ...`, `CommandLine = [string]$cmdLine`)이 주석으로 먹혀 `Win32_Process.Create` 호출 시 `CommandLine` 누락(`ReturnValue = 21`)이 발생함.
  2. `scripts/deploy-elvis-lite.ps1` 및 `scripts/start-kafka-kraft.bat`에서 PowerShell 5.1 `Set-Content -Encoding UTF8`이 파일 선두에 UTF-8 BOM(`EF BB BF`)을 삽입해 MySQL(`my.ini`)이 1행 `[mysqld]` 섹션을 인식하지 못하고 즉시 종료됨.
- **해결 방법**:
  1. `scripts/run-daemon.ps1`, `scripts/deploy-elvis-lite.ps1`을 UTF-8(BOM 포함) + CRLF로 저장하고, `run-daemon.ps1` / `run-daemon.vbs` 주석을 순수 ASCII로 정리.
  2. `scripts/deploy-elvis-lite.ps1`과 `scripts/start-kafka-kraft.bat`에서 설정 파일(`paths.env`, `my.ini`, `server.properties`, `kafka-server.runtime.properties`) 기록 시 `[System.Text.UTF8Encoding]::new($false)`(UTF-8 무BOM)을 명시 적용.
- **관련 파일**: `scripts/run-daemon.ps1`, `scripts/run-daemon.vbs`, `scripts/deploy-elvis-lite.ps1`, `scripts/start-kafka-kraft.bat`
