<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useCsmsStore } from '@/stores/csmsStore'
import { authFetch } from '@/utils/apiClient'

type BillingStatusFilter = 'ALL' | 'PAID' | 'PENDING' | 'CHARGING' | 'ABORTED'

interface CdrRow {
  transactionId: number | string
  userTag: string
  chargeBoxId: string
  stationName: string
  startTime: string
  stopTime: string | null
  energyKwh: number
  avgPriceWon: number
  totalAmountWon: number
  paymentStatus: string
  stopReason: string
}

interface CdrSummary {
  totalCount: number
  totalKwh: number
  totalAmount: number
  avgMinutes: number
}

const store = useCsmsStore()
const searchQuery = ref('')
const startDate = ref('')
const endDate = ref('')
const activeStatusFilter = ref<BillingStatusFilter>('ALL')
const isLoading = ref(false)
const lastLoadedAt = ref('')
const dbRecords = ref<CdrRow[]>([])
const selectedRecordKey = ref<string | null>(null)
const dbSummary = ref<CdrSummary>({
  totalCount: 0,
  totalKwh: 0,
  totalAmount: 0,
  avgMinutes: 0
})

function resolveStatusBucket(status: string): Exclude<BillingStatusFilter, 'ALL'> {
  const norm = (status || '').toUpperCase()
  if (norm === 'PAID' || norm === 'COMPLETED') return 'PAID'
  if (norm === 'CHARGING') return 'CHARGING'
  if (norm === 'ABORTED' || norm === 'CANCELLED' || norm === 'FAILED') return 'ABORTED'
  return 'PENDING'
}

function statusBadgeMeta(status: string): { label: string; badgeClass: string; dotClass: string } {
  const bucket = resolveStatusBucket(status)
  if (bucket === 'PAID') {
    return {
      label: '정산 완료',
      badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200',
      dotClass: 'bg-emerald-500'
    }
  }
  if (bucket === 'CHARGING') {
    return {
      label: '충전중',
      badgeClass: 'bg-blue-50 text-blue-700 border-blue-200',
      dotClass: 'bg-blue-600 animate-pulse'
    }
  }
  if (bucket === 'ABORTED') {
    return {
      label: '중단',
      badgeClass: 'bg-rose-50 text-rose-700 border-rose-200',
      dotClass: 'bg-rose-600'
    }
  }
  return {
    label: '정산 대기',
    badgeClass: 'bg-sky-50 text-sky-700 border-sky-200',
    dotClass: 'bg-sky-500'
  }
}

function recordKey(rec: CdrRow): string {
  return `${rec.chargeBoxId}-${rec.transactionId}`
}

const statusCounts = computed(() => {
  const counts: Record<BillingStatusFilter, number> = {
    ALL: dbRecords.value.length,
    PAID: 0,
    PENDING: 0,
    CHARGING: 0,
    ABORTED: 0
  }
  dbRecords.value.forEach(rec => {
    counts[resolveStatusBucket(rec.paymentStatus)]++
  })
  return counts
})

function matchesQuery(rec: CdrRow, q: string): boolean {
  if (!q) return true
  return (
    String(rec.transactionId ?? '').toLowerCase().includes(q) ||
    (rec.userTag || '').toLowerCase().includes(q) ||
    (rec.chargeBoxId || '').toLowerCase().includes(q) ||
    (rec.stationName || '').toLowerCase().includes(q) ||
    (rec.paymentStatus || '').toLowerCase().includes(q)
  )
}

const filteredRecords = computed(() => {
  const localQ = searchQuery.value.trim().toLowerCase()
  const globalQ = (store.searchQuery || '').trim().toLowerCase()

  return dbRecords.value.filter(rec => {
    if (activeStatusFilter.value !== 'ALL' && resolveStatusBucket(rec.paymentStatus) !== activeStatusFilter.value) {
      return false
    }
    return matchesQuery(rec, localQ) && matchesQuery(rec, globalQ)
  })
})

