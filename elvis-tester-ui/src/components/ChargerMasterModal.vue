<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useVirtualCharger } from '../composables/useVirtualCharger'
import type { ChargerInstance, ConnectorState, OcppChargePointStatus } from '../types/ocpp'

const props = defineProps<{
  charger: ChargerInstance | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'saved', charger: ChargerInstance): void
}>()

const store = useVirtualCharger()

// 활성화 탭: 'identity' (기본 및 소속 자산) | 'spec' (기기 HW 및 펌웨어) | 'connectors' (커넥터 상세 스펙) | 'network' (통신 및 테스터 설정)
const activeTab = ref<'identity' | 'spec' | 'connectors' | 'network'>('identity')

// 1. 기본 및 자산 식별자
const name = ref('')
const stationId = ref('')
const chargerId = ref('')
const stationName = ref('')
const corpName = ref('')
const addr = ref('')
const useYn = ref('Y')

// 2. HW 및 펌웨어 사양
const vendor = ref('')
const model = ref('')
const powerKw = ref(350)
const spec = ref('')
const protocol = ref('OCPP 1.6-J')
const firmwareVer = ref('1.0.0-RELEASE')

// 3. 듀얼 커넥터 상세 스펙 (기본 2개)
interface ConnectorDetailModel {
  connectorId: number
  type: string
  maxPowerKw: number
  voltageV: number
  currentA: number
  status: OcppChargePointStatus
  carModel: string
  userTag: string
  accumulatedKwh: number
}

const connector1 = ref<ConnectorDetailModel>({
  connectorId: 1,
  type: 'DC콤보1 (CCS1)',
  maxPowerKw: 175,
  voltageV: 800,
  currentA: 250,
  status: 'Available',
  carModel: 'IONIQ 5 (현대)',
  userTag: 'CARD-CORP-9901',
  accumulatedKwh: 5240.5
})

const connector2 = ref<ConnectorDetailModel>({
  connectorId: 2,
  type: 'DC콤보1 (CCS1)',
  maxPowerKw: 175,
  voltageV: 800,
  currentA: 250,
  status: 'Available',
  carModel: 'EV6 (기아)',
  userTag: 'CARD-USER-1001',
  accumulatedKwh: 3410.2
})

// 4. 통신 및 테스터 설정
const wsUrl = ref('')
const heartbeatIntervalSec = ref(60)
const autoHeartbeat = ref(true)
const autoBootOnConnect = ref(true)
const autoReply = ref(true)
const autoMeterValues = ref(true)

// 결합 식별자 계산 (ST_ID 6자리 + CP_ID 2자리 = 8자리)
const computedChargeBoxId = computed(() => {
  const st = (stationId.value || '000000').padStart(6, '0').slice(-6)
  const cp = (chargerId.value || '01').padStart(2, '0').slice(-2)
  return `${st}${cp}`
})

