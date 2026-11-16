import { defineStore } from 'pinia'
import { authFetch } from '@/utils/apiClient'

export interface StatusMeta {
  key: string
  name: string
  color: string
  darkHex: string
  cellClass: string
  badgeClass: string
  dotClass: string
}

// 커넥터 상태 매핑 상수 (한글 표준 상태 + OCPP 영문 상태 동시 지원)
export const STATUS_MAP: Record<string, StatusMeta> = {
  '연결안됨':   { key: 'disconnected', name: '연결안됨',   color: '#64748b', darkHex: '#475569', cellClass: 'cell-disconnected', badgeClass: 'badge-disconnected', dotClass: 'bg-slate-400' },
  '충전대기':   { key: 'standby',      name: '충전대기',   color: '#d97706', darkHex: '#9a3412', cellClass: 'cell-standby',      badgeClass: 'badge-standby',      dotClass: 'bg-amber-500' },
  '충전준비중': { key: 'preparing',    name: '충전준비중', color: '#0d9488', darkHex: '#115e59', cellClass: 'cell-preparing',    badgeClass: 'badge-preparing',    dotClass: 'bg-teal-500' },
  '충전중':     { key: 'charging',     name: '충전중',     color: '#3b82f6', darkHex: '#1e40af', cellClass: 'cell-charging',     badgeClass: 'badge-charging',     dotClass: 'bg-blue-500' },
  '충전완료':   { key: 'finished',     name: '충전완료',   color: '#059669', darkHex: '#166534', cellClass: 'cell-finished',     badgeClass: 'badge-finished',     dotClass: 'bg-emerald-500' },
  '고장':       { key: 'faulted',      name: '고장',       color: '#dc2626', darkHex: '#991b1b', cellClass: 'cell-commerr',      badgeClass: 'badge-faulted',      dotClass: 'bg-rose-500' },
  '통신이상':   { key: 'commerr',      name: '통신이상',   color: '#dc2626', darkHex: '#991b1b', cellClass: 'cell-commerr',      badgeClass: 'badge-commerr',      dotClass: 'bg-rose-500' },
  '점검중':     { key: 'inspecting',   name: '점검중',     color: '#b45309', darkHex: '#854d0e', cellClass: 'cell-inspecting',   badgeClass: 'badge-inspecting',   dotClass: 'bg-yellow-600' },
  '운영중지':   { key: 'suspended',    name: '운영중지',   color: '#7c3aed', darkHex: '#6b21a8', cellClass: 'cell-suspended',    badgeClass: 'badge-suspended',    dotClass: 'bg-purple-500' },
  '예약중':     { key: 'reserved',     name: '예약중',     color: '#4d7c0f', darkHex: '#3f6212', cellClass: 'cell-reserved',     badgeClass: 'badge-reserved',     dotClass: 'bg-lime-600' },
  '일시중지':   { key: 'paused',       name: '일시중지',   color: '#be123c', darkHex: '#9f1239', cellClass: 'cell-paused',       badgeClass: 'badge-paused',       dotClass: 'bg-rose-600' },
  '알수없음':   { key: 'unknown',      name: '알수없음',   color: '#64748b', darkHex: '#475569', cellClass: 'cell-unknown',      badgeClass: 'badge-unknown',      dotClass: 'bg-slate-400' },

  // OCPP 표준 영문 상태 매핑
  'Available':     { key: 'standby',    name: '충전대기',   color: '#d97706', darkHex: '#9a3412', cellClass: 'cell-standby',    badgeClass: 'badge-standby',    dotClass: 'bg-amber-500' },
  'Preparing':     { key: 'preparing',  name: '충전준비중', color: '#0d9488', darkHex: '#115e59', cellClass: 'cell-preparing',  badgeClass: 'badge-preparing',  dotClass: 'bg-teal-500' },
  'Charging':      { key: 'charging',   name: '충전중',     color: '#3b82f6', darkHex: '#1e40af', cellClass: 'cell-charging',   badgeClass: 'badge-charging',   dotClass: 'bg-blue-500' },
  'Finishing':     { key: 'finished',   name: '충전완료',   color: '#059669', darkHex: '#166534', cellClass: 'cell-finished',   badgeClass: 'badge-finished',   dotClass: 'bg-emerald-500' },
  'Faulted':       { key: 'faulted',    name: '고장',       color: '#dc2626', darkHex: '#991b1b', cellClass: 'cell-commerr',    badgeClass: 'badge-faulted',    dotClass: 'bg-rose-500' },
  'Unavailable':   { key: 'inspecting', name: '점검중',     color: '#b45309', darkHex: '#854d0e', cellClass: 'cell-inspecting', badgeClass: 'badge-inspecting', dotClass: 'bg-yellow-600' },
  'SuspendedEV':   { key: 'paused',     name: '일시중지',   color: '#be123c', darkHex: '#9f1239', cellClass: 'cell-paused',     badgeClass: 'badge-paused',     dotClass: 'bg-rose-600' },
  'SuspendedEVSE': { key: 'paused',     name: '일시중지',   color: '#be123c', darkHex: '#9f1239', cellClass: 'cell-paused',     badgeClass: 'badge-paused',     dotClass: 'bg-rose-600' }
}

