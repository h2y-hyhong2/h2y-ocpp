# CPO[^CPO] CSMS[^CSMS] Lite (Integrated CSMS Platform)

전기차 충전 인프라의 효율적 관리를 위한 CPO(Charge Point Operator) 전용 고성능 통합 관리 시스템(CSMS) 프로젝트 저장소입니다.

## 📌 문서 인덱스 (Documentation Index)

프로젝트와 관련된 주요 설계 문서는 로컬/사내 문서 저장소(`doc/01.standard_solution/`, `doc/as-is-functions`) 및 리포지토리 내 `docs/` 디렉터리에서 관리됩니다.

| 문서명                                 | 주요 내용                                                                                                           | 저장 경로 (참고)                                              |
| :------------------------------------- | :------------------------------------------------------------------------------------------------------------------ | :------------------------------------------------------------ |
| **01. 제품 요구사항 정의서 (PRD)**     | 프로젝트 개요, 시스템 아키텍처 후보군(1안/2안) 비교, 실시간 관제/정산 요건, 하드웨어 권장 스펙                      | `doc/01.standard_solution/01.prd.md`                          |
| **02. 통합 기능 분류 정의서**          | AS-IS 150여 개 화면 분석을 기반으로 Phase 1(필수) 및 Phase 2(선택) 분류 매트릭스 정의                               | `doc/01.standard_solution/02.feature_specification.md`        |
| **03. 예상 개발 일정 정의서**          | Phase 1 (Standalone 6개월) 및 Phase 2 (HA[^HA] 분산 6개월) 주차별 세부 작업 및 마일스톤                             | `doc/01.standard_solution/03.timeline.md`                     |
| **04. 소프트웨어 아키텍처 정의서**     | EIP[^EIP] 패턴 설계, 가상 스레드 분배, 분산 세션 관리, CQRS[^CQRS] 데이터 구조 설계 및 EAI[^EAI] 통합 아키텍처 매핑 | `doc/01.standard_solution/04.software_architecture.md`        |
| **05. Phase 1 기술 검증 정의서 (POC)** | Phase 1 기준 인프라 구성, EIP 구성 코드, 벌크 저장 설정 및 시뮬레이터 검증 시나리오 정의                            | `doc/01.standard_solution/05.poc.md`                          |
| **CI/BI 마스터 컬러 가이드**           | E-LINK / LS 브랜드 컬러 시스템, 라이트 테마 기준 60-30-10 컬러 위계 및 상태 색상 토큰                               | [docs/CI_COLOR_GUIDE.md](docs/CI_COLOR_GUIDE.md)              |
| **OCPP 1.6J 프로토콜 규약서**          | LSELINK 표준 OCPP 1.6J 전문 규격(Core, 확장 `DataTransfer`, 결제, 원격제어, Security) 및 CSMS Lite 매핑             | [docs/OCPP_1.6J_PROTOCOL_SPEC.md](docs/OCPP_1.6J_PROTOCOL_SPEC.md) |
| **OCPP 1.6J 엔진 점검 체크리스트**     | 보안 제외 전 전문 대상 엔진 구현 현황 점검 결과 및 Phase 1~3 보완 작업 체크리스트                                   | [docs/OCPP_1.6J_ENGINE_CHECKLIST.md](docs/OCPP_1.6J_ENGINE_CHECKLIST.md) |
| **외부 설정 및 인스톨러 가이드**       | `config/` 외부 설정 오버라이드 및 미들웨어 실행 가이드                                                              | [config/README.md](config/README.md)                          |
| **로컬 기동 스크립트 가이드**          | Windows Native(Zero-Docker) 미들웨어/서비스/테스터 기동 스크립트 안내                                               | [scripts/README.md](scripts/README.md)                        |

---

## 🛠️ 핵심 기술 스택 (Core Tech Stack)

- **Backend Core:** Java 25 (Temurin OpenJDK, Virtual Threads, No-Lombok), Spring Boot 4.1.2, Gradle 9.9.1
- **Enterprise Integration:** Spring-Kafka (제 2안 채택 적용 기준)
- **Message Broker:** Apache Kafka (KRaft Mode)
- **Databases:** MySQL 9.71 (OLTP[^OLTP], 정산/자산 마스터)
- **In-Memory Cache:** Caffeine (로컬 L1) & Redis (선택적 L2 연동 / Phase 2 HA 분산 세션 및 명령 라우팅 Pub/Sub 확장)
- **Frontend / Desktop App:** Vue 3 (Composition API[^API], Pinia, Pure Tailwind CSS, ECharts) & **Tauri 2.0 (Rust 기반 초경량 Standalone 데스크톱 앱, pnpm)**

