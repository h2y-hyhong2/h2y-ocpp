export const MESSAGE_TYPE = {
  CALL: 2,
  CALLRESULT: 3,
  CALLERROR: 4
} as const

export interface OcppLogEntry {
  id: string
  timestamp: string
  direction: 'OUT' | 'IN' | 'ERR'
  messageType: number
  messageId: string
  action?: string
  payload: any
  raw: string
  latencyMs?: number
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
  autoMeterValues: boolean
  autoMeterIntervalSec: number
  scenarioRunning: boolean
  scenarioStep: string
  scenarioProgress: number
}

