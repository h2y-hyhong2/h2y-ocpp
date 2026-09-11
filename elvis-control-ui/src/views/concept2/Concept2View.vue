<script setup lang="ts">
/**
 * ============================================================================
 * [디자인 컨셉 2] 대형 상황판 매트릭스형 (Wall View / Situation Room)
 * ============================================================================
 * - 특징: 종합 상황실 및 멀티 스크린 월(Wall) 전용 초고밀도 매트릭스 레이아웃
 * - 디자인 테마: Cyber Situation Room (./concept2.css)
 * - 상단: 10대 충전 상태별 카운트 바 & 이상 발생 요약
 * - 중앙: 2,000대 충전기 초고밀도 매트릭스 히트맵 타일 뷰 (장애 감지 최적화)
 * - 하단: 실시간 이벤트 & 이상 경보 알림 티커
 * - 사용자가 ./concept2.css 파일 및 이 컴포넌트의 구조와 스타일을 자유롭게 조정하실 수 있습니다.
 */
import { ref, computed } from 'vue'
import { useCsmsStore, STATUS_MAP, type ChargerItem } from '@/stores/csmsStore'
import './concept2.css'

const store = useCsmsStore()

// 선택된 충전기 상세 모달 제어
const selectedItem = ref<ChargerItem | null>(null)
const filterStatus = ref<string>('ALL')

// 상태별 카운트
const statusCounts = computed(() => {
  const counts: Record<string, number> = {}
  Object.keys(STATUS_MAP).forEach(k => counts[k] = 0)
  store.chargers.forEach(c => {
    if (counts[c.status] !== undefined) counts[c.status]++
  })
  return counts
})

// 매트릭스에 노출할 충전기 목록 (상태 필터 적용)
const displayChargers = computed(() => {
  if (filterStatus.value === 'ALL') return store.chargers
  return store.chargers.filter(c => c.status === filterStatus.value)
})

function openDetail(charger: ChargerItem) {
  selectedItem.value = charger
}

function closeDetail() {
  selectedItem.value = null
}

function sendCommand(action: string) {
  if (!selectedItem.value) return
  store.addToast('success', `${action} 명령 발행`, `[${selectedItem.value.chargeBoxId}] 단말에 ${action} 명령이 전송되었습니다.`)
}
</script>

