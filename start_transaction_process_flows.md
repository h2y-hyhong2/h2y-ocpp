# OCPP[^OCPP] 충전 거래 라이프사이클 시퀀스 다이어그램

이 문서는 사용자의 충전 및 결제 방식(충전 유형)에 따른 네 가지 핵심 시나리오별로 충전기의 시스템 기동(`BootNotification`)부터 시작하여 인증(`Authorize`), 충전 시작(`StartTransaction`), 충전 종료(`StopTransaction`)까지의 전체 연동 흐름을 정의합니다.

---

## 1. 회원 카드 태깅 충전 라이프사이클 (`Card Charging Lifecycle`)

사용자가 충전기에 실물 회원 카드(RFID[^RFID])를 태깅하여 인증하고 충전을 진행하는 표준 시나리오입니다.

```mermaid
sequenceDiagram
    autonumber
    actor 사용자 as User
    participant 충전기 as Charger
    participant OCPP as OCPP REST Service
    participant Redis as Redis Cache
    participant DB as MS-SQL Database

    %% 1. 부팅 및 초기 설정 (BootNotification)
    충전기 ->> OCPP: BootNotification.req (model, vendor, serialNumber, firmwareVersion, iccid)
    Note over OCPP: 마스터 정보 일치 여부 검증
    OCPP ->> DB: Boot 이력 저장 (bootTrMapper.insertTbBootTr)
    OCPP -->> 충전기: BootNotification.conf (status = Accepted, currentTime, interval)

    opt WebSocket 접속 URL 불일치 시
        OCPP ->> DB: 설정 변경 이력 생성
        OCPP ->> 충전기: ChangeConfiguration.req (key: "webSocketURL", value: deplyWsCntnInfo)
        충전기 -->> OCPP: ChangeConfiguration.conf (status = Accepted)
        Note over 충전기: 신규 WebSocket URL로 재접속 시도
    end

    %% 2. 인증 단계 (Authorize)
    사용자 ->> 충전기: 회원 카드 태그 (UserType.C)
    충전기 ->> OCPP: Authorize.req (idTag)
    OCPP ->> Redis: 인증 마스터 조회 (csmsCertDao.findByIdtag)
    
    alt CsmsCert 존재 및 유효함
        Redis -->> OCPP: csmsCert 정보 반환
        Note over OCPP: 법인 유효성 및 빌링키 만료 여부 확인
        OCPP -->> 충전기: Authorize.conf (status = Accepted)
        충전기 ->> 사용자: 인증 성공 알림
    else CsmsCert 존재하지 않거나 유효하지 않음
        OCPP -->> 충전기: Authorize.conf (status = Invalid/Expired)
        충전기 ->> 사용자: 인증 실패 알림 및 반환
    end

    %% 3. 시작 단계 (StartTransaction)
    충전기 ->> OCPP: StartTransaction.req (idTag, connectorId, meterStart)
    OCPP ->> Redis: 인증 마스터 조회 (csmsCertDao.findByIdtag)
    Redis -->> OCPP: csmsCert 반환
    Note over OCPP: parentIdTag = 차량번호 (userNm) 설정<br/>userNo = 회원번호 설정
    OCPP ->> DB: 거래 ID 채번 (transactionIdManager.getNextTransactionIdDistributed)
    DB -->> OCPP: 신규 trnId 반환
    OCPP ->> DB: 충전 시작 정보 저장 (tbStartTrMapper.saveTbStartTr)
    OCPP -->> 충전기: StartTransaction.conf (status = Accepted, trnId)
    충전기 ->> 사용자: 충전 시작 및 커넥터 잠금

    %% 4. 종료 단계 (StopTransaction)
    사용자 ->> 충전기: 충전 중지 요청 (또는 카드 재태그)
    충전기 ->> OCPP: StopTransaction.req (transactionId, idTag, meterStop, transactionData)
    OCPP ->> Redis: 인증 마스터 조회 (csmsCertDao.findByIdtag)
    OCPP ->> DB: 충전 시작 정보 조회 (tbStartTrMapper.selectTbStartTr)
    DB -->> OCPP: startTr (시작전력량 등) 반환
    OCPP ->> DB: 충전 이력 저장 (operRechgHistMapper.saveOperRechgHist)
    OCPP -->> 충전기: StopTransaction.conf (status = Accepted)
    충전기 ->> 사용자: 충전 완료 및 커넥터 잠금 해제
```

