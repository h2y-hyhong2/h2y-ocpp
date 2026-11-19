<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  useCsmsStore,
  getStatusMeta,
  isChargerConnected,
  getChargerConnectors,
  getPrimaryConnector,
  getChargerEffectiveStatus,
  getChargerTotalPowerKw,
  type ChargerItem,
  type ConnectorDetail
} from '@/stores/csmsStore'

type SimpleFilterKey = 'ALL' | 'standby' | 'charging' | 'preparing' | 'finished' | 'faulted' | 'disconnected'

const router = useRouter()
const store = useCsmsStore()

const isStationDropdownOpen = ref(false)
const stationDropdownRef = ref<HTMLElement | null>(null)
const activeFilter = ref<SimpleFilterKey>('ALL')
const localSearch = ref('')
const isRefreshing = ref(false)
const isPreviewSampleMode = ref(false)
const detailChargerId = ref<string | null>(null)
const detailConnectorId = ref<number>(1)

// Stitch 디자인 확인용 10기 샘플 데이터 (기본값은 OFF, MySQL 실데이터 사용)
const SAMPLE_CHARGERS: ChargerItem[] = [
  {
    id: '00000101-1',
    chargeBoxId: '00000101',
    stId: '000001',
    stationName: '강남 테헤란로점',
    corpId: 'CORP01',
    corpName: 'LS E-Link',
    corpShortName: 'LS',
    cpId: '01',
    connectorId: 1,
    status: 'Available',
    connected: true,
    spec: '급속 200kW (듀얼건)',
    powerKw: 0,
    voltageV: 0,
    currentA: 0,
    socPercent: 0,
    batteryTempC: 24,
    vendor: 'LS E-Link Power',
    model: 'ELVIS-HPC-200K',
    carModel: '',
    userTag: '',
    protocol: 'ocpp1.6',
    lastHeartbeat: '방금 전',
    accumulatedKwh: 0,
    chargingMinutes: 0,
    connectors: [
      { connectorId: 1, status: 'Available', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 24, accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '방금 전' },
      { connectorId: 2, status: 'Available', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 24, accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '방금 전' }
    ]
  },
  {
    id: '00000102-1',
    chargeBoxId: '00000102',
    stId: '000001',
    stationName: '강남 테헤란로점',
    corpId: 'CORP01',
    corpName: 'LS E-Link',
    corpShortName: 'LS',
    cpId: '02',
    connectorId: 1,
    status: 'Charging',
    connected: true,
    spec: '초급속 350kW (듀얼건)',
    powerKw: 142.5,
    voltageV: 780,
    currentA: 182.7,
    socPercent: 82,
    batteryTempC: 33,
    vendor: 'LS E-Link Power',
    model: 'ELVIS-HPC-350K',
    carModel: 'EV6 GT-Line',
    userTag: 'C005413',
    protocol: 'ocpp1.6',
    lastHeartbeat: '방금 전',
    accumulatedKwh: 52.4,
    chargingMinutes: 26,
    connectors: [
      { connectorId: 1, status: 'Charging', powerKw: 142.5, voltageV: 780, currentA: 182.7, socPercent: 82, batteryTempC: 33, carModel: 'EV6 GT-Line', userTag: 'C005413', accumulatedKwh: 52.4, chargingMinutes: 26, lastHeartbeat: '방금 전' },
      { connectorId: 2, status: 'Available', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 25, accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '방금 전' }
    ]
  },
  {
    id: '00000103-1',
    chargeBoxId: '00000103',
    stId: '000001',
    stationName: '강남 테헤란로점',
    corpId: 'CORP01',
    corpName: 'LS E-Link',
    corpShortName: 'LS',
    cpId: '03',
    connectorId: 1,
    status: 'Charging',
    connected: true,
    spec: '초급속 350kW (듀얼건)',
    powerKw: 186.0,
    voltageV: 760,
    currentA: 244.7,
    socPercent: 45,
    batteryTempC: 31,
    vendor: 'SK 시그넷',
    model: 'V2-350K',
    carModel: '아이오닉 6',
    userTag: 'M102938',
    protocol: 'ocpp1.6',
    lastHeartbeat: '방금 전',
    accumulatedKwh: 28.6,
    chargingMinutes: 14,
    connectors: [
      { connectorId: 1, status: 'Charging', powerKw: 186.0, voltageV: 760, currentA: 244.7, socPercent: 45, batteryTempC: 31, carModel: '아이오닉 6', userTag: 'M102938', accumulatedKwh: 28.6, chargingMinutes: 14, lastHeartbeat: '방금 전' },
      { connectorId: 2, status: 'Available', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 25, accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '방금 전' }
    ]
  },
  {
    id: '00000104-1',
    chargeBoxId: '00000104',
    stId: '000001',
    stationName: '강남 테헤란로점',
    corpId: 'CORP01',
    corpName: 'LS E-Link',
    corpShortName: 'LS',
    cpId: '04',
    connectorId: 1,
    status: 'Preparing',
    connected: true,
    spec: '급속 100kW (듀얼건)',
    powerKw: 0,
    voltageV: 0,
    currentA: 0,
    socPercent: 0,
    batteryTempC: 25,
    vendor: '대영채비',
    model: 'CHAEVI-100K',
    carModel: '',
    userTag: 'M774120',
    protocol: 'ocpp1.6',
    lastHeartbeat: '방금 전',
    accumulatedKwh: 0,
    chargingMinutes: 0,
    connectors: [
      { connectorId: 1, status: 'Preparing', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 25, userTag: 'M774120', accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '방금 전' },
      { connectorId: 2, status: 'Available', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 25, accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '방금 전' }
    ]
  },
  {
    id: '00000105-1',
    chargeBoxId: '00000105',
    stId: '000001',
    stationName: '강남 테헤란로점',
    corpId: 'CORP01',
    corpName: 'LS E-Link',
    corpShortName: 'LS',
    cpId: '05',
    connectorId: 1,
    status: 'Available',
    connected: true,
    spec: '완속 7kW (4채널)',
    powerKw: 0,
    voltageV: 0,
    currentA: 0,
    socPercent: 0,
    batteryTempC: 23,
    vendor: 'PNE 시스템즈',
    model: 'PNE-AC4',
    carModel: '',
    userTag: '',
    protocol: 'ocpp1.6',
    lastHeartbeat: '방금 전',
    accumulatedKwh: 0,
    chargingMinutes: 0,
    connectors: [
      { connectorId: 1, status: 'Available', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 23, accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '방금 전' },
      { connectorId: 2, status: 'Available', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 23, accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '방금 전' },
      { connectorId: 3, status: 'Available', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 23, accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '방금 전' },
      { connectorId: 4, status: 'Available', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 23, accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '방금 전' }
    ]
  },
  {
    id: '00000106-1',
    chargeBoxId: '00000106',
    stId: '000001',
    stationName: '강남 테헤란로점',
    corpId: 'CORP01',
    corpName: 'LS E-Link',
    corpShortName: 'LS',
    cpId: '06',
    connectorId: 1,
    status: 'Available',
    connected: true,
    spec: '급속 100kW (듀얼건)',
    powerKw: 0,
    voltageV: 0,
    currentA: 0,
    socPercent: 0,
    batteryTempC: 24,
    vendor: '클린앤차지',
    model: 'CC-100K',
    carModel: '',
    userTag: '',
    protocol: 'ocpp1.6',
    lastHeartbeat: '20분 전',
    accumulatedKwh: 0,
    chargingMinutes: 0,
    connectors: [
      { connectorId: 1, status: 'Available', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 24, accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '20분 전' },
      { connectorId: 2, status: 'Available', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 24, accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '20분 전' }
    ]
  },
  {
    id: '00000107-1',
    chargeBoxId: '00000107',
    stId: '000001',
    stationName: '강남 테헤란로점',
    corpId: 'CORP01',
    corpName: 'LS E-Link',
    corpShortName: 'LS',
    cpId: '07',
    connectorId: 1,
    status: 'Finishing',
    connected: true,
    spec: '초급속 200kW (듀얼건)',
    powerKw: 0,
    voltageV: 0,
    currentA: 0,
    socPercent: 100,
    batteryTempC: 28,
    vendor: '중앙제어',
    model: 'JC-200K',
    carModel: 'GV60',
    userTag: 'C008821',
    protocol: 'ocpp1.6',
    lastHeartbeat: '방금 전',
    accumulatedKwh: 61.2,
    chargingMinutes: 38,
    connectors: [
      { connectorId: 1, status: 'Finishing', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 100, batteryTempC: 28, carModel: 'GV60', userTag: 'C008821', accumulatedKwh: 61.2, chargingMinutes: 38, lastHeartbeat: '방금 전' },
      { connectorId: 2, status: 'Available', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 25, accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '방금 전' }
    ]
  },
  {
    id: '00000108-1',
    chargeBoxId: '00000108',
    stId: '000001',
    stationName: '강남 테헤란로점',
    corpId: 'CORP01',
    corpName: 'LS E-Link',
    corpShortName: 'LS',
    cpId: '08',
    connectorId: 1,
    status: 'Faulted',
    connected: true,
    spec: '급속 200kW (듀얼건)',
    powerKw: 0,
    voltageV: 0,
    currentA: 0,
    socPercent: 0,
    batteryTempC: 26,
    vendor: 'LS E-Link Power',
    model: 'ELVIS-HPC-200K',
    carModel: '',
    userTag: '',
    protocol: 'ocpp1.6',
    lastHeartbeat: '1분 전',
    accumulatedKwh: 0,
    chargingMinutes: 0,
    connectors: [
      { connectorId: 1, status: 'Faulted', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 26, accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '1분 전' },
      { connectorId: 2, status: 'Available', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 25, accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '1분 전' }
    ]
  },
  {
    id: '00000109-1',
    chargeBoxId: '00000109',
    stId: '000001',
    stationName: '강남 테헤란로점',
    corpId: 'CORP01',
    corpName: 'LS E-Link',
    corpShortName: 'LS',
    cpId: '09',
    connectorId: 1,
    status: 'Charging',
    connected: true,
    spec: '급속 100kW (듀얼건)',
    powerKw: 88.4,
    voltageV: 410,
    currentA: 215.6,
    socPercent: 65,
    batteryTempC: 30,
    vendor: 'LS E-Link Power',
    model: 'ELVIS-QC-100K',
    carModel: 'EV6 순수 전기차',
    userTag: 'K10100102010',
    protocol: 'ocpp1.6',
    lastHeartbeat: '방금 전',
    accumulatedKwh: 34.8,
    chargingMinutes: 22,
    connectors: [
      { connectorId: 1, status: 'Charging', powerKw: 88.4, voltageV: 410, currentA: 215.6, socPercent: 65, batteryTempC: 30, carModel: 'EV6 순수 전기차', userTag: 'K10100102010', accumulatedKwh: 34.8, chargingMinutes: 22, lastHeartbeat: '방금 전' },
      { connectorId: 2, status: 'Available', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 25, accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '방금 전' }
    ]
  },
  {
    id: '00000110-1',
    chargeBoxId: '00000110',
    stId: '000001',
    stationName: '강남 테헤란로점',
    corpId: 'CORP01',
    corpName: 'LS E-Link',
    corpShortName: 'LS',
    cpId: '10',
    connectorId: 1,
    status: '연결안됨',
    connected: false,
    spec: '급속 350kW (듀얼건)',
    powerKw: 0,
    voltageV: 0,
    currentA: 0,
    socPercent: 0,
    batteryTempC: 25,
    vendor: 'LS E-Link Power',
    model: 'ELVIS-HPC-350K',
    carModel: '',
    userTag: '',
    protocol: 'ocpp1.6',
    lastHeartbeat: '2026-11-19 08:52:08',
    accumulatedKwh: 0,
    chargingMinutes: 0,
    connectors: [
      { connectorId: 1, status: '연결안됨', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 25, accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '2026-11-19 08:52:08' },
      { connectorId: 2, status: '연결안됨', powerKw: 0, voltageV: 0, currentA: 0, socPercent: 0, batteryTempC: 25, accumulatedKwh: 0, chargingMinutes: 0, lastHeartbeat: '2026-11-19 08:52:08' }
    ]
  }
]

