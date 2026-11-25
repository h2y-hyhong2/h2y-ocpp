# CPO[^CPO] CSMS[^CSMS] Lite (Integrated CSMS Platform)

전기차 충전 인프라의 효율적 관리를 위한 CPO(Charge Point Operator) 전용 고성능 통합 관리 시스템(CSMS) 프로젝트 저장소입니다.  
Docker 없이 Windows 네이티브 단독 환경(Zero-Docker, Standalone)에서 MySQL, Apache Kafka(KRaft), OCPP 1.6J 백엔드 3종, 네이티브 데스크톱 앱 4종을 일괄 기동·관제할 수 있도록 구성되어 있습니다.

---

## 1. 문서 인덱스 (Documentation Index)

| 문서명                                 | 주요 내용                                                                                        | 저장 경로                                                                |
| :------------------------------------- | :----------------------------------------------------------------------------------------------- | :----------------------------------------------------------------------- |
| **01. 제품 요구사항 정의서 (PRD)**     | 프로젝트 개요, 시스템 아키텍처 후보군 비교, 실시간 관제/정산 요건, 하드웨어 권장 스펙            | `doc/01.standard_solution/01.prd.md`                                     |
| **02. 통합 기능 분류 정의서**          | AS-IS 150여 개 화면 분석 기반 Phase 1(필수) / Phase 2(선택) 분류 매트릭스                        | `doc/01.standard_solution/02.feature_specification.md`                   |
| **03. 예상 개발 일정 정의서**          | Phase 1 (Standalone 6개월) 및 Phase 2 (HA[^HA] 분산 6개월) 주차별 마일스톤                       | `doc/01.standard_solution/03.timeline.md`                                |
| **04. 소프트웨어 아키텍처 정의서**     | EIP[^EIP] 패턴 설계, 가상 스레드 분배, 분산 세션 관리, CQRS[^CQRS] 데이터 구조 및 EAI[^EAI] 매핑 | `doc/01.standard_solution/04.software_architecture.md`                   |
| **05. Phase 1 기술 검증 정의서 (POC)** | Phase 1 인프라 구성, EIP 코드, 벌크 저장 설정 및 시뮬레이터 검증 시나리오                        | `doc/01.standard_solution/05.poc.md`                                     |
| **CI/BI 마스터 컬러 가이드**           | E-LINK / LS 브랜드 컬러 시스템, 라이트 테마 기준 60-30-10 컬러 위계 및 상태 색상 토큰            | [docs/CI_COLOR_GUIDE.md](docs/CI_COLOR_GUIDE.md)                         |
| **OCPP 1.6J 프로토콜 규약서**          | LSELINK 표준 OCPP 1.6J 전문 규격(Core, 확장 `DataTransfer`, 결제, 원격제어) 및 CSMS Lite 매핑    | [docs/OCPP_1.6J_PROTOCOL_SPEC.md](docs/OCPP_1.6J_PROTOCOL_SPEC.md)       |
| **OCPP 1.6J 엔진 점검 체크리스트**     | 보안 제외 전 전문 대상 엔진 구현 현황 점검 결과 및 Phase 1~3 보완 체크리스트                     | [docs/OCPP_1.6J_ENGINE_CHECKLIST.md](docs/OCPP_1.6J_ENGINE_CHECKLIST.md) |
| **외부 설정 및 배포 설정 가이드**      | `config/` 외부 설정 오버라이드 및 미들웨어 실행 가이드                                           | [config/README.md](config/README.md)                                     |
| **로컬 기동 스크립트 가이드**          | Windows Native(Zero-Docker) 미들웨어/서비스/테스터 기동·배포 스크립트 안내                       | [scripts/README.md](scripts/README.md)                                   |

---

## 2. 사용 소프트웨어 목록 (Software & Toolchain Stack)

### 2.1 개발 런타임 및 빌드 툴체인

| 분류                | 소프트웨어                                          | 검증 버전          | 용도 및 비고                                                |
| :------------------ | :-------------------------------------------------- | :----------------- | :---------------------------------------------------------- |
| **Java Runtime**    | Eclipse Temurin OpenJDK                             | `25.0.4.1+1-LTS`   | 백엔드 3종 런타임 (Virtual Threads, No-Lombok)              |
| **Java Build**      | Gradle                                              | `9.9.1`            | 멀티모듈 빌드 및 Boot JAR 패키징 (`gradle.properties` 연동) |
| **Node Runtime**    | Node.js (`fnm` 권장)                                | `v24.22.2`         | 프론트엔드 개발 서버(Vite) 및 빌드 스크립트(`copy-exe.mjs`) |
| **Package Manager** | pnpm                                                | `12.16.1`          | 3종 UI 모듈 의존성 관리 및 Tauri CLI 실행                   |
| **Rust Toolchain**  | `rustc` / `cargo`                                   | `1.99.1`           | Tauri 2.0 데스크톱 3종 및 순수 Rust 로그 뷰어 빌드          |
| **Windows Native**  | Microsoft WebView2 Runtime / Windows SDK (`rc.exe`) | Windows 10/11 기본 | Tauri 2.0 WebView 렌더링 및 `.exe` 리소스 아이콘 임베딩     |

