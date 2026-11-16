<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useVirtualCharger } from '../composables/useVirtualCharger'
import type { ChargerInstance, OcppChargePointStatus } from '../types/ocpp'

const props = defineProps<{
  selectedId: string
}>()

const emit = defineEmits<{
  (e: 'select', id: string): void
  (e: 'open-add-modal'): void
  (e: 'open-edit-modal', charger: ChargerInstance): void
}>()

const store = useVirtualCharger()

// 1. 뷰 모드: 'matrix' (바둑판 매트릭스) | 'list' (상세 목록)
const viewMode = ref<'matrix' | 'list'>('matrix')

// 2. 검색 및 필터
const searchQuery = ref('')
const statusFilter = ref<'ALL' | 'CONNECTED' | 'DISCONNECTED'>('ALL')

// 3. 🖱️ 우클릭 플로팅 컨텍스트 메뉴 상태
const isContextMenuOpen = ref(false)
const contextMenuPos = ref({ x: 0, y: 0 })
const contextCharger = ref<ChargerInstance | null>(null)

function openContextMenu(e: MouseEvent, charger: ChargerInstance) {
  e.preventDefault()
  e.stopPropagation()
  // 우클릭 시 해당 충전기 자동 선택
  handleSelect(charger.id)
  contextCharger.value = charger

  const menuWidth = 360
  const menuHeight = 560
  let posX = e.clientX
  let posY = e.clientY

  if (posX + menuWidth > window.innerWidth) {
    posX = Math.max(10, window.innerWidth - menuWidth - 15)
  }
  if (posY + menuHeight > window.innerHeight) {
    posY = Math.max(10, window.innerHeight - menuHeight - 15)
  }

  contextMenuPos.value = { x: posX, y: posY }
  isContextMenuOpen.value = true
}

function handleContextSelectConnector(connId: number) {
  if (!contextCharger.value) return
  contextCharger.value.activeConnectorId = connId
  if (store.setActiveConnector) {
    store.setActiveConnector(connId, contextCharger.value.id)
  }
}

function closeContextMenu() {
  if (isContextMenuOpen.value) {
    isContextMenuOpen.value = false
    contextCharger.value = null
  }
}

onMounted(() => {
  window.addEventListener('click', closeContextMenu)
})

onUnmounted(() => {
  window.removeEventListener('click', closeContextMenu)
})

// 우클릭 메뉴 액션 핸들러들
function handleContextToggleConnect() {
  if (!contextCharger.value) return
  const c = contextCharger.value
  closeContextMenu()
  if (c.connected) {
    store.disconnect(c.id)
  } else {
    store.connect(c.id)
  }
}

function handleContextFilterTerminal() {
  if (!contextCharger.value) return
  store.setTerminalSearchQuery(contextCharger.value.id)
  closeContextMenu()
}

async function handleContextBootSequence() {
  if (!contextCharger.value) return
  const cid = contextCharger.value.id
  closeContextMenu()
  await store.runBootSequence(cid)
}

function handleContextSendBootNotification() {
  if (!contextCharger.value) return
  const c = contextCharger.value
  closeContextMenu()
  store.sendBootNotification(c.vendor, c.model, c.firmwareVer || '1.0.0-RELEASE', c.id)
}

function handleContextSendHeartbeat() {
  if (!contextCharger.value) return
  const cid = contextCharger.value.id
  closeContextMenu()
  store.sendHeartbeat(cid)
}

function handleContextToggleHeartbeat() {
  if (!contextCharger.value) return
  const cid = contextCharger.value.id
  closeContextMenu()
  store.toggleAutoHeartbeat(cid)
}

function handleContextSendMeterValues() {
  if (!contextCharger.value) return
  const cid = contextCharger.value.id
  closeContextMenu()
  store.sendMeterValues(500, cid)
}

function handleContextChangeStatus(st: OcppChargePointStatus) {
  if (!contextCharger.value) return
  const cid = contextCharger.value.id
  closeContextMenu()
  store.sendStatusNotification(st, 'NoError', cid)
}

function handleContextOpenEdit() {
  if (!contextCharger.value) return
  const c = contextCharger.value
  closeContextMenu()
  emit('open-edit-modal', c)
}

function handleContextDelete() {
  if (!contextCharger.value) return
  const cid = contextCharger.value.id
  closeContextMenu()
  handleDelete(new MouseEvent('click'), cid)
}

// 필터링된 충전기 목록
const filteredChargers = computed(() => {
  return store.chargers.filter(c => {
    // 1. 검색어 필터
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase().trim()
      const matchId = c.id.toLowerCase().includes(q)
      const matchName = c.name.toLowerCase().includes(q)
      const matchSt = c.stationId.toLowerCase().includes(q)
      if (!matchId && !matchName && !matchSt) return false
    }

    // 2. 상태 필터
    if (statusFilter.value === 'CONNECTED') {
      return c.connected
    }
    if (statusFilter.value === 'DISCONNECTED') {
      return !c.connected
    }

    return true
  })
})

// 충전소별 그룹핑 (매트릭스 뷰용)
interface StationGroup {
  stationId: string
  stationName: string
  chargers: ChargerInstance[]
}

const stationGroups = computed<StationGroup[]>(() => {
  const map = new Map<string, ChargerInstance[]>()
  for (const c of filteredChargers.value) {
    const stId = c.stationId || '000000'
    if (!map.has(stId)) {
      map.set(stId, [])
    }
    map.get(stId)!.push(c)
  }

  const result: StationGroup[] = []
  map.forEach((items, stId) => {
    // 충전기 번호순 정렬
    items.sort((a, b) => a.id.localeCompare(b.id))
    result.push({
      stationId: stId,
      stationName: items[0]?.stationName || items[0]?.name.split('-')[0]?.trim() || `충전소 ${stId}`,
      chargers: items
    })
  })

  return result.sort((a, b) => a.stationId.localeCompare(b.stationId))
})

