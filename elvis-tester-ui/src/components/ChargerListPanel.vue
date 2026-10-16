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

  const menuWidth = 260
  const menuHeight = 400
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
      <div class="flex items-center justify-between gap-1 pt-1">
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

        <!-- 바둑판 충전기 타일 그리드 -->
        <div class="grid grid-cols-4 sm:grid-cols-5 gap-1.5">
          <div
            v-for="c in grp.chargers"
            :key="c.id"
            @click="handleSelect(c.id)"
            @contextmenu.prevent="openContextMenu($event, c)"
            class="relative rounded-md border p-1.5 flex flex-col items-center justify-between transition-all cursor-pointer group"
            :class="[
              selectedId === c.id
                ? 'bg-sky-50 border-sky-400 ring-2 ring-sky-200 shadow-2xs font-bold'
                : 'bg-white border-slate-200 hover:border-slate-300 hover:bg-slate-50/80',
              c.lastError ? 'border-rose-300 bg-rose-50/40' : ''
            ]"
            :title="`[${c.id}] ${c.name}\n상태: ${c.status} (${c.connected ? '연결됨' : '미연결'})\n(좌클릭: 선택 / 우클릭: 빠른 전문 발송)`"
          >
            <!-- 연결 상태 도트 -->
            <div class="w-full flex items-center justify-between">
              <span
                class="w-2 h-2 rounded-full"
                :class="c.connected ? 'bg-emerald-500 animate-pulse ring-2 ring-emerald-100' : c.connecting ? 'bg-amber-400 animate-ping' : 'bg-slate-300'"
              ></span>
              <span class="text-[9px] font-mono text-slate-400 group-hover:text-slate-600">
                {{ c.powerKw }}k
              </span>
            </div>

            <!-- 충전기 번호 (2자리) -->
            <div class="my-0.5 text-center">
              <span class="text-xs font-mono font-black" :class="selectedId === c.id ? 'text-sky-700' : 'text-slate-800'">
                #{{ c.chargerId }}
              </span>
            </div>

            <!-- 상태 라벨 -->
            <span
              class="text-[9px] px-1 rounded-sm font-medium truncate max-w-full"
              :class="[
                c.status === 'Charging' ? 'bg-indigo-100 text-indigo-700 font-bold' :
                c.status === 'Preparing' ? 'bg-amber-100 text-amber-700' :
                c.connected ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'
              ]"
            >
              {{ c.status === 'Charging' ? '충전중' : c.status === 'Preparing' ? '준비중' : c.connected ? '대기' : '오프' }}
            </span>
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
        class="p-2.5 rounded-lg border transition-all cursor-pointer flex flex-col gap-1.5"
        :class="selectedId === c.id ? 'bg-sky-50 border-sky-300 ring-2 ring-sky-100 shadow-2xs' : 'bg-white border-slate-200 hover:border-slate-300 hover:bg-slate-50'"
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

        <div class="flex items-center justify-between text-[10px] font-mono text-slate-500 pt-1 border-t border-slate-100">
          <span>{{ c.model }}</span>
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

    <!-- 🖱️ 우클릭 플로팅 컨텍스트 메뉴 -->
    <div
      v-if="isContextMenuOpen && contextCharger"
      class="fixed z-50 bg-white rounded-xl shadow-xl border border-slate-200 w-64 overflow-hidden select-none animate-in fade-in zoom-in-95 duration-100 text-xs font-sans"
      :style="{ top: `${contextMenuPos.y}px`, left: `${contextMenuPos.x}px` }"
      @click.stop
      @contextmenu.prevent
    >
      <!-- 컨텍스트 메뉴 상단 헤더 -->
      <div class="px-3 py-2 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
        <div class="flex flex-col min-w-0">
          <span class="font-mono font-bold text-xs text-slate-900 truncate">
            🏢 {{ contextCharger.id }}
          </span>
          <span class="text-[10px] text-slate-500 truncate">
            {{ contextCharger.name }}
          </span>
        </div>
        <span
          class="text-[10px] font-mono px-1.5 py-0.2 rounded font-bold"
          :class="contextCharger.connected ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-slate-100 text-slate-500'"
        >
          {{ contextCharger.connected ? 'ONLINE' : 'OFFLINE' }}
        </span>
      </div>

      <!-- 컨텍스트 메뉴 액션 리스트 -->
      <div class="p-1 space-y-0.5">
        <!-- 1. 소켓 연결 / 해제 -->
        <button
          @click="handleContextToggleConnect"
          class="w-full px-2.5 py-1.5 rounded-md text-left font-semibold flex items-center gap-2 transition-colors cursor-pointer"
          :class="contextCharger.connected ? 'text-rose-700 hover:bg-rose-50' : 'text-emerald-700 hover:bg-emerald-50'"
        >
          <span>{{ contextCharger.connected ? '⏹' : '▶' }}</span>
          <span>{{ contextCharger.connected ? 'WebSocket 연결 해제' : 'WebSocket 연결하기' }}</span>
        </button>

        <!-- 2. 실시간 패킷 필터링 바로가기 -->
        <button
          @click="handleContextFilterTerminal"
          class="w-full px-2.5 py-1.5 rounded-md text-left text-slate-700 hover:bg-slate-100 flex items-center gap-2 transition-colors cursor-pointer"
        >
          <span>📡</span>
          <div class="flex flex-col">
            <span class="font-medium">실시간 패킷 필터링</span>
            <span class="text-[10px] text-slate-400">터미널에 [{{ contextCharger.id }}] 지정</span>
          </div>
        </button>

        <div class="my-1 border-t border-slate-100"></div>

        <!-- 3. 기동 4단계 연속 발송 -->
        <button
          @click="handleContextBootSequence"
          class="w-full px-2.5 py-1.5 rounded-md text-left font-bold text-sky-800 bg-sky-50/60 hover:bg-sky-100/80 flex items-center gap-2 transition-colors cursor-pointer"
          title="부팅 기동 4대 전문(Boot, Preparing, Available, Heartbeat) 순차 자동 발송"
        >
          <span>⚡</span>
          <div class="flex flex-col">
            <span>기동 4단계 연속 발송</span>
            <span class="text-[10px] text-sky-600 font-normal">Boot + 상태(2건) + HB</span>
          </div>
        </button>

        <div class="my-1 border-t border-slate-100"></div>

        <!-- 4. 개별 전문 빠른 발송 -->
        <button
          @click="handleContextSendBootNotification"
          class="w-full px-2.5 py-1.5 rounded-md text-left text-slate-700 hover:bg-slate-100 flex items-center gap-2 transition-colors cursor-pointer"
        >
          <span>🚀</span>
          <span>BootNotification 발송</span>
        </button>

        <button
          @click="handleContextSendHeartbeat"
          class="w-full px-2.5 py-1.5 rounded-md text-left text-slate-700 hover:bg-slate-100 flex items-center gap-2 transition-colors cursor-pointer"
        >
          <span>💓</span>
          <span>Heartbeat 즉시 전송</span>
        </button>

        <button
          @click="handleContextSendMeterValues"
          class="w-full px-2.5 py-1.5 rounded-md text-left text-slate-700 hover:bg-slate-100 flex items-center gap-2 transition-colors cursor-pointer"
        >
          <span>📊</span>
          <span>MeterValues(계량) 전송</span>
        </button>

        <div class="my-1 border-t border-slate-100"></div>

        <!-- 5. 상태(Status) 통보 칩 -->
        <div class="px-2 py-0.5 text-[10px] font-bold text-slate-400 uppercase">
          상태 통보 (StatusNotification)
        </div>
        <div class="grid grid-cols-4 gap-1 px-1 py-1">
          <button
            @click="handleContextChangeStatus('Available')"
            class="px-1 py-1 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100 text-center cursor-pointer"
            title="정상 대기 (Available)"
          >
            대기
          </button>
          <button
            @click="handleContextChangeStatus('Preparing')"
            class="px-1 py-1 rounded text-[10px] font-bold bg-amber-50 text-amber-700 border border-amber-200 hover:bg-amber-100 text-center cursor-pointer"
            title="준비/체결 (Preparing)"
          >
            준비
          </button>
          <button
            @click="handleContextChangeStatus('Charging')"
            class="px-1 py-1 rounded text-[10px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200 hover:bg-indigo-100 text-center cursor-pointer"
            title="충전중 (Charging)"
          >
            충전
          </button>
          <button
            @click="handleContextChangeStatus('Faulted')"
            class="px-1 py-1 rounded text-[10px] font-bold bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-100 text-center cursor-pointer"
            title="장애 (Faulted)"
          >
            장애
          </button>
        </div>

        <div class="my-1 border-t border-slate-100"></div>

        <!-- 6. 수정 & 삭제 -->
        <button
          @click="handleContextOpenEdit"
          class="w-full px-2.5 py-1.5 rounded-md text-left text-slate-700 hover:bg-slate-100 flex items-center gap-2 transition-colors cursor-pointer"
        >
          <span>✏️</span>
          <span>충전기 정보 수정</span>
        </button>
        <button
          @click="handleContextDelete"
          class="w-full px-2.5 py-1.5 rounded-md text-left text-rose-600 hover:bg-rose-50 flex items-center gap-2 transition-colors cursor-pointer"
        >
          <span>🗑️</span>
          <span>충전기 인스턴스 삭제</span>
        </button>
      </div>
    </div>
  </div>
</template>
