<script setup lang="ts">
import { ref } from 'vue'
import ChargerListPanel from '../components/ChargerListPanel.vue'
import ActionControlPanel from '../components/ActionControlPanel.vue'
import PacketTerminal from '../components/PacketTerminal.vue'
import AddChargerModal from '../components/AddChargerModal.vue'
import ChargerMasterModal from '../components/ChargerMasterModal.vue'
import MasterDataManager from '../components/MasterDataManager.vue'
import { useVirtualCharger } from '../composables/useVirtualCharger'
import { useMasterData } from '../composables/useMasterData'
import type { ChargerInstance } from '../types/ocpp'

const props = defineProps<{
  isTerminalOpen: boolean
}>()

const emit = defineEmits<{
  (e: 'update:isTerminalOpen', val: boolean): void
}>()

const store = useVirtualCharger()
const master = useMasterData()

const isAddModalOpen = ref(false)
const isEditModalOpen = ref(false)
const editingCharger = ref<ChargerInstance | null>(null)

function handleOpenEditModal(charger: ChargerInstance) {
  editingCharger.value = charger
  isEditModalOpen.value = true
}

// 좌측 충전기 목록 패널 너비 (기본 400px)
const leftPanelWidth = ref(400)
const isLeftResizing = ref(false)

// 중앙 전문 제어 및 시나리오 패널 너비 (기본 650px)
const centerPanelWidth = ref(650)
const isCenterResizing = ref(false)
let centerResizeStartX = 0
let centerResizeStartWidth = 650

// 하단 마스터 데이터 도크 패널 (기본 접힘, 펼쳤을 때 높이 280px)
const isMasterDataDockOpen = ref(false)
const dockHeight = ref(280)
const isDockResizing = ref(false)
let dockResizeStartY = 0
let dockResizeStartHeight = 280

function handleSelectCharger(id: string) {
  store.selectCharger(id)
}

// 1. 좌측 리사이저 (목록 너비 조절: 400px 기준)
function startLeftResize(e: MouseEvent) {
  isLeftResizing.value = true
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', handleLeftMouseMove)
  window.addEventListener('mouseup', stopLeftResize)
  e.preventDefault()
}

function handleLeftMouseMove(e: MouseEvent) {
  if (!isLeftResizing.value) return
  const newWidth = e.clientX
  if (newWidth >= 280 && newWidth <= Math.floor(window.innerWidth * 0.45)) {
    leftPanelWidth.value = newWidth
  }
}

function stopLeftResize() {
  if (isLeftResizing.value) {
    isLeftResizing.value = false
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
    window.removeEventListener('mousemove', handleLeftMouseMove)
    window.removeEventListener('mouseup', stopLeftResize)
  }
}

// 2. 중앙-우측 리사이저 (중앙 패널 너비 조절: 500px 기준, 우측은 auto)
function startCenterResize(e: MouseEvent) {
  isCenterResizing.value = true
  centerResizeStartX = e.clientX
  centerResizeStartWidth = centerPanelWidth.value
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', handleCenterMouseMove)
  window.addEventListener('mouseup', stopCenterResize)
  e.preventDefault()
}

function handleCenterMouseMove(e: MouseEvent) {
  if (!isCenterResizing.value) return
  const delta = e.clientX - centerResizeStartX
  const newWidth = centerResizeStartWidth + delta
  if (newWidth >= 380 && newWidth <= Math.floor(window.innerWidth * 0.6)) {
    centerPanelWidth.value = newWidth
  }
}

function stopCenterResize() {
  if (isCenterResizing.value) {
    isCenterResizing.value = false
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
    window.removeEventListener('mousemove', handleCenterMouseMove)
    window.removeEventListener('mouseup', stopCenterResize)
  }
}

// 3. 하단 도크 높이 리사이저 (위로 드래그 시 높이 확장)
function startDockResize(e: MouseEvent) {
  isDockResizing.value = true
  dockResizeStartY = e.clientY
  dockResizeStartHeight = dockHeight.value
  document.body.style.cursor = 'row-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', handleDockMouseMove)
  window.addEventListener('mouseup', stopDockResize)
  e.preventDefault()
}

