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

// 좌측 충전기 목록 패널 너비 (기본 360px)
const leftPanelWidth = ref(360)
const isLeftResizing = ref(false)

// 우측 실시간 패킷 터미널 너비 (기본 520px)
const terminalWidth = ref(
  typeof window !== 'undefined'
    ? Math.max(480, Math.floor(window.innerWidth * 0.38))
    : 520
)
const isRightResizing = ref(false)

function handleSelectCharger(id: string) {
  store.selectCharger(id)
}

// 1. 좌측 리사이저
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
  if (newWidth >= 260 && newWidth <= Math.floor(window.innerWidth * 0.45)) {
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

// 2. 우측 리사이저
function startRightResize(e: MouseEvent) {
  isRightResizing.value = true
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', handleRightMouseMove)
  window.addEventListener('mouseup', stopRightResize)
  e.preventDefault()
}

function handleRightMouseMove(e: MouseEvent) {
  if (!isRightResizing.value) return
  const newWidth = window.innerWidth - e.clientX
  if (newWidth >= 320 && newWidth <= Math.floor(window.innerWidth * 0.65)) {
    terminalWidth.value = newWidth
  }
}

function stopRightResize() {
  if (isRightResizing.value) {
    isRightResizing.value = false
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
    window.removeEventListener('mousemove', handleRightMouseMove)
    window.removeEventListener('mouseup', stopRightResize)
  }
}
</script>

<template>
  <div class="flex-1 flex overflow-hidden w-full h-full relative select-none bg-slate-100">
    <!-- 1. 좌측: 충전기 목록/매트릭스 패널 -->
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

    <!-- 좌측 리사이저 바 -->
    <div
      class="w-1.5 hover:w-2 bg-slate-200 hover:bg-sky-400 active:bg-sky-600 transition-colors cursor-col-resize flex items-center justify-center flex-shrink-0 z-20 group"
      @mousedown="startLeftResize"
      title="드래그하여 충전기 목록 너비 조절"
    >
      <div class="w-0.5 h-6 bg-slate-400 group-hover:bg-white rounded-full"></div>
    </div>

    <!-- 2. 중앙: 전문 제어 및 시나리오 패널 (flex-1) -->
    <main class="flex-1 h-full min-w-0 bg-slate-50 overflow-y-auto p-3">
      <ActionControlPanel
        :charger-id="store.selectedChargerId.value"
        @open-edit-modal="handleOpenEditModal"
      />
    </main>

    <!-- 우측 리사이저 바 (터미널 열려 있을 때) -->
    <div
      v-if="isTerminalOpen"
      class="w-1.5 hover:w-2 bg-slate-200 hover:bg-sky-400 active:bg-sky-600 transition-colors cursor-col-resize flex items-center justify-center flex-shrink-0 z-20 group"
      @mousedown="startRightResize"
      title="드래그하여 실시간 패킷 터미널 너비 조절"
    >
      <div class="w-0.5 h-6 bg-slate-400 group-hover:bg-white rounded-full"></div>
    </div>

    <!-- 3. 우측: 실시간 패킷 터미널 패널 -->
    <aside
      v-if="isTerminalOpen"
      class="h-full flex-shrink-0 flex flex-col bg-white overflow-hidden shadow-xs z-10"
      :style="{ width: `${terminalWidth}px` }"
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