const filteredSummary = computed(() => {
  let kwh = 0
  let amount = 0
  filteredRecords.value.forEach(r => {
    kwh += Number(r.energyKwh ?? 0)
    amount += Number(r.totalAmountWon ?? 0)
  })
  return { kwh, amount }
})

const selectedRecord = computed<CdrRow | null>(() => {
  if (!selectedRecordKey.value) return null
  return dbRecords.value.find(r => recordKey(r) === selectedRecordKey.value) ?? null
})

const avgUnitPriceWon = computed(() => {
  const kwh = Number(dbSummary.value.totalKwh ?? 0)
  if (kwh <= 0) return '0'
  return Math.round(Number(dbSummary.value.totalAmount ?? 0) / kwh).toLocaleString()
})

const hasActiveFilters = computed(() => {
  return Boolean(searchQuery.value.trim() || startDate.value || endDate.value || activeStatusFilter.value !== 'ALL')
})

function mapCdrRow(r: any): CdrRow {
  return {
    transactionId: r.transactionId,
    userTag: r.userTag || '',
    chargeBoxId: r.chargeBoxId || '',
    stationName: r.stationName || '',
    startTime: r.startTime || '',
    stopTime: r.stopTime ?? null,
    energyKwh: Number(r.kwh ?? 0),
    avgPriceWon: Number(r.unitPrice ?? 0),
    totalAmountWon: Number(r.totalAmount ?? 0),
    paymentStatus: r.paymentStatus || '',
    stopReason: r.stopReason || ''
  }
}

function buildFilterParams(): URLSearchParams {
  const params = new URLSearchParams()
  if (startDate.value) params.set('startDate', startDate.value)
  if (endDate.value) params.set('endDate', endDate.value)
  const kw = searchQuery.value.trim()
  if (kw) params.set('keyword', kw)
  return params
}

async function loadCdrRecords(showErrorToast = false) {
  if (isLoading.value) return
  isLoading.value = true
  try {
    const params = buildFilterParams()
    const pagedParams = new URLSearchParams(params)
    pagedParams.set('page', '0')
    pagedParams.set('size', '100')

    const [pagedRes, summaryRes] = await Promise.all([
      authFetch(`/api/v1/cdr/paged?${pagedParams.toString()}`),
      authFetch(`/api/v1/cdr/summary?${params.toString()}`)
    ])

    if (pagedRes.ok) {
      const pagedJson = await pagedRes.json()
      const pagedPayload = pagedJson?.data || pagedJson
      const content = Array.isArray(pagedPayload?.content) ? pagedPayload.content : []
      dbRecords.value = content.map(mapCdrRow)
    } else if (showErrorToast) {
      store.addToast('error', '과금 원장 조회 실패', `서버 응답 오류 (HTTP ${pagedRes.status})`)
    }

    if (summaryRes.ok) {
      const summaryJson = await summaryRes.json()
      const summaryPayload = summaryJson?.data || summaryJson
      if (summaryPayload) {
        dbSummary.value = {
          totalCount: Number(summaryPayload.totalCount ?? 0),
          totalKwh: Number(summaryPayload.totalKwh ?? 0),
          totalAmount: Number(summaryPayload.totalAmount ?? 0),
          avgMinutes: Number(summaryPayload.avgMinutes ?? 0)
        }
      }
    }

    lastLoadedAt.value = new Date().toLocaleTimeString('ko-KR', { hour12: false })
  } catch (err: any) {
    if (showErrorToast) {
      store.addToast('error', '과금 원장 조회 오류', err?.message || '서버와 통신할 수 없습니다.')
    }
  } finally {
    isLoading.value = false
  }
}

function resetFilters() {
  searchQuery.value = ''
  startDate.value = ''
  endDate.value = ''
  activeStatusFilter.value = 'ALL'
  loadCdrRecords(true)
}

