# OCPP 1.6J 엔진 점검 결과 및 작업 체크리스트 (보안 제외)

> **기준 규약 문서**: [OCPP_1.6J_PROTOCOL_SPEC.md](OCPP_1.6J_PROTOCOL_SPEC.md) (원본: LSELINK-EVCSOCPP-302003-DE V3.3)  
> **최초 점검 일시**: 2026-11-18 / **최종 업데이트**: 2026-11-19 (Phase 1 완료 반영)  
> **점검 범위**: `elvis-connect-ws`, `elvis-connect-sync`, `elvis-control-api`, `elvis-control-ui` (10장 Security Basic 제외)

---

## 1. 한눈에 보는 전체 점검 요약

| 영역 | 상태 | 요약 | 주요 영향 모듈 |
| :--- | :---: | :--- | :--- |
| **RPC 프레임 & WebSocket** | ✅ 개선완료 | `CALL`/`CALLRESULT`/`CALLERROR` 파싱 및 미지원 액션 `NotImplemented` 회신 구현. 64KB 버퍼 상향 완료 (하향 `UniqueId` 상관관계 추적만 잔여) | `elvis-connect-ws`, `elvis-connect-sync` |
| **Core 기본 (Boot/Status/Heartbeat)** | ✅ 개선완료 | WebSocket Ping/Pong(30s) 분리 + Idle 360s + DB 만료 65분 + 실시간 WS 세션 교차 검증(`연결안됨` 즉시 반영) + LS 커스텀 상태(`Pause`/`Maintenance`) 매핑 완료 | `elvis-connect-ws`, `elvis-connect-sync`, `elvis-control-api`, `elvis-control-ui` |
| **인증·세션·계량·과금** | ⚠️ 부분 가능 | `Authorize`/`StartTransaction`/`StopTransaction` + **표준/LS `MeterValues` 실시간 계측치 DB 반영 완료**. 단, `Authorize` prefix 규칙·`transactionData` 미구현 | `elvis-connect-sync` |
| **미전송 전문 (오프라인)** | ⚠️ 부분 가능 | `Start`/`StopTransaction` 멱등성·고아 세션 방어 및 `DataTransfer(MeterValues)` 처리 가능. 단, `payment`/`chargingAlarm` 미구현 | `elvis-connect-sync` |
| **LS 확장 `DataTransfer` 17종** | ⚠️ 부분 가능 | `DataTransferService` 라우터(`UnknownMessageId` 기본 응답) 및 `MeterValues` 구현 완료. 잔여 수신 9종·송신 8종 미구현 | `elvis-connect-sync`, `elvis-control-api` |
| **결제 (`payment` 등 3종)** | ❌ 미구현 | `DataTransfer(payment, keyInRechgInfo, prePaymentAmt)` 미구현 | `elvis-connect-sync`, `elvis-control-api` |
| **원격제어 & 펌웨어** | ⚠️ 부분 가능 | `RemoteStart/Stop`, `Reset`, `Unlock`, `ChangeConfig`, `Trigger` 6종 가능. **`ChangeAvailability`·`UpdateFirmware`·`chargingProfile` 미구현** | `elvis-control-api`, `elvis-control-ui` |
| **로컬 인증 (`LocalList`)** | ❌ 미구현 | `GetLocalListVersion`, `SendLocalList` 미구현 | `elvis-control-api`, `elvis-connect-sync` |

---

## 2. 영역별 상세 점검 결과

### 2.1 WebSocket 및 RPC 프레임워크 (규약서 2장)

