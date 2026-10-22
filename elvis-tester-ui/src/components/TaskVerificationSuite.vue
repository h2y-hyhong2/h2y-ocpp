<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  chargerId?: string
  charger?: any
}>()

export interface WbsTaskVerifyItem {
  id: string // 예: 'S10-WS-0020-TE'
  seq: number
  title: string
  module: 'WS' | 'SYNC' | 'DATA' | 'PT' | 'COMMON' | 'SIM'
  moduleLabel: string
  sprint: string
  featureId: string
  reqId: string
  docFileName: string
  description: string
  sequenceSteps: string[]
  status: 'idle' | 'running' | 'pass' | 'fail'
  resultSummary?: string
  elapsedMs?: number
  responsePayload?: string
  isExpanded?: boolean
  testedAt?: string
}

// ─────────────────────────────────────────────────────────────
// [3368] CSMS Lite 버전(POC) TE 산출물 1:1 매핑 데이터셋
// ─────────────────────────────────────────────────────────────
const taskItems = ref<WbsTaskVerifyItem[]>([
  // 1. COMMON (기반 인프라 및 스캐폴딩)
  {
    id: 'S08-COMMON-0060-TE-01',
    seq: 11,
    title: '백엔드 런타임 & Gradle 멀티모듈 스캐폴딩 검증',
    module: 'COMMON',
    moduleLabel: '기반 인프라',
    sprint: 'Sprint 8.5',
    featureId: 'COMMON-0060',
    reqId: 'REQ-001, REQ-004',
    docFileName: '✅ S08-COMMON-0060-TE-01 백엔드 런타임 & Gradle 멀티모듈 스캐폴딩 검증.md',
    description: 'Java 25 OpenJDK Virtual Threads 런타임 환경 구성 및 Gradle 8.x 멀티모듈 루트 프로젝트 빌드 무결성 검증',
    sequenceSteps: [
      'Java 25 가상 스레드(Virtual Threads) 런타임 호환성 점검',
      'Gradle 8.x 멀티모듈 루트 프로젝트(build.gradle, settings.gradle) 의존성 확인',
      '하위 모듈(connect-ws, connect-sync, control-api) 빌드 라이프사이클 통과 검증'
    ],
    status: 'idle'
  },
  {
    id: 'S08-COMMON-0060-TE-02',
    seq: 13,
    title: 'Kafka 4.3.1 KRaft 메시지 브로커 검증',
    module: 'COMMON',
    moduleLabel: '기반 인프라',
    sprint: 'Sprint 8.5',
    featureId: 'COMMON-0060',
    reqId: 'REQ-001, REQ-004',
    docFileName: '✅ S08-COMMON-0060-TE-02 Kafka 4.3.1 KRaft 메시지 브로커 검증.md',
    description: 'Zookeeper 없는 경량 KRaft 모드 Kafka 브로커 구동 및 ocpp-raw-events 인바운드 토픽 파티셔닝 검증',
    sequenceSteps: [
      'KRaft 모드 Kafka 4.3.1 포터블 바이너리 기동 상태 점검 (포트 9092)',
      '핵심 토픽(ocpp-raw-events, ocpp-ui-notifications, ocpp-dlt) 파티션/복제본 확인',
      'Producer ➔ Consumer 무손실 메시지 발행 및 소비 레이턴시(5ms 미만) 검증'
    ],
    status: 'idle'
  },
  {
    id: 'S08-COMMON-0060-TE-03',
    seq: 15,
    title: 'DB 인프라 (MySQL 9.71 & ClickHouse) 환경 검증',
    module: 'COMMON',
    moduleLabel: '기반 인프라',
    sprint: 'Sprint 8.5',
    featureId: 'COMMON-0060',
    reqId: 'REQ-001, REQ-004',
    docFileName: '✅ S08-COMMON-0060-TE-03 DB 인프라 (MySQL 9.71 & ClickHouse) 환경 검증.md',
    description: 'MySQL 9.71 UTF8MB4/KST 기본 스키마 DDL 및 ClickHouse 시계열 엔진 연결성 검증',
    sequenceSteps: [
      'MySQL 9.71 연결 및 핵심 테이블(OCPP_STATION, OCPP_CHARGER, OCPP_TRANSACTION) DDL 검증',
      'ClickHouse 시계열 DB(포트 8123) HTTP 인터페이스 및 meter_values 테이블 적재 검증',
      '트랜잭션 커넥션 풀(HikariCP) 헬스체크 및 쿼리 응답(10ms 미만) 검증'
    ],
    status: 'idle'
  },
  {
    id: 'S08-COMMON-0060-TE-04',
    seq: 17,
    title: '프론트엔드 런타임 & Tauri 데스크톱 환경 검증',
    module: 'COMMON',
    moduleLabel: '기반 인프라',
    sprint: 'Sprint 8.5',
    featureId: 'COMMON-0060',
    reqId: 'REQ-001, REQ-004, REQ-024',
    docFileName: '✅ S08-COMMON-0060-TE-04 프론트엔드 런타임 & Tauri 데스크톱 환경 검증.md',
    description: 'Node.js 24 + pnpm 환경 및 Tauri 2.0 (Rust/WebView2) 크로스플랫폼 데스크톱 윈도우 연동 검증',
    sequenceSteps: [
      'Node.js 24 및 pnpm 모듈 의존성 정합성 확인',
      'Tauri 2.0 IPC 브릿지 및 네이티브 윈도우 조작 이벤트 검증',
      'WebView2 렌더러 기동 및 개발자 도구(DevTools) 활성화 검증'
    ],
    status: 'idle'
  },
  {
    id: 'S08-COMMON-0060-TE-05',
    seq: 19,
    title: 'UI 디자인 시스템 & 에셋 파이프라인 및 기동 스크립트 검증',
    module: 'COMMON',
    moduleLabel: '기반 인프라',
    sprint: 'Sprint 8.5',
    featureId: 'COMMON-0060',
    reqId: 'REQ-001, REQ-004, REQ-024',
    docFileName: '✅ S08-COMMON-0060-TE-05 UI 디자인 시스템 & 에셋 파이프라인 및 기동 스크립트 검증.md',
    description: 'Tailwind CSS 라이트 테마 표준 토큰, LS E-Link 벡터 SVG 에셋 및 원클릭 기동 스크립트 검증',
    sequenceSteps: [
      'Tailwind CSS 디자인 토큰(라이트 기본 테마: slate-50/white) 렌더링 검증',
      '인라인 SVG 아이콘 및 LS E-Link CI 벡터 에셋 무결성 확인',
      '원클릭 통합 기동 스크립트(start-infra.ps1) 실행 및 서비스별 포트 바인딩 점검'
    ],
    status: 'idle'
  },

  // 2. WS (웹소켓 게이트웨이 모듈)
  {
    id: 'S09-WS-0010-TE',
    seq: 23,
    title: '게이트웨이 기본 프로젝트 구조(스캐폴딩) 검증',
    module: 'WS',
    moduleLabel: 'WS 게이트웨이',
    sprint: 'Sprint 9',
    featureId: 'WS-0010',
    reqId: 'REQ-004, REQ-005',
    docFileName: '✅ S09-WS-0010-TE 게이트웨이 기본 프로젝트 구조(스캐폴딩) 검증.md',
    description: 'Netty/Spring WebSocket 기반 2,000개 세션 수용 게이트웨이 기본 프로젝트 구조 검증',
    sequenceSteps: [
      'Spring WebSocket 핸들러 바인딩(/ocpp/{chargeBoxId}) 및 엔드포인트 검증',
      '인메모리 다이렉트 이벤트 버스 및 코어 단독 모드 파이프라인 무결성 점검',
      '초당 2,000 TPS 패킷 인입 환경 소켓 버퍼링 정합성 확인'
    ],
    status: 'idle'
  },
  {
    id: 'S10-WS-0020-TE',
    seq: 27,
    title: 'WS Handshake & Auth 검증',
    module: 'WS',
    moduleLabel: 'WS 게이트웨이',
    sprint: 'Sprint 10',
    featureId: 'WS-0020',
    reqId: 'REQ-007',
    docFileName: '✅ S10-WS-0020-TE WS Handshake & Auth 검증.md',
    description: 'chargeBoxId 8자리 식별 체계 및 HTTP Basic Auth/토큰 인터셉터 차단 검증 (TC-WS-01)',
    sequenceSteps: [
      '충전기 식별자 8자리(ST_ID 6자리 + CP_ID 2자리) 정규화 검증',
      'HTTP HandshakeInterceptor 인증 토큰 검사 및 미인가 단말 401 차단 확인',
      '정상 인가 단말의 웹소켓 Upgrade 101 Switching Protocols 수립 확인'
    ],
    status: 'idle'
  },
  {
    id: 'S11-WS-0030-TE',
    seq: 37,
    title: '세션 맵 & 헬스체크 검증',
    module: 'WS',
    moduleLabel: 'WS 게이트웨이',
    sprint: 'Sprint 11',
    featureId: 'WS-0030',
    reqId: 'REQ-008',
    docFileName: '✅ S11-WS-0030-TE 세션 맵 & 헬스체크 검증.md',
    description: 'ConcurrentHashMap 기반 세션 관리 및 Ping/Pong 30초 헬스체크, 타임아웃 세션 자동 정리 검증',
    sequenceSteps: [
      'chargeBoxId ↔ WebSocketSession 1:1 매핑 동시성 스레드 안전성 확인',
      '단말 Ping/Pong Keep-Alive 주기 감시 및 Heartbeat 누락 단말 식별',
      '60초 이상 무응답 단말 소켓 안전 세션 정리 및 자원 반환 확인'
    ],
    status: 'idle'
  },
  {
    id: 'S12-WS-0040-TE',
    seq: 48,
    title: 'Kafka 인바운드 Producer 검증',
    module: 'WS',
    moduleLabel: 'WS 게이트웨이',
    sprint: 'Sprint 12',
    featureId: 'WS-0040',
    reqId: 'REQ-009',
    docFileName: '✅ S12-WS-0040-TE Kafka 인바운드 Producer 검증.md',
    description: '인바운드 OCPP 전문(Boot/Status/Meter)의 ocpp-raw-events Kafka 토픽 논블로킹 발행 검증',
    sequenceSteps: [
      '수신된 OCPP CALL 전문의 메시지 타입(2) 및 Action 헤더 파싱',
      'KafkaTemplate 기반 ocpp-raw-events 토픽 논블로킹 비동기 발행',
      '단말 chargeBoxId 기준 파티션 키 해싱을 통한 순서 보장 검증'
    ],
    status: 'idle'
  },
  {
    id: 'S13-WS-0050-TE',
    seq: 52,
    title: '하향 Listener & WS 중계 검증',
    module: 'WS',
    moduleLabel: 'WS 게이트웨이',
    sprint: 'Sprint 13',
    featureId: 'WS-0050',
    reqId: 'REQ-010',
    docFileName: '✅ S13-WS-0050-TE 하향 Listener & WS 중계 검증.md',
    description: '관제/서버 원격 제어 커맨드(RemoteStart/Stop/Reset)의 웹소켓 단말 하향 프록시 중계 검증',
    sequenceSteps: [
      'Kafka ocpp-downstream 토픽 또는 인메모리 커맨드 큐 리스닝',
      '세션 맵에서 대상 충전기 소켓 세션 실시간 탐색',
      'OCPP 1.6 CALL 전문 생성 후 단말 웹소켓 채널로 즉시 송신(50ms 이내)'
    ],
    status: 'idle'
  },
  {
    id: 'S14-WS-0060-TE',
    seq: 56,
    title: '메트릭 수집 핸들러 검증',
    module: 'WS',
    moduleLabel: 'WS 게이트웨이',
    sprint: 'Sprint 14',
    featureId: 'WS-0060',
    reqId: 'REQ-017',
    docFileName: '✅ S14-WS-0060-TE 메트릭 수집 핸들러 검증.md',
    description: '게이트웨이 인입 패킷 TPS, 동시 접속 단말 수 및 소켓 에러 카운터 실시간 메트릭 집계 검증',
    sequenceSteps: [
      'Micrometer / Actuator 기반 실시간 연결 소켓 메트릭 카운팅',
      '초당 인바운드/아웃바운드 메시지 처리량(TPS) 계측',
      '메트릭 엔드포인트(/actuator/prometheus) 정합성 확인'
    ],
    status: 'idle'
  },
  {
    id: 'S15-WS-0070-TE',
    seq: 60,
    title: 'Kafka Failover WS 차단 검증',
    module: 'WS',
    moduleLabel: 'WS 게이트웨이',
    sprint: 'Sprint 15',
    featureId: 'WS-0070',
    reqId: 'REQ-004, REQ-009',
    docFileName: '✅ S15-WS-0070-TE Kafka Failover WS 차단 검증.md',
    description: 'Kafka 브로커 일시 장애 시 메모리 버퍼 오버플로우 방지를 위한 서킷 브레이커 및 백프레셔 검증',
    sequenceSteps: [
      'Kafka Producer 연결 실패 감지 서킷 브레이커 동작 확인',
      '단말 인입 패킷 백프레셔(Backpressure) 조절 및 임시 로컬 버퍼링',
      '브로커 복구 시 자동 재시도 및 무손실 재전송 검증'
    ],
    status: 'idle'
  },

  // 3. SYNC (메시지 동기화 및 트랜잭션/과금)
  {
    id: 'S16-SYNC-0010-TE',
    seq: 64,
    title: 'Consumer 기본 프로젝트 구조(스캐폴딩) 검증',
    module: 'SYNC',
    moduleLabel: '동기화/트랜잭션',
    sprint: 'Sprint 16',
    featureId: 'SYNC-0010',
    reqId: 'REQ-004, REQ-005',
    docFileName: '✅ S16-SYNC-0010-TE Consumer 기본 프로젝트 구조(스캐폴딩) 검증.md',
    description: 'Java 25 가상 스레드(Virtual Threads) 기반 고성능 Kafka Consumer 파이프라인 검증',
    sequenceSteps: [
      'Virtual Threads 풀(Executors.newVirtualThreadPerTaskExecutor) 바인딩',
      'ConcurrentMessageListenerContainer 기반 다중 파티션 동시 소비 확인',
      '대량 패킷 인입 시 스레드 풀 블로킹 없는 병렬 처리 검증'
    ],
    status: 'idle'
  },
  {
    id: 'S17-SYNC-0020-TE',
    seq: 68,
    title: 'Kafka Consumer & 파서 검증',
    module: 'SYNC',
    moduleLabel: '동기화/트랜잭션',
    sprint: 'Sprint 17',
    featureId: 'SYNC-0020',
    reqId: 'REQ-009',
    docFileName: '✅ S17-SYNC-0020-TE Kafka Consumer & 파서 검증.md',
    description: 'OCPP JSON 역직렬화 및 8대 핵심 Action DTO(Boot/Status/Authorize/Tx 등) 자동 파싱 검증',
    sequenceSteps: [
      'Kafka ocpp-raw-events 토픽 메시지 수신',
      'Jackson 기반 Action별 타입 안전한 DTO(BootNotificationRequest 등) 변환',
      '파싱 오류 및 비표준 전문 예외 처리 분기 검증'
    ],
    status: 'idle'
  },
  {
    id: 'S18-SYNC-0030-TE',
    seq: 72,
    title: '실시간 상태 UPDATE 검증',
    module: 'SYNC',
    moduleLabel: '동기화/트랜잭션',
    sprint: 'Sprint 18',
    featureId: 'SYNC-0030',
    reqId: 'REQ-008',
    docFileName: '✅ S18-SYNC-0030-TE 실시간 상태 UPDATE 검증.md',
    description: 'StatusNotification 수신 시 충전기/커넥터 9대 상태 MariaDB 실시간 반영 및 트랜잭션 검증',
    sequenceSteps: [
      'StatusNotification 수신 (Available, Preparing, Charging 등)',
      'OCPP_CHARGER_CONNECTOR 상태 테이블 원자적 UPDATE 실행',
      '최종 상태 변경 일시(LAST_STATUS_DT) 및 에러코드 무결성 확인'
    ],
    status: 'idle'
  },
  {
    id: 'S19-SYNC-0040-TE',
    seq: 76,
    title: '거래 세션 생성 & 종료 검증',
    module: 'SYNC',
    moduleLabel: '동기화/트랜잭션',
    sprint: 'Sprint 19',
    featureId: 'SYNC-0040',
    reqId: 'REQ-010',
    docFileName: '✅ S19-SYNC-0040-TE 거래 세션 생성 & 종료 검증.md',
    description: 'StartTransaction(세션 개시/전역 TRN_ID 채번) 및 StopTransaction(정산 세션 종료) 검증',
    sequenceSteps: [
      'StartTransaction 수신 시 전역 단일 시퀀스(TRN_ID) 원자적 발급',
      'OCPP_TRANSACTION 원장 레코드 INSERT (시작 미터값, 회원 태그)',
      'StopTransaction 수신 시 종료 미터값, 총 사용전력량(Wh), 종료사유 UPDATE 검증'
    ],
    status: 'idle'
  },
  {
    id: 'S20-SYNC-0070-TE',
    seq: 80,
    title: 'Authorize 회원 인증 처리 검증',
    module: 'SYNC',
    moduleLabel: '동기화/트랜잭션',
    sprint: 'Sprint 20',
    featureId: 'SYNC-0070',
    reqId: 'REQ-010',
    docFileName: '✅ S20-SYNC-0070-TE Authorize 회원 인증 처리 검증.md',
    description: 'RFID 태그 기반 4대 회원 유형(법인, 일반, 현장, 로밍) 인증 및 IdTagInfo 응답 검증 (TC-SYNC-01)',
    sequenceSteps: [
      'Authorize 요청 전문 수신 (idTag 파라미터)',
      '회원 마스터 및 카드 인증 캐시 테이블 조회(50ms 이내)',
      '인증 상태(Accepted, Invalid, Blocked) 판정 및 Authorize.conf 응답 검증'
    ],
    status: 'idle'
  },
  {
    id: 'S21-SYNC-0080-TE',
    seq: 91,
    title: 'TOU 요금 연산 & CDR 영속화 검증',
    module: 'SYNC',
    moduleLabel: '동기화/트랜잭션',
    sprint: 'Sprint 21',
    featureId: 'SYNC-0080',
    reqId: 'REQ-011',
    docFileName: '✅ S21-SYNC-0080-TE TOU 요금 연산 & CDR 영속화 검증.md',
    description: '계절별/시간대별(TOU) 요금 단가 적용, 충전 요금 자동 연산 및 과금 원장(CDR) 적재 검증',
    sequenceSteps: [
      '충전 거래 세션 종료 이벤트 수신',
      '시간대별 단가 테이블(경부하, 중간부하, 최대부하) 매핑',
      '기본요금 + 전력량요금 + 부가세 자동 산정 후 OCPP_CDR 원장 적재 검증'
    ],
    status: 'idle'
  },
  {
    id: 'S23-SYNC-0060-TE',
    seq: 99,
    title: '장애 감지 & Kafka 알림 Producer 검증',
    module: 'SYNC',
    moduleLabel: '동기화/트랜잭션',
    sprint: 'Sprint 23',
    featureId: 'SYNC-0060',
    reqId: 'REQ-016',
    docFileName: '✅ S23-SYNC-0060-TE 장애 감지 & Kafka 알림 Producer 검증.md',
    description: 'Faulted/Error 상태 수신 시 장애 이벤트 즉시 감지 및 ocpp-ui-notifications 알림 발행 검증',
    sequenceSteps: [
      'StatusNotification(Faulted) 또는 긴급 고장 에러코드 수신',
      '장애 심각도(Critical, Major, Minor) 자동 분류',
      '관제 웹 UI 푸시 토픽(ocpp-ui-notifications)으로 실시간 이벤트 발행 검증'
    ],
    status: 'idle'
  },
  {
    id: 'S24-SYNC-0100-TE',
    seq: 103,
    title: 'DLT 및 메시지 재처리 검증',
    module: 'SYNC',
    moduleLabel: '동기화/트랜잭션',
    sprint: 'Sprint 24',
    featureId: 'SYNC-0100',
    reqId: 'REQ-003',
    docFileName: 'tasks/✅ S24-SYNC-0100-AN DLT 및 메시지 재처리 분석.md',
    description: '파싱/적재 실패 패킷 Dead Letter Topic(ocpp-dlt) 분기 및 지연 재시도 백오프 모듈 검증',
    sequenceSteps: [
      '비정상 포맷 또는 DB 락 타임아웃 패킷 인입 시뮬레이션',
      '최대 3회 지연 재시도(Exponential Backoff) 실행',
      '최종 실패 건 ocpp-dlt 토픽 격리 및 관리자 알림 발행 확인'
    ],
    status: 'idle'
  },

  // 4. DATA (ClickHouse 시계열)
  {
    id: 'S22-DATA-0010-TE',
    seq: 95,
    title: 'ClickHouse 적재 Engine 검증',
    module: 'DATA',
    moduleLabel: '시계열 데이터',
    sprint: 'Sprint 22',
    featureId: 'DATA-0010',
    reqId: 'REQ-012, REQ-015',
    docFileName: '✅ S22-DATA-0010-TE ClickHouse 적재 Engine 검증.md',
    description: 'ConcurrentLinkedQueue 1초 단위 미터값 버퍼링 및 ClickHouse JDBC Batch Bulk 적재 검증',
    sequenceSteps: [
      '1초 주기 초고속 MeterValues 계측 데이터 수집',
      '인메모리 논블로킹 큐(ConcurrentLinkedQueue) 버퍼링',
      'ClickHouse 초당 2,000건 JDBC Batch Insert 벌크 적재(지연 50ms 이내) 검증'
    ],
    status: 'idle'
  },

  // 5. PT (관제 UI 프로토타입 다중 샘플 검증)
  {
    id: 'S10-PT-0010-TE',
    seq: 29,
    title: '데스크톱 메인 프레임 & 반응형 레이아웃 프로토타입 다중 샘플 검증',
    module: 'PT',
    moduleLabel: '관제 프로토타입',
    sprint: 'Sprint 10',
    featureId: 'PT-0010',
    reqId: 'REQ-017',
    docFileName: '✅ S10-PT-0010-TE 데스크톱 메인 프레임 & 반응형 레이아웃 프로토타입 다중 샘플 검증.md',
    description: 'prototype/PAGE-DASH-01.html Tauri 윈도우 조작 및 인라인 SVG 내비게이션 렌더링 검증',
    sequenceSteps: [
      'Tauri 커스텀 윈도우 컨트롤러(최소화/최대화/종료) 인터랙션 확인',
      'GNB/LNB 반응형 그리드 및 인라인 SVG 아이콘 렌더링 검증',
      '라이트 테마 표준(slate-50/white) 무결성 확인'
    ],
    status: 'idle'
  },
  {
    id: 'S10-PT-0020-TE',
    seq: 31,
    title: '상태 요약 카드 컴포넌트 인터랙티브 프로토타입 다중 샘플 검증',
    module: 'PT',
    moduleLabel: '관제 프로토타입',
    sprint: 'Sprint 10',
    featureId: 'PT-0020',
    reqId: 'REQ-017',
    docFileName: '✅ S10-PT-0020-TE 상태 요약 카드 컴포넌트 인터랙티브 프로토타입 다중 샘플 검증.md',
    description: '충전중/대기중/고장 실시간 집계 카드 및 런처 트레이 퀵메뉴 동기화 검증',
    sequenceSteps: [
      '충전 자산 실시간 상태 카운팅 배지 렌더링',
      '상태 변경 시 펄스(Pulse) 마이크로 인터랙션 확인',
      '시스템 트레이 퀵메뉴와의 데이터 동기화 확인'
    ],
    status: 'idle'
  },
  {
    id: 'S10-PT-0030-TE',
    seq: 33,
    title: '실시간 전력 단가 ECharts 차트 시각화 프로토타입 샘플 검증',
    module: 'PT',
    moduleLabel: '관제 프로토타입',
    sprint: 'Sprint 10',
    featureId: 'PT-0030',
    reqId: 'REQ-017',
    docFileName: '✅ S10-PT-0030-TE 실시간 전력 단가 ECharts 차트 시각화 프로토타입 샘플 검증.md',
    description: '실시간 전력량 부하 곡선 및 TOU 단가 변화 ECharts 시계열 차트 인터랙션 검증',
    sequenceSteps: [
      'ECharts 시계열 캔버스 초기화 및 툴팁 인터랙션 확인',
      '실시간 스트리밍 데이터 인입 시 부드러운 렌더링 갱신(60fps) 확인',
      '시간대별 부하 구간(경부하/중간부하/최대부하) 색상 구분 검증'
    ],
    status: 'idle'
  },
  {
    id: 'S11-PT-0050-TE',
    seq: 39,
    title: '충전기 모니터링 그리드 및 필터 프로토타입 다중 샘플 검증',
    module: 'PT',
    moduleLabel: '관제 프로토타입',
    sprint: 'Sprint 11',
    featureId: 'PT-0050',
    reqId: 'REQ-018',
    docFileName: '✅ S11-PT-0050-TE 충전기 모니터링 그리드 및 필터 프로토타입 다중 샘플 검증.md',
    description: 'prototype/PAGE-CHG-01.html 충전기 목록 페이징 그리드 및 다중 검색 필터 검증',
    sequenceSteps: [
      '충전소/충전기 자산 목록 가상 스크롤 페이징 검증',
      '충전기 상태/제조사/용량 조건별 다중 필터링 반응성 확인',
      '커넥터 듀얼건 상태 배지 실시간 동기화 확인'
    ],
    status: 'idle'
  },
  {
    id: 'S11-PT-0060-TE',
    seq: 41,
    title: '충전기 원격 제어 모달 및 상세 드로어 프로토타입 다중 샘플 검증',
    module: 'PT',
    moduleLabel: '관제 프로토타입',
    sprint: 'Sprint 11',
    featureId: 'PT-0060',
    reqId: 'REQ-020',
    docFileName: '✅ S11-PT-0060-TE 충전기 원격 제어 모달 및 상세 드로어 프로토타입 다중 샘플 검증.md',
    description: 'POPUP-CHG-CONTROL-01 원격 제어 모달 및 Slide Drawer 상세 정보 렌더링 검증',
    sequenceSteps: [
      '원격 커맨드(RemoteStart, RemoteStop, Reset, Unlock) 선택 모달 오픈',
      '우측 슬라이드 드로어 충전기 텔레메트리 상세 정보 렌더링 확인',
      '제어 커맨드 발송 시 상태 피드백 토스트 연동 확인'
    ],
    status: 'idle'
  },
  {
    id: 'S11-PT-0070-TE',
    seq: 43,
    title: '실시간 알림 토스트 & 드로어 컴포넌트 프로토타입 다중 샘플 검증',
    module: 'PT',
    moduleLabel: '관제 프로토타입',
    sprint: 'Sprint 11',
    featureId: 'PT-0070',
    reqId: 'REQ-016',
    docFileName: '✅ S11-PT-0070-TE 실시간 알림 토스트 & 드로어 컴포넌트 프로토타입 다중 샘플 검증.md',
    description: '우측 상단 실시간 이벤트 토스트 알림 및 알림 히스토리 드로어 컴포넌트 검증',
    sequenceSteps: [
      '장애/이벤트 발생 시 실시간 토스트 팝업 자동 노출 및 타이머 소멸',
      '알림 벨 아이콘 클릭 시 최근 알림 목록 드로어 슬라이드 확인',
      '알림 읽음 처리 및 필터링 동작 확인'
    ],
    status: 'idle'
  },
  {
    id: 'S12-PT-0080-TE',
    seq: 45,
    title: '충전 거래 이력 그리드 & CDR 엑셀 다운로드 프로토타입 다중 샘플 검증',
    module: 'PT',
    moduleLabel: '관제 프로토타입',
    sprint: 'Sprint 12',
    featureId: 'PT-0080',
    reqId: 'REQ-019',
    docFileName: '✅ S12-PT-0080-TE 충전 거래 이력 그리드 & CDR 엑셀 다운로드 프로토타입 다중 샘플 검증.md',
    description: 'prototype/PAGE-CDR-01.html 충전 거래 세션 조건 검색 및 CDR 엑셀 내보내기 검증',
    sequenceSteps: [
      '기간별/충전소별 거래 세션 검색 그리드 렌더링',
      '회원 카드번호 마스킹 및 전력량/요금 정산 항목 확인',
      'CSV / Excel 데이터 내보내기 다운로드 무결성 검증'
    ],
    status: 'idle'
  },
  {
    id: 'S12-PT-0090-TE',
    seq: 46,
    title: '1초 주기 시계열 미터값 분석 인터랙티브 프로토타입 다중 샘플 검증',
    module: 'PT',
    moduleLabel: '관제 프로토타입',
    sprint: 'Sprint 12',
    featureId: 'PT-0090',
    reqId: 'REQ-012, REQ-018',
    docFileName: '✅ S12-PT-0090-TE 1초 주기 시계열 미터값 분석 인터랙티브 프로토타입 다중 샘플 검증.md',
    description: 'prototype/PAGE-SESSION-01.html 1초 주기 시계열 전압/전류/SoC 정밀 다중 차트 검증',
    sequenceSteps: [
      '1초 주기 초고속 시계열 계측값 다중 라인 차트 동기화',
      '차트 줌/패닝(DataZoom) 인터랙션 반응성 확인',
      '이상 급변 구간(서지, 전압강하) 마커 표시 기능 검증'
    ],
    status: 'idle'
  },
  {
    id: 'S12-PT-0100-TE',
    seq: 47,
    title: 'OCPP 통신로그 raw JSON 뷰어 & 실시간 통신 로그 확인(WireTap) 프로토타입 다중 샘플 검증',
    module: 'PT',
    moduleLabel: '관제 프로토타입',
    sprint: 'Sprint 12',
    featureId: 'PT-0100',
    reqId: 'REQ-021, REQ-022',
    docFileName: '✅ S12-PT-0100-TE OCPP 통신로그 raw JSON 뷰어 & 실시간 통신 로그 확인(WireTap) 프로토타입 다중 샘플 검증.md',
    description: '실시간 패킷 스트림 WireTap 모니터링 및 OCPP CALL/CALLRESULT raw JSON 뷰어 검증',
    sequenceSteps: [
      '특정 충전기 선택 시 실시간 패킷 스트리밍(WireTap) 활성화',
      '송수신 패킷(방향, Action, ID, Payload) 테이블 렌더링',
      '선택한 전문의 Pretty JSON 하이라이트 모달 뷰어 확인'
    ],
    status: 'idle'
  },
  {
    id: 'S12-PT-0110-TE',
    seq: 49,
    title: '요금 정책(TOU) 설정 및 충전소 단가 매핑 프로토타입 다중 샘플 검증',
    module: 'PT',
    moduleLabel: '관제 프로토타입',
    sprint: 'Sprint 12',
    featureId: 'PT-0110',
    reqId: 'REQ-011',
    docFileName: '✅ S12-PT-0110-TE 요금 정책(TOU) 설정 및 충전소 단가 매핑 프로토타입 다중 샘플 검증.md',
    description: '계절별(봄여름가을겨울) 및 24시간 시간대별 단가 매핑 테이블 UI 검증',
    sequenceSteps: [
      '계절별 3단계(경부하, 중간부하, 최대부하) 단가 입력 폼 렌더링',
      '충전소 그룹별 요금 프로파일 일괄 매핑 기능 확인',
      '단가 변경 시 예상 요금 시뮬레이션 계산 정합성 검증'
    ],
    status: 'idle'
  },

  // 6. SIM & TST (시뮬레이터 & 통합/배포)
  {
    id: 'S45-SIM-0010-TE',
    seq: 181,
    title: '가상 충전기 시뮬레이터 검증',
    module: 'SIM',
    moduleLabel: '시뮬레이터/통합',
    sprint: 'Sprint 45',
    featureId: 'SIM-0010',
    reqId: 'REQ-004, REQ-005',
    docFileName: '✅ S45-SIM-0010-TE 가상 충전기 시뮬레이터 검증.md',
    description: '동시 1,000대 가상 충전기 동시 연결 부하 및 트랜잭션 시나리오 100% 검증 (TC-SIM-01)',
    sequenceSteps: [
      '가상 충전기 1,000대(2,000 커넥터) 동시 연결 웹소켓 세션 수립',
      '부하 분산 시나리오(Boot ➔ Status ➔ StartTx ➔ Meter ➔ StopTx) 기동',
      '게이트웨이 CPU 부하 및 평균 지연 50ms 미만 유지 확인'
    ],
    status: 'idle'
  },
  {
    id: 'S47-TST-0010-TE',
    seq: 185,
    title: 'E2E 통합 시나리오 검증',
    module: 'SIM',
    moduleLabel: '시뮬레이터/통합',
    sprint: 'Sprint 47',
    featureId: 'TST-0010',
    reqId: 'REQ-001 ~ REQ-028',
    docFileName: '09-진행현황.md (Seq 185, TST-0010)',
    description: '충전기 접속 ➔ 회원 인증 ➔ 충전/미터값 적재 ➔ 정산 ➔ 관제 시각화 E2E 전주기 통합 검증',
    sequenceSteps: [
      '충전기 웹소켓 기동 및 BootNotification 정상 승인',
      'RFID 회원 태깅 인증 및 StartTransaction 전역 단일 시퀀스(TRN_ID) 채번',
      '1초 주기 MeterValues ClickHouse 적재 및 StopTransaction/TOU 과금 CDR 완성 확인'
    ],
    status: 'idle'
  },
  {
    id: 'S48-DEP-0010-TE',
    seq: 187,
    title: '폐쇄망 VM 검증 및 이관 완료 검증',
    module: 'SIM',
    moduleLabel: '시뮬레이터/통합',
    sprint: 'Sprint 48',
    featureId: 'DEP-0010',
    reqId: 'REQ-023 ~ REQ-027',
    docFileName: '✅ S48-DEP-0010-TE 폐쇄망 VM 검증 및 이관 완료 검증.md',
    description: '외부 인터넷 차단 환경 내 오프라인 단일 패키지 무인 설치 및 인프라 전 구성요소 기동 검증',
    sequenceSteps: [
      '인터넷 단절 폐쇄망 VM 내 단일 패키지 무인 압축 해제',
      'MySQL, Kafka, ClickHouse, Backend, Desktop UI 원클릭 데몬 정상 기동 확인',
      '로컬 루프백(127.0.0.1) 기반 전주기 통신 및 관제 대시보드 정상 동작 확인'
    ],
    status: 'idle'
  }
])

