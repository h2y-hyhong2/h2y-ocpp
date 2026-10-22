<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useVirtualCharger } from '../composables/useVirtualCharger'
import type { OcppLogEntry } from '../types/ocpp'

const props = defineProps<{
  logs: OcppLogEntry[]
  isSidePanel?: boolean
}>()

const emit = defineEmits<{
  (e: 'clear'): void
  (e: 'close'): void
}>()

const store = useVirtualCharger()

const filterDirection = ref<'ALL' | 'SEND' | 'RECV' | 'SYS' | 'ERR'>('ALL')
const searchQuery = ref(store.terminalSearchQuery.value || '')
const expandedLogIds = ref<Set<string>>(new Set())
const isAllExpanded = ref(false)
const copiedId = ref<string | null>(null)

watch(
  () => store.terminalSearchQuery.value,
  (newVal) => {
    searchQuery.value = newVal
  }
)

const filteredLogs = computed(() => {
  return props.logs.filter(log => {
    if (filterDirection.value !== 'ALL') {
      const dir = log.direction
      if (filterDirection.value === 'SEND' && dir !== 'SEND' && dir !== 'OUT') return false
      if (filterDirection.value === 'RECV' && dir !== 'RECV' && dir !== 'IN') return false
      if (filterDirection.value === 'SYS' && dir !== 'SYS') return false
      if (filterDirection.value === 'ERR' && dir !== 'ERR') return false
    }
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase()
      const actionMatch = log.action?.toLowerCase().includes(q)
      const rawMatch = log.raw?.toLowerCase().includes(q)
      const idMatch = log.messageId?.toLowerCase().includes(q)
      const cidMatch = log.chargerId?.toLowerCase().includes(q)
      if (!actionMatch && !rawMatch && !idMatch && !cidMatch) return false
    }
    return true
  })
})

function isPayloadOpen(id: string): boolean {
  if (isAllExpanded.value) return true
  return expandedLogIds.value.has(id)
}

function togglePayload(id: string) {
  if (isAllExpanded.value) {
    expandedLogIds.value = new Set(props.logs.map(l => l.id))
    expandedLogIds.value.delete(id)
    isAllExpanded.value = false
  } else {
    if (expandedLogIds.value.has(id)) {
      expandedLogIds.value.delete(id)
    } else {
      expandedLogIds.value.add(id)
    }
  }
}

function toggleAllExpanded() {
  isAllExpanded.value = !isAllExpanded.value
  if (isAllExpanded.value) {
    expandedLogIds.value = new Set(props.logs.map(l => l.id))
  } else {
    expandedLogIds.value.clear()
  }
}

function copyText(id: string, text: string) {
  navigator.clipboard.writeText(text)
  copiedId.value = id
  setTimeout(() => {
    if (copiedId.value === id) copiedId.value = null
  }, 1500)
}

function downloadLogs() {
  const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(props.logs, null, 2))
  const downloadAnchor = document.createElement('a')
  downloadAnchor.setAttribute('href', dataStr)
  downloadAnchor.setAttribute('download', `ocpp-logs-${Date.now()}.json`)
  document.body.appendChild(downloadAnchor)
  downloadAnchor.click()
  downloadAnchor.remove()
}

function formatJson(payload: any): string {
  if (typeof payload === 'string') {
    try {
      const parsed = JSON.parse(payload)
      return JSON.stringify(parsed, null, 2)
    } catch {
      return payload
    }
  }
  return JSON.stringify(payload, null, 2)
}

function getByteSize(payload: any): string {
  try {
    const str = typeof payload === 'string' ? payload : JSON.stringify(payload)
    const bytes = new Blob([str]).size
    if (bytes < 1024) return `${bytes} B`
    return `${(bytes / 1024).toFixed(1)} KB`
  } catch {
    return '0 B'
  }
}

function getPayloadSummary(payload: any): string {
  if (!payload) return ''
  try {
    const str = typeof payload === 'string' ? payload : JSON.stringify(payload)
    return str
  } catch {
    return String(payload)
  }
}

interface SmartChip {
  key: string
  val: string
  type: 'success' | 'danger' | 'info' | 'warn' | 'neutral'
}