| 점검 항목 | 판정 | 현재 구현 상태 및 갭 | 관련 파일 |
| :--- | :---: | :--- | :--- |
| WebSocket 경로 `/ocpp/{chargeBoxId}` 및 `ocpp1.6` 서브프로토콜 | ✅ 가능 | 핸드셰이크 URI 경로에서 `chargeBoxId` 추출, `ocpp1.6`/`ocpp1.5` 지원 | `elvis-connect-ws/.../config/WebSocketConfig.java` (L38-L42)<br>`elvis-connect-ws/.../handler/OcppWebSocketHandler.java` (L43-L49) |
| `CALL(2)` / `CALLRESULT(3)` / `CALLERROR(4)` 파싱 | ✅ 가능 | 3종 프레임 파싱 지원. 단말의 `CALLRESULT`/`CALLERROR`는 DLT 격리 없이 정상 소비 | `elvis-connect-sync/.../ocpp/OcppMessageParser.java` (L48-L118)<br>`elvis-connect-sync/.../consumer/OcppRawEventConsumer.java` (L103-L108) |
| 미지원 액션 `CALLERROR` 회신 | ✅ 완료 | `OcppRawEventConsumer` `default` 분기에서 `OcppResponseSender.sendNotImplementedError()` 호출해 `[4, uniqueId, "NotImplemented", ..., {}]` 회신 | `elvis-connect-sync/.../consumer/OcppRawEventConsumer.java`<br>`elvis-connect-sync/.../service/OcppResponseSender.java` |
| 하향 `CALL` 응답(`UniqueId`) 상관관계 추적 | ❌ 미구현 | 단말이 보낸 `CALLRESULT`/`CALLERROR`를 무시만 하며 원격명령 성공/실패(`Accepted`/`Rejected`) 비동기 매핑 없음 | `elvis-connect-sync/.../consumer/OcppRawEventConsumer.java` (L104-L108) |
| WebSocket 버퍼 크기 | ✅ 완료 | `buffer-size-bytes: 65536` (64KB) 상향 적용 완료 (`ELVIS_WS_BUFFER_SIZE_BYTES` 환경변수 조정 가능) | `elvis-connect-ws/src/main/resources/application.yml`<br>`elvis-connect-ws/.../config/WebSocketConfig.java` |

---

### 2.2 충전기 전원 ON 및 기본 상태 동기화 (규약서 3장)

| 전문 / 항목 | 판정 | 현재 구현 상태 및 갭 | 관련 파일 |
| :--- | :---: | :--- | :--- |
| **`BootNotification`** | ⚠️ 부분 | `Accepted`, `currentTime`(UTC `...Z`), `interval=60` 응답 및 `connectorId=0, 1, 2` `Available` 초기화.<br>**갭**: 미등록 충전기 `Rejected`/`Pending` 분기 및 `vendor`/`model`/`firmwareVersion`의 `charger` 테이블 반영 미구현. UTC vs KST 시간대 합의 확인 필요 | `elvis-connect-sync/.../service/ConnectorStatusService.java` (L124-L149)<br>`elvis-connect-sync/.../service/OcppResponseSender.java` (L48-L56) |
| **`StatusNotification`** | ⚠️ 부분 | `(charge_box_id, connector_id)` 기준 `connector_status` Upsert 및 `{}` 응답. LS 커스텀 상태 `'Pause'`, `'Maintenance'` 및 `'Reserved'` UI/API 매핑 완료, 대기/오프라인 복귀 시 순시 출력·전압·전류 자동 0 초기화.<br>**잔여 갭**: 규약서는 `status != Faulted`일 때 `EVCommunicationError` 등을 경고로 보지만, 엔진은 `errorCode != NoError`면 모두 장애(`fault_history`)로 기록함 | `elvis-connect-sync/.../service/ConnectorStatusService.java`<br>`elvis-connect-sync/.../service/impl/FaultDetectionServiceImpl.java`<br>`elvis-control-ui/src/stores/csmsStore.ts` |
| **`Heartbeat` & 연결 생존 관리** | ✅ 완료 | `connector_id=0`의 `heartbeat_at`/`last_heartbeat` 갱신 및 `currentTime` 응답 정상. WebSocket Ping/Pong(30초) 헬스체크 분리 + `idle-timeout-seconds: 360` + 연결 종료(`DISCONNECTED`) 이벤트 발행 + `elvis-connect-sync` 만료 안전망 65분 + `elvis-control-api` `/internal/sessions` 실시간 교차 검증 완료 | `elvis-connect-ws/.../session/LocalSessionStore.java`<br>`elvis-connect-ws/.../controller/InternalSessionController.java`<br>`elvis-control-api/.../service/WsSessionClient.java` |

---

### 2.3 충전 시작 ~ 종료 및 오프라인 미전송 (규약서 4장, 5장)

