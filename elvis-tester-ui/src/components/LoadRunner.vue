<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const topic = ref('ocpp-raw-events')
const chargeBoxPrefix = ref('CP_LOAD_')
const deviceCount = ref(100)
const tps = ref(50)
const durationSeconds = ref(30)
const templateName = ref('MeterValues')

const isRunning = ref(false)
const totalSent = ref(0)
const totalFail = ref(0)
const actionLoading = ref(false)
let statusTimer: any = null

onMounted(() => {
  checkStatus()
  statusTimer = setInterval(checkStatus, 1000)
})

onUnmounted(() => {
  if (statusTimer) clearInterval(statusTimer)
})

async function checkStatus() {
  try {
    const res = await fetch('/api/tester/load/status')
    if (res.ok) {
      const data = await res.json()
      isRunning.value = data.running
      totalSent.value = data.totalSent
      totalFail.value = data.totalFail
    }
  } catch (err) {
    // ignore
  }
}

async function startLoad() {
  actionLoading.value = true
  try {
    const res = await fetch('/api/tester/load/start', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        topic: topic.value,
        chargeBoxPrefix: chargeBoxPrefix.value,
        deviceCount: Number(deviceCount.value),
        tps: Number(tps.value),
        durationSeconds: Number(durationSeconds.value),
        templateName: templateName.value
      })
    })
    const data = await res.json()
    if (data.success) {
      isRunning.value = true
    } else {
      alert(data.message)
    }
  } catch (e: any) {
    alert('부하 테스트 시작 실패: ' + e.message)
  } finally {
    actionLoading.value = false
  }
}

async function stopLoad() {
  actionLoading.value = true
  try {
    const res = await fetch('/api/tester/load/stop', {
      method: 'POST'
    })
    const data = await res.json()
    if (data.success) {
      isRunning.value = false
    }
  } catch (e: any) {
    alert('부하 테스트 중지 실패: ' + e.message)
  } finally {
    actionLoading.value = false
  }
}
</script>

<template>
  <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-xs dark:shadow-xl flex flex-col gap-5 transition-colors duration-200">
    <div class="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-4">
      <div class="flex items-center gap-2">
        <span class="w-3 h-3 rounded-full" :class="isRunning ? 'bg-amber-500 animate-ping' : 'bg-slate-400 dark:bg-slate-600'"></span>
        <h2 class="text-lg font-semibold text-slate-800 dark:text-slate-100">Kafka 부하 스트레스 발생기 (Load Generator)</h2>
      </div>
      <div class="flex items-center gap-2">
        <span
          class="text-xs px-2.5 py-1 rounded font-medium"
          :class="isRunning ? 'bg-amber-500/10 text-amber-600 dark:bg-amber-500/20 dark:text-amber-300 border border-amber-500/25 dark:border-amber-500/30' : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 border border-slate-200 dark:border-transparent'"
        >
          {{ isRunning ? '⚡ 부하 인젝션 중' : '대기 상태' }}
        </span>
      </div>
    </div>

    <!-- 부하 설정 그리드 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
      <div>
        <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1.5">가상 충전기 수 (Devices)</label>
        <input
          v-model="deviceCount"
          type="number"
          :disabled="isRunning"
          class="w-full bg-slate-50 dark:bg-slate-950 border border-slate-300 dark:border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-800 dark:text-slate-200 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition"
        />
      </div>

      <div>
        <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1.5">목표 초당 처리량 (Target TPS)</label>
        <input
          v-model="tps"
          type="number"
          :disabled="isRunning"
          class="w-full bg-slate-50 dark:bg-slate-950 border border-slate-300 dark:border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-800 dark:text-slate-200 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition"
        />
      </div>

      <div>
        <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1.5">지속 시간 (초)</label>
        <input
          v-model="durationSeconds"
          type="number"
          :disabled="isRunning"
          class="w-full bg-slate-50 dark:bg-slate-950 border border-slate-300 dark:border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-800 dark:text-slate-200 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition"
        />
      </div>

      <div>
        <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1.5">발행 메시지 유형</label>
        <select
          v-model="templateName"
          :disabled="isRunning"
          class="w-full bg-slate-50 dark:bg-slate-950 border border-slate-300 dark:border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-800 dark:text-slate-200 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition"
        >
          <option value="MeterValues">MeterValues (전력량 계측값)</option>
          <option value="BootNotification">BootNotification (부팅 등록)</option>
          <option value="StatusNotification">StatusNotification (상태 변경)</option>
        </select>
      </div>
    </div>

    <!-- 통계 모니터 카드 -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 bg-slate-50 dark:bg-slate-950/60 p-4 rounded-lg border border-slate-200 dark:border-slate-800/80 shadow-2xs">
      <div>
        <div class="text-xs text-slate-500">누적 발송 성공 건수</div>
        <div class="text-2xl font-bold font-mono text-emerald-600 dark:text-emerald-400 mt-1">{{ totalSent.toLocaleString() }} 건</div>
      </div>
      <div>
        <div class="text-xs text-slate-500">누적 실패 건수</div>
        <div class="text-2xl font-bold font-mono text-rose-600 dark:text-rose-400 mt-1">{{ totalFail.toLocaleString() }} 건</div>
      </div>
      <div>
        <div class="text-xs text-slate-500">예상 총 주입량</div>
        <div class="text-2xl font-bold font-mono text-indigo-600 dark:text-indigo-300 mt-1">{{ (tps * durationSeconds).toLocaleString() }} 건</div>
      </div>
    </div>

    <!-- 제어 버튼 -->
    <div class="flex justify-end gap-3 pt-2 border-t border-slate-200 dark:border-slate-800/80">
      <button
        v-if="!isRunning"
        @click="startLoad"
        :disabled="actionLoading"
        class="bg-amber-600 hover:bg-amber-500 text-white font-medium text-sm px-6 py-2.5 rounded-lg shadow-md shadow-amber-600/20 transition active:scale-95 cursor-pointer disabled:opacity-50"
      >
        ⚡ 부하 테스트 시작
      </button>
      <button
        v-else
        @click="stopLoad"
        :disabled="actionLoading"
        class="bg-rose-600 hover:bg-rose-500 text-white font-medium text-sm px-6 py-2.5 rounded-lg shadow-md shadow-rose-600/20 transition active:scale-95 cursor-pointer disabled:opacity-50"
      >
        🛑 긴급 중지
      </button>
    </div>
  </div>
</template>
