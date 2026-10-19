# elvis-data-clickhouse (고속 시계열 MeterValues 적재 엔진)

본 모듈은 CSMS Lite 시스템에서 충전기로부터 수신된 고빈도 시계열 계측 데이터(`MeterValues`)를 Apache Kafka(`meter-values.raw`)로부터 실시간 컨슘하여, OLAP 컬럼형 데이터베이스인 **ClickHouse(`meter_value_history`)**에 고속 벌크 적재(Bulk Insert)하는 마이크로서비스 엔진입니다.

---

## 🏗️ 1. 주요 사양 및 아키텍처

- **서비스 포트:** `18083`
- **런타임 환경:** Java 25 (Virtual Threads 기반 비차단 비동기 I/O)
- **프레임워크:** Spring Boot 3.x / 4.x
- **인프라 종속성:**
  - **ClickHouse:** HTTP `8123` (`jdbc:ch://localhost:8123/elvis_poc`)
  - **Kafka:** `19092` (토픽: `meter-values.raw`)
- **적재 정책:**
  - 배치 크기: 1,000건 또는 1,000ms 버퍼 타임아웃 도달 시 원자적 벌크 플러시(Flush)
  - 파티셔닝: 월별 파티션 (`toYYYYMM(event_time)`)
  - 보관 주기(TTL): 180일 자동 만료 (`TTL toDateTime(event_time) + INTERVAL 180 DAY`)

---

## 🚀 2. 사전 인프라 기동 가이드

본 모듈 실행 전, 로컬 ClickHouse 서버가 반드시 구동되어 있어야 합니다.

### ClickHouse 바이너리 기동

상세한 바이너리 실행 방법은 [`binaries/clickhouse-v26.7.34.3-stable/README.md`](../../binaries/clickhouse-v26.7.34.3-stable/README.md)를 참조하십시오.

```bash
# WSL2 Ubuntu 터미널에서 바이너리 실행
cd /mnt/d/project/lselink/ocpp-lite/git/h2y-ocpp/binaries/clickhouse-v26.7.34.3-stable
./clickhouse server -- --listen_host=0.0.0.0
```

### DDL 스키마 적용

```bash
# 초기 DDL 일괄 적용 (ClickHouse 클라이언트 이용)
./clickhouse client --multiquery < ../../elvis-data-clickhouse/src/main/resources/schema-clickhouse.sql
```

---

## 💻 3. 모듈 빌드 및 실행

### Gradle 실행 (Windows 호스트)

```powershell
./gradlew :elvis-data-clickhouse:bootRun
```

### 정상 기동 확인

- **애플리케이션 로그:**
  ```text
  [elvis-data-clickhouse] Started Application in ... seconds
  [ClickHouseBulkInsertEngine] Initialized ClickHouse connection pool (jdbc:ch://localhost:8123/elvis_poc)
  ```
- **ClickHouse 적재 현황 조회 (PowerShell):**
  ```powershell
  curl.exe -s "http://localhost:8123/?query=SELECT%20count()%20FROM%20elvis_poc.meter_value_history"
  ```
