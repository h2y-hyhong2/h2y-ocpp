<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useVirtualCharger } from '../composables/useVirtualCharger'
import type { ChargerInstance } from '../types/ocpp'
import TaskVerificationSuite from './TaskVerificationSuite.vue'

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

// 4대 용도별 메인 탭: 'message' (수동 전문 제어) | 'automation' (충전기 자동화 설정) | 'scenario' (충전 시나리오) | 'verify' (과제 검증)
const activeTab = ref<'message' | 'automation' | 'scenario' | 'verify'>('message')

// 세로형 전문 자동화 제어 패널 접기/펼치기 상태 (기본 펼침)
const isAutoPanelExpanded = ref(true)

// 단일 충전기 자동화 일괄 OFF/ON 프리셋
function handleManualPreset(id: string) {
  store.setAutoHeartbeat(false, id)
  store.setAutoBootOnConnect(false, id)
  store.setAutoReply(false, id)
  store.setAutoMeterValues(false, id)
}

function handleAutoPreset(id: string) {
  store.setAutoHeartbeat(true, id)
  store.setAutoBootOnConnect(true, id)
  store.setAutoReply(true, id)
  store.setAutoMeterValues(true, id)
}

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
const manualRfid = ref(store.selectedIdTag?.value || 'CARD-CORP-9901')
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

function handleSelectConnector(connId: number) {
  if (targetCharger.value) {
    targetCharger.value.activeConnectorId = connId
    if (store.setActiveConnector) {
      store.setActiveConnector(connId, targetCharger.value.id)
    }
    onActionChange()
  }
}

const connectorList = computed(() => {
  const c = targetCharger.value
  if (c && c.connectors && c.connectors.length > 0) {
    return c.connectors
  }
  return [
    { connectorId: 1, status: (c?.status || 'Available') as any, type: 'DC콤보1 (CCS1)', maxPowerKw: Math.round((c?.powerKw || 350) / 2) },
    { connectorId: 2, status: 'Available' as any, type: 'DC콤보1 (CCS1)', maxPowerKw: Math.round((c?.powerKw || 350) / 2) }
  ]
})

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
const scenarioRfid = ref(store.selectedIdTag?.value || 'CARD-CORP-9901')