watch(
  () => props.charger,
  (c) => {
    if (c) {
      name.value = c.name || ''
      stationId.value = c.stationId || '000000'
      chargerId.value = c.chargerId || '01'
      stationName.value = c.stationName || '판교 LS R&D 센터 충전소'
      corpName.value = c.corpName || 'LS E-Link'
      addr.value = c.addr || '경기도 성남시 분당구 판교로 255번길 21'
      useYn.value = c.useYn || 'Y'

      vendor.value = c.vendor || 'LS E-Link'
      model.value = c.model || 'ELVIS-HPC-350K'
      powerKw.value = c.powerKw || 350
      spec.value = c.spec || `${powerKw.value}kW 초급속 듀얼 (DC콤보1)`
      protocol.value = c.protocol || 'OCPP 1.6-J'
      firmwareVer.value = c.firmwareVer || '1.0.0-RELEASE'

      wsUrl.value = c.wsUrl || 'ws://127.0.0.1:18080/ocpp'
      heartbeatIntervalSec.value = c.heartbeatIntervalSec || 60
      autoHeartbeat.value = c.autoHeartbeat !== false
      autoBootOnConnect.value = c.autoBootOnConnect !== false
      autoReply.value = c.autoReply !== false
      autoMeterValues.value = c.autoMeterValues !== false

      // 커넥터 1, 2 동기화
      const conn1 = c.connectors?.find(cn => cn.connectorId === 1)
      if (conn1) {
        connector1.value = {
          connectorId: 1,
          type: conn1.type || 'DC콤보1 (CCS1)',
          maxPowerKw: conn1.maxPowerKw || Math.round(c.powerKw / 2),
          voltageV: conn1.voltageV || 800,
          currentA: conn1.currentA || 250,
          status: conn1.status || 'Available',
          carModel: conn1.carModel || 'IONIQ 5 (현대)',
          userTag: conn1.userTag || 'CARD-CORP-9901',
          accumulatedKwh: conn1.accumulatedKwh || 5240.5
        }
      }

      const conn2 = c.connectors?.find(cn => cn.connectorId === 2)
      if (conn2) {
        connector2.value = {
          connectorId: 2,
          type: conn2.type || 'DC콤보1 (CCS1)',
          maxPowerKw: conn2.maxPowerKw || Math.round(c.powerKw / 2),
          voltageV: conn2.voltageV || 800,
          currentA: conn2.currentA || 250,
          status: conn2.status || 'Available',
          carModel: conn2.carModel || 'EV6 (기아)',
          userTag: conn2.userTag || 'CARD-USER-1001',
          accumulatedKwh: conn2.accumulatedKwh || 3410.2
        }
      }
    }
  },
  { immediate: true }
)

function handleResetDefaults() {
  if (!confirm('충전기 마스터 정보를 기본 추천 프리셋으로 초기화하시겠습니까?')) return
  vendor.value = 'LS E-Link'
  model.value = 'ELVIS-HPC-350K'
  powerKw.value = 350
  spec.value = '350kW 초급속 듀얼 (DC콤보1)'
  protocol.value = 'OCPP 1.6-J'
  firmwareVer.value = '1.0.0-RELEASE'
  useYn.value = 'Y'
  wsUrl.value = 'ws://127.0.0.1:18080/ocpp'
  heartbeatIntervalSec.value = 60
  autoHeartbeat.value = true
  autoBootOnConnect.value = true
  autoReply.value = true
  autoMeterValues.value = true
}

function handleSave() {
  if (!props.charger) return

  const updatedConnectors: ConnectorState[] = [
    {
      connectorId: 1,
      status: connector1.value.status,
      type: connector1.value.type,
      maxPowerKw: connector1.value.maxPowerKw,
      voltageV: connector1.value.voltageV,
      currentA: connector1.value.currentA,
      carModel: connector1.value.carModel,
      userTag: connector1.value.userTag,
      accumulatedKwh: connector1.value.accumulatedKwh,
      transactionId: props.charger.connectors?.find(c => c.connectorId === 1)?.transactionId || null
    },
    {
      connectorId: 2,
      status: connector2.value.status,
      type: connector2.value.type,
      maxPowerKw: connector2.value.maxPowerKw,
      voltageV: connector2.value.voltageV,
      currentA: connector2.value.currentA,
      carModel: connector2.value.carModel,
      userTag: connector2.value.userTag,
      accumulatedKwh: connector2.value.accumulatedKwh,
      transactionId: props.charger.connectors?.find(c => c.connectorId === 2)?.transactionId || null
    }
  ]

  const updates: Partial<ChargerInstance> = {
    name: name.value.trim() || `충전기 ${computedChargeBoxId.value}`,
    stationId: stationId.value.trim(),
    chargerId: chargerId.value.trim(),
    stationName: stationName.value.trim(),
    corpName: corpName.value.trim(),
    addr: addr.value.trim(),
    useYn: useYn.value,
    vendor: vendor.value.trim(),
    model: model.value.trim(),
    powerKw: Number(powerKw.value) || 350,
    spec: spec.value.trim() || `${powerKw.value}kW 급속`,
    protocol: protocol.value.trim(),
    firmwareVer: firmwareVer.value.trim(),
    wsUrl: wsUrl.value.trim(),
    heartbeatIntervalSec: Number(heartbeatIntervalSec.value) || 60,
    autoHeartbeat: autoHeartbeat.value,
    autoBootOnConnect: autoBootOnConnect.value,
    autoReply: autoReply.value,
    autoMeterValues: autoMeterValues.value,
    connectors: updatedConnectors
  }

  store.updateCharger(props.charger.id, updates)

  emit('saved', {
    ...props.charger,
    ...updates
  })
  emit('close')
}
</script>

