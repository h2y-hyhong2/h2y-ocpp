<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

interface SystemStatus {
  kafka: boolean
  wsGateway: boolean
  syncEngine: boolean
  testerBackend: boolean
}

const status = ref<SystemStatus>({
  kafka: false,
  wsGateway: false,
  syncEngine: false,
  testerBackend: true
})

const loadingAction = ref<string | null>(null)
let pollTimer: any = null

async function checkStatus() {
  try {
    const res = await fetch('/api/tester/system/status')
    if (res.ok) {
      status.value = await res.json()
    }
  } catch {
    // ignore
  }
}

onMounted(() => {
  checkStatus()
  pollTimer = setInterval(checkStatus, 2500)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

async function executeAction(actionKey: string, endpoint: string) {
  loadingAction.value = actionKey
  try {
    const res = await fetch(`/api/tester/system/${endpoint}`, { method: 'POST' })
    const data = await res.json()
    if (!data.success) {
      alert('실행 알림: ' + data.message)
    }
    setTimeout(checkStatus, 3000)
  } catch (err: any) {
    alert('명령 호출 에러: ' + err.message)
  } finally {
    loadingAction.value = null
  }
}
</script>

<template>
  <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-xs dark:shadow-xl flex flex-col gap-4 transition-colors duration-200">
    <div class="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-3">
      <div class="flex items-center gap-2">
        <span class="w-3 h-3 rounded-full bg-cyan-500"></span>
        <h2 class="text-sm font-semibold text-slate-800 dark:text-slate-100 uppercase tracking-wider">
          ELVIS 인프라 & 서비스 런처 (One-Click Runner)
        </h2>
      </div>
      <span class="text-xs text-slate-500 dark:text-slate-400 font-mono">터미널 없이 UI에서 직접 서비스 기동</span>
    </div>

    <!-- 4대 서비스 상태 및 실행 카드 그리드 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3.5">
      <!-- 1. Apache Kafka -->
      <div class="bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800/90 rounded-lg p-3.5 flex flex-col justify-between gap-3 shadow-2xs">
        <div class="flex items-center justify-between">
          <span class="text-xs font-semibold text-slate-800 dark:text-slate-200">Apache Kafka (KRaft)</span>
          <span
            class="text-[10px] px-2 py-0.5 rounded-full font-mono font-medium flex items-center gap-1"
            :class="status.kafka ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/25 dark:border-emerald-500/30' : 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/25 dark:border-rose-500/30'"
          >
            <span class="w-1.5 h-1.5 rounded-full" :class="status.kafka ? 'bg-emerald-500' : 'bg-rose-500'"></span>
            {{ status.kafka ? 'PORT: 9092' : 'OFFLINE' }}
          </span>
        </div>
        <p class="text-[11px] text-slate-500 dark:text-slate-400">메시지 브로커 핵심 인프라</p>
        <button
          @click="executeAction('kafka', 'start-kafka')"
          :disabled="loadingAction === 'kafka' || status.kafka"
          class="w-full text-xs font-medium py-1.5 rounded bg-slate-200 hover:bg-slate-300 dark:bg-slate-800 dark:hover:bg-slate-700 disabled:opacity-40 text-slate-700 dark:text-slate-200 transition active:scale-98 cursor-pointer disabled:cursor-not-allowed flex items-center justify-center gap-1.5"
        >
          <span>{{ status.kafka ? '✔ 기동 중' : '▶ Kafka 기동' }}</span>
        </button>
      </div>

      <!-- 2. Kafka Topics -->
      <div class="bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800/90 rounded-lg p-3.5 flex flex-col justify-between gap-3 shadow-2xs">
        <div class="flex items-center justify-between">
          <span class="text-xs font-semibold text-slate-800 dark:text-slate-200">Kafka 4대 토픽</span>
          <span class="text-[10px] bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-400 px-2 py-0.5 rounded font-mono">자동 생성</span>
        </div>
        <p class="text-[11px] text-slate-500 dark:text-slate-400">ocpp-raw-events 등 4종</p>
        <button
          @click="executeAction('topics', 'create-topics')"
          :disabled="loadingAction === 'topics' || !status.kafka"
          class="w-full text-xs font-medium py-1.5 rounded bg-slate-200 hover:bg-slate-300 dark:bg-slate-800 dark:hover:bg-slate-700 disabled:opacity-40 text-slate-700 dark:text-slate-200 transition active:scale-98 cursor-pointer disabled:cursor-not-allowed flex items-center justify-center gap-1.5"
        >
          <span>⚙️ 토픽 생성 스크립트</span>
        </button>
      </div>

      <!-- 3. elvis-connect-ws -->
      <div class="bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800/90 rounded-lg p-3.5 flex flex-col justify-between gap-3 shadow-2xs">
        <div class="flex items-center justify-between">
          <span class="text-xs font-semibold text-slate-800 dark:text-slate-200">WS Gateway (:8080)</span>
          <span
            class="text-[10px] px-2 py-0.5 rounded-full font-mono font-medium flex items-center gap-1"
            :class="status.wsGateway ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/25 dark:border-emerald-500/30' : 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/25 dark:border-rose-500/30'"
          >
            <span class="w-1.5 h-1.5 rounded-full" :class="status.wsGateway ? 'bg-emerald-500' : 'bg-rose-500'"></span>
            {{ status.wsGateway ? 'ONLINE' : 'OFFLINE' }}
          </span>
        </div>
        <p class="text-[11px] text-slate-500 dark:text-slate-400">충전기 웹소켓 인그레스</p>
        <button
          @click="executeAction('ws', 'start-ws')"
          :disabled="loadingAction === 'ws' || status.wsGateway"
          class="w-full text-xs font-medium py-1.5 rounded bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 text-white transition active:scale-98 cursor-pointer disabled:cursor-not-allowed flex items-center justify-center gap-1.5 shadow-xs"
        >
          <span>{{ status.wsGateway ? '✔ 서비스 정상' : '▶ 게이트웨이 기동' }}</span>
        </button>
      </div>

      <!-- 4. elvis-connect-sync -->
      <div class="bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800/90 rounded-lg p-3.5 flex flex-col justify-between gap-3 shadow-2xs">
        <div class="flex items-center justify-between">
          <span class="text-xs font-semibold text-slate-800 dark:text-slate-200">Sync Engine (:8082)</span>
          <span
            class="text-[10px] px-2 py-0.5 rounded-full font-mono font-medium flex items-center gap-1"
            :class="status.syncEngine ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/25 dark:border-emerald-500/30' : 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/25 dark:border-rose-500/30'"
          >
            <span class="w-1.5 h-1.5 rounded-full" :class="status.syncEngine ? 'bg-emerald-500' : 'bg-rose-500'"></span>
            {{ status.syncEngine ? 'ONLINE' : 'OFFLINE' }}
          </span>
        </div>
        <p class="text-[11px] text-slate-500 dark:text-slate-400">Kafka 소비자 & 싱크 처리</p>
        <button
          @click="executeAction('sync', 'start-sync')"
          :disabled="loadingAction === 'sync' || status.syncEngine"
          class="w-full text-xs font-medium py-1.5 rounded bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 text-white transition active:scale-98 cursor-pointer disabled:cursor-not-allowed flex items-center justify-center gap-1.5 shadow-xs"
        >
          <span>{{ status.syncEngine ? '✔ 서비스 정상' : '▶ 싱크 엔진 기동' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>