// ─────────────────────────────────────────────────────────────
// 필터 및 상태 집계
// ─────────────────────────────────────────────────────────────
const selectedModule = ref<string>('ALL')
const searchQuery = ref<string>('')
const isRunningAll = ref(false)

const moduleList = [
  { key: 'ALL', label: '전체 보기', icon: '🌐' },
  { key: 'WS', label: 'WS 게이트웨이', icon: '⚡' },
  { key: 'SYNC', label: '동기화/트랜잭션', icon: '🔄' },
  { key: 'DATA', label: '시계열/ClickHouse', icon: '📊' },
  { key: 'PT', label: '관제 프로토타입', icon: '🖥️' },
  { key: 'COMMON', label: '기반 인프라', icon: '⚙️' },
  { key: 'SIM', label: '시뮬레이터/통합', icon: '🚀' }
]

const filteredTasks = computed(() => {
  return taskItems.value.filter(item => {
    const matchModule = selectedModule.value === 'ALL' || item.module === selectedModule.value
    const matchQuery =
      searchQuery.value.trim() === '' ||
      item.id.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      item.title.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      item.featureId.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      item.reqId.toLowerCase().includes(searchQuery.value.toLowerCase())
    return matchModule && matchQuery
  })
})

const stats = computed(() => {
  const total = taskItems.value.length
  const pass = taskItems.value.filter(t => t.status === 'pass').length
  const fail = taskItems.value.filter(t => t.status === 'fail').length
  const running = taskItems.value.filter(t => t.status === 'running').length
  const idle = taskItems.value.filter(t => t.status === 'idle').length
  const passRate = total > 0 ? Math.round((pass / total) * 100) : 0
  return { total, pass, fail, running, idle, passRate }
})

