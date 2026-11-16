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
| **외부 설정 및 인스톨러 가이드**       | `config/` 외부 설정 오버라이드 및 미들웨어 실행 가이드                                                              | [config/README.md](config/README.md)                          |
| **로컬 기동 스크립트 가이드**          | Windows Native(Zero-Docker) 미들웨어/서비스/테스터 기동 스크립트 안내                                               | [scripts/README.md](scripts/README.md)                        |

---

## 🛠️ 핵심 기술 스택 (Core Tech Stack)

- **Backend Core:** Java 25 (Temurin OpenJDK, Virtual Threads, No-Lombok), Spring Boot 4.1.2, Gradle 9.8.2
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

### 주요 마이크로서비스 및 UI 모듈

- **`elvis-connect-ws`**: OCPP 1.6J WebSocket 게이트웨이 수신 및 하향 커맨드 전달 (`:9080`)
- **`elvis-connect-sync`**: Kafka 이벤트 파싱, MySQL 실시간 상태/세션/과금/장애 동기화 엔진 (`:9081`)
- **`elvis-control-api`**: 관제 REST API, SSE 실시간 알림 푸시 및 Spring Security JWT 관리자 인증 백엔드 (`:9082`)
- **`elvis-launcher`**: 로컬 미들웨어 및 코어 서비스 통합 런처 데스크톱 앱 (Tauri 2.0 / Vue 3, `:1420`)
- **`elvis-control-ui`**: ELVIS-CSMS 관제 데스크톱 센터 (Tauri 2.0 / Vue 3, `:1421`)
- **`elvis-tester-ui`**: 가상 충전기(EVSE) 에뮬레이터 및 WireTap 패킷 시뮬레이터 UI (Tauri 2.0 / Vue 3, `:1422`)

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