// 총 오류 건수
const totalErrorCount = computed(() => {
  return store.chargers.filter(c => c.lastError || c.status === 'Faulted').length
})

function handleSelect(id: string) {
  emit('select', id)
  store.selectCharger(id)
}

function handleToggleConnect(e: Event, c: ChargerInstance) {
  e.stopPropagation()
  if (c.connected) {
    store.disconnect(c.id)
  } else {
    store.connect(c.id)
  }
}

function handleEdit(e: Event, c: ChargerInstance) {
  e.stopPropagation()
  emit('open-edit-modal', c)
}

function handleDelete(e: Event, id: string) {
  e.stopPropagation()
  if (confirm(`충전기 [${id}]를 삭제하시겠습니까?`)) {
    store.deleteCharger(id)
  }
}

// 4. 커넥터 상태 시각화 헬퍼 (기본 2개 커넥터 지원)
function getConnectors(c: ChargerInstance) {
  if (c.connectors && c.connectors.length >= 2) {
    return c.connectors
  }
  return [
    { connectorId: 1, status: c.connectors?.[0]?.status || c.status || 'Available' },
    { connectorId: 2, status: 'Available' }
  ]
}

function getConnectorBadgeClass(charger: ChargerInstance, status: string) {
  if (!charger.connected || status === 'Unavailable') {
    return 'bg-slate-100 text-slate-400 border-slate-200/80'
  }
  switch (status) {
    case 'Charging':
      return 'bg-indigo-50 text-indigo-700 border-indigo-200 font-bold animate-pulse'
    case 'Preparing':
      return 'bg-amber-50 text-amber-700 border-amber-200 font-medium'
    case 'SuspendedEVSE':
    case 'SuspendedEV':
      return 'bg-blue-50 text-blue-700 border-blue-200 font-medium'
    case 'Finishing':
      return 'bg-purple-50 text-purple-700 border-purple-200 font-medium'
    case 'Faulted':
      return 'bg-rose-50 text-rose-700 border-rose-200 font-bold'
    case 'Available':
      return 'bg-emerald-50 text-emerald-700 border-emerald-200 font-semibold'
    default:
      return 'bg-slate-100 text-slate-400 border-slate-200/80'
  }
}

function getConnectorStatusText(charger: ChargerInstance, status: string) {
  if (!charger.connected || status === 'Unavailable') return '오프'
  switch (status) {
    case 'Charging': return '충전'
    case 'Preparing': return '준비'
    case 'SuspendedEVSE':
    case 'SuspendedEV': return '일시'
    case 'Finishing': return '종료'
    case 'Faulted': return '고장'
    case 'Available': return '대기'
    default: return '오프'
  }
}

// 5. 충전기 연결 및 가동 상태별 배경/테두리 클래스 헬퍼
function getChargerTileClass(c: ChargerInstance) {
  const isSelected = (props.selectedId || store.selectedChargerId.value) === c.id

  // 1. 에러 상태
  if (c.lastError) {
    return isSelected
      ? 'bg-rose-50/90 border-rose-500 ring-2 ring-rose-300 font-bold shadow-2xs'
      : 'bg-rose-50/60 border-rose-300 hover:bg-rose-50 hover:border-rose-400'
  }

  // 2. 현재 선택된 충전기
  if (isSelected) {
    if (c.connected) {
      const isCharging = c.connectors?.some(conn => conn.status === 'Charging')
      if (isCharging) {
        return 'bg-indigo-50/90 border-indigo-500 ring-2 ring-indigo-200 font-bold shadow-2xs'
      }
      return 'bg-emerald-50/90 border-emerald-500 ring-2 ring-emerald-200 font-bold shadow-2xs'
    }
    if (c.connecting) {
      return 'bg-amber-50/90 border-amber-500 ring-2 ring-amber-200 font-bold animate-pulse shadow-2xs'
    }
    return 'bg-sky-50/90 border-sky-500 ring-2 ring-sky-200 font-bold shadow-2xs'
  }

  // 3. 미선택 - 연결 진행 중 (Connecting)
  if (c.connecting) {
    return 'bg-amber-50/70 border-amber-300 hover:bg-amber-50/90 hover:border-amber-400 animate-pulse'
  }

  // 4. 미선택 - 연결 완료 (Connected / Online)
  if (c.connected) {
    const isCharging = c.connectors?.some(conn => conn.status === 'Charging')
    if (isCharging) {
      return 'bg-indigo-50/60 border-indigo-200 hover:bg-indigo-50/80 hover:border-indigo-300 shadow-2xs'
    }
    return 'bg-emerald-50/50 border-emerald-200/90 hover:bg-emerald-50/80 hover:border-emerald-300 shadow-2xs'
  }

  // 5. 미선택 - 미연결 (Offline)
  return 'bg-white border-slate-200 hover:border-slate-300 hover:bg-slate-50/80 shadow-2xs'
}

