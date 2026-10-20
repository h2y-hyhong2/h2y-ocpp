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
}

export interface ChargerInstance {
  id: string              // 8자리 결합 식별자 (예: 00000001)
  name: string            // 충전기 명칭 (예: 강남 테헤란 1호기 - 350kW 초급속)
  stationId: string       // 6자리 충전소 ID (예: 000000)
  chargerId: string       // 2자리 충전기 ID (예: 01)
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

