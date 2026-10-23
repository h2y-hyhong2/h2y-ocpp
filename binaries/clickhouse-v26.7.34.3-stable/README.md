# ClickHouse v26.7.34.3-stable 바이너리 설치 및 실행 가이드

본 디렉토리는 CSMS Lite의 시계열 미터값(MeterValues) 및 대용량 비정형 원본 로그(Raw Log) 고속 분석을 위한 **ClickHouse 공식 단일 정적 바이너리(v26.7.34.3-stable amd64)**를 포함하고 있습니다.

외부 glibc나 시스템 종속 라이브러리가 필요 없는 **완전 정적 빌드(common-static)** 파일로, **WSL2(Ubuntu 24.04 LTS 포함) 및 리눅스 x86_64 환경**에서 즉시 단독 실행할 수 있습니다.

---

## 📌 1. 파일 구성

- **`clickhouse`** (769.5 MB): 공식 단일 실행 바이너리 (서버, 클라이언트, 로컬 등 모든 ClickHouse 도구 통합)
- **`clickhouse-common-static-26.7.34.3-amd64.tgz`** (225.8 MB): 공식 원본 릴리즈 아카이브
- **`README.md`**: 설치 및 실행 가이드 (본 문서)

---

## 🚀 2. 빠른 실행 단계 (Quick Start)

Windows 호스트의 PowerShell 또는 터미널에서 WSL2 Ubuntu 환경으로 진입하여 실행합니다.

### Step 1. WSL2 터미널 실행 및 바이너리 경로 이동

Windows 터미널에서 `wsl`을 실행한 후, 본 프로젝트의 바이너리 디렉토리로 이동합니다.

```bash
# WSL2 Ubuntu 셸 진입
wsl -d Ubuntu-24.04

# 프로젝트 바이너리 디렉토리로 이동 (경로는 본인 프로젝트 위치에 맞게 조정)
cd /mnt/d/project/lselink/ocpp-lite/git/h2y-ocpp/binaries/clickhouse-v26.7.34.3-stable
```

### Step 2. 바이너리 실행 권한 부여

```bash
chmod +x clickhouse
```

버전 확인:
```bash
./clickhouse --version
# 출력 예시: ClickHouse client/server version 26.7.34.3 (official build).
```

### Step 3. ClickHouse Server 기동 (Windows 호스트 바인딩)

Windows 호스트의 Spring Boot(`elvis-data-clickhouse`, 포트 9083) 또는 호스트 브라우저에서 접근하려면 반드시 `--listen_host=0.0.0.0` 인자를 전달해야 합니다.

```bash
# 포어그라운드(Foreground) 실행 (로그 실시간 확인)
./clickhouse server -- --listen_host=0.0.0.0
```

> **백그라운드(Daemon) 실행을 원하는 경우:**
> ```bash
> nohup ./clickhouse server -- --listen_host=0.0.0.0 > clickhouse.log 2>&1 &
> ```

기동 성공 시 기본 청취 포트:
- **HTTP 포트:** `8123` (Spring Boot JDBC 및 REST API 통신용)
- **Native TCP 포트:** `9000` (ClickHouse 네이티브 클라이언트 CLI용)

---

## 🗄️ 3. 초기 DDL 스키마 적용 (CSMS Lite PoC)

별도의 터미널 창을 열고, 내장된 `clickhouse client`를 통해 CSMS Lite 시계열 미터값 테이블을 생성합니다.

### 방법 A: 단일 스크립트 파일 일괄 적용 (권장)

```bash
# DDL 파일 경로 파이프라인 적용
./clickhouse client --multiquery < ../../elvis-data-clickhouse/src/main/resources/schema-clickhouse.sql
```

### 방법 B: CLI 대화형 셸에서 직접 적용

```bash
./clickhouse client
```

대화형 셸 진입 후 아래 SQL을 실행합니다:

```sql
-- 1. 전용 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS elvis_poc;

-- 2. 시계열 미터값 MergeTree 테이블 생성
CREATE TABLE IF NOT EXISTS elvis_poc.meter_value_history
(
    event_time      DateTime64(3, 'Asia/Seoul') COMMENT '계측 발생 일시',
    charge_box_id   LowCardinality(String)      COMMENT '충전기 식별자 (8자리 정규화)',
    connector_id    UInt8                       COMMENT '커넥터 번호 (1 또는 2)',
    transaction_id  Nullable(UInt32)            COMMENT '트랜잭션 ID (세션)',
    voltage         Float32                     COMMENT '전압 (V)',
    current_import  Float32                     COMMENT '전류 (A)',
    power_active    Float32                     COMMENT '유효 출력 (kW)',
    energy_active   UInt64                      COMMENT '누적 유효 전력량 (Wh)',
    soc             UInt8                       COMMENT '배터리 충전 상태 (0~100 %)',
    raw_json        String                      COMMENT '원본 JSON 페이로드'
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(event_time)
ORDER BY (charge_box_id, connector_id, event_time)
TTL toDateTime(event_time) + INTERVAL 180 DAY
SETTINGS index_granularity = 8192;

-- 3. 테이블 생성 확인
SHOW TABLES FROM elvis_poc;
```

---

## 🔌 4. Windows 호스트 연동 검증

Windows PowerShell에서 ClickHouse HTTP API 헬스체크를 수행합니다.

```powershell
# 1. 핑 테스트 (정상 시 "Ok." 반환)
curl.exe http://localhost:8123/ping

# 2. 버전 쿼리 실행
curl.exe -s "http://localhost:8123/?query=SELECT%20version()"

# 3. 적재 테이블 건수 확인
curl.exe -s "http://localhost:8123/?query=SELECT%20count()%20FROM%20elvis_poc.meter_value_history"
```

---

## 📦 5. Spring Boot 모듈 (`elvis-data-clickhouse`) 연동

ClickHouse 서버가 정상 기동되면 백엔드 모듈을 실행합니다.

```powershell
# Windows 프로젝트 루트에서 실행
./gradlew :elvis-data-clickhouse:bootRun
```

- **모듈 포트:** `9083`
- **JDBC 연결 URL:** `jdbc:ch://localhost:8123/elvis_poc`
- **Kafka 입력 토픽:** `meter-values.raw` (Kafka 19092 포트 기동 필요)

---

## 🛑 6. 서버 종료 및 프로세스 관리

```bash
# 백그라운드 프로세스 확인
ps aux | grep clickhouse

# 정상 종료 (SIGTERM)
kill $(pgrep -f "clickhouse server")
```