function getChargerListCardClass(c: ChargerInstance) {
  const isSelected = (props.selectedId || store.selectedChargerId.value) === c.id

  if (c.lastError) {
    return isSelected
      ? 'bg-rose-50 border-rose-400 ring-2 ring-rose-200 shadow-2xs'
      : 'bg-rose-50/60 border-rose-200 hover:bg-rose-50'
  }

  if (isSelected) {
    if (c.connected) {
      const isCharging = c.connectors?.some(conn => conn.status === 'Charging')
      if (isCharging) return 'bg-indigo-50 border-indigo-400 ring-2 ring-indigo-200 shadow-2xs'
      return 'bg-emerald-50 border-emerald-400 ring-2 ring-emerald-200 shadow-2xs'
    }
    if (c.connecting) return 'bg-amber-50 border-amber-400 ring-2 ring-amber-200 shadow-2xs animate-pulse'
    return 'bg-sky-50 border-sky-400 ring-2 ring-sky-200 shadow-2xs'
  }

  if (c.connecting) {
    return 'bg-amber-50/60 border-amber-300 hover:bg-amber-50 animate-pulse'
  }

  if (c.connected) {
    const isCharging = c.connectors?.some(conn => conn.status === 'Charging')
    if (isCharging) return 'bg-indigo-50/50 border-indigo-200 hover:bg-indigo-50/80'
    return 'bg-emerald-50/40 border-emerald-200 hover:bg-emerald-50/70'
  }

  return 'bg-white border-slate-200 hover:border-slate-300 hover:bg-slate-50'
}
</script>

