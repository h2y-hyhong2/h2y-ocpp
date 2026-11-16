# ELVIS-CSMS 관제 데스크톱 센터 (`elvis-control-ui`)

**Tauri 2.0 + Vue 3 + Pure Tailwind CSS + ECharts** 기반의 **단독 실행형(Standalone Desktop App)** 관제 센터 구현체입니다.

별도의 무거운 외부 UI 라이브러리 의존성 없이 순수 Tailwind CSS 기반 초경량 Zero-Dependency CSMS 관제 솔루션을 제공합니다.

---

## 주요 특징

- **초경량 데스크톱 네이티브 앱**: WebView2 + Rust 기반으로 메모리 사용량 최소화 및 초고속 기동 속도 제공
- **고성능 대시보드 시각화**:
  - `Pure Tailwind & Pinia`: 충전기 실시간 상태 그리드, 계측치 모니터링 및 원격 제어(시작/종료/리셋/잠금해제)
  - `Billing & CDR`: MySQL OLTP 과금 원장 조회, 기간 필터링 및 CSV 내보내기
- **라이트 테마 기본(Light Theme Default)**: 장시간 관제 모니터링에 적합한 깔끔한 밝은 뉴트럴 톤(`#f8fafc`) 기본 UI 및 멀티 테마(`light`, `cyber`, `emerald`, `purple`, `amber`) 전환 지원
- **로컬 오프라인 폰트 내장**: `Pretendard` 및 `Inconsolata` 로컬 폰트 탑재로 폐쇄망 환경에서도 외부 CDN 없이 동작

---

## 기본 관리자 계정 안내 (Initial Seed Accounts)

관제 센터 로그인 시 백엔드(`elvis-control-api`)에 사전 등록된 아래 기본 시드 계정을 사용합니다.

| 권한 역할 | **아이디 (Username)** | **비밀번호 (Password)** | 권한 등급 | 주요 기능 및 권한 |
| :--- | :---: | :---: | :--- | :--- |
| **시스템 최고관리자** | `admin` | `admin123!` | `ROLE_ADMIN` | 충전기 원격제어(리셋/언락/중지/시작) 및 전체 관리 권한 (기본 권장) |
| **충전 관제 운영자** | `operator` | `operator123!` | `ROLE_OPERATOR` | 충전기 원격제어 및 실시간 관제 권한 |
| **관제 대시보드 조회자** | `viewer` | `viewer123!` | `ROLE_VIEWER` | 대시보드 및 통계 단순 모니터링 조회 전용 권한 |

---

## 포트 및 서비스 연계 구성

| 서비스 | 기본 포트 | 비고 |
| :--- | :---: | :--- |
| **관제 UI 개발 서버 (Vite)** | `1421` | `http://127.0.0.1:1421` |
| **OCPP WebSocket 게이트웨이** | `9080` | `/ws` 프록시 대상 (`elvis-connect-ws`) |
| **Parser & Sync 엔진** | `9081` | Kafka/MySQL 동기화 (`elvis-connect-sync`) |
| **Admin REST API 서버** | `9082` | `/api`, `/ws-push` 프록시 대상 (`elvis-control-api`) |
| **MySQL 9.71** | `13306` | `ELVIS_MYSQL_PORT` |
| **Apache Kafka KRaft** | `19092` | `ELVIS_KAFKA_PORT` |

---

## 빠른 시작 (Quick Start)

### 1. 프론트엔드 의존성 설치
```powershell
cd elvis-control-ui
pnpm install
```

### 2. 웹 브라우저 개발 모드 실행
Rust 컴파일 없이 브라우저(`http://127.0.0.1:1421`)에서 관제 UI를 즉시 확인합니다.
```powershell
pnpm dev
```

### 3. 데스크톱 앱 개발 모드 실행
Windows 네이티브 앱 창을 띄워 실시간 핫리로드 개발을 진행합니다.
```powershell
pnpm tauri:dev
```

### 4. 앱 아이콘 일괄 생성 (SVG 기반)
```powershell
pnpm tauri icon ./public/ELVIS-LITE-ICON.svg
```

### 5. 독립 실행 배포 파일 빌드 (.exe)
```powershell
pnpm tauri:build
# -> src-tauri/target/release/ELVIS-CSMS-ControlCenter.exe 생성
```

---

## 상세 개발 및 트러블슈팅 가이드

- **[개발 및 빌드 상세 가이드 (DEVELOPMENT.md)](./DEVELOPMENT.md)**: 사전 요구사항, Tauri/Cargo 아키텍처, 빌드/테스트 절차
- **[오류 해결 및 트러블슈팅 가이드 (TROUBLESHOOTING.md)](./TROUBLESHOOTING.md)**: Tauri/Rust/Node.js/Vite 오류 사례별 해결법