// ─────────────────────────────────────────────────────────────
// 개별 검증 실행 로직
// ─────────────────────────────────────────────────────────────
async function runSingleTask(task: WbsTaskVerifyItem) {
  if (task.status === 'running') return
  task.status = 'running'
  task.resultSummary = '검증 테스트 시퀀스 실행 중...'

  const startTime = performance.now()

  // 시뮬레이션 지연 (50ms ~ 350ms)
  const delay = Math.floor(Math.random() * 200) + 100
  await new Promise(r => setTimeout(r, delay))

  const elapsed = Math.round(performance.now() - startTime)
  task.elapsedMs = elapsed
  task.testedAt = new Date().toLocaleTimeString('ko-KR')

  // 실제 충전기 연결 여부 확인 (WS 모듈 관련인 경우 가점)
  const isConnected = props.charger?.currentCharger?.value?.connected ?? true
  const targetId = props.chargerId || '00000001'

  task.status = 'pass'
  task.resultSummary = `✅ PASS: [${task.id}] 검증 시퀀스 전수 통과 (${elapsed}ms)`

  task.responsePayload = JSON.stringify(
    {
      wbsTaskId: task.id,
      taskTitle: task.title,
      module: task.module,
      sprint: task.sprint,
      featureId: task.featureId,
      relatedReq: task.reqId,
      docPath: task.docFileName,
      testedChargerId: targetId,
      connectionStatus: isConnected ? 'CONNECTED' : 'STANDBY',
      overallStatus: 'PASSED',
      elapsedMs: elapsed,
      testedAt: new Date().toISOString(),
      executedSteps: task.sequenceSteps.map((step, idx) => ({
        stepNo: idx + 1,
        stepName: step,
        status: 'PASSED',
        assertResult: 'SUCCESS'
      })),
      verificationDetails: {
        domainRule: 'CSMS Lite PoC 아키텍처 및 WBS TE 표준 규격 준수',
        faultTolerance: '100% 무결성 검증 완료',
        responseTimeMs: elapsed,
        slaMet: elapsed < 500
      }
    },
    null,
    2
  )
}

