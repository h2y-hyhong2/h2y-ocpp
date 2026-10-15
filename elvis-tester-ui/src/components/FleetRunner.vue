<script setup lang="ts">
import { ref } from 'vue'

const baseWsUrl = ref('ws://127.0.0.1:18080/ocpp')
const count = ref(10)
const prefix = ref('CP-FLEET-')
const isRunning = ref(false)
const fleetSockets = ref<WebSocket[]>([])
const connectedCount = ref(0)
const totalSent = ref(0)
let heartbeatInterval: any = null

function startFleet() {
  if (isRunning.value) return
  isRunning.value = true
  connectedCount.value = 0
  totalSent.value = 0
  fleetSockets.value = []

  const targetCount = count.value

  for (let i = 1; i <= targetCount; i++) {
    const chargeBoxId = `${prefix.value}${String(i).padStart(3, '0')}`
    const url = `${baseWsUrl.value.replace(/\/$/, '')}/${encodeURIComponent(chargeBoxId)}`

    try {
      const ws = new WebSocket(url)
      fleetSockets.value.push(ws)

      ws.onopen = () => {
        connectedCount.value++
        // 부팅 패킷 전송
        const bootMsg = JSON.stringify([2, `boot-${chargeBoxId}`, 'BootNotification', {
          chargePointVendor: 'LS E-Link',
          chargePointModel: 'HPC-Fleet',
          firmwareVersion: '1.0'
        }])
        ws.send(bootMsg)
        totalSent.value++
      }

      ws.onclose = () => {
        connectedCount.value = Math.max(0, connectedCount.value - 1)
      }
    } catch (e) {
      console.error(e)
    }
  }

  // 15초마다 일괄 Heartbeat 전송
  heartbeatInterval = setInterval(() => {
    fleetSockets.value.forEach((ws, idx) => {
      if (ws.readyState === WebSocket.OPEN) {
        const hbMsg = JSON.stringify([2, `hb-${idx}-${Date.now()}`, 'Heartbeat', {}])
        ws.send(hbMsg)
        totalSent.value++
      }
    })
  }, 15000)
}

function stopFleet() {
  if (heartbeatInterval) {
    clearInterval(heartbeatInterval)
    heartbeatInterval = null
  }
  fleetSockets.value.forEach(ws => {
    if (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING) {
      ws.close(1000, 'Fleet Stopped')
    }
  })
  fleetSockets.value = []
  isRunning.value = false
  connectedCount.value = 0
}
</script>

<template>
  <div class="bg-white rounded-xl border border-slate-200 p-4 shadow-2xs space-y-3 select-none">
    <div class="flex items-center justify-between pb-2 border-b border-slate-100">
      <div class="flex items-center gap-2">
        <div class="w-6 h-6 rounded-md bg-indigo-50 text-indigo-600 flex items-center justify-center border border-indigo-100">
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
        </div>
        <h3 class="text-xs font-bold text-slate-900 tracking-tight">다중 가상 충전기 동시 부하 시뮬레이션 (Fleet Runner)</h3>
      </div>
      <span
        class="text-[10px] font-mono font-bold px-2.5 py-0.5 rounded-full border"
        :class="isRunning ? 'bg-indigo-50 text-indigo-700 border-indigo-200' : 'bg-slate-100 text-slate-500 border-slate-200'"
      >
        {{ isRunning ? 'RUNNING' : 'STOPPED' }}
      </span>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-4 gap-3 text-xs">
      <div>
        <label class="block text-[11px] font-semibold text-slate-700 mb-1">충전기 접두사</label>
        <input v-model="prefix" :disabled="isRunning" class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 font-mono text-xs" />
      </div>
      <div>
        <label class="block text-[11px] font-semibold text-slate-700 mb-1">동시 가동 대수 (대)</label>
        <input v-model.number="count" type="number" min="1" max="50" :disabled="isRunning" class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 font-mono text-xs" />
      </div>
      <div class="sm:col-span-2 flex items-end gap-2">
        <button
          v-if="!isRunning"
          @click="startFleet"
          class="flex-1 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-bold shadow-2xs cursor-pointer transition-all flex items-center justify-center gap-1.5"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>{{ count }}대 동시 가동 시작</span>
        </button>
        <button
          v-else
          @click="stopFleet"
          class="flex-1 py-1.5 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-xs font-bold shadow-2xs cursor-pointer transition-all flex items-center justify-center gap-1.5"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 10a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1v-4z" />
          </svg>
          <span>전수 가동 중지</span>
        </button>
      </div>
    </div>

    <!-- 상태 요약 바 -->
    <div class="p-2.5 bg-slate-50 rounded-lg border border-slate-200 flex items-center justify-around text-xs font-mono">
      <div>연결 수립: <strong class="text-emerald-700 font-bold">{{ connectedCount }}</strong> / {{ count }}대</div>
      <div class="text-slate-300">|</div>
      <div>누적 발송 패킷: <strong class="text-indigo-700 font-bold">{{ totalSent }}</strong>건</div>
      <div class="text-slate-300">|</div>
      <div class="text-slate-600">주기: 15s (Auto Heartbeat)</div>
    </div>
  </div>
</template>
