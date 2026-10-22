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

// 4대 용도별 메인 탭: 'message' (전문 제어) | 'scenario' (충전 시나리오) | 'verify' (과제 검증) | 'spec' (기기 스펙)
const activeTab = ref<'message' | 'scenario' | 'verify' | 'spec'>('message')

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
    <!-- 1. 선택된 충전기 컴팩트 상태 및 빠른 제어 바 -->
    <div
      class="rounded-xl border p-3 shadow-2xs flex-shrink-0 transition-colors"
      :class="targetCharger.connected ? 'bg-emerald-50/20 border-emerald-200' : targetCharger.connecting ? 'bg-amber-50/40 border-amber-200 animate-pulse' : 'bg-white border-slate-200'"
    >
      <div class="flex flex-wrap items-center justify-between gap-2.5 pb-2.5 border-b border-slate-100">
        <!-- 충전기 식별자 및 타이틀 -->
        <div class="flex items-center gap-2.5">
          <div class="w-8 h-8 rounded-lg bg-sky-50 text-sky-600 border border-sky-100 flex items-center justify-center font-bold text-xs shadow-2xs">
            ⚡
          </div>
          <div class="flex flex-col">
            <div class="flex items-center gap-1.5">
              <span class="text-sm font-black font-mono text-slate-900 tracking-tight">
                {{ targetCharger.id }}
              </span>
              <span class="text-[9px] px-1.5 py-0.2 rounded-full font-bold bg-sky-100 text-sky-800 border border-sky-200">
                {{ targetCharger.powerKw }}kW 급속
              </span>
              <button
                @click="emit('open-edit-modal', targetCharger)"
                class="text-[11px] font-bold text-sky-700 bg-sky-50 hover:bg-sky-100 border border-sky-200 px-2 py-0.5 rounded-md transition-all shadow-2xs flex items-center gap-1 cursor-pointer"
                title="충전기 자산 마스터 관리 팝업 열기"
              >
                <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                <span>마스터 관리</span>
              </button>
            </div>
            <div class="flex items-center gap-2 text-[11px] text-slate-500 font-medium truncate max-w-sm">
              <span class="truncate font-semibold text-slate-700">{{ targetCharger.name }}</span>
              <span v-if="targetCharger.stationName" class="text-slate-400">|</span>
              <span v-if="targetCharger.stationName" class="truncate text-slate-500">{{ targetCharger.stationName }}</span>
            </div>
          </div>
        </div>

        <!-- 우측 연결 상태 및 연결/해제 토글 -->
        <div class="flex items-center gap-2">
          <!-- 연결 상태 배지 -->
          <div class="flex items-center gap-1.5 px-2.5 py-0.5 rounded-lg border text-xs font-mono bg-slate-50 border-slate-200">
            <span
              class="w-2 h-2 rounded-full"
              :class="targetCharger.connected ? 'bg-emerald-500 ring-2 ring-emerald-100 animate-pulse' : targetCharger.connecting ? 'bg-amber-400 animate-ping' : 'bg-slate-300'"
            ></span>
            <span class="font-bold text-[10px]" :class="targetCharger.connected ? 'text-emerald-700' : 'text-slate-500'">
              {{ targetCharger.connected ? 'ONLINE (:18080)' : targetCharger.connecting ? 'CONNECTING...' : 'OFFLINE' }}
            </span>
          </div>

          <!-- 연결/해제 토글 버튼 -->
          <button
            @click="targetCharger.connected ? store.disconnect(targetCharger.id) : store.connect(targetCharger.id)"
            class="px-2.5 py-1 text-xs font-bold rounded-lg transition-all shadow-2xs cursor-pointer flex items-center gap-1"
            :class="targetCharger.connected ? 'bg-rose-50 text-rose-700 border border-rose-300 hover:bg-rose-100' : 'bg-emerald-600 text-white hover:bg-emerald-700'"
          >
            <span v-if="targetCharger.connected">🔌 연결 해제</span>
            <span v-else>⚡ 즉시 연결</span>
          </button>
        </div>
      </div>

      <!-- 하단 실시간 상태 지표 그리드 -->
      <div class="grid grid-cols-4 gap-1.5 pt-2 text-[11px]">
        <div class="bg-slate-50 border border-slate-200/80 rounded-md px-2 py-1 flex flex-col">
          <span class="text-[9px] text-slate-400 uppercase font-mono">OCPP 상태</span>
          <span class="font-bold truncate" :class="targetCharger.status === 'Charging' ? 'text-indigo-600 font-black' : 'text-slate-800'">
            {{ targetCharger.status }}
          </span>
        </div>

        <div class="bg-slate-50 border border-slate-200/80 rounded-md px-2 py-1 flex flex-col">
          <span class="text-[9px] text-slate-400 uppercase font-mono">트랜잭션 ID</span>
          <span class="font-mono font-bold text-slate-800 truncate">
            {{ targetCharger.transactionId || '-' }}
          </span>
        </div>

        <div class="bg-slate-50 border border-slate-200/80 rounded-md px-2 py-1 flex flex-col">
          <span class="text-[9px] text-slate-400 uppercase font-mono">누적 계량 (Wh)</span>
          <span class="font-mono font-bold text-slate-800 truncate">
            {{ targetCharger.meterValueWh.toLocaleString() }}
          </span>
        </div>

        <div class="bg-slate-50 border border-slate-200/80 rounded-md px-2 py-1 flex flex-col">
          <span class="text-[9px] text-slate-400 uppercase font-mono">배터리 SoC</span>
          <span class="font-mono font-bold text-sky-700 truncate">
            {{ targetCharger.soc }} %
          </span>
        </div>
      </div>
    </div>

    <!-- 2. 용도별 4대 메인 탭 네비게이션 -->
    <div class="bg-white rounded-xl border border-slate-200 p-1.5 shadow-2xs flex-shrink-0">
      <div class="grid grid-cols-4 gap-1">
        <!-- 탭 1: 전문 제어 & 자동화 -->
        <button
          @click="activeTab = 'message'"
          class="py-2 px-2 text-xs font-bold rounded-lg transition-all cursor-pointer flex items-center justify-center gap-1.5 text-center"
          :class="activeTab === 'message' ? 'bg-sky-50 text-sky-700 border border-sky-300 shadow-2xs' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50 border border-transparent'"
        >
          <span>📨</span>
          <span class="truncate">전문 제어</span>
        </button>

        <!-- 탭 2: 충전 시나리오 러너 -->
        <button
          @click="activeTab = 'scenario'"
          class="py-2 px-2 text-xs font-bold rounded-lg transition-all cursor-pointer flex items-center justify-center gap-1.5 text-center"
          :class="activeTab === 'scenario' ? 'bg-indigo-50 text-indigo-700 border border-indigo-300 shadow-2xs' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50 border border-transparent'"
        >
          <span>🚀</span>
          <span class="truncate">시나리오</span>
        </button>

        <!-- 탭 3: WBS 과제 검증 -->
        <button
          @click="activeTab = 'verify'"
          class="py-2 px-2 text-xs font-bold rounded-lg transition-all cursor-pointer flex items-center justify-center gap-1.5 text-center"
          :class="activeTab === 'verify' ? 'bg-emerald-50 text-emerald-700 border border-emerald-300 shadow-2xs' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50 border border-transparent'"
        >
          <span>🧪</span>
          <span class="truncate">과제 검증</span>
        </button>

        <!-- 탭 4: 기기 스펙 & 커넥터 -->
        <button
          @click="activeTab = 'spec'"
          class="py-2 px-2 text-xs font-bold rounded-lg transition-all cursor-pointer flex items-center justify-center gap-1.5 text-center"
          :class="activeTab === 'spec' ? 'bg-slate-100 text-slate-800 border border-slate-300 shadow-2xs' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50 border border-transparent'"
        >
          <span>📋</span>
          <span class="truncate">기기 스펙</span>
        </button>
      </div>
    </div>

    <!-- 3. 메인 콘텐츠 영역 (탭별 내용) -->

    <!-- TAB 1: 전문 제어 & 자동화 (Message & Automation) -->
    <div v-if="activeTab === 'message'" class="space-y-3">
      <!-- 1-1. 충전기 전문 자동화 제어 패널 (수직 행 배치) -->
      <div class="bg-white rounded-xl border border-slate-200 shadow-2xs overflow-hidden font-sans">
        <div
          @click="isAutoPanelExpanded = !isAutoPanelExpanded"
          class="px-3.5 py-2.5 bg-slate-50 border-b border-slate-200/80 flex items-center justify-between cursor-pointer hover:bg-slate-100/70 transition-colors select-none"
        >
          <div class="flex items-center gap-2 flex-wrap">
            <span class="text-sm">⚙️</span>
            <span class="text-xs font-bold text-slate-900">전문 자동화 제어</span>
            <span class="text-[10px] px-1.5 py-0.2 rounded font-mono font-bold bg-slate-200/70 text-slate-700">
              {{ targetCharger.id }}
            </span>

            <!-- 요약 칩 배지 -->
            <div class="flex items-center gap-1 text-[10px] font-mono">
              <span
                class="px-1.5 py-0.2 rounded font-semibold border"
                :class="targetCharger.autoHeartbeat ? 'bg-sky-50 text-sky-700 border-sky-200' : 'bg-slate-100 text-slate-400 border-slate-200'"
              >
                💓 HB: {{ targetCharger.autoHeartbeat ? `${targetCharger.heartbeatIntervalSec || 60}s` : 'OFF' }}
              </span>
              <span
                class="px-1.5 py-0.2 rounded font-semibold border"
                :class="targetCharger.autoBootOnConnect ? 'bg-sky-50 text-sky-700 border-sky-200' : 'bg-slate-100 text-slate-400 border-slate-200'"
              >
                🚀 기동: {{ targetCharger.autoBootOnConnect ? 'ON' : 'OFF' }}
              </span>
              <span
                class="px-1.5 py-0.2 rounded font-semibold border"
                :class="targetCharger.autoReply ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-100 text-slate-400 border-slate-200'"
              >
                🤖 응답: {{ targetCharger.autoReply ? 'ON' : 'OFF' }}
              </span>
              <span
                class="px-1.5 py-0.2 rounded font-semibold border"
                :class="targetCharger.autoMeterValues ? 'bg-indigo-50 text-indigo-700 border-indigo-200' : 'bg-slate-100 text-slate-400 border-slate-200'"
              >
                📊 계측: {{ targetCharger.autoMeterValues ? 'ON' : 'OFF' }}
              </span>
            </div>
          </div>

          <div class="flex items-center gap-1.5 text-[11px] font-mono" @click.stop>
            <button
              type="button"
              @click="handleManualPreset(targetCharger.id)"
              class="px-2 py-0.5 rounded border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 font-bold cursor-pointer transition-colors shadow-2xs"
              title="이 충전기의 모든 자동화(Heartbeat, Auto-Boot, Auto-Reply, Auto-Meter) 일괄 정지"
            >
              🛑 전체OFF
            </button>
            <button
              type="button"
              @click="handleAutoPreset(targetCharger.id)"
              class="px-2 py-0.5 rounded border border-sky-200 bg-sky-50 text-sky-700 hover:bg-sky-100 font-bold cursor-pointer transition-colors shadow-2xs"
              title="이 충전기의 표준 자동화(Heartbeat 60s, Auto-Boot, Auto-Reply) 일괄 가동"
            >
              ⚡ 전체ON
            </button>
            <button
              type="button"
              @click="isAutoPanelExpanded = !isAutoPanelExpanded"
              class="px-1.5 py-0.5 rounded border border-slate-200 text-slate-500 hover:text-slate-800 hover:bg-white text-[10px] font-bold transition-colors cursor-pointer"
            >
              {{ isAutoPanelExpanded ? '▲ 접기' : '▼ 펼침' }}
            </button>
          </div>
        </div>

        <!-- 세로 목록 본문 (4대 전문 자동화 수직 행) -->
        <div v-if="isAutoPanelExpanded" class="p-2.5 space-y-2 text-xs bg-white">
          <!-- 1. 자동 Heartbeat 수직 행 -->
          <div
            class="p-2.5 rounded-lg border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-2"
            :class="targetCharger.autoHeartbeat ? 'bg-sky-50/40 border-sky-200' : 'bg-slate-50 border-slate-200'"
          >
            <div class="flex items-start gap-2.5 min-w-0">
              <span class="text-base flex-shrink-0 mt-0.5">💓</span>
              <div class="flex flex-col min-w-0">
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">자동 Heartbeat 주기 발송</span>
                  <span
                    class="text-[9px] font-mono px-1.5 py-0.2 rounded font-bold"
                    :class="targetCharger.autoHeartbeat ? 'bg-sky-100 text-sky-800' : 'bg-slate-200 text-slate-500'"
                  >
                    {{ targetCharger.autoHeartbeat ? 'ACTIVE' : 'STOPPED' }}
                  </span>
                </div>
                <span class="text-[11px] text-slate-500 truncate">
                  연결 상태에서 백그라운드 타이머로 Heartbeat(생존 신호) 주기 자동 발송
                </span>
              </div>
            </div>

            <div class="flex items-center gap-1.5 self-end sm:self-center flex-shrink-0">
              <div class="flex items-center gap-1 font-mono text-[11px]">
                <span class="text-slate-500">주기:</span>
                <select
                  :value="targetCharger.heartbeatIntervalSec || 60"
                  @change="(e: any) => store.setHeartbeatInterval(Number(e.target.value), targetCharger.id)"
                  class="bg-white border border-slate-200 rounded px-1.5 py-1 text-xs font-mono font-semibold text-slate-800 focus:outline-hidden focus:ring-1 focus:ring-sky-500"
                >
                  <option :value="10">10초</option>
                  <option :value="30">30초</option>
                  <option :value="60">60초 (표준)</option>
                  <option :value="120">120초 (2분)</option>
                  <option :value="300">300초 (5분)</option>
                  <option :value="600">600초 (10분)</option>
                </select>
              </div>

              <button
                @click="store.toggleAutoHeartbeat(targetCharger.id)"
                class="px-2.5 py-1 rounded-md text-[11px] font-mono font-bold transition-all cursor-pointer border shadow-2xs"
                :class="targetCharger.autoHeartbeat ? 'bg-sky-600 text-white border-sky-600 hover:bg-sky-700' : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-100'"
              >
                {{ targetCharger.autoHeartbeat ? '● ON' : '○ OFF' }}
              </button>

              <button
                @click="store.sendHeartbeat(targetCharger.id)"
                :disabled="!targetCharger.connected"
                class="px-2 py-1 rounded-md text-[11px] font-mono font-semibold transition-all border shadow-2xs"
                :class="targetCharger.connected ? 'bg-white text-slate-700 border-slate-200 hover:bg-slate-100 cursor-pointer' : 'bg-slate-100 text-slate-400 border-slate-200 cursor-not-allowed'"
              >
                1회 전송
              </button>
            </div>
          </div>

          <!-- 2. 연결 시 기동전문 자동 발송 -->
          <div
            class="p-2.5 rounded-lg border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-2"
            :class="targetCharger.autoBootOnConnect ? 'bg-sky-50/40 border-sky-200' : 'bg-slate-50 border-slate-200'"
          >
            <div class="flex items-start gap-2.5 min-w-0">
              <span class="text-base flex-shrink-0 mt-0.5">🚀</span>
              <div class="flex flex-col min-w-0">
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">연결 시 기동전문 자동 발송 (Auto-Boot)</span>
                  <span
                    class="text-[9px] font-mono px-1.5 py-0.2 rounded font-bold"
                    :class="targetCharger.autoBootOnConnect ? 'bg-sky-100 text-sky-800' : 'bg-slate-200 text-slate-500'"
                  >
                    {{ targetCharger.autoBootOnConnect ? 'ACTIVE' : 'STOPPED' }}
                  </span>
                </div>
                <span class="text-[11px] text-slate-500 truncate">
                  WebSocket 연결 직후 BootNotification 및 StatusNotification 기동 시퀀스 자동 순차 발송
                </span>
              </div>
            </div>

            <div class="flex items-center gap-2 self-end sm:self-center flex-shrink-0">
              <button
                @click="store.toggleAutoBootOnConnect(targetCharger.id)"
                class="px-2.5 py-1 rounded-md text-[11px] font-mono font-bold transition-all cursor-pointer border shadow-2xs"
                :class="targetCharger.autoBootOnConnect ? 'bg-sky-600 text-white border-sky-600 hover:bg-sky-700' : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-100'"
              >
                {{ targetCharger.autoBootOnConnect ? '● ON' : '○ OFF' }}
              </button>
            </div>
          </div>

          <!-- 3. 관제 CALL 자동응답 -->
          <div
            class="p-2.5 rounded-lg border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-2"
            :class="targetCharger.autoReply ? 'bg-emerald-50/40 border-emerald-200' : 'bg-slate-50 border-slate-200'"
          >
            <div class="flex items-start gap-2.5 min-w-0">
              <span class="text-base flex-shrink-0 mt-0.5">🤖</span>
              <div class="flex flex-col min-w-0">
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">관제 요청(CALL) 자동응답 (Auto-Reply)</span>
                  <span
                    class="text-[9px] font-mono px-1.5 py-0.2 rounded font-bold"
                    :class="targetCharger.autoReply ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'"
                  >
                    {{ targetCharger.autoReply ? 'ACCEPTED 회신' : '무응답(타임아웃 검증)' }}
                  </span>
                </div>
                <span class="text-[11px] text-slate-500 truncate">
                  관제 원격 명령(RemoteStart, Reset 등) 수신 시 Accepted 자동 회신
                </span>
              </div>
            </div>

            <div class="flex items-center gap-2 self-end sm:self-center flex-shrink-0">
              <button
                @click="store.toggleAutoReply(targetCharger.id)"
                class="px-2.5 py-1 rounded-md text-[11px] font-mono font-bold transition-all cursor-pointer border shadow-2xs"
                :class="targetCharger.autoReply ? 'bg-emerald-600 text-white border-emerald-600 hover:bg-emerald-700' : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-100'"
              >
                {{ targetCharger.autoReply ? '● ON' : '○ OFF' }}
              </button>
            </div>
          </div>

          <!-- 4. 계측치(MeterValues) 주기 발송 -->
          <div
            class="p-2.5 rounded-lg border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-2"
            :class="targetCharger.autoMeterValues ? 'bg-indigo-50/40 border-indigo-200' : 'bg-slate-50 border-slate-200'"
          >
            <div class="flex items-start gap-2.5 min-w-0">
              <span class="text-base flex-shrink-0 mt-0.5">📊</span>
              <div class="flex flex-col min-w-0">
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">충전 중 계측치(MeterValues) 주기 발송</span>
                  <span
                    class="text-[9px] font-mono px-1.5 py-0.2 rounded font-bold"
                    :class="targetCharger.autoMeterValues ? 'bg-indigo-100 text-indigo-800' : 'bg-slate-200 text-slate-500'"
                  >
                    {{ targetCharger.autoMeterValues ? 'ACTIVE' : 'STOPPED' }}
                  </span>
                </div>
                <span class="text-[11px] text-slate-500 truncate">
                  충전 진행 중(Charging)일 때 누적 계량치(Wh, SoC) 자동 발송
                </span>
              </div>
            </div>

            <div class="flex items-center gap-1.5 self-end sm:self-center flex-shrink-0">
              <div class="flex items-center gap-1 font-mono text-[11px]">
                <span class="text-slate-500">주기:</span>
                <select
                  :value="targetCharger.autoMeterIntervalSec || 10"
                  @change="(e: any) => store.setAutoMeterInterval(Number(e.target.value), targetCharger.id)"
                  class="bg-white border border-slate-200 rounded px-1.5 py-1 text-xs font-mono font-semibold text-slate-800 focus:outline-hidden focus:ring-1 focus:ring-sky-500"
                >
                  <option :value="5">5초 (고속)</option>
                  <option :value="10">10초 (표준)</option>
                  <option :value="30">30초</option>
                  <option :value="60">60초 (1분)</option>
                </select>
              </div>

              <button
                @click="store.toggleAutoMeterValues(targetCharger.id)"
                class="px-2.5 py-1 rounded-md text-[11px] font-mono font-bold transition-all cursor-pointer border shadow-2xs"
                :class="targetCharger.autoMeterValues ? 'bg-indigo-600 text-white border-indigo-600 hover:bg-indigo-700' : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-100'"
              >
                {{ targetCharger.autoMeterValues ? '● ON' : '○ OFF' }}
              </button>
            </div>
          </div>
        </div>
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

    <!-- TAB 2: 충전 라이프사이클 시나리오 러너 (Scenario) -->
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

    <!-- TAB 4: 기기 스펙 & 커넥터 정보 (Spec) -->
    <div v-else-if="activeTab === 'spec'" class="bg-white rounded-xl border border-slate-200 p-4 shadow-2xs space-y-4">
      <div class="flex items-center justify-between pb-3 border-b border-slate-100">
        <div class="flex items-center gap-2">
          <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
          <h3 class="text-xs font-bold text-slate-900">충전기 자산 마스터 & 듀얼 커넥터 스펙</h3>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 font-bold">
            CSMS Lite 표준
          </span>
          <button
            @click="emit('open-edit-modal', targetCharger)"
            class="text-[11px] font-bold text-sky-700 bg-sky-50 hover:bg-sky-100 border border-sky-200 px-2 py-0.5 rounded-md transition-all shadow-2xs flex items-center gap-1 cursor-pointer"
          >
            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <span>마스터 편집</span>
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
        <!-- 1. 기기 식별자 및 소속 -->
        <div class="p-3 rounded-lg bg-slate-50 border border-slate-200 space-y-1.5">
          <span class="text-[11px] font-bold text-slate-500">자산 식별자 및 소속</span>
          <div class="font-mono text-xs text-slate-800 space-y-1">
            <div class="flex justify-between">
              <span class="text-slate-500">결합 ID (chargeBoxId):</span>
              <b class="text-sky-700">{{ targetCharger.id }}</b>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">충전소 ID / 충전기 ID:</span>
              <b>{{ targetCharger.stationId }} / {{ targetCharger.chargerId }}</b>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">소속 충전소명:</span>
              <span class="font-sans font-semibold truncate">{{ targetCharger.stationName || '판교 LS R&D 센터 충전소' }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">운영 법인:</span>
              <span class="font-sans font-semibold">{{ targetCharger.corpName || 'LS E-Link' }}</span>
            </div>
          </div>
        </div>

        <!-- 2. 하드웨어 및 펌웨어 -->
        <div class="p-3 rounded-lg bg-slate-50 border border-slate-200 space-y-1.5">
          <span class="text-[11px] font-bold text-slate-500">하드웨어 & 펌웨어 사양</span>
          <div class="font-mono text-xs text-slate-800 space-y-1">
            <div class="flex justify-between">
              <span class="text-slate-500">제조사 / 모델:</span>
              <b>{{ targetCharger.vendor }} / {{ targetCharger.model }}</b>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">정격 용량 / 규격:</span>
              <b class="text-emerald-700">{{ targetCharger.powerKw }} kW ({{ targetCharger.spec || '초급속 듀얼' }})</b>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">통신 프로토콜:</span>
              <span class="font-semibold">{{ targetCharger.protocol || 'OCPP 1.6-J' }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">펌웨어 버전:</span>
              <span class="text-slate-700 font-bold">{{ targetCharger.firmwareVer || '1.0.0-RELEASE' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 커넥터 듀얼건 상세 현황 -->
      <div class="space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-[11px] font-bold text-slate-700">듀얼 커넥터 (1, 2번 포트) 세부 규격</span>
          <span class="text-[10px] text-slate-400">CSMS ConnectorDetailDto 1:1 대응</span>
        </div>

        <div class="grid grid-cols-2 gap-2.5">
          <div
            v-for="conn in targetCharger.connectors"
            :key="conn.connectorId"
            class="p-3 rounded-xl border border-slate-200 bg-slate-50 space-y-2"
          >
            <div class="flex items-center justify-between pb-1.5 border-b border-slate-200/80">
              <div class="flex items-center gap-1.5">
                <span
                  class="w-5 h-5 rounded-md flex items-center justify-center font-mono font-bold text-[10px] text-white"
                  :class="conn.connectorId === 1 ? 'bg-sky-600' : 'bg-indigo-600'"
                >
                  {{ conn.connectorId }}
                </span>
                <span class="text-xs font-bold text-slate-800">커넥터 #{{ conn.connectorId }}</span>
              </div>
              <span
                class="text-[10px] font-mono px-2 py-0.5 rounded-full font-bold"
                :class="conn.status === 'Charging' ? 'bg-indigo-100 text-indigo-700' : 'bg-emerald-100 text-emerald-700'"
              >
                {{ conn.status }}
              </span>
            </div>

            <div class="text-[11px] font-mono text-slate-600 space-y-1">
              <div class="flex justify-between">
                <span class="text-slate-400 font-sans">타입:</span>
                <span class="font-bold text-slate-800">{{ conn.type || 'DC콤보1 (CCS1)' }}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-slate-400 font-sans">최대 출력:</span>
                <span class="font-bold text-sky-700">{{ conn.maxPowerKw || Math.round(targetCharger.powerKw / 2) }} kW ({{ conn.voltageV || 800 }}V, {{ conn.currentA || 250 }}A)</span>
              </div>
              <div class="flex justify-between">
                <span class="text-slate-400 font-sans">배정차량:</span>
                <span class="font-sans font-medium text-slate-700">{{ conn.carModel || (conn.connectorId === 1 ? 'IONIQ 5 (현대)' : 'EV6 (기아)') }}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-slate-400 font-sans">배정 RFID:</span>
                <span class="font-bold text-slate-700">{{ conn.userTag || (conn.connectorId === 1 ? 'CARD-CORP-9901' : 'CARD-USER-1001') }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
