<template>
  <div class="space-y-5 pb-6">
    <!-- Top Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <h1 class="text-xl font-bold font-display text-slate-800 dark:text-slate-100 tracking-tight">충전 세션 & 과금 원장 (MySQL OLTP CDR)</h1>
        <span class="font-mono text-xs px-2 py-0.5 rounded-md font-semibold bg-sky-500/10 text-sky-600 dark:text-sky-400 border border-sky-500/30">PAGE-CDR-01</span>
      </div>
      <div class="flex items-center gap-2">
        <button class="btn-theme-action flex items-center gap-1.5" @click="exportExcel">
          <span>📥 엑셀(CSV) 내보내기</span>
        </button>
      </div>
    </div>

    <!-- 1. 정산 요약 KPI 4종 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="glass-card p-4">
        <div class="text-xs text-slate-500 font-medium">총 충전 세션 건수</div>
        <div class="mt-2 text-2xl font-bold font-display text-slate-900 dark:text-slate-100">
          5 <span class="text-xs font-normal text-slate-500">건</span>
        </div>
        <div class="text-xs text-emerald-600 dark:text-emerald-400 font-mono mt-1">100% 정상 정산</div>
      </div>

      <div class="glass-card p-4">
        <div class="text-xs text-slate-500 font-medium">총 누적 충전량 (kWh)</div>
        <div class="mt-2 text-2xl font-bold font-display text-slate-900 dark:text-slate-100">
          262.5 <span class="text-xs font-normal text-slate-500">kWh</span>
        </div>
        <div class="text-xs text-sky-600 dark:text-sky-400 font-mono mt-1">평균 52.5 kWh/건</div>
      </div>

      <div class="glass-card p-4">
        <div class="text-xs text-slate-500 font-medium">총 과금 매출액 (원)</div>
        <div class="mt-2 text-2xl font-bold font-display text-slate-900 dark:text-slate-100">
          75,638 <span class="text-xs font-normal text-slate-500">원</span>
        </div>
        <div class="text-xs text-violet-600 dark:text-violet-400 font-mono mt-1">TOU 가중평균 288.1원</div>
      </div>

      <div class="glass-card p-4">
        <div class="text-xs text-slate-500 font-medium">평균 충전 소요 시간</div>
        <div class="mt-2 text-2xl font-bold font-display text-slate-900 dark:text-slate-100">
          38.6 <span class="text-xs font-normal text-slate-500">분</span>
        </div>
        <div class="text-xs text-amber-600 dark:text-amber-400 font-mono mt-1">급속 충전 기준</div>
      </div>
    </div>

    <!-- 2. 필터 검색 바 -->
    <div class="glass-card p-4 flex flex-wrap items-center justify-between gap-4">
      <div class="flex items-center gap-3 flex-1 min-w-[280px]">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="회원 RFID 태그 또는 충전기 ID 검색..."
          class="input-theme w-full"
        />
      </div>

      <div class="flex items-center gap-2 text-xs font-mono text-slate-500">
        <span>조회 기간:</span>
        <input type="date" value="2026-08-20" class="input-theme py-1 px-2 text-xs" />
        <span>~</span>
        <input type="date" value="2026-08-20" class="input-theme py-1 px-2 text-xs" />
      </div>
    </div>

    <!-- 3. 과금 원장 (CDR) 상세 그리드 -->
    <div class="glass-card p-5">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-sm font-bold font-display text-slate-800 dark:text-slate-200">충전 트랜잭션 과금 원장 내역</h2>
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
            <tr v-for="rec in filteredRecords" :key="rec.transactionId">
              <td class="font-bold text-sky-600 dark:text-sky-400">#{{ rec.transactionId }}</td>
              <td class="text-slate-600 dark:text-slate-400">{{ rec.userTag }}</td>
              <td class="font-semibold text-slate-800 dark:text-slate-200">{{ rec.chargeBoxId }}</td>
              <td class="font-sans text-slate-600 dark:text-slate-400">{{ rec.stationName }}</td>
              <td class="text-slate-500">{{ rec.startTime }}</td>
              <td :class="rec.stopTime.includes('진행중') ? 'text-violet-500 font-bold' : 'text-slate-500'">{{ rec.stopTime }}</td>
              <td class="font-bold text-slate-800 dark:text-slate-100">{{ rec.energyKwh.toFixed(1) }} kWh</td>
              <td>{{ rec.avgPriceWon }} 원</td>
              <td class="font-bold text-emerald-600 dark:text-emerald-400">{{ rec.totalAmountWon.toLocaleString() }} 원</td>
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
import { ref, computed } from 'vue'
import { useCsmsStore } from '@/stores/csmsStore'

const store = useCsmsStore()
const searchQuery = ref('')

const filteredRecords = computed(() => {
  return store.cdrRecords.filter(r => {
    return r.userTag.includes(searchQuery.value) ||
           r.chargeBoxId.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
           r.stationName.includes(searchQuery.value)
  })
})

const exportExcel = () => {
  store.showToast('info', 'Excel 내보내기', '과금 원장 5건의 CSV 파일 다운로드가 시작되었습니다.')
}
</script>
