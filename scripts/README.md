# 🛠️ ELVIS 로컬 서비스 및 테스트 프로그램 기동 가이드 (Windows Native / Zero-Docker)

Docker 없이 `binaries/` 폴더에 위치한 포터블 바이너리와 Gradle 백엔드를 직접 실행하여 로컬 개발 환경을 구동하는 가이드입니다.

---

## 📂 1. 인프라 미들웨어 & 코어 서비스 스크립트

| 스크립트 파일 | 설명 | 포트 / 프로토콜 |
| :--- | :--- | :--- |
| **`start-elvis-launcher.bat`** | ELVIS 5대 코어 통합 런처 (데스크톱 GUI / 웹 모드) | 1422 (Tauri/Vite) |
| **`start-all-middleware.bat`** | Kafka와 MySQL을 각각 별도 콘솔 창으로 일괄 기동 | Kafka(19092), MySQL(13306) |
| **`start-kafka-kraft.bat`** | Apache Kafka KRaft 단독 모드 기동 (최초 1회 자동 포맷) | 19092 (TCP) |
| **`create-kafka-topics.bat`** | 4대 핵심 토픽(`ocpp-raw-events`, `ocpp-outbound-commands` 등) 자동 생성 | - |
| **`start-mysql.bat`** | MySQL 9.71 데이터베이스 서버 콘솔 기동 | 13306 (TCP) |
| **`load-mysql-schema.bat`** | `elvis-lite` DDL 스키마 및 마스터/시드 데이터 적재 | 13306 (TCP) |
| **`start-elvis-connect-ws.bat`** | OCPP 1.6 WebSocket 게이트웨이 기동 | 18080 (HTTP/WS) |
| **`start-elvis-connect-sync.bat`** | Kafka 이벤트 수신 및 상태 동기화 엔진 기동 | 8082 (Actuator/HTTP) |
| **`start-elvis-control-api.bat`** | 관제 어드민 REST API 서버 기동 | 18088 (HTTP) |

---

## 🧪 2. 독립 테스트 프로그램 스크립트 (완전 분리 운영)

> 💡 **안내**: 테스트 프로그램은 코어 CSMS 런처와 아예 별도로 독립 동작하도록 분리되었습니다.

| 스크립트 파일 | 설명 | 포트 / 프로토콜 |
| :--- | :--- | :--- |
| **`start-all-tester.bat`** (`start-tester.bat`) | 테스터 백엔드(:8085) 및 테스터 UI(:1421) 일괄 독립 기동 | 8085, 1421 |
| **`stop-all-tester.bat`** (`stop-tester.bat`) | 테스터 백엔드 및 UI 프로세스/포트 안전 정리 | 8085, 1421 |
| **`start-elvis-tester.bat`** | 가상 충전기 부하 시뮬레이터 백엔드 단독 기동 | 8085 (HTTP) |
| **`start-elvis-tester-ui.bat`** | 테스터 웹 프론트엔드 콘솔 단독 기동 | 1421 (Vite) |
| **`start-elvis-tester-ui-tauri.bat`** | 테스터 네이티브 데스크톱 앱 단독 기동 | 네이티브 창 (:1421) |

---

## 🚀 빠른 시작 (Quick Start)

1. **CSMS 런처 기동**:
   ```cmd
   scripts\start-elvis-launcher.bat
   ```
2. **테스트 프로그램 독립 기동 (원클릭)**:
   ```cmd
   start-tester.bat
   ```
   - 백엔드(`http://localhost:8085`) 및 UI(`http://localhost:1421`)가 자동으로 브라우저에 표시됩니다.
3. **테스트 프로그램 독립 종료**:
   ```cmd
   stop-tester.bat
   ```