export const PRIMARY_STATUS_KEYS = [
  '연결안됨',
  '충전대기',
  '충전준비중',
  '충전중',
  '충전완료',
  '고장',
  '통신이상',
  '점검중',
  '운영중지',
  '예약중',
  '일시중지',
  '알수없음'
] as const

export type StatusKey = keyof typeof STATUS_MAP
export type ThemeType = 'light' | 'cyber' | 'emerald' | 'purple' | 'amber'

export function getStatusMeta(status?: string | null): StatusMeta {
  if (status && STATUS_MAP[status]) {
    return STATUS_MAP[status]
  }
  return STATUS_MAP['알수없음']
}

export interface CorpItem {
  id: string
  name: string
  shortName: string
  tag: string
  colorClass: string
  badge?: string
}

export interface ConnectorDetail {
  connectorId: number
  status: string
  powerKw: number
  voltageV: number
  currentA: number
  socPercent: number
  batteryTempC: number
  carModel?: string | null
  userTag?: string | null
  accumulatedKwh: number
  chargingMinutes: number
  lastHeartbeat: string
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
  connectors?: ConnectorDetail[]
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
    // 테마 설정 (라이트 기본)
    currentTheme: 'light' as ThemeType,

    // 반응형 드로어 상태
    isSidebarOpen: false,
    isControlDrawerOpen: false,

    // 법인 및 충전소 데이터 (순수 DB 동기화 대상)
    corps: [] as CorpItem[],
    stations: [] as StationItem[],
    chargers: [] as ChargerItem[],

    // 선택 상태
    curCorpFilter: 'ALL',
    curStFilter: 'ALL',
    curStatusFilter: 'ALL',
    searchQuery: '',
    selectedCharger: null as ChargerItem | null,

    // 시스템 전체 요약 (대시보드 KPI - 실데이터 연동)
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

    // DB 연결 상태
    isDbConnected: false,