<template>
  <div class="h-full flex flex-col bg-white border-r border-slate-200 select-none overflow-hidden font-sans relative">
    <!-- 1. 패널 상단 헤더: 타이틀, MySQL 동기화, 뱃지, 유틸 버튼 -->
    <div class="p-3 bg-slate-50 border-b border-slate-200 flex flex-col gap-2 flex-shrink-0">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2 min-w-0">
          <svg class="w-4 h-4 text-slate-600 shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 21h16.5M4.5 3h15M5.25 3v18m13.5-18v18M9 6.75h1.5m-1.5 3h1.5m-1.5 3h1.5m3-6H15m-1.5 3H15m-1.5 3H15M9 21v-3.375c0-.621.504-1.125 1.125-1.125h3.75c.621 0 1.125.504 1.125 1.125V21" />
          </svg>
          <h2 class="text-xs font-bold text-slate-900 tracking-tight shrink-0">충전기 현황</h2>
          <!-- MySQL 실데이터 연동 상태 뱃지 -->
          <div
            class="flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-mono font-bold border transition-colors shrink-0"
            :class="store.isMysqlConnected.value ? 'bg-emerald-50 text-emerald-700 border-emerald-300' : 'bg-amber-50 text-amber-700 border-amber-300'"
            :title="store.isMysqlConnected.value ? 'MySQL DB 실데이터 연동 중 (Port 9082)' : (store.mysqlError.value || 'MySQL 연결 대기')"
          >
            <span
              class="w-1.5 h-1.5 rounded-full"
              :class="store.isMysqlConnected.value ? 'bg-emerald-500 animate-pulse' : 'bg-amber-500'"
            ></span>
            <span class="hidden sm:inline">{{ store.isMysqlConnected.value ? 'MySQL 실데이터' : 'DB 연결대기' }}</span>
          </div>

          <span class="text-[10px] font-mono font-bold px-1.5 py-0.2 rounded bg-slate-200 text-slate-700 shrink-0">
            {{ filteredChargers.length }}/{{ store.totalCount.value }}
          </span>
        </div>

        <div class="flex items-center gap-1.5 shrink-0">
          <!-- MySQL 실시간 동기화 버튼 -->
          <button
            @click="store.fetchChargersFromMysql()"
            :disabled="store.isMysqlSyncing.value"
            class="px-2 py-0.5 text-[11px] font-bold rounded border border-slate-200 bg-white hover:bg-slate-100 text-slate-700 shadow-2xs transition-all cursor-pointer flex items-center gap-1 disabled:opacity-50"
            title="MySQL 백엔드 API(9082)에서 충전기 마스터 목록 최신화"
          >
            <svg class="w-3 h-3" :class="store.isMysqlSyncing.value ? 'animate-spin' : ''" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            <span class="hidden md:inline">{{ store.isMysqlSyncing.value ? '동기화중' : 'DB 동기화' }}</span>
          </button>

          <button
            v-if="totalErrorCount > 0"
            @click="store.clearAllErrors"
            class="px-2 py-0.5 text-[11px] font-medium rounded border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 transition-colors cursor-pointer flex items-center gap-1"
            title="오류 상태 초기화"
          >
            <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
            <span>오류 ({{ totalErrorCount }})</span>
          </button>
          <button
            @click="emit('open-add-modal')"
            class="px-2 py-0.5 text-[11px] font-bold rounded bg-sky-600 text-white hover:bg-sky-700 shadow-2xs transition-colors cursor-pointer flex items-center gap-1"
            title="신규 충전기 등록"
          >
            <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
            </svg>
            <span>등록</span>
          </button>
        </div>
      </div>

      <!-- 뷰 모드 탭 (매트릭스 vs 리스트) -->
      <div class="flex flex-wrap items-center justify-between gap-1.5 pt-1">
        <div class="flex items-center bg-slate-200/70 p-0.5 rounded-md text-[11px] font-medium">
          <button
            @click="viewMode = 'matrix'"
            class="px-2.5 py-0.5 rounded transition-all cursor-pointer inline-flex items-center gap-1"
            :class="viewMode === 'matrix' ? 'bg-white text-slate-900 font-bold shadow-2xs' : 'text-slate-600 hover:text-slate-900'"
          >
            <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6A2.25 2.25 0 016 3.75h2.25A2.25 2.25 0 0110.5 6v2.25a2.25 2.25 0 01-2.25 2.25H6a2.25 2.25 0 01-2.25-2.25V6zM3.75 15.75A2.25 2.25 0 016 13.5h2.25a2.25 2.25 0 012.25 2.25V18a2.25 2.25 0 01-2.25 2.25H6A2.25 2.25 0 013.75 18v-2.25zM13.5 6a2.25 2.25 0 012.25-2.25H18A2.25 2.25 0 0120.25 6v2.25A2.25 2.25 0 0118 10.5h-2.25a2.25 2.25 0 01-2.25-2.25V6zM13.5 15.75a2.25 2.25 0 012.25-2.25H18a2.25 2.25 0 012.25 2.25V18A2.25 2.25 0 0118 20.25h-2.25A2.25 2.25 0 0113.5 18v-2.25z" />
            </svg>
            <span>매트릭스</span>
          </button>
          <button
            @click="viewMode = 'list'"
            class="px-2.5 py-0.5 rounded transition-all cursor-pointer inline-flex items-center gap-1"
            :class="viewMode === 'list' ? 'bg-white text-slate-900 font-bold shadow-2xs' : 'text-slate-600 hover:text-slate-900'"
          >
            <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 12h16.5m-16.5 3.75h16.5M3.75 19.5h16.5M5.625 4.5h12.75a1.875 1.875 0 010 3.75H5.625a1.875 1.875 0 010-3.75z" />
            </svg>
            <span>리스트</span>
          </button>
        </div>

        <!-- 상태 필터 탭 -->
        <div class="flex items-center gap-1 text-[10px]">
          <button
            v-for="st in (['ALL', 'CONNECTED', 'DISCONNECTED'] as const)"
            :key="st"
            @click="statusFilter = st"
            class="px-1.5 py-0.5 rounded border transition-colors cursor-pointer"
            :class="statusFilter === st ? 'bg-slate-800 text-white border-slate-800 font-bold' : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-100'"
          >
            {{ st === 'ALL' ? '전체' : st === 'CONNECTED' ? '연결' : '미연결' }}
          </button>
        </div>
      </div>

      <!-- 검색창 -->
      <div class="relative">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="식별자(00000001), 충전기명 검색..."
          class="w-full bg-white border border-slate-200 rounded-md px-2.5 py-1 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-hidden focus:ring-1 focus:ring-sky-500 font-mono"
        />
        <button
          v-if="searchQuery"
          @click="searchQuery = ''"
          class="absolute right-2 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 text-xs"
        >
          ✕
        </button>
      </div>
    </div>

    <!-- 2. 매트릭스 뷰 (바둑판 타일) -->
    <div v-if="viewMode === 'matrix'" class="flex-1 overflow-y-auto p-3 space-y-4">
      <div v-if="stationGroups.length === 0" class="text-center py-10 text-xs text-slate-400">
        검색 조건에 맞는 충전기가 없습니다.
      </div>

      <div
        v-for="grp in stationGroups"
        :key="grp.stationId"
        class="bg-slate-50/70 border border-slate-200 rounded-lg p-2.5 space-y-2"
      >
        <!-- 충전소 그룹 헤더 -->
        <div class="flex items-center justify-between text-xs text-slate-700 pb-1 border-b border-slate-200/80">
          <div class="flex items-center gap-1.5 font-bold">
            <span class="w-1.5 h-1.5 rounded-full bg-sky-600"></span>
            <span class="font-mono text-sky-800">[{{ grp.stationId }}]</span>
            <span class="truncate max-w-[200px]">{{ grp.stationName }}</span>
          </div>
          <span class="text-[10px] text-slate-500 font-mono">{{ grp.chargers.length }}기</span>
        </div>

        <!-- 바둑판 충전기 타일 그리드 (가로 폭에 따라 2~4열 세로 유연 배치) -->
        <div class="grid grid-cols-2 min-[340px]:grid-cols-3 min-[400px]:grid-cols-4 gap-1.5">
          <div
            v-for="c in grp.chargers"
            :key="c.id"
            @click="handleSelect(c.id)"
            @contextmenu.prevent="openContextMenu($event, c)"
            class="relative rounded-md border p-1.5 flex flex-col justify-between transition-all cursor-pointer group shadow-2xs hover:shadow-xs min-h-[52px]"
            :class="getChargerTileClass(c)"
            :title="`[${c.id}] ${c.name}\n네트워크: ${c.connected ? '연결됨 (ONLINE)' : '미연결 (OFFLINE)'}\n커넥터: ${getConnectors(c).map(k => `#${k.connectorId}(${k.status})`).join(', ')}\n(좌클릭: 선택 / 우클릭: 빠른 전문 발송)`"
          >
            <!-- 1. 상단: 연결 도트 + 충전기 #번호 + 용량 kW -->
            <div class="w-full flex items-center justify-between gap-1 leading-none mb-1">
              <div class="flex items-center gap-1 min-w-0">
                <span
                  class="w-1.5 h-1.5 rounded-full flex-shrink-0"
                  :class="c.connected ? 'bg-emerald-500 ring-1 ring-emerald-200 animate-pulse' : c.connecting ? 'bg-amber-400 animate-ping' : 'bg-slate-300'"
                ></span>
                <span
                  class="text-[11px] font-mono font-bold truncate"
                  :class="selectedId === c.id ? (c.connected ? 'text-emerald-950 font-black' : 'text-sky-800 font-black') : 'text-slate-800'"
                >
                  #{{ c.chargerId }}
                </span>
              </div>
              <span class="text-[9px] font-mono text-slate-400 group-hover:text-slate-600 flex-shrink-0">
                {{ c.powerKw }}k
              </span>
            </div>

            <!-- 2. 하단: 커넥터 슬롯 (기본 2구 듀얼 C1, C2) -->
            <div class="w-full flex items-center gap-1">
              <div
                v-for="conn in getConnectors(c)"
                :key="conn.connectorId"
                class="flex-1 flex items-center justify-center gap-0.5 py-0.5 px-0.5 rounded text-[8px] font-mono border transition-colors truncate min-w-0"
                :class="getConnectorBadgeClass(c, conn.status)"
                :title="`커넥터 #${conn.connectorId}: ${conn.status}`"
              >
                <span class="text-[7.5px] opacity-75 font-sans">C{{ conn.connectorId }}</span>
                <span class="font-bold text-[8px] truncate">
                  {{ getConnectorStatusText(c, conn.status) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. 상세 카드 리스트 뷰 -->
    <div v-else class="flex-1 overflow-y-auto p-2 space-y-1.5">
      <div v-if="filteredChargers.length === 0" class="text-center py-10 text-xs text-slate-400">
        검색 조건에 맞는 충전기가 없습니다.
      </div>

      <div
        v-for="c in filteredChargers"
        :key="c.id"
        @click="handleSelect(c.id)"
        @contextmenu.prevent="openContextMenu($event, c)"
        class="p-2.5 rounded-lg border transition-all cursor-pointer flex flex-col gap-1.5 shadow-2xs"
        :class="getChargerListCardClass(c)"
        :title="`[${c.id}] ${c.name}\n(좌클릭: 선택 / 우클릭: 빠른 전문 발송)`"
      >
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-1.5">
            <span
              class="w-2.5 h-2.5 rounded-full flex-shrink-0"
              :class="c.connected ? 'bg-emerald-500 animate-pulse' : c.connecting ? 'bg-amber-400 animate-ping' : 'bg-slate-300'"
            ></span>
            <span class="font-mono font-bold text-xs text-slate-900 tracking-tight">
              {{ c.id }}
            </span>
            <span class="text-[10px] px-1 py-0.2 rounded font-medium bg-slate-100 text-slate-600 border border-slate-200">
              {{ c.powerKw }}kW
            </span>
          </div>

          <div class="flex items-center gap-1">
            <button
              @click="handleToggleConnect($event, c)"
              class="px-2 py-0.5 text-[10px] font-bold rounded border transition-colors cursor-pointer"
              :class="c.connected ? 'bg-rose-50 border-rose-200 text-rose-700 hover:bg-rose-100' : 'bg-emerald-50 border-emerald-200 text-emerald-700 hover:bg-emerald-100'"
            >
              {{ c.connected ? '해제' : '연결' }}
            </button>
            <button
              @click="handleEdit($event, c)"
              class="p-1 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded text-xs"
              title="충전기 설정 수정"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L6.832 19.82a4.5 4.5 0 01-1.897 1.13l-2.685.8.8-2.685a4.5 4.5 0 011.13-1.897L16.863 4.487zm0 0L19.5 7.125" />
              </svg>
            </button>
            <button
              @click="handleDelete($event, c.id)"
              class="p-1 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded text-xs"
              title="충전기 삭제"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
              </svg>
            </button>
          </div>
        </div>

        <div class="text-[11px] text-slate-600 truncate">
          {{ c.name }}
        </div>

        <!-- 커넥터 간략 슬롯 (기본 2구 포트 1, 2) -->
        <div class="flex items-center gap-1.5 py-0.5">
          <div
            v-for="conn in getConnectors(c)"
            :key="conn.connectorId"
            class="flex-1 flex items-center justify-between px-2 py-0.5 rounded text-[9px] font-mono border transition-colors truncate"
            :class="getConnectorBadgeClass(c, conn.status)"
            :title="`커넥터 #${conn.connectorId}: ${conn.status}`"
          >
            <span class="font-sans font-bold opacity-80">포트 {{ conn.connectorId }}</span>
            <span class="font-bold truncate">
              {{ getConnectorStatusText(c, conn.status) }}
            </span>
          </div>
        </div>

        <div class="flex items-center justify-between text-[10px] font-mono text-slate-500 pt-1 border-t border-slate-100">
          <div class="flex items-center gap-1">
            <span>{{ c.model }}</span>
            <span
              class="px-1 py-0.2 rounded text-[9px] font-semibold border"
              :class="c.autoHeartbeat ? 'bg-sky-50 text-sky-700 border-sky-200 font-bold' : 'bg-slate-100 text-slate-400 border-slate-200'"
              :title="`Heartbeat: ${c.autoHeartbeat ? '자동 ' + (c.heartbeatIntervalSec || 60) + '초' : '수동(OFF)'}`"
            >
              {{ c.autoHeartbeat ? `HB ${c.heartbeatIntervalSec || 60}s` : 'HB OFF' }}
            </span>
            <span
              v-if="!c.autoReply"
              class="px-1 py-0.2 rounded text-[9px] font-semibold bg-amber-50 text-amber-700 border border-amber-200"
              title="관제 CALL 자동응답 비활성화 상태"
            >
              NoReply
            </span>
          </div>
          <span :class="c.status === 'Charging' ? 'text-indigo-600 font-bold' : ''">{{ c.status }}</span>
        </div>
      </div>
    </div>

    <!-- 하단 상태 범례 (미니) -->
    <div class="px-3 py-1.5 bg-slate-50 border-t border-slate-200 flex items-center justify-between text-[10px] text-slate-500 font-mono flex-shrink-0">
      <div class="flex items-center gap-2">
        <span class="flex items-center gap-1">
          <span class="w-2 h-2 rounded-full bg-emerald-500"></span> 연결됨
        </span>
        <span class="flex items-center gap-1">
          <span class="w-2 h-2 rounded-full bg-slate-300"></span> 미연결
        </span>
        <span class="flex items-center gap-1">
          <span class="w-2 h-2 rounded-full bg-rose-500"></span> 오류
        </span>
      </div>
      <span class="text-slate-400">우클릭: 빠른 전문 발송</span>
    </div>

    <!-- 🖱️ 우클릭 플로팅 컨텍스트 메뉴 (Teleport로 최상위 body에 띄워 세로선/리사이저 위로 항상 노출) -->
    <Teleport to="body">
      <div
        v-if="isContextMenuOpen && contextCharger"
        class="fixed z-[9999] bg-white rounded-xl shadow-2xl border border-slate-200/90 w-[360px] max-h-[85vh] flex flex-col overflow-hidden select-none animate-in fade-in zoom-in-95 duration-100 text-xs font-sans ring-1 ring-black/10"
        :style="{ top: `${contextMenuPos.y}px`, left: `${contextMenuPos.x}px` }"
        @click.stop
        @contextmenu.prevent
      >
        <!-- 1. 컨텍스트 메뉴 고정 헤더: 충전기 ID 및 연결 토글 -->
        <div class="px-3.5 py-2.5 bg-slate-50 border-b border-slate-200 flex items-center justify-between flex-shrink-0">
          <div class="flex items-center gap-2 min-w-0">
            <div class="w-7 h-7 rounded-lg bg-sky-100 text-sky-700 flex items-center justify-center font-bold text-xs shrink-0 shadow-2xs">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 13.5l10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75z" />
              </svg>
            </div>
            <div class="flex flex-col min-w-0">
              <div class="flex items-center gap-1.5">
                <span class="font-mono font-black text-sm text-slate-900 tracking-tight">
                  {{ contextCharger.id }}
                </span>
                <span class="text-[9px] px-1.5 py-0.2 rounded-full font-bold bg-sky-100 text-sky-800 border border-sky-200">
                  {{ contextCharger.powerKw }}kW
                </span>
              </div>
              <span class="text-[10px] text-slate-500 truncate" :title="contextCharger.name">
                {{ contextCharger.name }}
              </span>
            </div>
          </div>

          <div class="flex items-center gap-1.5 shrink-0">
            <span
              class="text-[10px] font-mono px-2 py-0.5 rounded-full font-bold border"
              :class="contextCharger.connected ? 'bg-emerald-50 text-emerald-700 border-emerald-300' : 'bg-slate-100 text-slate-500 border-slate-200'"
            >
              {{ contextCharger.connected ? 'ONLINE' : 'OFFLINE' }}
            </span>
            <button
              @click="closeContextMenu"
              class="text-slate-400 hover:text-slate-600 p-1 rounded-md hover:bg-slate-200/60 transition-colors"
              title="닫기"
            >
              ✕
            </button>
          </div>
        </div>

        <!-- 2. 내부 스크롤 콘텐츠 영역 (상세 정보 + 커넥터 + 퀵 액션) -->
        <div class="flex-1 overflow-y-auto divide-y divide-slate-100 text-slate-700">
          <!-- 2-1. 실시간 메트릭 2x2 카드 -->
          <div class="p-3 bg-slate-50/50">
            <div class="text-[10px] font-bold text-slate-500 mb-1.5 flex items-center justify-between">
              <span class="inline-flex items-center gap-1">
                <svg class="w-3 h-3 text-slate-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z" />
                </svg>
                실시간 운전 메트릭
              </span>
              <span class="font-mono text-[9px] text-slate-400">포트 #{{ contextCharger.activeConnectorId || 1 }} 기준</span>
            </div>
            <div class="grid grid-cols-2 gap-1.5 text-[11px]">
              <div class="bg-white border border-slate-200 rounded-lg p-2 flex flex-col shadow-2xs">
                <span class="text-[9px] text-slate-400 uppercase font-mono">OCPP 상태</span>
                <span
                  class="font-black truncate mt-0.5"
                  :class="contextCharger.status === 'Charging' ? 'text-indigo-600' : contextCharger.status === 'Available' ? 'text-emerald-600' : 'text-slate-800'"
                >
                  {{ contextCharger.status }}
                </span>
              </div>
              <div class="bg-white border border-slate-200 rounded-lg p-2 flex flex-col shadow-2xs">
                <span class="text-[9px] text-slate-400 uppercase font-mono">배터리 SoC</span>
                <span class="font-mono font-bold text-sky-700 truncate mt-0.5">
                  {{ contextCharger.soc || 0 }} %
                </span>
              </div>
              <div class="bg-white border border-slate-200 rounded-lg p-2 flex flex-col shadow-2xs">
                <span class="text-[9px] text-slate-400 uppercase font-mono">트랜잭션 ID</span>
                <span class="font-mono font-bold text-slate-800 truncate mt-0.5">
                  {{ contextCharger.transactionId || '-' }}
                </span>
              </div>
              <div class="bg-white border border-slate-200 rounded-lg p-2 flex flex-col shadow-2xs">
                <span class="text-[9px] text-slate-400 uppercase font-mono">누적 계량 (Wh)</span>
                <span class="font-mono font-bold text-slate-800 truncate mt-0.5">
                  {{ (contextCharger.meterValueWh || 0).toLocaleString() }}
                </span>
              </div>
            </div>
          </div>

          <!-- 2-2. 듀얼 커넥터 (충전건) 현황 및 포트 전환 -->
          <div class="p-3">
            <div class="flex items-center justify-between mb-1.5">
              <span class="text-[10px] font-bold text-slate-500">커넥터 (충전건) 현황</span>
              <span class="text-[9px] text-sky-600 font-medium">클릭 시 활성 포트 전환</span>
            </div>
            <div class="space-y-1.5">
              <div
                v-for="conn in getConnectors(contextCharger)"
                :key="conn.connectorId"
                @click="handleContextSelectConnector(conn.connectorId)"
                class="p-2 rounded-lg border transition-all cursor-pointer flex items-center justify-between gap-2 shadow-2xs"
                :class="(contextCharger.activeConnectorId || 1) === conn.connectorId
                  ? 'bg-sky-50/80 border-sky-300 ring-1 ring-sky-400'
                  : 'bg-white border-slate-200 hover:bg-slate-50 hover:border-slate-300'"
                :title="`포트 #${conn.connectorId}를 제어 대상으로 선택`"
              >
                <div class="flex items-center gap-2 min-w-0">
                  <span
                    class="w-5 h-5 rounded flex items-center justify-center font-mono font-bold text-[10px] text-white shrink-0"
                    :class="conn.connectorId === 1 ? 'bg-sky-600' : 'bg-indigo-600'"
                  >
                    {{ conn.connectorId }}
                  </span>
                  <div class="flex flex-col min-w-0">
                    <div class="flex items-center gap-1.5">
                      <span class="font-bold text-[11px] text-slate-800">포트 #{{ conn.connectorId }}</span>
                      <span class="text-[9px] font-mono text-slate-500">{{ conn.type || 'DC콤보1' }}</span>
                    </div>
                    <span class="text-[9px] text-slate-400 truncate">
                      {{ conn.carModel || (conn.connectorId === 1 ? '아이오닉5' : 'EV6') }} · {{ conn.userTag || 'CARD-CORP-9901' }}
                    </span>
                  </div>
                </div>
                <div class="flex items-center gap-1 shrink-0">
                  <span
                    class="text-[9px] font-mono px-1.5 py-0.2 rounded font-bold border"
                    :class="conn.status === 'Charging' ? 'bg-indigo-50 text-indigo-700 border-indigo-200' : conn.status === 'Available' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-100 text-slate-600 border-slate-200'"
                  >
                    {{ conn.status || '대기' }}
                  </span>
                  <span v-if="(contextCharger.activeConnectorId || 1) === conn.connectorId" class="text-[9px] text-sky-700 font-black">
                    ●
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 2-3. 하드웨어 스펙 요약 정보 -->
          <div class="p-3 bg-slate-50/40 text-[10px]">
            <span class="font-bold text-slate-500 block mb-1.5">기기 스펙 & 네트워크</span>
            <div class="grid grid-cols-2 gap-x-2 gap-y-1 font-mono text-slate-600 bg-white p-2 rounded-lg border border-slate-200">
              <div><span class="text-slate-400">제조사:</span> {{ contextCharger.vendor }}</div>
              <div><span class="text-slate-400">모델명:</span> {{ contextCharger.model }}</div>
              <div><span class="text-slate-400">충전소:</span> {{ contextCharger.stationId }}</div>
              <div><span class="text-slate-400">버전:</span> {{ contextCharger.firmwareVer || '1.0.0' }}</div>
              <div class="col-span-2 truncate" :title="contextCharger.wsUrl">
                <span class="text-slate-400">URL:</span> {{ contextCharger.wsUrl }}
              </div>
            </div>
          </div>

          <!-- 2-4. 빠른 제어 & 전문 발송 액션들 -->
          <div class="p-2 space-y-1">
            <span class="px-1 text-[10px] font-bold text-slate-400 block uppercase tracking-wider">빠른 전문 제어</span>

            <!-- 1) WebSocket 연결 / 해제 -->
            <button
              @click="handleContextToggleConnect"
              class="w-full px-2.5 py-1.5 rounded-lg text-left font-bold flex items-center justify-between transition-colors cursor-pointer border"
              :class="contextCharger.connected ? 'bg-rose-50 text-rose-700 border-rose-200 hover:bg-rose-100' : 'bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100'"
            >
              <div class="flex items-center gap-2">
                <svg class="w-3.5 h-3.5 shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path v-if="contextCharger.connected" stroke-linecap="round" stroke-linejoin="round" d="M18.364 5.636a9 9 0 11-12.728 0M12 3v9" />
                  <path v-else stroke-linecap="round" stroke-linejoin="round" d="M3.75 13.5l10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75z" />
                </svg>
                <span>{{ contextCharger.connected ? 'WebSocket 연결 해제' : 'WebSocket 즉시 연결' }}</span>
              </div>
              <span class="text-[9px] font-mono opacity-80">{{ contextCharger.connected ? 'DISCONNECT' : 'CONNECT' }}</span>
            </button>

            <!-- 2) 터미널 필터링 -->
            <button
              @click="handleContextFilterTerminal"
              class="w-full px-2.5 py-1.5 rounded-lg text-left text-slate-700 hover:bg-slate-100 flex items-center justify-between transition-colors cursor-pointer border border-transparent hover:border-slate-200"
            >
              <div class="flex items-center gap-2">
                <svg class="w-3.5 h-3.5 text-slate-500 shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M8.288 15.038a5.25 5.25 0 017.424 0M5.106 11.856c3.807-3.808 9.98-3.808 13.788 0M1.924 8.674c5.565-5.565 14.587-5.565 20.152 0M12.53 18.22l-.53.53-.53-.53a.75.75 0 011.06 0z" />
                </svg>
                <span>실시간 패킷 터미널 필터링</span>
              </div>
              <span class="text-[9px] font-mono text-slate-400">[{{ contextCharger.id }}]</span>
            </button>

            <!-- 3) 기동 4단계 시퀀스 -->
            <button
              @click="handleContextBootSequence"
              class="w-full px-2.5 py-1.5 rounded-lg text-left font-bold text-sky-800 bg-sky-50/70 hover:bg-sky-100 border border-sky-200/80 flex items-center justify-between transition-colors cursor-pointer"
              title="부팅 기동 4대 전문(Boot, Preparing, Available, Heartbeat) 순차 자동 발송"
            >
              <div class="flex items-center gap-2">
                <svg class="w-3.5 h-3.5 text-sky-600 shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M5.25 5.653c0-.856.917-1.398 1.667-.986l11.54 6.348a1.125 1.125 0 010 1.971l-11.54 6.347a1.125 1.125 0 01-1.667-.985V5.653z" />
                </svg>
                <span>기동 4단계 연속 발송</span>
              </div>
              <span class="text-[9px] font-mono text-sky-600">Boot+상태+HB</span>
            </button>

            <!-- 4) 개별 전문 발송 버튼군 -->
            <div class="grid grid-cols-2 gap-1 pt-1">
              <button
                @click="handleContextSendBootNotification"
                class="px-2 py-1.5 rounded-md text-left text-slate-700 bg-slate-50 hover:bg-slate-100 border border-slate-200 flex items-center gap-1.5 transition-colors cursor-pointer"
              >
                <span class="w-1.5 h-1.5 rounded-full bg-sky-500 shrink-0"></span>
                <span class="truncate">Boot 발송</span>
              </button>
              <button
                @click="handleContextSendHeartbeat"
                class="px-2 py-1.5 rounded-md text-left text-slate-700 bg-slate-50 hover:bg-slate-100 border border-slate-200 flex items-center gap-1.5 transition-colors cursor-pointer"
              >
                <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 shrink-0"></span>
                <span class="truncate">Heartbeat 1회</span>
              </button>
              <button
                @click="handleContextSendMeterValues"
                class="px-2 py-1.5 rounded-md text-left text-slate-700 bg-slate-50 hover:bg-slate-100 border border-slate-200 flex items-center gap-1.5 transition-colors cursor-pointer"
              >
                <span class="w-1.5 h-1.5 rounded-full bg-indigo-500 shrink-0"></span>
                <span class="truncate">MeterValues 전송</span>
              </button>
              <button
                @click="handleContextToggleHeartbeat"
                class="px-2 py-1.5 rounded-md text-left text-slate-700 bg-slate-50 hover:bg-slate-100 border border-slate-200 flex items-center justify-between transition-colors cursor-pointer"
              >
                <div class="flex items-center gap-1.5 truncate">
                  <span class="w-1.5 h-1.5 rounded-full bg-amber-500 shrink-0"></span>
                  <span>HB 자동</span>
                </div>
                <span
                  class="text-[9px] font-mono px-1 py-0.2 rounded font-bold border"
                  :class="contextCharger?.autoHeartbeat ? 'bg-sky-50 text-sky-700 border-sky-300' : 'bg-slate-200 text-slate-500 border-slate-300'"
                >
                  {{ contextCharger?.autoHeartbeat ? 'ON' : 'OFF' }}
                </span>
              </button>
            </div>

            <!-- 5) 상태 통보 (StatusNotification) 변경 칩 -->
            <div class="pt-1.5">
              <span class="px-1 text-[9px] font-bold text-slate-400 uppercase">상태 통보 (StatusNotification)</span>
              <div class="grid grid-cols-4 gap-1 pt-1">
                <button
                  @click="handleContextChangeStatus('Available')"
                  class="px-1 py-1 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100 text-center cursor-pointer transition-colors"
                  title="정상 대기 (Available)"
                >
                  대기
                </button>
                <button
                  @click="handleContextChangeStatus('Preparing')"
                  class="px-1 py-1 rounded text-[10px] font-bold bg-amber-50 text-amber-700 border border-amber-200 hover:bg-amber-100 text-center cursor-pointer transition-colors"
                  title="준비/체결 (Preparing)"
                >
                  준비
                </button>
                <button
                  @click="handleContextChangeStatus('Charging')"
                  class="px-1 py-1 rounded text-[10px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200 hover:bg-indigo-100 text-center cursor-pointer transition-colors"
                  title="충전중 (Charging)"
                >
                  충전
                </button>
                <button
                  @click="handleContextChangeStatus('Faulted')"
                  class="px-1 py-1 rounded text-[10px] font-bold bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-100 text-center cursor-pointer transition-colors"
                  title="장애 (Faulted)"
                >
                  장애
                </button>
              </div>
            </div>
          </div>

          <!-- 2-5. 하단 유틸리티 (수정 및 삭제) -->
          <div class="p-2 bg-slate-50 flex items-center justify-between gap-1.5">
            <button
              @click="handleContextOpenEdit"
              class="flex-1 px-2.5 py-1.5 rounded-lg text-slate-700 bg-white hover:bg-slate-100 border border-slate-200 font-bold flex items-center justify-center gap-1.5 transition-colors cursor-pointer shadow-2xs"
            >
              <svg class="w-3.5 h-3.5 text-slate-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 011.37.49l1.296 2.247a1.125 1.125 0 01-.26 1.431l-1.003.827c-.293.24-.438.613-.431.992a6.759 6.759 0 010 .255c-.007.378.138.75.43.99l1.005.828c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 01-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 01-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 01-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 01-1.369-.49l-1.297-2.247a1.125 1.125 0 01.26-1.431l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 010-.255c.007-.378-.138-.75-.43-.99l-1.004-.828a1.125 1.125 0 01-.26-1.43l1.297-2.247a1.125 1.125 0 011.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.644-.869l.214-1.281z" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              <span>마스터 정보 관리</span>
            </button>
            <button
              @click="handleContextDelete"
              class="px-2.5 py-1.5 rounded-lg text-rose-600 bg-white hover:bg-rose-50 border border-rose-200 font-bold flex items-center gap-1 transition-colors cursor-pointer shadow-2xs"
              title="충전기 인스턴스 삭제"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
              </svg>
              <span>삭제</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
