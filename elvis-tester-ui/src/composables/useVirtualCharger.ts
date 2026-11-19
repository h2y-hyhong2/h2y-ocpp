import { ref, reactive, computed } from 'vue'
import {
  MESSAGE_TYPE,
  type OcppLogEntry,
  type ChargerInstance,
  type OcppChargePointStatus,
  type VirtualChargerState
} from '../types/ocpp'

const STORAGE_KEY = 'elvis_tester_chargers_mysql_v1'
const API_BASE_URL = 'http://127.0.0.1:9082'

// ─────────────────────────────────────────────────────────────
// MySQL DB 실시간 연동 및 동기화 상태
// ─────────────────────────────────────────────────────────────
const isMysqlSyncing = ref(false)
const isMysqlConnected = ref(false)
const mysqlError = ref<string | null>(null)
const lastSyncedAt = ref<string | null>(null)

// MySQL DTO -> 테스터 충전기 인스턴스 매핑 헬퍼
function mapDtoToChargerInstance(dto: any): ChargerInstance {
  const chargeBoxId = String(dto.chargeBoxId || dto.id || '00000001').trim()
  const stId = String(dto.stId || '000000').trim()
  const cpId = String(dto.cpId || '01').trim()
  const powerKw = dto.powerKw && Number(dto.powerKw) > 0 ? Number(dto.powerKw) : 350

  // 듀얼 커넥터 (기본 2개 보장: 커넥터 ID 1, 2)
  let conns: any[] = []
  if (Array.isArray(dto.connectors) && dto.connectors.length > 0) {
    conns = dto.connectors.map((c: any) => ({
      connectorId: Number(c.connectorId) || 1,
      status: (c.status && c.status !== '연결안됨') ? c.status : 'Unavailable',
      type: c.type || 'DC콤보1 (CCS1)',
      maxPowerKw: Number(c.maxPowerKw) || Math.round(powerKw / 2),
      voltageV: Number(c.voltageV) || 800,
      currentA: Number(c.currentA) || 250,
      socPercent: Number(c.socPercent) || 0,
      batteryTempC: Number(c.batteryTempC) || 25,
      carModel: c.carModel || (c.connectorId === 1 ? '아이오닉5 (현대)' : 'EV6 (기아)'),
      userTag: c.userTag || (c.connectorId === 1 ? 'CARD-CORP-9901' : 'CARD-USER-1001'),
      accumulatedKwh: Number(c.accumulatedKwh) || 0
    }))
  }

  if (conns.length === 0) {
    conns.push({
      connectorId: 1,
      status: 'Unavailable',
      type: 'DC콤보1 (CCS1)',
      maxPowerKw: Math.round(powerKw / 2),
      voltageV: 800,
      currentA: 250,
      socPercent: 0,
      batteryTempC: 25,
      carModel: '아이오닉5 (현대)',
      userTag: 'CARD-CORP-9901',
      accumulatedKwh: 0
    })
  }
  if (conns.length === 1) {
    conns.push({
      connectorId: 2,
      status: 'Unavailable',
      type: 'DC콤보1 (CCS1)',
      maxPowerKw: Math.round(powerKw / 2),
      voltageV: 800,
      currentA: 250,
      socPercent: 0,
      batteryTempC: 25,
      carModel: 'EV6 (기아)',
      userTag: 'CARD-USER-1001',
      accumulatedKwh: 0
    })
  }

  return {
    id: chargeBoxId,
    name: dto.stationName ? `${dto.stationName} ${cpId}호기` : `충전소 ${cpId}호기`,
    stationId: stId,
    chargerId: cpId,
    stationName: dto.stationName || '서울 강남 테헤란로 하이퍼차징 스테이션',
    corpName: dto.corpName || '(주)LS E-Link (본사 직영)',
    addr: dto.addr || '서울특별시 강남구 테헤란로 152',
    spec: dto.spec || `${powerKw}kW 초급속 듀얼 (DC콤보1)`,
    protocol: dto.protocol || 'OCPP 1.6-J',
    firmwareVer: dto.firmwareVer || '1.0.0-RELEASE',
    useYn: dto.useYn || 'Y',
    wsUrl: 'ws://127.0.0.1:9080/ocpp',
    vendor: dto.vendor || 'LS E-Link Power',
    model: dto.model || 'ELVIS-HPC-350K',
    powerKw: powerKw,
    status: (dto.status && dto.status !== '연결안됨') ? dto.status : 'Unavailable',
    connected: false,
    connecting: false,
    connectors: conns,
    activeConnectorId: 1,
    transactionId: null,
    meterValueWh: dto.accumulatedKwh ? Math.round(Number(dto.accumulatedKwh) * 1000) : 100000,
    soc: dto.socPercent || 25,
    voltage: dto.voltageV || 380.0,
    current: dto.currentA || 125.0,
    heartbeatIntervalSec: 60,
    autoHeartbeat: true,
    autoBootOnConnect: true,
    autoReply: true,
    autoMeterValues: true,
    autoMeterIntervalSec: 10,
    scenarioRunning: false,
    scenarioStep: '',
    scenarioProgress: 0
  }
}