---

## 2. 모바일 앱 원격 충전 라이프사이클 (`Remote App Charging Lifecycle`)

모바일 앱에서 선결제 후 원격 충전 명령을 내려 기동하고, 종료 후 모바일 백엔드로 푸시를 전송하는 시나리오입니다.

```mermaid
sequenceDiagram
    autonumber
    actor 사용자 as User
    participant 앱 as Mobile App
    participant 충전기 as Charger
    participant OCPP as OCPP REST Service
    participant Redis as Redis Cache
    participant DB as MS-SQL Database

    %% 1. 부팅 및 초기 설정 (BootNotification)
    충전기 ->> OCPP: BootNotification.req (model, vendor, serialNumber, firmwareVersion, iccid)
    Note over OCPP: 마스터 정보 일치 여부 검증
    OCPP ->> DB: Boot 이력 저장 (bootTrMapper.insertTbBootTr)
    OCPP -->> 충전기: BootNotification.conf (status = Accepted, currentTime, interval)

    opt WebSocket 접속 URL 불일치 시
        OCPP ->> DB: 설정 변경 이력 생성
        OCPP ->> 충전기: ChangeConfiguration.req (key: "webSocketURL", value: deplyWsCntnInfo)
        충전기 -->> OCPP: ChangeConfiguration.conf (status = Accepted)
        Note over 충전기: 신규 WebSocket URL로 재접속 시도
    end

    %% 2. 선행 결제 및 원격 기동 (RemoteStart)
    사용자 ->> 앱: 충전 금액 결제 및 시작 요청
    Note over 앱, DB: [선결제 이력 생성 및 trnId 미리 발급]<br/>(OPER_STLM_HIST / OPER_RMT_START_HIST)
    앱 ->> OCPP: 원격기동 API 호출 (RemoteStartTransaction)
    OCPP ->> 충전기: RemoteStartTransaction.req (idTag, connectorId)
    충전기 -->> OCPP: RemoteStartTransaction.conf (status = Accepted)
    
    %% 3. 시작 단계 (StartTransaction)
    충전기 ->> OCPP: StartTransaction.req (idTag, connectorId, meterStart)
    OCPP ->> Redis: 원격시작이력 조회 (operRmtStartHistDao.findByRechgstIdAndRechgrIdAndCnnctrIdAndIdtag)
    Redis -->> OCPP: 원격시작이력 (rmtStar) 반환
    OCPP ->> DB: 최근 5분 내 결제 이력 조회 (selectRecentStlmHist)
    DB -->> OCPP: 결제 이력 (stlmHist) 반환
    
    rect rgb(240, 248, 255)
        Note over OCPP: [기존 결제/원격시작 거래 ID 매핑]
        Note over OCPP: 5분 내의 원격시작 trnId 또는 선결제 trnId 중<br/>DB(TB_STARTTR_TR)에 등록되지 않은 trnId를 재사용
    end
    
    OCPP ->> DB: 충전 시작 정보 저장 (tbStartTrMapper.saveTbStartTr)
    OCPP ->> DB: 모바일 결제 정보 유효성 검증 (stlmRechgMngMapper.selectStlmRechgMng)
    
    alt 결제 취소 또는 타임아웃 경과 시
        OCPP -->> 충전기: StartTransaction.conf (status = Blocked, trnId = 0)
        충전기 ->> 사용자: 충전 거부 및 커넥터 잠금 해제
    else 결제 정상
        OCPP ->> DB: 순차 충전 이력 업데이트 (seqRechgHistMapper.updateSeqRechgHist)
        OCPP -->> 충전기: StartTransaction.conf (status = Accepted, trnId)
        충전기 ->> 사용자: 충전 시작
    end

    %% 4. 종료 단계 (StopTransaction)
    충전기 ->> OCPP: StopTransaction.req (transactionId, meterStop, transactionData)
    OCPP ->> DB: 충전 시작 정보 조회 (tbStartTrMapper.selectTbStartTr)
    DB -->> OCPP: startTr (userNo 등) 반환
    OCPP ->> DB: 충전 이력 저장 (operRechgHistMapper.saveOperRechgHist)
    
    Note over OCPP: UserType == UserType.M 확인 후<br/>모바일 백엔드로 충전완료 푸시 연동 요청 호출
    
    OCPP -->> 충전기: StopTransaction.conf (status = Accepted)
    충전기 ->> 사용자: 충전 종료
```

