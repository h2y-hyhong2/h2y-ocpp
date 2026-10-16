import { ref, reactive, computed } from 'vue'
import {
  MESSAGE_TYPE,
  type OcppLogEntry,
  type ChargerInstance,
  type OcppChargePointStatus,
  type VirtualChargerState
} from '../types/ocpp'

const STORAGE_KEY = 'elvis_tester_chargers_v1'

const DEFAULT_PRESETS: ChargerInstance[] = [
  {
    id: '00000001',
    name: '강남 테헤란 1호기 (350kW 초급속)',
    stationId: '000000',
    chargerId: '01',
    wsUrl: 'ws://127.0.0.1:18080/ocpp',
    vendor: 'LS E-Link',
    model: 'ELVIS-HPC-350K',
    powerKw: 350,
    status: 'Available',
    connected: false,
    connecting: false,
    connectors: [
      { connectorId: 1, status: 'Available' },
      { connectorId: 2, status: 'Available' }
    ],
    activeConnectorId: 1,
    transactionId: null,
    meterValueWh: 100000,
    soc: 25,
    voltage: 380.0,
    current: 125.0,
    autoHeartbeat: false,
    autoMeterValues: false,
    scenarioRunning: false,
    scenarioStep: '',
    scenarioProgress: 0
  },
  {
    id: '00000002',
    name: '강남 테헤란 2호기 (350kW 초급속)',
    stationId: '000000',
    chargerId: '02',
    wsUrl: 'ws://127.0.0.1:18080/ocpp',
    vendor: 'LS E-Link',
    model: 'ELVIS-HPC-350K',
    powerKw: 350,
    status: 'Available',
    connected: false,
    connecting: false,
    connectors: [
      { connectorId: 1, status: 'Available' },
      { connectorId: 2, status: 'Available' }
    ],
    activeConnectorId: 1,
    transactionId: null,
    meterValueWh: 102000,
    soc: 30,
    voltage: 380.0,
    current: 125.0,
    autoHeartbeat: false,
    autoMeterValues: false,
    scenarioRunning: false,
    scenarioStep: '',
    scenarioProgress: 0
  },
  {
    id: '00000003',
    name: '강남 테헤란 3호기 (200kW 급속)',
    stationId: '000000',
    chargerId: '03',
    wsUrl: 'ws://127.0.0.1:18080/ocpp',
    vendor: 'LS E-Link',
    model: 'ELVIS-QC-200K',
    powerKw: 200,
    status: 'Available',
    connected: false,
    connecting: false,
    connectors: [{ connectorId: 1, status: 'Available' }],
    activeConnectorId: 1,
    transactionId: null,
    meterValueWh: 80500,
    soc: 20,
    voltage: 380.0,
    current: 100.0,
    autoHeartbeat: false,
    autoMeterValues: false,
    scenarioRunning: false,
    scenarioStep: '',
    scenarioProgress: 0
  },
  {
    id: '00000004',
    name: '강남 테헤란 4호기 (200kW 급속)',
    stationId: '000000',
    chargerId: '04',
    wsUrl: 'ws://127.0.0.1:18080/ocpp',
    vendor: 'LS E-Link',
    model: 'ELVIS-QC-200K',
    powerKw: 200,
    status: 'Available',
    connected: false,
    connecting: false,
    connectors: [{ connectorId: 1, status: 'Available' }],
    activeConnectorId: 1,
    transactionId: null,
    meterValueWh: 95000,
    soc: 40,
    voltage: 380.0,
    current: 100.0,
    autoHeartbeat: false,
    autoMeterValues: false,
    scenarioRunning: false,
    scenarioStep: '',
    scenarioProgress: 0
  },
  {
    id: '00000005',
    name: '강남 테헤란 5호기 (100kW 급속)',
    stationId: '000000',
    chargerId: '05',
    wsUrl: 'ws://127.0.0.1:18080/ocpp',
    vendor: 'LS E-Link',
    model: 'ELVIS-QC-100K',
    powerKw: 100,
    status: 'Available',
    connected: false,
    connecting: false,
    connectors: [{ connectorId: 1, status: 'Available' }],
    activeConnectorId: 1,
    transactionId: null,
    meterValueWh: 60000,
    soc: 15,
    voltage: 380.0,
    current: 80.0,
    autoHeartbeat: false,
    autoMeterValues: false,
    scenarioRunning: false,
    scenarioStep: '',
    scenarioProgress: 0
  },
  {
    id: '00000006',
    name: '강남 테헤란 6호기 (100kW 급속)',
    stationId: '000000',
    chargerId: '06',
    wsUrl: 'ws://127.0.0.1:18080/ocpp',
    vendor: 'LS E-Link',
    model: 'ELVIS-QC-100K',
    powerKw: 100,
    status: 'Available',
    connected: false,
    connecting: false,
    connectors: [{ connectorId: 1, status: 'Available' }],
    activeConnectorId: 1,
    transactionId: null,
    meterValueWh: 72000,
    soc: 50,
    voltage: 380.0,
    current: 80.0,
    autoHeartbeat: false,
    autoMeterValues: false,
    scenarioRunning: false,
    scenarioStep: '',
    scenarioProgress: 0
  },
  {
    id: '00000007',
    name: '강남 테헤란 7호기 (100kW 급속)',
    stationId: '000000',
    chargerId: '07',
    wsUrl: 'ws://127.0.0.1:18080/ocpp',
    vendor: 'LS E-Link',
    model: 'ELVIS-QC-100K',
    powerKw: 100,
    status: 'Available',
    connected: false,
    connecting: false,
    connectors: [{ connectorId: 1, status: 'Available' }],
    activeConnectorId: 1,
    transactionId: null,
    meterValueWh: 54000,
    soc: 35,
    voltage: 380.0,
    current: 80.0,
    autoHeartbeat: false,
    autoMeterValues: false,
    scenarioRunning: false,
    scenarioStep: '',
    scenarioProgress: 0
  },
  {
    id: '00000008',
    name: '강남 테헤란 8호기 (50kW 중급속)',
    stationId: '000000',
    chargerId: '08',
    wsUrl: 'ws://127.0.0.1:18080/ocpp',
    vendor: 'LS E-Link',
    model: 'ELVIS-MC-50K',
    powerKw: 50,
    status: 'Available',
    connected: false,
    connecting: false,
    connectors: [{ connectorId: 1, status: 'Available' }],
    activeConnectorId: 1,
    transactionId: null,
    meterValueWh: 43000,
    soc: 60,
    voltage: 380.0,
    current: 40.0,
    autoHeartbeat: false,
    autoMeterValues: false,
    scenarioRunning: false,
    scenarioStep: '',
    scenarioProgress: 0
  },
  {
    id: '00000009',
    name: '강남 테헤란 9호기 (50kW 중급속)',
    stationId: '000000',
    chargerId: '09',
    wsUrl: 'ws://127.0.0.1:18080/ocpp',
    vendor: 'LS E-Link',
    model: 'ELVIS-MC-50K',
    powerKw: 50,
    status: 'Available',
    connected: false,
    connecting: false,
    connectors: [{ connectorId: 1, status: 'Available' }],
    activeConnectorId: 1,
    transactionId: null,
    meterValueWh: 38000,
    soc: 45,
    voltage: 380.0,
    current: 40.0,
    autoHeartbeat: false,
    autoMeterValues: false,
    scenarioRunning: false,
    scenarioStep: '',
    scenarioProgress: 0
  },
  {
    id: '00000010',
    name: '강남 테헤란 10호기 (50kW 중급속)',
    stationId: '000000',
    chargerId: '10',
    wsUrl: 'ws://127.0.0.1:18080/ocpp',
    vendor: 'LS E-Link',
    model: 'ELVIS-MC-50K',
    powerKw: 50,
    status: 'Available',
    connected: false,
    connecting: false,
    connectors: [{ connectorId: 1, status: 'Available' }],
    activeConnectorId: 1,
    transactionId: null,
    meterValueWh: 29000,
    soc: 70,
    voltage: 380.0,
    current: 40.0,
    autoHeartbeat: false,
    autoMeterValues: false,
    scenarioRunning: false,
    scenarioStep: '',
    scenarioProgress: 0
  }
]

