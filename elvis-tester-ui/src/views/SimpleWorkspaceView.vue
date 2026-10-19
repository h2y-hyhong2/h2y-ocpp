<script setup lang="ts">
import { ref } from 'vue'
import ChargerListPanel from '../components/ChargerListPanel.vue'
import ActionControlPanel from '../components/ActionControlPanel.vue'
import PacketTerminal from '../components/PacketTerminal.vue'
import AddChargerModal from '../components/AddChargerModal.vue'
import EditChargerModal from '../components/EditChargerModal.vue'
import { useVirtualCharger } from '../composables/useVirtualCharger'
import type { ChargerInstance } from '../types/ocpp'

const props = defineProps<{
  isTerminalOpen: boolean
}>()

const emit = defineEmits<{
  (e: 'update:isTerminalOpen', val: boolean): void
}>()

const store = useVirtualCharger()

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

// 중앙 전문 제어 및 시나리오 패널 너비 (기본 500px)
const centerPanelWidth = ref(500)
const isCenterResizing = ref(false)
let centerResizeStartX = 0
let centerResizeStartWidth = 500

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
</script>

<template>
  <div class="flex-1 flex overflow-hidden w-full h-full relative select-none bg-slate-100">
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
      title="드래그하여 중앙 패널 너비 조절 (기본 500px)"
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

    <!-- 신규 충전기 등록 모달 -->
    <AddChargerModal
      v-if="isAddModalOpen"
      @close="isAddModalOpen = false"
    />

    <!-- 충전기 정보 수정 모달 -->
    <EditChargerModal
      v-if="isEditModalOpen"
      :charger="editingCharger"
      @close="isEditModalOpen = false"
    />
  </div>
</template>