function copyText(val: string, label: string, e?: Event) {
  if (e) e.stopPropagation()
  if (!val) return
  navigator.clipboard?.writeText(val)
  store.addToast('info', `${label} 복사 완료`, `[${val}] 값이 클립보드에 복사되었습니다.`)
}

function openRecordDetail(rec: CdrRow) {
  selectedRecordKey.value = recordKey(rec)
}

function closeRecordDetail() {
  selectedRecordKey.value = null
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && selectedRecordKey.value) {
    closeRecordDetail()
  }
}

async function exportExcel() {
  try {
    const params = buildFilterParams()
    const keyword = (searchQuery.value || store.searchQuery || '').trim()
    if (keyword) {
      params.set('keyword', keyword)
    }

    const qs = params.toString()
    const url = qs ? `/api/v1/cdr/export?${qs}` : '/api/v1/cdr/export'
    const res = await authFetch(url)

    if (!res.ok) {
      store.addToast('error', 'CSV 내보내기 실패', `서버 응답 오류 (HTTP ${res.status})`)
      return
    }

    const blob = await res.blob()
    const disposition = res.headers.get('Content-Disposition') || ''
    const match = disposition.match(/filename="?([^";]+)"?/i)
    const filename = match?.[1] || `cdr_records_${new Date().toISOString().slice(0, 10)}.csv`

    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(objectUrl)

    store.addToast('success', 'CSV 내보내기 완료', `${filename} 파일이 다운로드되었습니다.`)
  } catch (err: any) {
    store.addToast('error', 'CSV 내보내기 실패', err?.message || 'CSV 다운로드 중 오류가 발생했습니다.')
  }
}

let pollTimer: number | null = null

