<script setup lang="ts">
import { ref, computed } from 'vue'
import { useVirtualCharger } from '../composables/useVirtualCharger'
import type { ChargerInstance } from '../types/ocpp'

const props = defineProps<{
  charger?: ReturnType<typeof useVirtualCharger>
  chargerId?: string
}>()

const emit = defineEmits<{
  (e: 'open-edit-modal', charger: ChargerInstance): void
}>()

const store = props.charger || useVirtualCharger()

const targetCharger = computed(() => {
  if (props.chargerId) {
    return store.chargers.find(c => c.id === props.chargerId) || store.currentCharger.value
  }
  return store.currentCharger.value
})

// 3대 서브 탭: 'manual' (수동 전문) | 'scenario' (시나리오 러너) | 'spec' (기기 스펙)
const activeSubTab = ref<'manual' | 'scenario' | 'spec'>('manual')

const rfidPresets = [
  { tag: 'CARD-CORP-9901', label: '법인회원 (CARD-CORP-9901)', type: 'C' },
  { tag: 'CARD-USER-1001', label: '일반회원 (CARD-USER-1001)', type: 'M' },
  { tag: 'CREDIT-IC-01', label: '현장 비회원 (CREDIT-IC-01)', type: 'N' },
  { tag: 'ROAM-ME-7701', label: '환경부/로밍 (ROAM-ME-7701)', type: 'K' }
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
  const c = targetCharger.value
  switch (selectedAction.value) {
    case 'BootNotification':
      rawPayloadText.value = JSON.stringify({
        chargePointVendor: c.vendor,
        chargePointModel: c.model,
        chargePointSerialNumber: `${c.id}-SN`,
        chargeBoxSerialNumber: c.id,
        firmwareVersion: '1.0.0-RELEASE',
        iccid: '8982000000000000001'
      }, null, 2)
      break
    case 'Heartbeat':
      rawPayloadText.value = '{}'
      break
    case 'StatusNotification':
      rawPayloadText.value = JSON.stringify({
        connectorId: c.activeConnectorId || 1,
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
        connectorId: c.activeConnectorId || 1,
        idTag: manualRfid.value,
        meterStart: manualMeterWh.value,
        timestamp: new Date().toISOString()
      }, null, 2)
      break
    case 'MeterValues':
      rawPayloadText.value = JSON.stringify({
        connectorId: c.activeConnectorId || 1,
        transactionId: c.transactionId || 1001,
        meterValue: [
          {
            timestamp: new Date().toISOString(),
            sampledValue: [
              { value: String(c.meterValueWh), measurand: 'Energy.Active.Import.Register', unit: 'Wh' },
              { value: String(c.soc), measurand: 'SoC', unit: 'Percent' },
              { value: String(c.powerKw * 1000), measurand: 'Power.Active.Import', unit: 'W' }
            ]
          }
        ]
      }, null, 2)
      break
    case 'StopTransaction':
      rawPayloadText.value = JSON.stringify({
        idTag: manualRfid.value,
        meterStop: manualMeterWh.value + 1500,
        timestamp: new Date().toISOString(),
        transactionId: c.transactionId || 1001,
        reason: manualStopReason.value
      }, null, 2)
      break
    case 'DataTransfer':
      rawPayloadText.value = JSON.stringify({
        vendorId: 'LS E-Link',
        messageId: 'CustomDiagnostics',
        data: JSON.stringify({ chargeBoxId: c.id, time: Date.now() })
      }, null, 2)
      break
  }
}

function sendManualCall() {
  const cid = targetCharger.value.id
  if (sendMode.value === 'raw') {
    try {
      const parsed = JSON.parse(rawPayloadText.value)
      store.sendCall(selectedAction.value, parsed, cid)
    } catch (e: any) {
      alert(`JSON 문법 오류: ${e.message}`)
    }
  } else {
    // 템플릿 폼 기반 발송
    switch (selectedAction.value) {
      case 'BootNotification':
        store.sendBootNotification(targetCharger.value.vendor, targetCharger.value.model, '1.0.0', cid)
        break
      case 'Heartbeat':
        store.sendHeartbeat(cid)
        break
      case 'StatusNotification':
        store.sendStatusNotification(manualStatus.value, 'NoError', cid)
        break
      case 'Authorize':
        store.sendAuthorize(manualRfid.value, cid)
        break
      case 'StartTransaction':
        store.sendStartTransaction(manualRfid.value, manualMeterWh.value, cid)
        break
      case 'MeterValues':
        store.sendMeterValues(500, cid)
        break
      case 'StopTransaction':
        store.sendStopTransaction(manualRfid.value, manualStopReason.value, cid)
        break
      case 'DataTransfer':
        store.sendDataTransfer('LS E-Link', 'CustomData', JSON.stringify({ chargeBoxId: cid, ts: Date.now() }), cid)
        break
    }
  }
}