### 2.2 인프라 및 미들웨어 (로컬 내장 바이너리)

| 소프트웨어           | 버전                           |         기본 포트         | 저장 경로 (워크스페이스 / 배포) | 역할                                                             |
| :------------------- | :----------------------------- | :-----------------------: | :------------------------------ | :--------------------------------------------------------------- |
| **MySQL Server**     | `9.7.1-winx64`                 |          `13306`          | `binaries/mysql-9.7.1-winx64/`  | OLTP[^OLTP] 기준정보, 실시간 상태, 세션, 과금 원장(CDR) 저장     |
| **Apache Kafka**     | `2.13-4.3.1` (KRaft)           | `19092` (컨트롤러 `9093`) | `binaries/kafka_2.13-4.3.1/`    | ZooKeeper 없는 단독 KRaft 브로커 (`ocpp-raw-events` 등 4대 토픽) |
| **Caffeine / Redis** | Caffeine (내장) / Redis (선택) |       `6379` (선택)       | —                               | L1 인메모리 캐시 기본 동작, Redis 미기동 시 자동 인메모리 폴백   |

### 2.3 애플리케이션 프레임워크 및 주요 라이브러리

| 영역             | 기술 스택                                                                                 | 주요 버전 / 특징                                                                                          |
| :--------------- | :---------------------------------------------------------------------------------------- | :-------------------------------------------------------------------------------------------------------- |
| **Backend Core** | Spring Boot, Spring WebSocket, Spring Kafka, Spring Security (JWT), Actuator/Micrometer   | Spring Boot `4.1.2`, Java 25 Virtual Threads, Logback 파이프 표준 로그 + MDC(`charger`, `msgId`)          |
| **Data Access**  | Spring Data JPA, QueryDSL (Jakarta), Flyway, HikariCP                                     | QueryDSL `5.1.0:jakarta`, Flyway V1~V6 자동 마이그레이션, ISO 11179 단수형 테이블 명명                    |
| **Frontend UI**  | Vue 3 (Composition API[^API]), TypeScript, Vite, Tailwind CSS, Pinia, Vue Router, ECharts | Vue `3.4`, TypeScript `5.5`, Vite `5.4`, Tailwind `3.4`, 로컬 폰트(`Pretendard`, `Inconsolata`) 내장      |
| **Desktop Apps** | Tauri 2.0 (`@tauri-apps/cli`, `tauri`), `eframe` / `egui`                                 | Tauri `2.0` (`elvis-launcher`, `elvis-control-ui`, `elvis-tester-ui`), `eframe 0.31` (`elvis-log-viewer`) |

---

## 3. 모듈 구성, 프로젝트 폴더 구조 및 변경 이력

### 3.1 모듈 구성 및 포트 체계

| 모듈명                   | 유형               |  기본 포트   | 주요 역할                                                            | 변경 이력                                    | 오류 해결 이력                                           |
| :----------------------- | :----------------- | :----------: | :------------------------------------------------------------------- | :------------------------------------------- | :------------------------------------------------------- |
| **`elvis-connect-ws`**   | Spring Boot        |    `9080`    | OCPP 1.6J WebSocket 게이트웨이 수신 및 하향 커맨드 전송              | [CHANGELOG](elvis-connect-ws/CHANGELOG.md)   | [TROUBLESHOOTING](elvis-connect-ws/TROUBLESHOOTING.md)   |
| **`elvis-connect-sync`** | Spring Boot        |    `9081`    | Kafka 이벤트 파싱, MySQL 상태·세션·계측·TOU 과금 동기화              | [CHANGELOG](elvis-connect-sync/CHANGELOG.md) | [TROUBLESHOOTING](elvis-connect-sync/TROUBLESHOOTING.md) |
| **`elvis-control-api`**  | Spring Boot        |    `9082`    | 관제 REST API, WebSocket/SSE 푸시, JWT 인증 및 과금 원장 조회        | [CHANGELOG](elvis-control-api/CHANGELOG.md)  | [TROUBLESHOOTING](elvis-control-api/TROUBLESHOOTING.md)  |
| **`elvis-launcher`**     | Tauri 2.0 / Vue 3  | `1420` (Dev) | 인프라·코어 5대 서비스 통합 기동/종료 런처 및 시스템 트레이          | [CHANGELOG](elvis-launcher/CHANGELOG.md)     | [TROUBLESHOOTING](elvis-launcher/TROUBLESHOOTING.md)     |
| **`elvis-control-ui`**   | Tauri 2.0 / Vue 3  | `1421` (Dev) | 관제 데스크톱 센터 (`충전기 목록` `/` · `과금 원장` `/billing`)      | [CHANGELOG](elvis-control-ui/CHANGELOG.md)   | [TROUBLESHOOTING](elvis-control-ui/TROUBLESHOOTING.md)   |
| **`elvis-tester-ui`**    | Tauri 2.0 / Vue 3  | `1422` (Dev) | 듀얼건 가상 충전기(EVSE) 에뮬레이터 및 OCPP 패킷 시뮬레이터          | [CHANGELOG](elvis-tester-ui/CHANGELOG.md)    | [TROUBLESHOOTING](elvis-tester-ui/TROUBLESHOOTING.md)    |
| **`elvis-log-viewer`**   | Rust (`eframe`)    |      —       | 순수 Rust 5분할 실시간 로그 뷰어, 에러 분할 및 포트·프로세스 진단    | [CHANGELOG](elvis-log-viewer/CHANGELOG.md)   | [TROUBLESHOOTING](elvis-log-viewer/TROUBLESHOOTING.md)   |
| **`scripts`**            | Batch / PowerShell |      —       | Windows Native 인프라 초기화, 백그라운드 데몬 기동, 빌드·배포 자동화 | [CHANGELOG](scripts/CHANGELOG.md)            | [TROUBLESHOOTING](scripts/TROUBLESHOOTING.md)            |

