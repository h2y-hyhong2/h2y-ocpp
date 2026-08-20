import { defineStore } from 'pinia'

export interface ChargerItem {
  id: string
  chargeBoxId: string
  stationName: string
  vendor: string
  model: string
  connectorId: number
  status: 'Available' | 'Charging' | 'Preparing' | 'Faulted' | 'Unavailable'
  powerKw: number
  voltageV: number
  currentA: number
  socPercent: number
  lastHeartbeat: string
  activeTransactionId?: number
  meterKwh?: number
}

export interface OcppLogItem {
  id: string
  timestamp: string
  direction: 'INBOUND' | 'OUTBOUND'
  chargeBoxId: string
  action: string
  messageType: number
  payloadJson: string
}

export interface MeterTimeSeriesPoint {
  time: string
  powerKw: number
  voltageV: number
  currentA: number
  socPercent: number
}

export interface CdrRecord {
  transactionId: number
  chargeBoxId: string
  stationName: string
  userTag: string
  startTime: string
  stopTime: string
  durationMinutes: number
  energyKwh: number
  avgPriceWon: number
  totalAmountWon: number
  paymentStatus: 'COMPLETED' | 'PENDING' | 'FAILED'
}

export interface ToastMessage {
  id: string
  type: 'success' | 'info' | 'error'
  title: string
  detail: string
}

