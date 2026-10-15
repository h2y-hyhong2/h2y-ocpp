<script setup lang="ts">
import { ref, computed } from 'vue'
import type { OcppLogEntry } from '../types/ocpp'

const props = defineProps<{
  logs: OcppLogEntry[]
  isSidePanel?: boolean
}>()

const emit = defineEmits<{
  (e: 'clear'): void
  (e: 'close'): void
}>()

const filterDirection = ref<'ALL' | 'OUT' | 'IN' | 'ERR'>('ALL')
const searchQuery = ref('')
const selectedLog = ref<OcppLogEntry | null>(null)
const copied = ref(false)

const filteredLogs = computed(() => {
  return props.logs.filter(log => {
    if (filterDirection.value !== 'ALL' && log.direction !== filterDirection.value) {
      return false
    }
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase()
      const actionMatch = log.action?.toLowerCase().includes(q)
      const rawMatch = log.raw?.toLowerCase().includes(q)
      const idMatch = log.messageId?.toLowerCase().includes(q)
      if (!actionMatch && !rawMatch && !idMatch) return false
    }
    return true
  })
})

function selectLog(log: OcppLogEntry) {
  selectedLog.value = log
}

function copyJson(payload: any) {
  navigator.clipboard.writeText(JSON.stringify(payload, null, 2))
  copied.value = true
  setTimeout(() => (copied.value = false), 1500)
}

function downloadLogs() {
  const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(props.logs, null, 2))
  const downloadAnchor = document.createElement('a')
  downloadAnchor.setAttribute('href', dataStr)
  downloadAnchor.setAttribute('download', `ocpp-logs-${Date.now()}.json`)
  document.body.appendChild(downloadAnchor)
  downloadAnchor.click()
  downloadAnchor.remove()
}
</script>