### 3.2 프로젝트 저장소 폴더 구조 (`h2y-ocpp/`, 2레벨 기준)

| 1레벨 | 2레벨 | 주요 파일 / 하위 구성 | 설명 |
| :--- | :--- | :--- | :--- |
| **`/` (루트)** | — | `build.gradle`, `settings.gradle`, `gradle.properties`, `gradlew.bat`, `start-ui.bat`, `start-tester.bat`, `check-status.bat`, `AGENTS.md`, `README.md` | 멀티모듈 Gradle 설정, 개발 실행 래퍼 및 거버넌스 문서 |
| **`.vscode/`** | — | `settings.json`, `tasks.json` | VS Code 작업 공간 설정 및 4대 앱·인프라·빌드 태스크 |
| **`docs/`** | — | `CI_COLOR_GUIDE.md`, `OCPP_1.6J_PROTOCOL_SPEC.md`, `OCPP_1.6J_ENGINE_CHECKLIST.md` | 컬러 가이드, OCPP 1.6J 프로토콜 규약서 및 점검 체크리스트 |
| **`icons/`** | — | `elvis_lite_*_light.svg`, `elvis_lite_*_dark.svg` | 4대 데스크톱 앱 공식 제품 SVG 아이콘 원본 |
| **`libs/`** | — | `ocpp_core-1.0-SNAPSHOT.jar` | 사내 공용 OCPP 코어 라이브러리 JAR |
| **`binaries/`** | `mysql-9.7.1-winx64/` | `bin/mysqld.exe`, `bin/mysql.exe` | 로컬 개발용 MySQL 9.71 서버 바이너리 |
| **`binaries/`** | `kafka_2.13-4.3.1/` | `bin/windows/kafka-server-start.bat`, `kafka-storage.bat` | 로컬 개발용 Apache Kafka 4.3.1 (KRaft) 바이너리 |
| **`config/`** | *(루트)* | `paths.env`, `README.md` | 공통 데이터/로그 경로(`%ELVIS_HOME%`) 및 포트 환경변수 |
| **`config/`** | `mysql/` | `my.ini`, `01_schema.sql`, `02_seed_data.sql` | MySQL 설정, ISO 11179 통합 DDL 및 시드 데이터(RFID 포함) |
| **`config/`** | `kafka/` | `server.properties` | Kafka KRaft 단독 브로커 설정 |
| **`config/`** | `connect-ws/` | `application.yml` | WS 게이트웨이 외부 오버라이드 설정 |
| **`config/`** | `connect-sync/` | `application.yml` | Sync 엔진 외부 오버라이드 설정 |
| **`config/`** | `control-api/` | `application.yml` | Control API 외부 오버라이드 설정 |
| **`scripts/`** | — | `run-daemon.ps1/.vbs`, `start-*.bat`, `init-mysql.bat`, `load-mysql-schema.bat`, `create-kafka-topics.bat`, `watch-backend-classes.bat`, `build-desktop-exes.bat`, `copy-exe.mjs`, `deploy-elvis-lite.bat/.ps1` | 무창 데몬 기동, DB/Kafka 초기화, DevTools 워처, Release 빌드 및 로컬 배포 |
| **`elvis-connect-ws/`** | `src/` | `main/java/.../ws/`(`handler`, `kafka`, `session`, `util`, `filter`, `config`), `main/resources/`(`application.yml`, `logback-spring.xml`) | OCPP 1.6J WebSocket 게이트웨이(`:9080`) 소스 및 파이프 로그 설정 |
| **`elvis-connect-sync/`** | `src/` | `main/java/.../sync/`(`consumer`, `ocpp`, `service`, `repository`, `domain`, `util`, `filter`), `main/resources/`(`application.yml`, `logback-spring.xml`, `db/migration/V1~V6`) | Kafka 이벤트 파싱, MySQL 상태·세션·과금 동기화(`:9081`) 및 Flyway DDL |
| **`elvis-control-api/`** | `src/` | `main/java/.../control/`(`controller`, `service`, `repository`, `entity`, `config`), `main/resources/application.yml` | 관제 REST API(`:9082`), QueryDSL 과금 조회, WebSocket/SSE 푸시 및 JWT 인증 |
| **`elvis-launcher/`** | `public/` | `elvis_lite_light.svg`, `fonts/` | 런처 로고 및 로컬 오프라인 폰트(`Pretendard`, `Inconsolata`) |
| **`elvis-launcher/`** | `src/` | `App.vue`, `main.ts`, `style.css` | Vue 3 통합 런처 UI (경로 설정·설치 판별·서비스 제어) |
| **`elvis-launcher/`** | `src-tauri/` | `src/lib.rs`, `tauri.conf.json`, `icons/`, `Cargo.toml` | Tauri 2.0 Rust 백엔드 (트레이·데몬 실행·개발/Release 경로 분기) |
| **`elvis-control-ui/`** | `public/` | `elvis_lite_control_light.svg`, `fonts/` | 관제 센터 아이콘 및 로컬 오프라인 폰트 |
| **`elvis-control-ui/`** | `src/` | `views/`(`SimpleDashboardView`, `BillingCdrView`, `LoginView`), `stores/`, `router/`, `utils/` | Vue 3 관제 센터 (`충전기 목록` · `과금 원장` 2화면 구성) |
| **`elvis-control-ui/`** | `src-tauri/` | `src/lib.rs`, `tauri.conf.json`, `icons/`, `Cargo.toml` | Tauri 2.0 관제 센터 데스크톱 셸 |
| **`elvis-tester-ui/`** | `public/` | `elvis_lite_tester_light.svg`, `fonts/` | 테스터 아이콘 및 로컬 오프라인 폰트 |
| **`elvis-tester-ui/`** | `src/` | `composables/`, `components/`, `utils/testerApiAuth.ts` | Vue 3 듀얼건 가상 충전기 에뮬레이터 및 JWT 자동 연동 |
| **`elvis-tester-ui/`** | `src-tauri/` | `src/lib.rs`, `tauri.conf.json`, `icons/`, `Cargo.toml` | Tauri 2.0 테스터 데스크톱 셸 |
| **`elvis-log-viewer/`** | `src/` | `main.rs` | 순수 Rust(`eframe`/`egui`) 5분할 실시간 로그·에러 분할·프로세스 뷰어 |
| **`elvis-log-viewer/`** | `icons/` | `icon.ico`, `icon_64.rgba` | Windows 실행파일 리소스 임베딩 및 작업표시줄 아이콘 |