function handleDockMouseMove(e: MouseEvent) {
  if (!isDockResizing.value) return
  const delta = dockResizeStartY - e.clientY
  const newHeight = dockResizeStartHeight + delta
  if (newHeight >= 160 && newHeight <= Math.floor(window.innerHeight * 0.65)) {
    dockHeight.value = newHeight
  }
}

function stopDockResize() {
  if (isDockResizing.value) {
    isDockResizing.value = false
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
    window.removeEventListener('mousemove', handleDockMouseMove)
    window.removeEventListener('mouseup', stopDockResize)
  }
}
</script>

<template>
  <div class="flex-1 flex flex-col overflow-hidden w-full h-full relative select-none bg-slate-100">
    <!-- 상단 3단 메인 워크스페이스 영역 (목록 + 제어 + 터미널) -->
    <div class="flex-1 min-h-0 flex flex-row overflow-hidden w-full relative">
      <!-- 1. 좌측: 충전기 목록/매트릭스 패널 (너비 400px) -->
      <aside
        class="h-full flex-shrink-0 flex flex-col bg-white overflow-hidden shadow-xs z-10"
        :style="{ width: `${leftPanelWidth}px` }"
      >
        <ChargerListPanel
          :selected-id="store.selectedChargerId.value"
          @select="handleSelectCharger"
          @open-add-modal="isAddModalOpen = true"
          @open-edit-modal="handleOpenEditModal"
        />
      </aside>

      <!-- 좌측-중앙 리사이저 바 -->
      <div
        class="w-1.5 hover:w-2 bg-slate-200 hover:bg-sky-400 active:bg-sky-600 transition-colors cursor-col-resize flex items-center justify-center flex-shrink-0 z-20 group"
        @mousedown="startLeftResize"
        title="드래그하여 충전기 목록 너비 조절 (기본 400px)"
      >
        <div class="w-0.5 h-6 bg-slate-400 group-hover:bg-white rounded-full"></div>
      </div>

      <!-- 2. 중앙: 전문 제어 및 시나리오 패널 (너비 500px, 터미널 닫힘 시 flex-1 확장) -->
      <main
        class="h-full bg-slate-50 overflow-y-auto p-3"
        :class="isTerminalOpen ? 'flex-shrink-0' : 'flex-1 min-w-0'"
        :style="isTerminalOpen ? { width: `${centerPanelWidth}px` } : {}"
      >
        <ActionControlPanel
          :charger-id="store.selectedChargerId.value"
          @open-edit-modal="handleOpenEditModal"
        />
      </main>

      <!-- 중앙-우측 리사이저 바 (터미널 열려 있을 때) -->
      <div
        v-if="isTerminalOpen"
        class="w-1.5 hover:w-2 bg-slate-200 hover:bg-sky-400 active:bg-sky-600 transition-colors cursor-col-resize flex items-center justify-center flex-shrink-0 z-20 group"
        @mousedown="startCenterResize"
        title="드래그하여 중앙 패널 너비 조절 (기본 650px)"
      >
        <div class="w-0.5 h-6 bg-slate-400 group-hover:bg-white rounded-full"></div>
      </div>

      <!-- 3. 우측: 실시간 패킷 터미널 패널 (auto: 남은 공간 전체 flex-1 차지) -->
      <aside
        v-if="isTerminalOpen"
        class="flex-1 min-w-0 h-full flex flex-col bg-white overflow-hidden shadow-xs z-10"
      >
        <PacketTerminal
          :logs="store.logs.value"
          :is-side-panel="true"
          @clear="store.clearLogs"
          @close="emit('update:isTerminalOpen', false)"
        />
      </aside>
    </div>

    <!-- ───────────────────────────────────────────────────────────── -->
    <!-- 4. 하단: 마스터 데이터 관리 바텀 도크 (사용자 · 차량 · 인증정보) -->
    <!-- ───────────────────────────────────────────────────────────── -->
    <div class="flex-shrink-0 flex flex-col bg-white border-t border-slate-200 shadow-md z-30">
      <!-- 높이 조절 수평 리사이저 바 (도크가 열려 있을 때만 활성화) -->
      <div
        v-if="isMasterDataDockOpen"
        class="h-1.5 hover:h-2 bg-slate-200 hover:bg-sky-400 active:bg-sky-600 transition-colors cursor-row-resize flex items-center justify-center flex-shrink-0 group"
        @mousedown="startDockResize"
        title="드래그하여 마스터 데이터 패널 높이 조절"
      >
        <div class="h-0.5 w-10 bg-slate-400 group-hover:bg-white rounded-full"></div>
      </div>

      <!-- 도크 헤더 상태바 (항상 노출되는 1줄 컴팩트 바) -->
      <div
        class="h-9 px-3 bg-slate-50 hover:bg-slate-100/80 transition-colors border-b border-slate-200/80 flex items-center justify-between cursor-pointer select-none"
        @click="isMasterDataDockOpen = !isMasterDataDockOpen"
      >
        <div class="flex items-center gap-2.5 overflow-hidden min-w-0">
          <span class="text-sm shrink-0">🚗</span>
          <span class="text-xs font-bold text-slate-900 shrink-0">마스터 데이터 관리</span>
          <span class="text-[10px] text-slate-500 font-medium hidden sm:inline shrink-0">(사용자 · 차량 · 인증정보)</span>

          <!-- 실시간 집계 칩 배지군 (가로 부족 시 스크롤 허용) -->
          <div class="flex items-center gap-1 text-[10px] font-mono overflow-x-auto scrollbar-none">
            <span class="px-2 py-0.5 rounded-full font-bold bg-white text-slate-800 border border-slate-200 shadow-2xs">
              👤 사용자 <b>{{ master.users.value.length }}</b>
            </span>
            <span class="px-2 py-0.5 rounded-full font-bold bg-indigo-50 text-indigo-700 border border-indigo-200 shadow-2xs">
              🚙 차량 <b>{{ master.vehicles.value.length }}</b>
            </span>
            <span class="px-2 py-0.5 rounded-full font-bold bg-sky-50 text-sky-700 border border-sky-200 shadow-2xs">
              💳 RFID <b>{{ master.rfidCards.value.length }}</b>
            </span>
            <span class="px-2 py-0.5 rounded-full font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 shadow-2xs hidden md:inline">
              🚗 VHRNO <b>{{ master.vhrnoCerts.value.length }}</b>
            </span>
            <span class="px-2 py-0.5 rounded-full font-bold bg-amber-50 text-amber-800 border border-amber-200 shadow-2xs hidden md:inline">
              ⚡ PCID <b>{{ master.pcidCerts.value.length }}</b>
            </span>
          </div>
        </div>

        <div class="flex items-center gap-2" @click.stop>
          <button
            @click="isMasterDataDockOpen = !isMasterDataDockOpen"
            class="px-2.5 py-1 text-[11px] font-bold rounded-md bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 cursor-pointer shadow-2xs transition-colors flex items-center gap-1"
          >
            <span>{{ isMasterDataDockOpen ? '▼ 패널 접기' : '▲ 패널 펼치기' }}</span>
          </button>
        </div>
      </div>

      <!-- 도크 본문 영역 (펼쳐졌을 때 가변 높이로 렌더링) -->
      <div
        v-show="isMasterDataDockOpen"
        class="w-full overflow-hidden"
        :style="{ height: `${dockHeight}px` }"
      >
        <MasterDataManager
          :charger-id="store.selectedChargerId.value"
          @apply-idtag="(tag: string) => store.setSelectedIdTag(tag)"
        />
      </div>
    </div>

    <!-- 신규 충전기 등록 모달 -->
    <AddChargerModal
      v-if="isAddModalOpen"
      @close="isAddModalOpen = false"
    />

    <!-- 충전기 자산 마스터 관리 모달 -->
    <ChargerMasterModal
      v-if="isEditModalOpen"
      :charger="editingCharger"
      @close="isEditModalOpen = false"
    />
  </div>
</template>