export const useCsmsStore = defineStore('csms', {
  state: () => ({
    // 테마 설정
    currentTheme: 'light' as 'light' | 'cyber' | 'emerald' | 'purple' | 'amber',

    // 시스템 전체 요약 (대시보드 KPI)
    summary: {
      totalChargers: 2000,
      chargingCount: 842,
      availableCount: 1028,
      faultedCount: 98,
      preparingCount: 32,
      totalPowerKw: 28450.5,
      liveTps: 1920,
      activeSessions: 842,
      todayTotalKwh: 142850.8,
    },

    // 충전기 목록
    chargers: [
      {
        id: '1',
        chargeBoxId: 'CP-SEOUL-001',
        stationName: '서울 강남 충전소 #1',
        vendor: 'LS E-Link Power',
        model: 'ELVIS-DC-200K',
        connectorId: 1,
        status: 'Charging',
        powerKw: 148.5,
        voltageV: 750.2,
        currentA: 198.0,
        socPercent: 78,
        lastHeartbeat: '방금 전',
        activeTransactionId: 98124,
        meterKwh: 45.8
      },
      {
        id: '2',
        chargeBoxId: 'CP-SEOUL-002',
        stationName: '서울 강남 충전소 #2',
        vendor: 'LS E-Link Power',
        model: 'ELVIS-DC-200K',
        connectorId: 1,
        status: 'Charging',
        powerKw: 182.0,
        voltageV: 800.0,
        currentA: 227.5,
        socPercent: 45,
        lastHeartbeat: '방금 전',
        activeTransactionId: 98125,
        meterKwh: 32.1
      },
      {
        id: '3',
        chargeBoxId: 'CP-BUSAN-101',
        stationName: '부산 해운대 터미널 #1',
        vendor: 'LS E-Link Power',
        model: 'ELVIS-DC-350K',
        connectorId: 1,
        status: 'Available',
        powerKw: 0.0,
        voltageV: 0.0,
        currentA: 0.0,
        socPercent: 0,
        lastHeartbeat: '3초 전',
        meterKwh: 0.0
      },
      {
        id: '4',
        chargeBoxId: 'CP-INCHEON-042',
        stationName: '인천 송도 복합센터 #3',
        vendor: 'Signet EV',
        model: 'SG-200K-DUAL',
        connectorId: 1,
        status: 'Faulted',
        powerKw: 0.0,
        voltageV: 0.0,
        currentA: 0.0,
        socPercent: 0,
        lastHeartbeat: '12초 전',
        meterKwh: 0.0
      },
      {
        id: '5',
        chargeBoxId: 'CP-DAEJEON-015',
        stationName: '대전 유성 차고지 #5',
        vendor: 'Daeyoung Chaevi',
        model: 'CHV-100K',
        connectorId: 1,
        status: 'Preparing',
        powerKw: 0.0,
        voltageV: 0.0,
        currentA: 0.0,
        socPercent: 0,
        lastHeartbeat: '2초 전',
        meterKwh: 0.0
      },
      {
        id: '6',
        chargeBoxId: 'CP-GWANGJU-088',
        stationName: '광주 상무 Hub #2',
        vendor: 'LS E-Link Power',
        model: 'ELVIS-DC-350K',
        connectorId: 1,
        status: 'Available',
        powerKw: 0.0,
        voltageV: 0.0,
        currentA: 0.0,
        socPercent: 0,
        lastHeartbeat: '1초 전',
        meterKwh: 0.0
      }
    ] as ChargerItem[],

    // 1초 시계열 미터값 분석용 데이터
    selectedSessionId: 98124,
    meterTimeSeries: [
      { time: '14:20:00', powerKw: 130.2, voltageV: 730.0, currentA: 178.3, socPercent: 62 },
      { time: '14:20:05', powerKw: 135.0, voltageV: 735.2, currentA: 183.6, socPercent: 63 },
      { time: '14:20:10', powerKw: 142.1, voltageV: 742.0, currentA: 191.5, socPercent: 65 },
      { time: '14:20:15', powerKw: 145.8, voltageV: 746.5, currentA: 195.3, socPercent: 68 },
      { time: '14:20:20', powerKw: 148.5, voltageV: 750.2, currentA: 198.0, socPercent: 70 },
      { time: '14:20:25', powerKw: 151.2, voltageV: 752.1, currentA: 201.0, socPercent: 72 },
      { time: '14:20:30', powerKw: 152.0, voltageV: 753.0, currentA: 201.8, socPercent: 74 },
      { time: '14:20:35', powerKw: 150.8, voltageV: 751.5, currentA: 200.6, socPercent: 76 },
      { time: '14:20:40', powerKw: 149.3, voltageV: 750.8, currentA: 198.8, socPercent: 77 },
      { time: '14:20:45', powerKw: 148.5, voltageV: 750.2, currentA: 198.0, socPercent: 78 }
    ] as MeterTimeSeriesPoint[],

    // 충전 세션 및 과금 원장(CDR)
    cdrRecords: [
      {
        transactionId: 98124,
        chargeBoxId: 'CP-SEOUL-001',
        stationName: '서울 강남 충전소 #1',
        userTag: '100084928172',
        startTime: '2026-08-20 13:45:10',
        stopTime: '진행중 (Active)',
        durationMinutes: 35,
        energyKwh: 45.8,
        avgPriceWon: 290,
        totalAmountWon: 13282,
        paymentStatus: 'PENDING'
      },
      {
        transactionId: 98123,
        chargeBoxId: 'CP-SEOUL-002',
        stationName: '서울 강남 충전소 #2',
        userTag: '100072619481',
        startTime: '2026-08-20 12:30:00',
        stopTime: '2026-08-20 13:15:22',
        durationMinutes: 45,
        energyKwh: 68.4,
        avgPriceWon: 310,
        totalAmountWon: 21204,
        paymentStatus: 'COMPLETED'
      },
      {
        transactionId: 98122,
        chargeBoxId: 'CP-BUSAN-101',
        stationName: '부산 해운대 터미널 #1',
        userTag: '100055198273',
        startTime: '2026-08-20 11:10:05',
        stopTime: '2026-08-20 11:58:30',
        durationMinutes: 48,
        energyKwh: 82.1,
        avgPriceWon: 280,
        totalAmountWon: 22988,
        paymentStatus: 'COMPLETED'
      },
      {
        transactionId: 98121,
        chargeBoxId: 'CP-INCHEON-042',
        stationName: '인천 송도 복합센터 #3',
        userTag: '100033918274',
        startTime: '2026-08-20 10:05:12',
        stopTime: '2026-08-20 10:20:45',
        durationMinutes: 15,
        energyKwh: 14.2,
        avgPriceWon: 290,
        totalAmountWon: 4118,
        paymentStatus: 'COMPLETED'
      },
      {
        transactionId: 98120,
        chargeBoxId: 'CP-DAEJEON-015',
        stationName: '대전 유성 차고지 #5',
        userTag: '100099281745',
        startTime: '2026-08-20 09:20:00',
        stopTime: '2026-08-20 10:10:15',
        durationMinutes: 50,
        energyKwh: 52.0,
        avgPriceWon: 270,
        totalAmountWon: 14040,
        paymentStatus: 'COMPLETED'
      }
    ] as CdrRecord[],

    // 실시간 이벤트 및 통신 피드
    recentLogs: [
      {
        id: 'log-1',
        timestamp: '13:14:12.482',
        direction: 'INBOUND',
        chargeBoxId: 'CP-SEOUL-001',
        action: 'MeterValues',
        messageType: 2,
        payloadJson: '{"connectorId":1,"transactionId":98124,"powerKw":148.5,"soc":78}'
      },
      {
        id: 'log-2',
        timestamp: '13:14:10.154',
        direction: 'INBOUND',
        chargeBoxId: 'CP-INCHEON-042',
        action: 'StatusNotification',
        messageType: 2,
        payloadJson: '{"errorCode":"GroundFailure","status":"Faulted"}'
      },
      {
        id: 'log-3',
        timestamp: '13:14:08.520',
        direction: 'INBOUND',
        chargeBoxId: 'CP-DAEJEON-015',
        action: 'Authorize',
        messageType: 2,
        payloadJson: '{"idTag":"100084928172"}'
      },
      {
        id: 'log-4',
        timestamp: '13:14:08.535',
        direction: 'OUTBOUND',
        chargeBoxId: 'CP-DAEJEON-015',
        action: 'AuthorizeResponse',
        messageType: 3,
        payloadJson: '{"idTagInfo":{"status":"Accepted"}}'
      }
    ] as OcppLogItem[],

    // 토스트 알림 목록
    toasts: [] as ToastMessage[],
    isLiveConnected: true
  }),

  actions: {
    setTheme(theme: 'light' | 'cyber' | 'emerald' | 'purple' | 'amber') {
      this.currentTheme = theme
      document.documentElement.setAttribute('data-theme', theme)
    },

    showToast(type: 'success' | 'info' | 'error', title: string, detail: string) {
      const id = 'toast-' + Date.now()
      this.toasts.push({ id, type, title, detail })
      setTimeout(() => {
        this.toasts = this.toasts.filter(t => t.id !== id)
      }, 4000)
    },

    sendRemoteReset(chargeBoxId: string, resetType: 'Soft' | 'Hard') {
      this.recentLogs.unshift({
        id: 'log-' + Date.now(),
        timestamp: new Date().toLocaleTimeString() + '.' + Math.floor(Math.random() * 900 + 100),
        direction: 'OUTBOUND',
        chargeBoxId,
        action: 'Reset',
        messageType: 2,
        payloadJson: JSON.stringify({ type: resetType })
      })
      this.showToast('success', 'Reset 명령 하향 송신', `[${chargeBoxId}] ${resetType} Reset 명령이 게이트웨이로 전송되었습니다.`)
    },

    sendUnlockConnector(chargeBoxId: string, connectorId: number) {
      this.recentLogs.unshift({
        id: 'log-' + Date.now(),
        timestamp: new Date().toLocaleTimeString() + '.' + Math.floor(Math.random() * 900 + 100),
        direction: 'OUTBOUND',
        chargeBoxId,
        action: 'UnlockConnector',
        messageType: 2,
        payloadJson: JSON.stringify({ connectorId })
      })
      this.showToast('info', 'Unlock Connector 송신', `[${chargeBoxId}] 커넥터 #${connectorId} 잠금 해제 요청 전송 완료.`)
    },

    sendRemoteStart(chargeBoxId: string, idTag: string) {
      this.recentLogs.unshift({
        id: 'log-' + Date.now(),
        timestamp: new Date().toLocaleTimeString() + '.' + Math.floor(Math.random() * 900 + 100),
        direction: 'OUTBOUND',
        chargeBoxId,
        action: 'RemoteStartTransaction',
        messageType: 2,
        payloadJson: JSON.stringify({ connectorId: 1, idTag })
      })
      this.showToast('success', '충전 시작(RemoteStart) 송신', `[${chargeBoxId}] RFID 태그 (${idTag}) 원격 시작 명령 전송.`)
    },

    sendRemoteStop(chargeBoxId: string, transactionId: number) {
      this.recentLogs.unshift({
        id: 'log-' + Date.now(),
        timestamp: new Date().toLocaleTimeString() + '.' + Math.floor(Math.random() * 900 + 100),
        direction: 'OUTBOUND',
        chargeBoxId,
        action: 'RemoteStopTransaction',
        messageType: 2,
        payloadJson: JSON.stringify({ transactionId })
      })
      this.showToast('info', '충전 중지(RemoteStop) 송신', `[${chargeBoxId}] 트랜잭션 #${transactionId} 원격 종료 명령 전송.`)
    }
  }
})
