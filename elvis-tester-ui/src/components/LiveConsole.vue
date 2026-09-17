<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'

interface LogEntry {
  id: string
  timestamp: string
  direction: string
  topic: string
  chargeBoxId: string
  action: string
  payload: string
  status: string
  message: string
}

const logs = ref<LogEntry[]>([])
const selectedTopicFilter = ref('ALL')
const searchQuery = ref('')
const autoScroll = ref(true)
const selectedLog = ref<LogEntry | null>(null)
const logContainer = ref<HTMLElement | null>(null)

let eventSource: EventSource | null = null

onMounted(async () => {
  // 초기 최근 로그 가져오기
  try {
    const res = await fetch('/api/tester/logs')
    if (res.ok) {
      logs.value = await res.json()
    }
  } catch (err) {
    console.error('초기 로그 조회 실패:', err)
  }

  // SSE 실시간 스트림 연결
  connectSse()
})

onUnmounted(() => {
  if (eventSource) {
    eventSource.close()
  }
})

function connectSse() {
  eventSource = new EventSource('/api/tester/stream')
  
  eventSource.addEventListener('kafka-log', (event: MessageEvent) => {
    try {
      const entry: LogEntry = JSON.parse(event.data)
      logs.value.unshift(entry)
      if (logs.value.length > 500) {
        logs.value.pop()
      }
      if (autoScroll.value) {
        scrollToTop()
      }
    } catch (e) {
      console.error('SSE 파싱 에러:', e)
    }
  })

  eventSource.onerror = () => {
    // 자동 재연결 브라우저 기본 지원
  }
}

function scrollToTop() {
  nextTick(() => {
    if (logContainer.value) {
      logContainer.value.scrollTop = 0
    }
  })
}

function clearLogs() {
  logs.value = []
}

const filteredLogs = computed(() => {
  return logs.value.filter(item => {
    const matchTopic = selectedTopicFilter.value === 'ALL' || item.topic === selectedTopicFilter.value
    const matchSearch = !searchQuery.value ||
      item.chargeBoxId.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      item.action.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      item.payload.toLowerCase().includes(searchQuery.value.toLowerCase())
    return matchTopic && matchSearch
  })
})

function formatTime(iso: string) {
  if (!iso) return ''
  const d = new Date(iso)
  return d.toTimeString().split(' ')[0] + '.' + String(d.getMilliseconds()).padStart(3, '0')
}

function prettyJson(raw: string) {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}
</script>