<template>
  <div class="concept2-theme concept2-container h-full w-full flex flex-col gap-2 md:gap-3 overflow-hidden select-none p-1">
    
    <!-- ========================================================================= -->
    <!-- [상단] 10대 상태 요약 및 필터 툴바                                         -->
    <!-- ========================================================================= -->
    <header class="bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl px-3 py-2 flex flex-wrap items-center justify-between gap-2 shadow-xs flex-shrink-0">
      <div class="flex items-center gap-2">
        <div class="flex items-center gap-1.5">
          <span class="text-base">📊</span>
          <h2 class="text-sm font-black text-[var(--text-bright)]">종합 상황판 매트릭스</h2>
        </div>
        <span class="text-xs px-2 py-0.5 rounded-md font-bold bg-sky-500/10 text-sky-600 dark:text-sky-400 border border-sky-500/20">
          총 {{ store.chargers.length.toLocaleString() }}대
        </span>
      </div>

      <!-- 10대 상태 필터 태그 -->
      <div class="flex flex-wrap items-center gap-1 text-[11px]">
        <button
          @click="filterStatus = 'ALL'"
          class="px-2 py-0.5 rounded font-bold transition-all border"
          :class="filterStatus === 'ALL' ? 'bg-sky-600 text-white border-sky-600 shadow-xs' : 'bg-[var(--bg-surface-2)] text-slate-500 hover:text-[var(--text-bright)] border-transparent'"
        >
          전체 ({{ store.chargers.length }})
        </button>
        <button
          v-for="(info, key) in STATUS_MAP"
          :key="key"
          @click="filterStatus = key"
          class="px-2 py-0.5 rounded font-bold transition-all border flex items-center gap-1"
          :class="filterStatus === key ? 'bg-sky-600 text-white border-sky-600' : 'bg-[var(--bg-surface-2)] text-slate-500 hover:text-[var(--text-bright)] border-transparent'"
        >
          <span class="w-1.5 h-1.5 rounded-full" :style="{ backgroundColor: info.color }"></span>
          <span>{{ info.name }}</span>
          <span class="opacity-75 font-mono">({{ statusCounts[key] || 0 }})</span>
        </button>
      </div>
    </header>

    <!-- ========================================================================= -->
    <!-- [중앙] 2,000대 충전기 고밀도 매트릭스 그리드 (히트맵 뷰)                   -->
    <!-- ========================================================================= -->
    <main class="flex-1 bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl p-3 overflow-y-auto shadow-xs">
      <div class="grid grid-cols-10 sm:grid-cols-16 md:grid-cols-20 lg:grid-cols-25 xl:grid-cols-30 2xl:grid-cols-40 gap-1.5">
        <div
          v-for="chg in displayChargers"
          :key="chg.id"
          @click="openDetail(chg)"
          class="matrix-tile aspect-square rounded cursor-pointer transition-all hover:scale-125 hover:z-20 hover:shadow-lg flex flex-col items-center justify-center relative p-0.5 border"
          :class="[
            chg.status === '충전중' ? 'bg-blue-500/20 border-blue-500/40 text-blue-400 animate-pulse-slow' :
            chg.status === '통신이상' ? 'bg-red-500/30 border-red-500/60 text-red-400 animate-bounce-short' :
            chg.status === '충전완료' ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-400' :
            chg.status === '충전준비중' ? 'bg-teal-500/20 border-teal-500/40 text-teal-400' :
            chg.status === '점검중' ? 'bg-amber-500/20 border-amber-500/40 text-amber-400' :
            'bg-[var(--bg-surface-2)] border-[var(--border-glass)] text-slate-500'
          ]"
          :title="`[${chg.chargeBoxId}] ${chg.stationName} - ${chg.status} (${chg.powerKw}kW)`"
        >
          <span class="text-[8px] font-mono font-bold leading-none truncate w-full text-center">{{ chg.cpId }}</span>
          <span class="text-[7px] font-black leading-none opacity-80 mt-0.5" v-if="chg.powerKw > 0">{{ chg.powerKw }}k</span>
        </div>
      </div>
    </main>

    <!-- ========================================================================= -->
    <!-- [하단] 실시간 이상 발생 및 이벤트 롤링 티커                             -->
    <!-- ========================================================================= -->
    <footer class="bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl px-3 py-2 flex items-center justify-between text-xs shadow-xs flex-shrink-0">
      <div class="flex items-center gap-2 overflow-hidden">
        <span class="flex items-center gap-1 font-bold text-rose-600 dark:text-rose-400 flex-shrink-0">
          <span class="animate-ping w-2 h-2 rounded-full bg-rose-500"></span>
          <span>실시간 알림:</span>
        </span>
        <span class="text-slate-500 truncate">
          [CP-003-02] 경기 판교 테크노밸리 메가허브 - 충전 세션 개시 (전력 공급 175kW 시작)
        </span>
      </div>
      <div class="font-mono text-[11px] text-slate-400 flex-shrink-0">
        매트릭스 갱신 주기: 1초
      </div>
    </footer>

    <!-- ========================================================================= -->
    <!-- [모달] 단말 클릭 시 상세 정보 및 원격 제어 팝업                          -->
    <!-- ========================================================================= -->
    <div
      v-if="selectedItem"
      class="fixed inset-0 bg-slate-900/50 backdrop-blur-xs z-50 flex items-center justify-center p-4"
      @click.self="closeDetail"
    >
      <div class="bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-2xl w-full max-w-lg p-5 shadow-2xl space-y-4 animate-in fade-in zoom-in-95 duration-150">
        <!-- 팝업 헤더 -->
        <div class="flex items-center justify-between border-b border-[var(--border-glass)] pb-3">
          <div class="flex items-center gap-2">
            <span class="text-lg">⚡</span>
            <div>
              <h3 class="font-black text-sm text-[var(--text-bright)]">{{ selectedItem.chargeBoxId }}</h3>
              <p class="text-xs text-slate-500">{{ selectedItem.stationName }}</p>
            </div>
          </div>
          <button @click="closeDetail" class="text-slate-400 hover:text-slate-600 text-lg font-bold p-1">✕</button>
        </div>

        <!-- 팝업 본문 (지표) -->
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-2 text-center text-xs">
          <div class="p-2.5 rounded-xl bg-[var(--bg-surface-2)] border border-[var(--border-glass)]">
            <div class="text-[11px] text-slate-500">상태</div>
            <div class="font-black text-sm text-sky-600 mt-1">{{ selectedItem.status }}</div>
          </div>
          <div class="p-2.5 rounded-xl bg-[var(--bg-surface-2)] border border-[var(--border-glass)]">
            <div class="text-[11px] text-slate-500">출력 전력</div>
            <div class="font-black text-sm text-emerald-600 mt-1">{{ selectedItem.powerKw }} kW</div>
          </div>
          <div class="p-2.5 rounded-xl bg-[var(--bg-surface-2)] border border-[var(--border-glass)]">
            <div class="text-[11px] text-slate-500">배터리 SoC</div>
            <div class="font-black text-sm text-amber-600 mt-1">{{ selectedItem.socPercent }}%</div>
          </div>
          <div class="p-2.5 rounded-xl bg-[var(--bg-surface-2)] border border-[var(--border-glass)]">
            <div class="text-[11px] text-slate-500">누적 전력량</div>
            <div class="font-black text-sm text-violet-600 mt-1">{{ selectedItem.accumulatedKwh.toFixed(1) }} kWh</div>
          </div>
        </div>

        <!-- 팝업 원격 제어 버튼 그룹 -->
        <div class="flex items-center gap-2 pt-2 border-t border-[var(--border-glass)]">
          <button
            @click="sendCommand('충전 시작 (RemoteStart)')"
            class="flex-1 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs shadow-xs transition-all"
          >
            충전 시작
          </button>
          <button
            @click="sendCommand('충전 중지 (RemoteStop)')"
            class="flex-1 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs shadow-xs transition-all"
          >
            충전 중지
          </button>
          <button
            @click="sendCommand('단말 리셋 (Reset)')"
            class="py-2 px-3 rounded-xl bg-slate-700 hover:bg-slate-800 text-white font-bold text-xs transition-all"
          >
            리셋
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.animate-pulse-slow {
  animation: pulse 2.5s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
</style>