---

## 4. 개발 환경 세팅 (Development Environment Setup)

### 4.1 필수 도구 설치 및 경로 확인

1. **JDK 25 & Gradle 9.9.1**
   - 프로젝트 기본 경로([gradle.properties](gradle.properties) 및 [gradlew.bat](gradlew.bat) 참조):
     - JDK 25: `D:\project\lselink\ocpp-lite\jdk\jdk-25.0.4.1+1`
     - Gradle 9.9.1: `D:\project\lselink\ocpp-lite\gradle\gradle-9.9.1`
     - `GRADLE_USER_HOME`: `D:\project\lselink\ocpp-lite\gradle\.gradle`
   - 다른 경로에 JDK 25를 설치한 경우 `gradle.properties`의 `org.gradle.java.home` 또는 시스템 `JAVA_HOME` 환경변수를 맞춥니다.
2. **Node.js 24 (`fnm`) & `pnpm`**
   ```powershell
   fnm install 24
   fnm use 24
   corepack enable
   node -v   # v24.x
   pnpm -v   # 12.x
   ```
3. **Rust (`rustup`) & MSVC Build Tools**
   - Visual Studio Build Tools(C++ 데스크톱 개발 워크로드, Windows 10/11 SDK 포함) 및 `rustup` 설치:
   ```powershell
   rustc --version
   cargo --version
   ```
4. **미들웨어 바이너리 배치 확인**
   - 워크스페이스 루트의 `binaries/` 하위에 아래 두 폴더가 존재해야 합니다.
     - `binaries\mysql-9.7.1-winx64\bin\mysqld.exe`
     - `binaries\kafka_2.13-4.3.1\bin\windows\kafka-server-start.bat`

### 4.2 프론트엔드 3종 의존성 설치 (최초 1회)

```powershell
cd elvis-launcher   && pnpm install
cd ..\elvis-control-ui && pnpm install
cd ..\elvis-tester-ui  && pnpm install
```

### 4.3 개발 환경 vs Release 배포 경로 분리 원칙

