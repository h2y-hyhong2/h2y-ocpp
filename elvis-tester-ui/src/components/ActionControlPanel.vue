<script setup lang="ts">
import { ref, computed } from 'vue'
import { useVirtualCharger } from '../composables/useVirtualCharger'

const props = defineProps<{
  charger: ReturnType<typeof useVirtualCharger>
}>()

const {
  state,
  connect,
  disconnect,
  sendCall,
  sendBootNotification,
  sendHeartbeat,
  sendStatusNotification,
  sendAuthorize,
  sendStartTransaction,
  sendMeterValues,
  sendStopTransaction,
  sendDataTransfer,
  toggleAutoHeartbeat,
  toggleAutoMeterValues,
  runFullChargeScenario,
  abortScenario
} = props.charger

// 3대 서브 탭: 'manual' (수동 전문) | 'scenario' (시나리오 러너) | 'spec' (기기 스펙)
const activeSubTab = ref<'manual' | 'scenario' | 'spec'>('manual')

// 충전소/충전기 표준 식별자 프리셋 (6자리 충전소 + 2자리 충전기)
const presetChargers = [
  { id: '00000001', name: '00000001 (본사 사옥 1호기 - 350kW 급속)' },
  { id: '00000002', name: '00000002 (본사 사옥 2호기 - 100kW 급속)' },
  { id: '00000101', name: '00000101 (물류센터 Hub 1호기 - 200kW)' },
  { id: '00000102', name: '00000102 (물류센터 Hub 2호기 - 200kW)' }
]

const rfidPresets = [
  { tag: 'CARD-CORP-9901', label: '법인회원 (CARD-CORP-9901)' },
  { tag: 'CARD-USER-1001', label: '일반회원 (CARD-USER-1001)' },
  { tag: 'CREDIT-IC-01', label: '현장 비회원 (CREDIT-IC-01)' },
  { tag: 'ROAM-ME-7701', label: '환경부/로밍 (ROAM-ME-7701)' }
]

// ─────────────────────────────────────────────────────────────
// 1. 수동 전문 발송 (Manual Sender) 설정
// ─────────────────────────────────────────────────────────────
const sendMode = ref<'template' | 'raw'>('template')
const selectedAction = ref('StatusNotification')
const manualRfid = ref('CARD-CORP-9901')
const manualMeterWh = ref(100500)
const manualStatus = ref('Available')
const manualStopReason = ref('Local')
const rawPayloadText = ref('{\n  "connectorId": 1,\n  "errorCode": "NoError",\n  "status": "Available"\n}')

// 액션 변경 시 기본 템플릿 JSON 동기화
function onActionChange() {
  switch (selectedAction.value) {
    case 'BootNotification':
      rawPayloadText.value = JSON.stringify({
        chargePointVendor: 'LS E-Link',
        chargePointModel: 'ELVIS-HPC-350K',
        chargePointSerialNumber: `${state.chargeBoxId}-SN`,
        chargeBoxSerialNumber: state.chargeBoxId,
        firmwareVersion: '1.0.0-RELEASE',
        iccid: '8982000000000000001'
      }, null, 2)
      break
    case 'Heartbeat':
      rawPayloadText.value = '{}'
      break
    case 'StatusNotification':
      rawPayloadText.value = JSON.stringify({
        connectorId: state.connectorId,
        errorCode: 'NoError',
        status: manualStatus.value,
        timestamp: new Date().toISOString()
      }, null, 2)
      break
    case 'Authorize':
      rawPayloadText.value = JSON.stringify({
        idTag: manualRfid.value
      }, null, 2)
      break
    case 'StartTransaction':
      rawPayloadText.value = JSON.stringify({
        connectorId: state.connectorId,
        idTag: manualRfid.value,
        meterStart: manualMeterWh.value,
        timestamp: new Date().toISOString()
      }, null, 2)
      break
    case 'MeterValues':
      rawPayloadText.value = JSON.stringify({
        connectorId: state.connectorId,
        transactionId: state.transactionId || 1001,
        meterValue: [
          {
            timestamp: new Date().toISOString(),
            sampledValue: [
              { value: String(state.meterValueWh + 500), context: 'Sample.Periodic', measurand: 'Energy.Active.Import.Register', unit: 'Wh' },
              { value: String(state.soc + 1), context: 'Sample.Periodic', measurand: 'SoC', unit: 'Percent' },
              { value: String(state.voltage), context: 'Sample.Periodic', measurand: 'Voltage', unit: 'V' },
              { value: String(state.current), context: 'Sample.Periodic', measurand: 'Current.Import', unit: 'A' },
              { value: String(state.powerKw), context: 'Sample.Periodic', measurand: 'Power.Active.Import', unit: 'kW' }
            ]
          }
        ]
      }, null, 2)
      break
    case 'StopTransaction':
      rawPayloadText.value = JSON.stringify({
        transactionId: state.transactionId || 1001,
        idTag: manualRfid.value,
        meterStop: state.meterValueWh + 2500,
        timestamp: new Date().toISOString(),
        reason: manualStopReason.value
      }, null, 2)
      break
    case 'DataTransfer':
      rawPayloadText.value = JSON.stringify({
        vendorId: 'LS E-Link',
        messageId: 'MeterValues',
        data: JSON.stringify({
          chargeBoxId: state.chargeBoxId,
          connectorId: state.connectorId,
          soc: state.soc,
          temperature: 32.5
        })
      }, null, 2)
      break
  }
}