// MySQL 초기 시드 폴백
const FALLBACK_MYSQL_CHARGER: ChargerInstance = mapDtoToChargerInstance({
  chargeBoxId: '00000001',
  stId: '000000',
  cpId: '01',
  stationName: '서울 강남 테헤란로 하이퍼차징 스테이션',
  corpName: '(주)LS E-Link (본사 직영)',
  vendor: 'LS E-Link Power',
  model: 'ELVIS-HPC-350K',
  spec: '350kW 초급속',
  protocol: 'OCPP 1.6-J',
  powerKw: 350
})

function initChargers(): ChargerInstance[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed.map(c => ({
          ...c,
          connected: false,
          connecting: false,
          status: 'Unavailable',
          scenarioRunning: false
        }))
      }
    }
  } catch (e) {
    console.warn('[MySQL Sync] 로컬 캐시 파싱 실패:', e)
  }
  return [FALLBACK_MYSQL_CHARGER]
}

const chargers = reactive<ChargerInstance[]>(initChargers())
const selectedChargerId = ref<string>(chargers[0]?.id || '00000001')
const selectedIdTag = ref<string>('CARD-CORP-9901')

// MySQL 백엔드 API(9082)에서 실제 DB 충전기 목록 동기화
async function fetchChargersFromMysql(): Promise<boolean> {
  isMysqlSyncing.value = true
  mysqlError.value = null
  try {
    const resp = await fetch(`${API_BASE_URL}/api/v1/chargers`)
    if (!resp.ok) {
      throw new Error(`HTTP ${resp.status}: ${resp.statusText}`)
    }
    const json = await resp.json()
    if (!json.success || !Array.isArray(json.data)) {
      throw new Error(json.message || '유효하지 않은 MySQL 응답 포맷')
    }

    const fetchedList = json.data.map(mapDtoToChargerInstance)

    // 기존 연결 상태 및 커넥터 상태 보존 병합
    const mergedList = fetchedList.map(newC => {
      const existing = chargers.find(c => c.id === newC.id)
      if (existing) {
        const mergedConnectors = (newC.connectors || []).map(nc => {
          const exConn = existing.connectors?.find(ec => ec.connectorId === nc.connectorId)
          if (!exConn) return nc
          return {
            ...nc,
            status: existing.connected ? exConn.status : nc.status,
            transactionId: exConn.transactionId ?? nc.transactionId ?? null,
            socPercent: exConn.socPercent ?? nc.socPercent,
            accumulatedKwh: exConn.accumulatedKwh ?? nc.accumulatedKwh
          }
        })
        return {
          ...newC,
          connected: existing.connected,
          connecting: existing.connecting,
          status: existing.connected ? existing.status : newC.status,
          connectors: mergedConnectors.length >= 2 ? mergedConnectors : existing.connectors,
          activeConnectorId: existing.activeConnectorId || 1,
          transactionId: existing.transactionId,
          meterValueWh: existing.meterValueWh,
          soc: existing.soc,
          autoHeartbeat: existing.autoHeartbeat,
          autoBootOnConnect: existing.autoBootOnConnect,
          autoReply: existing.autoReply,
          autoMeterValues: existing.autoMeterValues,
          autoMeterIntervalSec: existing.autoMeterIntervalSec,
          heartbeatIntervalSec: existing.heartbeatIntervalSec,
          scenarioRunning: existing.scenarioRunning,
          scenarioStep: existing.scenarioStep,
          scenarioProgress: existing.scenarioProgress
        }
      }
      return newC
    })

    // 로컬에서만 추가된 충전기(MySQL 미등록 항목) 보존
    const fetchedIds = new Set(mergedList.map(c => c.id))
    const localOnlyChargers = chargers.filter(c => !fetchedIds.has(c.id))
    chargers.splice(0, chargers.length, ...mergedList, ...localOnlyChargers)
    if (!chargers.some(c => c.id === selectedChargerId.value) && chargers.length > 0) {
      selectedChargerId.value = chargers[0].id
    }
    isMysqlConnected.value = true
    lastSyncedAt.value = formatTimestamp()
    saveChargersToStorage()
    console.log(`[MySQL Sync] MySQL에서 ${chargers.length}기의 충전기 데이터를 성공적으로 동기화했습니다.`)
    return true
  } catch (err: any) {
    console.error('[MySQL Sync Failed]', err)
    mysqlError.value = err.message || 'MySQL 백엔드 API(9082) 통신 오류'
    isMysqlConnected.value = false
    return false
  } finally {
    isMysqlSyncing.value = false
  }
}