    // 토스트 알림 목록
    toasts: [] as ToastMessage[]
  }),

  actions: {
    setTheme(theme: string) {
      this.currentTheme = theme as ThemeType
    },

    toggleSidebar(isOpen?: boolean) {
      this.isSidebarOpen = isOpen !== undefined ? isOpen : !this.isSidebarOpen
    },

    toggleControlDrawer(isOpen?: boolean) {
      this.isControlDrawerOpen = isOpen !== undefined ? isOpen : !this.isControlDrawerOpen
    },

    setCorpFilter(corpId: string) {
      this.curCorpFilter = corpId
      if (corpId === 'ALL') {
        this.curStFilter = 'ALL'
        return
      }
      const stInCorp = this.stations.find(s => s.corpId === corpId)
      this.curStFilter = stInCorp ? stInCorp.id : 'ALL'
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

    // 중앙 REST API (/api/v1) MySQL 실데이터 연동 액션
    async fetchInitialData(silent = false) {
      try {
        if (!silent) {
          console.log('[CSMS UI] REST API 데이터 조회 실행: GET /api/v1/{corps, stations, chargers, chargers/summary, cdr/summary}')
        }

        const [corpRes, stRes, chgRes, chgSummaryRes, cdrSummaryRes] = await Promise.all([
          authFetch('/api/v1/corps'),
          authFetch('/api/v1/stations'),
          authFetch('/api/v1/chargers'),
          authFetch('/api/v1/chargers/summary'),
          authFetch('/api/v1/cdr/summary')
        ])

        if (stRes.ok && chgRes.ok) {
          const stJson = await stRes.json()
          const chgJson = await chgRes.json()

          const unwrapList = (json: any) => {
            if (!json) return []
            if (Array.isArray(json)) return json
            if (json.data && Array.isArray(json.data)) return json.data
            return []
          }

          if (corpRes.ok) {
            const corpJson = await corpRes.json()
            this.corps = unwrapList(corpJson)
          }

          const stList = unwrapList(stJson)
          if (stList.length > 0) {
            this.stations = stList
            if (this.curStFilter !== 'ALL' && !this.stations.some(s => s.id === this.curStFilter)) {
              this.curStFilter = 'ALL'
            }
          } else {
            this.stations = []
            this.curStFilter = 'ALL'
          }

          const chgList = unwrapList(chgJson)
          if (chgList.length > 0) {
            this.chargers = chgList
            if (!this.selectedCharger || !this.chargers.some(c => c.chargeBoxId === this.selectedCharger?.chargeBoxId)) {
              this.selectedCharger = this.chargers[0]
            } else {
              const updated = this.chargers.find(c => c.chargeBoxId === this.selectedCharger?.chargeBoxId)
              if (updated) {
                this.selectedCharger = updated
              }
            }
          } else {
            this.chargers = []
            this.selectedCharger = null
          }

          let chgSummary: any = null
          if (chgSummaryRes.ok) {
            const chgSummaryJson = await chgSummaryRes.json()
            chgSummary = chgSummaryJson?.data || chgSummaryJson
          }

          let cdrSummary: any = null
          if (cdrSummaryRes.ok) {
            const cdrSummaryJson = await cdrSummaryRes.json()
            cdrSummary = cdrSummaryJson?.data || cdrSummaryJson
          }

          const chargingCount = chgSummary?.chargingCount ?? this.chargers.filter(c => getStatusMeta(c.status).key === 'charging').length
          const availableCount = chgSummary?.availableCount ?? this.chargers.filter(c => getStatusMeta(c.status).key === 'standby').length
          const faultedCount = chgSummary?.faultedCount ?? this.chargers.filter(c => {
            const k = getStatusMeta(c.status).key
            return k === 'faulted' || k === 'commerr' || k === 'inspecting'
          }).length
          const preparingCount = this.chargers.filter(c => getStatusMeta(c.status).key === 'preparing').length
          const fallbackPower = this.chargers.reduce((acc, c) => acc + (Number(c.powerKw) || 0), 0)
          const totalPowerKw = chgSummary?.totalPowerKw != null ? Number(chgSummary.totalPowerKw) : fallbackPower

          this.summary = {
            totalChargers: chgSummary?.totalCount ?? this.chargers.length,
            chargingCount,
            availableCount,
            faultedCount,
            preparingCount,
            totalPowerKw: Math.round(totalPowerKw * 10) / 10,
            liveTps: 0,
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
      } catch {
        this.isDbConnected = false
      }
    },

    // 원격 충전기 제어 명령 API 연동
    async sendRemoteAction(action: string) {
      if (!this.selectedCharger) return

      try {
        const res = await authFetch(`/api/v1/chargers/${this.selectedCharger.chargeBoxId}/remote-command`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ action })
        })

        const result = await res.json().catch(() => null)
        if (res.ok && result?.success !== false && result?.data?.success !== false) {
          this.addToast('success', `${action} 명령 완료`, result?.message || '원격 제어 명령이 성공적으로 전송되었습니다.')
          setTimeout(() => this.fetchInitialData(true), 1000)
          return
        }

        const errMsg = result?.message || `[${this.selectedCharger.chargeBoxId}] 단말에 명령을 전달하지 못했습니다. (HTTP ${res.status})`
        this.addToast('error', `${action} 명령 실패`, errMsg)
      } catch (err: any) {
        this.addToast('error', `${action} 명령 실패`, err?.message || '원격 제어 명령 전송 중 오류가 발생했습니다.')
      }
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