| 전문 / 항목 | 판정 | 현재 구현 상태 및 갭 | 관련 파일 |
| :--- | :---: | :--- | :--- |
| **`Authorize`** | ⚠️ 부분 | L1(Caffeine) → L2(Redis) → L3(MySQL `rfid_card`) 조회 후 `idTagInfo` 응답.<br>**갭**: 규약서의 `C`/`M`/`N`/`K` prefix 기반 구분(특히 비회원 `N...` 자동 허용 등) 없이 `rfid_card` 미등록 태그는 전부 `Invalid` 응답 | `elvis-connect-sync/.../service/impl/AuthorizeServiceImpl.java` (L55-L129)<br>`elvis-connect-sync/.../service/impl/MemberAuthCacheServiceImpl.java` (L81-L105) |
| **`StartTransaction`** | ✅ 가능 | `connectorId`, `idTag`, `meterStart`, `timestamp` 파싱 + Redis `INCR` 전역 32bit `transactionId` 채번 + 멱등성 방어 + `Charging` 상태 전이 + `StartTransaction.conf` 응답.<br>**참고**: `reservationId` 예약 검증/해제 연동은 미구현 | `elvis-connect-sync/.../service/TransactionSessionService.java` (L75-L173)<br>`elvis-connect-sync/.../service/TransactionSequenceService.java` (L84-L99) |
| **표준 `MeterValues` & `DataTransfer(MeterValues)`** | ✅ 완료 | `MeterValuesService`에서 표준 `sampledValue`(`W`/`kW`, `Wh`/`kWh`, `V`, `A`, `SoC`, `Temperature`) 및 LS `DataTransfer(MeterValues)`(`power`, `eps`, `ecu`, `accWh`, `rechgHr`, `btrRm`, `idTag`)를 단일 `upsertTelemetry` 쿼리로 `connector_status`에 실시간 반영 | `elvis-connect-sync/.../service/MeterValuesService.java`<br>`elvis-connect-sync/.../service/DataTransferService.java`<br>`elvis-connect-sync/.../repository/ConnectorStatusRepository.java` |
| **`StopTransaction`** | ⚠️ 부분 | `transactionId`, `meterStop`, `timestamp`, `idTag`, `reason` 11종 처리 + `Available` 복귀 + TOU CDR 과금 생성 + `StopTransaction.conf` 응답.<br>**갭**: `transactionData` 내부 배열 파싱·저장 미구현 | `elvis-connect-sync/.../service/TransactionSessionService.java` (L189-L316)<br>`elvis-connect-sync/.../domain/StopReason.java`<br>`elvis-connect-sync/.../service/impl/TouRatingServiceImpl.java` |
| **미전송 전문 (오프라인)** | ⚠️ 부분 | `StartTransaction` 멱등성 및 Orphan `StopTransaction` 방어는 구현됨.<br>**갭**: 오프라인 복구 시 함께 전송되는 `DataTransfer(MeterValues, payment, chargingAlarm)` 미처리 | `elvis-connect-sync/.../service/TransactionSessionService.java` (L94-L108, L223-L277) |

---

### 2.4 LS E-Link 확장 `DataTransfer` & 결제 (규약서 6장, 9장)

`DataTransferService` 디스패처와 `MeterValues` 처리(및 미지원 `messageId`에 대한 `UnknownMessageId` 회신)가 구현되었으며, 나머지 16종은 Phase 2~3 예정입니다.

#### CP → CSMS 수신 `DataTransfer` (10종) — 1종 완료 / 9종 미구현 (`UnknownMessageId` 회신)
1. `unitprice.req` — 회원구분(`C`/`K`/`M`/`N`)별 단가 목록 응답 필요
2. `statusnoti` — 주간/야간 모드(`DM`/`NM`) 상태 수신
3. `chargingAlarm` — `msgType`(`1`: 시작, `2`: SoC 90%, `3`: 종료) 알림 트리거
4. `Authorize` — 직전 `Authorize` `uuid`와 `connectorId` 매핑 기록
5. `vehicleInfo` — 차량 `evccId`(MAC) 저장
6. `MeterValues` *(✅ 완료)* — `power`(W→kW), `eps`, `ecu`, `accWh`, `rechgHr`(초→분), `btrRm`, `idTag` 파싱 및 `connector_status` 실시간 반영 + `Accepted` 응답
7. `fullrechgsoc.req` — 당일/익일 시간대별(`HH00`~`HH23`) 노선별 목표 SoC 응답
8. `userSetSoc` *(스마트제어)* — 사용자 설정 목표 SoC 저장
9. `batteryInfo` *(스마트제어)* — 암호화된 `batteryDataSet` 수신 및 적재
10. `payment` *(결제)* — PG 선결제(`0210`) / 부분취소(`0431`) / 전체취소(`0430`) 승인 정보 저장