| 구분                              | 개발 환경 (`npm/pnpm dev`, `tauri:dev`, `cargo run`)                                         | Release 빌드 (`D:\elvis-lite\*.exe`)                                      |
| :-------------------------------- | :------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------ |
| **기본 메인 경로 (`ELVIS_HOME`)** | 워크스페이스 루트 (`d:\project\lselink\ocpp-lite\git\h2y-ocpp`)                              | `D:\elvis-lite`                                                           |
| **데이터 / 로그 경로**            | `{워크스페이스}\data`, `{워크스페이스}\logs` (`config/paths.env`의 `%ELVIS_HOME%` 자동 치환) | `D:\elvis-lite\data`, `D:\elvis-lite\logs`                                |
| **런처 경로 설정 저장 파일**      | `%APPDATA%\com.lselink.elvis.launcher\settings-dev.json`                                     | `%APPDATA%\com.lselink.elvis.launcher\settings.json`                      |
| **백엔드 실행 방식**              | `apps\elvis-*.jar` 미존재 시 Gradle `bootRun` 자동 폴백                                      | `apps\elvis-*.jar` 단독 실행 (`config\*\application.yml` 외부 오버라이드) |
| **로그 뷰어 바이너리 탐색**       | `elvis-log-viewer\target\{debug,release}\elvis-log-viewer.exe` 우선                          | `{제품경로}\elvis-log-viewer.exe` 우선                                    |

### 4.4 DB 접속 계정, 관제 관리자 시드 계정 및 테스트 RFID 카드

- **MySQL 9.71 (`127.0.0.1:13306`, DB: `elvis_lite`)**
  - `root` / 비밀번호 없음 (`--initialize-insecure` 초기화)
  - `elvis` / `elvis1234!` (애플리케이션 전용 계정, `elvis_lite.*` 전체 권한)
- **관제 센터(`elvis-control-ui`) 기본 관리자 계정**

| 권한 역할                | 아이디 (Username) | 비밀번호 (Password) | 권한 등급       | 주요 기능 및 권한                                                         |
| :----------------------- | :---------------: | :-----------------: | :-------------- | :------------------------------------------------------------------------ |
| **시스템 최고관리자**    |      `admin`      |     `admin123!`     | `ROLE_ADMIN`    | 충전기 원격제어(리셋/언락/중지/시작) 및 전체 관리 권한 (권장)             |
| **충전 관제 운영자**     |    `operator`     |   `operator123!`    | `ROLE_OPERATOR` | 충전기 원격제어 및 실시간 관제 권한                                       |
| **관제 대시보드 조회자** |     `viewer`      |    `viewer123!`     | `ROLE_VIEWER`   | 대시보드 및 과금 원장 조회 전용 권한 (`elvis-tester-ui` 자동 동기화 계정) |

- **초기 시드 RFID 테스트 카드 (`rfid_card`, [config/mysql/02_seed_data.sql](config/mysql/02_seed_data.sql))**
  - `CARD-CORP-9901`, `CARD-USER-1001`, `CREDIT-IC-01`, `ROAM-ME-7701`

---

## 5. 개발 환경 실행 방법 (Running in Dev Mode)

### 5.1 방법 A: 통합 런처(`elvis-launcher`)로 일괄 기동 (권장)

1. 개발 모드로 런처 실행:
   ```powershell
   # Tauri 데스크톱 앱 실행 (권장)
   cd elvis-launcher
   pnpm tauri:dev

   # 또는 스크립트 메뉴 선택 실행 (1: Tauri 앱, 2: 브라우저 http://127.0.0.1:1420)
   .\scripts\start-elvis-launcher.bat
   ```
2. 런처 상단 경로가 워크스페이스 루트로 자동 지정되어 있는지 확인합니다.
3. **인프라(`MySQL`, `Kafka`)를 먼저 시작**합니다.
   - MySQL 최초 기동 시 `init-mysql.bat` → 서버 기동 → `load-mysql-schema.bat`(`01_schema.sql`, `02_seed_data.sql`)이 자동 실행됩니다.
   - Kafka 최초 기동 시 KRaft 스토리지 포맷 후 브로커(`:19092`)가 기동됩니다. 필요 시 `scripts\create-kafka-topics.bat`으로 4대 토픽을 사전 생성할 수 있습니다.
4. 인프라가 `ONLINE`이 되면 **코어 백엔드(`ws`, `sync`, `api`)**를 기동합니다.

### 5.2 방법 B: 개별 모듈 터미널 실행

- **인프라 (MySQL & Kafka)**
  ```cmd
  scripts\start-all-middleware.bat              :: 콘솔 창 표시 기동
  scripts\start-all-middleware-background.bat   :: 무창 백그라운드 데몬 기동
  ```
- **백엔드 3종 (Gradle `bootRun`) + 자동 컴파일 워처**
  ```cmd
  scripts\start-elvis-connect-ws.bat            :: OCPP WS Gateway (:9080)
  scripts\start-elvis-connect-sync.bat          :: Kafka-MySQL Sync (:9081)
  scripts\start-elvis-control-api.bat           :: Admin REST API (:9082)
  scripts\watch-backend-classes.bat             :: Java/YAML 저장 시 DevTools 자동 리로드 워처
  ```
