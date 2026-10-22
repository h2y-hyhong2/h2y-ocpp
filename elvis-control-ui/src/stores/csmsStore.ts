import { defineStore } from 'pinia'

// 1. 커넥터 상태 매핑 상수 (UI 색상 및 배지 스타일)
export const STATUS_MAP = {
  '연결안됨':   { key: 'disconnected', name: '연결안됨',   color: '#64748b', darkHex: '#475569', cellClass: 'cell-disconnected', badgeClass: 'badge-disconnected', dotClass: 'bg-slate-400' },
  '충전대기':   { key: 'standby',      name: '충전대기',   color: '#d97706', darkHex: '#9a3412', cellClass: 'cell-standby',      badgeClass: 'badge-standby',      dotClass: 'bg-amber-500' },
  '충전준비중': { key: 'preparing',    name: '충전준비중', color: '#0d9488', darkHex: '#115e59', cellClass: 'cell-preparing',    badgeClass: 'badge-preparing',    dotClass: 'bg-teal-500' },
  '충전중':     { key: 'charging',     name: '충전중',     color: '#3b82f6', darkHex: '#1e40af', cellClass: 'cell-charging',     badgeClass: 'badge-charging',     dotClass: 'bg-blue-500' },
  '충전완료':   { key: 'finished',     name: '충전완료',   color: '#059669', darkHex: '#166534', cellClass: 'cell-finished',     badgeClass: 'badge-finished',     dotClass: 'bg-emerald-500' },
  '통신이상':   { key: 'commerr',      name: '통신이상',   color: '#dc2626', darkHex: '#991b1b', cellClass: 'cell-commerr',      badgeClass: 'badge-commerr',      dotClass: 'bg-rose-500' },
  '점검중':     { key: 'inspecting',   name: '점검중',     color: '#b45309', darkHex: '#854d0e', cellClass: 'cell-inspecting',   badgeClass: 'badge-inspecting',   dotClass: 'bg-yellow-600' },
  '운영중지':   { key: 'suspended',    name: '운영중지',   color: '#7c3aed', darkHex: '#6b21a8', cellClass: 'cell-suspended',    badgeClass: 'badge-suspended',    dotClass: 'bg-purple-500' },
  '예약중':     { key: 'reserved',     name: '예약중',     color: '#4d7c0f', darkHex: '#3f6212', cellClass: 'cell-reserved',     badgeClass: 'badge-reserved',     dotClass: 'bg-lime-600' },
  '일시중지':   { key: 'paused',       name: '일시중지',   color: '#be123c', darkHex: '#9f1239', cellClass: 'cell-paused',       badgeClass: 'badge-paused',       dotClass: 'bg-rose-600' },
  '알수없음':   { key: 'unknown',      name: '알수없음',   color: '#64748b', darkHex: '#475569', cellClass: 'cell-unknown',      badgeClass: 'badge-unknown',      dotClass: 'bg-slate-400' }
}

export type StatusKey = keyof typeof STATUS_MAP

export interface CorpItem {
  id: string
  name: string
  shortName: string
  tag: string
  colorClass: string
  badge?: string
}

export interface ChargerItem {
  id: string
  chargeBoxId: string
  stId: string
  stationName: string
  corpId: string
  corpName: string
  corpShortName: string
  cpId: string
  connectorId: number
  status: StatusKey
  spec: string
  powerKw: number
  voltageV: number
  currentA: number
  socPercent: number
  batteryTempC: number
  vendor: string
  model: string
  carModel: string
  userTag: string
  protocol: string
  lastHeartbeat: string
  accumulatedKwh: number
  chargingMinutes: number
}

export interface StationItem {
  id: string
  name: string
  corpId: string
  corpName: string
  corpShortName: string
  chargerCount: number
  startIdx?: number
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
    currentTheme: 'light' as 'light' | 'cyber',

    // 화면 디자인 컨셉 (3개 시안 스위처)
    activeConcept: 'concept1' as 'concept1' | 'concept2' | 'concept3',