// ─────────────────────────────────────────────────────────────
// 글로벌 싱글톤 상태 (전역 공유)
// ─────────────────────────────────────────────────────────────
function initChargers(): ChargerInstance[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed.map((item: any) => ({
          ...item,
          connected: false,
          connecting: false,
          scenarioRunning: false,
          scenarioStep: '',
          scenarioProgress: 0
        }))
      }
    }
  } catch (e) {
    console.warn('Failed to parse cached chargers, falling back to defaults:', e)
  }
  return JSON.parse(JSON.stringify(DEFAULT_PRESETS))
}

const chargers = reactive<ChargerInstance[]>(initChargers())
const selectedChargerId = ref<string>(chargers[0]?.id || '00000001')
const logs = ref<OcppLogEntry[]>([])
const terminalSearchQuery = ref<string>('')

// 활성 소켓 및 타이머 관리 맵
const socketMap = new Map<string, WebSocket>()
const heartbeatTimerMap = new Map<string, any>()
const autoMeterTimerMap = new Map<string, any>()
const abortFlagMap = new Map<string, boolean>()
const pendingRequests = new Map<string, { action: string; sentAt: number; chargerId: string }>()

function saveChargersToStorage() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(chargers))
  } catch (e) {
    console.warn('Failed to save chargers:', e)
  }
}

