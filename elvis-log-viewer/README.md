# ELVIS 네이티브 실시간 로그 · 프로세스 뷰어 (`elvis-log-viewer`)

**순수 Rust + `eframe` / `egui` (0.31)** 기반의 **초경량 단독 실행형(Standalone Native GUI)** 실시간 통합 로그 뷰어 및 포트·프로세스 진단 도구입니다.

WebView2나 터미널(`wt.exe`/PowerShell)의 가로 스크롤·창 분할 제약을 해소하기 위해 별도 독립 모듈로 분리되었으며, 단일 실행 파일(`elvis-log-viewer.exe`, 약 4.8 MB)로 빠르게 기동됩니다.

---

## 주요 기능

### 1. 다중 분할 레이아웃 & 4축 마우스 드래그 리사이즈
- 상단 툴바에서 창을 닫지 않고 모든 뷰 모드를 원클릭으로 전환할 수 있습니다.
  - **전체 5분할**: 3행 2열 격자 (`1: WS` / `3: SYNC` | `2+4: API`, 하단 `5: MySQL` | `6: Kafka`)
  - **코어 3분할**: `WS` / `SYNC` | `API`
  - **미들웨어 2분할**: `MySQL` | `Kafka`
  - **개별 단일 보기**: `WS` · `SYNC` · `API` · `MySQL` · `Kafka`
  - **프로세스 진단**: 포트 `LISTEN` 및 세부 프로세스(`mysqld`/`java`/`node`) 실시간 모니터링
- 모든 패널 경계선을 **마우스 드래그로 자유롭게 크기 조절**할 수 있으며, `비율 초기화` 버튼으로 기본 비율을 복원합니다.

```text
┌─────────────────────────┬─────────────────────────┐
│ 1. elvis-connect-ws     │                         │
├─────────────────────────┤ 2+4. elvis-control-api  │
│ 3. elvis-connect-sync   │                         │
├─────────────────────────┼─────────────────────────┤
│ 5. MySQL 9.71           │ 6. Apache Kafka         │
└─────────────────────────┴─────────────────────────┘
```

### 2. 네이티브 가로·세로 스크롤 & 줄바꿈 실시간 전환
- **기본 `줄바꿈 OFF (가로스크롤)`**: 긴 JSON/OCPP 전문이 잘리거나 강제 개행되지 않고 네이티브 가로·세로 스크롤바로 온전히 표시됩니다.
- **`줄바꿈 ON` 토글**: 상단 툴바에서 즉시 자동 줄바꿈 모드로 전환할 수 있습니다.
- **실시간 감시 (`300ms` 증분 폴링)**: 마지막 256 KB / 최대 600줄을 경량 로드하며, 상단 `● LIVE` / `❚❚ 정지` 토글 및 패널별 자동 스크롤 추적(`최신↓` / `고정`)을 지원합니다.

### 3. 로그 레벨 헤더 파싱 기반 색상 강조 · 필터링 · 좌·우 에러 분할
- Spring Boot(`---` 이전 헤더), Kafka(`[타임스탬프] LEVEL`), MySQL(`[ERROR]`/`[Warning]`/`[System]`) 로그 포맷의 **레벨 토큰을 우선 판별**합니다.
- 본문에 `"errorCode":"NoError"`나 `0 transport error`가 포함된 정상 `INFO` 로그가 빨간색으로 오탐되지 않으며, `에러만` 필터도 실제 `ERROR`/`WARN`/스택트레이스만 정확히 추려냅니다.
- **좌·우 `에러 분할` 토글**: 상단 `에러 분할` 버튼(또는 패널 헤더의 `에러 N` 배지)을 켜면 각 서비스 패널 내부가 **좌측 `전체 로그` | 우측 `에러 · 경고`**로 세로 양분되며, 패널별 중앙 경계선 마우스 드래그 리사이즈와 에러 로그 전용 `복사`를 지원합니다.
- **색상 체계 (라이트 테마 기본)**:
  - `ERROR` / `FATAL` / 예외 스택트레이스: Rose 강조
  - `WARN` / `Warning`: Amber 강조
  - `INFO` (`[WS-RECV]` / `[WS-SEND]` / `[SYNC-RECV]` / `[OCPP]`): Sky 강조
  - `DEBUG` / `TRACE`: Slate 보조 톤

### 4. 전 모드 공통 상단 시스템 자원 바 & 프로세스 실시간 진단
- **공통 상단 컴팩트 시스템 자원 바**: 상단 툴바 바로 아래에 초슬림 2행 바를 고정 배치해 5분할·3분할·2분할·단일 로그·프로세스 화면 어디서나 Windows 네이티브 API(`kernel32.dll`, 1초 주기)로 **CPU·RAM 개별 가로 막대**, **로컬 디스크(`C:`, `D:` 등) 사용률·여유 용량**, **OS 연속 가동시간(`UPTIME`)**을 실시간으로 확인합니다.
- **프로세스 탭 (`프로세스`)**: 백그라운드 스레드에서 2.5초 주기로 비동기 수집해 UI 프리징 없이 실시간 상태를 갱신합니다.
- **1. 서비스 포트 점유 현황 (`TCP LISTEN`)**: 런처(`:1420`), 미들웨어(`MySQL`, `Kafka`), 코어 엔진(`WS`, `SYNC`, `API`), 관제 UI(`:1421`), 테스터 UI(`:1422`)의 포트·상태(`RUNNING`/`STOPPED`)·PID·메모리(MB) 조회
- **2. 실행 중인 세부 PID 현황**: `mysqld.exe`, `java.exe`, `node.exe` 세부 프로세스 식별 및 메모리 합계 표시
- **제어 기능**:
  - 범위 전환 (`전체 (8)` / `미들웨어 (2)` / `코어 엔진 (3)`)
  - 행별 **`로그`** 바로가기(클릭 시 해당 서비스 단일 로그 뷰로 즉시 이동)
  - 오클릭 방지 2단계 **`종료` → `확인`/`취소`** (PID 강제 종료) 및 **특정 포트 번호 직접 종료**

