# 🛠️ ELVIS 로컬 미들웨어 기동 가이드 (Windows Native / Zero-Docker)

Docker 없이 `binaries/` 폴더에 위치한 포터블 바이너리를 직접 실행하여 로컬 개발 환경을 구동하는 가이드입니다.

---

## 📂 스크립트 목록

| 스크립트 파일 | 설명 | 포트 / 프로토콜 |
| :--- | :--- | :--- |
| **`start-all-middleware.bat`** | Kafka와 MySQL을 각각 별도 콘솔 창으로 일괄 기동 | Kafka(9092), MySQL(3306) |
| **`start-kafka-kraft.bat`** | Apache Kafka 4.3.1 KRaft 단독 모드 기동 (최초 1회 자동 포맷) | 9092 (TCP) |
| **`create-kafka-topics.bat`** | 4대 핵심 토픽(`ocpp-raw-events`, `ocpp-outbound-commands` 등) 자동 생성 | - |
| **`init-mysql.bat`** | MySQL 9.71 데이터 디렉토리(`data/`) 최초 초기화 (`root` 무암호) | - |
| **`start-mysql.bat`** | MySQL 9.71 데이터베이스 서버 콘솔 기동 | 3306 (TCP) |

---

## 🚀 빠른 시작 (Quick Start)

1. **미들웨어 전체 기동**:
   ```cmd
   scripts\start-all-middleware.bat
   ```
2. **Kafka 토픽 생성** (Kafka 기동 후 1회 실행):
   ```cmd
   scripts\create-kafka-topics.bat
   ```
3. **스프링 부트 애플리케이션 실행**:
   - `elvis-connect-ws` (8080 포트) 실행 시 자동으로 `localhost:9092` Kafka로 연결됩니다.
