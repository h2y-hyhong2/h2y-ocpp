<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  useCsmsStore,
  getStatusMeta,
  isChargerConnected,
  getChargerConnectionMeta,
  getChargerConnectors,
  getPrimaryConnector,
  type ChargerItem,
  type ConnectorDetail
} from '@/stores/csmsStore'

const props = withDefaults(
  defineProps<{
    chargers?: ChargerItem[]
    isSampleMode?: boolean
  }>(),
  {
    chargers: undefined,
    isSampleMode: false
  }
)

const emit = defineEmits<{
  (e: 'open-detail', charger: ChargerItem, connectorId: number): void
}>()

const router = useRouter()
const store = useCsmsStore()

const isOpen = ref(false)
const menuPos = ref({ x: 0, y: 0 })
const targetChargeBoxId = ref<string | null>(null)
const fallbackCharger = ref<ChargerItem | null>(null)
const activeConnectorId = ref<number>(1)

// 3초 주기 실시간 동기화 시에도 우클릭 메뉴 내 충전기 정보를 최신 상태로 유지
const contextCharger = computed<ChargerItem | null>(() => {
  if (!targetChargeBoxId.value) return null
  const list = props.chargers ?? store.chargers
  return list.find(c => c.chargeBoxId === targetChargeBoxId.value) ?? fallbackCharger.value
})

const connectors = computed<ConnectorDetail[]>(() => getChargerConnectors(contextCharger.value))

const activeConnector = computed<ConnectorDetail | null>(() => {
  const list = connectors.value
  if (list.length === 0) return null
  return list.find(c => c.connectorId === activeConnectorId.value) ?? getPrimaryConnector(contextCharger.value) ?? list[0]
})

function open(e: MouseEvent, charger: ChargerItem, connectorId?: number) {
  e.preventDefault()
  e.stopPropagation()

  const defaultConnId = connectorId ?? getPrimaryConnector(charger)?.connectorId ?? charger.connectorId ?? 1
  targetChargeBoxId.value = charger.chargeBoxId
  fallbackCharger.value = charger
  activeConnectorId.value = defaultConnId

  if (!props.isSampleMode) {
    store.setSelectedCharger(charger, defaultConnId)
  }

  const menuWidth = 392
  const menuHeight = 600
  let posX = e.clientX
  let posY = e.clientY

  if (posX + menuWidth > window.innerWidth) {
    posX = Math.max(10, window.innerWidth - menuWidth - 14)
  }
  if (posY + menuHeight > window.innerHeight) {
    posY = Math.max(10, window.innerHeight - menuHeight - 14)
  }

  menuPos.value = { x: posX, y: posY }
  isOpen.value = true
}

function close() {
  if (isOpen.value) {
    isOpen.value = false
    targetChargeBoxId.value = null
    fallbackCharger.value = null
  }
}

function handleSelectConnector(connId: number) {
  activeConnectorId.value = connId
  if (!props.isSampleMode && contextCharger.value) {
    store.setSelectedCharger(contextCharger.value, connId)
  }
}

async function handleRemoteAction(action: string) {
  const chg = contextCharger.value
  if (!chg) return
  const connId = activeConnector.value?.connectorId ?? activeConnectorId.value ?? 1
  close()

  if (props.isSampleMode) {
    store.addToast('info', '디자인 샘플 미리보기', `[${chg.chargeBoxId}] 샘플 모드에서는 실제 원격 제어 명령을 전송하지 않습니다.`)
    return
  }

  store.setSelectedCharger(chg, connId)
  await store.sendRemoteAction(action, connId)
}

async function handleTriggerMessage(requestedMessage: string) {
  const chg = contextCharger.value
  if (!chg) return
  const connId = requestedMessage === 'Heartbeat'
    ? undefined
    : (activeConnector.value?.connectorId ?? activeConnectorId.value ?? 1)
  close()

  if (props.isSampleMode) {
    store.addToast('info', '디자인 샘플 미리보기', `[${chg.chargeBoxId}] 샘플 모드에서는 ${requestedMessage} 트리거 명령을 전송하지 않습니다.`)
    return
  }

  await store.sendTriggerMessage(chg.chargeBoxId, requestedMessage, connId)
}

