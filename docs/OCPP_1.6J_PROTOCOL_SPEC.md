# OCPP 1.6J 프로토콜 표준 규약서 (h2y-ocpp / CSMS Lite 적용 기준)

> **원본 기준 문서**: [LSELINK-EVCSOCPP-302003-DE_개발 표준 정의서-OCPP1.6프로토콜 (V3.3, 2025.11.07)](https://docs.google.com/document/d/1VfNHov4pLLSeo6OysqtgwzFcFmmJPyvj40pQYXC3bX0/edit?tab=t.0#heading=h.ee67gmnjkowt)  
> **정리 기준일**: 2026-11-18  
> **적용 대상**: `elvis-connect-ws`, `elvis-connect-sync`, `elvis-control-api`, `elvis-control-ui`, `elvis-tester-ui`

---

## 목차

1. [h2y-ocpp (CSMS Lite) 적용 요약 및 구현 매핑](#1-h2y-ocpp-csms-lite-적용-요약-및-구현-매핑)
2. [Messages (RPC Framework)](#2-messages-rpc-framework)
3. [충전기 전원 ON 및 기본 상태 동기화](#3-충전기-전원-on-및-기본-상태-동기화)
4. [충전 시작 ~ 종료 및 계량/과금 플로우](#4-충전-시작--종료-및-계량과금-플로우)
5. [미전송 전문 (오프라인 큐잉) 처리 규칙](#5-미전송-전문-오프라인-큐잉-처리-규칙)
6. [LS E-Link 확장 DataTransfer 규격](#6-ls-e-link-확장-datatransfer-규격)
7. [원격 제어 및 설정/펌웨어 관리](#7-원격-제어-및-설정펌웨어-관리)
8. [Local Authorization (현장 승인 리스트)](#8-local-authorization-현장-승인-리스트)
9. [결제 (Payment) 연동 규격](#9-결제-payment-연동-규격)
10. [OCPP-J Security Basic (Profile 1 / Profile 2)](#10-ocpp-j-security-basic-profile-1--profile-2)
11. [원본 문서 주요 개정이력 (V0.1 ~ V3.3)](#11-원본-문서-주요-개정이력-v01--v33)

---

## 1. h2y-ocpp (CSMS Lite) 적용 요약 및 구현 매핑

### 1.1 식별자 및 접속 표준 (`AGENTS.md` §4 연계)

- **WebSocket 접속 URL**: `ws://{WS_SERVER_IP}:{WS_SERVER_PORT}/ocpp/{chargeBoxId}` (`wss://` 지원 시 Security Profile 2 적용)
  - `elvis-connect-ws` 기본 경로: `/ocpp/{chargeBoxId}` (`:9080`), Subprotocol: `ocpp1.6` (하위 호환 `ocpp1.5`)
- **결합 식별자 (`chargeBoxId`) 규칙**: `{RECHGST_ID(6자리)}{RECHGR_ID(2자리)}` 총 8자리 (예: `000001` + `08` → `00000108`)
- **커넥터 ID (`connectorId`) 규약**:
  - `0`: 충전기 본체(전체 기기) — `BootNotification`, `Heartbeat`, 본체 `StatusNotification`, 전체 `ChangeAvailability(0)`
  - `1, 2, ...`: 개별 충전건(샘플·에뮬레이터 기본 듀얼건 `1`, `2`)
- **트랜잭션 ID (`transactionId`)**: CSMS(`StartTransaction.conf`)가 발급하는 32비트 signed integer 전역 단일 시퀀스

### 1.2 현재 코드베이스 구현 현황 및 규약서 대비 차이점

| 분류 | Action / `messageId` | 방향 | 규약서 상태 | `h2y-ocpp` 현재 구현 모듈 | 비고 / 적용 가이드 |
| :--- | :--- | :---: | :---: | :--- | :--- |
| **Core** | `BootNotification` | CP → CSMS | 필수 | `elvis-connect-sync` | 수신 시 `connectorId=0` 및 듀얼건 `1, 2` `Available` 초기화 |
| **Core** | `StatusNotification` | CP → CSMS | 필수 | `elvis-connect-sync` | `(charge_box_id, connector_id)` 기준 `connector_status` Upsert |
| **Core** | `Heartbeat` | CP → CSMS | 필수 | `elvis-connect-sync` | `connector_id=0`의 `heartbeat_at` / `last_heartbeat` 갱신 |
| **Core** | `Authorize` | CP → CSMS | 필수 | `elvis-connect-sync` | `rfid_card` / Redis 캐시 조회 및 `auth_hist` 기록 |
| **Core** | `StartTransaction` | CP → CSMS | 필수 | `elvis-connect-sync` | Redis `INCR` 기반 전역 `transactionId` 발급 및 세션 생성 |
| **Core** | `StopTransaction` | CP → CSMS | 필수 | `elvis-connect-sync` | 세션 종료 및 CDR(`charge_detail_record`) 정산 생성 |
| **Core** | `MeterValues` | CP → CSMS | 규약서 미사용 표기 | `elvis-connect-sync` | 표준 `MeterValues` 및 `DataTransfer(MeterValues)` 모두 `MeterValuesService`로 실시간 계측치 DB 반영 |
| **Remote** | `RemoteStartTransaction` | CSMS → CP | 필수 | `elvis-control-api`, `elvis-connect-ws` | `CommandController` + Kafka `ocpp-outbound-commands` |
| **Remote** | `RemoteStopTransaction` | CSMS → CP | 규약서 미사용 표기 | `elvis-control-api`, `elvis-connect-ws` | **차이점**: `h2y-ocpp` 관제 API/UI에서 원격 충전 종료 지원 |
| **Remote** | `Reset` (`Hard`/`Soft`) | CSMS → CP | 필수 | `elvis-control-api`, `elvis-connect-ws` | 본체 전체 리셋 |
| **Remote** | `UnlockConnector` | CSMS → CP | 선택 | `elvis-control-api`, `elvis-connect-ws` | 커넥터 잠금 해제 |
| **Remote** | `ChangeConfiguration` | CSMS → CP | 필수 | `elvis-control-api`, `elvis-connect-ws` | 설정키 변경 |
| **Remote** | `TriggerMessage` | CSMS → CP | 선택 | `elvis-control-api`, `elvis-connect-ws` | 상태/하트비트/부트 전문 재요청 |
| **Remote** | `ChangeAvailability` | CSMS → CP | 필수 | 미구현 (예정) | LS 커스텀 `Maintenance`/`Pause`/`Managecomplete`/`Restart` 포함 |
| **Firmware** | `UpdateFirmware` / `FirmwareStatusNotification` | 양방향 | 선택 | 미구현 (예정) | 펌웨어 배포 시 추가 |
| **LocalAuth** | `GetLocalListVersion` / `SendLocalList` | CSMS → CP | 선택 | 미구현 (예정) | 미사용 시 `GetLocalListVersion.conf`는 `-1` 반환 |
| **DataTransfer** | LS E-Link 확장 17종 (`MeterValues` 완료, 잔여 16종 예정) | 양방향 | 선택/확장 | `elvis-connect-sync` (`DataTransferService`) | `MeterValues` 실시간 계측치 DB 반영 완료, 미지원 `messageId`는 `UnknownMessageId` 회신 |
| **Security** | Security Profile 1 / Profile 2 | 양방향 | 선택 | `elvis-connect-ws` (`require-auth: false`) | 운영 전환 시 10장 인증키 갱신 및 TLS(`wss://`) 적용 |

---

## 2. Messages (RPC Framework)

### 2.1 개요 및 통신 원칙

- **표준 명칭**: OCPP 1.6 JSON Specification (OCPP-J 1.6)
- **전송 계층**: WebSocket (`ws://` 또는 보안 `wss://`)
- **서브프로토콜**: `Sec-WebSocket-Protocol: ocpp1.6`
- **통신 아키텍처**:
  - 전이중 비동기 RPC(Full-Duplex) 구조: 충전기(CP)와 서버(CSMS) 모두 `CALL` 요청 가능
  - WebSocket 단일 텍스트 프레임에 최상위 요소가 JSON Array(`[ ... ]`) 형태로 인코딩됨

### 2.2 메시지 분류 (`MessageTypeId`)

| MessageTypeId | 메시지 명칭 | 역할 및 기능 | JSON 배열 크기 |
| :---: | :--- | :--- | :---: |
| `2` | `CALL` | 원격 프로시저/액션 실행 요청 (Request) | 4 |
| `3` | `CALLRESULT` | 요청 정상 처리 응답 (Response) | 3 |
| `4` | `CALLERROR` | 문법 오류, 미지원 액션, 예외 발생 시 오류 응답 (Error) | 5 |

### 2.3 프레임 구조

#### 1) CALL (`MessageTypeId = 2`)
```json
[2, "<UniqueId>", "<Action>", { "<Payload>" }]
```
- `UniqueId` (`String`, 필수): 요청-응답 매핑용 고유 식별자 (최대 36자, UUID v4 권장)
- `Action` (`String`, 필수): 대소문자 엄격 구분 (예: `"BootNotification"`, `"StatusNotification"`)
- `Payload` (`JSON Object`, 필수): 입력 파라미터 본문

#### 2) CALLRESULT (`MessageTypeId = 3`)
```json
[3, "<UniqueId>", { "<Payload>" }]
```
- `UniqueId`: 원본 `CALL`의 `UniqueId`와 100% 동일해야 함
- `Payload`: 빈 응답일 경우 `{}` 전달

#### 3) CALLERROR (`MessageTypeId = 4`)
```json
[4, "<UniqueId>", "<ErrorCode>", "<ErrorDescription>", { "<ErrorDetails>" }]
```

| ErrorCode | 의미 및 발생 사유 |
| :--- | :--- |
| `NotImplemented` | 사양에는 정의되어 있으나 수신 시스템에서 미구현된 기능 |
| `NotSupported` | 해당 액션 자체를 지원하지 않거나 알지 못함 |
| `InternalError` | 수신자 내부 시스템 장애 (DB 실패, NPE 등) |
| `ProtocolError` | 메시지 교환 순서나 프로토콜 라이프사이클 위반 (`BootNotification` 승인 전 `StartTransaction` 등) |
| `SecurityError` | 인증키(`AuthorizationKey`)·TLS·비인가 단말 등 보안 정책 위반 |
| `FormationViolation` | JSON 문법 오류 또는 스키마 구조 위반 |
| `PropertyConstraintViolation` | 필드 값이 허용 범위/경계 제약조건 위반 |
| `OccurrenceConstraintViolation` | 배열/필드의 출현 횟수(최소/최대 cardinality) 위반 |
| `TypeConstraintViolation` | 데이터 타입 불일치 (예: 정수 필드에 문자열 전달) |
| `GenericError` | 위 표준 에러에 속하지 않는 일반 오류 |

### 2.4 실무 구현 수칙

1. **단일 WebSocket 프레임 원칙**: 하나의 WebSocket 텍스트 프레임에는 반드시 하나의 완결된 JSON Array만 담아야 함(줄바꿈으로 여러 전문 연결 시 `FormationViolation`).
2. **UniqueId 상관관계 보장**: `CALL` 발행 시 고유 `UniqueId`를 맵에 등록하고 `CALLRESULT`/`CALLERROR` 수신 시 매핑 완료 처리 (응답 타임아웃 통상 10~30초).
3. **순차 처리 및 직렬화**: `StartTransaction` → `StopTransaction` 등 동일 충전기/트랜잭션의 선후 관계가 깨지지 않도록 파티션 키(`chargeBoxId`) 기반 순차 처리 보장.

---

## 3. 충전기 전원 ON 및 기본 상태 동기화

### 3.1 `[CP → CSMS]` BootNotification

충전기 전원 ON 또는 재부팅 시 가장 먼저 전송하며, `TriggerMessage.req` 요청이 없는 한 임의 재전송하지 않습니다.

- **보류(`Pending`) 상태 규칙**: CSMS가 `Pending`을 반환한 경우 통신 채널을 유지한 채 `ChangeConfiguration`/`GetConfiguration` 등 설정 교환만 응답하며, `RemoteStartTransaction`/`RemoteStopTransaction` 및 다른 임의 `CALL`은 허용되지 않습니다.
- **승인(`Accepted`) 상태 규칙**: 응답의 `interval`에 따라 하트비트 주기를 설정하고 `currentTime`으로 내부 시계를 동기화합니다.

#### Request (`BootNotification.req`)
| 필드명 | 형식 | 필수 | 설명 |
| :--- | :--- | :---: | :--- |
| `chargeBoxSerialNumber` | `CiString25Type` | 필수 | 충전소 ID (예: `"000001"`) |
| `chargePointModel` | `CiString20Type` | 필수 | 충전기 모델명 |
| `chargePointSerialNumber` | `CiString25Type` | 필수 | 충전기 ID/시리얼 (예: `"08"`) |
| `chargePointVendor` | `CiString20Type` | 필수 | 제조사/벤더 코드 (예: `"LSELINK"`) |
| `firmwareVersion` | `CiString50Type` | 필수 | 펌웨어 버전 |
| `iccid`, `imsi`, `meterSerialNumber`, `meterType` | `CiString20/25Type` | 선택 | 모뎀 SIM / 주전력량계 정보 |

```json
[
  2,
  "ddaab9db-f6ed-4670-bf6b-934144a2ab72",
  "BootNotification",
  {
    "chargeBoxSerialNumber": "000001",
    "chargePointModel": "MC-MS120-1A",
    "chargePointSerialNumber": "08",
    "chargePointVendor": "LSELINK",
    "firmwareVersion": "1.0.0.0",
    "iccid": "",
    "imsi": "",
    "meterSerialNumber": "",
    "meterType": ""
  }
]
```

#### Response (`BootNotification.conf`)
- `currentTime` (`dateTime`, 필수): 서버 현재 시간
- `interval` (`Integer`, 필수): 승인 시 하트비트 주기(초), 미승인 시 재요청 최소 대기 시간(초)
- `status` (`RegistrationStatus`, 필수): `Accepted` | `Pending` | `Rejected`

```json
[
  3,
  "ddaab9db-f6ed-4670-bf6b-934144a2ab72",
  {
    "currentTime": "2023-02-01T05:54:40.785Z",
    "interval": 60,
    "status": "Accepted"
  }
]
```

---

### 3.2 `[CP → CSMS]` StatusNotification

충전기 본체(`connectorId = 0`) 또는 개별 커넥터(`connectorId >= 1`)의 상태 변화·오류 발생 시 전송합니다. (규약서 운영 기준: 5분 주기 또는 상태 변경 시 즉시 전송, 변경 시 `DataTransfer(statusnoti)` 병행 가능)

#### 주요 규약 비고
- **`connectorId = 0` 제한 상태**: `Available`, `Unavailable`, `Faulted`만 허용됩니다.
- **`Occupied` 5분류**: `Preparing`, `Charging`, `SuspendedEV`, `SuspendedEVSE`, `Finishing`
- **일시중지 우선순위**: 차량과 충전기 모두 일시중지 시 `SuspendedEVSE`가 `SuspendedEV`보다 우선합니다.
- **경고 vs 고장**: `status`가 `Faulted`가 아니면서 `errorCode`가 보고되면 경고로 간주하며 충전 작업은 계속 가능합니다(`EVCommunicationError` 등은 `SuspendedEVSE`로 처리).
- **`StopTransactionOnEVSideDisconnect` 연동**:
  - `false`일 때 EV 측 분리 시 → `SuspendedEV` (`errorCode: "NoError"`), 트랜잭션 유지
  - `true`일 때 EV 측 분리 시 → `Finishing` (`errorCode: "NoError"`), 트랜잭션 중지
- **오프라인 복구 시**: 오프라인 중 상태가 변경된 경우 연결 복구 시 현재 상태와 발생 오류를 보고합니다.

#### Request (`StatusNotification.req`)
| 필드명 | 형식 | 필수 | 설명 |
| :--- | :--- | :---: | :--- |
| `connectorId` | `Integer (>= 0)` | 필수 | `0`=본체 전체, `1~N`=개별 커넥터 |
| `errorCode` | `ChargePointErrorCode` | 필수 | 오류 코드 (정상 시 `"NoError"`) |
| `status` | `ChargePointStatus` | 필수 | 현재 상태 |
| `timestamp` | `dateTime` | 선택(권장) | 상태 보고 시각 (KST 기준) |
| `info`, `vendorId`, `vendorErrorCode` | `CiString50/255Type` | 선택 | 부가 오류 설명 및 제조사 에러코드 |

```json
[
  2,
  "6d876720-22ec-479c-b540-ab3c86d39112",
  "StatusNotification",
  {
    "connectorId": 1,
    "errorCode": "NoError",
    "info": null,
    "status": "Available",
    "timestamp": "2023-02-01T05:54:40.968Z",
    "vendorId": "LSELINK",
    "vendorErrorCode": null
  }
]
```
- **Response (`StatusNotification.conf`)**: `[3, "<UniqueId>", {}]`

#### `ChargePointStatus` 목록 (LS 확장 포함)
| 코드 | 설명 |
| :--- | :--- |
| `Available` | 신규 사용자 이용 가능 (충전대기) |
| `Preparing` | 케이블 결착 또는 태그 제시 등 충전 준비 중 |
| `Charging` | 접촉기 닫힘, 에너지 공급 중 |
| `SuspendedEVSE` | 충전기(EVSE) 측 사유로 일시 중지 (스마트 충전 제한 등) |
| `SuspendedEV` | 차량(EV) 측 사유로 일시 중지 (EV 미준비 등) |
| `Finishing` | 충전 세션 종료 후 케이블 분리 대기 등 |
| `Reserved` | 예약 중 |
| `Unavailable` | `ChangeAvailability(Inoperative)` 등으로 사용 불가 |
| `Faulted` | 오류/고장으로 에너지 공급 불가 |
| `Pause` *(LS 추가)* | 원격 제어 명령 시 충전 중 일시정지 |
| `Maintenance` *(LS 추가)* | 원격 제어 명령 시 점검 중 전환 |

#### `ChargePointErrorCode` 목록
`ConnectorLockFailure`, `EVCommunicationError`, `GroundFailure`, `HighTemperature`, `InternalError`, `LocalListConflict`, `NoError`, `OtherError`, `OverCurrentFailure`, `OverVoltage`, `PowerMeterFailure`, `PowerSwitchFailure`, `ReaderFailure`, `ResetFailure`, `UnderVoltage`, `WeakSignal`

---

### 3.3 `[CP → CSMS]` Heartbeat

일정 주기마다 충전기가 동작 중임을 알리고 서버 시간으로 내부 클록을 동기화하는 본체(`1기당 1건`) 메시지입니다.

- `BootNotification.conf` 응답 시 1회 전송 후 설정된 `interval` 주기마다 송수신합니다(LS E-Link 운영 설정 예: 60분 / 또는 `BootNotification` `interval` 기준).
- `Heartbeat.conf` 수신 시(매시간) 충전기는 `currentTime`으로 시간 동기화를 수행해야 합니다.

```json
// Request
[2, "b9e7751b-8109-41a6-81de-0a3f0c15f5ec", "Heartbeat", {}]

// Response
[3, "b9e7751b-8109-41a6-81de-0a3f0c15f5ec", { "currentTime": "2023-02-01T05:54:40.993Z" }]
```

---

## 4. 충전 시작 ~ 종료 및 계량/과금 플로우

### 4.1 표준 충전 시퀀스

| 순서 | 주체 | 전문 | 설명 |
| :---: | :---: | :--- | :--- |
| 1 | CP | `BootNotification` | 하드웨어 부팅 시에만 전송 |
| 2 | CP | `Heartbeat` | 주기 전송 및 `currentTime` 시간 동기화 |
| 3 | CP | `StatusNotification` | 주기(5분) 및 커넥터 상태 변경 시 즉시 전송 |
| 4 | CP | `DataTransfer(unitprice.req)` | 하드웨어 부팅 시 / 단가 조회 |
| 5 | CP | `Authorize` | 사용자 인증 (`C`/`M`/`N`/`K` prefix 확인) |
| 6 | CP | `DataTransfer(Authorize)` | 인증 시 `connectorId` 전달 (`uuid` 매핑) |
| 7 | CP | `DataTransfer(payment)` | 선결제 승인 정보 전달 (`trantype: "0210"`) |
| 8 | CP | **`StartTransaction`** | 충전 시작 알림 및 `transactionId` 발급 |
| 9 | CP | `DataTransfer(fullrechgsoc.req)` | 노선별 목표 SoC 목록 조회 *(버스/법인)* |
| 10 | CP | `DataTransfer(vehicleInfo)` | 차량 `evccId` (MAC) 전송 *(PNC)* |
| 11 | CP | `DataTransfer(chargingAlarm)` | 충전 시작 알림 (`msgType: 1`) |
| 12 | CP | `DataTransfer(MeterValues)` *(또는 `MeterValues`)* | 시작 시 초기값 + 주기 전송 (급속 1분 / 완속 15분) + 종료 시 최종값 |
| 13 | CP | `DataTransfer(chargingAlarm)` | SoC 90% 도달 알림 (`msgType: 2`) |
| 14 | CP | **`StopTransaction`** | 충전 종료 알림 및 `transactionData` 전달 |
| 15 | CP | `DataTransfer(payment)` | 결제 정산 정보 전달 (전체취소 `0430` / 부분취소 `0431`) |
| 16 | CP | `DataTransfer(chargingAlarm)` | 충전 종료 알림 (`msgType: 3`) |

---

### 4.2 `[CP → CSMS]` Authorize (인증)

- `idTag` 식별자 Prefix 규약:
  - `C...`: 법인
  - `M...`: 회원
  - `N...`: 비회원
  - `K...`: 환경부 로밍 (예: `K1010010201040507`)
- 위 규약(`C`/`M`/`N`/`K`)에 부합하는 유효 회원/카드는 `Accepted`, 그 외는 `Invalid` (또는 `Blocked`, `Expired`, `ConcurrentTx`) 응답.

```json
// Request
[2, "1c0724c1-6e0b-4f49-a759-363fa9f42c4e", "Authorize", { "idTag": "C7122" }]

// Response
[
  3,
  "1c0724c1-6e0b-4f49-a759-363fa9f42c4e",
  {
    "idTagInfo": {
      "parentIdTag": "C7122",
      "status": "Accepted"
    }
  }
]
```

---

### 4.3 `[CP → CSMS]` StartTransaction

충전 시작 시 전송하며 CSMS는 응답에 고유 `transactionId`를 발급합니다.

| 필드명 | 형식 | 필수 | 설명 |
| :--- | :--- | :---: | :--- |
| `connectorId` | `Integer (> 0)` | 필수 | 충전을 시작하는 커넥터 ID |
| `idTag` | `idToken` | 필수 | 인증 식별자 |
| `meterStart` | `Integer` | 필수 | 충전 시작 시점 누적 전력량 (Wh) |
| `reservationId` | `Integer` | 선택 | 예약을 종료시키는 트랜잭션인 경우 예약 ID |
| `timestamp` | `dateTime` | 필수 | 시작 시각 |

```json
// Request
[
  2,
  "a459e24e-67d5-417c-b63a-9cca1247d0e6",
  "StartTransaction",
  {
    "connectorId": 1,
    "idTag": "C7122",
    "meterStart": 621566,
    "reservationId": 0,
    "timestamp": "2023-02-01T08:26:02.832Z"
  }
]

// Response
[
  3,
  "a459e24e-67d5-417c-b63a-9cca1247d0e6",
  {
    "idTagInfo": {
      "parentIdTag": "C7122",
      "status": "Accepted"
    },
    "transactionId": 35
  }
]
```

---

### 4.4 `[CP → CSMS]` MeterValues (표준) / DataTransfer(MeterValues)

- **원본 규약서 운영 원칙**: 표준 `MeterValues`는 HMI 옵션상 비활성(`false`)으로 두고 **`DataTransfer(MeterValues)`**로 대체 보고합니다(급속 1분 / 완속 15분 주기).
- **h2y-ocpp 호환성**: `elvis-connect-sync`는 표준 `MeterValues` 전문도 파싱 및 `{}` 응답 처리할 수 있도록 구현되어 있으므로, 현장 단말 정책에 따라 두 방식 모두 수용 가능합니다.
- 표준 `MeterValues` Enum 요약:
  - `ReadingContext`: `Interruption.Begin`, `Interruption.End`, `Other`, `Sample.Clock`, `Sample.Periodic`, `Transaction.Begin`, `Transaction.End`, `Trigger`
  - `ValueFormat`: `Raw`, `SignedData`
  - `Measurand`: `Current.Export`, `Current.Import`, `Current.Offered`, `Energy.Active.Export.Register`, `Energy.Active.Import.Register`, `Frequency`, `Power.Active.Import`, `Power.Offered`, `SoC`, `Temperature`, `Voltage` 등
  - `Location`: `Body`, `Cable`, `EV`, `Inlet`, `Outlet`
  - `UnitOfMeasure`: `Wh`, `kWh`, `W`, `kW`, `A`, `V`, `Celsius`, `Percent` 등

---

### 4.5 `[CP → CSMS]` StopTransaction

| 필드명 | 형식 | 필수 | 설명 |
| :--- | :--- | :---: | :--- |
| `idTag` | `idToken` | 선택(권장) | 종료 요청 식별자 |
| `meterStop` | `Integer` | 필수 | 종료 시점 누적 전력량 (Wh) |
| `timestamp` | `dateTime` | 필수 | 종료 시각 |
| `transactionId` | `Integer` | 필수 | `StartTransaction.conf`에서 발급받은 ID |
| `reason` | `Reason` | 선택 | `Local`(정상), `Remote`, `EVDisconnected`, `EmergencyStop`, `HardReset`, `SoftReset`, `Reboot`, `PowerLoss`, `UnlockCommand`, `DeAuthorized`, `Other` |
| `transactionData` | `MeterValue[]` | 선택(LS 필수) | 과금/정산용 상세 계측치 (`MeterValues.req`와 동일 구조) |

```json
// Request
[
  2,
  "46317731-ac21-4c71-b321-e698ff9d2aa9",
  "StopTransaction",
  {
    "idTag": "C7122",
    "meterStop": 624697,
    "timestamp": "2023-02-01T08:50:08.296Z",
    "transactionId": 35,
    "reason": "Local",
    "transactionData": [
      {
        "timestamp": "2023-02-01T08:50:08.296Z",
        "sampledValue": [
          {
            "value": "3.131",
            "context": "Transaction.End",
            "format": "Raw",
            "measurand": "Energy.Active.Import.Register",
            "location": "Outlet",
            "unit": "kWh"
          },
          {
            "value": "95",
            "context": "Transaction.End",
            "format": "Raw",
            "measurand": "SoC",
            "location": "Outlet",
            "unit": "Percent"
          }
        ]
      }
    ]
  }
]

// Response
[
  3,
  "46317731-ac21-4c71-b321-e698ff9d2aa9",
  {
    "idTagInfo": {
      "parentIdTag": "C7122",
      "status": "Accepted"
    }
  }
]
```

---

## 5. 미전송 전문 (오프라인 큐잉) 처리 규칙

충전기-서버 간 통신 단절 시 충전기는 오프라인 데이터를 저장했다가 통신 재개 시 필수 전문만 순차 전송합니다.

1. **필수 전송 대상 전문**:
   - `Authorize`
   - `StartTransaction`
   - `DataTransfer(payment)` (선결제 / 취소)
   - `DataTransfer(MeterValues)`
   - `StopTransaction`
   - `DataTransfer(chargingAlarm)` (`msgType`: `1`, `3`)
2. **`transactionId` 채번 규칙 (V2.8 개정)**:
   - 충전 중 통신이 끊긴 경우: 기존 `StartTransaction.conf`에서 받은 `transactionId` 유지
   - 오프라인(로컬 모드)에서 충전을 시작한 경우: 재접속 후 `StartTransaction` 전송 시 서버 응답으로 새로 발급받은 `transactionId`를 이후 전문에 매핑
3. **전송 배치 제한**: **1회 미전송 전문 전송 개수는 10개 미만**으로 제한하여 서버/네트워크 폭주를 방지합니다.

---

## 6. LS E-Link 확장 DataTransfer 규격

공통 Envelope 구조:
- Request: `[2, "<UniqueId>", "DataTransfer", { "vendorId": "<String>", "messageId": "<String>", "data": "<JSON 직렬화 문자열>" }]`
- Response: `[3, "<UniqueId>", { "status": "Accepted | Rejected | UnknownMessageId | UnknownVendorId", "data": "<JSON 문자열 또는 null>" }]`

### 6.1 확장 `messageId` 전체 일람

| `messageId` | 방향 | 용도 | 주요 `data` 필드 |
| :--- | :---: | :--- | :--- |
| `unitprice.req` | CP → CSMS | 회원구분별 단가 조회 | Req: `chargeBoxSerialNumber`, `chargePointSerialNumber`, `connectorId`<br>Conf: `[{ UnitPrice, UserTypeCd(C/K/M/N), CrtrUnitPrice, RechgType(DC/AC) }]` |
| `statusnoti` | CP → CSMS | 주간/야간 모드 상태 알림 | `connectorId`, `status` (`DM`: 주간, `NM`: 야간) |
| `chargingAlarm` | CP → CSMS | 충전 진행/완료 SMS·알림톡 트리거 | `connectorId`, `msgType` (`1`: 시작, `2`: SoC 90%, `3`: 종료), `transactionId`, `idTag`, `phoneNum` |
| `Authorize` | CP → CSMS | `Authorize` 요청 시 누락되는 커넥터 ID 보완 전달 | `uuid` (직전 `Authorize`의 `UniqueId`), `connectorId`, `idTag` |
| `vehicleInfo` | CP → CSMS | PNC 차량 MAC(`evccId`) 전달 | `connectorId`, `idTag`, `evccId` |
| `changeelecmode` | CSMS → CP | 시간대별 충전전력(kW) 설정 | `connectorId`, `HH00` ~ `HH23` |
| `fullrechgsoc.req` | CP → CSMS | `StartTransaction` 시점 당일/익일 시간대별 목표 SoC 조회 | Req: `chargeBoxSerialNumber`, `chargePointSerialNumber`, `connectorId`, `idTag`, `timestamp`<br>Conf: `[{ day, dayType(WD/WE), HH00..HH23 }]` |
| `userSetSoc` | CP → CSMS | *(스마트제어)* 사용자 설정 목표 SoC 전달 (충전 시작 후 전송, 마지막 값 기준) | `chargeBoxSerialNumber`, `chargePointSerialNumber`, `connectorId`, `transactionId`, `idTag`, `setSoc`, `timestamp` |
| `batteryEncryptKey` | CSMS → CP | *(스마트제어)* 환경부 배터리 암호화 공개키/서명 전달 | `chargerId`, `keyId`, `encryptPub`, `signData`, `validTime`, `retVal` |
| `batteryInfo` | CP → CSMS | *(스마트제어)* 암호화된 전기차 배터리 상태 정보 전달 | `infoCnt`, `connectorId`, `tsdt`, `keyId`, `batteryDataSet: [{ timestamp, batteryData }]` |
| `appSetSoc` | CSMS → CP | *(스마트제어)* 앱 설정 목표 SoC 전달 (`RemoteStart` 시점) | `connectorId`, `idTag`, `setSoc` |
| `UpdateCertification` | CSMS → CP | *(스마트제어)* 환경부/충전기 인증서 다운로드 URL 전달 | `kmePublic`, `kmeCerti`, `chargerPrivate`, `chargerCerti`, `validTime` |
| `changemode.req` | CSMS → CP | 시간대별 충전 운영 모드 제어 | `connectorId` (`0`: DM/NM, `1~N`: WM/IM), `rechgAmt`, `rechgElec`, `HH00` ~ `HH23` |
| `rechgrsocschedule.req` | CSMS → CP | 평일/주말 시간대별 충전완료 SoC 스케줄 | `connectorId`, `DH00` ~ `DH23` (평일), `WH00` ~ `WH23` (주말) |
| `payment` | CP → CSMS | 현장 결제 승인/취소 정보 (9장 참조) | `chargeBoxSerialNumber`, `chargePointSerialNumber`, `connectorId`, `transactionId`, `idTag`, `timestamp`, `paymentInfo` |
| `keyInRechgInfo` | CSMS → CP | QR/바코드 Key-In 결제 금액 전달 | `chargeBoxSerialNumber`, `chargePointSerialNumber`, `connectorId`, `idTag`, `rechgKw`, `rechgAmt`, `timestamp` |
| `prePaymentAmt` | CSMS → CP | 모바일 선결제 금액 변경 (`RemoteStart` 이후 전송) | `chargeBoxSerialNumber`, `chargePointSerialNumber`, `connectorId`, `idTag`, `rechgKw`, `rechgAmt`, `timestamp` |

### 6.2 `DataTransfer(MeterValues)` 상세 스키마

```json
[
  2,
  "86a7a34a-1f4b-41de-981c-5ef967d12b09",
  "DataTransfer",
  {
    "vendorId": "LSELINK",
    "messageId": "MeterValues",
    "data": "{\"chargeBoxSerialNumber\":\"000001\",\"chargePointSerialNumber\":\"01\",\"connectorId\":1,\"transactionId\":1,\"idTag\":\"C005413\",\"timestamp\":\"2023-05-30T11:16:12.899Z\",\"power\":8441.0,\"eps\":367,\"ecu\":23.0,\"accWh\":5119.042,\"accTickWh\":0.14,\"accTickTime\":60,\"rechgHr\":1141,\"remnHr\":43,\"btrRm\":91,\"slprcUpc\":270.0,\"crtrUpc\":102.1}"
  }
]
```

| `data` 필드 | 타입 | 설명 |
| :--- | :--- | :--- |
| `chargeBoxSerialNumber` / `chargePointSerialNumber` / `connectorId` | `String` / `String` / `INT` | 충전소 ID / 충전기 ID / 충전건 ID |
| `transactionId` / `idTag` / `timestamp` | `INT` / `String` / `String` | 트랜잭션 ID / 승인식별자 / 일시 |
| `power` / `eps` / `ecu` | `Double` / `INT` / `Float` | 순시 전력 / 전압(V) / 전류(A) |
| `accWh` | `Double` | 계량기 누적 적산 전력량 (kWh, 소수점 3자리) |
| `accTickWh` / `accTickTime` | `Float` / `INT` | 직전 보고 대비 구간 사용 전력량(kWh) / 구간 시간(초, 예: 60) |
| `rechgHr` / `remnHr` | `INT` / `INT` | 충전 경과 시간(초) / 예상 잔여 시간(분) |
| `btrRm` | `INT` | 차량 배터리 잔량 SoC (%) |
| `slprcUpc` / `crtrUpc` | `Float` / `Float` | 매출단가(고객 부과) / 매입단가(한전 기준) |

### 6.3 `changemode.req` 시간대별 운영 모드

| 코드 | 명칭 | 적용 단위 | 동작 |
| :---: | :--- | :---: | :--- |
| `DM` | 전체사용 | 충전기(`connectorId=0`) | 양구 모두 사용 가능 (B2C는 `DM`만 사용 가능) |
| `NM` | 1구충전 | 충전기(`connectorId=0`) | 1구만 사용 (`Authorize.conf`의 `parentIdTag` 동일값 여부로 제어) |
| `WM` | 충전대기 | 커넥터(`connectorId>=1`) | 인증 허용, 충전 시작 불가 (진행 중 진입 시 중지) |
| `IM` | 충전불가 | 커넥터(`connectorId>=1`) | 인증 및 충전 모두 불가 |

---

## 7. 원격 제어 및 설정/펌웨어 관리

### 7.1 `[CSMS → CP]` ChangeAvailability

- **표준 `type`**: `Operative` (사용가능), `Inoperative` (사용불가)
- **LS E-Link 확장 `type`**: `Inoperative`(사용불가), `Maintenance`(점검 중), `Pause`(충전 중 일시정지), `Operative`(사용가능), `Managecomplete`(점검완료), `Restart`(일시정지에서 충전재개)
- **응답(`AvailabilityStatus`)**: `Accepted` | `Rejected` | `Scheduled` (트랜잭션 종료 후 적용 예정)
- `connectorId = 0` 전달 시 충전기 및 모든 커넥터에 적용되며, `Inoperative` 설정은 재부팅 후에도 유지됩니다. 변경 완료 시 충전기는 `StatusNotification.req`로 새 상태를 보고합니다.

### 7.2 `[CSMS → CP]` ChangeConfiguration & LS 전용 설정키

- **응답(`ConfigurationStatus`)**: `Accepted` | `Rejected` | `RebootRequired` | `NotSupported`
- **LS E-Link 전용 Configuration Key**:

| Key | 권한 | 타입 | 설명 |
| :--- | :---: | :--- | :--- |
| `FullRechgAmt` | RW | `Int` | 완충 기준 기본 선결제 충전 금액 (원) |
| `PersonUtztnLmtYn` | RW | `String` | 개인 사용 제한 여부 (`"Y"` / `"N"`) |
| `PersonUtztnLmtHr` | RW | `Int` | 개인 사용 제한 시간 (분, `Yn="Y"`일 때 적용) |
| `webSocketURL` | RW | `String` | WebSocket 접속 URL 변경 (충전 중이면 종료 후 적용) |
| `UseBasicAuth` | RW | `String` | Security Basic 인증 사용 여부 (`"true"` / `"false"`) |
| `AuthorizationKey` | RW | `String` | Security Basic 인증키 갱신 값 |

### 7.3 `[CSMS → CP]` RemoteStartTransaction / RemoteStopTransaction / Reset

- **`RemoteStartTransaction`**:
  - 필드: `idTag` (필수), `connectorId` (`> 0`, 선택), `chargingProfile` (선택, 목적 `TxProfile`)
  - 응답 `status`: `Accepted` | `Rejected`
  - 단말 내 `AuthorizeRemoteTxRequests = true`면 현장 인증 절차 수행 후 시작, `false`면 즉시 트랜잭션 시작 시도
- **`RemoteStopTransaction`**:
  - 필드: `transactionId` (필수)
  - 응답 `status`: `Accepted` | `Rejected`
  - *(원본 규약서는 미사용 표기이나 `h2y-ocpp` 원격제어 API/UI에서 지원)*
- **`Reset`**:
  - 필드: `type` (`"Hard"` | `"Soft"`)
  - 진행 중인 트랜잭션이 있으면 종료(`StopTransaction`, reason: `HardReset`/`SoftReset`) 후 재부팅/초기화

### 7.4 `[CSMS → CP]` UpdateFirmware & `[CP → CSMS]` FirmwareStatusNotification

- **`UpdateFirmware.req`**: `location` (`anyURI`), `retrieveDate` (`dateTime`), `retries` (`Integer`, 선택), `retryInterval` (`Integer`, 선택) → 응답 `{}`
- **`FirmwareStatusNotification.req`**: `Downloading` → `Downloaded` → `Installing` → `Installed` (실패 시 `DownloadFailed` / `InstallationFailed`, `TriggerMessage` 응답 시 `Idle`) → 응답 `{}`

---

## 8. Local Authorization (현장 승인 리스트)

- **`GetLocalListVersion`**:
  - 서버가 충전기의 현장 승인 리스트 버전 확인 (`{}`)
  - 충전기 최초 버전은 `0`, **미사용 시 `-1` 반환** (`{ "listVersion": -1 }`)
- **`SendLocalList`**:
  - 서버 버전과 불일치 시 `listVersion`, `updateType` (`"Full"` | `"Differential"`), `localAuthorizationList` (`[{ idTag, idTagInfo }]`) 전송

---

## 9. 결제 (Payment) 연동 규격

### 9.1 `[CP → CSMS]` DataTransfer(payment)

- **원칙**: 결제 승인·부분취소·전체취소는 충전기에서 PG 연동으로 수행 후 결과를 전달합니다(지원 PG: KG이니시스, 스마트로, 나이스페이먼츠).
- **`transactionId` 규칙**:
  - 정상 충전 시작 후 결제/부분취소: `StartTransaction.conf`로 발급받은 `transactionId` 전송
  - 선결제 시점 또는 충전 미개시 전체취소: `transactionId: 0` 전송
- **비회원 신용카드 결제 시 필수**: PG 구매자명에 **충전소 ID** 기입 필수
- **`trantype` 코드**: PG사 코드와 무관하게 `0210`(선결제 승인), `0431`(부분취소), `0430`(전체취소)로 통일

| `paymentInfo` 주요 필드 | 필수 | 설명 |
| :--- | :---: | :--- |
| `tid` | O | 결제승인관리번호 |
| `trantype` | O | `0210`(승인), `0431`(부분취소), `0430`(전체취소) |
| `errcode` | O | `0000`(정상승인), 실패 시 PG 에러코드 (`msg4`에 실패 내역 기록) |
| `cardno` / `halbu` / `tamt` | O | 마스킹 카드번호 / 할부개월 / 결제금액 |
| `trandate` / `trantime` / `authno` | O | 승인일자 / 승인시간 / 승인번호 |
| `merno`, `tran_serial`, `stlinst`, `reqinst`, `signpath`, `msg1`~`msg4` | 선택 | 가맹점·카드사·승인/실패 상세 메시지 |

### 9.2 `[CSMS → CP]` keyInRechgInfo / prePaymentAmt

- **`keyInRechgInfo`**: 충전기 부착 QR/바코드 스캔 후 웹 Key-In 결제 완료 시 충전기로 `rechgKw`, `rechgAmt` 전달
- **`prePaymentAmt`**: 모바일 앱 선결제 금액 변경 시 `RemoteStartTransaction` 이후 전달 (종료 또는 충전 실패 시 기본 `FullRechgAmt`로 리셋)

---

## 10. OCPP-J Security Basic (Profile 1 / Profile 2)

### 10.1 Security Profile 1 (Basic Authentication)

- 인증번호 만료 여부 확인은 `BootNotification` 시점이 아니라 **WebSocket Connection 시점**에 `Authorization` 헤더로 수행합니다.
- **적용 여부 제어**: CSMS가 `ChangeConfiguration(key="UseBasicAuth", value="true"|"false")` 전송
  - `true` + `Accepted`: 다음 연결부터 `Authorization` 헤더 검증 적용 (`Reject` 또는 무응답 시 미적용)
  - `false`: 충전기 응답과 무관하게 다음 연결부터 인증키 검증 미적용
- **초기/만료 키 갱신 플로우**:
  1. 최초 연결 시 LS E-Link 정의 규칙으로 생성한 초기 인증번호로 접속
  2. 서버가 초기값 또는 만료 키로 판단 시 `ChangeConfiguration(key="AuthorizationKey", value="<NEW_KEY>")` 전송 후 **연결 강제 종료**
  3. 충전기는 재연결 시 변경된 키로 접속해야 하며, 재전송 후에도 이전 키로 계속 접속 시도 시 연결 차단

### 10.2 Security Profile 2 (TLS + 인증서)

- TLS 1.2 이상 적용 및 `ws://` → `wss://` 전환 (서버 인증서 기반 검증, OCPP 1.6 Security Whitepaper Edition 3-2 참조)
- `ChangeConfiguration(key="webSocketURL", value="wss://...")` 수신 시 `wss://` 전환 처리

---

## 11. 원본 문서 주요 개정이력 (V0.1 ~ V3.3)

| 버전 | 일자 | 주요 변경 사항 요약 |
| :---: | :---: | :--- |
| V0.1 ~ V1.1 | 2023.01 ~ 2023.04 | 최초 규약 작성, HMI/REST·원격제어 방향 정리, 미사용 전문 표기 |
| V1.2 ~ V1.8 | 2023.04 ~ 2023.11 | `DataTransfer(unitprice.req, chargingAlarm, MeterValues, Authorize)` 추가, `Heartbeat.conf` 시간 동기화 명시 |
| V2.0 ~ V2.4 | 2023.12 ~ 2024.03 | `vehicleInfo` 추가, 미전송 전문 규칙 추가, `payment` `errcode` 및 `MeterValues` 전송주기 추가 |
| V2.5 ~ V2.9 | 2024.04 ~ 2024.12 | `changeelecmode`·`fullrechgsoc`·`changemode` 조건 정비, 미전송 `transactionId` 채번·`GetLocalListVersion(-1)`·Security Basic 개정 |
| V3.0 | 2025.04.22 | `DataTransfer(rechgrsocschedule)` 및 Security Basic Profile 2 적용 기준 추가 |
| V3.1 | 2025.08.18 | 스마트제어 충전 전문(`userSetSoc`, `appSetSoc`, `batteryInfo`, `batteryEncryptKey`) 및 Payment 순서 변경 |
| V3.2 | 2025.09.05 | 스마트제어 인증서 전송 `DataTransfer(UpdateCertification)` 추가 |
| V3.3 | 2025.11.07 | 모바일 선결제 금액 변경 `DataTransfer(prePaymentAmt)` 추가 |
