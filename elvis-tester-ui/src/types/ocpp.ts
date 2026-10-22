export const MESSAGE_TYPE = {
  CALL: 2,
  CALLRESULT: 3,
  CALLERROR: 4
} as const

export interface OcppLogEntry {
  id: string
  timestamp: string
  direction: 'SEND' | 'RECV' | 'SYS' | 'ERR' | 'OUT' | 'IN'
  messageType: number
  messageId: string
  action?: string
  payload: any
  raw: string
  latencyMs?: number
  chargerId?: string
}

export type OcppChargePointStatus =
  | 'Available'
  | 'Preparing'
  | 'Charging'
  | 'SuspendedEVSE'
  | 'SuspendedEV'
  | 'Finishing'
  | 'Reserved'
  | 'Unavailable'
  | 'Faulted'

export interface ConnectorState {
  connectorId: number
  status: OcppChargePointStatus
  errorCode?: string
  transactionId?: number | null
  type?: string           // 커넥터 타입 (예: DC콤보1, Chademo, AC완속)
  maxPowerKw?: number     // 커넥터 최대 전력 (kW)
  voltageV?: number       // 정격 전압 (V)
  currentA?: number       // 정격 전류 (A)
  socPercent?: number     // 실시간 SoC (%)
  batteryTempC?: number   // 배터리 온도 (°C)
  carModel?: string       // 연결 차량 모델
  userTag?: string        // 사용자 RFID 태그
  accumulatedKwh?: number // 누적 전력량 (kWh)
  chargingMinutes?: number// 충전 시간(분)
}

export interface ChargerInstance {
  id: string              // 8자리 결합 식별자 (예: 00000001)
  name: string            // 충전기 명칭 (예: 판교 R&D 센터 1호기)
  stationId: string       // 6자리 충전소 ID (예: 000000)
  chargerId: string       // 2자리 충전기 ID (예: 01)
  stationName?: string    // 충전소명 (예: 판교 LS R&D 센터)
  corpName?: string       // 소속 법인/운영사 (예: LS E-Link)
  addr?: string           // 설치 주소
  spec?: string           // 정격 규격 (예: 350kW 초급속 듀얼)
  protocol?: string       // 프로토콜 (기본: OCPP 1.6-J)
  firmwareVer?: string    // 펌웨어 버전 (예: 1.0.0-RELEASE)
  useYn?: string          // 사용 여부 (Y/N)
  wsUrl: string           // WebSocket 엔드포인트 URL
  vendor: string          // 제조사 (예: LS E-Link)
  model: string           // 모델명 (예: ELVIS-HPC-350K)
  powerKw: number         // 용량 (kW)
  status: OcppChargePointStatus
  connected: boolean
  connecting: boolean
  lastError?: string
  connectors: ConnectorState[]
  activeConnectorId: number
  transactionId: number | null
  meterValueWh: number
  soc: number
  voltage: number
  current: number
  heartbeatIntervalSec: number
  autoHeartbeat: boolean
  autoBootOnConnect: boolean
  autoReply: boolean
  autoMeterValues: boolean
  autoMeterIntervalSec: number
  scenarioRunning: boolean
  scenarioStep: string
  scenarioProgress: number
}

export interface VirtualChargerState {
  chargeBoxId: string
  wsUrl: string
  connected: boolean
  connecting: boolean
  status: string
  connectorId: number
  transactionId: number | null
  meterValueWh: number
  soc: number
  voltage: number
  current: number
  powerKw: number
  heartbeatIntervalSec: number
  autoHeartbeat: boolean
  autoBootOnConnect: boolean
  autoReply: boolean
  autoMeterValues: boolean
  autoMeterIntervalSec: number
  scenarioRunning: boolean
  scenarioStep: string
  scenarioProgress: number
}