const sourceChargers = computed<ChargerItem[]>(() => {
  if (isPreviewSampleMode.value) {
    return SAMPLE_CHARGERS
  }
  return store.chargers.filter(c => {
    const matchCorp = store.curCorpFilter === 'ALL' || c.corpId === store.curCorpFilter
    const matchSt = store.curStFilter === 'ALL' || c.stId === store.curStFilter
    return matchCorp && matchSt
  })
})

const currentStationLabel = computed(() => {
  if (isPreviewSampleMode.value) {
    return { id: '000001', name: '강남 테헤란로점 (디자인 샘플)', count: SAMPLE_CHARGERS.length }
  }
  if (store.curStFilter === 'ALL' || store.stations.length === 0) {
    return {
      id: 'ALL',
      name: store.stations.length === 0 ? '등록된 충전소 없음' : '전체 충전소 통합 관제',
      count: sourceChargers.value.length
    }
  }
  const st = store.stations.find(s => s.id === store.curStFilter)
  return st
    ? { id: st.id, name: st.name, count: sourceChargers.value.length }
    : { id: 'ALL', name: '전체 충전소 통합 관제', count: sourceChargers.value.length }
})

function resolveCardBucket(chg: ChargerItem): Exclude<SimpleFilterKey, 'ALL'> {
  if (!isChargerConnected(chg)) return 'disconnected'
  const key = getStatusMeta(getChargerEffectiveStatus(chg)).key
  if (key === 'charging') return 'charging'
  if (key === 'preparing' || key === 'paused') return 'preparing'
  if (key === 'finished' || key === 'reserved') return 'finished'
  if (key === 'faulted' || key === 'commerr' || key === 'inspecting' || key === 'suspended') return 'faulted'
  if (key === 'disconnected' || key === 'unknown') return 'disconnected'
  return 'standby'
}