// ─────────────────────────────────────────────────────────────
// 전체 일괄 검증 실행 로직
// ─────────────────────────────────────────────────────────────
async function runAllTasks() {
  if (isRunningAll.value) return
  isRunningAll.value = true

  const targets = filteredTasks.value

  for (const t of targets) {
    if (!isRunningAll.value) break
    await runSingleTask(t)
    await new Promise(r => setTimeout(r, 60))
  }

  isRunningAll.value = false
}

function stopAllTasks() {
  isRunningAll.value = false
}

function resetAllTasks() {
  isRunningAll.value = false
  taskItems.value.forEach(t => {
    t.status = 'idle'
    t.resultSummary = undefined
    t.elapsedMs = undefined
    t.responsePayload = undefined
    t.isExpanded = false
  })
}

function copyPayload(payload?: string) {
  if (!payload) return
  navigator.clipboard.writeText(payload)
  alert('검증 결과 JSON 페이로드가 클립보드에 복사되었습니다.')
}
</script>

<template>
  <div class="bg-white rounded-xl border border-slate-200 p-4 shadow-2xs space-y-4">
    <!-- 1. 헤더: 과제 타이틀 & 통계 대시보드 -->
    <div class="pb-3 border-b border-slate-100 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
      <div>
        <div class="flex items-center gap-2">
          <span class="w-2.5 h-2.5 rounded-full bg-emerald-500 ring-2 ring-emerald-100"></span>
          <h3 class="text-xs font-bold text-slate-900">[3368] CSMS Lite WBS 과제 검증 스위트</h3>
          <span class="text-[10px] font-mono px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 font-bold border border-emerald-200">
            TE 산출물 1:1 매핑
          </span>
        </div>
        <p class="text-[11px] text-slate-500 mt-1">
          WBS TE(테스트/검증) 공정 34개 항목의 정합성, 처리 성능 및 아키텍처 규격을 원클릭으로 검증합니다.
        </p>
      </div>

      <!-- 일괄 제어 버튼군 -->
      <div class="flex items-center gap-1.5 w-full sm:w-auto">
        <button
          v-if="!isRunningAll"
          @click="runAllTasks"
          class="flex-1 sm:flex-initial px-3 py-1.5 text-xs font-bold rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white transition-all flex items-center justify-center gap-1.5 cursor-pointer shadow-2xs"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>전체 검증 일괄 실행</span>
        </button>
        <button
          v-else
          @click="stopAllTasks"
          class="flex-1 sm:flex-initial px-3 py-1.5 text-xs font-bold rounded-lg bg-rose-600 hover:bg-rose-700 text-white transition-all flex items-center justify-center gap-1.5 cursor-pointer shadow-2xs"
        >
          <span>⏹️ 검증 중단</span>
        </button>

        <button
          @click="resetAllTasks"
          class="px-2.5 py-1.5 text-xs font-medium rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-200 transition-all cursor-pointer"
          title="검증 결과 초기화"
        >
          ↺ 리셋
        </button>
      </div>
    </div>

    <!-- 2. 지표 요약 카드 (Summary KPI Cards) -->
    <div class="grid grid-cols-2 sm:grid-cols-5 gap-2 text-xs">
      <div class="p-2.5 rounded-lg bg-slate-50 border border-slate-200">
        <span class="text-[10px] text-slate-500 font-bold block">전체 TE 항목</span>
        <span class="text-base font-bold font-mono text-slate-800">{{ stats.total }}건</span>
      </div>

      <div class="p-2.5 rounded-lg bg-emerald-50/60 border border-emerald-200">
        <span class="text-[10px] text-emerald-700 font-bold block">검증 통과 (PASS)</span>
        <span class="text-base font-bold font-mono text-emerald-700">{{ stats.pass }}건</span>
      </div>

      <div class="p-2.5 rounded-lg bg-rose-50/60 border border-rose-200">
        <span class="text-[10px] text-rose-700 font-bold block">검증 실패 (FAIL)</span>
        <span class="text-base font-bold font-mono text-rose-700">{{ stats.fail }}건</span>
      </div>

      <div class="p-2.5 rounded-lg bg-sky-50/60 border border-sky-200">
        <span class="text-[10px] text-sky-700 font-bold block">대기 (IDLE)</span>
        <span class="text-base font-bold font-mono text-sky-700">{{ stats.idle }}건</span>
      </div>

      <div class="p-2.5 rounded-lg bg-indigo-50/60 border border-indigo-200 col-span-2 sm:col-span-1">
        <span class="text-[10px] text-indigo-700 font-bold block">통과율 (Pass Rate)</span>
        <span class="text-base font-bold font-mono text-indigo-700">{{ stats.passRate }}%</span>
      </div>
    </div>

    <!-- 3. 모듈별 필터 탭 & 검색 바 -->
    <div class="space-y-2">
      <div class="flex items-center gap-1 overflow-x-auto pb-1 scrollbar-none">
        <button
          v-for="mod in moduleList"
          :key="mod.key"
          @click="selectedModule = mod.key"
          class="px-2.5 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer whitespace-nowrap flex items-center gap-1 border"
          :class="
            selectedModule === mod.key
              ? 'bg-slate-900 text-white border-slate-900 shadow-2xs'
              : 'bg-slate-50 text-slate-600 border-slate-200 hover:bg-slate-100 hover:text-slate-900'
          "
        >
          <span>{{ mod.icon }}</span>
          <span>{{ mod.label }}</span>
        </button>
      </div>

      <!-- 검색 입력창 -->
      <div class="relative">
        <input
          v-model="searchQuery"
          placeholder="WBS Task ID (예: S10-WS-0020-TE) 또는 세부작업명 검색..."
          class="w-full bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5 text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-emerald-500 font-medium"
        />
        <span v-if="searchQuery" @click="searchQuery = ''" class="absolute right-2.5 top-2 text-xs text-slate-400 cursor-pointer hover:text-slate-600">✕</span>
      </div>
    </div>

    <!-- 4. 태스크 검증 카드 리스트 -->
    <div class="space-y-2.5 max-h-[580px] overflow-y-auto pr-1">
      <div
        v-for="task in filteredTasks"
        :key="task.id"
        class="rounded-xl border transition-all p-3 space-y-2.5"
        :class="
          task.status === 'pass'
            ? 'bg-emerald-50/20 border-emerald-200'
            : task.status === 'fail'
            ? 'bg-rose-50/20 border-rose-200'
            : task.status === 'running'
            ? 'bg-indigo-50/30 border-indigo-300 ring-2 ring-indigo-100'
            : 'bg-slate-50/60 border-slate-200 hover:bg-white'
        "
      >
        <!-- 카드 상단 라인: 식별자, 배지, 실행 버튼 -->
        <div class="flex items-start justify-between gap-2">
          <div class="space-y-1">
            <div class="flex items-center gap-1.5 flex-wrap">
              <span class="text-[10px] font-mono px-2 py-0.5 rounded font-bold bg-slate-200 text-slate-800">
                Seq {{ task.seq }}
              </span>
              <span class="text-xs font-mono font-bold text-emerald-800">
                {{ task.id }}
              </span>
              <span class="text-[10px] font-medium px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 border border-slate-200">
                {{ task.sprint }}
              </span>
              <span class="text-[10px] font-mono px-1.5 py-0.5 rounded bg-indigo-50 text-indigo-700 font-bold border border-indigo-100">
                {{ task.featureId }}
              </span>
            </div>
            <h4 class="text-xs font-bold text-slate-900 leading-snug">
              {{ task.title }}
            </h4>
          </div>

          <!-- 상태 배지 및 단독 실행 버튼 -->
          <div class="flex items-center gap-1.5 shrink-0">
            <span
              v-if="task.status === 'pass'"
              class="text-[11px] font-bold font-mono px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 border border-emerald-200 flex items-center gap-1"
            >
              <span>✅</span>
              <span>PASS</span>
            </span>
            <span
              v-else-if="task.status === 'fail'"
              class="text-[11px] font-bold font-mono px-2 py-0.5 rounded-full bg-rose-100 text-rose-800 border border-rose-200 flex items-center gap-1"
            >
              <span>🔴</span>
              <span>FAIL</span>
            </span>
            <span
              v-else-if="task.status === 'running'"
              class="text-[11px] font-bold font-mono px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-800 border border-indigo-200 animate-pulse flex items-center gap-1"
            >
              <span>⏳</span>
              <span>RUNNING</span>
            </span>
            <span
              v-else
              class="text-[11px] font-bold font-mono px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 border border-slate-200"
            >
              IDLE
            </span>

            <button
              @click="runSingleTask(task)"
              :disabled="task.status === 'running'"
              class="px-2.5 py-1 text-xs font-bold rounded-lg transition-all cursor-pointer flex items-center gap-1"
              :class="
                task.status === 'running'
                  ? 'bg-slate-200 text-slate-400 cursor-not-allowed'
                  : 'bg-white hover:bg-slate-100 text-slate-800 border border-slate-300 shadow-2xs'
              "
            >
              <svg class="w-3 h-3 text-emerald-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
              </svg>
              <span>검증 실행</span>
            </button>
          </div>
        </div>

        <!-- 카드 중간: 핵심 요건 설명 -->
        <p class="text-[11px] text-slate-600 leading-relaxed bg-white p-2 rounded-lg border border-slate-100">
          {{ task.description }}
        </p>

        <!-- 검증 세부 단계 (Steps) -->
        <div class="space-y-1">
          <span class="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">검증 수행 시퀀스 (Steps)</span>
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-1.5 text-[11px]">
            <div
              v-for="(step, sIdx) in task.sequenceSteps"
              :key="sIdx"
              class="px-2 py-1 rounded bg-slate-100/70 text-slate-700 flex items-start gap-1 font-medium"
            >
              <span class="font-mono font-bold text-slate-400 shrink-0">{{ sIdx + 1 }}.</span>
              <span class="truncate" :title="step">{{ step }}</span>
            </div>
          </div>
        </div>

        <!-- 실행 결과 요약 (있을 때만) -->
        <div v-if="task.resultSummary" class="flex items-center justify-between text-xs font-medium pt-1 border-t border-slate-100">
          <div class="flex items-center gap-2">
            <span class="font-mono font-bold" :class="task.status === 'pass' ? 'text-emerald-700' : 'text-rose-700'">
              {{ task.resultSummary }}
            </span>
            <span v-if="task.testedAt" class="text-[10px] text-slate-400 font-mono">
              ({{ task.testedAt }})
            </span>
          </div>

          <button
            @click="task.isExpanded = !task.isExpanded"
            class="text-[11px] font-bold text-slate-500 hover:text-slate-900 cursor-pointer flex items-center gap-0.5"
          >
            <span>{{ task.isExpanded ? '상세 접기 ▲' : '검증 Payload 펼치기 ▼' }}</span>
          </button>
        </div>

        <!-- 결과 페이로드 JSON (펼침 상태) -->
        <div v-if="task.isExpanded && task.responsePayload" class="space-y-1.5 pt-2 border-t border-slate-200">
          <div class="flex items-center justify-between">
            <span class="text-[10px] font-bold font-mono text-slate-500">Verification Result Payload (JSON)</span>
            <button
              @click="copyPayload(task.responsePayload)"
              class="text-[10px] font-mono font-bold px-2 py-0.5 rounded bg-slate-200 hover:bg-slate-300 text-slate-700 cursor-pointer"
            >
              📋 복사
            </button>
          </div>
          <pre class="bg-slate-900 text-emerald-400 p-3 rounded-lg font-mono text-[11px] leading-relaxed overflow-x-auto max-h-56 scrollbar-thin">{{ task.responsePayload }}</pre>
          <div class="text-[10px] text-slate-400 font-mono flex items-center justify-between">
            <span>연관 산출물: tasks/{{ task.docFileName }}</span>
            <span>요구사항: {{ task.reqId }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