---

## 💻 로컬 개발 환경 및 미들웨어 바이너리 (Local Middleware Binaries)

CSMS Lite 프로젝트는 폐쇄망 및 로컬 독립 개발 환경을 지원하기 위해 `binaries/` 디렉토리에 주요 미들웨어 바이너리를 내장/관리합니다.

| 미들웨어         | 버전                        | 실행 환경               | 전용 포트                   | 설치 및 실행 상세 가이드                                                  |
| :--------------- | :-------------------------- | :---------------------- | :-------------------------- | :------------------------------------------------------------------------ |
| **Apache Kafka** | `2.13-4.3.1` (KRaft)        | Windows / JVM           | `19092`                     | `binaries/kafka_2.13-4.3.1/`                                              |
| **MySQL**        | `9.7.1-winx64`              | Windows x64             | `13306`                     | `binaries/mysql-9.7.1-winx64/`                                            |
| **Gradle**       | `9.9.1`                     | Windows / JVM           | -                           | `D:\project\lselink\ocpp-lite\gradle\gradle-9.9.1/`                       |

### 주요 마이크로서비스 및 UI 모듈

- **`elvis-connect-ws`**: OCPP 1.6J WebSocket 게이트웨이 수신 및 하향 커맨드 전달 (`:9080`)
- **`elvis-connect-sync`**: Kafka 이벤트 파싱, MySQL 실시간 상태/세션/과금/장애 동기화 엔진 (`:9081`)
- **`elvis-control-api`**: 관제 REST API, SSE 실시간 알림 푸시 및 Spring Security JWT 관리자 인증 백엔드 (`:9082`)
- **`elvis-launcher`**: 로컬 미들웨어 및 코어 서비스 통합 런처 데스크톱 앱 (Tauri 2.0 / Vue 3, `:1420`)
- **`elvis-log-viewer`**: 순수 Rust 네이티브 GUI(`eframe`/`egui`) 5분할 실시간 로그 뷰어 및 포트·프로세스 실시간 진단 도구
- **`elvis-control-ui`**: ELVIS-CSMS 관제 데스크톱 센터 (Tauri 2.0 / Vue 3, `:1421`)
- **`elvis-tester-ui`**: 가상 충전기(EVSE) 에뮬레이터 및 WireTap 패킷 시뮬레이터 UI (Tauri 2.0 / Vue 3, `:1422`)

### 모듈별 변경 로그 · 오류 해결 이력

모듈별 담당자 분리 운영을 위해 각 모듈 루트에서 `CHANGELOG.md`(변경 이력)와 `TROUBLESHOOTING.md`(오류 해결 이력)를 독립 관리합니다.

| 모듈 / 영역 | 변경 로그 이력 | 오류 해결 이력 |
| :--- | :--- | :--- |
| **`elvis-connect-ws`** | [elvis-connect-ws/CHANGELOG.md](elvis-connect-ws/CHANGELOG.md) | [elvis-connect-ws/TROUBLESHOOTING.md](elvis-connect-ws/TROUBLESHOOTING.md) |
| **`elvis-connect-sync`** | [elvis-connect-sync/CHANGELOG.md](elvis-connect-sync/CHANGELOG.md) | [elvis-connect-sync/TROUBLESHOOTING.md](elvis-connect-sync/TROUBLESHOOTING.md) |
| **`elvis-control-api`** | [elvis-control-api/CHANGELOG.md](elvis-control-api/CHANGELOG.md) | [elvis-control-api/TROUBLESHOOTING.md](elvis-control-api/TROUBLESHOOTING.md) |
| **`elvis-control-ui`** | [elvis-control-ui/CHANGELOG.md](elvis-control-ui/CHANGELOG.md) | [elvis-control-ui/TROUBLESHOOTING.md](elvis-control-ui/TROUBLESHOOTING.md) |
| **`elvis-launcher`** | [elvis-launcher/CHANGELOG.md](elvis-launcher/CHANGELOG.md) | [elvis-launcher/TROUBLESHOOTING.md](elvis-launcher/TROUBLESHOOTING.md) |
| **`elvis-log-viewer`** | [elvis-log-viewer/CHANGELOG.md](elvis-log-viewer/CHANGELOG.md) | [elvis-log-viewer/TROUBLESHOOTING.md](elvis-log-viewer/TROUBLESHOOTING.md) |
| **`elvis-tester-ui`** | [elvis-tester-ui/CHANGELOG.md](elvis-tester-ui/CHANGELOG.md) | [elvis-tester-ui/TROUBLESHOOTING.md](elvis-tester-ui/TROUBLESHOOTING.md) |
| **`scripts` (인프라·기동)** | [scripts/CHANGELOG.md](scripts/CHANGELOG.md) | [scripts/TROUBLESHOOTING.md](scripts/TROUBLESHOOTING.md) |