onMounted(() => {
  loadCdrRecords(false)
  pollTimer = window.setInterval(() => {
    loadCdrRecords(false)
  }, 5000)
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="w-full h-full flex flex-col gap-2.5 overflow-hidden select-none">
    <!-- 1. 상단 컨트롤 바 (충전기 목록과 동일한 1줄 모던 바) -->
    <div class="w-full bg-white rounded-xl px-3.5 py-2 border border-slate-200 shadow-2xs flex flex-wrap items-center justify-between gap-2.5 flex-shrink-0">
      <!-- 좌측: 타이틀 필 + 인라인 핵심 지표 요약 -->
      <div class="flex flex-wrap items-center gap-2">
        <div class="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-xl bg-sky-50 text-sky-700 font-bold border border-sky-200 shadow-2xs">
          <svg class="w-4 h-4 text-sky-600 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <span class="text-sm text-slate-900 font-extrabold tracking-tight whitespace-nowrap">과금 원장</span>
          <span class="px-2 py-0.5 rounded-full bg-sky-600 text-white text-[13px] font-bold flex-shrink-0 font-mono">
            {{ dbSummary.totalCount }}건
          </span>
        </div>

        <!-- 인라인 핵심 집계 요약 배지 (충전기 목록 요약 필과 동일 규격) -->
        <div class="hidden xl:inline-flex items-center gap-2 px-3 py-1 rounded-full bg-slate-50 border border-slate-200 text-[13px] font-mono font-bold text-slate-600">
          <span class="text-blue-700">{{ Number(dbSummary.totalKwh ?? 0).toFixed(2) }} kWh</span>
          <span class="text-slate-300">|</span>
          <span class="text-emerald-700">{{ Number(dbSummary.totalAmount ?? 0).toLocaleString() }}원</span>
          <span class="text-slate-300">|</span>
          <span class="text-slate-500">평균 {{ Number(dbSummary.avgMinutes ?? 0).toFixed(1) }}분</span>
        </div>

        <!-- 컴팩트 기간 선택 -->
        <div class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-50 border border-slate-200 text-xs text-slate-600">
          <input
            v-model="startDate"
            type="date"
            aria-label="조회 시작일"
            class="bg-transparent text-xs font-mono font-medium text-slate-700 outline-hidden cursor-pointer"
            @change="loadCdrRecords(true)"
          />
          <span class="text-slate-400">~</span>
          <input
            v-model="endDate"
            type="date"
            aria-label="조회 종료일"
            class="bg-transparent text-xs font-mono font-medium text-slate-700 outline-hidden cursor-pointer"
            @change="loadCdrRecords(true)"
          />
          <button
            v-if="hasActiveFilters"
            type="button"
            @click="resetFilters"
            class="ml-0.5 text-slate-400 hover:text-slate-700 p-0.5 rounded cursor-pointer transition-colors"
            title="필터 초기화"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <!-- 우측: 결제 상태 세그먼트 칩 + 검색 + 새로고침 + CSV -->
      <div class="flex flex-wrap items-center gap-2">
        <div class="flex flex-wrap items-center gap-1 bg-slate-100 p-1 rounded-xl">
          <button
            type="button"
            @click="activeStatusFilter = 'ALL'"
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer"
            :class="activeStatusFilter === 'ALL' ? 'bg-white text-sky-700 shadow-2xs' : 'text-slate-600 hover:text-slate-900'"
          >
            <span>전체</span>
            <span class="font-mono">{{ statusCounts.ALL }}</span>
          </button>
          <button
            type="button"
            @click="activeStatusFilter = 'PAID'"
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold transition-all cursor-pointer"
            :class="activeStatusFilter === 'PAID' ? 'bg-white text-emerald-700 shadow-2xs font-bold' : 'text-slate-600 hover:text-slate-900'"
          >
            <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
            <span>완료</span>
            <span class="font-mono">{{ statusCounts.PAID }}</span>
          </button>
          <button
            type="button"
            @click="activeStatusFilter = 'PENDING'"
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold transition-all cursor-pointer"
            :class="activeStatusFilter === 'PENDING' ? 'bg-white text-sky-700 shadow-2xs font-bold' : 'text-slate-600 hover:text-slate-900'"
          >
            <span class="w-2 h-2 rounded-full bg-sky-500"></span>
            <span>대기</span>
            <span class="font-mono">{{ statusCounts.PENDING }}</span>
          </button>
          <button
            type="button"
            @click="activeStatusFilter = 'CHARGING'"
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold transition-all cursor-pointer"
            :class="activeStatusFilter === 'CHARGING' ? 'bg-white text-blue-700 shadow-2xs font-bold' : 'text-slate-600 hover:text-slate-900'"
          >
            <span class="w-2 h-2 rounded-full bg-blue-600"></span>
            <span>충전중</span>
            <span class="font-mono">{{ statusCounts.CHARGING }}</span>
          </button>
          <button
            type="button"
            @click="activeStatusFilter = 'ABORTED'"
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold transition-all cursor-pointer"
            :class="activeStatusFilter === 'ABORTED' ? 'bg-white text-rose-700 shadow-2xs font-bold' : 'text-slate-600 hover:text-slate-900'"
          >
            <span class="w-2 h-2 rounded-full bg-rose-600"></span>
            <span>중단</span>
            <span class="font-mono">{{ statusCounts.ABORTED }}</span>
          </button>
        </div>

        <div class="flex items-center gap-1.5">
          <div class="relative flex items-center">
            <svg class="w-3.5 h-3.5 absolute left-2.5 text-slate-400 pointer-events-none" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              v-model="searchQuery"
              type="text"
              placeholder="트랜잭션 · 충전기 · RFID"
              class="w-44 pl-8 pr-2.5 py-1.5 bg-slate-100 focus:bg-white rounded-lg text-slate-800 text-xs font-medium placeholder:text-slate-400 outline-hidden border border-transparent focus:border-sky-500 transition-all"
              @keyup.enter="loadCdrRecords(true)"
            />
          </div>

          <button
            type="button"
            :disabled="isLoading"
            @click="loadCdrRecords(true)"
            class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold transition-colors cursor-pointer border border-slate-200 disabled:opacity-50"
            title="실시간 데이터 즉시 조회"
          >
            <svg
              class="w-3.5 h-3.5 text-sky-600 transition-transform"
              :class="isLoading ? 'animate-spin' : ''"
              fill="none"
              stroke="currentColor"
              stroke-width="2.2"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            <span>새로고침</span>
          </button>

          <button
            type="button"
            @click="exportExcel"
            class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-sky-600 hover:bg-sky-700 text-white text-xs font-bold transition-colors cursor-pointer shadow-2xs"
            title="현재 필터 기준 CSV 다운로드"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            <span>CSV</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 2. 메인 과금 원장 테이블 (군더더기 없는 풀-하이트 화이트 카드) -->
    <div class="flex-1 min-h-0 bg-white rounded-xl border border-slate-200 shadow-2xs flex flex-col overflow-hidden">
      <!-- 데이터 없음 -->
      <div
        v-if="filteredRecords.length === 0"
        class="flex-1 flex flex-col items-center justify-center text-center p-8"
      >
        <div class="w-12 h-12 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-400 mb-3">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="1.6" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
        </div>
        <p class="text-sm font-bold text-slate-800">표시할 과금 내역이 없습니다</p>
        <p class="text-xs text-slate-500 mt-1">검색 조건이나 상태 필터를 변경해 주세요.</p>
      </div>

      <!-- 테이블 본문 -->
      <div v-else class="flex-1 overflow-auto min-h-0">
        <table class="w-full border-collapse text-left">
          <thead class="sticky top-0 z-10 bg-slate-50/95 backdrop-blur-xs border-b border-slate-200 text-[12px] font-bold text-slate-500">
            <tr>
              <th class="px-4 py-2.5 whitespace-nowrap">트랜잭션</th>
              <th class="px-4 py-2.5 whitespace-nowrap">충전기</th>
              <th class="px-4 py-2.5 whitespace-nowrap">충전소</th>
              <th class="px-4 py-2.5 whitespace-nowrap">회원 태그</th>
              <th class="px-4 py-2.5 whitespace-nowrap">시작 시각</th>
              <th class="px-4 py-2.5 whitespace-nowrap">종료 시각</th>
              <th class="px-4 py-2.5 whitespace-nowrap text-right">충전량</th>
              <th class="px-4 py-2.5 whitespace-nowrap text-right">단가</th>
              <th class="px-4 py-2.5 whitespace-nowrap text-right">과금액</th>
              <th class="px-4 py-2.5 whitespace-nowrap text-center">상태</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 text-xs">
            <tr
              v-for="rec in filteredRecords"
              :key="recordKey(rec)"
              @click="openRecordDetail(rec)"
              class="transition-colors cursor-pointer"
              :class="selectedRecordKey === recordKey(rec) ? 'bg-sky-50/80' : 'hover:bg-slate-50/90'"
            >
              <td class="px-4 py-3 whitespace-nowrap font-mono font-bold text-sky-700">
                #{{ rec.transactionId }}
              </td>
              <td class="px-4 py-3 whitespace-nowrap">
                <button
                  type="button"
                  @click="copyText(rec.chargeBoxId, '충전기 ID', $event)"
                  class="px-1.5 py-0.5 rounded bg-slate-50 hover:bg-sky-50 text-slate-700 hover:text-sky-700 text-[12px] font-mono font-semibold border border-slate-200 cursor-pointer transition-colors"
                  title="충전기 ID 복사"
                >
                  {{ rec.chargeBoxId || '-' }}
                </button>
              </td>
              <td class="px-4 py-3 whitespace-nowrap font-semibold text-slate-800">
                {{ rec.stationName || '-' }}
              </td>
              <td class="px-4 py-3 whitespace-nowrap font-mono text-slate-600">
                {{ rec.userTag || '-' }}
              </td>
              <td class="px-4 py-3 whitespace-nowrap font-mono text-slate-500">
                {{ rec.startTime || '-' }}
              </td>
              <td
                class="px-4 py-3 whitespace-nowrap font-mono"
                :class="rec.stopTime?.includes('진행중') ? 'text-blue-700 font-bold' : 'text-slate-500'"
              >
                {{ rec.stopTime || '-' }}
              </td>
              <td class="px-4 py-3 whitespace-nowrap font-mono font-bold text-slate-800 text-right tabular-nums">
                {{ Number(rec.energyKwh ?? 0).toFixed(2) }} <span class="text-[11px] font-normal text-slate-400">kWh</span>
              </td>
              <td class="px-4 py-3 whitespace-nowrap font-mono text-slate-500 text-right tabular-nums">
                {{ Number(rec.avgPriceWon ?? 0).toLocaleString() }}<span class="text-[11px] text-slate-400">원</span>
              </td>
              <td class="px-4 py-3 whitespace-nowrap font-mono font-bold text-slate-900 text-right tabular-nums">
                {{ Number(rec.totalAmountWon ?? 0).toLocaleString() }}<span class="text-[11px] font-normal text-slate-500">원</span>
              </td>
              <td class="px-4 py-3 whitespace-nowrap text-center">
                <span
                  class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[12px] font-bold border"
                  :class="statusBadgeMeta(rec.paymentStatus).badgeClass"
                  :title="rec.stopReason ? `종료 사유: ${rec.stopReason}` : undefined"
                >
                  <span class="w-1.5 h-1.5 rounded-full" :class="statusBadgeMeta(rec.paymentStatus).dotClass"></span>
                  <span>{{ statusBadgeMeta(rec.paymentStatus).label }}</span>
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 3. 하단 고정 상태 바 (충전기 목록 하단바와 동일 높이·톤) -->
    <footer class="w-full bg-white rounded-xl px-3.5 py-1.5 border border-slate-200 shadow-2xs flex items-center justify-between gap-2.5 text-[13px] text-slate-500 flex-shrink-0">
      <div class="flex items-center gap-2 min-w-0 font-mono text-xs">
        <span class="font-bold text-slate-800">표시 {{ filteredRecords.length }}/{{ dbSummary.totalCount }}건</span>
        <span class="text-slate-300">·</span>
        <span class="text-slate-600">{{ filteredSummary.kwh.toFixed(2) }} kWh</span>
        <span class="text-slate-300">·</span>
        <span class="font-bold text-emerald-700">{{ filteredSummary.amount.toLocaleString() }}원</span>
        <span class="hidden md:inline text-slate-300">·</span>
        <span class="hidden md:inline text-slate-500">평균 단가 {{ avgUnitPriceWon }}원/kWh</span>
      </div>

      <div class="flex items-center gap-3 flex-shrink-0 font-mono text-[12px]">
        <span class="hidden sm:inline-flex items-center gap-1.5 font-semibold" :class="store.isDbConnected ? 'text-emerald-700' : 'text-slate-400'">
          <span class="w-1.5 h-1.5 rounded-full" :class="store.isDbConnected ? 'bg-emerald-500' : 'bg-slate-400'"></span>
          <span>{{ store.isDbConnected ? '관제 서버 온라인' : '서버 연결 대기' }}</span>
        </span>
        <span v-if="lastLoadedAt" class="text-slate-400">갱신 {{ lastLoadedAt }}</span>
      </div>
    </footer>

    <!-- 4. 우측 슬라이드 상세 드로어 (충전기 목록 상세 드로어와 동일 규격) -->
    <div
      v-if="selectedRecord"
      @click="closeRecordDetail"
      class="fixed inset-0 bg-slate-900/30 z-40"
    ></div>

    <aside
      v-if="selectedRecord"
      class="fixed top-[60px] right-3 bottom-3 w-[348px] max-w-[92vw] bg-white border border-slate-200 rounded-2xl shadow-2xl p-4 z-50 flex flex-col justify-between overflow-y-auto"
    >
      <div class="space-y-3">
        <div class="flex items-center justify-between pb-2.5 border-b border-slate-200">
          <div>
            <div class="flex items-center gap-1.5">
              <span class="font-mono font-extrabold text-sm text-sky-700">#{{ selectedRecord.transactionId }}</span>
              <span
                class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[12px] font-bold border"
                :class="statusBadgeMeta(selectedRecord.paymentStatus).badgeClass"
              >
                <span class="w-1.5 h-1.5 rounded-full" :class="statusBadgeMeta(selectedRecord.paymentStatus).dotClass"></span>
                <span>{{ statusBadgeMeta(selectedRecord.paymentStatus).label }}</span>
              </span>
            </div>
            <div class="text-xs font-bold text-slate-800 mt-0.5">{{ selectedRecord.stationName || '-' }}</div>
          </div>
          <button
            type="button"
            @click="closeRecordDetail"
            class="p-1.5 rounded-lg text-slate-400 hover:text-slate-800 hover:bg-slate-100 cursor-pointer"
            title="닫기 (ESC)"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="p-3 rounded-xl bg-slate-50 border border-slate-200 grid grid-cols-2 gap-2 font-mono text-xs">
          <div class="p-2 rounded-lg bg-white border border-slate-200">
            <div class="text-[12px] text-slate-500 font-sans">충전량</div>
            <div class="font-bold text-slate-900 mt-0.5">{{ Number(selectedRecord.energyKwh ?? 0).toFixed(2) }} kWh</div>
          </div>
          <div class="p-2 rounded-lg bg-white border border-slate-200">
            <div class="text-[12px] text-slate-500 font-sans">적용 단가</div>
            <div class="font-bold text-slate-900 mt-0.5">{{ Number(selectedRecord.avgPriceWon ?? 0).toLocaleString() }}원</div>
          </div>
          <div class="p-2.5 rounded-lg bg-white border border-slate-200 col-span-2 flex items-baseline justify-between">
            <span class="text-[12px] text-slate-500 font-sans">최종 과금액</span>
            <span class="text-base font-black text-emerald-700">{{ Number(selectedRecord.totalAmountWon ?? 0).toLocaleString() }}원</span>
          </div>
        </div>

        <div class="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-2 text-xs">
          <div class="flex items-center justify-between gap-2">
            <span class="text-slate-500">충전기 ID</span>
            <button
              type="button"
              @click="copyText(selectedRecord.chargeBoxId, '충전기 ID', $event)"
              class="px-1.5 py-0.5 rounded bg-white hover:bg-sky-50 text-slate-800 hover:text-sky-700 border border-slate-200 font-mono font-bold cursor-pointer transition-colors"
            >
              {{ selectedRecord.chargeBoxId || '-' }}
            </button>
          </div>
          <div class="flex items-center justify-between gap-2">
            <span class="text-slate-500">회원 태그</span>
            <span class="font-mono font-semibold text-slate-800">{{ selectedRecord.userTag || '-' }}</span>
          </div>
          <div class="flex items-center justify-between gap-2">
            <span class="text-slate-500">시작 시각</span>
            <span class="font-mono text-slate-700">{{ selectedRecord.startTime || '-' }}</span>
          </div>
          <div class="flex items-center justify-between gap-2">
            <span class="text-slate-500">종료 시각</span>
            <span class="font-mono" :class="selectedRecord.stopTime?.includes('진행중') ? 'text-blue-700 font-bold' : 'text-slate-700'">
              {{ selectedRecord.stopTime || '-' }}
            </span>
          </div>
          <div class="flex items-center justify-between gap-2">
            <span class="text-slate-500">종료 사유</span>
            <span class="font-mono text-slate-700">{{ selectedRecord.stopReason || '-' }}</span>
          </div>
        </div>
      </div>

      <div class="pt-3 border-t border-slate-100 text-[12px] text-slate-400 font-mono flex justify-between">
        <span>상태: {{ selectedRecord.paymentStatus || '-' }}</span>
        <span>갱신: {{ lastLoadedAt || '-' }}</span>
      </div>
    </aside>
  </div>
</template>