---

## 3. 비회원 현장 결제 충전 라이프사이클 (`Key-In Charging Lifecycle`)

충전기 터미널 화면에서 비회원이 신용카드를 삽입하거나 번호를 입력(Key-In)하여 결제 후 충전하는 시나리오입니다.

```mermaid
sequenceDiagram
    autonumber
    actor 사용자 as User
    participant 충전기 as Charger
    participant OCPP as OCPP REST Service
    participant DB as MS-SQL Database
    participant IFS as service-ifs (관제 연동)

    %% 1. 부팅 및 초기 설정 (BootNotification)
    충전기 ->> OCPP: BootNotification.req (model, vendor, serialNumber, firmwareVersion, iccid)
    Note over OCPP: 마스터 정보 일치 여부 검증
    OCPP ->> DB: Boot 이력 저장 (bootTrMapper.insertTbBootTr)
    OCPP -->> 충전기: BootNotification.conf (status = Accepted, currentTime, interval)

    opt WebSocket 접속 URL 불일치 시
        OCPP ->> DB: 설정 변경 이력 생성
        OCPP ->> 충전기: ChangeConfiguration.req (key: "webSocketURL", value: deplyWsCntnInfo)
        충전기 -->> OCPP: ChangeConfiguration.conf (status = Accepted)
        Note over 충전기: 신규 WebSocket URL로 재접속 시도
    end

    %% 2. 선결제 완료
    사용자 ->> 충전기: 신용카드 결제 (Key-In / 단말기 승인)
    Note over 충전기, DB: [선결제 이력 생성 및 trnId 발급]<br/>(OPER_STLM_HIST / OPER_KEYIN_HIST)

    %% 3. 시작 단계 (StartTransaction)
    충전기 ->> OCPP: StartTransaction.req (idTag, connectorId, meterStart)
    OCPP ->> DB: 최근 5분 내 결제 이력 조회 (selectRecentStlmHist)
    DB -->> OCPP: 결제 이력 (stlmHist) 반환
    
    alt 5분 이내 선결제 trnId 재사용 가능
        Note over OCPP: trnId = 결제 거래 ID 재사용
    else 재사용 불가
        OCPP ->> DB: 신규 거래 ID 채번
    end
    
    OCPP ->> DB: 충전 시작 정보 저장 (tbStartTrMapper.saveTbStartTr)
    OCPP ->> DB: KeyIn 결제 상태 조회 (operKeyinHistMapper.selectOperKeyinHist)
    DB -->> OCPP: KeyIn 결제 이력 반환
    
    alt 결제가 이미 취소된 상태
        OCPP -->> 충전기: StartTransaction.conf (status = Blocked, trnId = 0)
        충전기 ->> 사용자: 충전 취소 안내
    else 결제 정상
        OCPP ->> IFS: 충전 시작 상태 알림 API 호출 (KEYIN_STATUS_CHARGING_START)
        IFS -->> OCPP: 응답 수신
        OCPP -->> 충전기: StartTransaction.conf (status = Accepted, trnId)
        충전기 ->> 사용자: 충전 시작
    end

    %% 4. 종료 단계 (StopTransaction)
    충전기 ->> OCPP: StopTransaction.req (transactionId, meterStop, transactionData)
    OCPP ->> DB: 충전 시작 정보 조회 (tbStartTrMapper.selectTbStartTr)
    DB -->> OCPP: startTr 반환
    OCPP ->> DB: 충전 이력 저장 (operRechgHistMapper.saveOperRechgHist)
    OCPP -->> 충전기: StopTransaction.conf (status = Accepted)
```

