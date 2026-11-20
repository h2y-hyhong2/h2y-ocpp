<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import ChargerContextMenu from '@/components/ChargerContextMenu.vue'
import { SAMPLE_CHARGERS } from '@/data/sampleChargers'
import {
  useCsmsStore,
  getStatusMeta,
  isChargerConnected,
  getChargerConnectionMeta,
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
const contextMenuRef = ref<InstanceType<typeof ChargerContextMenu> | null>(null)

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

function openContextMenu(e: MouseEvent, chg: ChargerItem, connectorId?: number) {
  contextMenuRef.value?.open(e, chg, connectorId)
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

const totalConnectorCount = computed(() => {
  return sourceChargers.value.reduce((acc, chg) => acc + getChargerConnectors(chg).length, 0)
})

const onlineChargerCount = computed(() => {
  return sourceChargers.value.filter(chg => isChargerConnected(chg)).length
})

const offlineChargerCount = computed(() => {
  return sourceChargers.value.length - onlineChargerCount.value
})

const totalLivePowerKw = computed(() => {
  return sourceChargers.value.reduce((acc, chg) => acc + getChargerTotalPowerKw(chg), 0)
})

interface FooterRollingItem {
  id: string
  badge: string
  tone: 'rose' | 'blue' | 'emerald' | 'indigo' | 'amber' | 'slate'
  title: string
  detail: string
  filterKey?: SimpleFilterKey
  charger?: ChargerItem
  connectorId?: number
}

const footerWrapRef = ref<HTMLElement | null>(null)
const isFooterRolledUp = ref(false)
const isRollingPaused = ref(false)
const isRollingHovered = ref(false)
const rollingIndex = ref(0)
const rollingDirection = ref<'up' | 'down'>('up')

const rollingItems = computed<FooterRollingItem[]>(() => {
  const items: FooterRollingItem[] = []
  const chargers = sourceChargers.value

  const faultedList = chargers.filter(c => resolveCardBucket(c) === 'faulted')
  if (faultedList.length > 0) {
    const first = faultedList[0]
    items.push({
      id: 'summary-faulted',
      badge: '경보',
      tone: 'rose',
      title: `고장·점검 충전기 ${faultedList.length}기 감지`,
      detail: faultedList.length === 1
        ? `[${first.chargeBoxId}] ${first.stationName} 상태 확인 및 원격 리셋 점검 필요`
        : `[${first.chargeBoxId}] 외 ${faultedList.length - 1}기 점검 필요 (클릭 시 고장 필터 보기)`,
      filterKey: 'faulted'
    })
    faultedList.slice(0, 2).forEach(chg => {
      const primary = getPrimaryConnector(chg)
      items.push({
        id: `fault-${chg.chargeBoxId}`,
        badge: '고장',
        tone: 'rose',
        title: `[${chg.chargeBoxId}] ${chg.stationName}`,
        detail: `#${primary?.connectorId ?? 1} ${getStatusMeta(primary?.status ?? chg.status).name} · 최종수신 ${chg.lastHeartbeat || '-'}`,
        charger: chg,
        connectorId: primary?.connectorId ?? 1
      })
    })
  }

  const chargingList = chargers.filter(c => resolveCardBucket(c) === 'charging')
  if (chargingList.length > 0) {
    items.push({
      id: 'summary-charging',
      badge: '충전',
      tone: 'blue',
      title: `실시간 충전중 ${chargingList.length}기 · 합계 ${totalLivePowerKw.value.toFixed(1)} kW`,
      detail: `대기 ${filterCounts.value.standby}기 · 준비 ${filterCounts.value.preparing}기 · 완료 ${filterCounts.value.finished}기`,
      filterKey: 'charging'
    })
    chargingList.slice(0, 2).forEach(chg => {
      const primary = getPrimaryConnector(chg)
      items.push({
        id: `charging-${chg.chargeBoxId}`,
        badge: '진행',
        tone: 'blue',
        title: `[${chg.chargeBoxId}] #${primary?.connectorId ?? 1} 충전중 (${primary?.socPercent ?? chg.socPercent ?? 0}%)`,
        detail: `${(primary?.powerKw ?? chg.powerKw ?? 0).toFixed(1)} kW · 누적 ${(primary?.accumulatedKwh ?? chg.accumulatedKwh ?? 0).toFixed(1)} kWh · ${primary?.carModel || chg.stationName}`,
        charger: chg,
        connectorId: primary?.connectorId ?? 1
      })
    })
  }

  if (filterCounts.value.preparing > 0) {
    items.push({
      id: 'summary-preparing',
      badge: '준비',
      tone: 'amber',
      title: `충전 준비·승인 대기 ${filterCounts.value.preparing}기`,
      detail: '커넥터 체결 후 인증 또는 원격 시작 승인을 대기 중인 단말이 있습니다.',
      filterKey: 'preparing'
    })
  }

  items.push({
    id: 'summary-status',
    badge: '가동',
    tone: 'emerald',
    title: `충전기 온라인 ${onlineChargerCount.value} / 전체 ${chargers.length}기 · 커넥터 ${totalConnectorCount.value}개`,
    detail: `충전대기 ${filterCounts.value.standby} · 충전중 ${filterCounts.value.charging} · 오프라인 ${offlineChargerCount.value}`,
    filterKey: 'ALL'
  })

  items.push({
    id: 'summary-kpi',
    badge: '집계',
    tone: 'indigo',
    title: `금일 누적 ${(Number(store.summary.todayTotalKwh) || 0).toFixed(1)} kWh · 실시간 ${store.summary.liveTps.toLocaleString()} TPS`,
    detail: `활성 세션 ${store.summary.activeSessions}건 · 서버 ${store.isDbConnected ? 'MySQL 실시간 연동 중' : '연결 대기 중'}`
  })

  items.push({
    id: 'guide-controls',
    badge: '안내',
    tone: 'slate',
    title: 'ELVIS·LITE 충전기 목록 관제 대시보드 (OCPP 1.6J)',
    detail: '카드 좌클릭: 상세 제어반 · 카드 우클릭: 빠른 원격제어 · 하단바 롤업: 전체 피드 펼치기'
  })

  return items
})

watch(
  () => rollingItems.value.length,
  (len) => {
    if (len === 0) {
      rollingIndex.value = 0
    } else if (rollingIndex.value >= len) {
      rollingIndex.value = 0
    }
  }
)

const currentRollingItem = computed<FooterRollingItem | null>(() => {
  const list = rollingItems.value
  if (list.length === 0) return null
  return list[rollingIndex.value % list.length]
})

function nextRollingItem() {
  const len = rollingItems.value.length
  if (len <= 1) return
  rollingDirection.value = 'up'
  rollingIndex.value = (rollingIndex.value + 1) % len
}

function prevRollingItem() {
  const len = rollingItems.value.length
  if (len <= 1) return
  rollingDirection.value = 'down'
  rollingIndex.value = (rollingIndex.value - 1 + len) % len
}

function toggleFooterRollup() {
  isFooterRolledUp.value = !isFooterRolledUp.value
}

function handleRollingItemClick(item: FooterRollingItem) {
  if (item.charger) {
    openChargerDetail(item.charger, item.connectorId)
    isFooterRolledUp.value = false
    return
  }
  if (item.filterKey) {
    activeFilter.value = item.filterKey
    isFooterRolledUp.value = false
  }
}

function getRollingBadgeClass(tone: FooterRollingItem['tone']): string {
  switch (tone) {
    case 'rose':
      return 'bg-rose-50 text-rose-700 border-rose-200'
    case 'blue':
      return 'bg-blue-50 text-blue-700 border-blue-200'
    case 'emerald':
      return 'bg-emerald-50 text-emerald-700 border-emerald-200'
    case 'indigo':
      return 'bg-indigo-50 text-indigo-700 border-indigo-200'
    case 'amber':
      return 'bg-amber-50 text-amber-700 border-amber-200'
    default:
      return 'bg-slate-100 text-slate-600 border-slate-200'
  }
}

function getRollingDotClass(tone: FooterRollingItem['tone']): string {
  switch (tone) {
    case 'rose':
      return 'bg-rose-500'
    case 'blue':
      return 'bg-blue-500'
    case 'emerald':
      return 'bg-emerald-500'
    case 'indigo':
      return 'bg-indigo-500'
    case 'amber':
      return 'bg-amber-500'
    default:
      return 'bg-slate-400'
  }
}

let rollingTimer: number | null = null

function getConnectorStatusTextClass(status?: string | null): string {
  const key = getStatusMeta(status).key
  if (key === 'charging') return 'text-blue-700'
  if (key === 'standby') return 'text-emerald-700'
  if (key === 'preparing') return 'text-teal-700'
  if (key === 'finished') return 'text-violet-700'
  if (key === 'reserved') return 'text-lime-700'
  if (key === 'faulted' || key === 'commerr') return 'text-rose-700'
  if (key === 'inspecting' || key === 'paused') return 'text-amber-700'
  if (key === 'suspended') return 'text-purple-700'
  return 'text-slate-600'
}

function formatConnectorSubText(conn: ConnectorDetail): string {
  const key = getStatusMeta(conn.status).key
  if (key === 'charging') {
    const kw = Number(conn.powerKw ?? 0)
    const soc = conn.socPercent ?? 0
    return kw > 0 ? `${soc}%·${kw.toFixed(0)}kW` : `${soc}%`
  }
  if (key === 'finished') {
    const kwh = Number(conn.accumulatedKwh ?? 0)
    return kwh > 0 ? `${kwh.toFixed(1)}kWh` : '완료'
  }
  if (key === 'reserved') return '예약'
  if (key === 'paused') return '일시중지'
  if (key === 'disconnected') return '미연결'
  if (key === 'faulted' || key === 'commerr') return '점검'
  if (key === 'inspecting') return '점검중'
  if (key === 'suspended') return '중지'
  if (key === 'preparing') return '준비'
  if (key === 'unknown') return '미확인'
  return ''
}

function getFaultedDescription(chg: ChargerItem): string {
  const key = getStatusMeta(getChargerEffectiveStatus(chg)).key
  if (key === 'commerr') return '충전기 내부 통신 이상이 감지되었습니다. 통신선·모뎀 점검 또는 원격 재부팅을 시도해 주세요.'
  if (key === 'inspecting') return '현장 또는 원격 점검(Unavailable/Maintenance) 모드입니다. 점검 완료 후 대기 전환이 필요합니다.'
  if (key === 'suspended') return '운영 정책에 의해 사용이 중지된 단말입니다. 운영 재개 전까지 충전이 차단됩니다.'
  return '단말 오류 또는 하드웨어 이상 상태입니다. 원격 재부팅 후 상태를 확인해 주세요.'
}

function handleOutsideClick(e: MouseEvent) {
  if (stationDropdownRef.value && !stationDropdownRef.value.contains(e.target as Node)) {
    isStationDropdownOpen.value = false
  }
  if (footerWrapRef.value && !footerWrapRef.value.contains(e.target as Node)) {
    isFooterRolledUp.value = false
  }
}

function handleFooterKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && isFooterRolledUp.value) {
    isFooterRolledUp.value = false
  }
}