<template>
  <div class="bg-white rounded-xl border border-slate-200 shadow-2xs flex flex-col h-full overflow-hidden select-none">
    <!-- 콘솔 상단 툴바 -->
    <div class="px-3.5 py-2.5 bg-slate-50 border-b border-slate-200 flex flex-wrap items-center justify-between gap-2 flex-shrink-0">
      <div class="flex items-center gap-2">
        <div class="w-2 h-2 rounded-full bg-indigo-500 animate-pulse"></div>
        <h3 class="text-xs font-bold text-slate-900 tracking-tight">실시간 WireTap 패킷 터미널</h3>
        <span class="text-[10px] font-mono px-1.5 py-0.2 rounded font-bold bg-slate-200 text-slate-700">
          {{ filteredLogs.length }} / {{ logs.length }}
        </span>
      </div>

      <div class="flex items-center gap-1.5 text-xs">
        <!-- 방향 필터 탭 -->
        <div class="flex items-center bg-white border border-slate-200 rounded-md p-0.5 text-[10px]">
          <button
            v-for="f in (['ALL', 'OUT', 'IN', 'ERR'] as const)"
            :key="f"
            @click="filterDirection = f"
            class="px-2 py-0.5 rounded transition-all cursor-pointer font-medium"
            :class="filterDirection === f ? 'bg-slate-900 text-white font-bold' : 'text-slate-600 hover:text-slate-900'"
          >
            {{ f === 'ALL' ? '전체' : f === 'OUT' ? '▲ OUT' : f === 'IN' ? '▼ IN' : '✕ ERR' }}
          </button>
        </div>

        <!-- 액션 검색 -->
        <input
          v-model="searchQuery"
          placeholder="액션/키워드 검색..."
          class="w-28 sm:w-32 bg-white border border-slate-200 rounded-md px-2 py-1 text-[11px] font-mono text-slate-800 focus:outline-hidden focus:ring-1 focus:ring-sky-500"
        />

        <!-- 로그 다운로드 버튼 -->
        <button
          @click="downloadLogs"
          class="p-1 text-slate-500 hover:text-sky-600 border border-slate-200 hover:border-sky-300 rounded-md bg-white transition-colors cursor-pointer"
          title="로그 JSON 파일로 내보내기"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
          </svg>
        </button>

        <!-- 지우기 버튼 -->
        <button
          @click="emit('clear')"
          class="px-2 py-1 text-[11px] text-slate-500 hover:text-rose-600 border border-slate-200 hover:border-rose-300 rounded-md bg-white transition-colors cursor-pointer"
          title="전체 패킷 로그 삭제"
        >
          지우기
        </button>

        <!-- 닫기 버튼 (사이드패널 시) -->
        <button
          v-if="isSidePanel"
          @click="emit('close')"
          class="p-1 text-slate-400 hover:text-slate-600 border border-slate-200 rounded-md bg-white transition-colors cursor-pointer"
          title="터미널 닫기"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 패킷 스트림 목록 (상단/좌측) + 상세 뷰어 (하단/우측) -->
    <div class="flex-1 flex flex-col overflow-hidden divide-y divide-slate-200 min-h-0">
      <!-- 패킷 스트림 리스트 -->
      <div class="flex-1 overflow-y-auto divide-y divide-slate-100 font-mono text-[11px] min-h-[220px]">
        <div
          v-if="filteredLogs.length === 0"
          class="h-full flex flex-col items-center justify-center text-slate-400 text-xs py-12 gap-1.5"
        >
          <svg class="w-6 h-6 text-slate-300" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
          </svg>
          <span>수신/발송된 OCPP 패킷이 없습니다.</span>
        </div>

        <div
          v-for="log in filteredLogs"
          :key="log.id"
          @click="selectLog(log)"
          class="px-3 py-2 hover:bg-slate-50 cursor-pointer transition-colors flex items-center justify-between gap-2"
          :class="{ 'bg-sky-50/80 border-l-2 border-sky-500': selectedLog?.id === log.id }"
        >
          <div class="flex items-center gap-2 overflow-hidden flex-1">
            <!-- 방향 배지 -->
            <span
              class="px-1.5 py-0.2 rounded text-[9px] font-bold tracking-tight uppercase flex-shrink-0"
              :class="{
                'bg-sky-100 text-sky-800 border border-sky-200': log.direction === 'OUT',
                'bg-emerald-100 text-emerald-800 border border-emerald-200': log.direction === 'IN',
                'bg-rose-100 text-rose-800 border border-rose-200': log.direction === 'ERR'
              }"
            >
              {{ log.direction === 'OUT' ? '▲ OUT' : log.direction === 'IN' ? '▼ IN' : '✕ ERR' }}
            </span>

            <!-- 타임스탬프 -->
            <span class="text-slate-400 text-[10px] flex-shrink-0">{{ log.timestamp }}</span>

            <!-- 액션명 -->
            <strong class="text-slate-800 truncate text-[11px] font-bold">{{ log.action || 'CALL' }}</strong>

            <!-- 페이로드 요약 스트링 -->
            <span class="text-slate-400 text-[10px] truncate hidden sm:inline max-w-[200px]">
              {{ JSON.stringify(log.payload) }}
            </span>
          </div>

          <!-- 응답 지연 (Latency) -->
          <div v-if="log.latencyMs !== undefined" class="flex-shrink-0 text-[10px] font-mono text-emerald-600 bg-emerald-50 px-1.5 py-0.2 rounded border border-emerald-100">
            {{ log.latencyMs }}ms
          </div>
        </div>
      </div>

      <!-- 선택된 패킷 상세 뷰어 (하단 패널) -->
      <div v-if="selectedLog" class="h-60 bg-slate-900 text-slate-100 flex flex-col flex-shrink-0 overflow-hidden font-mono text-xs">
        <div class="px-3 py-1.5 bg-slate-950/80 border-b border-slate-800 flex items-center justify-between">
          <div class="flex items-center gap-2">
            <span
              class="px-1.5 py-0.2 rounded text-[9px] font-bold"
              :class="selectedLog.direction === 'OUT' ? 'bg-sky-500/20 text-sky-400' : selectedLog.direction === 'IN' ? 'bg-emerald-500/20 text-emerald-400' : 'bg-rose-500/20 text-rose-400'"
            >
              {{ selectedLog.direction }}
            </span>
            <span class="font-bold text-slate-200">{{ selectedLog.action }}</span>
            <span class="text-slate-500 text-[10px]">ID: {{ selectedLog.messageId }}</span>
          </div>

          <div class="flex items-center gap-2">
            <span v-if="selectedLog.latencyMs !== undefined" class="text-emerald-400 text-[10px]">
              왕복 소요: {{ selectedLog.latencyMs }}ms
            </span>
            <button
              @click="copyJson(selectedLog.payload)"
              class="px-2 py-0.5 rounded text-[10px] bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-colors cursor-pointer flex items-center gap-1"
            >
              <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
              </svg>
              <span>{{ copied ? '복사됨!' : 'JSON 복사' }}</span>
            </button>
          </div>
        </div>

        <div class="flex-1 overflow-auto p-3 text-[11px] leading-relaxed selection:bg-sky-500/30">
          <pre class="text-sky-300 font-mono">{{ JSON.stringify(selectedLog.payload, null, 2) }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>
