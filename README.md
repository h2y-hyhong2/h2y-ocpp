# CPO CSMS Lite (Integrated CSMS Platform)

전기차 충전 인프라의 효율적 관리를 위한 CPO(Charge Point Operator) 전용 고성능 통합 관리 시스템(CSMS) 프로젝트 저장소입니다.

## 📌 문서 인덱스 (Documentation Index)

프로젝트와 관련된 주요 설계 및 요구사항 정의 문서 목록입니다. 각 문서는 Markdown 원본 또는 웹 브라우저에서 편리하게 볼 수 있는 **HTML 뷰어** 링크를 제공합니다.

| 문서명 | 주요 내용 | 문서 링크 (Markdown) | 브라우저 뷰어 (HTML) |
| :--- | :--- | :---: | :---: |
| **01. 제품 요구사항 정의서 (PRD)** | 프로젝트 개요, 시스템 아키텍처 후보군(1안/2안) 비교, 실시간 관제/정산 요건, 하드웨어 권장 스펙 | [01.prd.md](file:///d:/project/lselink/ocpp-lite/git/h2y-ocpp/doc/01.prd.md) | [HTML 뷰어](file:///d:/project/lselink/ocpp-lite/git/h2y-ocpp/doc/01.prd.html) |
| **02. 통합 기능 분류 정의서** | AS-IS 150여 개 화면 분석을 기반으로 Phase 1(필수) 및 Phase 2(선택) 분류 매트릭스 정의 | [02.feature_specification.md](file:///d:/project/lselink/ocpp-lite/git/h2y-ocpp/doc/02.feature_specification.md) | [HTML 뷰어](file:///d:/project/lselink/ocpp-lite/git/h2y-ocpp/doc/02.feature_specification.html) |
| **03. 예상 개발 일정 정의서** | Phase 1 (Standalone 6개월) 및 Phase 2 (HA 분산 6개월) 주차별 세부 작업 및 마일스톤 | [03.timeline.md](file:///d:/project/lselink/ocpp-lite/git/h2y-ocpp/doc/03.timeline.md) | [HTML 뷰어](file:///d:/project/lselink/ocpp-lite/git/h2y-ocpp/doc/03.timeline.html) |
| **04. 소프트웨어 아키텍처 정의서** | EIP 패턴 설계, 가상 스레드 분배, 분산 세션 관리, CQRS 데이터 구조 설계 및 EAI 통합 아키텍처 매핑 | [04.software_architecture.md](file:///d:/project/lselink/ocpp-lite/git/h2y-ocpp/doc/04.software_architecture.md) | [HTML 뷰어](file:///d:/project/lselink/ocpp-lite/git/h2y-ocpp/doc/04.software_architecture.html) |
| **05. Phase 1 기술 검증 정의서 (POC)** | Phase 1 기준 인프라 구성, EIP 구성 코드, 벌크 저장 설정 및 시뮬레이터 검증 시나리오 정의 | [05.poc.md](file:///d:/project/lselink/ocpp-lite/git/h2y-ocpp/doc/05.poc.md) | [HTML 뷰어](file:///d:/project/lselink/ocpp-lite/git/h2y-ocpp/doc/05.poc.html) |
| **AS-IS 메뉴 목록** | 이전 시스템의 원본 화면 목록 (CSV 포맷) | [as-is-functions](file:///d:/project/lselink/ocpp-lite/git/h2y-ocpp/doc/as-is-functions) | - |

---

## 🛠️ 핵심 기술 스택 (Core Tech Stack)

* **Backend Core:** Java 25 (Virtual Threads), Spring Boot 3.x/4.x
* **Enterprise Integration:** Spring Integration 6.x (제 1안 권장안 적용 기준)
* **Message Broker:** Apache Kafka (KRaft Mode)
* **Databases:** PostgreSQL (OLTP, 정산/자산 마스터) & ClickHouse (OLAP, 미터값/원본로그 시계열)
* **In-Memory Cache:** Redis (분산 세션 및 명령 라우팅 Pub/Sub)
* **Frontend:** Vue 3 (Composition API, Pinia, Leaflet.js 오프라인 지도)

---

## 🚀 프로젝트 로드맵

1. **Phase 1 (Standalone 단독 배포 모드 - 6개월)**
   * 단일 서버 기동 환경 최적화
   * OCPP 1.6J 필수 메시지 수집 및 ClickHouse 벌크 적재
   * 기본 정산 원장 계산 및 대시보드 관제 구축
   * 완전 폐쇄망(Air-Gapped) 배포 패키지 구성 및 Zero-Dependency 실행 보장

2. **Phase 2 (HA 분산 확장 클러스터 모드 - 6개월)**
   * L4/L7 로드밸런싱 및 다중 노드 기동
   * Redis 세션 맵 동기화 및 Pub/Sub 명령 라우팅 구축
   * DB/Kafka 분산 다중화 (Active-Standby, Replica 구성)
   * OCPP 2.0.1 확장, 스마트 차징 스케줄러, 전기 버스 차고지 관제 및 대외 로밍 연동