const filterCounts = computed(() => {
  const counts: Record<SimpleFilterKey, number> = {
    ALL: sourceChargers.value.length,
    standby: 0,
    charging: 0,
    preparing: 0,
    finished: 0,
    faulted: 0,
    disconnected: 0
  }
  sourceChargers.value.forEach(chg => {
    counts[resolveCardBucket(chg)]++
  })
  return counts
})

const filteredChargers = computed(() => {
  const localQ = localSearch.value.trim().toLowerCase()
  const globalQ = (store.searchQuery || '').trim().toLowerCase()

  return sourceChargers.value.filter(chg => {
    if (activeFilter.value !== 'ALL' && resolveCardBucket(chg) !== activeFilter.value) {
      return false
    }
    const targetText = `${chg.cpId} ${chg.chargeBoxId} ${chg.stationName} ${chg.vendor} ${chg.model} ${chg.carModel || ''} ${chg.userTag || ''}`.toLowerCase()
    if (localQ && !targetText.includes(localQ)) return false
    if (globalQ && !targetText.includes(globalQ)) return false
    return true
  })
})

const detailCharger = computed<ChargerItem | null>(() => {
  if (!detailChargerId.value) return null
  return sourceChargers.value.find(c => c.chargeBoxId === detailChargerId.value) ?? null
})

const detailConnectors = computed<ConnectorDetail[]>(() => getChargerConnectors(detailCharger.value))

const detailConnector = computed<ConnectorDetail | null>(() => {
  const list = detailConnectors.value
  if (list.length === 0) return null
  return list.find(c => c.connectorId === detailConnectorId.value) ?? getPrimaryConnector(detailCharger.value) ?? list[0]
})

function selectStation(stId: string) {
  isPreviewSampleMode.value = false
  store.setStationFilter(stId)
  isStationDropdownOpen.value = false
}

function copyText(val: string, label: string, e?: Event) {
  if (e) e.stopPropagation()
  navigator.clipboard?.writeText(val)
  store.addToast('info', `${label} 복사 완료`, `[${val}] 값이 클립보드에 복사되었습니다.`)
}

async function handleRefresh() {
  isRefreshing.value = true
  try {
    await store.fetchInitialData(false)
  } finally {
    setTimeout(() => {
      isRefreshing.value = false
    }, 450)
  }
}

function openChargerDetail(chg: ChargerItem, connectorId?: number) {
  detailChargerId.value = chg.chargeBoxId
  const primary = getPrimaryConnector(chg)
  detailConnectorId.value = connectorId ?? primary?.connectorId ?? 1
  if (!isPreviewSampleMode.value) {
    store.setSelectedCharger(chg, detailConnectorId.value)
  }
}

function closeChargerDetail() {
  detailChargerId.value = null
}

async function triggerCardAction(chg: ChargerItem, action: string, e?: Event) {
  if (e) e.stopPropagation()
  if (isPreviewSampleMode.value) {
    store.addToast('info', '디자인 샘플 미리보기', `[${chg.chargeBoxId}] 샘플 모드에서는 실제 제어 명령을 전송하지 않습니다.`)
    return
  }
  const targetConnId = getPrimaryConnector(chg)?.connectorId ?? chg.connectorId ?? 1
  store.setSelectedCharger(chg, targetConnId)
  await store.sendRemoteAction(action, targetConnId)
}

async function triggerDetailAction(action: string) {
  if (!detailCharger.value) return
  if (isPreviewSampleMode.value) {
    store.addToast('info', '디자인 샘플 미리보기', `[${detailCharger.value.chargeBoxId}] 샘플 모드에서는 실제 제어 명령을 전송하지 않습니다.`)
    return
  }
  store.setSelectedCharger(detailCharger.value, detailConnectorId.value)
  await store.sendRemoteAction(action, detailConnectorId.value)
}

function goBillingView(e?: Event) {
  if (e) e.stopPropagation()
  router.push('/billing')
}

function estimateRemainMinutes(soc: number): number {
  const clamped = Math.max(0, Math.min(100, soc || 0))
  return Math.max(1, Math.round((100 - clamped) * 0.45))
}

function estimateCostWon(kwh: number): number {
  return Math.round((Number(kwh) || 0) * 347)
}

function getAvailableConnectorCount(chg: ChargerItem): number {
  return getChargerConnectors(chg).filter(c => getStatusMeta(c.status).key === 'standby').length
}

function handleOutsideClick(e: MouseEvent) {
  if (stationDropdownRef.value && !stationDropdownRef.value.contains(e.target as Node)) {
    isStationDropdownOpen.value = false
  }
}

onMounted(() => {
  store.fetchInitialData(true)
  document.addEventListener('click', handleOutsideClick)
})

onUnmounted(() => {
  document.removeEventListener('click', handleOutsideClick)
})
</script>

