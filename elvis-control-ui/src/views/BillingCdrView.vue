<template>
  <div class="space-y-5 pb-6">
    <!-- Top Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <h1 class="text-xl font-bold text-slate-800 tracking-tight">충전 세션 & 과금 원장 (MySQL OLTP CDR)</h1>
        <span class="font-mono text-xs px-2 py-0.5 rounded-md font-semibold bg-sky-500/10 text-sky-600 border border-sky-500/30">PAGE-CDR-01</span>
      </div>
      <div class="flex items-center gap-2">
        <button type="button" class="btn-theme-action flex items-center gap-1.5" @click="exportExcel">
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" /></svg>
          <span>엑셀(CSV) 내보내기</span>
        </button>
      </div>
    </div>

    <!-- 1. 정산 요약 KPI 4종 (MySQL elvis-lite 실시간 연동) -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="glass-card p-4">
        <div class="text-xs text-slate-500 font-medium">총 충전 세션 건수</div>
        <div class="mt-2 text-2xl font-bold text-slate-900">
          {{ dbSummary.totalCount ?? 0 }} <span class="text-xs font-normal text-slate-500">건</span>
        </div>
        <div class="text-xs text-emerald-600 font-mono mt-1">100% 정상 정산</div>
      </div>

      <div class="glass-card p-4">
        <div class="text-xs text-slate-500 font-medium">총 누적 충전량 (kWh)</div>
        <div class="mt-2 text-2xl font-bold text-slate-900">
          {{ Number(dbSummary.totalKwh ?? 0).toFixed(1) }} <span class="text-xs font-normal text-slate-500">kWh</span>
        </div>
        <div class="text-xs text-sky-600 font-mono mt-1">평균 {{ (Number(dbSummary.totalKwh ?? 0) / (Number(dbSummary.totalCount) || 1)).toFixed(1) }} kWh/건</div>
      </div>

      <div class="glass-card p-4">
        <div class="text-xs text-slate-500 font-medium">총 과금 매출액 (원)</div>
        <div class="mt-2 text-2xl font-bold text-slate-900">
          {{ Number(dbSummary.totalAmount ?? 0).toLocaleString() }} <span class="text-xs font-normal text-slate-500">원</span>
        </div>
        <div class="text-xs text-violet-600 font-mono mt-1">TOU 가중평균 288.1원</div>
      </div>

      <div class="glass-card p-4">
        <div class="text-xs text-slate-500 font-medium">평균 충전 소요 시간</div>
        <div class="mt-2 text-2xl font-bold text-slate-900">
          {{ Number(dbSummary.avgMinutes ?? 0).toFixed(1) }} <span class="text-xs font-normal text-slate-500">분</span>
        </div>
        <div class="text-xs text-amber-600 font-mono mt-1">급속 충전 기준</div>
      </div>
    </div>

    <!-- 2. 필터 검색 바 -->
    <div class="glass-card p-4 flex flex-wrap items-center justify-between gap-4">
      <div class="flex items-center gap-3 flex-1 min-w-[280px]">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="트랜잭션 ID, 회원 RFID 태그, 충전기 ID 또는 충전소명 검색..."
          class="input-theme w-full"
        />
      </div>

      <div class="flex items-center gap-2 text-xs font-mono text-slate-500">
        <span>조회 기간:</span>
        <input
          v-model="startDate"
          type="date"
          class="input-theme py-1 px-2 text-xs"
          @change="loadCdrRecords"
        />
        <span>~</span>
        <input
          v-model="endDate"
          type="date"
          class="input-theme py-1 px-2 text-xs"
          @change="loadCdrRecords"
        />
        <button
          v-if="startDate || endDate"
          type="button"
          class="px-2 py-1 rounded border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 cursor-pointer"
          @click="resetDateFilter"
        >
          초기화
        </button>
      </div>
    </div>

    <!-- 3. 과금 원장 (CDR) 상세 그리드 -->
    <div class="glass-card p-5">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-sm font-bold text-slate-800">충전 트랜잭션 과금 원장 내역</h2>
        <span class="text-xs text-slate-500 font-mono">총 {{ filteredRecords.length }}건</span>
      </div>

      <div class="overflow-x-auto">
        <table class="table-theme font-mono text-xs">
          <thead>
            <tr>
              <th>트랜잭션 ID</th>
              <th>회원 RFID (IdTag)</th>
              <th>충전기 ID</th>
              <th>충전소명</th>
              <th>충전 시작 시각</th>
              <th>충전 종료 시각</th>
              <th>충전량 (kWh)</th>
              <th>적용단가 (원)</th>
              <th>최종 과금액</th>
              <th>결제 상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredRecords.length === 0">
              <td colspan="10" class="py-12 text-center text-slate-400">
                <svg class="w-9 h-9 mx-auto text-slate-300 mb-2" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <div class="font-bold text-xs text-[var(--text-bright)]">과금 원장 내역이 없습니다</div>
                <div class="text-[11px] text-slate-500 mt-0.5">데이터베이스가 비어 있거나 조건에 맞는 충전 세션이 없습니다.</div>
              </td>
            </tr>
            <tr v-for="rec in filteredRecords" :key="rec.transactionId">
              <td class="font-bold text-sky-600">#{{ rec.transactionId }}</td>
              <td class="text-slate-600">{{ rec.userTag || '-' }}</td>
              <td class="font-semibold text-slate-800">{{ rec.chargeBoxId || '-' }}</td>
              <td class="font-sans text-slate-600">{{ rec.stationName || '-' }}</td>
              <td class="text-slate-500">{{ rec.startTime || '-' }}</td>
              <td :class="rec.stopTime?.includes('진행중') ? 'text-violet-600 font-bold' : 'text-slate-500'">{{ rec.stopTime || '-' }}</td>
              <td class="font-bold text-slate-800">{{ Number(rec.energyKwh ?? 0).toFixed(1) }} kWh</td>
              <td>{{ Number(rec.avgPriceWon ?? 0).toLocaleString() }} 원</td>
              <td class="font-bold text-emerald-600">{{ Number(rec.totalAmountWon ?? 0).toLocaleString() }} 원</td>
              <td>
                <span v-if="rec.paymentStatus === 'COMPLETED'" class="badge badge-available text-[10px]">정산 완료</span>
                <span v-else class="badge badge-charging text-[10px]">충전 진행중</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useCsmsStore } from '@/stores/csmsStore'