<template>
  <div v-if="charger" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs select-none p-4">
    <div class="bg-white rounded-xl shadow-2xl border border-slate-200 w-full max-w-2xl overflow-hidden flex flex-col max-h-[90vh] animate-in fade-in zoom-in-95 duration-150">
      
      <!-- 1. 모달 상단 헤더 -->
      <div class="px-5 py-3.5 bg-slate-50 border-b border-slate-200 flex items-center justify-between flex-shrink-0">
        <div class="flex items-center gap-2.5">
          <div class="w-7 h-7 rounded-lg bg-sky-100 text-sky-700 flex items-center justify-center">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h3 class="text-sm font-bold text-slate-900 tracking-tight">충전기 자산 마스터 관리</h3>
              <span class="text-xs font-mono font-bold text-sky-700 bg-sky-50 px-2 py-0.5 rounded border border-sky-200">
                {{ charger.id }}
              </span>
              <span
                class="text-[10px] font-bold px-2 py-0.5 rounded-full border"
                :class="charger.connected ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-100 text-slate-600 border-slate-200'"
              >
                {{ charger.connected ? 'ONLINE' : 'OFFLINE' }}
              </span>
            </div>
            <p class="text-[11px] text-slate-500">CSMS Lite 자산 규격 (S26-API-0020-DE) 1:1 매핑 데이터 관리</p>
          </div>
        </div>

        <button
          @click="emit('close')"
          class="text-slate-400 hover:text-slate-600 hover:bg-slate-200/60 rounded-md p-1.5 transition-colors"
          title="닫기"
        >
          <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- 2. 탭 바 (가로 부족 시 스크롤 허용) -->
      <div class="flex items-center border-b border-slate-200 bg-slate-50 px-5 gap-1 pt-2 flex-shrink-0 text-xs font-medium overflow-x-auto scrollbar-none">
        <button
          @click="activeTab = 'identity'"
          class="pb-2.5 px-3 border-b-2 transition-all flex items-center gap-1.5 cursor-pointer"
          :class="activeTab === 'identity' ? 'border-sky-600 text-sky-700 font-bold bg-white rounded-t-md border-t border-x border-slate-200 -mb-px shadow-2xs' : 'border-transparent text-slate-500 hover:text-slate-800'"
        >
          <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
          </svg>
          기본 및 소속 자산
        </button>
        <button
          @click="activeTab = 'spec'"
          class="pb-2.5 px-3 border-b-2 transition-all flex items-center gap-1.5 cursor-pointer"
          :class="activeTab === 'spec' ? 'border-sky-600 text-sky-700 font-bold bg-white rounded-t-md border-t border-x border-slate-200 -mb-px shadow-2xs' : 'border-transparent text-slate-500 hover:text-slate-800'"
        >
          <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" />
          </svg>
          기기 HW & 펌웨어
        </button>
        <button
          @click="activeTab = 'connectors'"
          class="pb-2.5 px-3 border-b-2 transition-all flex items-center gap-1.5 cursor-pointer"
          :class="activeTab === 'connectors' ? 'border-sky-600 text-sky-700 font-bold bg-white rounded-t-md border-t border-x border-slate-200 -mb-px shadow-2xs' : 'border-transparent text-slate-500 hover:text-slate-800'"
        >
          <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
          듀얼 커넥터 상세
          <span class="text-[10px] px-1.5 py-0.2 bg-sky-100 text-sky-700 rounded-full font-bold">2개</span>
        </button>
        <button
          @click="activeTab = 'network'"
          class="pb-2.5 px-3 border-b-2 transition-all flex items-center gap-1.5 cursor-pointer"
          :class="activeTab === 'network' ? 'border-sky-600 text-sky-700 font-bold bg-white rounded-t-md border-t border-x border-slate-200 -mb-px shadow-2xs' : 'border-transparent text-slate-500 hover:text-slate-800'"
        >
          <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.111 16.404a5.5 5.5 0 017.778 0M12 20h.01m-7.08-7.071c3.904-3.905 10.236-3.905 14.141 0M1.394 9.393c5.857-5.857 15.355-5.857 21.213 0" />
          </svg>
          OCPP 통신 & 테스터
        </button>
      </div>

      <!-- 3. 모달 바디 내용 (스크롤 영역) -->
      <div class="p-5 overflow-y-auto space-y-4 text-xs flex-1">

        <!-- 탭 1: 기본 및 소속 자산 -->
        <div v-show="activeTab === 'identity'" class="space-y-4">
          <div class="p-3 bg-sky-50/60 border border-sky-200 rounded-lg flex items-center justify-between text-xs">
            <div class="flex items-center gap-2">
              <span class="font-bold text-sky-900">정규화 결합 식별자:</span>
              <span class="font-mono font-black text-sm text-sky-700 bg-white px-2.5 py-0.5 rounded border border-sky-300">
                {{ computedChargeBoxId }}
              </span>
            </div>
            <span class="text-[11px] text-sky-600 font-medium">충전소(6자리) + 충전기(2자리) = 8자리 표준</span>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">충전소 ID (ST_ID, 6자리)</label>
              <input
                v-model="stationId"
                maxlength="6"
                class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-2 font-mono text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500 font-semibold"
                placeholder="000000"
              />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">충전기 ID (CP_ID, 2자리)</label>
              <input
                v-model="chargerId"
                maxlength="2"
                class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-2 font-mono text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500 font-semibold"
                placeholder="01"
              />
            </div>
          </div>

          <div>
            <label class="block text-[11px] font-bold text-slate-700 mb-1">충전기 명칭</label>
            <input
              v-model="name"
              class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-2 text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500 font-medium"
              placeholder="예: 강남 테헤란 1호기 (350kW 초급속)"
            />
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">충전소명</label>
              <input
                v-model="stationName"
                class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-2 text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500"
                placeholder="예: 강남 테헤란 충전스테이션"
              />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">소속 운영사 (Corp)</label>
              <input
                v-model="corpName"
                class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-2 text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500"
                placeholder="예: LS E-Link"
              />
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div class="sm:col-span-2">
              <label class="block text-[11px] font-bold text-slate-700 mb-1">설치 위치 / 주소</label>
              <input
                v-model="addr"
                class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-2 text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500"
                placeholder="예: 서울특별시 강남구 테헤란로 128 지상 1층 충전구역"
              />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">운영 상태 (USE_YN)</label>
              <select
                v-model="useYn"
                class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-2 text-xs font-semibold focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500"
              >
                <option value="Y">Y (정상 운영)</option>
                <option value="N">N (점검/사용중지)</option>
              </select>
            </div>
          </div>
        </div>

        <!-- 탭 2: 기기 HW & 펌웨어 -->
        <div v-show="activeTab === 'spec'" class="space-y-4">
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">제조사 (Vendor)</label>
              <input
                v-model="vendor"
                class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-2 text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500 font-semibold"
                placeholder="예: LS E-Link"
              />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">모델명 (Model)</label>
              <input
                v-model="model"
                class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-2 text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500 font-semibold"
                placeholder="예: ELVIS-HPC-350K"
              />
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">정격 용량 (kW)</label>
              <div class="flex items-center gap-2">
                <input
                  v-model.number="powerKw"
                  type="number"
                  class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-2 font-mono text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500 font-bold"
                  placeholder="350"
                />
                <span class="text-xs font-bold text-slate-500">kW</span>
              </div>
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">기기 규격 명칭 (SPEC)</label>
              <input
                v-model="spec"
                class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-2 text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500"
                placeholder="예: 350kW 초급속 듀얼"
              />
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">통신 프로토콜 (PROTOCOL)</label>
              <select
                v-model="protocol"
                class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-2 text-xs font-semibold focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500"
              >
                <option value="OCPP 1.6-J">OCPP 1.6-J (JSON over WebSocket)</option>
                <option value="OCPP 2.0.1">OCPP 2.0.1 (차세대)</option>
              </select>
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">펌웨어 버전 (FIRMWARE_VER)</label>
              <input
                v-model="firmwareVer"
                class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-2 font-mono text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500"
                placeholder="1.0.0-RELEASE"
              />
            </div>
          </div>

          <div class="p-3 bg-slate-50 rounded-lg border border-slate-200 text-slate-600 text-[11px] space-y-1">
            <span class="font-bold text-slate-800">💡 펌웨어 및 사양 연동 안내:</span>
            <p>설정된 펌웨어 버전과 모델명은 가상 충전기 기동(`BootNotification`) 전문 발송 시 CSMS로 자동 전송됩니다.</p>
          </div>
        </div>

        <!-- 탭 3: 듀얼 커넥터 상세 스펙 (기본 2개) -->
        <div v-show="activeTab === 'connectors'" class="space-y-4">
          <div class="flex items-center justify-between text-xs text-slate-500 pb-1 border-b border-slate-200">
            <span>CSMS Lite 표준 원칙: 샘플/에뮬레이터 충전기는 <strong>기본 2개(듀얼건)</strong>로 구성됩니다.</span>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <!-- 커넥터 #1 -->
            <div class="p-3.5 bg-slate-50/70 border border-slate-200 rounded-xl space-y-3">
              <div class="flex items-center justify-between pb-2 border-b border-slate-200">
                <div class="flex items-center gap-1.5 font-bold text-xs text-slate-800">
                  <span class="w-5 h-5 rounded-full bg-sky-600 text-white flex items-center justify-center text-[10px] font-mono">1</span>
                  커넥터 #1 (포트 1)
                </div>
                <span class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-white border border-slate-200 text-slate-600">
                  {{ connector1.status }}
                </span>
              </div>

              <div>
                <label class="block text-[10px] font-bold text-slate-600 mb-1">커넥터 타입</label>
                <select
                  v-model="connector1.type"
                  class="w-full bg-white border border-slate-200 rounded px-2 py-1.5 text-xs font-medium"
                >
                  <option value="DC콤보1 (CCS1)">DC콤보1 (CCS1)</option>
                  <option value="차데모 (CHAdeMO)">차데모 (CHAdeMO)</option>
                  <option value="AC단상 5핀">AC단상 5핀 (완속)</option>
                  <option value="AC3상 7핀">AC3상 7핀</option>
                  <option value="NACS (테슬라)">NACS (북미규격)</option>
                </select>
              </div>

              <div class="grid grid-cols-3 gap-1.5">
                <div>
                  <label class="block text-[10px] text-slate-500 mb-0.5">정격 kW</label>
                  <input
                    v-model.number="connector1.maxPowerKw"
                    type="number"
                    class="w-full bg-white border border-slate-200 rounded px-2 py-1 font-mono text-xs"
                  />
                </div>
                <div>
                  <label class="block text-[10px] text-slate-500 mb-0.5">전압 (V)</label>
                  <input
                    v-model.number="connector1.voltageV"
                    type="number"
                    class="w-full bg-white border border-slate-200 rounded px-2 py-1 font-mono text-xs"
                  />
                </div>
                <div>
                  <label class="block text-[10px] text-slate-500 mb-0.5">전류 (A)</label>
                  <input
                    v-model.number="connector1.currentA"
                    type="number"
                    class="w-full bg-white border border-slate-200 rounded px-2 py-1 font-mono text-xs"
                  />
                </div>
              </div>

              <div>
                <label class="block text-[10px] text-slate-500 mb-0.5">기본 배정 차량 모델</label>
                <input
                  v-model="connector1.carModel"
                  class="w-full bg-white border border-slate-200 rounded px-2 py-1 text-xs"
                  placeholder="예: IONIQ 5 (현대)"
                />
              </div>

              <div>
                <label class="block text-[10px] text-slate-500 mb-0.5">기본 배정 회원 RFID 태그</label>
                <input
                  v-model="connector1.userTag"
                  class="w-full bg-white border border-slate-200 rounded px-2 py-1 font-mono text-xs font-semibold"
                  placeholder="CARD-CORP-9901"
                />
              </div>
            </div>

            <!-- 커넥터 #2 -->
            <div class="p-3.5 bg-slate-50/70 border border-slate-200 rounded-xl space-y-3">
              <div class="flex items-center justify-between pb-2 border-b border-slate-200">
                <div class="flex items-center gap-1.5 font-bold text-xs text-slate-800">
                  <span class="w-5 h-5 rounded-full bg-indigo-600 text-white flex items-center justify-center text-[10px] font-mono">2</span>
                  커넥터 #2 (포트 2)
                </div>
                <span class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-white border border-slate-200 text-slate-600">
                  {{ connector2.status }}
                </span>
              </div>

              <div>
                <label class="block text-[10px] font-bold text-slate-600 mb-1">커넥터 타입</label>
                <select
                  v-model="connector2.type"
                  class="w-full bg-white border border-slate-200 rounded px-2 py-1.5 text-xs font-medium"
                >
                  <option value="DC콤보1 (CCS1)">DC콤보1 (CCS1)</option>
                  <option value="차데모 (CHAdeMO)">차데모 (CHAdeMO)</option>
                  <option value="AC단상 5핀">AC단상 5핀 (완속)</option>
                  <option value="AC3상 7핀">AC3상 7핀</option>
                  <option value="NACS (테슬라)">NACS (북미규격)</option>
                </select>
              </div>

              <div class="grid grid-cols-3 gap-1.5">
                <div>
                  <label class="block text-[10px] text-slate-500 mb-0.5">정격 kW</label>
                  <input
                    v-model.number="connector2.maxPowerKw"
                    type="number"
                    class="w-full bg-white border border-slate-200 rounded px-2 py-1 font-mono text-xs"
                  />
                </div>
                <div>
                  <label class="block text-[10px] text-slate-500 mb-0.5">전압 (V)</label>
                  <input
                    v-model.number="connector2.voltageV"
                    type="number"
                    class="w-full bg-white border border-slate-200 rounded px-2 py-1 font-mono text-xs"
                  />
                </div>
                <div>
                  <label class="block text-[10px] text-slate-500 mb-0.5">전류 (A)</label>
                  <input
                    v-model.number="connector2.currentA"
                    type="number"
                    class="w-full bg-white border border-slate-200 rounded px-2 py-1 font-mono text-xs"
                  />
                </div>
              </div>

              <div>
                <label class="block text-[10px] text-slate-500 mb-0.5">기본 배정 차량 모델</label>
                <input
                  v-model="connector2.carModel"
                  class="w-full bg-white border border-slate-200 rounded px-2 py-1 text-xs"
                  placeholder="예: EV6 (기아)"
                />
              </div>

              <div>
                <label class="block text-[10px] text-slate-500 mb-0.5">기본 배정 회원 RFID 태그</label>
                <input
                  v-model="connector2.userTag"
                  class="w-full bg-white border border-slate-200 rounded px-2 py-1 font-mono text-xs font-semibold"
                  placeholder="CARD-USER-1001"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 탭 4: 통신 및 테스터 설정 -->
        <div v-show="activeTab === 'network'" class="space-y-4">
          <div>
            <div class="flex items-center justify-between mb-1">
              <label class="block text-[11px] font-bold text-slate-700">WebSocket 엔드포인트 URL</label>
              <div class="flex items-center gap-1.5">
                <button
                  type="button"
                  @click="wsUrl = 'ws://127.0.0.1:18080/ocpp'"
                  class="text-[10px] px-2 py-0.5 bg-slate-100 hover:bg-slate-200 rounded text-slate-600 font-mono transition-colors cursor-pointer"
                >
                  로컬 18080
                </button>
                <button
                  type="button"
                  @click="wsUrl = 'ws://127.0.0.1:8080/ocpp'"
                  class="text-[10px] px-2 py-0.5 bg-slate-100 hover:bg-slate-200 rounded text-slate-600 font-mono transition-colors cursor-pointer"
                >
                  기본 8080
                </button>
              </div>
            </div>
            <input
              v-model="wsUrl"
              class="w-full bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-2 font-mono text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500"
            />
          </div>

          <div class="p-3.5 rounded-xl bg-slate-50 border border-slate-200 space-y-3">
            <div class="flex items-center justify-between pb-2 border-b border-slate-200">
              <span class="text-xs font-bold text-slate-800">Heartbeat 주기 설정</span>
              <select
                v-model.number="heartbeatIntervalSec"
                class="bg-white border border-slate-200 rounded-md px-2.5 py-1 text-xs font-mono font-bold text-sky-700"
              >
                <option :value="10">10초</option>
                <option :value="30">30초</option>
                <option :value="60">60초 (기본값)</option>
                <option :value="120">120초 (2분)</option>
                <option :value="300">300초 (5분)</option>
              </select>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-2.5 pt-1 text-xs">
              <label class="flex items-center gap-2 p-2 bg-white rounded-lg border border-slate-200 cursor-pointer hover:bg-sky-50/40 transition-colors">
                <input type="checkbox" v-model="autoHeartbeat" class="rounded text-sky-600" />
                <div class="flex flex-col">
                  <span class="font-bold text-slate-800">Heartbeat 주기 발송</span>
                  <span class="text-[10px] text-slate-500">설정된 주기로 서버에 생존 신호 전송</span>
                </div>
              </label>

              <label class="flex items-center gap-2 p-2 bg-white rounded-lg border border-slate-200 cursor-pointer hover:bg-sky-50/40 transition-colors">
                <input type="checkbox" v-model="autoBootOnConnect" class="rounded text-sky-600" />
                <div class="flex flex-col">
                  <span class="font-bold text-slate-800">접속 시 자동 기동(Boot)</span>
                  <span class="text-[10px] text-slate-500">WS 연결 직후 BootNotification 발송</span>
                </div>
              </label>

              <label class="flex items-center gap-2 p-2 bg-white rounded-lg border border-slate-200 cursor-pointer hover:bg-sky-50/40 transition-colors">
                <input type="checkbox" v-model="autoReply" class="rounded text-sky-600" />
                <div class="flex flex-col">
                  <span class="font-bold text-slate-800">관제 원격명령 자동응답</span>
                  <span class="text-[10px] text-slate-500">Reset, Unlock 등 관제 명령에 즉시 응답</span>
                </div>
              </label>

              <label class="flex items-center gap-2 p-2 bg-white rounded-lg border border-slate-200 cursor-pointer hover:bg-sky-50/40 transition-colors">
                <input type="checkbox" v-model="autoMeterValues" class="rounded text-sky-600" />
                <div class="flex flex-col">
                  <span class="font-bold text-slate-800">충전 중 계측치 주기 발송</span>
                  <span class="text-[10px] text-slate-500">시나리오 충전 시 MeterValues 자동 송신</span>
                </div>
              </label>
            </div>
          </div>
        </div>

      </div>

      <!-- 4. 모달 푸터 액션 -->
      <div class="px-5 py-3 bg-slate-50 border-t border-slate-200 flex items-center justify-between flex-shrink-0">
        <button
          type="button"
          @click="handleResetDefaults"
          class="px-3 py-1.5 text-xs font-semibold rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
        >
          기본값 복원
        </button>

        <div class="flex items-center gap-2">
          <button
            type="button"
            @click="emit('close')"
            class="px-3.5 py-1.5 text-xs font-semibold rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-100 transition-colors cursor-pointer"
          >
            취소
          </button>
          <button
            type="button"
            @click="handleSave"
            class="px-4 py-1.5 text-xs font-bold rounded-lg bg-sky-600 hover:bg-sky-700 text-white shadow-2xs transition-colors cursor-pointer flex items-center gap-1.5"
          >
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            마스터 정보 저장
          </button>
        </div>
      </div>

    </div>
  </div>
</template>
