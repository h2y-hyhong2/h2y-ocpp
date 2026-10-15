<script setup lang="ts">
import { ref } from 'vue'
import { useVirtualCharger } from '../composables/useVirtualCharger'

const props = defineProps<{
  charger: ReturnType<typeof useVirtualCharger>
}>()

const { state, sendBootNotification, sendHeartbeat, sendStatusNotification, sendAuthorize, sendStartTransaction, sendMeterValues, sendStopTransaction, toggleAutoHeartbeat } = props.charger

const presetChargers = [
  '00000001'
]

const rfidInput = ref('RFID-10001')
const meterStartWh = ref(105000)
const stopReason = ref('Local')
const customAction = ref('StatusNotification')
const customJson = ref('{\n  "connectorId": 1,\n  "errorCode": "NoError",\n  "status": "Available"\n}')
const showCustomModal = ref(false)

function onPresetChange(event: Event) {
  const target = event.target as HTMLSelectElement
  state.chargeBoxId = target.value
}

function sendCustomPayload() {
  try {
    const parsed = JSON.parse(customJson.value)
    props.charger.sendCall(customAction.value, parsed)
    showCustomModal.value = false
  } catch (e: any) {
    alert('JSON 파싱 오류: ' + e.message)
  }
}
</script>

<template>
  <div class="space-y-4">
    <!-- 1. 단말 설정 및 접속 제어 카드 -->
    <div class="bg-white rounded-xl border border-slate-200 p-4 shadow-2xs">
      <div class="flex items-center justify-between pb-3 border-b border-slate-100 mb-3">
        <div class="flex items-center gap-2">
          <span class="w-2.5 h-2.5 rounded-full" :class="state.connected ? 'bg-emerald-500 animate-pulse' : state.connecting ? 'bg-amber-400 animate-ping' : 'bg-slate-300'"></span>
          <h2 class="text-xs font-bold text-slate-900 tracking-tight">가상 충전기(Virtual EVSE) 단말 제어</h2>
        </div>
        <div class="flex items-center gap-1.5">
          <span class="text-[10px] font-mono px-2 py-0.5 rounded-full" :class="state.connected ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-slate-100 text-slate-500'">
            {{ state.connected ? 'ONLINE (:18080)' : state.connecting ? 'CONNECTING...' : 'OFFLINE' }}
          </span>
        </div>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
        <div>
          <label class="block text-[11px] font-medium text-slate-600 mb-1">충전기 식별자 (ChargeBoxId)</label>
          <div class="flex items-center gap-1.5">
            <select
              :value="presetChargers.includes(state.chargeBoxId) ? state.chargeBoxId : 'custom'"
              @change="onPresetChange"
              :disabled="state.connected"
              class="w-32 bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 text-xs text-slate-800 focus:outline-hidden focus:ring-1 focus:ring-sky-500 disabled:opacity-60"
            >
              <option v-for="cp in presetChargers" :key="cp" :value="cp">{{ cp }}</option>
              <option value="custom">직접입력</option>
            </select>
            <input
              v-model="state.chargeBoxId"
              :disabled="state.connected"
              placeholder="예: 00000001"
              class="flex-1 bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 text-xs font-mono text-slate-800 focus:outline-hidden focus:ring-1 focus:ring-sky-500 disabled:opacity-60"
            />

          </div>
        </div>

        <div>
          <label class="block text-[11px] font-medium text-slate-600 mb-1">CSMS WebSocket 게이트웨이</label>
          <div class="flex items-center gap-2">
            <input
              v-model="state.wsUrl"
              :disabled="state.connected"
              placeholder="ws://127.0.0.1:18080/ocpp"
              class="flex-1 bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 text-xs font-mono text-slate-800 focus:outline-hidden focus:ring-1 focus:ring-sky-500 disabled:opacity-60"
            />
            <button
              v-if="!state.connected"
              @click="props.charger.connect"
              :disabled="state.connecting"
              class="px-3.5 py-1.5 bg-sky-600 hover:bg-sky-700 text-white rounded-lg text-xs font-medium transition-colors shadow-2xs cursor-pointer disabled:opacity-50"
            >
              연결
            </button>
            <button
              v-else
              @click="props.charger.disconnect"
              class="px-3.5 py-1.5 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-xs font-medium transition-colors shadow-2xs cursor-pointer"
            >
              연결 해제
            </button>
          </div>
        </div>
      </div>

      <!-- 단말 상태 요약 바 -->
      <div class="mt-3 pt-3 border-t border-slate-100 flex flex-wrap items-center justify-between gap-2 text-[11px]">
        <div class="flex items-center gap-3">
          <span class="text-slate-500">현재 상태: <strong class="text-slate-900 font-semibold">{{ state.status }}</strong></span>
          <span class="text-slate-300">|</span>
          <span class="text-slate-500">세션 ID: <strong class="font-mono text-indigo-600">{{ state.transactionId || 'None' }}</strong></span>
          <span class="text-slate-300">|</span>
          <span class="text-slate-500">누적: <strong class="font-mono text-slate-800">{{ (state.meterValueWh / 1000).toFixed(2) }} kWh</strong></span>
          <span class="text-slate-300">|</span>
          <span class="text-slate-500">SoC: <strong class="font-mono text-emerald-600">{{ state.soc }}%</strong></span>
        </div>

        <div class="flex items-center gap-2">
          <label class="flex items-center gap-1.5 cursor-pointer text-slate-600 select-none">
            <input
              type="checkbox"
              :checked="state.autoHeartbeat"
              @change="toggleAutoHeartbeat(($event.target as HTMLInputElement).checked)"
              class="rounded border-slate-300 text-sky-600 focus:ring-sky-500 cursor-pointer"
            />
            <span>자동 Heartbeat ({{ state.heartbeatIntervalSec }}s)</span>
          </label>
        </div>
      </div>
    </div>

    <!-- 2. 표준 OCPP 라이프사이클 원클릭 시나리오 패널 -->
    <div class="bg-white rounded-xl border border-slate-200 p-4 shadow-2xs">
      <div class="flex items-center justify-between pb-3 border-b border-slate-100 mb-3">
        <h3 class="text-xs font-bold text-slate-900 tracking-tight">OCPP 1.6-J 단계별 표준 시나리오</h3>
        <button
          @click="showCustomModal = true"
          class="text-[11px] text-sky-600 hover:text-sky-800 font-medium cursor-pointer"
        >
          + 커스텀 전문 직접 발송
        </button>
      </div>

      <div class="space-y-3">
        <!-- Step 1 & 2: 부팅 & 하트비트 -->
        <div class="p-3 bg-slate-50/70 rounded-lg border border-slate-100 space-y-2">
          <div class="flex items-center justify-between">
            <span class="text-xs font-semibold text-slate-700">1. 부팅 & 생존 신호</span>
            <span class="text-[10px] text-slate-400">BootNotification & Heartbeat</span>
          </div>
          <div class="flex items-center gap-2">
            <button
              @click="sendBootNotification()"
              :disabled="!state.connected"
              class="px-3 py-1.5 bg-white hover:bg-slate-50 border border-slate-200 text-slate-700 rounded-lg text-xs font-medium transition-all shadow-2xs cursor-pointer disabled:opacity-40"
            >
              🚀 BootNotification 전송
            </button>
            <button
              @click="sendHeartbeat()"
              :disabled="!state.connected"
              class="px-3 py-1.5 bg-white hover:bg-slate-50 border border-slate-200 text-slate-700 rounded-lg text-xs font-medium transition-all shadow-2xs cursor-pointer disabled:opacity-40"
            >
              💓 Heartbeat 1회 즉시 전송
            </button>
          </div>
        </div>

        <!-- Step 3: 상태 보고 (StatusNotification) -->
        <div class="p-3 bg-slate-50/70 rounded-lg border border-slate-100 space-y-2">
          <div class="flex items-center justify-between">
            <span class="text-xs font-semibold text-slate-700">2. 커넥터 상태 보고 (StatusNotification)</span>
            <span class="text-[10px] text-slate-400">Connector #1</span>
          </div>
          <div class="flex flex-wrap items-center gap-1.5">
            <button
              v-for="st in ['Available', 'Preparing', 'Charging', 'SuspendedEVSE', 'Finishing', 'Faulted', 'Unavailable']"
              :key="st"
              @click="sendStatusNotification(st)"
              :disabled="!state.connected"
              class="px-2.5 py-1 text-xs rounded-md border transition-all cursor-pointer disabled:opacity-40"
              :class="state.status === st ? 'bg-sky-50 border-sky-300 text-sky-700 font-bold' : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'"
            >
              {{ st }}
            </button>
          </div>
        </div>

        <!-- Step 4: 회원 인증 (Authorize) -->
        <div class="p-3 bg-slate-50/70 rounded-lg border border-slate-100 space-y-2">
          <div class="flex items-center justify-between">
            <span class="text-xs font-semibold text-slate-700">3. RFID 회원 인증 (Authorize)</span>
            <span class="text-[10px] text-slate-400">DB/L1/L2 캐시 검증</span>
          </div>
          <div class="flex items-center gap-2">
            <input
              v-model="rfidInput"
              placeholder="예: RFID-10001"
              class="w-48 bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 text-xs font-mono text-slate-800 focus:outline-hidden focus:ring-1 focus:ring-sky-500"
            />
            <button
              @click="sendAuthorize(rfidInput)"
              :disabled="!state.connected"
              class="px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-medium transition-colors shadow-2xs cursor-pointer disabled:opacity-40"
            >
              💳 Authorize 요청
            </button>
          </div>
        </div>

        <!-- Step 5 & 6 & 7: 충전 거래 (Start / MeterValues / Stop) -->
        <div class="p-3 bg-slate-50/70 rounded-lg border border-slate-100 space-y-2.5">
          <div class="flex items-center justify-between">
            <span class="text-xs font-semibold text-slate-700">4. 충전 거래 세션 (Start ➔ MeterValues ➔ Stop)</span>
            <span class="text-[10px] text-slate-400">Transaction LifeCycle</span>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-3 gap-2">
            <!-- StartTransaction -->
            <div class="bg-white p-2.5 rounded-lg border border-slate-200 space-y-1.5">
              <span class="text-[11px] font-bold text-emerald-700">A. 충전 시작 (Start)</span>
              <button
                @click="sendStartTransaction(rfidInput, meterStartWh)"
                :disabled="!state.connected || !!state.transactionId"
                class="w-full py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-md text-xs font-medium transition-colors shadow-2xs cursor-pointer disabled:opacity-40"
              >
                ⚡ StartTransaction
              </button>
            </div>

            <!-- MeterValues -->
            <div class="bg-white p-2.5 rounded-lg border border-slate-200 space-y-1.5">
              <span class="text-[11px] font-bold text-sky-700">B. 실시간 계측 (Meter)</span>
              <button
                @click="sendMeterValues(500)"
                :disabled="!state.connected"
                class="w-full py-1.5 bg-sky-600 hover:bg-sky-700 text-white rounded-md text-xs font-medium transition-colors shadow-2xs cursor-pointer disabled:opacity-40"
              >
                📈 MeterValues (+500Wh)
              </button>
            </div>

            <!-- StopTransaction -->
            <div class="bg-white p-2.5 rounded-lg border border-slate-200 space-y-1.5">
              <span class="text-[11px] font-bold text-rose-700">C. 충전 마감 (Stop)</span>
              <button
                @click="sendStopTransaction(rfidInput, stopReason)"
                :disabled="!state.connected"
                class="w-full py-1.5 bg-rose-600 hover:bg-rose-700 text-white rounded-md text-xs font-medium transition-colors shadow-2xs cursor-pointer disabled:opacity-40"
              >
                🛑 StopTransaction
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 커스텀 전문 전송 모달 -->
    <div v-if="showCustomModal" class="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
      <div class="bg-white rounded-xl border border-slate-200 shadow-xl max-w-lg w-full p-4 space-y-3">
        <div class="flex items-center justify-between pb-2 border-b border-slate-100">
          <h3 class="text-xs font-bold text-slate-900">커스텀 OCPP 전문 직접 발송</h3>
          <button @click="showCustomModal = false" class="text-slate-400 hover:text-slate-600 cursor-pointer">✕</button>
        </div>

        <div class="space-y-2 text-xs">
          <div>
            <label class="block text-[11px] font-medium text-slate-600 mb-1">Action 명</label>
            <input v-model="customAction" class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 font-mono" />
          </div>
          <div>
            <label class="block text-[11px] font-medium text-slate-600 mb-1">Payload JSON</label>
            <textarea v-model="customJson" rows="6" class="w-full bg-slate-50 border border-slate-200 rounded-lg p-2.5 font-mono text-[11px]"></textarea>
          </div>
        </div>

        <div class="flex items-center justify-end gap-2 pt-2 border-t border-slate-100">
          <button @click="showCustomModal = false" class="px-3 py-1.5 bg-slate-100 text-slate-600 rounded-lg text-xs cursor-pointer">취소</button>
          <button @click="sendCustomPayload" class="px-4 py-1.5 bg-sky-600 hover:bg-sky-700 text-white rounded-lg text-xs font-medium cursor-pointer">전송</button>
        </div>
      </div>
    </div>
  </div>
</template>