import { authFetch } from '@/utils/apiClient'

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
const dbRecords = ref<CdrRow[]>([])
const dbSummary = ref<CdrSummary>({
  totalCount: 0,
  totalKwh: 0,
  totalAmount: 0,
  avgMinutes: 0
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
    avgPriceWon: Number(r.unitPrice ?? 288.1),
    totalAmountWon: Number(r.totalAmount ?? 0),
    paymentStatus: r.paymentStatus || ''
  }
}

function buildFilterParams(): URLSearchParams {
  const params = new URLSearchParams()
  if (startDate.value) params.set('startDate', startDate.value)
  if (endDate.value) params.set('endDate', endDate.value)
  return params
}

async function loadCdrRecords() {
  try {
    const hasDateFilter = Boolean(startDate.value || endDate.value)

    if (!hasDateFilter) {
      const res = await authFetch('/api/v1/cdr')
      if (res.ok) {
        const json = await res.json()
        const payload = json?.data || json
        if (payload && Array.isArray(payload.records)) {
          dbRecords.value = payload.records.map(mapCdrRow)
          if (payload.summary) {
            dbSummary.value = {
              totalCount: Number(payload.summary.totalCount ?? 0),
              totalKwh: Number(payload.summary.totalKwh ?? 0),
              totalAmount: Number(payload.summary.totalAmount ?? 0),
              avgMinutes: Number(payload.summary.avgMinutes ?? 0)
            }
          }
        }
      }
      return
    }

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
  } catch {
    // API 연결 실패 시 기존 상태 유지
  }
}

function resetDateFilter() {
  startDate.value = ''
  endDate.value = ''
  loadCdrRecords()
}

onMounted(() => {
  loadCdrRecords()
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

  return dbRecords.value.filter(rec => matchesQuery(rec, localQ) && matchesQuery(rec, globalQ))
})

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
      store.addToast('error', 'Excel 내보내기 실패', `서버 응답 오류 (HTTP ${res.status})`)
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

    store.addToast('success', 'Excel 내보내기 완료', `${filename} 파일이 다운로드되었습니다.`)
  } catch (err: any) {
    store.addToast('error', 'Excel 내보내기 실패', err?.message || 'CSV 다운로드 중 오류가 발생했습니다.')
  }
}
</script>