---

## 4. 환경부 및 로밍 회원 카드 충전 라이프사이클 (`Roaming Charging Lifecycle`)

타사 환경부 또는 로밍 카드(`UserType.K`)를 태깅하여 시작하며, 로밍 연동망을 통해 결제 및 이력을 송수신하는 시나리오입니다.

```mermaid
sequenceDiagram
    autonumber
    actor 사용자 as User
    participant 충전기 as Charger
    participant OCPP as OCPP REST Service
    participant DB as MS-SQL Database
    participant IFS as service-ifs (환경부/로밍망)

    %% 1. 부팅 및 초기 설정 (BootNotification)
    충전기 ->> OCPP: BootNotification.req (model, vendor, serialNumber, firmwareVersion, iccid)
    Note over OCPP: 마스터 정보 일치 여부 검증
    OCPP ->> DB: Boot 이력 저장 (bootTrMapper.insertTbBootTr)
    OCPP -->> 충전기: BootNotification.conf (status = Accepted, currentTime, interval)

    opt WebSocket 접속 URL 불일치 시
        OCPP ->> DB: 설정 변경 이력 생성
        OCPP ->> 충전기: ChangeConfiguration.req (key: "webSocketURL", value: deplyWsCntnInfo)
        충전기 -->> OCPP: ChangeConfiguration.conf (status = Accepted)
        Note over 충전기: 신규 WebSocket URL로 재접속 시도
    end

    %% 2. 인증 단계 (Authorize)
    사용자 ->> 충전기: 환경부 카드 태그 (UserType.K)
    충전기 ->> OCPP: Authorize.req (idTag)
    Note over OCPP: UserType == UserType.K 확인
    OCPP ->> IFS: 환경부 회원카드 조회 API 요청 (selectKmeCard)
    IFS -->> OCPP: 카드 유효성 인증 결과 반환 (isKmeSelect = true/false)
    
    alt 인증 성공
        OCPP -->> 충전기: Authorize.conf (status = Accepted)
        충전기 ->> 사용자: 로밍 카드 승인 알림
    else 인증 실패
        OCPP -->> 충전기: Authorize.conf (status = Invalid)
        충전기 ->> 사용자: 승인 거절
    end

    %% 3. 시작 단계 (StartTransaction)
    충전기 ->> OCPP: StartTransaction.req (idTag, connectorId, meterStart)
    Note over OCPP: CsmsCert 모의 객체 생성 및 검증 통과 (선인증 완료 상태)
    OCPP ->> DB: 신규 거래 ID 채번
    DB -->> OCPP: 신규 trnId 반환
    OCPP ->> DB: 충전 시작 정보 저장 (tbStartTrMapper.saveTbStartTr)
    OCPP -->> 충전기: StartTransaction.conf (status = Accepted, trnId)
    충전기 ->> 사용자: 충전 시작

    %% 4. 종료 단계 (StopTransaction)
    충전기 ->> OCPP: StopTransaction.req (transactionId, meterStop, transactionData)
    OCPP ->> DB: 충전 시작 정보 조회 (tbStartTrMapper.selectTbStartTr)
    DB -->> OCPP: startTr 반환
    OCPP ->> DB: 환경부 카드 기관 ID 조회 (selectKmeCardInstId)
    DB -->> OCPP: 기관 ID (bid) 반환
    OCPP ->> DB: 충전 이력 저장 (operRechgHistMapper.saveOperRechgHist)
    
    Note over OCPP: UserType == UserType.K 확인 후<br/>환경부 충전 이력 적재 API 호출 (KmeRechgHist)
    OCPP ->> IFS: 환경부 충전 데이터 전송 API 호출
    IFS -->> OCPP: 전송 완료 응답
    
    OCPP -->> 충전기: StopTransaction.conf (status = Accepted)
    충전기 ->> 사용자: 충전 종료
```

---
[^OCPP]: **OCPP (Open Charge Point Protocol):** 개방형 충전 통신 규격
[^RFID]: **RFID (Radio Frequency Identification):** 무선 주파수 식별 (충전 회원 카드 등)