function getSmartChips(log: OcppLogEntry): SmartChip[] {
  const chips: SmartChip[] = []
  if (log.direction === 'SYS') return chips

  let p = log.payload
  if (typeof p === 'string') {
    try {
      p = JSON.parse(p)
    } catch {
      return chips
    }
  }
  if (!p || typeof p !== 'object') return chips

  // 1. status 확인
  const statusVal = p.status || (p.idTagInfo && p.idTagInfo.status)
  if (statusVal) {
    const s = String(statusVal).toLowerCase()
    let chipType: SmartChip['type'] = 'danger'
    let labelVal = String(statusVal)

    if (s === 'accepted') {
      chipType = 'success'
      labelVal = '승인 (Accepted)'
    } else if (s === 'available') {
      chipType = 'success'
      labelVal = '정상대기 (Available)'
    } else if (s === 'charging') {
      chipType = 'info'
      labelVal = '충전중 (Charging)'
    } else if (s === 'preparing') {
      chipType = 'warn'
      labelVal = '준비중 (Preparing)'
    } else if (s === 'suspendedev') {
      chipType = 'warn'
      labelVal = '차량정지 (SuspendedEV)'
    } else if (s === 'suspendedevse') {
      chipType = 'warn'
      labelVal = '충전기정지 (SuspendedEVSE)'
    } else if (s === 'finishing') {
      chipType = 'warn'
      labelVal = '종료중 (Finishing)'
    } else if (s === 'pending') {
      chipType = 'warn'
      labelVal = '보류 (Pending)'
    } else if (s === 'scheduled') {
      chipType = 'warn'
      labelVal = '예약됨 (Scheduled)'
    } else if (s === 'unavailable') {
      chipType = 'neutral'
      labelVal = '사용불가 (Unavailable)'
    } else if (s === 'reserved') {
      chipType = 'neutral'
      labelVal = '예약중 (Reserved)'
    } else if (s === 'rejected') {
      chipType = 'danger'
      labelVal = '거부 (Rejected)'
    } else if (s === 'invalid') {
      chipType = 'danger'
      labelVal = '무효 (Invalid)'
    } else if (s === 'blocked') {
      chipType = 'danger'
      labelVal = '차단 (Blocked)'
    } else if (s === 'expired') {
      chipType = 'danger'
      labelVal = '만료 (Expired)'
    } else if (s === 'faulted') {
      chipType = 'danger'
      labelVal = '장애 (Faulted)'
    }

    chips.push({ key: 'status', val: labelVal, type: chipType })
  }

  // 2. transactionId
  if (p.transactionId !== undefined && p.transactionId !== null) {
    chips.push({ key: 'trnId', val: String(p.transactionId), type: 'info' })
  }

  // 3. idTag
  if (p.idTag) {
    const tagStr = String(p.idTag)
    chips.push({ key: 'idTag', val: tagStr.length > 12 ? `${tagStr.substring(0, 8)}...` : tagStr, type: 'info' })
  }

  // 4. connectorId
  if (p.connectorId !== undefined) {
    chips.push({ key: 'connector', val: String(p.connectorId), type: 'neutral' })
  }

  // 5. errorCode
  if (p.errorCode && p.errorCode !== 'NoError') {
    chips.push({ key: 'errorCode', val: String(p.errorCode), type: 'danger' })
  }

  // 6. meterStop or meterStart
  if (p.meterStop !== undefined) {
    chips.push({ key: 'meterStop', val: `${p.meterStop} Wh`, type: 'warn' })
  } else if (p.meterStart !== undefined) {
    chips.push({ key: 'meterStart', val: `${p.meterStart} Wh`, type: 'warn' })
  }

  // 7. meterValue
  if (Array.isArray(p.meterValue) && p.meterValue.length > 0) {
    const mv = p.meterValue[0]
    if (Array.isArray(mv.sampledValue) && mv.sampledValue.length > 0) {
      const sv = mv.sampledValue[0]
      chips.push({ key: 'meter', val: `${sv.value} ${sv.unit || 'Wh'}`, type: 'warn' })
    }
  }

  return chips
}
</script>