function generateMsgId(): string {
  return 'msg-' + Math.random().toString(36).substring(2, 10) + '-' + Date.now().toString(36)
}

function formatTimestamp(d = new Date()): string {
  const pad = (n: number, width = 2) => String(n).padStart(width, '0')
  const yyyy = d.getFullYear()
  const MM = pad(d.getMonth() + 1)
  const dd = pad(d.getDate())
  const hh = pad(d.getHours())
  const mm = pad(d.getMinutes())
  const ss = pad(d.getSeconds())
  const ms = pad(d.getMilliseconds(), 3)
  return `${yyyy}-${MM}-${dd} ${hh}:${mm}:${ss}.${ms}`
}

function addLog(entry: Omit<OcppLogEntry, 'id' | 'timestamp'>) {
  const newLog: OcppLogEntry = {
    id: 'log-' + Math.random().toString(36).substring(2, 9),
    timestamp: formatTimestamp(),
    ...entry
  }
  logs.value.unshift(newLog)
  if (logs.value.length > 1000) {
    logs.value.pop()
  }
}

export function useVirtualCharger() {
  const currentCharger = computed(() => {
    return chargers.find(c => c.id === selectedChargerId.value) || chargers[0]
  })

  // 기존 컴포넌트 하위 호환용 state 래퍼
  const state = computed<VirtualChargerState>(() => {
    const c = currentCharger.value
    return {
      chargeBoxId: c.id,
      wsUrl: c.wsUrl,
      connected: c.connected,
      connecting: c.connecting,
      status: c.status,
      connectorId: c.activeConnectorId || 1,
      transactionId: c.transactionId,
      meterValueWh: c.meterValueWh,
      soc: c.soc,
      voltage: c.voltage,
      current: c.current,
      powerKw: c.powerKw,
      heartbeatIntervalSec: 60,
      autoHeartbeat: c.autoHeartbeat,
      autoMeterValues: c.autoMeterValues,
      autoMeterIntervalSec: 5,
      scenarioRunning: c.scenarioRunning,
      scenarioStep: c.scenarioStep,
      scenarioProgress: c.scenarioProgress
    }
  })

  function selectCharger(id: string) {
    const found = chargers.find(c => c.id === id)
    if (found) {
      selectedChargerId.value = id
      terminalSearchQuery.value = id
    }
  }

  function addCharger(item: Partial<ChargerInstance>) {
    const stId = (item.stationId || '000000').padStart(6, '0')
    const cpId = (item.chargerId || '01').padStart(2, '0')
    const fullId = item.id || `${stId}${cpId}`

    if (chargers.some(c => c.id === fullId)) {
      alert(`이미 존재하는 충전기 식별자입니다: [${fullId}]`)
      return false
    }

    const newInst: ChargerInstance = {
      id: fullId,
      name: item.name || `${stId} 충전소 ${cpId}호기`,
      stationId: stId,
      chargerId: cpId,
      wsUrl: item.wsUrl || 'ws://127.0.0.1:18080/ocpp',
      vendor: item.vendor || 'LS E-Link',
      model: item.model || 'ELVIS-HPC-350K',
      powerKw: item.powerKw || 350,
      status: 'Available',
      connected: false,
      connecting: false,
      connectors: item.connectors || [{ connectorId: 1, status: 'Available' }],
      activeConnectorId: 1,
      transactionId: null,
      meterValueWh: 100000,
      soc: 20,
      voltage: 380.0,
      current: 125.0,
      autoHeartbeat: false,
      autoMeterValues: false,
      scenarioRunning: false,
      scenarioStep: '',
      scenarioProgress: 0
    }

    chargers.push(newInst)
    saveChargersToStorage()
    selectCharger(fullId)
    return true
  }

  function updateCharger(id: string, updates: Partial<ChargerInstance>) {
    const idx = chargers.findIndex(c => c.id === id)
    if (idx !== -1) {
      Object.assign(chargers[idx], updates)
      saveChargersToStorage()
    }
  }

  function deleteCharger(id: string) {
    disconnect(id)
    const idx = chargers.findIndex(c => c.id === id)
    if (idx !== -1) {
      chargers.splice(idx, 1)
      saveChargersToStorage()
      if (selectedChargerId.value === id && chargers.length > 0) {
        selectedChargerId.value = chargers[0].id
      }
    }
  }

  function clearAllErrors() {
    for (const c of chargers) {
      c.lastError = undefined
      if (c.status === 'Faulted') {
        c.status = 'Available'
      }
    }
  }

  // ─── WebSocket 연결 제어 ─────────────────────────────────────────

  function connect(targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (!c || c.connected || c.connecting) return

    const fullUrl = `${c.wsUrl.replace(/\/$/, '')}/${encodeURIComponent(c.id)}`
    c.connecting = true

    try {
      const ws = new WebSocket(fullUrl)
      socketMap.set(cid, ws)

      ws.onopen = () => {
        c.connected = true
        c.connecting = false
        c.lastError = undefined
        addLog({
          chargerId: cid,
          direction: 'SYS',
          messageType: 0,
          messageId: 'SYS-CONN',
          action: 'WS_CONNECTED',
          payload: { url: fullUrl, chargeBoxId: cid },
          raw: `[CONNECTED] [${cid}] WebSocket 연결 수립: ${fullUrl}`
        })
        if (c.autoHeartbeat) {
          startHeartbeatTimer(cid)
        }
      }

      ws.onmessage = (event: MessageEvent) => {
        handleIncomingMessage(cid, event.data)
      }

      ws.onerror = () => {
        c.connecting = false
        c.lastError = 'WebSocket 소켓 통신 에러'
        addLog({
          chargerId: cid,
          direction: 'SYS',
          messageType: MESSAGE_TYPE.CALLERROR,
          messageId: 'SYS-ERR',
          action: 'WS_ERROR',
          payload: { error: '소켓 에러 발생' },
          raw: `[ERROR] [${cid}] WebSocket 연결 실패 또는 에러 발생`
        })
      }

      ws.onclose = (event: CloseEvent) => {
        c.connected = false
        c.connecting = false
        stopHeartbeatTimer(cid)
        stopAutoMeterTimer(cid)
        abortScenario(cid)
        socketMap.delete(cid)
        addLog({
          chargerId: cid,
          direction: 'SYS',
          messageType: 0,
          messageId: 'SYS-CLOSE',
          action: 'WS_CLOSED',
          payload: { code: event.code, reason: event.reason },
          raw: `[CLOSED] [${cid}] 소켓 종료 (Code: ${event.code}, Reason: ${event.reason || 'None'})`
        })
      }
    } catch (e: any) {
      c.connecting = false
      c.lastError = e.message
      addLog({
        chargerId: cid,
        direction: 'SYS',
        messageType: 0,
        messageId: 'SYS-EXCEPT',
        action: 'CONNECT_EXCEPTION',
        payload: { message: e.message },
        raw: `[EXCEPTION] [${cid}] 소켓 초기화 실패: ${e.message}`
      })
    }
  }

  function disconnect(targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    stopHeartbeatTimer(cid)
    stopAutoMeterTimer(cid)
    abortScenario(cid)

    const ws = socketMap.get(cid)
    if (ws) {
      ws.close(1000, 'User Disconnected')
      socketMap.delete(cid)
      addLog({
        chargerId: cid,
        direction: 'SYS',
        messageType: 0,
        messageId: 'SYS-DISC',
        action: 'WS_DISCONNECTED',
        payload: { reason: '사용자 수동 연결 해제' },
        raw: `[DISCONNECTED] [${cid}] 사용자 수동 연결 해제`
      })
    }

    if (c) {
      c.connected = false
      c.connecting = false
    }
  }

  function connectAll() {
    for (const c of chargers) {
      if (!c.connected && !c.connecting) {
        connect(c.id)
      }
    }
  }

  function disconnectAll() {
    for (const c of chargers) {
      disconnect(c.id)
    }
  }

  // ─── 전문 송수신 제어 ───────────────────────────────────────────

  function sendCall(action: string, payload: any, targetId?: string): string | null {
    const cid = targetId || selectedChargerId.value
    const ws = socketMap.get(cid)

    if (!ws || ws.readyState !== WebSocket.OPEN) {
      addLog({
        chargerId: cid,
        direction: 'ERR',
        messageType: 0,
        messageId: 'ERR',
        action,
        payload: { error: '소켓 미연결 상태' },
        raw: `[SEND_FAIL] [${cid}] WebSocket이 열려있지 않아 ${action} 전송 불가`
      })
      return null
    }

    const messageId = generateMsgId()
    const rawPacket = JSON.stringify([MESSAGE_TYPE.CALL, messageId, action, payload])

    pendingRequests.set(messageId, {
      action,
      sentAt: performance.now(),
      chargerId: cid
    })

    ws.send(rawPacket)

    addLog({
      chargerId: cid,
      direction: 'SEND',
      messageType: MESSAGE_TYPE.CALL,
      messageId,
      action,
      payload,
      raw: rawPacket
    })

    return messageId
  }

  function handleIncomingMessage(cid: string, raw: string) {
    try {
      const parsed = JSON.parse(raw)
      if (!Array.isArray(parsed) || parsed.length < 3) {
        addLog({
          chargerId: cid,
          direction: 'RECV',
          messageType: 0,
          messageId: 'UNKNOWN',
          payload: parsed,
          raw
        })
        return
      }

      const messageType = parsed[0]
      const messageId = parsed[1]

      if (messageType === MESSAGE_TYPE.CALLRESULT) {
        const payload = parsed[2]
        const req = pendingRequests.get(messageId)
        let latency = 0
        let action = 'CALLRESULT'

        if (req) {
          latency = Math.round(performance.now() - req.sentAt)
          action = `${req.action}.conf`
          handleActionConf(cid, req.action, payload)
          pendingRequests.delete(messageId)
        }

        addLog({
          chargerId: cid,
          direction: 'RECV',
          messageType,
          messageId,
          action,
          payload,
          latencyMs: latency,
          raw
        })
      } else if (messageType === MESSAGE_TYPE.CALL) {
        const action = parsed[2]
        const payload = parsed[3]

        addLog({
          chargerId: cid,
          direction: 'RECV',
          messageType,
          messageId,
          action: `${action}.req (하향)`,
          payload,
          raw
        })

        handleServerCall(cid, messageId, action, payload)
      } else if (messageType === MESSAGE_TYPE.CALLERROR) {
        const req = pendingRequests.get(messageId)
        pendingRequests.delete(messageId)
        addLog({
          chargerId: cid,
          direction: 'ERR',
          messageType,
          messageId,
          action: req ? `${req.action}.error` : 'CALLERROR',
          payload: { errorCode: parsed[2], errorDescription: parsed[3], errorDetails: parsed[4] },
          raw
        })
      }
    } catch (e: any) {
      addLog({
        chargerId: cid,
        direction: 'ERR',
        messageType: 0,
        messageId: 'PARSE_ERR',
        payload: { error: e.message },
        raw
      })
    }
  }

  function sendCallResult(cid: string, messageId: string, payload: any) {
    const ws = socketMap.get(cid)
    if (!ws || ws.readyState !== WebSocket.OPEN) return
    const rawPacket = JSON.stringify([MESSAGE_TYPE.CALLRESULT, messageId, payload])
    ws.send(rawPacket)
    addLog({
      chargerId: cid,
      direction: 'SEND',
      messageType: MESSAGE_TYPE.CALLRESULT,
      messageId,
      action: 'CALLRESULT (ACK)',
      payload,
      raw: rawPacket
    })
  }

  function handleActionConf(cid: string, action: string, payload: any) {
    const c = chargers.find(item => item.id === cid)
    if (!c) return

    switch (action) {
      case 'BootNotification':
        if (payload.status === 'Accepted') {
          c.lastError = undefined
        }
        break
      case 'StartTransaction':
        if (payload.transactionId) {
          c.transactionId = payload.transactionId
          c.status = 'Charging'
          const conn = c.connectors.find(g => g.connectorId === c.activeConnectorId)
          if (conn) conn.status = 'Charging'
        }
        break
      case 'StopTransaction':
        c.transactionId = null
        c.status = 'Available'
        const conn = c.connectors.find(g => g.connectorId === c.activeConnectorId)
        if (conn) conn.status = 'Available'
        stopAutoMeterTimer(cid)
        break
      case 'StatusNotification':
        break
    }
  }

  function handleServerCall(cid: string, messageId: string, action: string, payload: any) {
    const c = chargers.find(item => item.id === cid)
    switch (action) {
      case 'Reset':
        sendCallResult(cid, messageId, { status: 'Accepted' })
        setTimeout(() => {
          disconnect(cid)
          setTimeout(() => connect(cid), 1500)
        }, 800)
        break
      case 'RemoteStartTransaction':
        sendCallResult(cid, messageId, { status: 'Accepted' })
        setTimeout(() => {
          if (c) {
            c.status = 'Preparing'
            sendStatusNotification('Preparing', 'NoError', cid)
            setTimeout(() => {
              sendStartTransaction(payload.idTag || 'CARD-USER-1001', c.meterValueWh, cid)
            }, 1000)
          }
        }, 500)
        break
      case 'RemoteStopTransaction':
        sendCallResult(cid, messageId, { status: 'Accepted' })
        setTimeout(() => {
          if (c) {
            sendStopTransaction('APP-REMOTE-STOP', 'Remote', cid)
          }
        }, 500)
        break
      case 'UnlockConnector':
        sendCallResult(cid, messageId, { status: 'Unlocked' })
        break
      case 'ChangeAvailability':
        sendCallResult(cid, messageId, { status: 'Accepted' })
        break
      case 'TriggerMessage':
        sendCallResult(cid, messageId, { status: 'Accepted' })
        if (payload.requestedMessage === 'StatusNotification') {
          sendStatusNotification(c ? c.status : 'Available', 'NoError', cid)
        } else if (payload.requestedMessage === 'Heartbeat') {
          sendHeartbeat(cid)
        }
        break
      default:
        sendCallResult(cid, messageId, { status: 'Accepted' })
        break
    }
  }

  // ─── 간편 단축 전문 발송 함수들 ───────────────────────────────────

  function sendBootNotification(vendor = 'LS E-Link', model = 'ELVIS-HPC-350K', firmware = 'v1.0.0', targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    return sendCall('BootNotification', {
      chargePointVendor: vendor || (c?.vendor ?? 'LS E-Link'),
      chargePointModel: model || (c?.model ?? 'ELVIS-HPC-350K'),
      chargePointSerialNumber: `${cid}-SN`,
      chargeBoxSerialNumber: cid,
      firmwareVersion: firmware,
      iccid: '8982000000000000001',
      imsi: '450000000000001',
      meterType: 'ELVIS-DIGITAL-01',
      meterSerialNumber: `MTR-${cid}`
    }, cid)
  }

  function sendHeartbeat(targetId?: string) {
    const cid = targetId || selectedChargerId.value
    return sendCall('Heartbeat', {}, cid)
  }

  function sendStatusNotification(status: string, errorCode = 'NoError', targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (c) {
      c.status = status as OcppChargePointStatus
      const conn = c.connectors.find(g => g.connectorId === c.activeConnectorId)
      if (conn) conn.status = status as OcppChargePointStatus
    }
    return sendCall('StatusNotification', {
      connectorId: c?.activeConnectorId || 1,
      errorCode,
      status,
      timestamp: new Date().toISOString()
    }, cid)
  }

  function sendAuthorize(idTag: string, targetId?: string) {
    const cid = targetId || selectedChargerId.value
    return sendCall('Authorize', { idTag }, cid)
  }

  function sendStartTransaction(idTag: string, meterStart?: number, targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    const startVal = meterStart ?? (c?.meterValueWh ?? 100000)
    if (c) c.meterValueWh = startVal

    return sendCall('StartTransaction', {
      connectorId: c?.activeConnectorId || 1,
      idTag,
      meterStart: startVal,
      timestamp: new Date().toISOString()
    }, cid)
  }

  function sendMeterValues(incrementWh = 500, targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (!c) return null

    c.meterValueWh += incrementWh
    c.soc = Math.min(100, c.soc + 1)
    const nowIso = new Date().toISOString()

    return sendCall('MeterValues', {
      connectorId: c.activeConnectorId || 1,
      transactionId: c.transactionId || 1001,
      meterValue: [
        {
          timestamp: nowIso,
          sampledValue: [
            {
              value: String(c.meterValueWh),
              context: 'Sample.Periodic',
              format: 'Raw',
              measurand: 'Energy.Active.Import.Register',
              unit: 'Wh'
            },
            {
              value: String(c.soc),
              context: 'Sample.Periodic',
              format: 'Raw',
              measurand: 'SoC',
              unit: 'Percent'
            },
            {
              value: String(c.powerKw * 1000),
              context: 'Sample.Periodic',
              format: 'Raw',
              measurand: 'Power.Active.Import',
              unit: 'W'
            },
            {
              value: String(c.voltage),
              context: 'Sample.Periodic',
              format: 'Raw',
              measurand: 'Voltage',
              unit: 'V'
            },
            {
              value: String(c.current),
              context: 'Sample.Periodic',
              format: 'Raw',
              measurand: 'Current.Import',
              unit: 'A'
            }
          ]
        }
      ]
    }, cid)
  }

  function sendStopTransaction(idTag = 'CARD-CORP-9901', reason = 'Local', targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    const txId = c?.transactionId || 1001

    return sendCall('StopTransaction', {
      idTag,
      meterStop: c?.meterValueWh || 105000,
      timestamp: new Date().toISOString(),
      transactionId: txId,
      reason,
      transactionData: [
        {
          timestamp: new Date().toISOString(),
          sampledValue: [
            {
              value: String(c?.meterValueWh || 105000),
              context: 'Transaction.End',
              format: 'Raw',
              measurand: 'Energy.Active.Import.Register',
              unit: 'Wh'
            },
            {
              value: String(c?.soc || 80),
              context: 'Transaction.End',
              format: 'Raw',
              measurand: 'SoC',
              unit: 'Percent'
            }
          ]
        }
      ]
    }, cid)
  }

  function sendDataTransfer(vendorId = 'LS E-Link', messageId = 'DiagnosticsData', data = '{}', targetId?: string) {
    const cid = targetId || selectedChargerId.value
    return sendCall('DataTransfer', {
      vendorId,
      messageId,
      data
    }, cid)
  }

  function toggleAutoHeartbeat(targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (!c) return
    c.autoHeartbeat = !c.autoHeartbeat
    if (c.autoHeartbeat) {
      startHeartbeatTimer(cid)
    } else {
      stopHeartbeatTimer(cid)
    }
  }

  function startHeartbeatTimer(cid: string) {
    stopHeartbeatTimer(cid)
    const timer = setInterval(() => {
      const c = chargers.find(item => item.id === cid)
      if (c && c.connected) {
        sendHeartbeat(cid)
      } else {
        stopHeartbeatTimer(cid)
      }
    }, 60000)
    heartbeatTimerMap.set(cid, timer)
  }

  function stopHeartbeatTimer(cid: string) {
    const timer = heartbeatTimerMap.get(cid)
    if (timer) {
      clearInterval(timer)
      heartbeatTimerMap.delete(cid)
    }
    const c = chargers.find(item => item.id === cid)
    if (c) c.autoHeartbeat = false
  }

  function toggleAutoMeterValues(targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (!c) return
    c.autoMeterValues = !c.autoMeterValues
    if (c.autoMeterValues) {
      startAutoMeterTimer(cid)
    } else {
      stopAutoMeterTimer(cid)
    }
  }

  function startAutoMeterTimer(cid: string) {
    stopAutoMeterTimer(cid)
    const timer = setInterval(() => {
      const c = chargers.find(item => item.id === cid)
      if (c && c.connected && c.status === 'Charging') {
        sendMeterValues(500, cid)
      } else if (!c || !c.connected) {
        stopAutoMeterTimer(cid)
      }
    }, 5000)
    autoMeterTimerMap.set(cid, timer)
  }

  function stopAutoMeterTimer(cid: string) {
    const timer = autoMeterTimerMap.get(cid)
    if (timer) {
      clearInterval(timer)
      autoMeterTimerMap.delete(cid)
    }
    const c = chargers.find(item => item.id === cid)
    if (c) c.autoMeterValues = false
  }

  // ─── 4대 회원 유형별 충전 시나리오 러너 ───────────────────────────────────

  function delay(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms))
  }

  async function runFullChargeScenario(userType = 'C', idTag = 'CARD-CORP-9901', targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (!c) return
    if (!c.connected || c.scenarioRunning) {
      if (!c.connected) connect(cid)
      await delay(800)
    }

    c.scenarioRunning = true
    c.scenarioProgress = 0
    abortFlagMap.set(cid, false)

    addLog({
      chargerId: cid,
      direction: 'SYS',
      messageType: 0,
      messageId: 'SYS-SCENARIO',
      action: 'SCENARIO_START',
      payload: { userType, idTag, stepCount: 8 },
      raw: `[SCENARIO] [${cid}] [${userType}] 8단계 충전 라이프사이클 시나리오 시작 (${idTag})`
    })

    try {
      // Step 1: BootNotification
      c.scenarioStep = '1/8: 부팅 및 기동 통보 (BootNotification)'
      c.scenarioProgress = 12
      sendBootNotification(c.vendor, c.model, '1.0.0', cid)
      await delay(1200)
      if (abortFlagMap.get(cid)) return

      // Step 2: Preparing
      c.scenarioStep = '2/8: 커넥터 결착 감지 (StatusNotification: Preparing)'
      c.scenarioProgress = 25
      sendStatusNotification('Preparing', 'NoError', cid)
      await delay(1000)
      if (abortFlagMap.get(cid)) return

      // Step 3: Authorize
      c.scenarioStep = `3/8: [${userType}] 회원 인증 (${idTag})`
      c.scenarioProgress = 37
      sendAuthorize(idTag, cid)
      await delay(1200)
      if (abortFlagMap.get(cid)) return

      // Step 4: StartTransaction
      c.scenarioStep = '4/8: 충전 트랜잭션 개시 (StartTransaction)'
      c.scenarioProgress = 50
      sendStartTransaction(idTag, c.meterValueWh, cid)
      await delay(1500)
      if (abortFlagMap.get(cid)) return

      // Step 5: MeterValues 3회
      for (let i = 1; i <= 3; i++) {
        c.scenarioStep = `5/8: 충전 전력 계측 전송 (${i}/3회, MeterValues)`
        c.scenarioProgress = 50 + (i * 10)
        sendMeterValues(800, cid)
        await delay(1400)
        if (abortFlagMap.get(cid)) return
      }

      // Step 6: Finishing
      c.scenarioStep = '6/8: 충전 종료 통보 (StatusNotification: Finishing)'
      c.scenarioProgress = 85
      sendStatusNotification('Finishing', 'NoError', cid)
      await delay(1000)
      if (abortFlagMap.get(cid)) return

      // Step 7: StopTransaction
      c.scenarioStep = '7/8: 거래 세션 마감 정산 (StopTransaction)'
      c.scenarioProgress = 95
      sendStopTransaction(idTag, 'Local', cid)
      await delay(1200)
      if (abortFlagMap.get(cid)) return

      // Step 8: Available
      c.scenarioStep = '8/8: 커넥터 분리 및 대기 복귀 (StatusNotification: Available)'
      c.scenarioProgress = 100
      sendStatusNotification('Available', 'NoError', cid)
      await delay(500)

      c.scenarioStep = `[${userType}] 시나리오 성공 완주!`
      addLog({
        chargerId: cid,
        direction: 'SYS',
        messageType: 0,
        messageId: 'SYS-SCENARIO',
        action: 'SCENARIO_COMPLETE',
        payload: { userType, idTag, success: true },
        raw: `[SCENARIO] [${cid}] [${userType}] 정상 충전 라이프사이클 시나리오 성공 완주`
      })
    } catch (e: any) {
      c.scenarioStep = `시나리오 오류: ${e.message}`
      addLog({
        chargerId: cid,
        direction: 'SYS',
        messageType: 0,
        messageId: 'SYS-SCENARIO',
        action: 'SCENARIO_ERROR',
        payload: { error: e.message },
        raw: `[SCENARIO] [${cid}] 시나리오 오류 발생: ${e.message}`
      })
    } finally {
      c.scenarioRunning = false
      setTimeout(() => {
        if (!c.scenarioRunning && c.scenarioProgress === 100) {
          c.scenarioProgress = 0
          c.scenarioStep = ''
        }
      }, 5000)
    }
  }

  function abortScenario(targetId?: string) {
    const cid = targetId || selectedChargerId.value
    abortFlagMap.set(cid, true)
    const c = chargers.find(item => item.id === cid)
    if (c) {
      c.scenarioRunning = false
      c.scenarioStep = '시나리오 중단됨'
      addLog({
        chargerId: cid,
        direction: 'SYS',
        messageType: 0,
        messageId: 'SYS-ABORT',
        action: 'SCENARIO_ABORTED',
        payload: { reason: 'User Abort' },
        raw: `[SCENARIO] [${cid}] 시나리오 강제 중단됨`
      })
      setTimeout(() => {
        c.scenarioStep = ''
        c.scenarioProgress = 0
      }, 2500)
    }
  }

  async function runBootSequence(targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (!c) return

    addLog({
      chargerId: cid,
      direction: 'SYS',
      messageType: 0,
      messageId: 'SYS-BOOT',
      action: 'BOOT_SEQUENCE_START',
      payload: { stage: '4단계 기동 전문 순차 발송' },
      raw: `[SYS] [${cid}] 기동 전문 4단계(Boot, Preparing, Available, Heartbeat) 순차 발송 개시`
    })

    if (!c.connected) {
      connect(cid)
      await delay(800)
    }
    sendBootNotification(c.vendor, c.model, '1.0.0', cid)
    await delay(1000)
    sendStatusNotification('Preparing', 'NoError', cid)
    await delay(800)
    sendStatusNotification('Available', 'NoError', cid)
    await delay(800)
    sendHeartbeat(cid)
  }

  function setTerminalSearchQuery(query: string) {
    terminalSearchQuery.value = query
  }

  function clearLogs() {
    logs.value = []
  }

  return {
    chargers,
    selectedChargerId,
    currentCharger,
    state,
    logs,
    terminalSearchQuery,
    totalCount: computed(() => chargers.length),
    connectedCount: computed(() => chargers.filter(c => c.connected).length),
    selectCharger,
    setTerminalSearchQuery,
    addCharger,
    updateCharger,
    deleteCharger,
    clearAllErrors,
    connect,
    disconnect,
    connectAll,
    disconnectAll,
    sendCall,
    sendBootNotification,
    sendHeartbeat,
    sendStatusNotification,
    sendAuthorize,
    sendStartTransaction,
    sendMeterValues,
    sendStopTransaction,
    sendDataTransfer,
    toggleAutoHeartbeat,
    toggleAutoMeterValues,
    runFullChargeScenario,
    runBootSequence,
    abortScenario,
    clearLogs
  }
}
