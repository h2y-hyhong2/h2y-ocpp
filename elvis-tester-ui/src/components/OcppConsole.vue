<script setup lang="ts">
import { ref, computed } from 'vue'
import type { OcppLogEntry } from '../types/ocpp'

const props = defineProps<{
  logs: OcppLogEntry[]
}>()

const emit = defineEmits<{
  (e: 'clear'): void
}>()

const filterDirection = ref<'ALL' | 'OUT' | 'IN' | 'ERR'>('ALL')
const autoScroll = ref(true)
const selectedLog = ref<OcppLogEntry | null>(null)
const copied = ref(false)

const filteredLogs = computed(() => {
  if (filterDirection.value === 'ALL') return props.logs
  return props.logs.filter(l => l.direction === filterDirection.value)
})

function copyJson(payload: any) {
  navigator.clipboard.writeText(JSON.stringify(payload, null, 2))
  copied.value = true
  setTimeout(() => (copied.value = false), 1500)
}
</script>

<template>
  <div class="bg-white rounded-xl border border-slate-200 shadow-2xs flex flex-col h-[650px] overflow-hidden">
    <!-- 콘솔 상단 툴바 -->
    <div class="px-4 py-2.5 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
      <div class="flex items-center gap-2">
        <span class="w-2 h-2 rounded-full bg-indigo-500 animate-pulse"></span>
        <h3 class="text-xs font-bold text-slate-900">실시간 OCPP 통신 WireTap 콘솔</h3>
        <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-200/70 text-slate-600">
          {{ filteredLogs.length }}건
        </span>
      </div>

      <div class="flex items-center gap-2 text-xs">
        <!-- 방향 필터 탭 -->
        <div class="flex items-center bg-white border border-slate-200 rounded-md p-0.5 text-[11px]">
          <button
            v-for="f in (['ALL', 'OUT', 'IN', 'ERR'] as const)"
            :key="f"
            @click="filterDirection = f"
            class="px-2 py-0.5 rounded transition-colors cursor-pointer"
            :class="filterDirection === f ? 'bg-slate-900 text-white font-bold' : 'text-slate-600 hover:text-slate-900'"
          >
            {{ f }}
          </button>
        </div>

        <button
          @click="emit('clear')"
          class="px-2 py-1 text-[11px] text-slate-500 hover:text-rose-600 border border-slate-200 hover:border-rose-200 rounded-md bg-white transition-colors cursor-pointer"
        >
          지우기
        </button>
      </div>
    </div>

    <!-- 콘솔 로그 리스트 & 상세 뷰 분할 영역 -->
    <div class="flex-1 flex flex-col sm:flex-row overflow-hidden divide-y sm:divide-y-0 sm:divide-x divide-slate-200">
      <!-- 1. 패킷 목록 스트림 -->
      <div class="flex-1 overflow-y-auto divide-y divide-slate-100 font-mono text-[11px]">
        <div
          v-if="filteredLogs.length === 0"
          class="h-full flex items-center justify-center text-slate-400 text-xs py-12"
        >
          수신/발송된 OCPP 패킷이 없습니다.
        </div>

        <div
          v-for="log in filteredLogs"
          :key="log.id"
          @click="selectedLog = log"
          class="px-3.5 py-2 hover:bg-slate-50/80 cursor-pointer transition-colors flex items-center justify-between gap-2"
          :class="{ 'bg-sky-50/50': selectedLog?.id === log.id }"
        >
          <div class="flex items-center gap-2 overflow-hidden">
            <!-- 방향 배지 -->
            <span
              class="px-1.5 py-0.2 rounded text-[10px] font-bold tracking-tight uppercase flex-shrink-0"
              :class="{
                'bg-sky-100 text-sky-800 border border-sky-200': log.direction === 'OUT',
                'bg-emerald-100 text-emerald-800 border border-emerald-200': log.direction === 'IN',
                'bg-rose-100 text-rose-800 border border-rose-200': log.direction === 'ERR'
              }"
            >
              {{ log.direction === 'OUT' ? '▲ OUT' : log.direction === 'IN' ? '▼ IN' : '✕ ERR' }}
            </span>

            <span class="text-slate-400 text-[10px] flex-shrink-0">{{ log.timestamp }}</span>
            <strong class="text-slate-800 truncate">{{ log.action || 'CALL' }}</strong>
            <span class="text-slate-400 text-[10px] truncate max-w-[160px]">{{ JSON.stringify(log.payload) }}</span>
          </div>

          <!-- RTT 응답속도 -->
          <div class="flex items-center gap-1.5 flex-shrink-0">
            <span
              v-if="log.latencyMs !== undefined"
              class="px-1.5 py-0.2 rounded text-[10px] font-mono"
              :class="log.latencyMs < 50 ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'"
            >
              {{ log.latencyMs }}ms
            </span>
          </div>
        </div>
      </div>

      <!-- 2. 선택된 패킷 상세 JSON 뷰어 (우측 고정) -->
      <div class="w-full sm:w-80 bg-slate-50/60 p-3 flex flex-col justify-between overflow-hidden">
        <div class="flex items-center justify-between pb-2 border-b border-slate-200 mb-2">
          <span class="text-[11px] font-bold text-slate-700">패킷 세부 명세</span>
          <button
            v-if="selectedLog"
            @click="copyJson(selectedLog.payload)"
            class="text-[10px] text-sky-600 hover:text-sky-800 font-medium cursor-pointer"
          >
            {{ copied ? '✓ 복사됨' : 'JSON 복사' }}
          </button>
        </div>

        <div v-if="selectedLog" class="flex-1 overflow-y-auto space-y-2 font-mono text-[11px]">
          <div>
            <span class="text-slate-400 text-[10px]">Action:</span>
            <div class="font-bold text-slate-800">{{ selectedLog.action }}</div>
          </div>
          <div>
            <span class="text-slate-400 text-[10px]">MessageId:</span>
            <div class="text-slate-600 text-[10px]">{{ selectedLog.messageId }}</div>
          </div>
          <div class="flex-1">
            <span class="text-slate-400 text-[10px]">Payload:</span>
            <pre class="bg-white p-2 rounded border border-slate-200 text-slate-800 text-[10px] overflow-x-auto max-h-72">{{ JSON.stringify(selectedLog.payload, null, 2) }}</pre>
          </div>
        </div>

        <div v-else class="flex-1 flex items-center justify-center text-slate-400 text-xs text-center py-8">
          왼쪽 목록에서 패킷을 클릭하면<br />상세 JSON이 표시됩니다.
        </div>
      </div>
    </div>
  </div>
</template>