function handleCopyChargerId() {
  const chg = contextCharger.value
  if (!chg) return
  navigator.clipboard?.writeText(chg.chargeBoxId)
  store.addToast('info', 'ID 복사 완료', `충전기 ID [${chg.chargeBoxId}]가 클립보드에 복사되었습니다.`)
  close()
}

function handleOpenDetail() {
  const chg = contextCharger.value
  if (!chg) return
  const connId = activeConnector.value?.connectorId ?? activeConnectorId.value ?? 1
  close()
  emit('open-detail', chg, connId)
}

function handleGoBilling() {
  close()
  router.push('/billing')
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    close()
  }
}

onMounted(() => {
  window.addEventListener('click', close)
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('click', close)
  window.removeEventListener('keydown', handleKeydown)
})

defineExpose({
  open,
  close
})
</script>

<template>
  <!-- 우클릭 플로팅 컨텍스트 메뉴 (Teleport로 최상위 body에 띄워 스크롤/패널 위로 항상 노출) -->
  <Teleport to="body">
    <div
      v-if="isOpen && contextCharger"
      class="fixed z-[9999] bg-white rounded-xl shadow-2xl border border-slate-200/90 w-[392px] max-h-[85vh] flex flex-col overflow-hidden select-none text-xs font-sans ring-1 ring-black/10"
      :style="{ top: `${menuPos.y}px`, left: `${menuPos.x}px` }"
      @click.stop
      @contextmenu.prevent
    >
      <!-- 1. 컨텍스트 메뉴 고정 헤더: 충전기 ID 및 온라인 상태 -->
      <div class="px-3.5 py-2.5 bg-slate-50 border-b border-slate-200 flex items-center justify-between flex-shrink-0 gap-2">
        <div class="flex items-center gap-2 min-w-0">
          <div class="w-7 h-7 rounded-lg bg-sky-100 text-sky-700 flex items-center justify-center font-bold text-xs shrink-0 shadow-2xs">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 13.5l10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75z" />
            </svg>
          </div>
          <div class="flex flex-col min-w-0">
            <div class="flex items-center gap-1.5">
              <span class="font-mono font-black text-sm text-slate-900 tracking-tight">
                {{ contextCharger.chargeBoxId }}
              </span>
              <span class="text-[11px] px-1.5 py-0.2 rounded-full font-bold bg-sky-100 text-sky-800 border border-sky-200 truncate max-w-[110px]">
                {{ contextCharger.spec || '급속' }}
              </span>
            </div>
            <span class="text-[12px] text-slate-500 truncate" :title="contextCharger.stationName">
              {{ contextCharger.stationName }}
            </span>
          </div>
        </div>

        <div class="flex items-center gap-1.5 shrink-0">
          <span
            class="text-[12px] font-mono px-2 py-0.5 rounded-full font-bold border inline-flex items-center gap-1"
            :class="getChargerConnectionMeta(contextCharger).badgeClass"
          >
            <span class="w-1.5 h-1.5 rounded-full" :class="getChargerConnectionMeta(contextCharger).dotClass"></span>
            <span>{{ getChargerConnectionMeta(contextCharger).label }}</span>
          </span>
          <button
            type="button"
            @click="close"
            class="text-slate-400 hover:text-slate-700 p-1 rounded-md hover:bg-slate-200/60 transition-colors cursor-pointer"
            title="닫기 (ESC)"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <!-- 2. 내부 스크롤 콘텐츠 영역 (계측 요약 + 커넥터 전환 + 스펙 + 빠른 원격제어) -->
      <div class="flex-1 overflow-y-auto divide-y divide-slate-100 text-slate-700">
        <!-- 2-1. 실시간 운전 메트릭 2x2 카드 -->
        <div class="p-3 bg-slate-50/50">
          <div class="text-[12px] font-bold text-slate-500 mb-1.5 flex items-center justify-between">
            <span class="inline-flex items-center gap-1">
              <svg class="w-3 h-3 text-slate-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z" />
              </svg>
              <span>실시간 계측 요약</span>
            </span>
            <span class="font-mono text-[11px] text-slate-400">커넥터 #{{ activeConnector?.connectorId ?? 1 }} 기준</span>
          </div>
          <div class="grid grid-cols-2 gap-1.5 text-[13px]">
            <div class="bg-white border border-slate-200 rounded-lg p-2 flex flex-col shadow-2xs">
              <span class="text-[11px] text-slate-400 font-semibold">커넥터 상태</span>
              <span
                class="font-black truncate mt-0.5 inline-flex items-center gap-1"
                :class="getStatusMeta(activeConnector?.status).key === 'charging'
                  ? 'text-blue-600'
                  : getStatusMeta(activeConnector?.status).key === 'standby'
                    ? 'text-emerald-600'
                    : getStatusMeta(activeConnector?.status).key === 'faulted'
                      ? 'text-rose-600'
                      : 'text-slate-800'"
              >
                <span class="w-1.5 h-1.5 rounded-full flex-shrink-0" :class="getStatusMeta(activeConnector?.status).dotClass"></span>
                <span>{{ getStatusMeta(activeConnector?.status).name }}</span>
              </span>
            </div>
            <div class="bg-white border border-slate-200 rounded-lg p-2 flex flex-col shadow-2xs">
              <span class="text-[11px] text-slate-400 font-semibold">순시 출력</span>
              <span class="font-mono font-bold text-sky-700 truncate mt-0.5">
                {{ (activeConnector?.powerKw ?? 0).toFixed(1) }} kW
              </span>
            </div>
            <div class="bg-white border border-slate-200 rounded-lg p-2 flex flex-col shadow-2xs">
              <span class="text-[11px] text-slate-400 font-semibold">배터리 SoC</span>
              <span class="font-mono font-bold text-emerald-600 truncate mt-0.5">
                {{ activeConnector?.socPercent ?? 0 }} %
              </span>
            </div>
            <div class="bg-white border border-slate-200 rounded-lg p-2 flex flex-col shadow-2xs">
              <span class="text-[11px] text-slate-400 font-semibold">누적 전력량</span>
              <span class="font-mono font-bold text-slate-800 truncate mt-0.5">
                {{ (activeConnector?.accumulatedKwh ?? 0).toFixed(1) }} kWh
              </span>
            </div>
          </div>
        </div>

        <!-- 2-2. 커넥터 (충전건) 현황 및 활성 채널 전환 -->
        <div class="p-3">
          <div class="flex items-center justify-between mb-1.5">
            <span class="text-[12px] font-bold text-slate-500">커넥터 (충전건) 현황</span>
            <span class="text-[11px] text-sky-600 font-medium">클릭 시 제어 대상 전환</span>
          </div>
          <div class="space-y-1.5">
            <div
              v-for="conn in connectors"
              :key="conn.connectorId"
              @click="handleSelectConnector(conn.connectorId)"
              class="p-2 rounded-lg border transition-all cursor-pointer flex items-center justify-between gap-2 shadow-2xs"
              :class="(activeConnector?.connectorId ?? 1) === conn.connectorId
                ? 'bg-sky-50/80 border-sky-300 ring-1 ring-sky-400'
                : 'bg-white border-slate-200 hover:bg-slate-50 hover:border-slate-300'"
              :title="`#${conn.connectorId} 커넥터를 제어 대상으로 선택`"
            >
              <div class="flex items-center gap-2 min-w-0">
                <span
                  class="w-5 h-5 rounded flex items-center justify-center font-mono font-bold text-[12px] text-white shrink-0"
                  :class="conn.connectorId === 1 ? 'bg-sky-600' : 'bg-indigo-600'"
                >
                  {{ conn.connectorId }}
                </span>
                <div class="flex flex-col min-w-0">
                  <div class="flex items-center gap-1.5">
                    <span class="font-bold text-[13px] text-slate-800">커넥터 #{{ conn.connectorId }}</span>
                    <span class="text-[11px] font-mono text-slate-500">
                      {{ (conn.powerKw ?? 0) > 0 ? `${conn.powerKw.toFixed(1)} kW` : (isChargerConnected(contextCharger) ? '0.0 kW' : '미연결') }}
                    </span>
                  </div>
                  <span class="text-[11px] text-slate-400 truncate font-mono">
                    <template v-if="conn.carModel || conn.userTag">
                      {{ conn.carModel || '-' }} · {{ conn.userTag || '-' }}
                    </template>
                    <template v-else>
                      {{ (conn.voltageV ?? 0).toFixed(0) }}V / {{ (conn.currentA ?? 0).toFixed(0) }}A · {{ conn.batteryTempC ?? 25 }}°C
                    </template>
                  </span>
                </div>
              </div>
              <div class="flex items-center gap-1 shrink-0">
                <span
                  class="text-[11px] px-1.5 py-0.2 rounded font-bold border inline-flex items-center gap-1"
                  :class="getStatusMeta(conn.status).badgeClass"
                >
                  <span class="w-1 h-1 rounded-full" :class="getStatusMeta(conn.status).dotClass"></span>
                  <span>{{ getStatusMeta(conn.status).name }}</span>
                </span>
                <span v-if="(activeConnector?.connectorId ?? 1) === conn.connectorId" class="w-1.5 h-1.5 rounded-full bg-sky-600"></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 2-3. 기기 스펙 & 네트워크 요약 -->
        <div class="p-3 bg-slate-50/40 text-[12px]">
          <span class="font-bold text-slate-500 block mb-1.5">기기 스펙 & 통신 정보</span>
          <div class="grid grid-cols-2 gap-x-2 gap-y-1 font-mono text-slate-600 bg-white p-2 rounded-lg border border-slate-200">
            <div class="truncate"><span class="text-slate-400">제조사:</span> {{ contextCharger.vendor || '-' }}</div>
            <div class="truncate"><span class="text-slate-400">모델명:</span> {{ contextCharger.model || '-' }}</div>
            <div class="truncate"><span class="text-slate-400">충전소:</span> {{ contextCharger.stId }}</div>
            <div class="truncate"><span class="text-slate-400">규약:</span> {{ contextCharger.protocol || 'ocpp1.6' }}</div>
            <div class="col-span-2 truncate">
              <span class="text-slate-400">최종 수신:</span> {{ contextCharger.lastHeartbeat || '-' }}
            </div>
          </div>
        </div>

        <!-- 2-4. OCPP 빠른 원격 제어 액션 -->
        <div class="p-2.5 space-y-2">
          <div class="flex items-center justify-between px-0.5">
            <span class="text-[12px] font-bold text-slate-400 uppercase tracking-wider">OCPP 빠른 원격 제어</span>
            <span class="text-[12px] font-mono font-bold text-sky-600">#{{ activeConnector?.connectorId ?? 1 }} 대상</span>
          </div>

          <div class="grid grid-cols-2 gap-1.5 font-bold text-[13px]">
            <button
              type="button"
              @click="handleRemoteAction('RemoteStartTransaction')"
              class="px-2.5 py-1.5 rounded-lg bg-sky-600 hover:bg-sky-700 text-white transition-colors flex items-center justify-center gap-1.5 shadow-2xs cursor-pointer"
              :title="`#${activeConnector?.connectorId ?? 1} 커넥터 원격 충전 시작`"
            >
              <svg class="w-3.5 h-3.5 fill-current shrink-0" viewBox="0 0 24 24"><path d="M8 5v14l11-7z" /></svg>
              <span>충전 시작</span>
            </button>
            <button
              type="button"
              @click="handleRemoteAction('RemoteStopTransaction')"
              class="px-2.5 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-700 text-white transition-colors flex items-center justify-center gap-1.5 shadow-2xs cursor-pointer"
              :title="`#${activeConnector?.connectorId ?? 1} 커넥터 원격 충전 종료`"
            >
              <svg class="w-3.5 h-3.5 fill-current shrink-0" viewBox="0 0 24 24"><path d="M6 6h12v12H6z" /></svg>
              <span>충전 종료</span>
            </button>
            <button
              type="button"
              @click="handleRemoteAction('UnlockConnector')"
              class="px-2.5 py-1.5 rounded-lg bg-slate-50 hover:bg-slate-100 text-slate-700 border border-slate-200 transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
              :title="`#${activeConnector?.connectorId ?? 1} 커넥터 잠금 해제`"
            >
              <svg class="w-3.5 h-3.5 text-slate-500 shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M8 11V7a4 4 0 118 0m-4 8v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2z" />
              </svg>
              <span>잠금 해제</span>
            </button>
            <button
              type="button"
              @click="handleRemoteAction('Reset')"
              class="px-2.5 py-1.5 rounded-lg bg-amber-50 hover:bg-amber-100 text-amber-800 border border-amber-200 transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
              title="충전기 본체 전체 리셋"
            >
              <svg class="w-3.5 h-3.5 text-amber-600 shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              <span>단말 리셋</span>
            </button>
          </div>

          <!-- 전문 즉시 보고 요청 (TriggerMessage) -->
          <div class="pt-1">
            <span class="px-0.5 text-[11px] font-bold text-slate-400 uppercase block mb-1">전문 즉시 보고 (TriggerMessage)</span>
            <div class="grid grid-cols-3 gap-1">
              <button
                type="button"
                @click="handleTriggerMessage('StatusNotification')"
                class="px-1.5 py-1 rounded-md text-[12px] font-bold bg-sky-50 text-sky-700 border border-sky-200 hover:bg-sky-100 text-center cursor-pointer transition-colors"
                title="StatusNotification 즉시 보고 요청"
              >
                상태 보고
              </button>
              <button
                type="button"
                @click="handleTriggerMessage('MeterValues')"
                class="px-1.5 py-1 rounded-md text-[12px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200 hover:bg-indigo-100 text-center cursor-pointer transition-colors"
                title="MeterValues 즉시 보고 요청"
              >
                계량 보고
              </button>
              <button
                type="button"
                @click="handleTriggerMessage('Heartbeat')"
                class="px-1.5 py-1 rounded-md text-[12px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100 text-center cursor-pointer transition-colors"
                title="Heartbeat 즉시 보고 요청"
              >
                하트비트
              </button>
            </div>
          </div>
        </div>

        <!-- 2-5. 하단 유틸리티 (상세 보기 · ID 복사 · 과금 원장) -->
        <div class="p-2 bg-slate-50 flex items-center justify-between gap-1.5">
          <button
            type="button"
            @click="handleOpenDetail"
            class="flex-1 px-2.5 py-1.5 rounded-lg text-slate-700 bg-white hover:bg-slate-100 border border-slate-200 font-bold flex items-center justify-center gap-1 transition-colors cursor-pointer shadow-2xs"
          >
            <svg class="w-3.5 h-3.5 text-sky-600 shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>상세 제어반</span>
          </button>
          <button
            type="button"
            @click="handleCopyChargerId"
            class="px-2.5 py-1.5 rounded-lg text-slate-700 bg-white hover:bg-slate-100 border border-slate-200 font-bold flex items-center gap-1 transition-colors cursor-pointer shadow-2xs"
            title="충전기 ID 복사"
          >
            <svg class="w-3.5 h-3.5 text-slate-500 shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
            </svg>
            <span>ID 복사</span>
          </button>
          <button
            type="button"
            @click="handleGoBilling"
            class="px-2.5 py-1.5 rounded-lg text-slate-700 bg-white hover:bg-slate-100 border border-slate-200 font-bold flex items-center gap-1 transition-colors cursor-pointer shadow-2xs"
            title="과금 원장 화면으로 이동"
          >
            <svg class="w-3.5 h-3.5 text-slate-500 shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <span>원장</span>
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
