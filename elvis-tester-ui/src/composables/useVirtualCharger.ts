import { ref, reactive } from 'vue'
import { MESSAGE_TYPE, type OcppLogEntry, type VirtualChargerState } from '../types/ocpp'

export function useVirtualCharger() {
  const state = reactive<VirtualChargerState>({
    chargeBoxId: '00000001',
    wsUrl: 'ws://127.0.0.1:18080/ocpp',
    connected: false,
    connecting: false,
    status: 'Available',
    connectorId: 1,
    transactionId: null,
    meterValueWh: 100000,
    soc: 25,
    voltage: 380.0,
    current: 125.0,
    powerKw: 47.5,
    heartbeatIntervalSec: 60,
    autoHeartbeat: false,
    autoMeterValues: false,
    autoMeterIntervalSec: 5,
    scenarioRunning: false,
    scenarioStep: '',
    scenarioProgress: 0
  })

  const logs = ref<OcppLogEntry[]>([])
  let socket: WebSocket | null = null
  let heartbeatTimer: any = null
  let autoMeterTimer: any = null
  let isScenarioAborted = false
  const pendingRequests = new Map<string, { action: string; sentAt: number }>()

  function generateMsgId(): string {
    return 'msg-' + Math.random().toString(36).substring(2, 10) + '-' + Date.now().toString(36)
  }

  function addLog(entry: Omit<OcppLogEntry, 'id' | 'timestamp'>) {
    const newLog: OcppLogEntry = {
      id: 'log-' + Math.random().toString(36).substring(2, 9),
      timestamp: new Date().toLocaleTimeString('ko-KR', { hour12: false, fractionalSecondDigits: 3 }),
      ...entry
    }
    logs.value.unshift(newLog)
    if (logs.value.length > 500) {
      logs.value.pop()
    }
  }

  function connect() {
    if (state.connected || state.connecting) return

    const fullUrl = `${state.wsUrl.replace(/\/$/, '')}/${encodeURIComponent(state.chargeBoxId)}`
    state.connecting = true

    try {
      socket = new WebSocket(fullUrl)

      socket.onopen = () => {
        state.connected = true
        state.connecting = false
        addLog({
          direction: 'IN',
          messageType: 0,
          messageId: 'SYSTEM',
          action: 'WS_CONNECTED',
          payload: { url: fullUrl, chargeBoxId: state.chargeBoxId },
          raw: `[CONNECTED] WebSocket 연결 수립: ${fullUrl}`
        })
        if (state.autoHeartbeat) {
          startHeartbeatTimer()
        }
      }

      socket.onmessage = (event: MessageEvent) => {
        handleIncomingMessage(event.data)
      }

      socket.onerror = (err: Event) => {
        state.connecting = false
        addLog({
          direction: 'ERR',
          messageType: MESSAGE_TYPE.CALLERROR,
          messageId: 'SYSTEM',
          action: 'WS_ERROR',
          payload: { error: '소켓 에러 발생' },
          raw: `[ERROR] WebSocket 연결 실패 또는 에러 발생`
        })
      }

      socket.onclose = (event: CloseEvent) => {
        state.connected = false
        state.connecting = false
        stopHeartbeatTimer()
        stopAutoMeterTimer()
        abortScenario()
        addLog({
          direction: 'IN',
          messageType: 0,
          messageId: 'SYSTEM',
          action: 'WS_CLOSED',
          payload: { code: event.code, reason: event.reason },
          raw: `[CLOSED] 소켓 종료 (Code: ${event.code}, Reason: ${event.reason || 'None'})`
        })
      }
    } catch (e: any) {
      state.connecting = false
      addLog({
        direction: 'ERR',
        messageType: 0,
        messageId: 'SYSTEM',
        action: 'CONNECT_EXCEPTION',
        payload: { message: e.message },
        raw: `[EXCEPTION] 소켓 초기화 실패: ${e.message}`
      })
    }
  }

  function disconnect() {
    stopHeartbeatTimer()
    stopAutoMeterTimer()
    abortScenario()
    if (socket) {
      socket.close(1000, 'User Disconnected')
      socket = null
    }
    state.connected = false
    state.connecting = false
  }

  function sendCall(action: string, payload: any): string | null {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      addLog({
        direction: 'ERR',
        messageType: 0,
        messageId: 'ERR',
        action,
        payload: { error: '소켓 미연결 상태' },
        raw: `[SEND_FAIL] WebSocket이 열려있지 않아 ${action} 전송 불가`
      })
      return null
    }

    const messageId = generateMsgId()
    const rawPacket = JSON.stringify([MESSAGE_TYPE.CALL, messageId, action, payload])

    pendingRequests.set(messageId, {
      action,
      sentAt: performance.now()
    })

    socket.send(rawPacket)

    addLog({
      direction: 'OUT',
      messageType: MESSAGE_TYPE.CALL,
      messageId,
      action,
      payload,
      raw: rawPacket
    })

    return messageId
  }

  function handleIncomingMessage(raw: string) {
    try {
      const parsed = JSON.parse(raw)
      if (!Array.isArray(parsed) || parsed.length < 3) {
        addLog({
          direction: 'IN',
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
        const payload = parsed[2] || {}
        let latencyMs: number | undefined
        let action: string | undefined

        const pending = pendingRequests.get(messageId)
        if (pending) {
          latencyMs = Math.round(performance.now() - pending.sentAt)
          action = pending.action
          pendingRequests.delete(messageId)

          // 액션별 응답 처리 로직
          handleActionConf(action, payload)
        }

        addLog({
          direction: 'IN',
          messageType,
          messageId,
          action: action ? `${action}.conf` : 'CALLRESULT',
          payload,
          raw,
          latencyMs
        })
      } else if (messageType === MESSAGE_TYPE.CALL) {
        // CSMS ➔ 충전기 하향 원격 제어 수신 (RemoteStartTransaction, Reset 등)
        const action = parsed[2]
        const payload = parsed[3] || {}

        addLog({
          direction: 'IN',
          messageType,
          messageId,
          action: `${action}.req`,
          payload,
          raw
        })

        // 표준 기본 회신 (Accepted)
        sendCallResult(messageId, { status: 'Accepted' })
      } else if (messageType === MESSAGE_TYPE.CALLERROR) {
        const errorCode = parsed[2]
        const errorDesc = parsed[3]
        addLog({
          direction: 'ERR',
          messageType,
          messageId,
          action: 'CALLERROR',
          payload: { errorCode, errorDesc, errorDetails: parsed[4] },
          raw
        })
      }
    } catch (e: any) {
      addLog({
        direction: 'ERR',
        messageType: 0,
        messageId: 'PARSE_ERR',
        payload: { error: e.message },
        raw
      })
    }
  }

  function sendCallResult(messageId: string, payload: any) {
    if (!socket || socket.readyState !== WebSocket.OPEN) return
    const rawPacket = JSON.stringify([MESSAGE_TYPE.CALLRESULT, messageId, payload])
    socket.send(rawPacket)
    addLog({
      direction: 'OUT',
      messageType: MESSAGE_TYPE.CALLRESULT,
      messageId,
      action: 'CALLRESULT (ACK)',
      payload,
      raw: rawPacket
    })
  }

  function handleActionConf(action: string, payload: any) {
    switch (action) {
      case 'BootNotification':
        if (payload.status === 'Accepted' && payload.interval) {
          state.heartbeatIntervalSec = payload.interval
        }
        break
      case 'StartTransaction':
        if (payload.transactionId) {
          state.transactionId = payload.transactionId
          state.status = 'Charging'
        }
        break
      case 'StopTransaction':
        state.transactionId = null
        state.status = 'Available'
        stopAutoMeterTimer()
        break
      case 'StatusNotification':
        break
    }
  }

  // ─── 간편 단축 액션 메서드 ───────────────────────────────────────

  function sendBootNotification(vendor = 'LS E-Link', model = 'ELVIS-HPC-350K', firmware = 'v1.0.0') {
    return sendCall('BootNotification', {
      chargePointVendor: vendor,
      chargePointModel: model,
      chargePointSerialNumber: state.chargeBoxId + '-SN',
      chargeBoxSerialNumber: state.chargeBoxId,
      firmwareVersion: firmware,
      iccid: '8982000000000000001',
      imsi: '450000000000001',
      meterType: 'ELVIS-DIGITAL-01',
      meterSerialNumber: 'MTR-' + state.chargeBoxId
    })
  }

  function sendHeartbeat() {
    return sendCall('Heartbeat', {})
  }

  function sendStatusNotification(status: string, errorCode = 'NoError') {
    state.status = status
    return sendCall('StatusNotification', {
      connectorId: state.connectorId,
      errorCode,
      status,
      timestamp: new Date().toISOString()
    })
  }

  function sendAuthorize(idTag: string) {
    return sendCall('Authorize', {
      idTag
    })
  }

  function sendStartTransaction(idTag: string, meterStart = state.meterValueWh) {
    state.meterValueWh = meterStart
    state.status = 'Preparing'
    return sendCall('StartTransaction', {
      connectorId: state.connectorId,
      idTag,
      meterStart,
      timestamp: new Date().toISOString()
    })
  }

  function sendMeterValues(incrementWh = 500) {
    state.meterValueWh += incrementWh
    if (state.soc < 100) {
      state.soc = Math.min(100, state.soc + 1)
    }

    return sendCall('MeterValues', {
      connectorId: state.connectorId,
      transactionId: state.transactionId || undefined,
      meterValue: [
        {
          timestamp: new Date().toISOString(),
          sampledValue: [
            {
              value: String(state.meterValueWh),
              context: 'Sample.Periodic',
              format: 'Raw',
              measurand: 'Energy.Active.Import.Register',
              unit: 'Wh'
            },
            {
              value: String(state.soc),
              context: 'Sample.Periodic',
              format: 'Raw',
              measurand: 'SoC',
              unit: 'Percent'
            },
            {
              value: String(state.voltage.toFixed(1)),
              context: 'Sample.Periodic',
              format: 'Raw',
              measurand: 'Voltage',
              unit: 'V'
            },
            {
              value: String(state.current.toFixed(1)),
              context: 'Sample.Periodic',
              format: 'Raw',
              measurand: 'Current.Import',
              unit: 'A'
            },
            {
              value: String(state.powerKw.toFixed(1)),
              context: 'Sample.Periodic',
              format: 'Raw',
              measurand: 'Power.Active.Import',
              unit: 'kW'
            }
          ]
        }
      ]
    })
  }

  function sendStopTransaction(idTag: string, reason = 'Local') {
    return sendCall('StopTransaction', {
      transactionId: state.transactionId || 0,
      idTag,
      meterStop: state.meterValueWh,
      timestamp: new Date().toISOString(),
      reason
    })
  }

  function sendDataTransfer(vendorId = 'LS E-Link', messageId = 'MeterValues', data: any = {}) {
    return sendCall('DataTransfer', {
      vendorId,
      messageId,
      data: typeof data === 'string' ? data : JSON.stringify(data)
    })
  }

  // ─── 타이머 및 자동화 ──────────────────────────────────────────

  function toggleAutoHeartbeat(enabled: boolean) {
    state.autoHeartbeat = enabled
    if (enabled) {
      startHeartbeatTimer()
    } else {
      stopHeartbeatTimer()
    }
  }

  function startHeartbeatTimer() {
    stopHeartbeatTimer()
    if (!state.connected) return
    heartbeatTimer = setInterval(() => {
      if (state.connected) {
        sendHeartbeat()
      }
    }, Math.max(5000, state.heartbeatIntervalSec * 1000))
  }

  function stopHeartbeatTimer() {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  function toggleAutoMeterValues(enabled: boolean, intervalSec = 5) {
    state.autoMeterValues = enabled
    state.autoMeterIntervalSec = intervalSec
    if (enabled) {
      startAutoMeterTimer()
    } else {
      stopAutoMeterTimer()
    }
  }

  function startAutoMeterTimer() {
    stopAutoMeterTimer()
    if (!state.connected) return
    autoMeterTimer = setInterval(() => {
      if (state.connected && state.transactionId) {
        sendMeterValues(500)
      }
    }, Math.max(1000, state.autoMeterIntervalSec * 1000))
  }

  function stopAutoMeterTimer() {
    if (autoMeterTimer) {
      clearInterval(autoMeterTimer)
      autoMeterTimer = null
    }
    state.autoMeterValues = false
  }

  // ─── 원클릭 정상 충전 시나리오 러너 ───────────────────────────────────

  function delay(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms))
  }

  async function runFullChargeScenario(idTag = 'CARD-CORP-9901') {
    if (!state.connected || state.scenarioRunning) return
    state.scenarioRunning = true
    state.scenarioProgress = 0
    isScenarioAborted = false

    try {
      // Step 1: BootNotification
      state.scenarioStep = '1/8: BootNotification 발송 및 승인 대기'
      state.scenarioProgress = 12
      sendBootNotification()
      await delay(1200)
      if (isScenarioAborted) return

      // Step 2: StatusNotification (Preparing - 차량 결착)
      state.scenarioStep = '2/8: 커넥터 결착 감지 (StatusNotification: Preparing)'
      state.scenarioProgress = 25
      sendStatusNotification('Preparing')
      await delay(1000)
      if (isScenarioAborted) return

      // Step 3: Authorize
      state.scenarioStep = `3/8: 회원 카드 태깅 및 인증 요청 (Authorize: ${idTag})`
      state.scenarioProgress = 37
      sendAuthorize(idTag)
      await delay(1200)
      if (isScenarioAborted) return

      // Step 4: StartTransaction
      state.scenarioStep = '4/8: 충전 트랜잭션 개시 (StartTransaction)'
      state.scenarioProgress = 50
      sendStartTransaction(idTag, state.meterValueWh)
      await delay(1500)
      if (isScenarioAborted) return

      // Step 5: MeterValues 3회 연속 전송
      for (let i = 1; i <= 3; i++) {
        state.scenarioStep = `5/8: 실시간 충전 계측 전송 (${i}/3회, MeterValues)`
        state.scenarioProgress = 50 + (i * 9)
        sendMeterValues(800)
        await delay(1500)
        if (isScenarioAborted) return
      }

      // Step 6: StatusNotification (Finishing)
      state.scenarioStep = '6/8: 충전 완료 마감 (StatusNotification: Finishing)'
      state.scenarioProgress = 85
      sendStatusNotification('Finishing')
      await delay(1000)
      if (isScenarioAborted) return

      // Step 7: StopTransaction
      state.scenarioStep = '7/8: 거래 세션 마감 정산 (StopTransaction)'
      state.scenarioProgress = 95
      sendStopTransaction(idTag, 'Local')
      await delay(1200)
      if (isScenarioAborted) return

      // Step 8: StatusNotification (Available)
      state.scenarioStep = '8/8: 커넥터 분리 및 대기 복귀 (StatusNotification: Available)'
      state.scenarioProgress = 100
      sendStatusNotification('Available')
      await delay(500)

      state.scenarioStep = '정상 충전 라이프사이클 시나리오 완주 성공'
    } catch (e: any) {
      state.scenarioStep = `시나리오 실행 중 오류: ${e.message}`
    } finally {
      state.scenarioRunning = false
      setTimeout(() => {
        if (!state.scenarioRunning && state.scenarioProgress === 100) {
          state.scenarioProgress = 0
          state.scenarioStep = ''
        }
      }, 5000)
    }
  }

  function abortScenario() {
    isScenarioAborted = true
    state.scenarioRunning = false
    state.scenarioStep = '시나리오 중단됨'
    setTimeout(() => {
      state.scenarioStep = ''
      state.scenarioProgress = 0
    }, 2500)
  }

  function clearLogs() {
    logs.value = []
  }

  return {
    state,
    logs,
    connect,
    disconnect,
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
    abortScenario,
    clearLogs
  }
}