<template>
  <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-xs dark:shadow-xl flex flex-col gap-4 transition-colors duration-200">
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-200 dark:border-slate-800 pb-4">
      <div class="flex items-center gap-2">
        <span class="w-2.5 h-2.5 rounded-full bg-cyan-500 animate-pulse"></span>
        <h2 class="text-lg font-semibold text-slate-800 dark:text-slate-100">실시간 Kafka 메시지 스트림 콘솔</h2>
        <span class="text-xs bg-slate-100 dark:bg-slate-800 text-cyan-600 dark:text-cyan-300 border border-slate-200 dark:border-transparent px-2 py-0.5 rounded-full font-mono">
          {{ filteredLogs.length }} 건
        </span>
      </div>

      <!-- 상단 컨트롤러 -->
      <div class="flex items-center gap-2 flex-wrap">
        <!-- 토픽 필터 -->
        <select
          v-model="selectedTopicFilter"
          class="bg-slate-50 dark:bg-slate-950 border border-slate-300 dark:border-slate-700 text-xs rounded-lg px-2.5 py-1.5 text-slate-800 dark:text-slate-200 focus:outline-none focus:border-indigo-500 transition"
        >
          <option value="ALL">모든 토픽</option>
          <option value="ocpp-raw-events">ocpp-raw-events</option>
          <option value="ocpp-outbound-commands">ocpp-outbound-commands</option>
          <option value="ocpp-ui-notifications">ocpp-ui-notifications</option>
          <option value="ocpp-raw-events.DLT">ocpp-raw-events.DLT</option>
        </select>

        <!-- 검색창 -->
        <input
          v-model="searchQuery"
          type="text"
          placeholder="단말ID / Action 검색..."
          class="bg-slate-50 dark:bg-slate-950 border border-slate-300 dark:border-slate-700 text-xs rounded-lg px-2.5 py-1.5 text-slate-800 dark:text-slate-200 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:border-indigo-500 w-36 sm:w-44 transition"
        />

        <!-- 비우기 버튼 -->
        <button
          @click="clearLogs"
          class="text-xs bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-transparent px-3 py-1.5 rounded-lg transition"
        >
          지우기
        </button>
      </div>
    </div>

    <!-- 로그 리스트 테이블 / 스트림 창 -->
    <div
      ref="logContainer"
      class="h-96 overflow-y-auto border border-slate-300 dark:border-slate-800/80 bg-slate-950 rounded-lg p-2 font-mono text-xs flex flex-col gap-1.5 divide-y divide-slate-900 shadow-inner"
    >
      <div v-if="filteredLogs.length === 0" class="text-slate-500 text-center py-20">
        수신된 Kafka 패킷이 없습니다. 메시지를 발송하거나 모니터링을 대기 중입니다.
      </div>

      <div
        v-for="log in filteredLogs"
        :key="log.id"
        @click="selectedLog = log"
        class="pt-1.5 pb-1 px-2 rounded hover:bg-slate-900 cursor-pointer transition flex items-center justify-between gap-3 text-slate-300"
      >
        <div class="flex items-center gap-2.5 overflow-hidden">
          <span class="text-slate-500 shrink-0">{{ formatTime(log.timestamp) }}</span>

          <span
            class="text-[10px] px-1.5 py-0.5 rounded font-semibold shrink-0"
            :class="{
              'bg-emerald-950 text-emerald-300 border border-emerald-800': log.direction === 'INBOUND',
              'bg-purple-950 text-purple-300 border border-purple-800': log.direction === 'OUTBOUND',
              'bg-cyan-950 text-cyan-300 border border-cyan-800': log.direction === 'MONITOR'
            }"
          >
            {{ log.direction }}
          </span>

          <span class="text-slate-400 font-semibold truncate max-w-[120px]">{{ log.chargeBoxId }}</span>
          <span class="text-indigo-400 font-medium shrink-0">{{ log.action }}</span>
          <span class="text-slate-500 truncate max-w-[280px] sm:max-w-[450px]">{{ log.payload }}</span>
        </div>

        <div class="flex items-center gap-2 shrink-0">
          <span class="text-[10px] text-slate-500 bg-slate-900 px-1.5 py-0.5 rounded">{{ log.topic }}</span>
          <span
            class="w-2 h-2 rounded-full"
            :class="log.status === 'SUCCESS' ? 'bg-emerald-500' : 'bg-rose-500'"
          ></span>
        </div>
      </div>
    </div>

    <!-- 페이로드 상세 모달 -->
    <div
      v-if="selectedLog"
      class="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 z-50"
      @click.self="selectedLog = null"
    >
      <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl p-5 max-w-2xl w-full shadow-2xl flex flex-col gap-4">
        <div class="flex justify-between items-center border-b border-slate-200 dark:border-slate-800 pb-3">
          <div class="flex items-center gap-2">
            <span class="text-indigo-600 dark:text-indigo-400 font-semibold font-mono">{{ selectedLog.chargeBoxId }}</span>
            <span class="text-slate-500 dark:text-slate-400 font-mono text-xs">({{ selectedLog.action }})</span>
          </div>
          <button @click="selectedLog = null" class="text-slate-500 hover:text-slate-800 dark:text-slate-400 dark:hover:text-slate-200 text-sm">✕ 닫기</button>
        </div>

        <div class="grid grid-cols-2 gap-2 text-xs font-mono text-slate-600 dark:text-slate-400 bg-slate-50 dark:bg-slate-950 p-3 rounded-lg border border-slate-200 dark:border-slate-800">
          <div><span class="text-slate-400 dark:text-slate-600">토픽:</span> {{ selectedLog.topic }}</div>
          <div><span class="text-slate-400 dark:text-slate-600">수신시각:</span> {{ selectedLog.timestamp }}</div>
          <div><span class="text-slate-400 dark:text-slate-600">구분:</span> {{ selectedLog.direction }}</div>
          <div><span class="text-slate-400 dark:text-slate-600">상태:</span> {{ selectedLog.status }} ({{ selectedLog.message }})</div>
        </div>

        <div>
          <label class="text-xs text-slate-600 dark:text-slate-400 block mb-1">Payload 원문 (JSON Format):</label>
          <pre class="bg-slate-950 border border-slate-800 rounded-lg p-3 text-xs font-mono text-emerald-400 max-h-72 overflow-y-auto leading-relaxed">{{ prettyJson(selectedLog.payload) }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>
