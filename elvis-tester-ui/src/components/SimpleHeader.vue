<script setup lang="ts">
import { useVirtualCharger } from '../composables/useVirtualCharger'

defineProps<{
  isTerminalOpen: boolean
}>()

const emit = defineEmits<{
  (e: 'toggleTerminal'): void
}>()

const store = useVirtualCharger()

async function handleToggleMaximize() {
  try {
    // Tauri 환경일 때 윈도우 최대화/복원 시도
    if (typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window) {
      const { getCurrentWindow } = await import('@tauri-apps/api/window')
      const appWindow = getCurrentWindow()
      await appWindow.toggleMaximize()
    } else {
      // 일반 브라우저에서는 F11 전체화면 토글
      if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen().catch(() => {})
      } else {
        document.exitFullscreen().catch(() => {})
      }
    }
  } catch (e) {
    console.warn('Window maximize API not available:', e)
  }
}
</script>

<template>
  <header class="bg-white border-b border-slate-200 px-3 md:px-4 h-12 flex items-center justify-between shadow-2xs select-none flex-shrink-0 z-30 font-sans">
    <!-- 좌측 브랜드 및 요약 정보 -->
    <div class="flex items-center gap-4">
      <div class="flex items-center gap-2">
        <div class="w-7 h-7 rounded-lg bg-gradient-to-br from-indigo-500 to-sky-600 flex items-center justify-center text-white shadow-2xs">
          <!-- Lightning bolt SVG -->
          <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
        </div>
        <div class="flex items-center gap-1.5">
          <span class="text-sm font-black text-slate-900 tracking-tight whitespace-nowrap">
            <span>LSELink</span>
            <span class="text-sky-600 mx-0.5">•</span>
            <span class="text-slate-500 font-semibold">OCPP Tester</span>
          </span>
          <span class="text-[10px] px-1.5 py-0.2 rounded font-mono font-bold bg-sky-50 text-sky-700 border border-sky-200">
            OCPP 1.6-J
          </span>
        </div>
      </div>

      <!-- 요약 상태 뱃지군 -->
      <div class="hidden sm:flex items-center gap-1.5 font-mono text-[11px]">
        <span class="px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 border border-slate-200">
          충전기 <b class="text-slate-900">{{ store.totalCount.value }}</b>대
        </span>
        <span class="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
          연결됨 <b class="text-emerald-800">{{ store.connectedCount.value }}</b>대
        </span>
        <span class="px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 border border-slate-200">
          패킷 <b class="text-slate-900">{{ store.logs.value.length }}</b>건
        </span>
      </div>
    </div>

    <!-- 우측 일괄 제어 및 유틸리티 버튼군 -->
    <div class="flex items-center gap-2 font-mono text-xs">
      <!-- 빠른 일괄 제어 버튼 -->
      <div class="hidden md:flex items-center gap-1.5">
        <button
          @click="store.connectAll"
          class="px-2.5 py-1 rounded-md font-semibold bg-sky-50 text-sky-700 border border-sky-200 hover:bg-sky-600 hover:text-white transition-all cursor-pointer flex items-center gap-1 shadow-2xs"
          title="모든 가상 충전기 일괄 연결"
        >
          <span>⚡ 전체 연결</span>
        </button>
        <button
          @click="store.disconnectAll"
          class="px-2.5 py-1 rounded-md font-semibold bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-600 hover:text-white transition-all cursor-pointer flex items-center gap-1 shadow-2xs"
          title="모든 가상 충전기 일괄 연결 해제"
        >
          <span>🔌 전체 해제</span>
        </button>
        <button
          @click="store.clearLogs"
          class="px-2 py-1 rounded-md font-medium bg-slate-100 text-slate-600 border border-slate-200 hover:bg-slate-200 hover:text-slate-900 transition-all cursor-pointer flex items-center gap-1"
          title="패킷 로그 비우기"
        >
          <span>🧹 로그 비우기</span>
        </button>
      </div>

      <div class="hidden md:block w-px h-4 bg-slate-200 mx-1"></div>

      <!-- 우측 패킷 터미널 토글 -->
      <button
        @click="emit('toggleTerminal')"
        class="px-2.5 py-1 rounded-lg border font-semibold transition-all cursor-pointer flex items-center gap-1.5"
        :class="isTerminalOpen ? 'bg-sky-50 text-sky-700 border-sky-300 shadow-2xs font-bold' : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'"
        :title="isTerminalOpen ? '우측 터미널 패널 접기' : '우측 터미널 패널 펼치기'"
      >
        <span>📟</span>
        <span>패킷 터미널</span>
        <span
          class="text-[10px] px-1 rounded-sm font-mono"
          :class="isTerminalOpen ? 'bg-sky-200/80 text-sky-900' : 'bg-slate-200 text-slate-600'"
        >
          {{ isTerminalOpen ? 'ON' : 'OFF' }}
        </span>
      </button>

      <!-- 전체 창 최대화/복원 버튼 -->
      <button
        @click="handleToggleMaximize"
        class="px-2 py-1 rounded-md border border-slate-200 text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition-all cursor-pointer text-xs"
        title="전체 창 화면 가득 최대화 / 원래 크기 복원"
      >
        🗖 최대화
      </button>
    </div>
  </header>
</template>