- **데스크톱 / 웹 UI 4종**
  ```powershell
  # 1) 통합 메뉴 실행기 (Launcher / Control UI / Middleware / Tester 선택)
  .\start-ui.bat

  # 2) 관제 센터 (elvis-control-ui, http://127.0.0.1:1421)
  cd elvis-control-ui && pnpm dev          # 브라우저 개발 모드
  cd elvis-control-ui && pnpm tauri:dev    # Tauri 네이티브 앱 개발 모드

  # 3) 가상 충전기 테스터 (elvis-tester-ui, http://127.0.0.1:1422)
  cd elvis-tester-ui && pnpm dev           # 브라우저 개발 모드
  cd elvis-tester-ui && pnpm tauri:dev     # Tauri 네이티브 앱 개발 모드

  # 4) 실시간 로그·프로세스 뷰어 (elvis-log-viewer)
  cd elvis-log-viewer && cargo run         # 워크스페이스 logs\ 자동 감시
  ```

### 5.3 방법 C: VS Code 태스크 실행

VS Code에서 `Ctrl + Shift + P` → `Tasks: Run Task` 선택 시 [.vscode/tasks.json](.vscode/tasks.json)에 등록된 8개 태스크(`[Launcher]`, `[Control UI]`, `[Tester UI]`, `[Log Viewer]`, `[Infra]`, `[Backend]`, `[Release]`)를 바로 실행할 수 있습니다.

---

## 6. 빌드 및 로컬 배포 방법 (Build & Deployment)

> **주의**: `D:\elvis-lite`에 실행 파일(`.exe`)이나 백엔드 JAR를 덮어쓰기 전에 실행 중인 앱과 백엔드 프로세스를 먼저 종료해야 파일 잠금 오류를 방지할 수 있습니다.

### 6.1 코어 백엔드 3종 Boot JAR 빌드

```powershell
.\gradlew.bat :elvis-connect-ws:bootJar :elvis-connect-sync:bootJar :elvis-control-api:bootJar -x test --no-daemon
```
- 생성 산출물:
  - `elvis-connect-ws\build\libs\elvis-connect-ws-1.0.0-SNAPSHOT.jar`
  - `elvis-connect-sync\build\libs\elvis-connect-sync-1.0.0-SNAPSHOT.jar`
  - `elvis-control-api\build\libs\elvis-control-api-1.0.0-SNAPSHOT.jar`

### 6.2 데스크톱 4대 앱 Release 빌드 및 `D:\elvis-lite` 배포

- **4대 앱 일괄 빌드·배포 (권장)**:
  ```cmd
  scripts\build-desktop-exes.bat all
  ```
- **특정 앱만 선택 빌드·배포**:
  ```cmd
  scripts\build-desktop-exes.bat log-viewer
  scripts\build-desktop-exes.bat launcher
  scripts\build-desktop-exes.bat control-ui
  scripts\build-desktop-exes.bat tester-ui
  ```
- **모듈 폴더에서 직접 실행**:
  ```powershell
  cd elvis-launcher    && pnpm tauri:build:exe    # -> D:\elvis-lite\elvis-launcher.exe
  cd elvis-control-ui  && pnpm tauri:build:exe    # -> D:\elvis-lite\elvis-control-ui.exe
  cd elvis-tester-ui   && pnpm tauri:build:exe    # -> D:\elvis-lite\elvis-tester-ui.exe
  cd elvis-log-viewer  && .\build-release.bat     # -> D:\elvis-lite\elvis-log-viewer.exe
  ```
  - 출력 경로 변경이 필요하면 환경변수 `ELVIS_EXE_OUT_DIR`을 지정합니다([scripts/copy-exe.mjs](scripts/copy-exe.mjs)).

### 6.3 로컬 제품 배포 디렉터리(`D:\elvis-lite`) 일괄 세팅

```cmd
:: 최초 전체 세팅 (config, scripts, MySQL/Kafka, 내장 JDK, JAR 빌드 및 복사, 4대 .exe 복사)
scripts\deploy-elvis-lite.bat D:\elvis-lite -BuildJars

:: 바이너리·JDK가 이미 복사된 상태에서 설정/스크립트/JAR/EXE만 빠르게 갱신
scripts\deploy-elvis-lite.bat D:\elvis-lite -SkipBinaries -SkipJdk
```
- 배포 스크립트는 대상 경로(`TargetDir`) 기준으로 `config/paths.env`, `config/mysql/my.ini`, `config/kafka/server.properties`의 경로를 자동 치환(UTF-8 No BOM)합니다.
- *(참고: 최종 사용자용 단일 설치 인스톨러 `Setup.exe` 패키징은 아직 미착수 상태이며 추후 진행 예정입니다.)*

---

## 7. 설치(배포) 완료 시 폴더 구조 (`D:\elvis-lite\`, 2레벨 기준)