// ─────────────────────────────────────────────────────────────
// 2. 시나리오 러너 설정
// ─────────────────────────────────────────────────────────────
const scenarioUserType = ref<'C' | 'M' | 'N' | 'K'>('C')
const scenarioRfid = ref('CARD-CORP-9901')

function onUserTypeChange() {
  const found = rfidPresets.find(p => p.type === scenarioUserType.value)
  if (found) {
    scenarioRfid.value = found.tag
  }
}

function handleRunScenario() {
  store.runFullChargeScenario(scenarioUserType.value, scenarioRfid.value, targetCharger.value.id)
}

function handleAbortScenario() {
  store.abortScenario(targetCharger.value.id)
}
</script>

<template>
  <div class="h-full flex flex-col overflow-y-auto space-y-3 select-none p-1 font-sans">
    <!-- 1. 선택된 충전기 상태 및 빠른 제어 바 -->
    <div class="bg-white rounded-xl border border-slate-200 p-3.5 shadow-2xs flex-shrink-0">
      <div class="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-slate-100">
        <!-- 충전기 식별자 및 타이틀 -->
        <div class="flex items-center gap-3">
          <div class="w-9 h-9 rounded-lg bg-sky-50 text-sky-600 border border-sky-100 flex items-center justify-center font-bold text-sm shadow-2xs">
            ⚡
          </div>
          <div class="flex flex-col">
            <div class="flex items-center gap-2">
              <span class="text-base font-black font-mono text-slate-900 tracking-tight">
                {{ targetCharger.id }}
              </span>
              <span class="text-[10px] px-1.5 py-0.5 rounded-full font-bold bg-sky-100 text-sky-800 border border-sky-200">
                {{ targetCharger.powerKw }}kW 급속
              </span>
              <button
                @click="emit('open-edit-modal', targetCharger)"
                class="text-[11px] text-slate-400 hover:text-slate-700 hover:bg-slate-100 px-1 rounded transition-colors"
                title="충전기 설정 수정"
              >
                ✏️ 수정
              </button>
            </div>
            <span class="text-xs text-slate-500 font-medium truncate max-w-sm">
              {{ targetCharger.name }}
            </span>
          </div>
        </div>

        <!-- 우측 연결 상태 및 연결/해제 토글 -->
        <div class="flex items-center gap-2">
          <!-- 연결 상태 배지 -->
          <div class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg border text-xs font-mono bg-slate-50 border-slate-200">
            <span
              class="w-2.5 h-2.5 rounded-full"
              :class="targetCharger.connected ? 'bg-emerald-500 ring-2 ring-emerald-100 animate-pulse' : targetCharger.connecting ? 'bg-amber-400 animate-ping' : 'bg-slate-300'"
            ></span>
            <span class="font-bold text-[11px]" :class="targetCharger.connected ? 'text-emerald-700' : 'text-slate-500'">
              {{ targetCharger.connected ? 'ONLINE (:18080)' : targetCharger.connecting ? 'CONNECTING...' : 'OFFLINE' }}
            </span>
          </div>

          <!-- 연결/해제 토글 버튼 -->
          <button
            @click="targetCharger.connected ? store.disconnect(targetCharger.id) : store.connect(targetCharger.id)"
            class="px-3 py-1.5 text-xs font-bold rounded-lg transition-all shadow-2xs cursor-pointer flex items-center gap-1.5"
            :class="targetCharger.connected ? 'bg-rose-50 text-rose-700 border border-rose-300 hover:bg-rose-100' : 'bg-emerald-600 text-white hover:bg-emerald-700'"
          >
            <span v-if="targetCharger.connected">🔌 연결 해제</span>
            <span v-else>⚡ 즉시 연결</span>
          </button>
        </div>
      </div>

      <!-- 하단 실시간 상태 지표 그리드 -->
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-3">
        <div class="bg-slate-50/80 border border-slate-200/80 rounded-lg p-2 flex flex-col">
          <span class="text-[10px] text-slate-400 uppercase font-mono">OCPP 상태</span>
          <span class="text-xs font-bold mt-0.5" :class="targetCharger.status === 'Charging' ? 'text-indigo-600 font-black' : 'text-slate-800'">
            {{ targetCharger.status }}
          </span>
        </div>

        <div class="bg-slate-50/80 border border-slate-200/80 rounded-lg p-2 flex flex-col">
          <span class="text-[10px] text-slate-400 uppercase font-mono">트랜잭션 ID</span>
          <span class="text-xs font-mono font-bold text-slate-800 mt-0.5">
            {{ targetCharger.transactionId || '세션 없음' }}
          </span>
        </div>

        <div class="bg-slate-50/80 border border-slate-200/80 rounded-lg p-2 flex flex-col">
          <span class="text-[10px] text-slate-400 uppercase font-mono">누적 계량 (Wh)</span>
          <span class="text-xs font-mono font-bold text-slate-800 mt-0.5">
            {{ targetCharger.meterValueWh.toLocaleString() }} Wh
          </span>
        </div>

        <div class="bg-slate-50/80 border border-slate-200/80 rounded-lg p-2 flex flex-col">
          <span class="text-[10px] text-slate-400 uppercase font-mono">배터리 SoC</span>
          <span class="text-xs font-mono font-bold text-sky-700 mt-0.5">
            {{ targetCharger.soc }} %
          </span>
        </div>
      </div>
    </div>

    <!-- 2. 서브 탭 네비게이션 -->
    <div class="bg-white rounded-xl border border-slate-200 p-2 shadow-2xs flex-shrink-0">
      <div class="flex items-center gap-1">
        <button
          @click="activeSubTab = 'manual'"
          class="flex-1 py-1.5 text-xs font-bold rounded-lg transition-all cursor-pointer flex items-center justify-center gap-1.5"
          :class="activeSubTab === 'manual' ? 'bg-sky-50 text-sky-700 border border-sky-300 shadow-2xs' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'"
        >
          <span>📨</span>
          <span>수동 전문 전송 (Manual Sender)</span>
        </button>
        <button
          @click="activeSubTab = 'scenario'"
          class="flex-1 py-1.5 text-xs font-bold rounded-lg transition-all cursor-pointer flex items-center justify-center gap-1.5"
          :class="activeSubTab === 'scenario' ? 'bg-indigo-50 text-indigo-700 border border-indigo-300 shadow-2xs' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'"
        >
          <span>🚀</span>
          <span>충전 시나리오 러너 (Scenario)</span>
        </button>
        <button
          @click="activeSubTab = 'spec'"
          class="flex-1 py-1.5 text-xs font-bold rounded-lg transition-all cursor-pointer flex items-center justify-center gap-1.5"
          :class="activeSubTab === 'spec' ? 'bg-emerald-50 text-emerald-700 border border-emerald-300 shadow-2xs' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'"
        >
          <span>📋</span>
          <span>기기 스펙 & 커넥터</span>
        </button>
      </div>
    </div>

    <!-- 3. 메인 콘텐츠 영역 (탭별 내용) -->

    <!-- TAB 1: 수동 전문 전송 -->
    <div v-if="activeSubTab === 'manual'" class="bg-white rounded-xl border border-slate-200 p-4 shadow-2xs space-y-4">
      <div class="flex items-center justify-between pb-3 border-b border-slate-100">
        <div class="flex items-center gap-2">
          <span class="w-2 h-2 rounded-full bg-sky-500"></span>
          <h3 class="text-xs font-bold text-slate-900">OCPP 1.6-J 핵심 전문 수동 발송</h3>
        </div>

        <div class="flex items-center bg-slate-100 p-0.5 rounded-lg text-[11px] border border-slate-200">
          <button
            @click="sendMode = 'template'"
            class="px-2.5 py-0.5 rounded transition-all cursor-pointer font-medium"
            :class="sendMode === 'template' ? 'bg-white text-slate-900 font-bold shadow-2xs' : 'text-slate-600 hover:text-slate-900'"
          >
            템플릿 폼
          </button>
          <button
            @click="sendMode = 'raw'; onActionChange()"
            class="px-2.5 py-0.5 rounded transition-all cursor-pointer font-medium"
            :class="sendMode === 'raw' ? 'bg-white text-slate-900 font-bold shadow-2xs' : 'text-slate-600 hover:text-slate-900'"
          >
            Raw JSON 편집
          </button>
        </div>
      </div>

      <!-- 액션 선택 드롭다운 -->
      <div>
        <label class="block text-[11px] font-bold text-slate-700 mb-1">
          발송할 OCPP Action 선택
        </label>
        <select
          v-model="selectedAction"
          @change="onActionChange"
          class="w-full bg-slate-50 border border-slate-200 rounded-lg px-3 py-2 text-xs font-mono font-bold text-slate-800 focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500"
        >
          <option value="StatusNotification">StatusNotification (상태 통보)</option>
          <option value="BootNotification">BootNotification (충전기 기동/부팅)</option>
          <option value="Heartbeat">Heartbeat (생존 신호)</option>
          <option value="Authorize">Authorize (RFID 회원 인증 요청)</option>
          <option value="StartTransaction">StartTransaction (충전 시작 세션 개시)</option>
          <option value="MeterValues">MeterValues (충전 계측 전력량 전송)</option>
          <option value="StopTransaction">StopTransaction (충전 종료 및 정산)</option>
          <option value="DataTransfer">DataTransfer (커스텀 데이터 전송)</option>
        </select>
      </div>

      <!-- 1) 템플릿 폼 모드 -->
      <div v-if="sendMode === 'template'" class="space-y-3 p-3 bg-slate-50 rounded-lg border border-slate-200 text-xs">
        <!-- StatusNotification 전용 파라미터 -->
        <div v-if="selectedAction === 'StatusNotification'" class="space-y-2">
          <label class="block text-[11px] font-bold text-slate-700">변경할 충전기 상태 (Status)</label>
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-1.5 font-mono text-[11px]">
            <button
              v-for="st in (['Available', 'Preparing', 'Charging', 'Finishing', 'Faulted', 'Unavailable'] as const)"
              :key="st"
              @click="manualStatus = st"
              class="px-2 py-1 rounded border transition-colors cursor-pointer text-center"
              :class="manualStatus === st ? 'bg-sky-600 text-white font-bold border-sky-600' : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-100'"
            >
              {{ st }}
            </button>
          </div>
        </div>

        <!-- Authorize / StartTransaction / StopTransaction RFID -->
        <div v-if="['Authorize', 'StartTransaction', 'StopTransaction'].includes(selectedAction)" class="space-y-2">
          <label class="block text-[11px] font-bold text-slate-700">RFID 회원 카드번호 (idTag)</label>
          <div class="flex items-center gap-1.5 flex-wrap">
            <button
              v-for="p in rfidPresets"
              :key="p.tag"
              @click="manualRfid = p.tag"
              class="px-2 py-1 text-[11px] font-mono rounded border transition-colors cursor-pointer"
              :class="manualRfid === p.tag ? 'bg-sky-600 text-white font-bold border-sky-600' : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-100'"
            >
              {{ p.label }}
            </button>
          </div>
          <input
            v-model="manualRfid"
            class="w-full bg-white border border-slate-200 rounded-md px-2.5 py-1.5 font-mono text-xs focus:outline-hidden focus:ring-1 focus:ring-sky-500"
          />
        </div>

        <!-- StartTransaction / MeterValues 전력량 (Wh) -->
        <div v-if="['StartTransaction', 'MeterValues'].includes(selectedAction)" class="space-y-1">
          <label class="block text-[11px] font-bold text-slate-700">계측 전력량 (meterValue Wh)</label>
          <input
            v-model.number="manualMeterWh"
            type="number"
            class="w-full bg-white border border-slate-200 rounded-md px-2.5 py-1.5 font-mono text-xs focus:outline-hidden focus:ring-1 focus:ring-sky-500"
          />
        </div>

        <!-- StopTransaction 종료 사유 -->
        <div v-if="selectedAction === 'StopTransaction'" class="space-y-1">
          <label class="block text-[11px] font-bold text-slate-700">종료 사유 (Reason)</label>
          <select
            v-model="manualStopReason"
            class="w-full bg-white border border-slate-200 rounded-md px-2.5 py-1.5 font-mono text-xs"
          >
            <option value="Local">Local (사용자 현장 종료)</option>
            <option value="Remote">Remote (원격 정지)</option>
            <option value="EVDisconnected">EVDisconnected (차량 커넥터 분리)</option>
            <option value="EmergencyStop">EmergencyStop (비상 정지)</option>
          </select>
        </div>
      </div>

      <!-- 2) Raw JSON 편집 모드 -->
      <div v-else class="space-y-1.5">
        <label class="block text-[11px] font-bold text-slate-700">JSON Payload 편집</label>
        <textarea
          v-model="rawPayloadText"
          rows="8"
          class="w-full bg-slate-900 text-slate-100 p-3 rounded-lg font-mono text-xs focus:outline-hidden focus:ring-1 focus:ring-sky-500"
        ></textarea>
      </div>

      <!-- 발송 버튼 -->
      <button
        @click="sendManualCall"
        :disabled="!targetCharger.connected"
        class="w-full py-2.5 text-xs font-bold rounded-lg transition-all flex items-center justify-center gap-1.5 shadow-2xs"
        :class="targetCharger.connected ? 'bg-sky-600 hover:bg-sky-700 text-white cursor-pointer' : 'bg-slate-200 text-slate-400 cursor-not-allowed'"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M14 5l7 7m0 0l-7 7m7-7H3" />
        </svg>
        <span>[{{ targetCharger.id }}] 로 {{ selectedAction }} CALL 전문 발송</span>
      </button>
    </div>

    <!-- TAB 2: 충전 라이프사이클 시나리오 러너 -->
    <div v-else-if="activeSubTab === 'scenario'" class="bg-white rounded-xl border border-slate-200 p-4 shadow-2xs space-y-4">
      <div class="flex items-center justify-between pb-3 border-b border-slate-100">
        <div class="flex items-center gap-2">
          <span class="w-2 h-2 rounded-full bg-indigo-500"></span>
          <h3 class="text-xs font-bold text-slate-900">4대 회원 유형별 8단계 라이프사이클 자동 시뮬레이션</h3>
        </div>
        <span class="text-[10px] font-mono px-2 py-0.5 rounded bg-indigo-50 text-indigo-700 font-bold">
          E2E 시나리오
        </span>
      </div>

      <!-- 회원 유형 선택 라디오 카드 -->
      <div class="space-y-2">
        <label class="block text-[11px] font-bold text-slate-700">테스트할 회원 유형 선택</label>
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-2">
          <div
            v-for="p in rfidPresets"
            :key="p.type"
            @click="scenarioUserType = p.type as any; onUserTypeChange()"
            class="p-2.5 rounded-lg border transition-all cursor-pointer flex flex-col justify-between"
            :class="scenarioUserType === p.type ? 'bg-indigo-50 border-indigo-400 ring-2 ring-indigo-100 font-bold' : 'bg-slate-50 border-slate-200 hover:bg-white'"
          >
            <div class="flex items-center justify-between">
              <span class="text-xs font-bold text-slate-900">{{ p.label.split(' ')[0] }}</span>
              <span class="text-[10px] font-mono px-1 rounded bg-slate-200 text-slate-700">{{ p.type }}</span>
            </div>
            <span class="text-[10px] font-mono text-slate-500 mt-1 truncate">{{ p.tag }}</span>
          </div>
        </div>
      </div>

      <!-- 시나리오 진행 상태 및 프로그레스 바 -->
      <div v-if="targetCharger.scenarioRunning || targetCharger.scenarioProgress > 0" class="p-3 bg-indigo-50/70 border border-indigo-200 rounded-lg space-y-2">
        <div class="flex items-center justify-between text-xs font-bold text-indigo-900">
          <span>{{ targetCharger.scenarioStep }}</span>
          <span class="font-mono">{{ targetCharger.scenarioProgress }}%</span>
        </div>
        <div class="w-full bg-indigo-100 h-2 rounded-full overflow-hidden">
          <div
            class="bg-indigo-600 h-full transition-all duration-300"
            :style="{ width: `${targetCharger.scenarioProgress}%` }"
          ></div>
        </div>
      </div>

      <!-- 시나리오 실행/중단 버튼군 -->
      <div class="flex items-center gap-2">
        <button
          v-if="!targetCharger.scenarioRunning"
          @click="handleRunScenario"
          :disabled="!targetCharger.connected"
          class="flex-1 py-2.5 text-xs font-bold rounded-lg transition-all flex items-center justify-center gap-1.5 shadow-2xs"
          :class="targetCharger.connected ? 'bg-indigo-600 hover:bg-indigo-700 text-white cursor-pointer' : 'bg-slate-200 text-slate-400 cursor-not-allowed'"
        >
          <span>🚀 [{{ targetCharger.id }}] 원클릭 시나리오 기동</span>
        </button>
        <button
          v-else
          @click="handleAbortScenario"
          class="flex-1 py-2.5 text-xs font-bold rounded-lg bg-rose-600 hover:bg-rose-700 text-white transition-all flex items-center justify-center gap-1.5 cursor-pointer shadow-2xs"
        >
          <span>⏹️ 시나리오 긴급 중단</span>
        </button>
      </div>
    </div>

    <!-- TAB 3: 기기 스펙 & 커넥터 정보 -->
    <div v-else class="bg-white rounded-xl border border-slate-200 p-4 shadow-2xs space-y-4">
      <div class="flex items-center justify-between pb-3 border-b border-slate-100">
        <div class="flex items-center gap-2">
          <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
          <h3 class="text-xs font-bold text-slate-900">충전기 하드웨어 스펙 & 커넥터(충전건) 현황</h3>
        </div>
        <span class="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 font-bold">
          CSMS Lite 표준
        </span>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
        <div class="p-3 rounded-lg bg-slate-50 border border-slate-200 space-y-1.5">
          <span class="text-[11px] font-bold text-slate-500">기기 식별자 체계</span>
          <div class="font-mono text-xs text-slate-800 space-y-1">
            <div>충전소 ID (ST_ID): <b>{{ targetCharger.stationId }}</b></div>
            <div>충전기 ID (CP_ID): <b>{{ targetCharger.chargerId }}</b></div>
            <div>결합 ID (chargeBoxId): <b class="text-sky-700">{{ targetCharger.id }}</b></div>
          </div>
        </div>

        <div class="p-3 rounded-lg bg-slate-50 border border-slate-200 space-y-1.5">
          <span class="text-[11px] font-bold text-slate-500">엔드포인트 및 하드웨어</span>
          <div class="font-mono text-xs text-slate-800 space-y-1 truncate">
            <div>엔드포인트: <span class="text-slate-600">{{ targetCharger.wsUrl }}</span></div>
            <div>제조사/모델: <b>{{ targetCharger.vendor }} / {{ targetCharger.model }}</b></div>
            <div>정격 용량: <b class="text-emerald-700">{{ targetCharger.powerKw }} kW</b></div>
          </div>
        </div>
      </div>

      <!-- 커넥터 목록 -->
      <div class="space-y-2">
        <span class="text-[11px] font-bold text-slate-700">채널 커넥터 현황</span>
        <div class="grid grid-cols-2 gap-2">
          <div
            v-for="conn in targetCharger.connectors"
            :key="conn.connectorId"
            class="p-2.5 rounded-lg border border-slate-200 bg-slate-50 flex items-center justify-between"
          >
            <div class="flex items-center gap-2">
              <span class="w-6 h-6 rounded-md bg-white border border-slate-200 flex items-center justify-center font-mono font-bold text-xs text-slate-700">
                #{{ conn.connectorId }}
              </span>
              <span class="text-xs font-medium text-slate-800">커넥터 {{ conn.connectorId }}</span>
            </div>
            <span class="text-[10px] font-mono px-2 py-0.5 rounded-full font-bold" :class="conn.status === 'Charging' ? 'bg-indigo-100 text-indigo-700' : 'bg-emerald-100 text-emerald-700'">
              {{ conn.status }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