<template>
  <div class="bg-white rounded-xl border border-slate-200 shadow-2xs flex flex-col h-full overflow-hidden select-none">
    <!-- 콘솔 상단 툴바 -->
    <div class="px-3.5 py-2.5 bg-slate-50 border-b border-slate-200 flex flex-wrap items-center justify-between gap-2 flex-shrink-0">
      <div class="flex items-center gap-2">
        <div class="w-2 h-2 rounded-full bg-indigo-500 animate-pulse"></div>
        <h3 class="text-xs font-bold text-slate-900 tracking-tight">실시간 WireTap 패킷 터미널</h3>
        <span class="text-[10px] font-mono px-1.5 py-0.2 rounded font-bold bg-slate-200 text-slate-700">
          {{ filteredLogs.length }} / {{ logs.length }}
        </span>
      </div>

      <div class="flex items-center gap-1.5 text-xs flex-wrap">
        <!-- 방향 필터 탭 -->
        <div class="flex items-center bg-white border border-slate-200 rounded-md p-0.5 text-[10px]">
          <button
            v-for="f in (['ALL', 'SEND', 'RECV', 'SYS', 'ERR'] as const)"
            :key="f"
            @click="filterDirection = f"
            class="px-2 py-0.5 rounded transition-all cursor-pointer font-medium"
            :class="filterDirection === f ? 'bg-slate-900 text-white font-bold' : 'text-slate-600 hover:text-slate-900'"
          >
            {{ f === 'ALL' ? '전체' : f === 'SEND' ? '▶ SEND' : f === 'RECV' ? '◀ RECV' : f === 'SYS' ? '⚙️ SYS' : '✕ ERR' }}
          </button>
        </div>

        <!-- 액션 검색 -->
        <input
          v-model="searchQuery"
          placeholder="액션/키워드 검색..."
          class="w-28 sm:w-32 bg-white border border-slate-200 rounded-md px-2 py-1 text-[11px] font-mono text-slate-800 focus:outline-hidden focus:ring-1 focus:ring-sky-500"
        />

        <!-- 전체 펼치기 / 접기 버튼 -->
        <button
          @click="toggleAllExpanded"
          class="px-2 py-1 text-[11px] font-medium border border-slate-200 hover:border-slate-300 rounded-md bg-white transition-colors cursor-pointer flex items-center gap-1"
          :class="isAllExpanded ? 'bg-sky-50 text-sky-700 border-sky-300 font-bold' : 'text-slate-600 hover:text-slate-900'"
          :title="isAllExpanded ? '전문 전체 접기 (요약 모드로 전환)' : '전문 전체 펼치기 (상세 모드로 전환)'"
        >
          <span>{{ isAllExpanded ? '📖 전체 접기' : '📄 전체 펼치기' }}</span>
        </button>

        <!-- 로그 다운로드 버튼 -->
        <button
          @click="downloadLogs"
          class="p-1 text-slate-500 hover:text-sky-600 border border-slate-200 hover:border-sky-300 rounded-md bg-white transition-colors cursor-pointer"
          title="로그 JSON 파일로 내보내기"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
          </svg>
        </button>

        <!-- 지우기 버튼 -->
        <button
          @click="emit('clear')"
          class="px-2 py-1 text-[11px] text-slate-500 hover:text-rose-600 border border-slate-200 hover:border-rose-300 rounded-md bg-white transition-colors cursor-pointer"
          title="전체 패킷 로그 삭제"
        >
          지우기
        </button>

        <!-- 닫기 버튼 (사이드패널 시) -->
        <button
          v-if="isSidePanel"
          @click="emit('close')"
          class="p-1 text-slate-400 hover:text-slate-600 border border-slate-200 rounded-md bg-white transition-colors cursor-pointer"
          title="터미널 닫기"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 패킷 스트림 리스트 (2줄 아코디언 카드 스트림) -->
    <div class="flex-1 overflow-y-auto p-2 bg-slate-50/60 font-mono text-[11px] min-h-0">
      <!-- 빈 상태 화면 -->
      <div
        v-if="filteredLogs.length === 0"
        class="h-full flex flex-col items-center justify-center text-slate-400 text-xs py-16 gap-2"
      >
        <svg class="w-8 h-8 text-slate-300" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
        </svg>
        <span class="font-medium">수신/발송된 실시간 패킷 로그가 없습니다.</span>
        <span class="text-[11px] text-slate-400">충전기 연결 또는 시나리오 실행 시 실시간으로 패킷 카드가 기록됩니다.</span>
      </div>

      <!-- 2줄 카드 리스트 -->
      <div
        v-for="log in filteredLogs"
        :key="log.id"
        class="bg-white rounded-lg border border-slate-200 shadow-2xs mb-2 transition-all hover:border-slate-300 hover:shadow-xs overflow-hidden"
        :class="{
          'border-l-4 border-l-sky-500': log.direction === 'SEND' || log.direction === 'OUT',
          'border-l-4 border-l-emerald-500': log.direction === 'RECV' || log.direction === 'IN',
          'border-l-4 border-l-violet-500': log.direction === 'SYS',
          'border-l-4 border-l-rose-500': log.direction === 'ERR',
          'ring-1 ring-sky-400/50': isPayloadOpen(log.id)
        }"
      >
        <!-- 1번째 줄: 메타 정보 헤더 (클릭 시 펼치기/접기 토글) -->
        <div
          @click="togglePayload(log.id)"
          class="flex items-center justify-between px-3 py-2 cursor-pointer bg-white hover:bg-slate-50/70 transition-colors gap-2"
        >
          <div class="flex items-center gap-1.5 flex-wrap overflow-hidden">
            <!-- 1. 타임스탬프 -->
            <span class="text-slate-500 font-mono text-[10px] flex-shrink-0 bg-slate-100/80 px-1.5 py-0.5 rounded border border-slate-200/80 whitespace-nowrap">
              {{ log.timestamp }}
            </span>

            <!-- 2. 방향 배지 -->
            <span
              class="px-1.5 py-0.5 rounded text-[9px] font-bold tracking-tight uppercase flex-shrink-0 font-mono"
              :class="{
                'bg-sky-100 text-sky-800 border border-sky-200': log.direction === 'SEND' || log.direction === 'OUT',
                'bg-emerald-100 text-emerald-800 border border-emerald-200': log.direction === 'RECV' || log.direction === 'IN',
                'bg-violet-100 text-violet-800 border border-violet-200': log.direction === 'SYS',
                'bg-rose-100 text-rose-800 border border-rose-200': log.direction === 'ERR'
              }"
            >
              {{ (log.direction === 'SEND' || log.direction === 'OUT') ? '▶ SEND' : (log.direction === 'RECV' || log.direction === 'IN') ? '◀ RECV' : log.direction === 'SYS' ? '⚙️ SYS' : '✕ ERR' }}
            </span>

            <!-- 3. 메시지 타입 (CALL / RESULT / ERROR) -->
            <span
              v-if="log.messageType"
              class="px-1.5 py-0.5 rounded text-[9px] font-bold font-mono flex-shrink-0"
              :class="{
                'bg-indigo-50 text-indigo-700 border border-indigo-200': log.messageType === 2,
                'bg-emerald-50 text-emerald-700 border border-emerald-200': log.messageType === 3,
                'bg-rose-50 text-rose-700 border border-rose-200': log.messageType === 4
              }"
            >
              {{ log.messageType === 2 ? 'CALL' : log.messageType === 3 ? 'RESULT' : 'ERROR' }}
            </span>

            <!-- 4. 액션명 -->
            <strong
              class="font-mono text-xs flex-shrink-0 font-bold"
              :class="(log.direction === 'SEND' || log.direction === 'OUT') ? 'text-sky-900' : (log.direction === 'RECV' || log.direction === 'IN') ? 'text-emerald-900' : log.direction === 'SYS' ? 'text-violet-900' : 'text-rose-900'"
            >
              {{ log.action || (log.messageType === 3 ? 'RESULT' : (log.messageType === 2 ? 'CALL' : 'EVENT')) }}
            </strong>

            <!-- 5. 충전기 ID -->
            <span v-if="log.chargerId" class="px-1.5 py-0.5 rounded text-[10px] font-mono font-bold bg-slate-100 text-slate-700 border border-slate-200 flex-shrink-0">
              {{ log.chargerId }}
            </span>

            <!-- 6. UniqueId (클릭 복사 가능) -->
            <span
              v-if="log.messageId && !log.messageId.startsWith('SYS-')"
              class="px-1.5 py-0.5 rounded text-[10px] font-mono text-slate-500 bg-slate-50 border border-slate-200 hover:text-sky-600 hover:border-sky-300 flex-shrink-0 cursor-pointer"
              :title="'클릭하여 MessageId 복사: ' + log.messageId"
              @click.stop="copyText(log.id + '-uid', log.messageId)"
            >
              #{{ log.messageId }}
              <span v-if="copiedId === log.id + '-uid'" class="text-emerald-600 font-bold ml-0.5">✓</span>
            </span>

            <!-- 7. 패킷 바이트 크기 -->
            <span class="text-slate-400 font-mono text-[10px] flex-shrink-0">
              {{ getByteSize(log.payload) }}
            </span>

            <!-- 8. Latency 배지 -->
            <span
              v-if="log.latencyMs !== undefined"
              class="text-[10px] font-mono text-emerald-700 bg-emerald-50 px-1.5 py-0.5 rounded border border-emerald-200 flex-shrink-0"
            >
              왕복 {{ log.latencyMs }}ms
            </span>
          </div>

          <!-- 우측 툴 (복사 & 접기/펼치기 화살표) -->
          <div class="flex items-center gap-1.5 flex-shrink-0">
            <button
              class="px-2 py-0.5 text-[10px] rounded border border-slate-200 bg-white hover:bg-sky-50 hover:text-sky-600 text-slate-600 transition-colors cursor-pointer font-medium"
              title="전체 JSON 전문 복사"
              @click.stop="copyText(log.id, formatJson(log.payload))"
            >
              {{ copiedId === log.id ? '✓ 복사됨' : '📋 복사' }}
            </button>
            <span class="text-[10px] font-medium text-slate-400 flex items-center gap-0.5 hover:text-slate-700">
              <span>{{ isPayloadOpen(log.id) ? '▲ 전문 닫기' : '▼ 전문 보기' }}</span>
            </span>
          </div>
        </div>

        <!-- 2번째 줄: 스마트 파라미터 칩 & 요약 미리보기 (클릭 시 토글) -->
        <div
          @click="togglePayload(log.id)"
          class="px-3 pb-2 pt-0.5 flex flex-wrap items-center gap-1.5 cursor-pointer bg-white hover:bg-slate-50/70 transition-colors border-t border-slate-100"
        >
          <!-- 스마트 칩 목록 -->
          <span
            v-for="chip in getSmartChips(log)"
            :key="chip.key"
            class="inline-flex items-center gap-1 text-[10px] font-mono px-2 py-0.5 rounded-sm font-semibold flex-shrink-0"
            :class="{
              'bg-emerald-50 text-emerald-700 border border-emerald-200': chip.type === 'success',
              'bg-sky-50 text-sky-700 border border-sky-200': chip.type === 'info',
              'bg-amber-50 text-amber-700 border border-amber-200': chip.type === 'warn',
              'bg-rose-50 text-rose-700 border border-rose-200': chip.type === 'danger',
              'bg-slate-100 text-slate-700 border border-slate-200': chip.type === 'neutral'
            }"
          >
            <span class="opacity-70">{{ chip.key }}:</span>
            <span>{{ chip.val }}</span>
          </span>

          <!-- 접힌 상태 한 줄 미리보기 텍스트 -->
          <span
            v-if="!isPayloadOpen(log.id)"
            class="text-slate-400 text-[10px] font-mono truncate max-w-lg select-text"
            :title="getPayloadSummary(log.payload)"
          >
            {{ getPayloadSummary(log.payload) }}
          </span>
        </div>

        <!-- 펼침 상세 영역: 인라인 포맷팅 JSON 뷰어 (라이트 기본 테마) -->
        <div
          v-if="isPayloadOpen(log.id)"
          class="border-t border-slate-200 bg-slate-50/80 p-3 font-mono text-[11px]"
        >
          <div class="flex items-center justify-between pb-2 mb-2 border-b border-slate-200">
            <span class="text-[10px] text-slate-600 font-bold flex items-center gap-1.5">
              <span>📦 실제 전문 페이로드 (Raw OCPP Packet)</span>
            </span>
            <button
              @click.stop="copyText(log.id, formatJson(log.payload))"
              class="px-2 py-0.5 text-[10px] rounded bg-white hover:bg-slate-100 text-slate-700 border border-slate-200 hover:border-slate-300 transition-colors cursor-pointer flex items-center gap-1 font-medium shadow-2xs"
            >
              <span>{{ copiedId === log.id ? '✓ 복사완료' : '📋 전문 복사' }}</span>
            </button>
          </div>
          <pre class="text-slate-800 bg-white p-2.5 rounded-md border border-slate-200 font-mono text-[11px] leading-relaxed overflow-x-auto selection:bg-sky-100 max-h-80 whitespace-pre">{{ formatJson(log.payload) }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>