function sendManualCall() {
  if (sendMode.value === 'raw') {
    try {
      const parsed = JSON.parse(rawPayloadText.value)
      sendCall(selectedAction.value, parsed)
    } catch (e: any) {
      alert(`JSON 문법 오류: ${e.message}`)
    }
  } else {
    // 템플릿 폼 기반 발송
    switch (selectedAction.value) {
      case 'BootNotification':
        sendBootNotification()
        break
      case 'Heartbeat':
        sendHeartbeat()
        break
      case 'StatusNotification':
        sendStatusNotification(manualStatus.value)
        break
      case 'Authorize':
        sendAuthorize(manualRfid.value)
        break
      case 'StartTransaction':
        sendStartTransaction(manualRfid.value, manualMeterWh.value)
        break
      case 'MeterValues':
        sendMeterValues(500)
        break
      case 'StopTransaction':
        sendStopTransaction(manualRfid.value, manualStopReason.value)
        break
      case 'DataTransfer':
        sendDataTransfer('LS E-Link', 'CustomData', { chargeBoxId: state.chargeBoxId, ts: Date.now() })
        break
    }
  }
}

// ─────────────────────────────────────────────────────────────
// 2. 시나리오 러너 설정
// ─────────────────────────────────────────────────────────────
const scenarioRfid = ref('CARD-CORP-9901')

function onPresetChange(event: Event) {
  const target = event.target as HTMLSelectElement
  state.chargeBoxId = target.value
}
</script>