// 하단 마스터 데이터에서 선택된 태그 실시간 동기화
if (store.selectedIdTag) {
  watch(store.selectedIdTag, (newTag) => {
    if (newTag) {
      manualRfid.value = newTag
      scenarioRfid.value = newTag
    }
  })
}

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
    <!-- 메인 4대 탭 네비게이션 (전문 제어 / 자동화 설정 / 시나리오 / 과제 검증) -->
    <div class="bg-white rounded-xl border border-slate-200 p-1.5 shadow-2xs flex-shrink-0">
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-1">
        <!-- 탭 1: 전문 수동 발송 -->
        <button
          @click="activeTab = 'message'"
          class="py-2 px-2 text-xs font-bold rounded-lg transition-all cursor-pointer flex items-center justify-center gap-1.5 text-center"
          :class="activeTab === 'message' ? 'bg-sky-50 text-sky-700 border border-sky-300 shadow-2xs' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50 border border-transparent'"
        >
          <span>📨</span>
          <span class="truncate">전문 제어</span>
        </button>

        <!-- 탭 2: 충전기 자동화 설정 (자동으로 나가는 전문 설정) -->
        <button
          @click="activeTab = 'automation'"
          class="py-2 px-2 text-xs font-bold rounded-lg transition-all cursor-pointer flex items-center justify-center gap-1.5 text-center"
          :class="activeTab === 'automation' ? 'bg-amber-50 text-amber-800 border border-amber-300 shadow-2xs' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50 border border-transparent'"
        >
          <span>⚙️</span>
          <span class="truncate">자동화 설정</span>
        </button>

        <!-- 탭 3: 충전 시나리오 러너 -->
        <button
          @click="activeTab = 'scenario'"
          class="py-2 px-2 text-xs font-bold rounded-lg transition-all cursor-pointer flex items-center justify-center gap-1.5 text-center"
          :class="activeTab === 'scenario' ? 'bg-indigo-50 text-indigo-700 border border-indigo-300 shadow-2xs' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50 border border-transparent'"
        >
          <span>🚀</span>
          <span class="truncate">시나리오</span>
        </button>

        <!-- 탭 4: WBS 과제 검증 -->
        <button
          @click="activeTab = 'verify'"
          class="py-2 px-2 text-xs font-bold rounded-lg transition-all cursor-pointer flex items-center justify-center gap-1.5 text-center"
          :class="activeTab === 'verify' ? 'bg-emerald-50 text-emerald-700 border border-emerald-300 shadow-2xs' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50 border border-transparent'"
        >
          <span>🧪</span>
          <span class="truncate">과제 검증</span>
        </button>
      </div>
    </div>

    <!-- 3. 메인 콘텐츠 영역 (탭별 내용) -->

    <!-- TAB 1: 전문 제어 & 자동화 (Message & Automation) -->
    <div v-if="activeTab === 'message'" class="space-y-3">
      <!-- 컴팩트 자동화 현황 요약 바 (자동화 설정 탭 바로가기) -->
      <div class="px-3 py-2 bg-white rounded-xl border border-slate-200 flex items-center justify-between gap-2 shadow-2xs select-none">
        <div class="flex items-center gap-1.5 overflow-hidden text-xs">
          <span class="text-xs shrink-0">⚙️</span>
          <span class="font-bold text-slate-700 shrink-0 hidden sm:inline">자동 전문:</span>
          <div class="flex items-center gap-1 text-[10px] font-mono overflow-x-auto scrollbar-none">
            <span
              class="px-1.5 py-0.5 rounded font-semibold border"
              :class="targetCharger.autoHeartbeat ? 'bg-sky-50 text-sky-700 border-sky-200' : 'bg-slate-100 text-slate-400 border-slate-200'"
            >
              💓 HB: {{ targetCharger.autoHeartbeat ? `${targetCharger.heartbeatIntervalSec || 60}s` : 'OFF' }}
            </span>
            <span
              class="px-1.5 py-0.5 rounded font-semibold border"
              :class="targetCharger.autoBootOnConnect ? 'bg-sky-50 text-sky-700 border-sky-200' : 'bg-slate-100 text-slate-400 border-slate-200'"
            >
              🚀 기동: {{ targetCharger.autoBootOnConnect ? 'ON' : 'OFF' }}
            </span>
            <span
              class="px-1.5 py-0.5 rounded font-semibold border"
              :class="targetCharger.autoReply ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-100 text-slate-400 border-slate-200'"
            >
              🤖 응답: {{ targetCharger.autoReply ? 'ON' : 'OFF' }}
            </span>
            <span
              class="px-1.5 py-0.5 rounded font-semibold border"
              :class="targetCharger.autoMeterValues ? 'bg-indigo-50 text-indigo-700 border-indigo-200' : 'bg-slate-100 text-slate-400 border-slate-200'"
            >
              📊 계측: {{ targetCharger.autoMeterValues ? `${targetCharger.autoMeterIntervalSec || 10}s` : 'OFF' }}
            </span>
          </div>
        </div>
        <button
          type="button"
          @click="activeTab = 'automation'"
          class="px-2 py-0.5 text-[11px] font-bold rounded text-amber-800 bg-amber-50 hover:bg-amber-100 border border-amber-200 cursor-pointer transition-colors shrink-0 shadow-2xs"
          title="자동으로 나가는 전문 상세 설정 탭으로 이동"
        >
          설정 변경 ➔
        </button>
      </div>

      <!-- 1-2. 수동 전문 전송 (Manual Sender) -->
      <div class="bg-white rounded-xl border border-slate-200 p-4 shadow-2xs space-y-4">
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

        <!-- 템플릿 폼 모드 -->
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

        <!-- Raw JSON 편집 모드 -->
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
    </div>

    <!-- TAB 2: 충전기 자동화 설정 (자동으로 나가는 전문 설정) -->
    <div v-else-if="activeTab === 'automation'" class="space-y-3 font-sans">
      <!-- 2-0. 상단 마스터 헤더 & 일괄 프리셋 바 -->
      <div class="bg-white rounded-xl border border-slate-200 p-3.5 shadow-2xs space-y-3">
        <div class="flex items-center justify-between pb-2.5 border-b border-slate-100">
          <div class="flex items-center gap-2">
            <span class="w-2.5 h-2.5 rounded-full bg-amber-500 animate-pulse"></span>
            <div>
              <h3 class="text-xs font-bold text-slate-900 flex items-center gap-1.5">
                <span>충전기 전문 자동화 엔진 설정</span>
                <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-amber-50 text-amber-800 border border-amber-200 font-bold">
                  {{ targetCharger.id }}
                </span>
              </h3>
              <p class="text-[11px] text-slate-500 mt-0.5">
                충전기가 서버와 통신할 때 상황 및 주기에 따라 백그라운드로 자동 발송·회신하는 전문을 제어합니다.
              </p>
            </div>
          </div>
          <span
            class="text-[10px] font-mono px-2 py-0.5 rounded-full font-bold border"
            :class="targetCharger.connected ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-100 text-slate-500 border-slate-200'"
          >
            {{ targetCharger.connected ? '● ONLINE' : '○ OFFLINE' }}
          </span>
        </div>

        <!-- 일괄 제어 프리셋 버튼 바 -->
        <div class="flex items-center justify-between gap-2 flex-wrap text-xs font-mono">
          <div class="flex items-center gap-1.5">
            <span class="text-[11px] font-bold text-slate-500 font-sans">현재 충전기:</span>
            <button
              type="button"
              @click="handleManualPreset(targetCharger.id)"
              class="px-2.5 py-1 rounded-md border border-rose-200 bg-rose-50 hover:bg-rose-100 text-rose-700 font-bold cursor-pointer transition-colors shadow-2xs"
              title="이 충전기의 모든 자동화(Heartbeat, Auto-Boot, Auto-Reply, Auto-Meter) 정지"
            >
              🛑 전체 OFF
            </button>
            <button
              type="button"
              @click="handleAutoPreset(targetCharger.id)"
              class="px-2.5 py-1 rounded-md border border-sky-200 bg-sky-50 hover:bg-sky-100 text-sky-700 font-bold cursor-pointer transition-colors shadow-2xs"
              title="이 충전기의 표준 자동화(Heartbeat 60s, Auto-Boot, Auto-Reply) 가동"
            >
              ⚡ 전체 ON
            </button>
          </div>

          <div class="flex items-center gap-1.5">
            <span class="text-[11px] font-bold text-slate-500 font-sans">전사 일괄:</span>
            <button
              type="button"
              @click="store.setFullAutoMode"
              class="px-2.5 py-1 rounded-md border border-indigo-200 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold cursor-pointer transition-colors shadow-2xs"
              title="등록된 모든 충전기의 자동화를 일괄 활성화"
            >
              🌐 전사 전체 ON
            </button>
            <button
              type="button"
              @click="store.setFullManualMode"
              class="px-2.5 py-1 rounded-md border border-slate-200 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold cursor-pointer transition-colors shadow-2xs"
              title="등록된 모든 충전기를 완전 수동 통제 모드로 전환"
            >
              🔒 전사 전체 OFF
            </button>
          </div>
        </div>
      </div>

      <!-- 2-1. 카드 1: 자동 Heartbeat 주기 발송 -->
      <div
        class="bg-white rounded-xl border transition-all p-3.5 shadow-2xs space-y-2.5"
        :class="targetCharger.autoHeartbeat ? 'border-sky-200 bg-sky-50/20' : 'border-slate-200'"
      >
        <div class="flex items-start justify-between gap-2">
          <div class="flex items-start gap-2.5">
            <span class="text-xl flex-shrink-0 mt-0.5">💓</span>
            <div>
              <div class="flex items-center gap-2">
                <h4 class="text-xs font-bold text-slate-900">1. 자동 Heartbeat 주기 발송 (Keep-Alive)</h4>
                <span
                  class="text-[9px] font-mono px-1.5 py-0.5 rounded font-bold"
                  :class="targetCharger.autoHeartbeat ? 'bg-sky-100 text-sky-800 border border-sky-200' : 'bg-slate-100 text-slate-500 border border-slate-200'"
                >
                  {{ targetCharger.autoHeartbeat ? 'ACTIVE (자동발송 중)' : 'STOPPED (정지됨)' }}
                </span>
              </div>
              <p class="text-[11px] text-slate-500 mt-0.5">
                웹소켓 연결 상태에서 CSMS 서버와의 세션을 유지하고 서버 타임아웃을 방지하기 위해 정해진 주기마다 Heartbeat 전문을 자동 발송합니다.
              </p>
            </div>
          </div>

          <button
            @click="store.toggleAutoHeartbeat(targetCharger.id)"
            class="px-3 py-1 rounded-lg text-xs font-mono font-bold transition-all cursor-pointer border shadow-2xs shrink-0"
            :class="targetCharger.autoHeartbeat ? 'bg-sky-600 text-white border-sky-600 hover:bg-sky-700' : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-100'"
          >
            {{ targetCharger.autoHeartbeat ? '● 자동발송 ON' : '○ 자동발송 OFF' }}
          </button>
        </div>

        <div class="pt-2 border-t border-slate-100 flex items-center justify-between flex-wrap gap-2 text-xs">
          <div class="flex items-center gap-2 font-mono">
            <span class="text-slate-500 text-[11px] font-sans">발송 주기:</span>
            <select
              :value="targetCharger.heartbeatIntervalSec || 60"
              @change="(e: any) => store.setHeartbeatInterval(Number(e.target.value), targetCharger.id)"
              class="bg-white border border-slate-200 rounded-md px-2 py-1 text-xs font-mono font-semibold text-slate-800 focus:outline-hidden focus:ring-1 focus:ring-sky-500 shadow-2xs"
            >
              <option :value="5">5초 (초고속 검증용)</option>
              <option :value="10">10초</option>
              <option :value="30">30초</option>
              <option :value="60">60초 (OCPP 1.6 표준 권장)</option>
              <option :value="120">120초 (2분)</option>
              <option :value="300">300초 (5분)</option>
            </select>
            <span class="text-[10px] text-slate-400 font-sans hidden sm:inline">(CSMS 타임아웃: 180s)</span>
          </div>

          <div class="flex items-center gap-1.5">
            <span class="text-[10px] font-mono text-slate-400 bg-slate-100 px-1.5 py-0.5 rounded border border-slate-200">
              Payload: {}
            </span>
            <button
              @click="store.sendHeartbeat(targetCharger.id)"
              :disabled="!targetCharger.connected"
              class="px-2.5 py-1 rounded-md text-[11px] font-bold transition-all border shadow-2xs"
              :class="targetCharger.connected ? 'bg-white hover:bg-slate-50 text-slate-700 border-slate-200 cursor-pointer' : 'bg-slate-100 text-slate-400 border-slate-200 cursor-not-allowed'"
              title="지금 즉시 1회 Heartbeat 전문 전송"
            >
              ⚡ 1회 즉시 전송
            </button>
          </div>
        </div>
      </div>

      <!-- 2-2. 카드 2: 접속 시 기동전문 자동 발송 (Auto-Boot Sequence) -->
      <div
        class="bg-white rounded-xl border transition-all p-3.5 shadow-2xs space-y-2.5"
        :class="targetCharger.autoBootOnConnect ? 'border-sky-200 bg-sky-50/20' : 'border-slate-200'"
      >
        <div class="flex items-start justify-between gap-2">
          <div class="flex items-start gap-2.5">
            <span class="text-xl flex-shrink-0 mt-0.5">🚀</span>
            <div>
              <div class="flex items-center gap-2">
                <h4 class="text-xs font-bold text-slate-900">2. 연결 시 기동전문 자동 발송 (Auto-Boot)</h4>
                <span
                  class="text-[9px] font-mono px-1.5 py-0.5 rounded font-bold"
                  :class="targetCharger.autoBootOnConnect ? 'bg-sky-100 text-sky-800 border border-sky-200' : 'bg-slate-100 text-slate-500 border border-slate-200'"
                >
                  {{ targetCharger.autoBootOnConnect ? 'ACTIVE (연결시 자동발송)' : 'STOPPED (정지됨)' }}
                </span>
              </div>
              <p class="text-[11px] text-slate-500 mt-0.5">
                WebSocket 연결 수립 직후 충전기 기동 및 상태 등록 시퀀스(BootNotification 및 커넥터 #1, #2 StatusNotification)를 순차적으로 자동 발송합니다.
              </p>
            </div>
          </div>

          <button
            @click="store.toggleAutoBootOnConnect(targetCharger.id)"
            class="px-3 py-1 rounded-lg text-xs font-mono font-bold transition-all cursor-pointer border shadow-2xs shrink-0"
            :class="targetCharger.autoBootOnConnect ? 'bg-sky-600 text-white border-sky-600 hover:bg-sky-700' : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-100'"
          >
            {{ targetCharger.autoBootOnConnect ? '● 자동발송 ON' : '○ 자동발송 OFF' }}
          </button>
        </div>

        <div class="pt-2 border-t border-slate-100 flex items-center justify-between flex-wrap gap-2 text-xs">
          <div class="flex items-center gap-1.5 font-mono text-[11px] text-slate-600">
            <span class="font-sans font-bold text-slate-500 text-[10px]">자동 발송 시퀀스:</span>
            <span class="px-1.5 py-0.5 rounded bg-slate-100 border border-slate-200 font-bold text-slate-700">1. BootNotification</span>
            <span class="text-slate-400">➔</span>
            <span class="px-1.5 py-0.5 rounded bg-slate-100 border border-slate-200 font-bold text-slate-700">2. StatusNotification (Available)</span>
          </div>

          <div class="flex items-center gap-1.5">
            <button
              @click="store.runBootSequence(targetCharger.id)"
              :disabled="!targetCharger.connected"
              class="px-2.5 py-1 rounded-md text-[11px] font-bold transition-all border shadow-2xs"
              :class="targetCharger.connected ? 'bg-white hover:bg-slate-50 text-sky-700 border-sky-200 cursor-pointer' : 'bg-slate-100 text-slate-400 border-slate-200 cursor-not-allowed'"
              title="지금 기동 시퀀스를 수동으로 즉시 발송합니다."
            >
              🚀 기동 시퀀스 즉시 실행
            </button>
          </div>
        </div>
      </div>

      <!-- 2-3. 카드 3: 충전 중 계측치(MeterValues) 주기 자동 발송 -->
      <div
        class="bg-white rounded-xl border transition-all p-3.5 shadow-2xs space-y-2.5"
        :class="targetCharger.autoMeterValues ? 'border-indigo-200 bg-indigo-50/20' : 'border-slate-200'"
      >
        <div class="flex items-start justify-between gap-2">
          <div class="flex items-start gap-2.5">
            <span class="text-xl flex-shrink-0 mt-0.5">📊</span>
            <div>
              <div class="flex items-center gap-2">
                <h4 class="text-xs font-bold text-slate-900">3. 충전 중 계측치(MeterValues) 주기 발송</h4>
                <span
                  class="text-[9px] font-mono px-1.5 py-0.5 rounded font-bold"
                  :class="targetCharger.autoMeterValues ? 'bg-indigo-100 text-indigo-800 border border-indigo-200' : 'bg-slate-100 text-slate-500 border border-slate-200'"
                >
                  {{ targetCharger.autoMeterValues ? 'ACTIVE (충전중 주기발송)' : 'STOPPED (정지됨)' }}
                </span>
              </div>
              <p class="text-[11px] text-slate-500 mt-0.5">
                충전 진행 중(Charging 상태)일 때 실시간 누적 전력량(Wh), 충전 출력(W), 배터리 SoC(%)를 주기적으로 자동 계산하여 CSMS로 전송합니다.
              </p>
            </div>
          </div>

          <button
            @click="store.toggleAutoMeterValues(targetCharger.id)"
            class="px-3 py-1 rounded-lg text-xs font-mono font-bold transition-all cursor-pointer border shadow-2xs shrink-0"
            :class="targetCharger.autoMeterValues ? 'bg-indigo-600 text-white border-indigo-600 hover:bg-indigo-700' : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-100'"
          >
            {{ targetCharger.autoMeterValues ? '● 자동발송 ON' : '○ 자동발송 OFF' }}
          </button>
        </div>

        <div class="pt-2 border-t border-slate-100 flex items-center justify-between flex-wrap gap-2 text-xs">
          <div class="flex items-center gap-2 font-mono">
            <span class="text-slate-500 text-[11px] font-sans">계측 주기:</span>
            <select
              :value="targetCharger.autoMeterIntervalSec || 10"
              @change="(e: any) => store.setAutoMeterInterval(Number(e.target.value), targetCharger.id)"
              class="bg-white border border-slate-200 rounded-md px-2 py-1 text-xs font-mono font-semibold text-slate-800 focus:outline-hidden focus:ring-1 focus:ring-indigo-500 shadow-2xs"
            >
              <option :value="5">5초 (고속 시뮬레이션)</option>
              <option :value="10">10초 (표준 권장)</option>
              <option :value="30">30초</option>
              <option :value="60">60초 (1분)</option>
            </select>
            <span class="text-[10px] text-slate-500 font-sans">
              (주기당 +500Wh 누적)
            </span>
          </div>

          <div class="flex items-center gap-1.5">
            <button
              @click="store.sendMeterValues(500, targetCharger.id)"
              :disabled="!targetCharger.connected"
              class="px-2.5 py-1 rounded-md text-[11px] font-bold transition-all border shadow-2xs"
              :class="targetCharger.connected ? 'bg-white hover:bg-slate-50 text-indigo-700 border-indigo-200 cursor-pointer' : 'bg-slate-100 text-slate-400 border-slate-200 cursor-not-allowed'"
              title="지금 즉시 1회 MeterValues 전문 전송"
            >
              📊 1회 계측 전송
            </button>
          </div>
        </div>

        <!-- Measurand 항목 태그 칩 -->
        <div class="flex items-center gap-1 text-[10px] font-mono text-slate-500">
          <span class="font-sans text-[10px] text-slate-400">포함 항목:</span>
          <span class="px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 border border-slate-200">Energy.Active.Import.Register (Wh)</span>
          <span class="px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 border border-slate-200">SoC (%)</span>
          <span class="px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 border border-slate-200">Power.Active.Import (W)</span>
        </div>
      </div>

      <!-- 2-4. 카드 4: 관제 요청(CALL) 자동응답 (Auto-Reply Engine) -->
      <div
        class="bg-white rounded-xl border transition-all p-3.5 shadow-2xs space-y-2.5"
        :class="targetCharger.autoReply ? 'border-emerald-200 bg-emerald-50/20' : 'border-slate-200'"
      >
        <div class="flex items-start justify-between gap-2">
          <div class="flex items-start gap-2.5">
            <span class="text-xl flex-shrink-0 mt-0.5">🤖</span>
            <div>
              <div class="flex items-center gap-2">
                <h4 class="text-xs font-bold text-slate-900">4. 관제 원격 명령(CALL) 자동응답 (Auto-Reply Engine)</h4>
                <span
                  class="text-[9px] font-mono px-1.5 py-0.5 rounded font-bold"
                  :class="targetCharger.autoReply ? 'bg-emerald-100 text-emerald-800 border border-emerald-200' : 'bg-amber-100 text-amber-800 border border-amber-200'"
                >
                  {{ targetCharger.autoReply ? 'ACCEPTED 자동 회신' : '무응답 모드 (타임아웃 시험)' }}
                </span>
              </div>
              <p class="text-[11px] text-slate-500 mt-0.5">
                CSMS 관제 서버로부터 원격 제어 명령(CALL)을 수신했을 때 지체 없이 승인 응답(CallResult [3])을 자동 생성하여 회신합니다.
              </p>
            </div>
          </div>

          <button
            @click="store.toggleAutoReply(targetCharger.id)"
            class="px-3 py-1 rounded-lg text-xs font-mono font-bold transition-all cursor-pointer border shadow-2xs shrink-0"
            :class="targetCharger.autoReply ? 'bg-emerald-600 text-white border-emerald-600 hover:bg-emerald-700' : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-100'"
          >
            {{ targetCharger.autoReply ? '● 자동응답 ON' : '○ 무응답 모드' }}
          </button>
        </div>

        <div class="pt-2 border-t border-slate-100 space-y-1.5">
          <span class="text-[10px] font-bold text-slate-400 block uppercase tracking-wider">
            자동 응답 처리 대상 원격 전문 목록 (Accepted 회신):
          </span>
          <div class="grid grid-cols-2 sm:grid-cols-3 gap-1.5 text-[11px] font-mono">
            <div class="p-1.5 rounded bg-slate-50 border border-slate-200 flex items-center justify-between">
              <span class="font-bold text-slate-700">RemoteStartTransaction</span>
              <span class="text-[9px] text-emerald-700 font-bold bg-emerald-50 px-1 rounded">Accepted</span>
            </div>
            <div class="p-1.5 rounded bg-slate-50 border border-slate-200 flex items-center justify-between">
              <span class="font-bold text-slate-700">RemoteStopTransaction</span>
              <span class="text-[9px] text-emerald-700 font-bold bg-emerald-50 px-1 rounded">Accepted</span>
            </div>
            <div class="p-1.5 rounded bg-slate-50 border border-slate-200 flex items-center justify-between">
              <span class="font-bold text-slate-700">Reset (Soft/Hard)</span>
              <span class="text-[9px] text-emerald-700 font-bold bg-emerald-50 px-1 rounded">Accepted</span>
            </div>
            <div class="p-1.5 rounded bg-slate-50 border border-slate-200 flex items-center justify-between">
              <span class="font-bold text-slate-700">UnlockConnector</span>
              <span class="text-[9px] text-emerald-700 font-bold bg-emerald-50 px-1 rounded">Unlocked</span>
            </div>
            <div class="p-1.5 rounded bg-slate-50 border border-slate-200 flex items-center justify-between">
              <span class="font-bold text-slate-700">ChangeAvailability</span>
              <span class="text-[9px] text-emerald-700 font-bold bg-emerald-50 px-1 rounded">Accepted</span>
            </div>
            <div class="p-1.5 rounded bg-slate-50 border border-slate-200 flex items-center justify-between">
              <span class="font-bold text-slate-700">TriggerMessage</span>
              <span class="text-[9px] text-sky-700 font-bold bg-sky-50 px-1 rounded">즉시트리거</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 2-5. 카드 5: 커넥터 상태 변화 자동 통지 (StatusNotification Policy) -->
      <div class="bg-white rounded-xl border border-slate-200 p-3.5 shadow-2xs space-y-2.5">
        <div class="flex items-start justify-between gap-2">
          <div class="flex items-start gap-2.5">
            <span class="text-xl flex-shrink-0 mt-0.5">⚡</span>
            <div>
              <div class="flex items-center gap-2">
                <h4 class="text-xs font-bold text-slate-900">5. 커넥터 상태 변화 자동 통지 (StatusNotification Policy)</h4>
                <span class="text-[9px] font-mono px-1.5 py-0.5 rounded font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                  EVENT-DRIVEN
                </span>
              </div>
              <p class="text-[11px] text-slate-500 mt-0.5">
                커넥터의 물리적/논리적 상태가 전이될 때마다 CSMS 관제 시스템으로 StatusNotification 전문을 실시간 자동 송신합니다.
              </p>
            </div>
          </div>
        </div>

        <div class="pt-2 border-t border-slate-100 space-y-2">
          <!-- 상태 흐름 다이어그램 뱃지 -->
          <div class="flex items-center gap-1.5 text-[10px] font-mono flex-wrap bg-slate-50 p-2 rounded-lg border border-slate-200">
            <span class="px-2 py-0.5 rounded bg-emerald-100 text-emerald-800 font-bold">Available</span>
            <span class="text-slate-400">➔</span>
            <span class="px-2 py-0.5 rounded bg-sky-100 text-sky-800 font-bold">Preparing</span>
            <span class="text-slate-400">➔</span>
            <span class="px-2 py-0.5 rounded bg-indigo-100 text-indigo-800 font-bold">Charging</span>
            <span class="text-slate-400">➔</span>
            <span class="px-2 py-0.5 rounded bg-amber-100 text-amber-800 font-bold">Finishing</span>
            <span class="text-slate-400">➔</span>
            <span class="px-2 py-0.5 rounded bg-emerald-100 text-emerald-800 font-bold">Available</span>
          </div>

          <!-- 상태별 원클릭 즉시 통지 트리거 버튼들 -->
          <div class="flex items-center justify-between flex-wrap gap-1.5 pt-1">
            <span class="text-[11px] font-bold text-slate-500 font-sans">모의 상태 즉시 통지:</span>
            <div class="flex items-center gap-1">
              <button
                @click="store.sendStatusNotification('Available', 'NoError', targetCharger.id)"
                :disabled="!targetCharger.connected"
                class="px-2 py-1 text-[10px] font-bold rounded bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border border-emerald-200 cursor-pointer shadow-2xs"
              >
                ● Available (대기)
              </button>
              <button
                @click="store.sendStatusNotification('Preparing', 'NoError', targetCharger.id)"
                :disabled="!targetCharger.connected"
                class="px-2 py-1 text-[10px] font-bold rounded bg-sky-50 text-sky-700 hover:bg-sky-100 border border-sky-200 cursor-pointer shadow-2xs"
              >
                ● Preparing (연결)
              </button>
              <button
                @click="store.sendStatusNotification('Faulted', 'EVCommunicationError', targetCharger.id)"
                :disabled="!targetCharger.connected"
                class="px-2 py-1 text-[10px] font-bold rounded bg-rose-50 text-rose-700 hover:bg-rose-100 border border-rose-200 cursor-pointer shadow-2xs"
                title="통신 장애 오류 모의 발송"
              >
                ⚠️ Faulted (장애모의)
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- TAB 3: 충전 라이프사이클 시나리오 러너 (Scenario) -->
    <div v-else-if="activeTab === 'scenario'" class="bg-white rounded-xl border border-slate-200 p-4 shadow-2xs space-y-4">
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

    <!-- TAB 3: [3368] CSMS Lite WBS 과제 검증 스위트 (Task Verification) -->
    <div v-else-if="activeTab === 'verify'">
      <TaskVerificationSuite
        :charger-id="targetCharger.id"
        :charger="store"
      />
    </div>

  </div>
</template>