#### CSMS → CP 송신 `DataTransfer` (8종) — ❌ 발행 기능 없음
1. `changeelecmode` — 시간대별 충전전력(`HH00`~`HH23`) 전송
2. `changemode.req` — 시간대별 운영 모드(`DM`/`NM`/`WM`/`IM`) 전송
3. `rechgrsocschedule.req` — 평일(`DH00`~`DH23`)/주말(`WH00`~`WH23`) 목표 SoC 스케줄 전송
4. `batteryEncryptKey` *(스마트제어)* — 환경부 배터리 암호화 공개키/서명 전달
5. `appSetSoc` *(스마트제어)* — 앱 설정 목표 SoC 전달 (`RemoteStart` 시점)
6. `UpdateCertification` *(스마트제어)* — 인증서 다운로드 경로 전달
7. `keyInRechgInfo` *(결제)* — QR/바코드 Key-In 결제 금액 전달
8. `prePaymentAmt` *(결제)* — 모바일 선결제 금액 변경 전달

---

### 2.5 원격 제어, 펌웨어, 로컬 인증 (규약서 7장, 8장)

| 전문 / 항목 | 판정 | 현재 구현 상태 및 갭 | 관련 파일 |
| :--- | :---: | :--- | :--- |
| `RemoteStartTransaction` | ⚠️ 부분 | `/api/v1/commands/remote-start`로 CALL 발행 가능. 단, `chargingProfile`(`TxProfile`) 미지원 | `elvis-control-api/.../controller/CommandController.java` (L35-L43)<br>`elvis-control-api/.../dto/command/RemoteStartCommandRequest.java` |
| `RemoteStopTransaction`, `Reset`, `UnlockConnector`, `ChangeConfiguration`, `TriggerMessage` | ✅ 가능 | `/api/v1/commands/*` 통해 OCPP CALL 4-Tuple 발행 가능.<br>**주의**: `elvis-control-ui` 제어반은 현재 `/api/v1/chargers/{id}/remote-command`(DB만 갱신)를 호출 중이므로 실제 단말 전송 시 `/api/v1/commands/*` 연동 필요 | `elvis-control-api/.../service/impl/OutboundCommandServiceImpl.java` (L80-L189)<br>`elvis-control-api/.../controller/ChargerController.java` (L55-L89) |
| `ChangeAvailability` | ❌ 미구현 | 표준(`Operative`, `Inoperative`) + LS 확장(`Maintenance`, `Pause`, `Managecomplete`, `Restart`) 미구현 | - |
| `UpdateFirmware` / `FirmwareStatusNotification` | ❌ 미구현 | 송신 API 및 상태 수신 핸들러 미구현 | - |
| `GetLocalListVersion` / `SendLocalList` | ❌ 미구현 | 송신 API 미구현 (충전기 미사용 시 `-1` 응답 처리 규약 참고) | - |

---

## 3. 작업 우선순위 체크리스트 (내일 작업용)

### Phase 1 — 통신 끊김 방지 & 기본 호환성 확보 (최우선)

- [x] **1. `elvis-connect-ws` Idle Timeout 조정** *(2026-11-19 완료)*
  - 파일: `elvis-connect-ws/src/main/resources/application.yml`, `WebSocketConfig.java`, `WebSocketProperties.java`, `OcppWebSocketHandler.java`, `LocalSessionStore.java`, `elvis-connect-sync/src/main/resources/application.yml`
  - 작업: WebSocket Ping/Pong 헬스체크(`ping-interval-seconds: 30`, `handlePongMessage`) 분리 + `idle-timeout-seconds: 360`, `buffer-size-bytes: 65536`(64KB) 상향, `elvis-connect-sync` 만료 안전망 `heartbeat-timeout-seconds: 3900` 조정
- [x] **2. `OcppResponseSender`에 `CALLERROR` 발송 메서드 추가 + `OcppRawEventConsumer` `default` 분기 처리** *(2026-11-19 완료)*
  - 파일: `elvis-connect-sync/.../service/OcppResponseSender.java`, `OcppRawEventConsumer.java`
  - 작업: 미지원/미구현 액션 수신 시 `[4, uniqueId, "NotImplemented", "...", {}]` 회신해 단말 타임아웃 대기 방지
- [x] **3. `OcppRawEventConsumer`에 `DataTransfer` 디스패처 추가 (최소 응답 보장)** *(2026-11-19 완료)*
  - 파일: `elvis-connect-sync/.../consumer/OcppRawEventConsumer.java`, 신규 `DataTransferService.java`
  - 작업: `vendorId` / `messageId` 분기 라우터 구축. 지원하지 않는 `messageId`는 `{"status":"UnknownMessageId"}` 회신
