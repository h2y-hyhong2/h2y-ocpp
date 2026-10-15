<script setup lang="ts">
import type { VirtualChargerState } from '../types/ocpp'

defineProps<{
  state: VirtualChargerState
  currentTab: 'single' | 'fleet'
  isTerminalOpen: boolean
  packetCount: number
}>()

const emit = defineEmits<{
  (e: 'update:currentTab', tab: 'single' | 'fleet'): void
  (e: 'toggleTerminal'): void
}>()
</script>

<template>
  <header class="bg-white/95 backdrop-blur-md border-b border-slate-200 sticky top-0 z-40 px-3 md:px-4 h-12 flex items-center justify-between shadow-2xs select-none">
    <!-- 좌측 로고 및 시스템 타이틀 -->
    <div class="flex items-center gap-3">
      <div class="flex items-center gap-2">
        <div class="w-7 h-7 rounded-lg bg-gradient-to-br from-indigo-500 to-sky-600 flex items-center justify-center text-white shadow-2xs">
          <!-- Lightning bolt SVG -->
          <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
        </div>
        <div class="flex items-center gap-1.5">
          <span class="text-sm font-black text-slate-900 tracking-tight whitespace-nowrap">
            <span>ELVIS</span>
            <span class="text-sky-600 mx-0.5">•</span>
            <span class="text-slate-500 font-semibold">TESTER</span>
          </span>
          <span class="text-[10px] px-1.5 py-0.2 rounded font-mono font-bold bg-sky-50 text-sky-700 border border-sky-200">
            OCPP 1.6-J
          </span>
        </div>
      </div>

      <div class="hidden md:flex items-center pl-3 border-l border-slate-200 text-xs text-slate-500">
        가상 충전기(Virtual EVSE) & 실시간 WireTap 시뮬레이터
      </div>
    </div>

    <!-- 우측 상태 정보 및 제어 도구 -->
    <div class="flex items-center gap-2">
      <!-- 모드 탭 (단일 단말 모드 / 플릿 부하 모드) -->
      <div class="flex items-center bg-slate-100 p-0.5 rounded-lg text-xs border border-slate-200">
        <button
          @click="emit('update:currentTab', 'single')"
          class="px-2.5 py-1 rounded-md font-medium transition-all cursor-pointer flex items-center gap-1"
          :class="currentTab === 'single' ? 'bg-white text-slate-900 shadow-2xs font-bold' : 'text-slate-600 hover:text-slate-900'"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" />
          </svg>
          <span>단일 단말 모드</span>
        </button>
        <button
          @click="emit('update:currentTab', 'fleet')"
          class="px-2.5 py-1 rounded-md font-medium transition-all cursor-pointer flex items-center gap-1"
          :class="currentTab === 'fleet' ? 'bg-white text-slate-900 shadow-2xs font-bold' : 'text-slate-600 hover:text-slate-900'"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
          <span>플릿 부하 모드</span>
        </button>
      </div>

      <!-- 연결 상태 배지 -->
      <div class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg border text-xs font-mono bg-white border-slate-200">
        <span
          class="w-2 h-2 rounded-full"
          :class="state.connected ? 'bg-emerald-500 ring-2 ring-emerald-100 animate-pulse' : state.connecting ? 'bg-amber-400 animate-ping' : 'bg-slate-300'"
        ></span>
        <span class="font-bold tracking-tight text-[11px]" :class="state.connected ? 'text-emerald-700' : 'text-slate-500'">
          {{ state.connected ? 'ONLINE' : state.connecting ? 'CONNECTING...' : 'OFFLINE' }}
        </span>
      </div>

      <!-- 실시간 WireTap 패킷 터미널 토글 버튼 -->
      <button
        @click="emit('toggleTerminal')"
        class="px-2.5 py-1 text-xs rounded-lg border transition-all cursor-pointer flex items-center gap-1.5"
        :class="isTerminalOpen ? 'bg-sky-50 text-sky-700 border-sky-300 font-bold shadow-2xs' : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'"
        title="실시간 패킷 WireTap 터미널 토글"
      >
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
        <span>터미널</span>
        <span class="text-[10px] px-1 rounded-full bg-slate-200/80 text-slate-700 font-mono">
          {{ packetCount }}
        </span>
      </button>
    </div>
  </header>
</template>