onMounted(() => {
  store.fetchInitialData(true)
  document.addEventListener('click', handleOutsideClick)
  window.addEventListener('keydown', handleFooterKeydown)
  rollingTimer = window.setInterval(() => {
    if (!isRollingPaused.value && !isRollingHovered.value && !isFooterRolledUp.value && rollingItems.value.length > 1) {
      nextRollingItem()
    }
  }, 3500)
})

onUnmounted(() => {
  document.removeEventListener('click', handleOutsideClick)
  window.removeEventListener('keydown', handleFooterKeydown)
  if (rollingTimer !== null) {
    clearInterval(rollingTimer)
    rollingTimer = null
  }
})
</script>

<template>
  <div class="w-full h-full flex flex-col gap-2.5 overflow-hidden select-none">
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
              <span class="px-2 py-0.5 rounded-full bg-sky-600 text-white text-[13px] font-bold flex-shrink-0 font-mono">
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
            <div class="px-2 py-1 flex items-center justify-between text-[13px] font-bold text-slate-400 border-b border-slate-100">
              <span>충전소 전환</span>
              <span class="text-[12px] text-sky-600 font-semibold">총 {{ store.stations.length }}개 지점</span>
            </div>

            <button
              type="button"
              @click="selectStation('ALL')"
              class="w-full flex items-center justify-between px-2.5 py-1.5 rounded-lg text-left transition-colors cursor-pointer"
              :class="!isPreviewSampleMode && store.curStFilter === 'ALL' ? 'bg-sky-50 text-sky-700' : 'hover:bg-slate-50 text-slate-700'"
            >
              <div class="flex flex-col">
                <span class="text-xs font-bold">전체 충전소 통합 관제</span>
                <span class="text-[12px] text-slate-500">전체 {{ store.chargers.length }}기</span>
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
                  <span class="text-[12px] text-slate-500 font-mono">ID {{ st.id }} · {{ st.chargerCount }}기</span>
                </div>
                <svg v-if="!isPreviewSampleMode && store.curStFilter === st.id" class="w-4 h-4 text-sky-600 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                </svg>
                <span v-else class="text-[12px] text-slate-400 font-semibold flex-shrink-0">선택</span>
              </button>
            </div>
          </div>
        </div>

        <!-- 충전소 ID 복사 배지 -->
        <button
          v-if="currentStationLabel.id !== 'ALL'"
          type="button"
          @click="copyText(currentStationLabel.id, '충전소 ID', $event)"
          class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-slate-100 hover:bg-sky-50 hover:text-sky-700 hover:border-sky-300 text-[13px] font-bold text-slate-600 border border-slate-200 cursor-pointer transition-all font-mono"
          title="클릭하여 충전소 ID 복사"
        >
          <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
          </svg>
          <span>충전소 ID {{ currentStationLabel.id }}</span>
        </button>

        <!-- 충전기·커넥터 요약 배지 -->
        <div class="hidden xl:inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-50 border border-slate-200 text-[13px] font-mono font-bold text-slate-600">
          <span class="inline-flex items-center gap-1 text-emerald-700">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
            <span>온라인 {{ onlineChargerCount }}</span>
          </span>
          <span class="text-slate-300">|</span>
          <span class="inline-flex items-center gap-1 text-slate-500">
            <span class="w-1.5 h-1.5 rounded-full bg-slate-400"></span>
            <span>오프라인 {{ offlineChargerCount }}</span>
          </span>
          <span class="text-slate-300">|</span>
          <span class="text-sky-700">커넥터 {{ totalConnectorCount }}개</span>
        </div>

        <!-- 데이터 소스 전환 (MySQL 실데이터 vs 전수 케이스 디자인 샘플) -->
        <button
          type="button"
          @click="isPreviewSampleMode = !isPreviewSampleMode"
          class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[13px] font-bold border transition-all cursor-pointer"
          :class="isPreviewSampleMode
            ? 'bg-violet-50 text-violet-700 border-violet-300'
            : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'"
          :title="`MySQL 실데이터와 전수 케이스 디자인 샘플(${SAMPLE_CHARGERS.length}기) 화면 전환`"
        >
          <span class="w-1.5 h-1.5 rounded-full" :class="isPreviewSampleMode ? 'bg-violet-500' : 'bg-emerald-500'"></span>
          <span>{{ isPreviewSampleMode ? `샘플 ${SAMPLE_CHARGERS.length}기 보기 중 (실데이터로 전환)` : `디자인 샘플 ${SAMPLE_CHARGERS.length}기 미리보기` }}</span>
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
            @click="activeFilter = 'preparing'"
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold transition-all cursor-pointer"
            :class="activeFilter === 'preparing' ? 'bg-white text-orange-700 shadow-2xs font-bold' : 'text-slate-600 hover:text-slate-900'"
          >
            <span class="w-2 h-2 rounded-full bg-orange-500"></span>
            <span>준비중</span>
            <span class="font-mono">{{ filterCounts.preparing }}</span>
          </button>
          <button
            type="button"
            @click="activeFilter = 'finished'"
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold transition-all cursor-pointer"
            :class="activeFilter === 'finished' ? 'bg-white text-violet-700 shadow-2xs font-bold' : 'text-slate-600 hover:text-slate-900'"
          >
            <span class="w-2 h-2 rounded-full bg-violet-500"></span>
            <span>완료</span>
            <span class="font-mono">{{ filterCounts.finished }}</span>
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
        <p class="text-xs text-slate-500 mt-1">검색어나 상태 필터를 변경하거나 상단 [디자인 샘플 {{ SAMPLE_CHARGERS.length }}기 미리보기]를 눌러 전체 상태 케이스 시안을 확인할 수 있습니다.</p>
      </div>

      <div
        v-else
        class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-2.5 auto-rows-fr"
      >
        <div
          v-for="chg in filteredChargers"
          :key="chg.chargeBoxId"
          @click="openChargerDetail(chg)"
          @contextmenu.prevent="openContextMenu($event, chg)"
          class="bg-white rounded-xl p-3 flex flex-col justify-between min-h-[280px] relative overflow-hidden group border shadow-xs hover:shadow-md transition-all duration-200 cursor-pointer"
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
                    class="px-1.5 py-0.2 rounded bg-slate-50 hover:bg-sky-50 text-slate-600 hover:text-sky-700 text-[12px] font-mono font-semibold border border-slate-200 cursor-pointer transition-colors"
                    title="충전기 ID 복사"
                  >
                    {{ chg.chargeBoxId }}
                  </button>
                  <span class="text-[12px] text-slate-400 truncate">{{ chg.stationName }}</span>
                </div>
              </div>
            </div>

            <!-- 충전기 상태 배지 (온라인 / 오프라인) -->
            <span
              class="px-2 py-0.5 rounded-full text-[12px] font-bold inline-flex items-center gap-1 border flex-shrink-0"
              :class="getChargerConnectionMeta(chg).badgeClass"
              :title="isChargerConnected(chg) ? '충전기 WebSocket 온라인' : '충전기 WebSocket 오프라인'"
            >
              <span
                class="w-1.5 h-1.5 rounded-full"
                :class="[getChargerConnectionMeta(chg).dotClass, isChargerConnected(chg) ? 'animate-pulse' : '']"
              ></span>
              <span>{{ getChargerConnectionMeta(chg).label }}</span>
            </span>
          </div>

          <!-- 카드 중앙 본문: 충전기 대표 상태별 직관 요약 -->
          <!-- A. 충전대기 (Standby) -->
          <div v-if="resolveCardBucket(chg) === 'standby'" class="my-auto flex flex-col items-center justify-center text-center py-2">
            <template v-if="getChargerConnectors(chg).length >= 4">
              <span class="font-extrabold text-2xl text-emerald-700 tracking-tight leading-none mb-1 font-mono">
                {{ getAvailableConnectorCount(chg) }} / {{ getChargerConnectors(chg).length }} 대
              </span>
              <p class="font-medium text-xs text-slate-500">모든 커넥터 즉시 사용 가능</p>
            </template>

            <template v-else>
              <div class="w-11 h-11 rounded-full bg-emerald-50 border border-emerald-200 flex items-center justify-center text-emerald-600 mb-1.5 group-hover:scale-105 transition-transform">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
              </div>
              <p class="font-bold text-sm text-slate-900 tracking-tight">비어있음</p>
              <p class="font-medium text-[13px] text-slate-500 mt-0.5">케이블을 꽂으면 바로 충전 시작</p>
            </template>
          </div>

          <!-- B. 충전중 (Charging) -->
          <div v-else-if="resolveCardBucket(chg) === 'charging'" class="my-auto flex flex-col items-center justify-center text-center py-1">
            <div class="flex items-baseline gap-1 text-blue-700 font-bold leading-none mb-1 font-mono">
              <span class="text-[13px] font-bold px-1.5 py-0.5 rounded bg-blue-100 text-blue-700 self-center">#{{ getPrimaryConnector(chg)?.connectorId ?? 1 }}</span>
              <span class="text-3xl font-black tracking-tight">{{ getPrimaryConnector(chg)?.socPercent ?? chg.socPercent ?? 0 }}</span>
              <span class="text-sm font-bold">%</span>
            </div>
            <div class="w-full bg-slate-200 rounded-full h-2.5 mb-1.5 overflow-hidden p-0.5">
              <div
                class="bg-gradient-to-r from-blue-600 via-sky-500 to-blue-500 charging-bar-shimmer h-full rounded-full transition-all duration-500"
                :style="{ width: `${Math.min(100, getPrimaryConnector(chg)?.socPercent ?? chg.socPercent ?? 0)}%` }"
              ></div>
            </div>
            <div class="inline-flex items-center gap-1 px-2 py-0.5 bg-blue-50 border border-blue-200 text-blue-800 rounded-full text-[12px] font-bold font-mono">
              <svg class="w-3 h-3 text-blue-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span>약 {{ estimateRemainMinutes(getPrimaryConnector(chg)?.socPercent ?? chg.socPercent ?? 0) }}분 후 완료 · {{ getChargerTotalPowerKw(chg).toFixed(1) }} kW</span>
            </div>
            <span class="text-[12px] text-slate-500 font-medium mt-1 truncate max-w-full">
              {{ getPrimaryConnector(chg)?.carModel || `현재 ${estimateCostWon(getPrimaryConnector(chg)?.accumulatedKwh ?? chg.accumulatedKwh ?? 0).toLocaleString()}원 충전 중` }}
            </span>
          </div>

          <!-- C. 충전준비중 / 일시중지 (Preparing / Paused) -->
          <div v-else-if="resolveCardBucket(chg) === 'preparing'" class="my-auto flex flex-col items-center justify-center text-center py-1">
            <template v-if="getStatusMeta(getChargerEffectiveStatus(chg)).key === 'paused'">
              <div class="w-11 h-11 rounded-full bg-amber-100 border border-amber-300 flex items-center justify-center text-amber-700 mb-1.5 group-hover:scale-105 transition-transform">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M10 9v6m4-6v6m7-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <p class="font-bold text-sm text-amber-800 tracking-tight">
                #{{ getPrimaryConnector(chg)?.connectorId ?? 1 }} 충전 일시중지
              </p>
              <p class="font-medium text-[13px] text-slate-500 mt-0.5 leading-snug px-1">
                {{ getPrimaryConnector(chg)?.status === 'SuspendedEV'
                  ? '차량(EV) 요청으로 충전이 일시 정지됨'
                  : getPrimaryConnector(chg)?.status === 'SuspendedEVSE'
                    ? '충전기(EVSE) 전력 제어로 일시 정지됨'
                    : '충전 세션이 일시중지 상태입니다' }}
              </p>
              <span v-if="(getPrimaryConnector(chg)?.socPercent ?? 0) > 0" class="inline-block mt-1 px-2 py-0.5 bg-amber-50 border border-amber-200 rounded-md text-[12px] text-amber-800 font-mono font-semibold">
                SoC {{ getPrimaryConnector(chg)?.socPercent }}% · {{ Number(getPrimaryConnector(chg)?.accumulatedKwh ?? 0).toFixed(1) }} kWh
              </span>
            </template>

            <template v-else>
              <div class="w-11 h-11 rounded-full bg-orange-100 border border-orange-300 flex items-center justify-center text-orange-600 mb-1.5 group-hover:scale-105 transition-transform">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
                </svg>
              </div>
              <p class="font-bold text-sm text-slate-900 tracking-tight">
                #{{ getPrimaryConnector(chg)?.connectorId ?? 1 }} 케이블 연결됨
              </p>
              <p class="font-medium text-[13px] text-slate-500 mt-0.5 leading-snug px-1">
                운전자가 인증 및 충전 시작을 준비 중입니다
              </p>
            </template>
          </div>

          <!-- D. 충전완료 / 예약중 (Finished / Reserved) -->
          <div v-else-if="resolveCardBucket(chg) === 'finished'" class="my-auto flex flex-col items-center justify-center text-center py-1">
            <template v-if="getStatusMeta(getChargerEffectiveStatus(chg)).key === 'reserved'">
              <div class="w-11 h-11 rounded-full bg-lime-100 border border-lime-300 flex items-center justify-center text-lime-700 mb-1.5 group-hover:scale-105 transition-transform">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
              </div>
              <p class="font-bold text-sm text-slate-900 tracking-tight">
                #{{ getPrimaryConnector(chg)?.connectorId ?? 1 }} 예약 대기 중
              </p>
              <p class="font-medium text-[13px] text-slate-500 mt-0.5">
                {{ getPrimaryConnector(chg)?.carModel || '예약 차량 도착 및 태그 인증 대기' }}
              </p>
              <span v-if="getPrimaryConnector(chg)?.userTag" class="inline-block mt-1 px-2 py-0.5 bg-lime-50 border border-lime-200 rounded-md text-[12px] text-lime-800 font-mono font-semibold">
                예약 태그 {{ getPrimaryConnector(chg)?.userTag }}
              </span>
            </template>

            <template v-else>
              <div class="w-11 h-11 rounded-full bg-violet-100 border border-violet-300 flex items-center justify-center text-violet-600 mb-1.5 group-hover:scale-105 transition-transform">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <p class="font-bold text-sm text-slate-900 tracking-tight">
                #{{ getPrimaryConnector(chg)?.connectorId ?? 1 }} 충전 끝 ({{ getPrimaryConnector(chg)?.socPercent || 100 }}%)
              </p>
              <p class="font-medium text-[13px] text-slate-500 mt-0.5">
                누적 {{ Number(getPrimaryConnector(chg)?.accumulatedKwh ?? chg.accumulatedKwh ?? 0).toFixed(1) }} kWh · 예상 {{ estimateCostWon(getPrimaryConnector(chg)?.accumulatedKwh ?? chg.accumulatedKwh ?? 0).toLocaleString() }}원
              </p>
              <span class="inline-block mt-1 px-2 py-0.5 bg-slate-50 border border-slate-200 rounded-md text-[12px] text-violet-700 font-semibold">
                커넥터 분리 후 출차 대기
              </span>
            </template>
          </div>

          <!-- E. 고장/통신이상/점검중/운영중지 (Faulted) -->
          <div v-else-if="resolveCardBucket(chg) === 'faulted'" class="my-auto flex flex-col items-center justify-center text-center py-1">
            <div class="w-11 h-11 rounded-full bg-rose-100 border border-rose-300 flex items-center justify-center text-rose-600 mb-1 group-hover:scale-105 transition-transform">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <p class="font-bold text-sm text-rose-600 tracking-tight">
              #{{ getPrimaryConnector(chg)?.connectorId ?? 1 }} {{ getStatusMeta(getChargerEffectiveStatus(chg)).name }}
            </p>
            <div class="mt-1 p-1.5 bg-rose-50 border border-rose-200 rounded-lg text-left w-full">
              <p class="text-[12px] text-rose-800 font-medium leading-snug">
                {{ getFaultedDescription(chg) }}
              </p>
            </div>
          </div>

          <!-- F. 연결안됨 / 알수없음 (Disconnected / Unknown) -->
          <div v-else class="my-auto flex flex-col items-center justify-center text-center py-2">
            <div class="w-11 h-11 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-400 mb-1.5">
              <svg v-if="isChargerConnected(chg)" class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <svg v-else class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M18.364 5.636a9 9 0 010 12.728M5.636 5.636a9 9 0 000 12.728M15.536 8.464a5 5 0 010 7.072M8.464 8.464a5 5 0 000 7.072M3 3l18 18" />
              </svg>
            </div>
            <p class="font-bold text-sm text-slate-700 tracking-tight">
              {{ isChargerConnected(chg) ? '상태 알수없음' : '연결안됨' }}
            </p>
            <p class="font-medium text-[12px] text-slate-400 mt-0.5 font-mono">
              최종 수신: {{ chg.lastHeartbeat || '-' }}
            </p>
          </div>

          <!-- 공통: 커넥터별 실시간 상태 (모든 충전기 카드에 항상 표시) -->
          <div class="mt-2 pt-2 border-t border-slate-100 space-y-1">
            <div class="flex items-center justify-between text-[12px]">
              <span class="font-bold text-slate-500">커넥터 상태</span>
              <span class="font-mono text-slate-400">
                {{ getChargerConnectors(chg).length }}채널<template v-if="isChargerConnected(chg)"> · 가용 {{ getAvailableConnectorCount(chg) }}</template>
              </span>
            </div>
            <div
              class="grid gap-1"
              :class="getChargerConnectors(chg).length === 1 ? 'grid-cols-1' : 'grid-cols-2'"
            >
              <button
                v-for="conn in getChargerConnectors(chg)"
                :key="`${chg.chargeBoxId}-conn-${conn.connectorId}`"
                type="button"
                @click.stop="openChargerDetail(chg, conn.connectorId)"
                @contextmenu.prevent.stop="openContextMenu($event, chg, conn.connectorId)"
                class="px-1.5 py-1 rounded-lg border text-left transition-all cursor-pointer flex items-center justify-between gap-1 hover:border-sky-400"
                :class="detailChargerId === chg.chargeBoxId && detailConnectorId === conn.connectorId
                  ? 'border-sky-500 ring-1 ring-sky-400 bg-sky-50/70'
                  : 'bg-slate-50/90 border-slate-200/90 hover:bg-sky-50/50'"
                :title="`#${conn.connectorId} 커넥터 상세 보기 (${getStatusMeta(conn.status).name})`"
              >
                <div class="flex items-center gap-1 min-w-0">
                  <span class="font-mono font-extrabold text-[12px] text-slate-600 flex-shrink-0">#{{ conn.connectorId }}</span>
                  <span class="w-1.5 h-1.5 rounded-full flex-shrink-0" :class="getStatusMeta(conn.status).dotClass"></span>
                  <span class="text-[12px] font-bold truncate" :class="getConnectorStatusTextClass(conn.status)">
                    {{ getStatusMeta(conn.status).name }}
                  </span>
                </div>
                <span v-if="formatConnectorSubText(conn)" class="text-[11px] font-mono text-slate-500 flex-shrink-0">
                  {{ formatConnectorSubText(conn) }}
                </span>
              </button>
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

    <!-- 3. 하단 롤링 + 롤업 상태 바 -->
    <div ref="footerWrapRef" class="relative w-full flex-shrink-0 z-20">
      <!-- 3-A. 롤업(Roll-up) 확장 패널 -->
      <Transition name="footer-rollup">
        <section
          v-if="isFooterRolledUp"
          class="absolute left-0 right-0 bottom-full mb-2 bg-white/98 backdrop-blur-md border border-slate-200 rounded-xl shadow-2xl overflow-hidden"
        >
          <!-- 롤업 패널 헤더 -->
          <div class="px-4 py-2.5 bg-slate-50 border-b border-slate-200 flex flex-wrap items-center justify-between gap-2">
            <div class="flex items-center gap-2">
              <span class="w-2 h-2 rounded-full bg-sky-500 animate-pulse"></span>
              <span class="text-xs font-extrabold text-slate-900">실시간 관제 롤링 피드 &amp; 운전 요약</span>
              <span class="px-1.5 py-0.2 rounded-full bg-sky-50 text-sky-700 border border-sky-200 text-[12px] font-mono font-bold">
                {{ rollingItems.length }}건
              </span>
            </div>
            <div class="flex items-center gap-1.5 text-[13px]">
              <button
                type="button"
                @click="isRollingPaused = !isRollingPaused"
                class="px-2 py-1 rounded-lg border font-bold inline-flex items-center gap-1 transition-colors cursor-pointer"
                :class="isRollingPaused
                  ? 'bg-amber-50 text-amber-700 border-amber-200 hover:bg-amber-100'
                  : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-100'"
                :title="isRollingPaused ? '자동 롤링 재개' : '자동 롤링 일시정지'"
              >
                <svg v-if="isRollingPaused" class="w-3 h-3 fill-current" viewBox="0 0 24 24"><path d="M8 5v14l11-7z" /></svg>
                <svg v-else class="w-3 h-3 fill-current" viewBox="0 0 24 24"><path d="M6 5h4v14H6zm8 0h4v14h-4z" /></svg>
                <span>{{ isRollingPaused ? '롤링 정지됨' : '자동 롤링 중' }}</span>
              </button>
              <button
                type="button"
                @click="isFooterRolledUp = false"
                class="px-2 py-1 rounded-lg bg-white hover:bg-slate-100 text-slate-600 border border-slate-200 font-bold inline-flex items-center gap-1 transition-colors cursor-pointer"
                title="롤업 패널 접기 (ESC)"
              >
                <span>접기</span>
                <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
                </svg>
              </button>
            </div>
          </div>

          <!-- 롤업 패널 본문: 4열 미니 KPI + 전체 롤링 피드 -->
          <div class="p-3 space-y-2.5">
            <div class="grid grid-cols-2 lg:grid-cols-4 gap-2">
              <button
                type="button"
                @click="activeFilter = 'ALL'; isFooterRolledUp = false"
                class="p-2.5 rounded-xl bg-slate-50 hover:bg-emerald-50/60 border border-slate-200 text-left transition-colors cursor-pointer"
              >
                <div class="text-[12px] font-bold text-slate-500">온라인 / 전체 충전기</div>
                <div class="mt-0.5 flex items-baseline justify-between font-mono">
                  <span class="text-sm font-black text-emerald-700">{{ onlineChargerCount }} / {{ sourceChargers.length }}기</span>
                  <span class="text-[12px] text-slate-500">커넥터 {{ totalConnectorCount }}개</span>
                </div>
              </button>

              <button
                type="button"
                @click="activeFilter = 'charging'; isFooterRolledUp = false"
                class="p-2.5 rounded-xl bg-slate-50 hover:bg-blue-50/70 border border-slate-200 text-left transition-colors cursor-pointer"
              >
                <div class="text-[12px] font-bold text-slate-500">실시간 합계 출력</div>
                <div class="mt-0.5 flex items-baseline justify-between font-mono">
                  <span class="text-sm font-black text-blue-700">{{ totalLivePowerKw.toFixed(1) }} kW</span>
                  <span class="text-[12px] text-slate-500">충전중 {{ filterCounts.charging }}기</span>
                </div>
              </button>

              <div class="p-2.5 rounded-xl bg-slate-50 border border-slate-200">
                <div class="text-[12px] font-bold text-slate-500">금일 누적 전력량 · TPS</div>
                <div class="mt-0.5 flex items-baseline justify-between font-mono">
                  <span class="text-sm font-black text-indigo-700">{{ (Number(store.summary.todayTotalKwh) || 0).toFixed(1) }} kWh</span>
                  <span class="text-[12px] text-slate-500">{{ store.summary.liveTps.toLocaleString() }} TPS</span>
                </div>
              </div>

              <button
                type="button"
                @click="activeFilter = filterCounts.faulted > 0 ? 'faulted' : 'disconnected'; isFooterRolledUp = false"
                class="p-2.5 rounded-xl bg-slate-50 hover:bg-rose-50/70 border border-slate-200 text-left transition-colors cursor-pointer"
              >
                <div class="text-[12px] font-bold text-slate-500">고장 · 오프라인 현황</div>
                <div class="mt-0.5 flex items-baseline justify-between font-mono">
                  <span class="text-sm font-black" :class="filterCounts.faulted > 0 ? 'text-rose-600' : 'text-slate-700'">고장 {{ filterCounts.faulted }}기</span>
                  <span class="text-[12px] text-slate-500">오프라인 {{ offlineChargerCount }}기</span>
                </div>
              </button>
            </div>

            <!-- 전체 롤링 항목 리스트 -->
            <div class="max-h-52 overflow-y-auto divide-y divide-slate-100 border border-slate-200 rounded-xl bg-white">
              <button
                v-for="(item, idx) in rollingItems"
                :key="item.id"
                type="button"
                @click="handleRollingItemClick(item)"
                class="w-full px-3 py-2 text-left flex items-center justify-between gap-3 transition-colors cursor-pointer"
                :class="idx === (rollingIndex % rollingItems.length)
                  ? 'bg-sky-50/70'
                  : 'hover:bg-slate-50'"
              >
                <div class="flex items-center gap-2 min-w-0">
                  <span class="text-[12px] font-mono font-bold text-slate-400 w-4 text-right shrink-0">{{ idx + 1 }}</span>
                  <span
                    class="px-1.5 py-0.2 rounded text-[12px] font-bold border shrink-0 inline-flex items-center gap-1"
                    :class="getRollingBadgeClass(item.tone)"
                  >
                    <span class="w-1.5 h-1.5 rounded-full" :class="getRollingDotClass(item.tone)"></span>
                    <span>{{ item.badge }}</span>
                  </span>
                  <span class="text-xs font-bold text-slate-800 truncate">{{ item.title }}</span>
                  <span class="hidden md:inline text-[13px] text-slate-500 truncate">{{ item.detail }}</span>
                </div>
                <span
                  v-if="item.charger || item.filterKey"
                  class="text-[12px] font-semibold text-sky-600 shrink-0 inline-flex items-center gap-0.5"
                >
                  <span>{{ item.charger ? '상세' : '필터' }}</span>
                  <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
                  </svg>
                </span>
              </button>
            </div>
          </div>
        </section>
      </Transition>

      <!-- 3-B. 하단 고정 롤링 상태 바 -->
      <footer
        @mouseenter="isRollingHovered = true"
        @mouseleave="isRollingHovered = false"
        class="w-full bg-white rounded-xl px-3 py-1.5 border border-slate-200 shadow-2xs flex items-center justify-between gap-2.5 text-[13px] text-slate-500"
      >
        <!-- 좌측: 롤링 컨트롤 (이전/다음/일시정지 + 카운터) -->
        <div class="flex items-center gap-1 flex-shrink-0">
          <button
            type="button"
            @click="prevRollingItem"
            class="p-1 rounded-md text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors cursor-pointer"
            title="이전 피드"
          >
            <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M5 15l7-7 7 7" />
            </svg>
          </button>
          <button
            type="button"
            @click="nextRollingItem"
            class="p-1 rounded-md text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors cursor-pointer"
            title="다음 피드"
          >
            <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
            </svg>
          </button>
          <button
            type="button"
            @click="isRollingPaused = !isRollingPaused"
            class="p-1 rounded-md transition-colors cursor-pointer"
            :class="isRollingPaused ? 'text-amber-600 bg-amber-50 hover:bg-amber-100' : 'text-slate-400 hover:text-slate-700 hover:bg-slate-100'"
            :title="isRollingPaused ? '자동 롤링 재생' : '자동 롤링 일시정지'"
          >
            <svg v-if="isRollingPaused" class="w-3 h-3 fill-current" viewBox="0 0 24 24"><path d="M8 5v14l11-7z" /></svg>
            <svg v-else class="w-3 h-3 fill-current" viewBox="0 0 24 24"><path d="M6 5h4v14H6zm8 0h4v14h-4z" /></svg>
          </button>
          <span class="hidden sm:inline-block font-mono text-[12px] text-slate-400 px-1 min-w-[28px] text-center">
            {{ rollingItems.length > 0 ? (rollingIndex % rollingItems.length) + 1 : 0 }}/{{ rollingItems.length }}
          </span>
        </div>

        <!-- 중앙: 수직 롤링 티커 -->
        <div class="relative flex-1 min-w-0 h-7 overflow-hidden">
          <Transition :name="rollingDirection === 'up' ? 'ticker-roll-up' : 'ticker-roll-down'">
            <button
              v-if="currentRollingItem"
              :key="currentRollingItem.id"
              type="button"
              @click="handleRollingItemClick(currentRollingItem)"
              class="absolute inset-0 w-full flex items-center gap-2 text-left rounded-md px-1.5 hover:bg-slate-50 transition-colors cursor-pointer"
              :title="`${currentRollingItem.title} — ${currentRollingItem.detail}`"
            >
              <span
                class="px-1.5 py-0.2 rounded text-[12px] font-bold border shrink-0 inline-flex items-center gap-1"
                :class="getRollingBadgeClass(currentRollingItem.tone)"
              >
                <span class="w-1.5 h-1.5 rounded-full" :class="getRollingDotClass(currentRollingItem.tone)"></span>
                <span>{{ currentRollingItem.badge }}</span>
              </span>
              <span class="text-[13px] font-bold text-slate-800 truncate">{{ currentRollingItem.title }}</span>
              <span class="hidden xl:inline text-[13px] text-slate-500 truncate">{{ currentRollingItem.detail }}</span>
            </button>
          </Transition>
        </div>

        <!-- 우측: 서버 상태 + 표시 건수 + 롤업 토글 버튼 -->
        <div class="flex items-center gap-2.5 flex-shrink-0">
          <span class="hidden md:inline-flex items-center gap-1.5 font-mono font-semibold" :class="store.isDbConnected ? 'text-emerald-700' : 'text-slate-400'">
            <span class="w-1.5 h-1.5 rounded-full" :class="store.isDbConnected ? 'bg-emerald-500' : 'bg-slate-400'"></span>
            <span>{{ store.isDbConnected ? '관제 서버 온라인' : '서버 연결 대기' }}</span>
          </span>
          <span class="hidden lg:inline font-mono text-slate-500">
            표시 {{ filteredChargers.length }}/{{ sourceChargers.length }}기 · 커넥터 {{ totalConnectorCount }}개
          </span>
          <button
            type="button"
            @click="toggleFooterRollup"
            class="px-2.5 py-1 rounded-lg border text-[13px] font-bold inline-flex items-center gap-1 transition-all cursor-pointer shadow-2xs"
            :class="isFooterRolledUp
              ? 'bg-sky-600 text-white border-sky-600 hover:bg-sky-700'
              : 'bg-slate-50 hover:bg-sky-50 text-slate-700 hover:text-sky-700 border-slate-200 hover:border-sky-200'"
            :title="isFooterRolledUp ? '하단 롤업 패널 접기' : '하단 롤업 패널 펼치기'"
          >
            <span>{{ isFooterRolledUp ? '접기' : '롤업' }}</span>
            <svg
              class="w-3 h-3 transition-transform duration-200"
              :class="isFooterRolledUp ? 'rotate-180' : ''"
              fill="none"
              stroke="currentColor"
              stroke-width="2.4"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M5 15l7-7 7 7" />
            </svg>
          </button>
        </div>
      </footer>
    </div>

    <!-- 4. 우측 슬라이드 충전기 상세·원격제어 드로어 -->
    <div
      v-if="detailCharger"
      @click="closeChargerDetail"
      class="fixed inset-0 bg-slate-900/30 z-40"
    ></div>

    <aside
      v-if="detailCharger"
      class="fixed top-[60px] right-3 bottom-3 w-[356px] max-w-[92vw] bg-white border border-slate-200 rounded-2xl shadow-2xl p-4 z-50 flex flex-col justify-between overflow-y-auto"
    >
      <div class="space-y-3">
        <div class="flex items-center justify-between pb-2.5 border-b border-slate-200">
          <div>
            <div class="flex items-center gap-1.5">
              <span class="font-mono font-extrabold text-sm text-sky-700">{{ detailCharger.chargeBoxId }}</span>
              <span
                class="px-2 py-0.5 rounded-full text-[12px] font-bold border"
                :class="getStatusMeta(detailConnector?.status ?? detailCharger.status).badgeClass"
              >
                #{{ detailConnector?.connectorId ?? 1 }} {{ getStatusMeta(detailConnector?.status ?? detailCharger.status).name }}
              </span>
            </div>
            <div class="text-xs font-bold text-slate-800 mt-0.5">{{ detailCharger.stationName }}</div>
            <div class="text-[12px] text-slate-500">{{ detailCharger.vendor }} · {{ detailCharger.model }}</div>
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

        <!-- 충전기 상태 요약 (온라인 / 오프라인) -->
        <div class="p-2.5 rounded-xl bg-slate-50 border border-slate-200 flex items-center justify-between gap-2">
          <span class="text-[13px] font-bold text-slate-600">충전기 상태</span>
          <span
            class="px-2 py-0.5 rounded-full text-[12px] font-bold border inline-flex items-center gap-1"
            :class="getChargerConnectionMeta(detailCharger).badgeClass"
          >
            <span class="w-1.5 h-1.5 rounded-full" :class="getChargerConnectionMeta(detailCharger).dotClass"></span>
            <span>{{ getChargerConnectionMeta(detailCharger).label }}</span>
          </span>
        </div>

        <!-- 커넥터별 상태 · 채널 선택 -->
        <div class="space-y-1.5">
          <div class="flex items-center justify-between text-[13px] font-bold text-slate-500">
            <span>커넥터별 상태 · 채널 선택</span>
            <span class="font-mono text-sky-600">{{ detailConnectors.length }}채널</span>
          </div>
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
                <span class="text-[12px] font-bold px-1.5 py-0.2 rounded border" :class="getStatusMeta(conn.status).badgeClass">
                  {{ getStatusMeta(conn.status).name }}
                </span>
              </div>
              <div class="text-[12px] font-mono text-slate-500 mt-1">
                {{ (conn.powerKw ?? 0) > 0 ? `${conn.powerKw.toFixed(1)} kW` : (getStatusMeta(conn.status).key === 'disconnected' ? '미연결' : '대기 0.0 kW') }}
              </div>
            </button>
          </div>
        </div>

        <!-- 계측치 카드 -->
        <div class="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-2.5">
          <div class="flex items-center justify-between text-xs font-bold text-slate-600">
            <span>#{{ detailConnector?.connectorId ?? 1 }} 계측 정보</span>
            <span class="font-mono text-[12px]" :class="isChargerConnected(detailCharger) ? 'text-emerald-600' : 'text-slate-400'">
              {{ isChargerConnected(detailCharger) ? '실시간 연결' : '미연결' }}
            </span>
          </div>
          <div class="grid grid-cols-2 gap-2 font-mono text-xs">
            <div class="p-2 rounded-lg bg-white border border-slate-200">
              <div class="text-[12px] text-slate-500 font-sans">순시 출력</div>
              <div class="font-bold text-sky-700 mt-0.5">{{ (detailConnector?.powerKw ?? 0).toFixed(1) }} kW</div>
            </div>
            <div class="p-2 rounded-lg bg-white border border-slate-200">
              <div class="text-[12px] text-slate-500 font-sans">배터리 SoC</div>
              <div class="font-bold text-emerald-600 mt-0.5">{{ detailConnector?.socPercent ?? 0 }} %</div>
            </div>
            <div class="p-2 rounded-lg bg-white border border-slate-200">
              <div class="text-[12px] text-slate-500 font-sans">전압 / 전류</div>
              <div class="font-bold text-slate-800 mt-0.5">{{ (detailConnector?.voltageV ?? 0).toFixed(0) }}V / {{ (detailConnector?.currentA ?? 0).toFixed(0) }}A</div>
            </div>
            <div class="p-2 rounded-lg bg-white border border-slate-200">
              <div class="text-[12px] text-slate-500 font-sans">누적 전력량</div>
              <div class="font-bold text-slate-800 mt-0.5">{{ (detailConnector?.accumulatedKwh ?? 0).toFixed(1) }} kWh</div>
            </div>
          </div>
        </div>

        <!-- OCPP 원격 제어 -->
        <div class="space-y-2">
          <div class="text-[13px] font-bold text-slate-500">OCPP 원격 제어 (#{{ detailConnector?.connectorId ?? 1 }})</div>
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

      <div class="pt-3 border-t border-slate-100 text-[12px] text-slate-400 font-mono flex justify-between">
        <span>프로토콜: {{ detailCharger.protocol }}</span>
        <span>수신: {{ detailCharger.lastHeartbeat }}</span>
      </div>
    </aside>

    <!-- 5. 우클릭 플로팅 컨텍스트 메뉴 -->
    <ChargerContextMenu
      ref="contextMenuRef"
      :chargers="sourceChargers"
      :is-sample-mode="isPreviewSampleMode"
      @open-detail="openChargerDetail"
    />
  </div>
</template>

<style scoped>
/* 하단바 수직 롤링 티커 (위로) */
.ticker-roll-up-enter-active,
.ticker-roll-up-leave-active,
.ticker-roll-down-enter-active,
.ticker-roll-down-leave-active {
  transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.25s ease;
}

.ticker-roll-up-enter-from {
  transform: translateY(100%);
  opacity: 0;
}
.ticker-roll-up-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}

/* 하단바 수직 롤링 티커 (아래로 - 이전 버튼) */
.ticker-roll-down-enter-from {
  transform: translateY(-100%);
  opacity: 0;
}
.ticker-roll-down-leave-to {
  transform: translateY(100%);
  opacity: 0;
}

/* 하단바 위로 펼쳐지는 롤업(Roll-up) 패널 트랜지션 */
.footer-rollup-enter-active,
.footer-rollup-leave-active {
  transition: transform 0.24s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.2s ease;
  transform-origin: bottom center;
}
.footer-rollup-enter-from,
.footer-rollup-leave-to {
  transform: translateY(12px) scaleY(0.96);
  opacity: 0;
}
</style>