### 5. 편의 기능 & 오프라인 폰트 지원
- 패널 개별 확대/복귀(`확대`/`복귀`, 단축키 `ESC`), 마우스 드래그 텍스트 선택 복사, 패널 전체 `복사`, 로그 파일 `비우기`
- 전역 검색(`전체 로그 검색...`) + 패널별 개별 필터 동시 적용, 폰트 크기 조절(`A-` / `A+`)
- 로컬 폰트 자동 탐색: `PretendardVariable.ttf` → `C:\Windows\Fonts\malgun.ttf`, 고정폭 `Inconsolata` → `C:\Windows\Fonts\consola.ttf`

---

## 빌드 및 실행 방법

### 1. 릴리스 빌드 (`D:\elvis-lite\elvis-log-viewer.exe` 자동 배포)
최초 1회(또는 소스 수정 후) 아래 스크립트로 릴리스 빌드를 실행하면 중간 산출물(`target/`)은 모듈 내부에 유지하고 최종 실행파일만 `D:\elvis-lite\elvis-log-viewer.exe`로 복사합니다.
```powershell
.\elvis-log-viewer\build-release.bat
# 또는 4개 데스크톱 앱 일괄 빌드:
.\scripts\build-desktop-exes.bat
```

### 2. `elvis-launcher` 자동 연동 (권장)
`D:\elvis-lite\elvis-log-viewer.exe`(또는 모듈 내부 `target/release/elvis-log-viewer.exe`)가 존재하면 `elvis-launcher` 상단의 **`로그`**, **`에러로그`**, **`프로세스`** 버튼 클릭 시 자동으로 `elvis-log-viewer.exe`가 실행됩니다.
- 바이너리가 아직 빌드되지 않은 경우에만 기존 Windows Terminal(`wt.exe`) / `check-status.bat` 콘솔로 자동 폴백됩니다.

### 3. 단독 실행
별도 인자 없이 실행하면 워크스페이스 루트의 [`config/paths.env`](../config/paths.env)를 자동 탐색해 `ELVIS_LOG_DIR`과 5대 서비스 포트를 읽어옵니다.
```powershell
# 전체 5분할 실시간 로그 (기본값)
D:\elvis-lite\elvis-log-viewer.exe

# 특정 서비스 단일 로그 보기 (줄바꿈 OFF)
D:\elvis-lite\elvis-log-viewer.exe --service ws --type all --wrap 0

# 전체 에러 로그만 보기
D:\elvis-lite\elvis-log-viewer.exe --group all --type error

# 프로세스 및 포트 실시간 진단 모드로 바로 열기
D:\elvis-lite\elvis-log-viewer.exe --mode process --target all
```

---

## CLI 옵션 레퍼런스

| 옵션 | 허용 값 | 기본값 | 설명 |
| :--- | :--- | :---: | :--- |
| `--log-dir <PATH>` | 디렉터리 경로 | `config/paths.env` (`D:\elvis-lite\logs`) | 감시 대상 로그 디렉터리 경로 |
| `--group <GROUP>` | `all` \| `core` \| `infra` \| `ws` \| `sync` \| `api` \| `mysql` \| `kafka` \| `process` | `all` | 초기 표시 레이아웃 또는 단일 서비스/프로세스 뷰 지정 |
| `--service <SVC>` | `ws` \| `sync` \| `api` \| `mysql` \| `kafka` | - | 단일 서비스 로그 보기 (`--group <SVC>`와 동일) |
| `--mode <MODE>` | `process` \| `proc` | - | 프로세스·포트 실시간 진단 모드로 바로 진입 |
| `--target <SCOPE>` | `all` \| `infra` \| `core` | `all` | 프로세스 진단 모드의 초기 필터 범위 |
| `--type <TYPE>` | `all` \| `split` \| `error` | `all` | `split` 지정 시 패널 좌·우 에러 분할 ON, `error` 지정 시 에러/경고 로그만 표시 |
| `--wrap <0\|1>` | `0` \| `1` \| `true` \| `false` | `0` (`OFF`) | 초기 자동 줄바꿈 여부 (`0`: 가로 스크롤) |
| `--ports <CSV>` | `mysql,kafka,ws,sync,api` | `13306,19092,9080,9081,9082` | 5대 서비스 포트 오버라이드 |

---

## 관련 문서

- **[변경 로그 이력 (CHANGELOG.md)](./CHANGELOG.md)**: 기능 추가·수정 및 WBS 변경 이력
- **[트러블슈팅 가이드 (TROUBLESHOOTING.md)](./TROUBLESHOOTING.md)**: 빌드·폰트·실행 환경 오류 해결 이력
- **[프로젝트 루트 README](../README.md)**: 전체 CSMS Lite 아키텍처 및 모듈 구성