---

## ⚙️ 개발 환경 설정 (Development Environment Setup)

### 1. 필수 런타임 및 경로 설정
- **JDK 25**: `D:\project\lselink\ocpp-lite\jdk\jdk-25.0.4.1+1` (`gradle.properties` 및 기동 스크립트 참조)
- **Gradle 9.9.1**: `D:\project\lselink\ocpp-lite\gradle\gradle-9.9.1` ([gradlew.bat](gradlew.bat) 및 백엔드 기동 스크립트 1순위 사용)
- **Gradle 캐시 홈 (`GRADLE_USER_HOME`)**: `D:\project\lselink\ocpp-lite\gradle\.gradle` (기동 스크립트 및 [.vscode/settings.json](.vscode/settings.json) 연동)
- **Frontend / Desktop**: Node.js `v24.x` (`fnm`), `pnpm`, Rust (`cargo`) — UI 최초 기동 전 각 UI 폴더(`elvis-launcher`, `elvis-control-ui`, `elvis-tester-ui`)에서 `pnpm install` 필요

### 2. 로컬 스토리지 · 포트 · MySQL 접속 정보
- **경로/포트 설정**: [config/paths.env](config/paths.env) (`ELVIS_DATA_DIR=%ELVIS_HOME%\data`, `ELVIS_LOG_DIR=%ELVIS_HOME%\logs`)
  - **개발 환경(Dev/Debug)**: 워크스페이스 루트(`%ELVIS_HOME%`) 기준 `data/`·`logs/` 및 Gradle `bootRun` 사용 (`elvis-launcher` 설정 `settings-dev.json` 별도 관리)
  - **Release 배포(`D:\elvis-lite\`)**: [scripts/deploy-elvis-lite.bat](scripts/deploy-elvis-lite.bat)로 표준 제품 구조(`apps/`, `binaries/`, `config/`, `scripts/`, `data/`, `logs/`, 4대 `.exe`)를 구성하며 Release 빌드 실행 파일은 `D:\elvis-lite` 제품 경로를 기본 참조합니다.
- **데스크톱 4대 앱 릴리스 실행파일 (`D:\elvis-lite\`)**: [scripts/build-desktop-exes.bat](scripts/build-desktop-exes.bat)로 4대 앱(`elvis-launcher.exe`, `elvis-control-ui.exe`, `elvis-log-viewer.exe`, `elvis-tester-ui.exe`)을 공식 제품 아이콘(`icons/`) 적용 상태로 일괄 빌드·배포합니다.
- **MySQL 9.71 (`127.0.0.1:13306`, DB: `elvis_lite`)**:
  - `root`: 비밀번호 없음 (`--initialize-insecure` 초기화)
  - `elvis` / `elvis1234!`: 애플리케이션 전용 계정 (`elvis_lite.*` 전체 권한)
  - 초기 테스트 RFID 카드 4종 시드 포함 (`CARD-CORP-9901`, `CARD-USER-1001`, `CREDIT-IC-01`, `ROAM-ME-7701`)

---

## 📊 제품 구성 및 작업 현황 (2026-11-25 기준)

| 영역 | 구성 모듈 / 산출물 | 진행 상태 | 주요 구현 내용 |
| :--- | :--- | :---: | :--- |
| **로컬 배포 스크립트** | `D:\elvis-lite\`, [scripts/deploy-elvis-lite.ps1](scripts/deploy-elvis-lite.ps1) | 완료 | Standalone 제품 디렉터리 표준화(`apps/`, `binaries/`, `config/`, `scripts/`, `data/`, `logs/`), 무창 독립 데몬 기동(`run-daemon.ps1`/`vbs`), 개발/Release 경로 자동 분기 |
| **설치 인스톨러 패키징** | Windows Installer (`Setup.exe` / Inno Setup 등) | 미착수 | 최종 사용자용 단일 설치 마법사 패키징 및 자동 환경 구성 (추후 진행 예정) |
| **통합 런처** | `elvis-launcher` (`elvis-launcher.exe`) | 완료 | 제품 메인 경로 동적 변경·영속 저장, 5대 서비스 설치 여부(`미설치` 배지) 판별, 일괄·개별 제어 및 시스템 트레이 연동 |
| **로그·프로세스 뷰어** | `elvis-log-viewer` (`elvis-log-viewer.exe`) | 완료 | 5분할/3분할/2분할/단일/에러 분할 뷰, 하단 1줄 시스템 자원 바(`CPU`/`RAM`/`DISK`/`UPTIME`), OCPP 파이프 표준 로그 레벨 파싱 및 태그 강조 |
| **OCPP 게이트웨이·동기화** | `elvis-connect-ws` (`:9080`), `elvis-connect-sync` (`:9081`) | 완료 | OCPP 1.6J 송수신·Kafka 파이프라인·MySQL 실시간 상태/세션/계측/TOU 과금 동기화, `evcs_ocpp_ws`/`evcs_ocpp_rest` 표준 파이프 로그·MDC(`charger`, `msgId`)·에러/Kafka 파일 분리 |
| **관제 API·관제 UI** | `elvis-control-api` (`:9082`), `elvis-control-ui` (`elvis-control-ui.exe`) | 완료 | Tauri 2.0 CORS/WebSocket·JWT 인증 연동, `충전기 목록` + `과금 원장` 2화면 단순화·모던 라이트 디자인 통일, `transaction_session`/`charge_detail_record` 실시간 과금 원장 조회 및 CSV 내보내기 |
| **가상 충전기 테스터** | `elvis-tester-ui` (`elvis-tester-ui.exe`) | 완료 | 듀얼건 가상 충전기 시뮬레이션, JWT 자동 인증 기반 MySQL 충전기 동기화 및 OCPP E2E 검증 스위트 |

### 3. 최초 세팅 및 기동 순서 (클린 환경 권장)
1. [scripts/start-elvis-launcher.bat](scripts/start-elvis-launcher.bat)로 ELVIS 런처를 실행합니다.
2. **인프라(`MySQL`, `Kafka`)를 먼저 기동**합니다.
   - MySQL 최초 기동 시 `init-mysql.bat` → 서버 기동 → `load-mysql-schema.bat`(`01_schema.sql`, `02_seed_data.sql`)이 자동 실행됩니다.
   - Kafka 최초 기동 시 KRaft 스토리지 포맷 후 브로커가 기동됩니다.
3. 두 인프라가 `ONLINE`이 된 뒤 **코어 백엔드(`ws`, `sync`, `api`)**를 기동하고, 필요 시 [scripts/create-kafka-topics.bat](scripts/create-kafka-topics.bat)로 4대 토픽을 생성합니다.

### 4. Antigravity MySQL MCP 연동 (선택)
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

## 🔐 관제 센터 기본 관리자 계정 (Initial Seed Accounts)

관제 센터(`elvis-control-ui`) 로그인 및 API 호출 시 백엔드(`elvis-control-api`)에 사전 탑재된 아래 기본 시드 계정을 사용합니다.

| 권한 역할                | **아이디 (Username)** | **비밀번호 (Password)** | 권한 등급       | 주요 기능 및 제어 권한                                             |
| :----------------------- | :-------------------: | :---------------------: | :-------------- | :----------------------------------------------------------------- |
| **시스템 최고관리자**    |        `admin`        |       `admin123!`       | `ROLE_ADMIN`    | 충전기 원격제어(리셋/언락/중지/시작) 및 전체 관리 권한 (기본 권장) |
| **충전 관제 운영자**     |      `operator`       |     `operator123!`      | `ROLE_OPERATOR` | 충전기 원격제어 및 실시간 관제 권한                                |
| **관제 대시보드 조회자** |       `viewer`        |      `viewer123!`       | `ROLE_VIEWER`   | 대시보드 및 통계 단순 모니터링 조회 전용 권한                      |

---

## 🚀 프로젝트 로드맵

1. **Phase 1 (Standalone 단독 배포 모드 - 6개월)**
   - 단일 서버 기동 환경 최적화
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
