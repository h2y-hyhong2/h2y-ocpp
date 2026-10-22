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
  store.sendBootNotification(c.vendor, c.model, '1.0.0', c.id)
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
      stationName: items[0]?.name.split('-')[0]?.trim() || `충전소 ${stId}`,
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
    <!-- 1. 패널 상단 헤더: 타이틀, 뱃지, 유틸 버튼 -->
    <div class="p-3 bg-slate-50 border-b border-slate-200 flex flex-col gap-2 flex-shrink-0">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <span class="text-base">🏢</span>
          <h2 class="text-xs font-bold text-slate-900 tracking-tight">충전기 현황</h2>
          <span class="text-[10px] font-mono font-bold px-1.5 py-0.2 rounded bg-slate-200 text-slate-700">
            {{ filteredChargers.length }}/{{ store.totalCount.value }}
          </span>
        </div>

        <div class="flex items-center gap-1.5">
          <button
            v-if="totalErrorCount > 0"
            @click="store.clearAllErrors"
            class="px-2 py-0.5 text-[11px] font-medium rounded border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 transition-colors cursor-pointer"
            title="오류 상태 초기화"
          >
            🧹 오류 초기화 ({{ totalErrorCount }})
          </button>
          <button
            @click="emit('open-add-modal')"
            class="px-2 py-0.5 text-[11px] font-bold rounded bg-sky-600 text-white hover:bg-sky-700 shadow-2xs transition-colors cursor-pointer flex items-center gap-1"
            title="신규 가상 충전기 추가"
          >
            <span>➕ 등록</span>
          </button>
        </div>
      </div>

      <!-- 뷰 모드 탭 (매트릭스 vs 리스트) -->
      <div class="flex flex-wrap items-center justify-between gap-1.5 pt-1">
        <div class="flex items-center bg-slate-200/70 p-0.5 rounded-md text-[11px] font-medium">
          <button
            @click="viewMode = 'matrix'"
            class="px-2.5 py-0.5 rounded transition-all cursor-pointer"
            :class="viewMode === 'matrix' ? 'bg-white text-slate-900 font-bold shadow-2xs' : 'text-slate-600 hover:text-slate-900'"
          >
            📊 매트릭스
          </button>
          <button
            @click="viewMode = 'list'"
            class="px-2.5 py-0.5 rounded transition-all cursor-pointer"
            :class="viewMode === 'list' ? 'bg-white text-slate-900 font-bold shadow-2xs' : 'text-slate-600 hover:text-slate-900'"
          >
            📋 리스트
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
              ✏️
            </button>
            <button
              @click="handleDelete($event, c.id)"
              class="p-1 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded text-xs"
              title="충전기 삭제"
            >
              🗑️
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

    <!-- 🖱️ 우클릭 플로팅 컨텍스트 메뉴 (충전기 종합 상세 정보 & 퀵 제어) -->
    <div
      v-if="isContextMenuOpen && contextCharger"
      class="fixed z-50 bg-white rounded-xl shadow-2xl border border-slate-200/90 w-[360px] max-h-[85vh] flex flex-col overflow-hidden select-none animate-in fade-in zoom-in-95 duration-100 text-xs font-sans ring-1 ring-black/5"
      :style="{ top: `${contextMenuPos.y}px`, left: `${contextMenuPos.x}px` }"
      @click.stop
      @contextmenu.prevent
    >
      <!-- 1. 컨텍스트 메뉴 고정 헤더: 충전기 ID 및 연결 토글 -->
      <div class="px-3.5 py-2.5 bg-slate-50 border-b border-slate-200 flex items-center justify-between flex-shrink-0">
        <div class="flex items-center gap-2 min-w-0">
          <div class="w-7 h-7 rounded-lg bg-sky-100 text-sky-700 flex items-center justify-center font-bold text-xs shrink-0 shadow-2xs">
            ⚡
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
            <span>📊 실시간 운전 메트릭</span>
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
            <span class="text-[10px] font-bold text-slate-500">🔌 커넥터 (충전건) 현황</span>
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
          <span class="font-bold text-slate-500 block mb-1.5">📋 기기 스펙 & 네트워크</span>
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
          <span class="px-1 text-[10px] font-bold text-slate-400 block uppercase tracking-wider">⚡ 빠른 전문 제어</span>

          <!-- 1) WebSocket 연결 / 해제 -->
          <button
            @click="handleContextToggleConnect"
            class="w-full px-2.5 py-1.5 rounded-lg text-left font-bold flex items-center justify-between transition-colors cursor-pointer border"
            :class="contextCharger.connected ? 'bg-rose-50 text-rose-700 border-rose-200 hover:bg-rose-100' : 'bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100'"
          >
            <div class="flex items-center gap-2">
              <span>{{ contextCharger.connected ? '🔌' : '⚡' }}</span>
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
              <span>📡</span>
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
              <span>🚀</span>
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
              <span>📨</span>
              <span class="truncate">Boot 발송</span>
            </button>
            <button
              @click="handleContextSendHeartbeat"
              class="px-2 py-1.5 rounded-md text-left text-slate-700 bg-slate-50 hover:bg-slate-100 border border-slate-200 flex items-center gap-1.5 transition-colors cursor-pointer"
            >
              <span>💓</span>
              <span class="truncate">Heartbeat 1회</span>
            </button>
            <button
              @click="handleContextSendMeterValues"
              class="px-2 py-1.5 rounded-md text-left text-slate-700 bg-slate-50 hover:bg-slate-100 border border-slate-200 flex items-center gap-1.5 transition-colors cursor-pointer"
            >
              <span>📊</span>
              <span class="truncate">MeterValues 전송</span>
            </button>
            <button
              @click="handleContextToggleHeartbeat"
              class="px-2 py-1.5 rounded-md text-left text-slate-700 bg-slate-50 hover:bg-slate-100 border border-slate-200 flex items-center justify-between transition-colors cursor-pointer"
            >
              <div class="flex items-center gap-1.5 truncate">
                <span>🔄</span>
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
            <span>⚙️</span>
            <span>마스터 정보 관리</span>
          </button>
          <button
            @click="handleContextDelete"
            class="px-2.5 py-1.5 rounded-lg text-rose-600 bg-white hover:bg-rose-50 border border-rose-200 font-bold flex items-center gap-1 transition-colors cursor-pointer shadow-2xs"
            title="충전기 인스턴스 삭제"
          >
            <span>🗑️</span>
            <span>삭제</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