<template>
  <div class="space-y-4 select-none">
    <!-- 1. 충전 인프라 접속 및 식별자 제어 카드 -->
    <div class="bg-white rounded-xl border border-slate-200 p-4 shadow-2xs">
      <div class="flex items-center justify-between pb-3 border-b border-slate-100 mb-3">
        <div class="flex items-center gap-2">
          <div class="w-6 h-6 rounded-md bg-sky-50 text-sky-600 flex items-center justify-center border border-sky-100">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4" />
            </svg>
          </div>
          <h2 class="text-xs font-bold text-slate-900 tracking-tight">가상 충전기(Virtual EVSE) 단말 및 게이트웨이 접속 제어</h2>
        </div>
        <div class="flex items-center gap-1.5">
          <span
            class="text-[10px] font-mono font-bold px-2.5 py-0.5 rounded-full border"
            :class="state.connected ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : state.connecting ? 'bg-amber-50 text-amber-700 border-amber-200' : 'bg-slate-100 text-slate-500 border-slate-200'"
          >
            {{ state.connected ? 'ONLINE (:18080)' : state.connecting ? 'CONNECTING...' : 'OFFLINE' }}
          </span>
        </div>
      </div>

      <!-- 식별자 및 게이트웨이 URL 입력 그리드 -->
      <div class="grid grid-cols-1 md:grid-cols-12 gap-3 text-xs">
        <!-- 충전기 식별자 (ChargeBoxId: 8자리 정규화) -->
        <div class="md:col-span-5">
          <label class="block text-[11px] font-semibold text-slate-700 mb-1">충전기 식별자 (ChargeBoxId)</label>
          <div class="flex items-center gap-1.5">
            <select
              :value="presetChargers.some(p => p.id === state.chargeBoxId) ? state.chargeBoxId : 'custom'"
              @change="onPresetChange"
              :disabled="state.connected"
              class="w-36 bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 text-xs text-slate-800 focus:outline-hidden focus:ring-1 focus:ring-sky-500 disabled:opacity-60"
            >
              <option v-for="cp in presetChargers" :key="cp.id" :value="cp.id">{{ cp.id }}</option>
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

        <!-- 커넥터 ID 선택 -->
        <div class="md:col-span-2">
          <label class="block text-[11px] font-semibold text-slate-700 mb-1">커넥터 번호</label>
          <select
            v-model.number="state.connectorId"
            :disabled="state.connected"
            class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 text-xs font-mono text-slate-800 focus:outline-hidden focus:ring-1 focus:ring-sky-500 disabled:opacity-60"
          >
            <option :value="1">Connector #1</option>
            <option :value="2">Connector #2</option>
          </select>
        </div>

        <!-- CSMS WebSocket 게이트웨이 및 연결 버튼 -->
        <div class="md:col-span-5">
          <label class="block text-[11px] font-semibold text-slate-700 mb-1">CSMS WebSocket 게이트웨이</label>
          <div class="flex items-center gap-2">
            <input
              v-model="state.wsUrl"
              :disabled="state.connected"
              placeholder="ws://127.0.0.1:18080/ocpp"
              class="flex-1 bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 text-xs font-mono text-slate-800 focus:outline-hidden focus:ring-1 focus:ring-sky-500 disabled:opacity-60"
            />
            <button
              v-if="!state.connected"
              @click="connect"
              :disabled="state.connecting"
              class="px-4 py-1.5 bg-sky-600 hover:bg-sky-700 text-white rounded-lg text-xs font-bold transition-all shadow-2xs cursor-pointer disabled:opacity-50 flex items-center gap-1.5 flex-shrink-0"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
              <span>접속</span>
            </button>
            <button
              v-else
              @click="disconnect"
              class="px-3.5 py-1.5 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-xs font-bold transition-all shadow-2xs cursor-pointer flex items-center gap-1.5 flex-shrink-0"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
              <span>연결 해제</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 단말 실시간 상태 요약 바 -->
      <div class="mt-3 pt-3 border-t border-slate-100 flex flex-wrap items-center justify-between gap-3 text-xs">
        <div class="flex items-center flex-wrap gap-2.5">
          <div class="flex items-center gap-1.5 bg-slate-50 px-2 py-1 rounded-md border border-slate-200">
            <span class="text-slate-500 text-[11px]">상태:</span>
            <span
              class="font-bold font-mono px-1.5 py-0.2 rounded text-[11px]"
              :class="{
                'bg-emerald-100 text-emerald-800': state.status === 'Available',
                'bg-amber-100 text-amber-800': state.status === 'Preparing',
                'bg-sky-100 text-sky-800': state.status === 'Charging',
                'bg-purple-100 text-purple-800': state.status === 'Finishing',
                'bg-rose-100 text-rose-800': state.status === 'Faulted'
              }"
            >
              {{ state.status }}
            </span>
          </div>

          <div class="flex items-center gap-1.5 bg-slate-50 px-2 py-1 rounded-md border border-slate-200">
            <span class="text-slate-500 text-[11px]">세션 ID:</span>
            <strong class="font-mono text-indigo-700">{{ state.transactionId || 'None' }}</strong>
          </div>

          <div class="flex items-center gap-1.5 bg-slate-50 px-2 py-1 rounded-md border border-slate-200">
            <span class="text-slate-500 text-[11px]">누적량:</span>
            <strong class="font-mono text-slate-800">{{ (state.meterValueWh / 1000).toFixed(2) }} kWh</strong>
          </div>

          <div class="flex items-center gap-1.5 bg-slate-50 px-2 py-1 rounded-md border border-slate-200">
            <span class="text-slate-500 text-[11px]">배터리 SoC:</span>
            <strong class="font-mono text-emerald-600">{{ state.soc }}%</strong>
          </div>

          <div class="hidden sm:flex items-center gap-1.5 bg-slate-50 px-2 py-1 rounded-md border border-slate-200">
            <span class="text-slate-500 text-[11px]">출력:</span>
            <strong class="font-mono text-slate-700">{{ state.powerKw.toFixed(1) }} kW ({{ state.voltage }}V / {{ state.current }}A)</strong>
          </div>
        </div>

        <!-- 자동 Heartbeat 토글 -->
        <label class="flex items-center gap-1.5 cursor-pointer text-slate-700 select-none text-xs">
          <input
            type="checkbox"
            :checked="state.autoHeartbeat"
            @change="toggleAutoHeartbeat(($event.target as HTMLInputElement).checked)"
            class="rounded border-slate-300 text-sky-600 focus:ring-sky-500 cursor-pointer"
          />
          <span class="font-medium">자동 Heartbeat ({{ state.heartbeatIntervalSec }}s)</span>
        </label>
      </div>
    </div>

    <!-- 2. 중앙 메인 서브 탭 (수동 전문 발송 / 시나리오 러너 / 기기 스펙) -->
    <div class="bg-white rounded-xl border border-slate-200 p-4 shadow-2xs space-y-4">
      <!-- 서브 탭 헤더 -->
      <div class="flex items-center justify-between pb-3 border-b border-slate-100">
        <div class="flex items-center gap-1 bg-slate-100 p-1 rounded-lg border border-slate-200 text-xs">
          <button
            @click="activeSubTab = 'manual'"
            class="px-3 py-1.5 rounded-md font-bold transition-all cursor-pointer flex items-center gap-1.5"
            :class="activeSubTab === 'manual' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600 hover:text-slate-900'"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
            </svg>
            <span>수동 전문 발송 (Manual Sender)</span>
          </button>
          <button
            @click="activeSubTab = 'scenario'"
            class="px-3 py-1.5 rounded-md font-bold transition-all cursor-pointer flex items-center gap-1.5"
            :class="activeSubTab === 'scenario' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600 hover:text-slate-900'"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
              <path stroke-linecap="round" stroke-linejoin="round" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>원클릭 시나리오 러너 (Scenario Runner)</span>
          </button>
          <button
            @click="activeSubTab = 'spec'"
            class="px-3 py-1.5 rounded-md font-bold transition-all cursor-pointer flex items-center gap-1.5"
            :class="activeSubTab === 'spec' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600 hover:text-slate-900'"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <span>충전기 기기 사양 (Profile)</span>
          </button>
        </div>

        <span class="text-[11px] text-slate-400 font-mono hidden md:inline-block">
          OCPP 1.6-J JSON over WebSocket
        </span>
      </div>

      <!-- ─── 서브탭 1: 수동 전문 발송 (Manual Sender) ─── -->
      <div v-if="activeSubTab === 'manual'" class="space-y-4">
        <!-- 상단 옵션: 모드 선택(템플릿 / Raw JSON) 및 액션 선택 -->
        <div class="flex flex-wrap items-center justify-between gap-3 text-xs">
          <div class="flex items-center gap-2">
            <label class="font-semibold text-slate-700">대상 액션 (OCPP Action):</label>
            <select
              v-model="selectedAction"
              @change="onActionChange"
              class="bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 text-xs font-mono font-bold text-slate-800 focus:outline-hidden focus:ring-1 focus:ring-sky-500"
            >
              <option value="BootNotification">BootNotification</option>
              <option value="Heartbeat">Heartbeat</option>
              <option value="StatusNotification">StatusNotification</option>
              <option value="Authorize">Authorize</option>
              <option value="StartTransaction">StartTransaction</option>
              <option value="MeterValues">MeterValues</option>
              <option value="StopTransaction">StopTransaction</option>
              <option value="DataTransfer">DataTransfer</option>
            </select>
          </div>

          <!-- 모드 전환 (템플릿 폼 vs Raw JSON) -->
          <div class="flex items-center bg-slate-100 p-0.5 rounded-lg border border-slate-200 text-xs">
            <button
              @click="sendMode = 'template'"
              class="px-2.5 py-1 rounded-md transition-all cursor-pointer font-medium"
              :class="sendMode === 'template' ? 'bg-white text-slate-900 shadow-2xs font-bold' : 'text-slate-600 hover:text-slate-900'"
            >
              템플릿 폼 모드
            </button>
            <button
              @click="sendMode = 'raw'; onActionChange()"
              class="px-2.5 py-1 rounded-md transition-all cursor-pointer font-medium"
              :class="sendMode === 'raw' ? 'bg-white text-slate-900 shadow-2xs font-bold' : 'text-slate-600 hover:text-slate-900'"
            >
              Raw JSON 모드
            </button>
          </div>
        </div>

        <!-- 템플릿 폼 모드 뷰 -->
        <div v-if="sendMode === 'template'" class="p-3.5 bg-slate-50/70 rounded-xl border border-slate-100 space-y-3">
          <!-- 1. BootNotification -->
          <div v-if="selectedAction === 'BootNotification'" class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
            <div>
              <label class="block text-[11px] font-medium text-slate-600 mb-1">제조사 (Vendor)</label>
              <input value="LS E-Link" disabled class="w-full bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 font-mono" />
            </div>
            <div>
              <label class="block text-[11px] font-medium text-slate-600 mb-1">모델명 (Model)</label>
              <input value="ELVIS-HPC-350K" disabled class="w-full bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 font-mono" />
            </div>
          </div>

          <!-- 2. Heartbeat -->
          <div v-else-if="selectedAction === 'Heartbeat'" class="text-xs text-slate-600 py-2">
            빈 페이로드({})로 즉시 CSMS에 생존 신호를 발송하고 응답 시간(currentTime)을 동기화합니다.
          </div>

          <!-- 3. StatusNotification -->
          <div v-else-if="selectedAction === 'StatusNotification'" class="space-y-2 text-xs">
            <label class="block text-[11px] font-medium text-slate-600">변경할 커넥터 상태 (Status)</label>
            <div class="flex flex-wrap items-center gap-1.5">
              <button
                v-for="st in ['Available', 'Preparing', 'Charging', 'SuspendedEVSE', 'Finishing', 'Faulted', 'Unavailable']"
                :key="st"
                @click="manualStatus = st"
                class="px-3 py-1.5 rounded-lg border text-xs font-medium transition-all cursor-pointer"
                :class="manualStatus === st ? 'bg-sky-50 border-sky-300 text-sky-700 font-bold shadow-2xs' : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'"
              >
                {{ st }}
              </button>
            </div>
          </div>

          <!-- 4. Authorize -->
          <div v-else-if="selectedAction === 'Authorize'" class="space-y-2 text-xs">
            <label class="block text-[11px] font-medium text-slate-600">인증 RFID 카드 ID</label>
            <div class="flex items-center gap-2">
              <select
                v-model="manualRfid"
                class="w-56 bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 font-mono text-xs text-slate-800"
              >
                <option v-for="r in rfidPresets" :key="r.tag" :value="r.tag">{{ r.label }}</option>
              </select>
              <input
                v-model="manualRfid"
                placeholder="직접입력 (예: CARD-CORP-9901)"
                class="flex-1 bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 font-mono text-xs"
              />
            </div>
          </div>

          <!-- 5. StartTransaction -->
          <div v-else-if="selectedAction === 'StartTransaction'" class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
            <div>
              <label class="block text-[11px] font-medium text-slate-600 mb-1">회원 카드 ID (idTag)</label>
              <input v-model="manualRfid" class="w-full bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 font-mono" />
            </div>
            <div>
              <label class="block text-[11px] font-medium text-slate-600 mb-1">시작 전력량 (meterStart: Wh)</label>
              <input v-model.number="manualMeterWh" type="number" class="w-full bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 font-mono" />
            </div>
          </div>

          <!-- 6. MeterValues -->
          <div v-else-if="selectedAction === 'MeterValues'" class="grid grid-cols-2 sm:grid-cols-4 gap-2.5 text-xs">
            <div>
              <label class="block text-[11px] font-medium text-slate-600 mb-1">누적량 (Wh)</label>
              <input :value="state.meterValueWh" disabled class="w-full bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 font-mono text-xs" />
            </div>
            <div>
              <label class="block text-[11px] font-medium text-slate-600 mb-1">SoC (%)</label>
              <input :value="state.soc" disabled class="w-full bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 font-mono text-xs" />
            </div>
            <div>
              <label class="block text-[11px] font-medium text-slate-600 mb-1">전압 (V)</label>
              <input :value="state.voltage" disabled class="w-full bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 font-mono text-xs" />
            </div>
            <div>
              <label class="block text-[11px] font-medium text-slate-600 mb-1">전류 (A)</label>
              <input :value="state.current" disabled class="w-full bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 font-mono text-xs" />
            </div>
          </div>

          <!-- 7. StopTransaction -->
          <div v-else-if="selectedAction === 'StopTransaction'" class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
            <div>
              <label class="block text-[11px] font-medium text-slate-600 mb-1">마감 세션 (transactionId)</label>
              <input :value="state.transactionId || '미진행'" disabled class="w-full bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 font-mono text-xs" />
            </div>
            <div>
              <label class="block text-[11px] font-medium text-slate-600 mb-1">종료 사유 (Reason)</label>
              <select v-model="manualStopReason" class="w-full bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 font-mono text-xs">
                <option value="Local">Local (사용자 정상 종료)</option>
                <option value="Remote">Remote (CSMS 원격 정지)</option>
                <option value="EVDisconnected">EVDisconnected (차량 분리)</option>
                <option value="EmergencyStop">EmergencyStop (비상정지)</option>
                <option value="Other">Other</option>
              </select>
            </div>
          </div>

          <!-- 8. DataTransfer -->
          <div v-else-if="selectedAction === 'DataTransfer'" class="text-xs text-slate-600 py-2">
            LS E-Link 독자 확장 전문(DataTransfer)을 전송하여 관제 서버와의 비표준 데이터 동기화를 검증합니다.
          </div>
        </div>

        <!-- Raw JSON 모드 뷰 -->
        <div v-else class="space-y-1.5">
          <label class="block text-[11px] font-semibold text-slate-700">전송 Payload JSON 편집기</label>
          <textarea
            v-model="rawPayloadText"
            rows="6"
            class="w-full bg-slate-900 text-sky-300 border border-slate-800 rounded-xl p-3 font-mono text-xs leading-relaxed focus:outline-hidden focus:ring-1 focus:ring-sky-500 selection:bg-sky-500/30"
          ></textarea>
        </div>

        <!-- 하단 발송 액션 버튼 -->
        <div class="flex items-center justify-between pt-2 border-t border-slate-100">
          <span class="text-xs text-slate-500">
            상태: <strong :class="state.connected ? 'text-emerald-700' : 'text-slate-400'">{{ state.connected ? '전송 가능' : '접속 필요' }}</strong>
          </span>

          <button
            @click="sendManualCall"
            :disabled="!state.connected"
            class="px-5 py-2 bg-gradient-to-r from-sky-600 to-indigo-600 hover:from-sky-700 hover:to-indigo-700 text-white rounded-lg text-xs font-bold transition-all shadow-2xs cursor-pointer disabled:opacity-40 flex items-center gap-1.5"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
            </svg>
            <span>{{ selectedAction }} 전문 전송 (Call)</span>
          </button>
        </div>
      </div>

      <!-- ─── 서브탭 2: 원클릭 시나리오 러너 (Scenario Runner) ─── -->
      <div v-else-if="activeSubTab === 'scenario'" class="space-y-4">
        <!-- A. 정상 충전 라이프사이클 완주 시뮬레이션 카드 -->
        <div class="p-4 bg-slate-50/80 rounded-xl border border-slate-200 space-y-3">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <div class="w-6 h-6 rounded-md bg-emerald-50 text-emerald-600 flex items-center justify-center border border-emerald-100">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div>
                <h4 class="text-xs font-bold text-slate-900">E2E 충전 라이프사이클 원클릭 완주 시나리오</h4>
                <p class="text-[11px] text-slate-500">부팅 ➔ 차량결착 ➔ 카드인증 ➔ 충전시작 ➔ 계측전송(3회) ➔ 마감정산 ➔ 대기복귀</p>
              </div>
            </div>

            <!-- 시나리오 시작 / 중단 버튼 -->
            <div class="flex items-center gap-2">
              <button
                v-if="!state.scenarioRunning"
                @click="runFullChargeScenario(scenarioRfid)"
                :disabled="!state.connected"
                class="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold transition-all shadow-2xs cursor-pointer disabled:opacity-40 flex items-center gap-1.5"
              >
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span>시나리오 자동 완주 시작</span>
              </button>
              <button
                v-else
                @click="abortScenario"
                class="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-xs font-bold transition-all shadow-2xs cursor-pointer flex items-center gap-1.5"
              >
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 10a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1v-4z" />
                </svg>
                <span>시나리오 중단</span>
              </button>
            </div>
          </div>

          <!-- 시나리오 옵션: 회원 카드 프리셋 -->
          <div class="flex items-center gap-2 text-xs pt-1">
            <span class="text-slate-600 font-medium">테스트 회원 카드:</span>
            <select
              v-model="scenarioRfid"
              :disabled="state.scenarioRunning"
              class="bg-white border border-slate-200 rounded-lg px-2.5 py-1 text-xs font-mono font-bold text-slate-800"
            >
              <option v-for="r in rfidPresets" :key="r.tag" :value="r.tag">{{ r.label }}</option>
            </select>
          </div>

          <!-- 시나리오 실행 프로그레스 바 -->
          <div v-if="state.scenarioRunning || state.scenarioStep" class="p-3 bg-white rounded-lg border border-slate-200 space-y-2">
            <div class="flex items-center justify-between text-xs">
              <span class="font-bold text-indigo-700">{{ state.scenarioStep }}</span>
              <span class="font-mono text-slate-500 font-bold">{{ state.scenarioProgress }}%</span>
            </div>
            <div class="w-full bg-slate-100 rounded-full h-2 overflow-hidden border border-slate-200">
              <div
                class="bg-gradient-to-r from-sky-500 to-indigo-600 h-2 rounded-full transition-all duration-300"
                :style="{ width: `${state.scenarioProgress}%` }"
              ></div>
            </div>
          </div>
        </div>

        <!-- B. 실시간 자동 계측기 (Auto MeterValues) 카드 -->
        <div class="p-4 bg-slate-50/80 rounded-xl border border-slate-200 space-y-2.5">
          <div class="flex items-center justify-between">
            <div>
              <h4 class="text-xs font-bold text-slate-900">충전 중 실시간 자동 계측기 (Auto MeterValues)</h4>
              <p class="text-[11px] text-slate-500">거래 세션 활성 시 설정 주기마다 +500Wh 누적량 및 SoC를 실시간 자동 전송합니다.</p>
            </div>
            <div class="flex items-center gap-2">
              <select
                :value="state.autoMeterIntervalSec"
                @change="toggleAutoMeterValues(state.autoMeterValues, Number(($event.target as HTMLSelectElement).value))"
                class="bg-white border border-slate-200 rounded-lg px-2 py-1 text-xs font-mono"
              >
                <option :value="3">3초 주기</option>
                <option :value="5">5초 주기</option>
                <option :value="10">10초 주기</option>
              </select>
              <label class="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  :checked="state.autoMeterValues"
                  @change="toggleAutoMeterValues(($event.target as HTMLInputElement).checked, state.autoMeterIntervalSec)"
                  class="sr-only peer"
                />
                <div class="w-9 h-5 bg-slate-300 peer-focus:outline-hidden rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-sky-600"></div>
              </label>
            </div>
          </div>
        </div>

        <!-- C. 예외 및 이상 시나리오 원클릭 검증 -->
        <div class="p-4 bg-slate-50/80 rounded-xl border border-slate-200 space-y-2.5">
          <h4 class="text-xs font-bold text-slate-900">예외 및 이상 시나리오 즉시 검증</h4>
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-2 text-xs">
            <!-- 비인가 카드 -->
            <button
              @click="sendAuthorize('INVALID-CARD-999')"
              :disabled="!state.connected"
              class="p-2.5 bg-white hover:bg-slate-50 border border-slate-200 rounded-lg text-left transition-all shadow-2xs cursor-pointer disabled:opacity-40"
            >
              <div class="font-bold text-slate-800">비인가 카드 인증 거절</div>
              <div class="text-[10px] text-slate-500">INVALID-CARD-999 (Blocked/Invalid)</div>
            </button>

            <!-- 커넥터 결함 -->
            <button
              @click="sendStatusNotification('Faulted', 'GroundFailure')"
              :disabled="!state.connected"
              class="p-2.5 bg-white hover:bg-slate-50 border border-slate-200 rounded-lg text-left transition-all shadow-2xs cursor-pointer disabled:opacity-40"
            >
              <div class="font-bold text-rose-700">커넥터 장애 보고 (Faulted)</div>
              <div class="text-[10px] text-slate-500">ErrorCode: GroundFailure 전송</div>
            </button>

            <!-- 비상 정지 -->
            <button
              @click="sendStopTransaction(scenarioRfid, 'EmergencyStop')"
              :disabled="!state.connected"
              class="p-2.5 bg-white hover:bg-slate-50 border border-slate-200 rounded-lg text-left transition-all shadow-2xs cursor-pointer disabled:opacity-40"
            >
              <div class="font-bold text-amber-700">비상정지 버튼 (EmergencyStop)</div>
              <div class="text-[10px] text-slate-500">StopTransaction 즉시 강제 마감</div>
            </button>
          </div>
        </div>
      </div>

      <!-- ─── 서브탭 3: 충전기 기기 사양 (Profile) ─── -->
      <div v-else class="space-y-3 text-xs">
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div class="bg-slate-50 p-3 rounded-lg border border-slate-200 space-y-1">
            <span class="text-[11px] text-slate-500">제조사 (Vendor)</span>
            <div class="font-mono font-bold text-slate-800">LS E-Link (엘에스이링크)</div>
          </div>
          <div class="bg-slate-50 p-3 rounded-lg border border-slate-200 space-y-1">
            <span class="text-[11px] text-slate-500">충전기 모델 (Model)</span>
            <div class="font-mono font-bold text-slate-800">ELVIS-HPC-350K (초급속 350kW DC)</div>
          </div>
          <div class="bg-slate-50 p-3 rounded-lg border border-slate-200 space-y-1">
            <span class="text-[11px] text-slate-500">펌웨어 버전 (Firmware)</span>
            <div class="font-mono font-bold text-slate-800">v1.0.0-RELEASE (OCPP 1.6-J)</div>
          </div>
          <div class="bg-slate-50 p-3 rounded-lg border border-slate-200 space-y-1">
            <span class="text-[11px] text-slate-500">지원 커넥터 수량</span>
            <div class="font-mono font-bold text-slate-800">2 Guns (CCS1 Combo 듀얼)</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