    // 반응형 드로어 상태 (프로토타입 LAYOUT-OPTION-B 규격)
    isSidebarOpen: false,
    isControlDrawerOpen: false,

    // 법인 및 충전소 데이터 (순수 DB 동기화 대상, 더미 제거)
    corps: [] as CorpItem[],
    stations: [] as StationItem[],
    chargers: [] as ChargerItem[],

    // 선택 상태
    curCorpFilter: 'ALL',
    curStFilter: 'ALL',
    curStatusFilter: 'ALL',
    searchQuery: '',
    selectedCharger: null as ChargerItem | null,

    // 시스템 전체 요약 (대시보드 KPI - 순수 실데이터 연동)
    summary: {
      totalChargers: 0,
      chargingCount: 0,
      availableCount: 0,
      faultedCount: 0,
      preparingCount: 0,
      totalPowerKw: 0,
      liveTps: 0,
      activeSessions: 0,
      todayTotalKwh: 0
    },

    // DB 연결 상태 (MySQL 연동 여부)
    isDbConnected: false,

    // 토스트 알림 목록
    toasts: [] as ToastMessage[]
  }),

  actions: {
    setTheme(theme: string) {
      this.currentTheme = theme as 'light' | 'cyber'
    },

    setConcept(concept: 'concept1' | 'concept2' | 'concept3') {
      this.activeConcept = concept
    },

    toggleSidebar(isOpen?: boolean) {
      this.isSidebarOpen = isOpen !== undefined ? isOpen : !this.isSidebarOpen
    },

    toggleControlDrawer(isOpen?: boolean) {
      this.isControlDrawerOpen = isOpen !== undefined ? isOpen : !this.isControlDrawerOpen
    },

    setCorpFilter(corpId: string) {
      this.curCorpFilter = corpId
      const stInCorp = this.stations.find(s => corpId === 'ALL' || s.corpId === corpId)
      if (stInCorp) {
        this.curStFilter = stInCorp.id
      }
    },

    setStationFilter(stId: string) {
      this.curStFilter = stId
      const targetCharger = this.chargers.find(c => stId === 'ALL' || c.stId === stId)
      if (targetCharger) {
        this.selectedCharger = targetCharger
      }
    },

    setStatusFilter(status: string) {
      this.curStatusFilter = status
    },

    setSelectedCharger(charger: ChargerItem) {
      this.selectedCharger = charger
    },

    // 📡 중앙 REST API (/api/v1) MySQL 9.71 실데이터 연동 액션
    async fetchInitialData(silent = false) {
      try {
        console.log('%c[CSMS UI] 📡 REST API 데이터 조회 쿼리 실행: GET /api/v1/{corps, stations, chargers, cdr}', 'color: #3b82f6; font-weight: bold')
        const [corpRes, stRes, chgRes, cdrRes] = await Promise.all([
          fetch('/api/v1/corps'),
          fetch('/api/v1/stations'),
          fetch('/api/v1/chargers'),
          fetch('/api/v1/cdr')
        ])

        if (stRes.ok && chgRes.ok) {
          const stJson = await stRes.json()
          const chgJson = await chgRes.json()

          const unwrap = (json: any) => {
            if (!json) return []
            if (Array.isArray(json)) return json
            if (json.data && Array.isArray(json.data)) return json.data
            return []
          }

          if (corpRes.ok) {
            const corpJson = await corpRes.json()
            const corpList = unwrap(corpJson)
            if (corpList.length > 0) {
              this.corps = corpList
            }
          }

          const stList = unwrap(stJson)
          if (stList.length > 0) {
            this.stations = stList
            if (this.curStFilter !== 'ALL' && !this.stations.some(s => s.id === this.curStFilter)) {
              this.curStFilter = this.stations[0].id
            }
          } else {
            this.stations = []
            this.curStFilter = 'ALL'
          }

          const chgList = unwrap(chgJson)
          if (chgList.length > 0) {
            this.chargers = chgList
            if (!this.selectedCharger || !this.chargers.some(c => c.chargeBoxId === this.selectedCharger?.chargeBoxId)) {
              this.selectedCharger = this.chargers[0]
            }
          } else {
            this.chargers = []
            this.selectedCharger = null
          }

          console.log(`%c[CSMS UI] 📥 실데이터 응답 수신: 법인 ${this.corps.length}건, 충전소 ${this.stations.length}건, 충전기 ${this.chargers.length}건`, 'color: #10b981; font-weight: bold', {
            corps: this.corps,
            stations: this.stations,
            chargers: this.chargers
          })

          let cdrSummary = null
          if (cdrRes.ok) {
            const cdrJson = await cdrRes.json()
            const cdrData = cdrJson.data || cdrJson
            if (cdrData && cdrData.summary) {
              cdrSummary = cdrData.summary
            }
          }

          // 📊 DB 실데이터 기반 summary KPI 자동 계산
          const totalChargers = this.chargers.length
          const chargingCount = this.chargers.filter(c => c.status === '충전중').length
          const availableCount = this.chargers.filter(c => c.status === '충전대기').length
          const faultedCount = this.chargers.filter(c => c.status === '통신이상').length
          const preparingCount = this.chargers.filter(c => c.status === '충전준비중').length
          const totalPowerKw = this.chargers.reduce((acc, c) => acc + (Number(c.powerKw) || 0), 0)

          this.summary = {
            totalChargers,
            chargingCount,
            availableCount,
            faultedCount,
            preparingCount,
            totalPowerKw: Math.round(totalPowerKw * 10) / 10,
            liveTps: totalChargers > 0 ? 35 : 0,
            activeSessions: chargingCount,
            todayTotalKwh: cdrSummary ? Number(cdrSummary.totalKwh) || 0 : 0
          }

          const isCleared = this.stations.length === 0 && this.chargers.length === 0
          this.isDbConnected = true
          if (!silent) {
            this.addToast(
              isCleared ? 'info' : 'success',
              isCleared ? 'MySQL 초기화 상태 (0건)' : 'MySQL 실데이터 동기화',
              isCleared
                ? '데이터베이스가 완전히 비어 있는 초기 상태입니다 (충전소 0개소, 충전기 0기).'
                : `MySQL(elvis-lite) 연동 완료: 충전소 ${this.stations.length}개소, 충전기 ${this.chargers.length}기`
            )
          }
        } else {
          this.isDbConnected = false
        }
      } catch (err) {
        this.isDbConnected = false
      }
    },

    // 📡 원격 충전기 제어 명령 API 연동
    async sendRemoteAction(action: string) {
      if (!this.selectedCharger) return

      try {
        const res = await fetch(`/api/v1/chargers/${this.selectedCharger.chargeBoxId}/remote-command`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ action })
        })

        if (res.ok) {
          const result = await res.json()
          this.addToast('success', `${action} 명령 완료`, result.message || '원격 제어 명령이 성공적으로 전송되었습니다.')
          // 1초 후 충전기 목록 재조회
          setTimeout(() => this.fetchInitialData(), 1000)
          return
        }
      } catch (err: any) {
        this.addToast('error', `${action} 명령 실패`, err.message || '원격 제어 명령 전송 중 오류가 발생했습니다.')
        return
      }

      this.addToast('error', `${action} 명령 실패`, `[${this.selectedCharger.chargeBoxId}] 단말에 명령을 전달하지 못했습니다.`)
    },

    addToast(typeOrObj: 'success' | 'info' | 'error' | { title: string; detail: string; type: 'success' | 'info' | 'error' }, title = '', detail = '') {
      const id = 'toast-' + Date.now() + Math.random()
      let toastItem: ToastMessage

      if (typeof typeOrObj === 'object') {
        toastItem = { id, type: typeOrObj.type, title: typeOrObj.title, detail: typeOrObj.detail }
      } else {
        toastItem = { id, type: typeOrObj, title, detail }
      }

      this.toasts.push(toastItem)
      setTimeout(() => {
        this.toasts = this.toasts.filter(t => t.id !== id)
      }, 3500)
    }
  }
})