<template>
  <div class="w-full h-full flex flex-col gap-2.5 overflow-hidden select-none bg-[#F1F4F9] rounded-xl p-2.5">
    <!-- 1. 상단 요약 & 컨트롤 바 (Stitch 디자인 반영) -->
    <div class="w-full bg-white rounded-xl px-3.5 py-2 border border-slate-200 shadow-2xs flex flex-wrap items-center justify-between gap-2.5 flex-shrink-0">
      <!-- 좌측: 충전소 전환 드롭다운 + ID 복사 배지 + 샘플 토글 -->
      <div class="flex flex-wrap items-center gap-2">
        <div ref="stationDropdownRef" class="relative">
          <button
            type="button"
            @click="isStationDropdownOpen = !isStationDropdownOpen"
            class="inline-flex items-center justify-between gap-3 px-3.5 py-1.5 rounded-xl bg-sky-50 hover:bg-sky-100/80 text-sky-700 font-bold border border-sky-200 transition-all shadow-2xs cursor-pointer min-w-[260px] max-w-[360px]"
          >
            <div class="flex items-center gap-2 min-w-0">
              <svg class="w-4 h-4 text-sky-600 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
              <span class="text-sm text-slate-900 font-extrabold tracking-tight truncate">{{ currentStationLabel.name }}</span>
              <span class="px-2 py-0.5 rounded-full bg-sky-600 text-white text-[11px] font-bold flex-shrink-0 font-mono">
                {{ currentStationLabel.count }}기
              </span>
            </div>
            <svg
              class="w-4 h-4 text-sky-600 transition-transform flex-shrink-0"
              :class="isStationDropdownOpen ? 'rotate-180' : ''"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
            </svg>
          </button>

          <!-- 충전소 선택 드롭다운 메뉴 -->
          <div
            v-if="isStationDropdownOpen"
            class="absolute left-0 top-full mt-1.5 w-80 bg-white border border-slate-200 rounded-xl shadow-xl p-2 z-50 flex flex-col gap-1"
          >
            <div class="px-2 py-1 flex items-center justify-between text-[11px] font-bold text-slate-400 border-b border-slate-100">
              <span>충전소 전환</span>
              <span class="text-[10px] text-sky-600 font-semibold">총 {{ store.stations.length }}개 지점</span>
            </div>

            <button
              type="button"
              @click="selectStation('ALL')"
              class="w-full flex items-center justify-between px-2.5 py-1.5 rounded-lg text-left transition-colors cursor-pointer"
              :class="!isPreviewSampleMode && store.curStFilter === 'ALL' ? 'bg-sky-50 text-sky-700' : 'hover:bg-slate-50 text-slate-700'"
            >
              <div class="flex flex-col">
                <span class="text-xs font-bold">전체 충전소 통합 관제</span>
                <span class="text-[10px] text-slate-500">전체 {{ store.chargers.length }}기</span>
              </div>
              <svg v-if="!isPreviewSampleMode && store.curStFilter === 'ALL'" class="w-4 h-4 text-sky-600" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
              </svg>
            </button>

            <div class="max-h-56 overflow-y-auto space-y-0.5">
              <button
                v-for="st in store.stations"
                :key="st.id"
                type="button"
                @click="selectStation(st.id)"
                class="w-full flex items-center justify-between px-2.5 py-1.5 rounded-lg text-left transition-colors cursor-pointer"
                :class="!isPreviewSampleMode && store.curStFilter === st.id ? 'bg-sky-50 text-sky-700' : 'hover:bg-slate-50 text-slate-700'"
              >
                <div class="flex flex-col min-w-0">
                  <span class="text-xs font-bold truncate flex items-center gap-1.5">
                    <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 flex-shrink-0"></span>
                    <span class="truncate">{{ st.name }}</span>
                  </span>
                  <span class="text-[10px] text-slate-500 font-mono">ID {{ st.id }} · {{ st.chargerCount }}기</span>
                </div>
                <svg v-if="!isPreviewSampleMode && store.curStFilter === st.id" class="w-4 h-4 text-sky-600 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                </svg>
                <span v-else class="text-[10px] text-slate-400 font-semibold flex-shrink-0">선택</span>
              </button>
            </div>
          </div>
        </div>

        <!-- 충전소 ID 복사 배지 -->
        <button
          v-if="currentStationLabel.id !== 'ALL'"
          type="button"
          @click="copyText(currentStationLabel.id, '충전소 ID', $event)"
          class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-slate-100 hover:bg-sky-50 hover:text-sky-700 hover:border-sky-300 text-[11px] font-bold text-slate-600 border border-slate-200 cursor-pointer transition-all font-mono"
          title="클릭하여 충전소 ID 복사"
        >
          <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
          </svg>
          <span>충전소 ID {{ currentStationLabel.id }}</span>
        </button>

        <!-- 데이터 소스 전환 (MySQL 실데이터 vs Stitch 10기 디자인 샘플) -->
        <button
          type="button"
          @click="isPreviewSampleMode = !isPreviewSampleMode"
          class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold border transition-all cursor-pointer"
          :class="isPreviewSampleMode
            ? 'bg-violet-50 text-violet-700 border-violet-300'
            : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'"
          title="MySQL 실데이터와 Stitch 10기 디자인 샘플 화면 전환"
        >
          <span class="w-1.5 h-1.5 rounded-full" :class="isPreviewSampleMode ? 'bg-violet-500' : 'bg-emerald-500'"></span>
          <span>{{ isPreviewSampleMode ? '샘플 10기 보기 중 (실데이터로 전환)' : '디자인 샘플 10기 미리보기' }}</span>
        </button>
      </div>

      <!-- 우측: 상태 필터 칩 + 검색창 + 새로고침 버튼 -->
      <div class="flex flex-wrap items-center gap-2">
        <div class="flex flex-wrap items-center gap-1 bg-slate-100 p-1 rounded-xl">
          <button
            type="button"
            @click="activeFilter = 'ALL'"
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer"
            :class="activeFilter === 'ALL' ? 'bg-white text-sky-700 shadow-2xs' : 'text-slate-600 hover:text-slate-900'"
          >
            <span>전체</span>
            <span class="font-mono">{{ filterCounts.ALL }}</span>
          </button>
          <button
            type="button"
            @click="activeFilter = 'standby'"
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold transition-all cursor-pointer"
            :class="activeFilter === 'standby' ? 'bg-white text-emerald-700 shadow-2xs font-bold' : 'text-slate-600 hover:text-slate-900'"
          >
            <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
            <span>비어있음</span>
            <span class="font-mono">{{ filterCounts.standby }}</span>
          </button>
          <button
            type="button"
            @click="activeFilter = 'charging'"
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold transition-all cursor-pointer"
            :class="activeFilter === 'charging' ? 'bg-white text-blue-700 shadow-2xs font-bold' : 'text-slate-600 hover:text-slate-900'"
          >
            <span class="w-2 h-2 rounded-full bg-blue-600"></span>
            <span>충전중</span>
            <span class="font-mono">{{ filterCounts.charging }}</span>
          </button>
          <button
            type="button"
            @click="activeFilter = 'faulted'"
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold transition-all cursor-pointer"
            :class="activeFilter === 'faulted' ? 'bg-white text-rose-700 shadow-2xs font-bold' : 'text-slate-600 hover:text-slate-900'"
          >
            <span class="w-2 h-2 rounded-full bg-rose-600"></span>
            <span>점검</span>
            <span class="font-mono">{{ filterCounts.faulted }}</span>
          </button>
          <button
            type="button"
            @click="activeFilter = 'disconnected'"
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold transition-all cursor-pointer"
            :class="activeFilter === 'disconnected' ? 'bg-white text-slate-800 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-900'"
          >
            <span class="w-2 h-2 rounded-full bg-slate-400"></span>
            <span>미연결</span>
            <span class="font-mono">{{ filterCounts.disconnected }}</span>
          </button>
        </div>

        <div class="flex items-center gap-1.5">
          <div class="relative flex items-center">
            <svg class="w-3.5 h-3.5 absolute left-2.5 text-slate-400 pointer-events-none" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              v-model="localSearch"
              type="text"
              placeholder="충전기 번호 (예: 01)"
              class="w-40 pl-8 pr-2.5 py-1.5 bg-slate-100 focus:bg-white rounded-lg text-slate-800 text-xs font-medium placeholder:text-slate-400 outline-hidden border border-transparent focus:border-sky-500 transition-all"
            />
          </div>

          <!-- 수동 새로고침(조회) 버튼 -->
          <button
            type="button"
            @click="handleRefresh"
            class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold transition-colors cursor-pointer border border-slate-200"
            title="실시간 데이터 즉시 조회"
          >
            <svg
              class="w-3.5 h-3.5 text-sky-600 transition-transform"
              :class="isRefreshing ? 'animate-spin' : ''"
              fill="none"
              stroke="currentColor"
              stroke-width="2.2"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            <span>새로고침</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 2. 반응형 5열 충전기 카드 매트릭스 (Stitch 5 Columns Grid) -->
    <div class="flex-1 overflow-y-auto pr-0.5 pb-1 min-h-0">
      <div
        v-if="filteredChargers.length === 0"
        class="h-full min-h-[260px] bg-white rounded-xl border border-slate-200 flex flex-col items-center justify-center text-center p-8"
      >
        <div class="w-14 h-14 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-400 mb-3">
          <svg class="w-7 h-7" fill="none" stroke="currentColor" stroke-width="1.6" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
        </div>
        <p class="text-sm font-bold text-slate-800">표시할 충전기가 없습니다</p>
        <p class="text-xs text-slate-500 mt-1">검색어나 상태 필터를 변경하거나 상단 [디자인 샘플 10기 미리보기]를 눌러 전체 카드 시안을 확인할 수 있습니다.</p>
      </div>

      <div
        v-else
        class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-2.5 auto-rows-fr"
      >
        <div
          v-for="chg in filteredChargers"
          :key="chg.chargeBoxId"
          @click="openChargerDetail(chg)"
          class="bg-white rounded-xl p-3 flex flex-col justify-between min-h-[260px] relative overflow-hidden group border shadow-xs hover:shadow-md transition-all duration-200 cursor-pointer"
          :class="{
            'border-slate-200 hover:border-slate-300': resolveCardBucket(chg) === 'standby',
            'border-blue-200 bg-gradient-to-b from-blue-50/70 via-white to-white': resolveCardBucket(chg) === 'charging',
            'border-orange-200 bg-gradient-to-b from-orange-50/80 via-white to-white': resolveCardBucket(chg) === 'preparing',
            'border-violet-200 bg-gradient-to-b from-violet-50/80 via-white to-white': resolveCardBucket(chg) === 'finished',
            'border-rose-200 bg-gradient-to-b from-rose-50/80 via-white to-white': resolveCardBucket(chg) === 'faulted',
            'border-slate-200 bg-gradient-to-b from-slate-50/90 via-white to-white': resolveCardBucket(chg) === 'disconnected',
            'ring-2 ring-sky-500': detailChargerId === chg.chargeBoxId
          }"
        >
          <!-- 카드 상단: 충전기 번호 + 이름/ID + 상태 배지 -->
          <div class="flex items-start justify-between gap-2">
            <div class="flex items-center gap-2 min-w-0">
              <span
                class="px-2 py-0.5 rounded-md font-mono text-base font-extrabold border flex-shrink-0"
                :class="{
                  'bg-slate-100 text-slate-900 border-slate-200': resolveCardBucket(chg) === 'standby' || resolveCardBucket(chg) === 'disconnected',
                  'bg-blue-100 text-blue-700 border-blue-200': resolveCardBucket(chg) === 'charging',
                  'bg-orange-100 text-orange-700 border-orange-200': resolveCardBucket(chg) === 'preparing',
                  'bg-violet-100 text-violet-700 border-violet-200': resolveCardBucket(chg) === 'finished',
                  'bg-rose-100 text-rose-700 border-rose-200': resolveCardBucket(chg) === 'faulted'
                }"
              >
                {{ chg.cpId || chg.chargeBoxId.slice(-2) }}
              </span>

              <div class="flex flex-col min-w-0">
                <div class="flex items-center gap-1 min-w-0">
                  <span class="text-xs font-bold text-slate-900 truncate">{{ chg.model || chg.vendor || 'ELVIS 충전기' }}</span>
                </div>
                <div class="flex items-center gap-1 mt-0.5">
                  <button
                    type="button"
                    @click="copyText(chg.chargeBoxId, '충전기 ID', $event)"
                    class="px-1.5 py-0.2 rounded bg-slate-50 hover:bg-sky-50 text-slate-600 hover:text-sky-700 text-[10px] font-mono font-semibold border border-slate-200 cursor-pointer transition-colors"
                    title="충전기 ID 복사"
                  >
                    {{ chg.chargeBoxId }}
                  </button>
                  <span class="text-[10px] text-slate-400 truncate">{{ chg.stationName }}</span>
                </div>
              </div>
            </div>

            <!-- 상태 배지 -->
            <span
              v-if="resolveCardBucket(chg) === 'standby'"
              class="px-2 py-0.5 rounded-full text-[10px] font-bold inline-flex items-center gap-1 border border-emerald-200 bg-emerald-50 text-emerald-700 flex-shrink-0"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              <span>{{ getChargerConnectors(chg).length >= 4 ? '전체 비어있음' : '즉시 충전 가능' }}</span>
            </span>

            <span
              v-else-if="resolveCardBucket(chg) === 'charging'"
              class="px-2 py-0.5 rounded-full text-[10px] font-bold inline-flex items-center gap-1 border border-blue-200 bg-blue-50 text-blue-700 flex-shrink-0"
            >
              <svg class="w-3 h-3 text-blue-600" fill="none" stroke="currentColor" stroke-width="2.4" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
              <span>충전 중</span>
            </span>

            <span
              v-else-if="resolveCardBucket(chg) === 'preparing'"
              class="px-2 py-0.5 rounded-full text-[10px] font-bold inline-flex items-center gap-1 border border-orange-200 bg-orange-50 text-orange-700 flex-shrink-0"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-orange-500 animate-ping"></span>
              <span>시작 대기</span>
            </span>

            <span
              v-else-if="resolveCardBucket(chg) === 'finished'"
              class="px-2 py-0.5 rounded-full text-[10px] font-bold inline-flex items-center gap-1 border border-violet-200 bg-violet-50 text-violet-700 flex-shrink-0"
            >
              <svg class="w-3 h-3 text-violet-600" fill="none" stroke="currentColor" stroke-width="2.4" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
              </svg>
              <span>충전 완료</span>
            </span>

            <span
              v-else-if="resolveCardBucket(chg) === 'faulted'"
              class="px-2 py-0.5 rounded-full text-[10px] font-bold inline-flex items-center gap-1 border border-rose-200 bg-rose-50 text-rose-700 flex-shrink-0"
            >
              <svg class="w-3 h-3 text-rose-600" fill="none" stroke="currentColor" stroke-width="2.4" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
              </svg>
              <span>점검 필요</span>
            </span>

            <span
              v-else
              class="px-2 py-0.5 rounded-full text-[10px] font-bold inline-flex items-center gap-1 border border-slate-200 bg-slate-100 text-slate-600 flex-shrink-0"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-slate-400"></span>
              <span>연결안됨</span>
            </span>
          </div>

          <!-- 카드 중앙 본문: 상태별 맞춤형 직관 레이아웃 -->
          <!-- A. 충전대기 (Standby) -->
          <div v-if="resolveCardBucket(chg) === 'standby'" class="my-auto flex flex-col items-center justify-center text-center py-2">
            <template v-if="getChargerConnectors(chg).length >= 4">
              <span class="font-extrabold text-2xl text-emerald-700 tracking-tight leading-none mb-1 font-mono">
                {{ getAvailableConnectorCount(chg) }} / {{ getChargerConnectors(chg).length }} 대
              </span>
              <p class="font-medium text-xs text-slate-500 mb-2">모든 케이블 사용 가능</p>
              <div class="grid grid-cols-4 gap-1.5 w-full">
                <div
                  v-for="conn in getChargerConnectors(chg)"
                  :key="conn.connectorId"
                  class="flex flex-col items-center py-1.5 rounded-lg bg-slate-50 border border-slate-200"
                >
                  <span class="w-2.5 h-2.5 rounded-full mb-1" :class="getStatusMeta(conn.status).dotClass"></span>
                  <span class="text-[11px] text-slate-700 font-bold">{{ conn.connectorId }}번</span>
                </div>
              </div>
            </template>

            <template v-else>
              <div class="w-14 h-14 rounded-full bg-emerald-50 border border-emerald-200 flex items-center justify-center text-emerald-600 mb-2 group-hover:scale-105 transition-transform">
                <svg class="w-7 h-7" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
              </div>
              <p class="font-bold text-base text-slate-900 tracking-tight">비어있음</p>
              <p class="font-medium text-[11px] text-slate-500 mt-0.5">케이블을 꽂으면 바로 충전 시작</p>
              <div class="w-full mt-2.5 bg-slate-50 border border-slate-200 rounded-lg p-1.5 flex items-center justify-around divide-x divide-slate-200">
                <div v-for="conn in getChargerConnectors(chg)" :key="conn.connectorId" class="flex-1 text-center px-1">
                  <span class="text-[10px] text-slate-500 block font-medium">케이블 {{ conn.connectorId }}</span>
                  <span class="text-[11px] font-bold inline-flex items-center justify-center gap-1 mt-0.5" :class="getStatusMeta(conn.status).key === 'standby' ? 'text-emerald-700' : 'text-slate-500'">
                    <span class="w-1.5 h-1.5 rounded-full" :class="getStatusMeta(conn.status).dotClass"></span>
                    <span>{{ getStatusMeta(conn.status).key === 'standby' ? '준비됨' : getStatusMeta(conn.status).name }}</span>
                  </span>
                </div>
              </div>
            </template>
          </div>

          <!-- B. 충전중 (Charging) -->
          <div v-else-if="resolveCardBucket(chg) === 'charging'" class="my-auto flex flex-col items-center justify-center text-center py-1">
            <div class="flex items-baseline gap-0.5 text-blue-700 font-bold leading-none mb-1.5 font-mono">
              <span class="text-4xl font-black tracking-tight">{{ getPrimaryConnector(chg)?.socPercent ?? chg.socPercent ?? 0 }}</span>
              <span class="text-base font-bold">%</span>
            </div>
            <div class="w-full bg-slate-200 rounded-full h-2.5 mb-2 overflow-hidden p-0.5">
              <div
                class="bg-blue-600 h-full rounded-full transition-all duration-500"
                :style="{ width: `${Math.min(100, getPrimaryConnector(chg)?.socPercent ?? chg.socPercent ?? 0)}%` }"
              ></div>
            </div>
            <div class="inline-flex items-center gap-1 px-2.5 py-0.5 bg-blue-50 border border-blue-200 text-blue-800 rounded-full text-[11px] font-bold font-mono">
              <svg class="w-3.5 h-3.5 text-blue-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span>약 {{ estimateRemainMinutes(getPrimaryConnector(chg)?.socPercent ?? chg.socPercent ?? 0) }}분 후 완료 · {{ getChargerTotalPowerKw(chg).toFixed(1) }} kW</span>
            </div>
            <span class="text-[11px] text-slate-500 font-medium mt-1.5 truncate max-w-full">
              {{ getPrimaryConnector(chg)?.carModel || `현재 ${estimateCostWon(getPrimaryConnector(chg)?.accumulatedKwh ?? chg.accumulatedKwh ?? 0).toLocaleString()}원 충전 중` }}
            </span>
          </div>

          <!-- C. 충전준비중 (Preparing) -->
          <div v-else-if="resolveCardBucket(chg) === 'preparing'" class="my-auto flex flex-col items-center justify-center text-center py-1">
            <div class="w-14 h-14 rounded-full bg-orange-100 border border-orange-300 flex items-center justify-center text-orange-600 mb-2 group-hover:scale-105 transition-transform">
              <svg class="w-7 h-7" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
              </svg>
            </div>
            <p class="font-bold text-base text-slate-900 tracking-tight">케이블 연결됨</p>
            <p class="font-medium text-[11px] text-slate-500 mt-0.5 leading-snug px-1">
              운전자가 인증 및 충전 시작을 준비 중입니다
            </p>
          </div>

          <!-- D. 충전완료 (Finished) -->
          <div v-else-if="resolveCardBucket(chg) === 'finished'" class="my-auto flex flex-col items-center justify-center text-center py-1">
            <div class="w-14 h-14 rounded-full bg-violet-100 border border-violet-300 flex items-center justify-center text-violet-600 mb-2 group-hover:scale-105 transition-transform">
              <svg class="w-7 h-7" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <p class="font-bold text-base text-slate-900 tracking-tight">
              충전 끝 ({{ getPrimaryConnector(chg)?.socPercent || 100 }}%)
            </p>
            <p class="font-medium text-[11px] text-slate-500 mt-0.5">
              누적 {{ Number(getPrimaryConnector(chg)?.accumulatedKwh ?? chg.accumulatedKwh ?? 0).toFixed(1) }} kWh · 예상 {{ estimateCostWon(getPrimaryConnector(chg)?.accumulatedKwh ?? chg.accumulatedKwh ?? 0).toLocaleString() }}원
            </p>
            <span class="inline-block mt-1.5 px-2.5 py-0.5 bg-slate-50 border border-slate-200 rounded-md text-[10px] text-violet-700 font-semibold">
              커넥터 분리 후 출차 대기
            </span>
          </div>

          <!-- E. 고장/점검 (Faulted) -->
          <div v-else-if="resolveCardBucket(chg) === 'faulted'" class="my-auto flex flex-col items-center justify-center text-center py-1">
            <div class="w-14 h-14 rounded-full bg-rose-100 border border-rose-300 flex items-center justify-center text-rose-600 mb-1.5 group-hover:scale-105 transition-transform">
              <svg class="w-7 h-7" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <p class="font-bold text-base text-rose-600 tracking-tight">{{ getStatusMeta(getChargerEffectiveStatus(chg)).name }}</p>
            <div class="mt-1.5 p-2 bg-rose-50 border border-rose-200 rounded-lg text-left w-full">
              <p class="text-[11px] text-rose-800 font-medium leading-snug">
                단말 오류 또는 점검 상태입니다. 원격 재부팅 후 상태를 확인해 주세요.
              </p>
            </div>
          </div>

          <!-- F. 연결안됨 (Disconnected) -->
          <div v-else class="my-auto flex flex-col items-center justify-center text-center py-2">
            <div class="w-14 h-14 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-400 mb-2">
              <svg class="w-7 h-7" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M18.364 5.636a9 9 0 010 12.728M5.636 5.636a9 9 0 000 12.728M15.536 8.464a5 5 0 010 7.072M8.464 8.464a5 5 0 000 7.072M3 3l18 18" />
              </svg>
            </div>
            <p class="font-bold text-base text-slate-700 tracking-tight">연결안됨</p>
            <p class="font-medium text-[11px] text-slate-400 mt-0.5 font-mono">
              최종 수신: {{ chg.lastHeartbeat || '-' }}
            </p>
            <div class="w-full mt-2.5 bg-slate-50 border border-slate-200 rounded-lg p-1.5 flex items-center justify-around divide-x divide-slate-200">
              <div v-for="conn in getChargerConnectors(chg)" :key="conn.connectorId" class="flex-1 text-center px-1">
                <span class="text-[10px] text-slate-400 block font-medium">케이블 {{ conn.connectorId }}</span>
                <span class="text-[11px] text-slate-500 font-bold inline-flex items-center justify-center gap-1 mt-0.5">
                  <span class="w-1.5 h-1.5 rounded-full bg-slate-400"></span>
                  <span>미연결</span>
                </span>
              </div>
            </div>
          </div>

          <!-- 카드 하단 액션 버튼 -->
          <div class="pt-2">
            <div v-if="resolveCardBucket(chg) === 'charging'" class="grid grid-cols-2 gap-1.5">
              <button
                type="button"
                @click="triggerCardAction(chg, 'RemoteStopTransaction', $event)"
                class="py-1.5 text-xs font-bold rounded-lg transition-colors flex items-center justify-center gap-1 bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200 cursor-pointer"
              >
                <svg class="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24"><path d="M6 6h12v12H6z" /></svg>
                <span>충전 멈춤</span>
              </button>
              <button
                type="button"
                @click="openChargerDetail(chg)"
                class="py-1.5 bg-slate-50 hover:bg-slate-100 text-slate-700 border border-slate-200 text-xs font-semibold rounded-lg transition-colors flex items-center justify-center gap-1 cursor-pointer"
              >
                <span>상세 보기</span>
              </button>
            </div>

            <button
              v-else-if="resolveCardBucket(chg) === 'preparing'"
              type="button"
              @click="triggerCardAction(chg, 'RemoteStartTransaction', $event)"
              class="w-full py-1.5 text-xs font-bold rounded-lg transition-all flex items-center justify-center gap-1 bg-orange-50 hover:bg-orange-100 text-orange-700 border border-orange-300 cursor-pointer"
            >
              <svg class="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24"><path d="M8 5v14l11-7z" /></svg>
              <span>원격 시작 승인하기</span>
            </button>

            <div v-else-if="resolveCardBucket(chg) === 'finished'" class="grid grid-cols-2 gap-1.5">
              <button
                type="button"
                @click="goBillingView($event)"
                class="py-1.5 bg-slate-50 hover:bg-slate-100 text-slate-700 border border-slate-200 text-xs font-semibold rounded-lg transition-colors flex items-center justify-center gap-1 cursor-pointer"
              >
                <span>결제 확인</span>
              </button>
              <button
                type="button"
                @click="triggerCardAction(chg, 'UnlockConnector', $event)"
                class="py-1.5 text-xs font-bold rounded-lg transition-colors flex items-center justify-center gap-1 bg-violet-50 hover:bg-violet-100 text-violet-700 border border-violet-200 cursor-pointer"
              >
                <span>잠금 풀기</span>
              </button>
            </div>

            <div v-else-if="resolveCardBucket(chg) === 'faulted'" class="grid grid-cols-2 gap-1.5">
              <button
                type="button"
                @click="triggerCardAction(chg, 'Reset', $event)"
                class="py-1.5 text-xs font-bold rounded-lg transition-colors flex items-center justify-center gap-1 bg-rose-600 hover:bg-rose-700 text-white cursor-pointer"
              >
                <span>원격 재부팅</span>
              </button>
              <button
                type="button"
                @click="openChargerDetail(chg)"
                class="py-1.5 bg-slate-50 hover:bg-slate-100 text-slate-700 border border-slate-200 text-xs font-semibold rounded-lg transition-colors flex items-center justify-center gap-1 cursor-pointer"
              >
                <span>상세 보기</span>
              </button>
            </div>

            <button
              v-else
              type="button"
              @click="openChargerDetail(chg)"
              class="w-full py-1.5 bg-slate-50 hover:bg-sky-600 hover:text-white text-slate-700 border border-slate-200 hover:border-sky-600 font-semibold text-xs rounded-lg transition-all flex items-center justify-center gap-1 cursor-pointer"
            >
              <span>이 충전기 상태 보기</span>
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. 하단 상태 바 (Stitch Footer 반영) -->
    <footer class="w-full bg-white rounded-xl px-4 py-1.5 border border-slate-200 flex flex-wrap items-center justify-between gap-2 text-[11px] text-slate-500 flex-shrink-0">
      <span>ELVIS·LITE 초간편 통합 관제 대시보드 (OCPP 1.6J)</span>
      <div class="flex items-center gap-3 font-mono">
        <span class="inline-flex items-center gap-1.5 font-semibold" :class="store.isDbConnected ? 'text-emerald-700' : 'text-slate-400'">
          <span class="w-1.5 h-1.5 rounded-full" :class="store.isDbConnected ? 'bg-emerald-500' : 'bg-slate-400'"></span>
          <span>{{ store.isDbConnected ? '관제 서버 온라인' : '서버 연결 대기' }}</span>
        </span>
        <span>표시 {{ filteredChargers.length }} / 전체 {{ sourceChargers.length }}기</span>
      </div>
    </footer>

    <!-- 4. 우측 슬라이드 충전기 상세·원격제어 드로어 -->
    <div
      v-if="detailCharger"
      @click="closeChargerDetail"
      class="fixed inset-0 bg-slate-900/30 z-40"
    ></div>

    <aside
      v-if="detailCharger"
      class="fixed top-14 right-3 bottom-3 w-80 max-w-[90vw] bg-white border border-slate-200 rounded-2xl shadow-2xl p-4 z-50 flex flex-col justify-between overflow-y-auto"
    >
      <div class="space-y-3">
        <div class="flex items-center justify-between pb-2.5 border-b border-slate-200">
          <div>
            <div class="flex items-center gap-1.5">
              <span class="font-mono font-extrabold text-sm text-sky-700">{{ detailCharger.chargeBoxId }}</span>
              <span
                class="px-2 py-0.5 rounded-full text-[10px] font-bold border"
                :class="getStatusMeta(detailConnector?.status ?? detailCharger.status).badgeClass"
              >
                #{{ detailConnector?.connectorId ?? 1 }} {{ getStatusMeta(detailConnector?.status ?? detailCharger.status).name }}
              </span>
            </div>
            <div class="text-xs font-bold text-slate-800 mt-0.5">{{ detailCharger.stationName }}</div>
            <div class="text-[10px] text-slate-500">{{ detailCharger.vendor }} · {{ detailCharger.model }}</div>
          </div>
          <button
            type="button"
            @click="closeChargerDetail"
            class="p-1.5 rounded-lg text-slate-400 hover:text-slate-800 hover:bg-slate-100 cursor-pointer"
            title="닫기"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <!-- 커넥터 채널 선택 -->
        <div class="space-y-1.5">
          <div class="text-[11px] font-bold text-slate-500">커넥터 채널 선택</div>
          <div class="grid grid-cols-2 gap-1.5">
            <button
              v-for="conn in detailConnectors"
              :key="conn.connectorId"
              type="button"
              @click="detailConnectorId = conn.connectorId"
              class="p-2 rounded-lg border text-left transition-all cursor-pointer"
              :class="detailConnector?.connectorId === conn.connectorId
                ? 'bg-sky-50 border-sky-500 ring-1 ring-sky-500'
                : 'bg-slate-50 border-slate-200 hover:bg-slate-100'"
            >
              <div class="flex items-center justify-between">
                <span class="font-mono font-bold text-xs text-slate-800">#{{ conn.connectorId }}</span>
                <span class="text-[10px] font-bold px-1.5 py-0.2 rounded border" :class="getStatusMeta(conn.status).badgeClass">
                  {{ getStatusMeta(conn.status).name }}
                </span>
              </div>
              <div class="text-[10px] font-mono text-slate-500 mt-1">
                {{ (conn.powerKw ?? 0) > 0 ? `${conn.powerKw.toFixed(1)} kW` : (getStatusMeta(conn.status).key === 'disconnected' ? '미연결' : '대기 0.0 kW') }}
              </div>
            </button>
          </div>
        </div>

        <!-- 계측치 카드 -->
        <div class="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-2.5">
          <div class="flex items-center justify-between text-xs font-bold text-slate-600">
            <span>#{{ detailConnector?.connectorId ?? 1 }} 계측 정보</span>
            <span class="font-mono text-[10px]" :class="isChargerConnected(detailCharger) ? 'text-emerald-600' : 'text-slate-400'">
              {{ isChargerConnected(detailCharger) ? '실시간 연결' : '미연결' }}
            </span>
          </div>
          <div class="grid grid-cols-2 gap-2 font-mono text-xs">
            <div class="p-2 rounded-lg bg-white border border-slate-200">
              <div class="text-[10px] text-slate-500 font-sans">순시 출력</div>
              <div class="font-bold text-sky-700 mt-0.5">{{ (detailConnector?.powerKw ?? 0).toFixed(1) }} kW</div>
            </div>
            <div class="p-2 rounded-lg bg-white border border-slate-200">
              <div class="text-[10px] text-slate-500 font-sans">배터리 SoC</div>
              <div class="font-bold text-emerald-600 mt-0.5">{{ detailConnector?.socPercent ?? 0 }} %</div>
            </div>
            <div class="p-2 rounded-lg bg-white border border-slate-200">
              <div class="text-[10px] text-slate-500 font-sans">전압 / 전류</div>
              <div class="font-bold text-slate-800 mt-0.5">{{ (detailConnector?.voltageV ?? 0).toFixed(0) }}V / {{ (detailConnector?.currentA ?? 0).toFixed(0) }}A</div>
            </div>
            <div class="p-2 rounded-lg bg-white border border-slate-200">
              <div class="text-[10px] text-slate-500 font-sans">누적 전력량</div>
              <div class="font-bold text-slate-800 mt-0.5">{{ (detailConnector?.accumulatedKwh ?? 0).toFixed(1) }} kWh</div>
            </div>
          </div>
        </div>

        <!-- OCPP 원격 제어 -->
        <div class="space-y-2">
          <div class="text-[11px] font-bold text-slate-500">OCPP 원격 제어 (#{{ detailConnector?.connectorId ?? 1 }})</div>
          <div class="grid grid-cols-2 gap-1.5 text-xs font-bold">
            <button
              type="button"
              @click="triggerDetailAction('Reset')"
              class="py-2 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-800 border border-slate-200 cursor-pointer"
            >
              단말 리셋
            </button>
            <button
              type="button"
              @click="triggerDetailAction('UnlockConnector')"
              class="py-2 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-800 border border-slate-200 cursor-pointer"
            >
              잠금 해제
            </button>
            <button
              type="button"
              @click="triggerDetailAction('RemoteStartTransaction')"
              class="py-2 rounded-lg bg-sky-600 hover:bg-sky-700 text-white cursor-pointer"
            >
              충전 시작
            </button>
            <button
              type="button"
              @click="triggerDetailAction('RemoteStopTransaction')"
              class="py-2 rounded-lg bg-rose-600 hover:bg-rose-700 text-white cursor-pointer"
            >
              충전 종료
            </button>
          </div>
        </div>
      </div>

      <div class="pt-3 border-t border-slate-100 text-[10px] text-slate-400 font-mono flex justify-between">
        <span>프로토콜: {{ detailCharger.protocol }}</span>
        <span>수신: {{ detailCharger.lastHeartbeat }}</span>
      </div>
    </aside>
  </div>
</template>