// 모듈 로드 시 즉시 백엔드 MySQL 동기화 시도
fetchChargersFromMysql()

const logs = ref<OcppLogEntry[]>([])
const terminalSearchQuery = ref<string>('')

function setSelectedIdTag(idTag: string) {
  selectedIdTag.value = idTag
}

// 활성 소켓 및 타이머 관리 맵
const socketMap = new Map<string, WebSocket>()
const heartbeatTimerMap = new Map<string, any>()
const autoMeterTimerMap = new Map<string, any>()
const abortFlagMap = new Map<string, boolean>()
const pendingRequests = new Map<string, { action: string; sentAt: number; chargerId: string; connectorId?: number; transactionId?: number | null }>()

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

  // 브라우저 DevTools 콘솔에도 OCPP 방향 배지와 함께 클릭하여 탐색 가능한 JSON 객체로 출력
  let parsedJson: any = entry.payload
  if (entry.raw && typeof entry.raw === 'string' && (entry.raw.startsWith('[') || entry.raw.startsWith('{'))) {
    try {
      parsedJson = JSON.parse(entry.raw)
    } catch {
      parsedJson = entry.raw
    }
  }

  const badge = entry.direction === 'SEND' ? '[SEND]' : entry.direction === 'RECV' ? '[RECV]' : '[SYS]'
  const cid = entry.chargerId ? `[${entry.chargerId}]` : ''
  const act = entry.action ? `[${entry.action}]` : ''
  console.log(`${badge} ${cid} ${act}`, parsedJson)
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
      heartbeatIntervalSec: c.heartbeatIntervalSec || 60,
      autoHeartbeat: c.autoHeartbeat,
      autoBootOnConnect: c.autoBootOnConnect,
      autoReply: c.autoReply,
      autoMeterValues: c.autoMeterValues,
      autoMeterIntervalSec: c.autoMeterIntervalSec || 10,
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
    const rawSt = String(item.stationId ?? '000000').trim()
    const rawCp = String(item.chargerId ?? '01').trim()
    if (!/^\d{1,6}$/.test(rawSt)) {
      alert('충전소 ID(ST_ID)는 000000~999999 범위의 6자리 이하 숫자여야 합니다.')
      return false
    }
    if (!/^\d{1,2}$/.test(rawCp) || Number(rawCp) < 1 || Number(rawCp) > 99) {
      alert('충전기 ID(CP_ID)는 01~99 범위의 숫자여야 합니다.')
      return false
    }

    const stId = rawSt.padStart(6, '0')
    const cpId = rawCp.padStart(2, '0')
    const fullId = `${stId}${cpId}`

    if (chargers.some(c => c.id === fullId)) {
      alert(`이미 존재하는 충전기 식별자입니다: [${fullId}]`)
      return false
    }

    const powerKw = Number(item.powerKw) > 0 ? Number(item.powerKw) : 350
    const halfKw = Math.round(powerKw / 2)
    const defaultConnectors = (item.connectors && item.connectors.length >= 2)
      ? item.connectors
      : [
          {
            connectorId: 1,
            status: 'Unavailable' as OcppChargePointStatus,
            transactionId: null,
            type: 'DC콤보1 (CCS1)',
            maxPowerKw: halfKw,
            voltageV: 800,
            currentA: 250,
            socPercent: 0,
            batteryTempC: 25,
            carModel: '아이오닉5 (현대)',
            userTag: 'CARD-CORP-9901',
            accumulatedKwh: 0
          },
          {
            connectorId: 2,
            status: 'Unavailable' as OcppChargePointStatus,
            transactionId: null,
            type: 'DC콤보1 (CCS1)',
            maxPowerKw: halfKw,
            voltageV: 800,
            currentA: 250,
            socPercent: 0,
            batteryTempC: 25,
            carModel: 'EV6 (기아)',
            userTag: 'CARD-USER-1001',
            accumulatedKwh: 0
          }
        ]

    const newInst: ChargerInstance = {
      id: fullId,
      name: item.name || `${stId} 충전소 ${cpId}호기`,
      stationId: stId,
      chargerId: cpId,
      stationName: item.stationName || `${stId} 충전소`,
      corpName: item.corpName || '(주)LS E-Link',
      addr: item.addr || '',
      spec: item.spec || `${powerKw}kW 초급속 듀얼 (DC콤보1)`,
      protocol: item.protocol || 'OCPP 1.6-J',
      firmwareVer: item.firmwareVer || '1.0.0-RELEASE',
      useYn: item.useYn || 'Y',
      wsUrl: item.wsUrl || 'ws://127.0.0.1:9080/ocpp',
      vendor: item.vendor || 'LS E-Link',
      model: item.model || 'ELVIS-HPC-350K',
      powerKw,
      status: 'Unavailable',
      connected: false,
      connecting: false,
      connectors: defaultConnectors,
      activeConnectorId: 1,
      transactionId: null,
      meterValueWh: 100000,
      soc: 20,
      voltage: 380.0,
      current: 125.0,
      heartbeatIntervalSec: Number(item.heartbeatIntervalSec) || 60,
      autoHeartbeat: item.autoHeartbeat !== undefined ? Boolean(item.autoHeartbeat) : true,
      autoBootOnConnect: item.autoBootOnConnect !== undefined ? Boolean(item.autoBootOnConnect) : true,
      autoReply: item.autoReply !== undefined ? Boolean(item.autoReply) : true,
      autoMeterValues: item.autoMeterValues !== undefined ? Boolean(item.autoMeterValues) : true,
      autoMeterIntervalSec: Number(item.autoMeterIntervalSec) || 10,
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
    if (idx === -1) return false

    const nextId = updates.id || id
    if (nextId !== id && chargers.some((c, i) => i !== idx && c.id === nextId)) {
      alert(`이미 존재하는 충전기 식별자입니다: [${nextId}]`)
      return false
    }

    Object.assign(chargers[idx], updates)
    if (nextId !== id) {
      if (selectedChargerId.value === id) {
        selectedChargerId.value = nextId
      }
      const ws = socketMap.get(id)
      if (ws) {
        socketMap.delete(id)
        socketMap.set(nextId, ws)
      }
      const hb = heartbeatTimerMap.get(id)
      if (hb) {
        heartbeatTimerMap.delete(id)
        heartbeatTimerMap.set(nextId, hb)
      }
      const mt = autoMeterTimerMap.get(id)
      if (mt) {
        autoMeterTimerMap.delete(id)
        autoMeterTimerMap.set(nextId, mt)
      }
    }
    saveChargersToStorage()
    return true
  }

  function setActiveConnector(connectorId: number, targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (c) {
      c.activeConnectorId = connectorId
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
      const ws = new WebSocket(fullUrl, ['ocpp1.6'])
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
        if (c.autoBootOnConnect) {
          setTimeout(() => {
            const current = chargers.find(item => item.id === cid)
            if (current && current.connected) {
              runBootSequence(cid)
            }
          }, 350)
        }
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
      c.status = 'Unavailable'
      if (c.connectors) {
        c.connectors.forEach(conn => { conn.status = 'Unavailable' })
      }
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
    const c = chargers.find(item => item.id === cid)
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
      chargerId: cid,
      connectorId: typeof payload?.connectorId === 'number' ? payload.connectorId : undefined,
      transactionId: typeof payload?.transactionId === 'number' ? payload.transactionId : undefined
    })

    ws.send(rawPacket)

    // StatusNotification 발송 시 충전기 및 커넥터 상태 즉시 동기화
    if (c && action === 'StatusNotification' && payload?.status) {
      c.status = payload.status
      const connId = payload.connectorId ?? c.activeConnectorId ?? 1
      const conn = c.connectors?.find(g => g.connectorId === connId)
      if (conn) conn.status = payload.status
    }

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
          handleActionConf(cid, req.action, payload, req)
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

  function handleActionConf(
    cid: string,
    action: string,
    payload: any,
    reqMeta?: { connectorId?: number; transactionId?: number | null }
  ) {
    const c = chargers.find(item => item.id === cid)
    if (!c) return

    switch (action) {
      case 'BootNotification':
        if (payload.status === 'Accepted') {
          c.lastError = undefined
        }
        // 관제 서버에서 회신한 heartbeat interval 동기화
        const hbInterval = payload.interval || payload.heartbeatInterval
        if (hbInterval && Number(hbInterval) > 0) {
          c.heartbeatIntervalSec = Number(hbInterval)
          if (c.autoHeartbeat && c.connected) {
            startHeartbeatTimer(cid)
          }
        }
        break
      case 'StartTransaction':
        if (payload.transactionId != null) {
          const targetConnId = reqMeta?.connectorId ?? c.activeConnectorId ?? 1
          const startConn = c.connectors.find(g => g.connectorId === targetConnId)
          if (startConn) {
            startConn.transactionId = payload.transactionId
            startConn.status = 'Charging'
          }
          c.transactionId = payload.transactionId
          c.status = 'Charging'
          // 충전 시작 시 autoMeterValues가 켜져 있으면 자동 계측치 발송 시작
          if (c.autoMeterValues) {
            startAutoMeterTimer(cid)
          }
        }
        break
      case 'StopTransaction': {
        const stoppedTxId = reqMeta?.transactionId
        let stopConn = stoppedTxId != null
          ? c.connectors.find(g => g.transactionId === stoppedTxId)
          : undefined
        if (!stopConn) {
          const fallbackConnId = reqMeta?.connectorId ?? c.activeConnectorId ?? 1
          stopConn = c.connectors.find(g => g.connectorId === fallbackConnId)
        }
        if (stopConn) {
          stopConn.transactionId = null
          stopConn.status = 'Available'
        }

        const otherCharging = c.connectors.find(
          g => g.connectorId !== stopConn?.connectorId && (g.status === 'Charging' || g.transactionId != null)
        )
        if (otherCharging) {
          c.transactionId = otherCharging.transactionId ?? null
          c.status = 'Charging'
        } else {
          c.transactionId = null
          c.status = 'Available'
          stopAutoMeterTimer(cid)
        }
        break
      }
      case 'StatusNotification':
        break
    }
  }

  function handleServerCall(cid: string, messageId: string, action: string, payload: any) {
    const c = chargers.find(item => item.id === cid)

    // autoReply가 false인 경우: 서버의 CALL에 자동 응답하지 않고 대기 (수동 검증 or 타임아웃 테스트용)
    if (c && c.autoReply === false) {
      addLog({
        chargerId: cid,
        direction: 'SYS',
        messageType: 0,
        messageId,
        action: `${action}.req`,
        payload: { autoReply: false, reason: 'AutoReply 비활성화로 자동응답(CALLRESULT) 생략됨' },
        raw: `[AUTO-REPLY OFF] [${cid}] 관제 CALL [${action}] 수신: autoReply 설정이 꺼져 있어 자동 ACK를 전송하지 않았습니다.`
      })
      return
    }

    switch (action) {
      case 'Reset':
        sendCallResult(cid, messageId, { status: 'Accepted' })
        setTimeout(() => {
          disconnect(cid)
          setTimeout(() => connect(cid), 1500)
        }, 800)
        break
      case 'RemoteStartTransaction': {
        const targetConnId = typeof payload?.connectorId === 'number' && payload.connectorId > 0
          ? payload.connectorId
          : (c?.activeConnectorId || 1)
        const targetConn = c?.connectors.find(g => g.connectorId === targetConnId)
        if (c && typeof payload?.connectorId === 'number' && payload.connectorId > 0 && !targetConn) {
          sendCallResult(cid, messageId, { status: 'Rejected' })
          break
        }
        if (c && targetConn) {
          c.activeConnectorId = targetConnId
        }
        sendCallResult(cid, messageId, { status: 'Accepted' })
        setTimeout(() => {
          if (c) {
            c.status = 'Preparing'
            sendStatusNotification('Preparing', 'NoError', cid, targetConnId)
            setTimeout(() => {
              sendStartTransaction(payload?.idTag || 'CARD-USER-1001', c.meterValueWh, cid, targetConnId)
            }, 1000)
          }
        }, 500)
        break
      }
      case 'RemoteStopTransaction': {
        const reqTxId = typeof payload?.transactionId === 'number' ? payload.transactionId : null
        const matchedConn = reqTxId != null
          ? c?.connectors.find(g => g.transactionId === reqTxId)
          : c?.connectors.find(g => g.transactionId != null)
        const hasMatch = Boolean(
          c && (matchedConn || (reqTxId != null && c.transactionId === reqTxId))
        )
        if (!c || !hasMatch) {
          sendCallResult(cid, messageId, { status: 'Rejected' })
          break
        }
        const txToStop = reqTxId ?? matchedConn?.transactionId ?? c.transactionId
        const connToStop = matchedConn?.connectorId ?? c.activeConnectorId
        sendCallResult(cid, messageId, { status: 'Accepted' })
        setTimeout(() => {
          sendStopTransaction('APP-REMOTE-STOP', 'Remote', cid, connToStop, txToStop)
        }, 500)
        break
      }
      case 'UnlockConnector':
        sendCallResult(cid, messageId, { status: 'Unlocked' })
        break
      case 'ChangeAvailability':
        sendCallResult(cid, messageId, { status: 'Accepted' })
        break
      case 'ChangeConfiguration':
        // 관제에서 HeartbeatInterval 등 설정 변경 지시 시 실시간 반영
        if (payload && payload.key === 'HeartbeatInterval' && c) {
          const newInt = parseInt(payload.value, 10)
          if (!isNaN(newInt) && newInt > 0) {
            c.heartbeatIntervalSec = newInt
            if (c.autoHeartbeat && c.connected) {
              startHeartbeatTimer(cid)
            }
          }
        }
        sendCallResult(cid, messageId, { status: 'Accepted' })
        break
      case 'GetConfiguration':
        sendCallResult(cid, messageId, {
          configurationKey: [
            { key: 'HeartbeatInterval', readonly: false, value: String(c?.heartbeatIntervalSec || 60) },
            { key: 'MeterValueSampleInterval', readonly: false, value: String(c?.autoMeterIntervalSec || 10) }
          ],
          unknownKey: []
        })
        break
      case 'TriggerMessage': {
        const reqMsg = payload?.requestedMessage
        const triggerConnId = typeof payload?.connectorId === 'number' && payload.connectorId > 0
          ? payload.connectorId
          : (c?.activeConnectorId || 1)
        const triggerConn = c?.connectors.find(g => g.connectorId === triggerConnId)

        if (reqMsg === 'StatusNotification') {
          sendCallResult(cid, messageId, { status: 'Accepted' })
          sendStatusNotification(triggerConn?.status || c?.status || 'Available', 'NoError', cid, triggerConnId)
        } else if (reqMsg === 'Heartbeat') {
          sendCallResult(cid, messageId, { status: 'Accepted' })
          sendHeartbeat(cid)
        } else if (reqMsg === 'BootNotification') {
          sendCallResult(cid, messageId, { status: 'Accepted' })
          sendBootNotification(c?.vendor, c?.model, c?.firmwareVer, cid)
        } else if (reqMsg === 'MeterValues') {
          sendCallResult(cid, messageId, { status: 'Accepted' })
          sendMeterValues(500, cid, triggerConnId)
        } else if (reqMsg === 'DiagnosticsStatusNotification' || reqMsg === 'FirmwareStatusNotification') {
          sendCallResult(cid, messageId, { status: 'NotImplemented' })
        } else {
          sendCallResult(cid, messageId, { status: 'Rejected' })
        }
        break
      }
      default:
        sendCallResult(cid, messageId, { status: 'Accepted' })
        break
    }
  }

  // ─── 간편 단축 전문 발송 함수들 ───────────────────────────────────

  function sendBootNotification(vendor?: string, model?: string, firmware?: string, targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    return sendCall('BootNotification', {
      chargePointVendor: vendor || (c?.vendor ?? 'LS E-Link'),
      chargePointModel: model || (c?.model ?? 'ELVIS-HPC-350K'),
      chargePointSerialNumber: `${cid}-SN`,
      chargeBoxSerialNumber: cid,
      firmwareVersion: firmware || (c?.firmwareVer ?? '1.0.0-RELEASE'),
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

  function sendStatusNotification(status: string, errorCode = 'NoError', targetId?: string, connectorIdOverride?: number) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    const connId = connectorIdOverride ?? c?.activeConnectorId ?? 1
    if (c) {
      c.status = status as OcppChargePointStatus
      const conn = c.connectors.find(g => g.connectorId === connId)
      if (conn) conn.status = status as OcppChargePointStatus
    }
    return sendCall('StatusNotification', {
      connectorId: connId,
      errorCode,
      status,
      timestamp: new Date().toISOString()
    }, cid)
  }

  function sendAuthorize(idTag: string, targetId?: string) {
    const cid = targetId || selectedChargerId.value
    return sendCall('Authorize', { idTag }, cid)
  }

  function sendStartTransaction(idTag: string, meterStart?: number, targetId?: string, connectorIdOverride?: number) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    const startVal = meterStart ?? (c?.meterValueWh ?? 100000)
    const connId = connectorIdOverride ?? c?.activeConnectorId ?? 1
    if (c) c.meterValueWh = startVal

    return sendCall('StartTransaction', {
      connectorId: connId,
      idTag,
      meterStart: startVal,
      timestamp: new Date().toISOString()
    }, cid)
  }

  function sendMeterValues(incrementWh = 500, targetId?: string, connectorIdOverride?: number) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (!c) return null

    const connId = connectorIdOverride ?? c.activeConnectorId ?? 1
    const conn = c.connectors.find(g => g.connectorId === connId)
    const txId = conn?.transactionId ?? c.transactionId

    c.meterValueWh += incrementWh
    const currentSoc = conn?.socPercent ?? c.soc
    const nextSoc = Math.min(100, currentSoc + 1)
    c.soc = nextSoc
    if (conn) {
      conn.socPercent = nextSoc
      conn.accumulatedKwh = Number(((conn.accumulatedKwh || 0) + incrementWh / 1000).toFixed(3))
    }
    const nowIso = new Date().toISOString()

    return sendCall('MeterValues', {
      connectorId: connId,
      ...(txId != null ? { transactionId: txId } : {}),
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
              value: String(nextSoc),
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

  function sendStopTransaction(
    idTag = 'CARD-CORP-9901',
    reason = 'Local',
    targetId?: string,
    connectorIdOverride?: number,
    transactionIdOverride?: number | null
  ) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    const connId = connectorIdOverride ?? c?.activeConnectorId ?? 1
    const conn = c?.connectors.find(g => g.connectorId === connId)
    const txId = transactionIdOverride ?? conn?.transactionId ?? c?.transactionId

    if (txId == null) {
      addLog({
        chargerId: cid,
        direction: 'ERR',
        messageType: 0,
        messageId: 'ERR-NO-TX',
        action: 'StopTransaction',
        payload: { error: '진행 중인 transactionId가 없습니다.' },
        raw: `[STOP_FAIL] [${cid}] 활성 트랜잭션이 없어 StopTransaction을 전송할 수 없습니다.`
      })
      return null
    }

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
              value: String(conn?.socPercent ?? c?.soc ?? 80),
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
    saveChargersToStorage()
    if (c.autoHeartbeat && c.connected) {
      startHeartbeatTimer(cid)
    } else {
      stopHeartbeatTimer(cid)
    }
  }

  function setAutoHeartbeat(enabled: boolean, targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (!c) return
    c.autoHeartbeat = enabled
    saveChargersToStorage()
    if (c.autoHeartbeat && c.connected) {
      startHeartbeatTimer(cid)
    } else {
      stopHeartbeatTimer(cid)
    }
  }

  function setHeartbeatInterval(intervalSec: number, targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (!c) return
    c.heartbeatIntervalSec = Math.max(5, Number(intervalSec) || 60)
    saveChargersToStorage()
    if (c.autoHeartbeat && c.connected) {
      startHeartbeatTimer(cid)
    }
  }

  function startHeartbeatTimer(cid: string) {
    stopHeartbeatTimer(cid)
    const c = chargers.find(item => item.id === cid)
    const intervalMs = ((c?.heartbeatIntervalSec || 60)) * 1000
    const timer = setInterval(() => {
      const cur = chargers.find(item => item.id === cid)
      if (cur && cur.connected) {
        sendHeartbeat(cid)
      } else {
        stopHeartbeatTimer(cid)
      }
    }, intervalMs)
    heartbeatTimerMap.set(cid, timer)
  }

  function stopHeartbeatTimer(cid: string) {
    const timer = heartbeatTimerMap.get(cid)
    if (timer) {
      clearInterval(timer)
      heartbeatTimerMap.delete(cid)
    }
  }

  function toggleAutoBootOnConnect(targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (!c) return
    c.autoBootOnConnect = !c.autoBootOnConnect
    saveChargersToStorage()
  }

  function setAutoBootOnConnect(enabled: boolean, targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (!c) return
    c.autoBootOnConnect = enabled
    saveChargersToStorage()
  }

  function toggleAutoReply(targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (!c) return
    c.autoReply = !c.autoReply
    saveChargersToStorage()
  }

  function setAutoReply(enabled: boolean, targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (!c) return
    c.autoReply = enabled
    saveChargersToStorage()
  }

  function toggleAutoMeterValues(targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (!c) return
    c.autoMeterValues = !c.autoMeterValues
    saveChargersToStorage()
    if (c.autoMeterValues && c.connected && c.status === 'Charging') {
      startAutoMeterTimer(cid)
    } else {
      stopAutoMeterTimer(cid)
    }
  }

  function setAutoMeterValues(enabled: boolean, targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (!c) return
    c.autoMeterValues = enabled
    saveChargersToStorage()
    if (c.autoMeterValues && c.connected && c.status === 'Charging') {
      startAutoMeterTimer(cid)
    } else {
      stopAutoMeterTimer(cid)
    }
  }

  function setAutoMeterInterval(intervalSec: number, targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (!c) return
    c.autoMeterIntervalSec = Math.max(1, Number(intervalSec) || 10)
    saveChargersToStorage()
    if (c.autoMeterValues && c.connected && c.status === 'Charging') {
      startAutoMeterTimer(cid)
    }
  }

  function startAutoMeterTimer(cid: string) {
    stopAutoMeterTimer(cid)
    const c = chargers.find(item => item.id === cid)
    const intervalMs = ((c?.autoMeterIntervalSec || 10)) * 1000
    const timer = setInterval(() => {
      const cur = chargers.find(item => item.id === cid)
      if (cur && cur.connected && cur.status === 'Charging') {
        sendMeterValues(500, cid)
      } else if (!cur || !cur.connected) {
        stopAutoMeterTimer(cid)
      }
    }, intervalMs)
    autoMeterTimerMap.set(cid, timer)
  }

  function stopAutoMeterTimer(cid: string) {
    const timer = autoMeterTimerMap.get(cid)
    if (timer) {
      clearInterval(timer)
      autoMeterTimerMap.delete(cid)
    }
  }

  // ─── 완전 수동 모드 vs 표준 자동화 모드 글로벌 프리셋 ────────────────────

  const isFullManualMode = computed(() => {
    return chargers.every(c => !c.autoHeartbeat && !c.autoBootOnConnect && !c.autoReply && !c.autoMeterValues)
  })

  function setFullManualMode() {
    for (const c of chargers) {
      c.autoHeartbeat = false
      c.autoBootOnConnect = false
      c.autoReply = false
      c.autoMeterValues = false
      stopHeartbeatTimer(c.id)
      stopAutoMeterTimer(c.id)
    }
    saveChargersToStorage()
    addLog({
      chargerId: 'ALL',
      direction: 'SYS',
      messageType: 0,
      messageId: 'SYS-MANUAL-' + Date.now(),
      action: 'FULL_MANUAL_MODE',
      payload: { autoHeartbeat: false, autoBootOnConnect: false, autoReply: false, autoMeterValues: false },
      raw: '[완전 수동 모드 활성화] 모든 충전기의 자동 전문(Heartbeat 주기 발송, 접속 시 자동 기동, 서버 CALL 자동응답, 계측치 주기 발송)이 일괄 정지되었습니다. 모든 전문을 수동으로 통제할 수 있습니다.'
    })
  }

  function setFullAutoMode() {
    for (const c of chargers) {
      c.autoHeartbeat = true
      c.autoBootOnConnect = true
      c.autoReply = true
      c.autoMeterValues = true
      if (c.connected) {
        startHeartbeatTimer(c.id)
        if (c.status === 'Charging') {
          startAutoMeterTimer(c.id)
        }
      }
    }
    saveChargersToStorage()
    addLog({
      chargerId: 'ALL',
      direction: 'SYS',
      messageType: 0,
      messageId: 'SYS-AUTO-' + Date.now(),
      action: 'FULL_AUTO_MODE',
      payload: { autoHeartbeat: true, autoBootOnConnect: true, autoReply: true, autoMeterValues: true },
      raw: '[표준 자동화 모드 활성화] 모든 충전기의 자동 Heartbeat(60초), 접속 시 기동 시퀀스, 관제 CALL 자동응답(Accepted), 계측치 자동 전송이 일괄 활성화되었습니다.'
    })
  }

  // ─── 4대 회원 유형별 충전 시나리오 러너 ───────────────────────────────────

  function delay(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms))
  }

  async function runFullChargeScenario(userType = 'C', idTag = 'CARD-CORP-9901', targetId?: string) {
    const cid = targetId || selectedChargerId.value
    const c = chargers.find(item => item.id === cid)
    if (!c || c.scenarioRunning) return
    if (!c.connected) {
      connect(cid)
      await delay(800)
    }
    if (!c.connected || c.scenarioRunning) return

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
      sendBootNotification(c.vendor, c.model, c.firmwareVer || '1.0.0-RELEASE', cid)
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

    const connectorIds = (c.connectors && c.connectors.length > 0)
      ? c.connectors.map(g => g.connectorId)
      : [1, 2]

    addLog({
      chargerId: cid,
      direction: 'SYS',
      messageType: 0,
      messageId: 'SYS-BOOT',
      action: 'BOOT_SEQUENCE_START',
      payload: { stage: '4단계 기동 전문 순차 발송', connectors: connectorIds },
      raw: `[SYS] [${cid}] 기동 전문 4단계(Boot, 커넥터 ${connectorIds.map(id => `#${id}`).join(', ')} Preparing/Available, Heartbeat) 순차 발송 개시`
    })

    if (!c.connected) {
      connect(cid)
      await delay(800)
    }
    if (!c.connected) return
    sendBootNotification(c.vendor, c.model, c.firmwareVer || '1.0.0-RELEASE', cid)
    await delay(1000)
    for (const connId of connectorIds) {
      if (!c.connected) return
      sendStatusNotification('Preparing', 'NoError', cid, connId)
      await delay(300)
    }
    await delay(500)
    for (const connId of connectorIds) {
      if (!c.connected) return
      sendStatusNotification('Available', 'NoError', cid, connId)
      await delay(300)
    }
    await delay(500)
    if (!c.connected) return
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
    setAutoHeartbeat,
    setHeartbeatInterval,
    toggleAutoBootOnConnect,
    setAutoBootOnConnect,
    toggleAutoReply,
    setAutoReply,
    toggleAutoMeterValues,
    setAutoMeterValues,
    setAutoMeterInterval,
    setFullManualMode,
    setFullAutoMode,
    isFullManualMode,
    selectedIdTag,
    setSelectedIdTag,
    setActiveConnector,
    runFullChargeScenario,
    runBootSequence,
    abortScenario,
    clearLogs,
    fetchChargersFromMysql,
    isMysqlSyncing,
    isMysqlConnected,
    mysqlError,
    lastSyncedAt
  }
}