- [x] **4. `DataTransfer(MeterValues)` 및 표준 `MeterValues` 실시간 계측치 DB 반영** *(2026-11-19 완료)*
  - 파일: `elvis-connect-sync/.../repository/ConnectorStatusRepository.java` (`upsertTelemetry`), 신규 `MeterValuesService.java`
  - 작업: `power_kw`, `voltage_v`, `current_a`, `soc_percent`, `battery_temp_c`, `accumulated_kwh`, `charging_minutes`를 단일 Upsert + `COALESCE`로 실시간 반영, 대기/종료 상태 복귀 시 순시 출력·전압·전류 자동 0 초기화
- [x] **5. `elvis-control-ui` `STATUS_MAP`에 LS 커스텀 상태 추가** *(2026-11-19 완료)*
  - 파일: `elvis-control-ui/src/stores/csmsStore.ts`, `elvis-control-api/.../service/ChargerService.java`
  - 작업: `'Pause'` (`일시중지`), `'Maintenance'` (`점검중`), `'Reserved'` (`예약중`) 영문 키 매핑 및 API 우선순위/요약 집계 반영

---

### Phase 2 — LS E-Link 핵심 업무 전문 구현

- [ ] **6. `DataTransfer` 수신 필수 전문 구현**
  - `unitprice.req`: `pricing_policy` / `pricing_tou_rule` (또는 단가 조회 서비스) 기반 `C`/`K`/`M`/`N` 단가 응답 조립
  - `Authorize`: 직전 `Authorize` 이력(`auth_hist`)에 `connectorId` 보완 연결
  - `payment`: 결제 승인/취소 원장 테이블(`payment_history` 등) 신설 및 `transactionId` (`0` vs 발급 ID) 연계 저장
  - `chargingAlarm` (`msgType` 1/2/3), `statusnoti` (`DM`/`NM`), `vehicleInfo` (`evccId`) 처리 및 `Accepted` 응답
- [ ] **7. `Authorize` prefix(`C`/`M`/`N`/`K`) 정책 및 `BootNotification` 단말 검증 여부 결정**
  - `N...`(비회원) 선결제/현장태그 허용 정책, `BootNotification` 미등록 단말 `Accepted` vs `Rejected` 정책 확정 후 `MemberAuthCacheServiceImpl` / `ConnectorStatusService` 반영
- [ ] **8. 관제 UI 원격제어 → 실제 OCPP 하향 명령(`/api/v1/commands/*`) 연동**
  - 파일: `elvis-control-ui/src/stores/csmsStore.ts`, `elvis-control-api/.../controller/CommandController.java`
  - 작업: `RemoteStartTransaction`, `RemoteStopTransaction`, `Reset`, `UnlockConnector` 호출 시 Kafka `ocpp-outbound-commands`를 통해 실제 WebSocket 단말로 전문 전송되도록 통합 (시뮬레이션용 DB 직접 수정과 분리/병행)
- [ ] **9. `StopTransaction.transactionData` 파싱 및 `FaultDetectionServiceImpl` 경고성 `errorCode` 구분**
  - `status != Faulted`인 경고성 코드(`EVCommunicationError` 등)를 장애 이력(`fault_history`)과 분리할지 정책 확인

---

### Phase 3 — 원격 스케줄·로컬인증·스마트제어·펌웨어 확장

- [ ] **10. `ChangeAvailability` (LS 확장 6종 포함) 하향 명령 추가**
  - `OutboundCommandServiceImpl` + `CommandController`에 추가 (`Operative`, `Inoperative`, `Maintenance`, `Pause`, `Managecomplete`, `Restart`)
- [ ] **11. `DataTransfer` 하향 송신 명령 구현**
  - 운영 제어: `changemode.req`, `changeelecmode`, `rechgrsocschedule.req`
  - 결제/선결제: `keyInRechgInfo`, `prePaymentAmt`
  - 스마트제어: `batteryEncryptKey`, `appSetSoc`, `UpdateCertification` (수신: `userSetSoc`, `batteryInfo`, `fullrechgsoc.req`)
- [ ] **12. `UpdateFirmware` / `FirmwareStatusNotification`, `GetLocalListVersion` / `SendLocalList`, `ChargingProfile` 구현**
  - `SendLocalList` 사용 시 `elvis-connect-ws` 버퍼 크기 상향 동반 검토
- [ ] **13. 하향 `CALL` 응답(`CALLRESULT`/`CALLERROR`) 상관관계 추적**
  - `UniqueId` 기반 명령 결과(`Accepted`/`Rejected`/Timeout) 상태 저장 및 관제 UI 알림 연계