배포 완료 후 제품 메인 경로(`D:\elvis-lite\`, 런처에서 다른 경로로 변경 가능)는 아래의 독립 실행형(Standalone) 구조로 구성됩니다.

| 1레벨 | 2레벨 | 포함 파일 / 하위 구성 | 역할 및 비고 |
| :--- | :--- | :--- | :--- |
| **루트 (`\`)** | *(실행파일)* | `elvis-launcher.exe`, `elvis-control-ui.exe`, `elvis-log-viewer.exe`, `elvis-tester-ui.exe` | 별도 포트 점유 없이 실행되는 4대 네이티브 데스크톱 앱 |
| **`apps\`** | — | `elvis-connect-ws.jar` (`:9080`), `elvis-connect-sync.jar` (`:9081`), `elvis-control-api.jar` (`:9082`) | Spring Boot Fat JAR (외부 `config\` 설정 우선 로드) |
| **`binaries\`** | `jdk\` | `bin\java.exe` (OpenJDK 25) | 시스템 JDK 미설치 PC 지원용 내장 자바 런타임 |
| **`binaries\`** | `mysql-9.7.1-winx64\` | `bin\mysqld.exe`, `bin\mysql.exe` | 내장 MySQL 9.71 서버 바이너리 |
| **`binaries\`** | `kafka_2.13-4.3.1\` | `bin\windows\kafka-server-start.bat`, `libs\*` | 내장 Apache Kafka 4.3.1 (KRaft) 바이너리 |
| **`config\`** | *(루트)* | `paths.env`, `README.md` | 배포 경로(`D:\elvis-lite`) 기준 데이터/로그 경로 및 포트 설정 |
| **`config\`** | `mysql\` | `my.ini`, `01_schema.sql`, `02_seed_data.sql` | MySQL 운영 설정, DDL 스키마 및 초기 시드 데이터 |
| **`config\`** | `kafka\` | `server.properties` | Kafka KRaft 브로커 기본 설정 |
| **`config\`** | `connect-ws\` | `application.yml` | WS 게이트웨이 외부 오버라이드 설정 |
| **`config\`** | `connect-sync\` | `application.yml` | Sync 엔진 외부 오버라이드 설정 |
| **`config\`** | `control-api\` | `application.yml` | Control API 외부 오버라이드 설정 |
| **`scripts\`** | — | `run-daemon.ps1/.vbs`, `start-mysql.bat`, `init-mysql.bat`, `load-mysql-schema.bat`, `clear-mysql-data.bat`, `start-kafka-kraft.bat`, `create-kafka-topics.bat`, `start-elvis-connect-{ws,sync}.bat`, `start-elvis-control-api.bat`, `check-status.bat`, `check-process-status.ps1` | 무창 독립 데몬 기동, DB/Kafka 초기화 및 포트·프로세스 진단 |
| **`data\`** | `mysql\` | MySQL InnoDB 데이터 파일 (최초 기동 시 자동 생성) | `elvis_lite` 데이터베이스 영속 스토리지 |
| **`data\`** | `kafka\` | KRaft 메타데이터 및 토픽 로그 세그먼트 (자동 생성) | Kafka 브로커 영속 스토리지 (`kafka-server.runtime.properties` 포함) |
| **`logs\`** | — | `mysql.log`, `kafka.log`, `api.log`, `ws.log` / `ocpp-ws.log` / `ws-err.log` / `ws-kafka.log`, `sync.log` / `ocpp-sync.log` / `sync-err.log` / `sync-kafka.log` | 콘솔 메인 로그, 일반 롤링(`100MB/7일 .gz`), ERROR 전용(`*-err.log`, 로그 뷰어 에러 분할 연동), Kafka 전용 분리 로그 |

---

## 8. Antigravity MySQL MCP 연동 (선택)

AI 에이전트에서 로컬 `elvis_lite` DB를 직접 조회(기본 `READ-ONLY`)하려면 전역 MCP 설정(`~/.gemini/config/mcp_config.json`)에 아래와 같이 등록합니다.

```bash
# Node 24 환경에 MCP 서버 전역 설치 (fnm 사용 시 절대경로 실행 권장)
npm install -g @benborla29/mcp-server-mysql
```

```json
{
  "mcpServers": {
    "mysql-elvis-lite": {
      "command": "C:\\Users\\<USER>\\AppData\\Roaming\\fnm\\node-versions\\v24.22.2\\installation\\node.exe",
      "args": [
        "C:\\Users\\<USER>\\AppData\\Roaming\\fnm\\node-versions\\v24.22.2\\installation\\node_modules\\@benborla29\\mcp-server-mysql\\dist\\index.js"
      ],
      "env": {
        "MYSQL_HOST": "127.0.0.1",
        "MYSQL_PORT": "13306",
        "MYSQL_USER": "elvis",
        "MYSQL_PASS": "elvis1234!",
        "MYSQL_DB": "elvis_lite"
      }
    }
  }
}
```

---

## 9. 제품 구성 및 작업 현황 (2026-11-25 기준)

| 영역                       | 구성 모듈 / 산출물                                                               | 진행 상태 | 주요 구현 내용                                                                                                                                                                               |
| :------------------------- | :------------------------------------------------------------------------------- | :-------: | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **로컬 배포 스크립트**     | `D:\elvis-lite\`, [scripts/deploy-elvis-lite.ps1](scripts/deploy-elvis-lite.ps1) |   완료    | Standalone 제품 디렉터리 표준화(`apps/`, `binaries/`, `config/`, `scripts/`, `data/`, `logs/`), 무창 독립 데몬 기동(`run-daemon.ps1`/`vbs`), 개발/Release 경로 자동 분기                     |
| **설치 인스톨러 패키징**   | Windows Installer (`Setup.exe` / Inno Setup 등)                                  |  미착수   | 최종 사용자용 단일 설치 마법사 패키징 및 자동 환경 구성 (추후 진행 예정)                                                                                                                     |
| **통합 런처**              | `elvis-launcher` (`elvis-launcher.exe`)                                          |   완료    | 제품 메인 경로 동적 변경·영속 저장, 5대 서비스 설치 여부(`미설치` 배지) 판별, 일괄·개별 제어 및 시스템 트레이 연동                                                                           |
| **로그·프로세스 뷰어**     | `elvis-log-viewer` (`elvis-log-viewer.exe`)                                      |   완료    | 5분할/3분할/2분할/단일/에러 분할 뷰, 하단 1줄 시스템 자원 바(`CPU`/`RAM`/`DISK`/`UPTIME`), OCPP 파이프 표준 로그 레벨 파싱 및 태그 강조                                                      |
| **OCPP 게이트웨이·동기화** | `elvis-connect-ws` (`:9080`), `elvis-connect-sync` (`:9081`)                     |   완료    | OCPP 1.6J 송수신·Kafka 파이프라인·MySQL 실시간 상태/세션/계측/TOU 과금 동기화, `evcs_ocpp_ws`/`evcs_ocpp_rest` 표준 파이프 로그·MDC(`charger`, `msgId`)·에러/Kafka 파일 분리                 |
| **관제 API·관제 UI**       | `elvis-control-api` (`:9082`), `elvis-control-ui` (`elvis-control-ui.exe`)       |   완료    | Tauri 2.0 CORS/WebSocket·JWT 인증 연동, `충전기 목록` + `과금 원장` 2화면 단순화·모던 라이트 디자인 통일, `transaction_session`/`charge_detail_record` 실시간 과금 원장 조회 및 CSV 내보내기 |
| **가상 충전기 테스터**     | `elvis-tester-ui` (`elvis-tester-ui.exe`)                                        |   완료    | 듀얼건 가상 충전기 시뮬레이션, JWT 자동 인증 기반 MySQL 충전기 동기화 및 OCPP E2E 검증 스위트                                                                                                |

---

## 10. 프로젝트 로드맵

1. **Phase 1 (Standalone 단독 배포 모드 - 6개월)**
   - 단일 서버 기동 환경 최적화 및 Windows 설치 인스톨러 패키징
   - OCPP[^OCPP] 1.6J 필수 메시지 수집 및 데이터 파이프라인 처리
   - 기본 정산 원장 계산 및 대시보드 관제 구축
   - 완전 폐쇄망(Air-Gapped) 배포 패키지 구성 및 Zero-Dependency 실행 보장

2. **Phase 2 (HA 분산 확장 클러스터 모드 - 6개월)**
   - L4/L7 로드밸런싱 및 다중 노드 기동
   - Redis 세션 맵 동기화 및 Pub/Sub 명령 라우팅 구축
   - DB[^DB]/Kafka 분산 다중화 (Active-Standby, Replica 구성)
   - OCPP 2.0.1 확장, 스마트 차징 스케줄러, 전기 버스 차고지 관제 및 대외 로밍 연동

---

[^API]: **API (Application Programming Interface):** 응용 프로그램 프로그래밍 인터페이스

[^CPO]: **CPO (Charge Point Operator):** 전기차 충전소 운영 사업자

[^CQRS]: **CQRS (Command Query Responsibility Segregation):** 명령 및 조회 책임 분리 패턴

[^CSMS]: **CSMS (Charging Station Management System):** 충전기 통합 관리 시스템

[^DB]: **DB (Database):** 데이터베이스

[^EAI]: **EAI (Enterprise Application Integration):** 기업 애플리케이션 통합

[^EIP]: **EIP (Enterprise Integration Patterns):** 기업 통합 패턴 (소프트웨어 아키텍처 디자인 패턴)

[^HA]: **HA (High Availability):** 고가용성 (서버 다중화 등을 통한 서비스 중단 최소화 설계)

[^OCPP]: **OCPP (Open Charge Point Protocol):** 개방형 충전 통신 규격

[^OLTP]: **OLTP (Online Transaction Processing):** 실시간 트랜잭션 처리
