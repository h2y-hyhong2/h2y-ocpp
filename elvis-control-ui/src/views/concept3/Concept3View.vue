<script setup lang="ts">
/**
 * ============================================================================
 * [디자인 컨셉 3] 전력 콕핏 / 에너지 분석형 (Energy Cockpit & Analytics)
 * ============================================================================
 * - 특징: 전기버스 차고지 / 스마트 차징 및 전력 피크 부하 제어 특화 레이아웃
 * - 디자인 테마: Eco Fleet Cockpit (./concept3.css)
 * - 상단: 4대 핵심 에너지 & 인프라 지표 (총 공급 전력, 피크율, 활성 세션, TPS)
 * - 중앙 좌측: Apache ECharts 실시간 전력량(kW) & TOU 단가 시계열 분석 차트
 * - 중앙 우측: 활성 충전기 상태 및 OCPP 통신 패킷 스트림 (WireTap)
 * - 사용자가 ./concept3.css 파일 및 이 컴포넌트의 구조와 디자인을 자유롭게 조정하실 수 있습니다.
 */
import { ref, computed } from 'vue'
import { useCsmsStore } from '@/stores/csmsStore'
import MeterValuesView from '../MeterValuesView.vue'
import LiveLogsView from '../LiveLogsView.vue'
import './concept3.css'

const store = useCsmsStore()
const activeTab = ref<'chart' | 'logs'>('chart')

// 충전 중인 활성 충전기 목록
const activeChargers = computed(() => {
  return store.chargers.filter(c => c.status === '충전중').slice(0, 8)
})
</script>

<template>
  <div class="concept3-theme concept3-container h-full w-full flex flex-col gap-2 md:gap-3 overflow-hidden select-none p-1">
    
    <!-- ========================================================================= -->
    <!-- [상단] 4대 핵심 에너지 KPI 지표 카드 바                                    -->
    <!-- ========================================================================= -->
    <header class="grid grid-cols-2 lg:grid-cols-4 gap-2 flex-shrink-0">
      <!-- 1. 총 공급 전력 -->
      <div class="bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl p-3 flex items-center justify-between shadow-xs">
        <div>
          <span class="text-[11px] font-semibold text-slate-500">실시간 총 공급 전력</span>
          <div class="text-lg sm:text-xl font-black text-sky-600 dark:text-sky-400 mt-0.5">
            {{ (store.summary.totalPowerKw / 1000).toFixed(2) }} <span class="text-xs font-bold text-slate-400">MW</span>
          </div>
        </div>
        <div class="w-9 h-9 rounded-lg bg-sky-500/10 text-sky-600 flex items-center justify-center text-lg">⚡</div>
      </div>

      <!-- 2. 피크 부하율 -->
      <div class="bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl p-3 flex items-center justify-between shadow-xs">
        <div>
          <span class="text-[11px] font-semibold text-slate-500">피크 부하율 (Peak)</span>
          <div class="text-lg sm:text-xl font-black text-emerald-600 dark:text-emerald-400 mt-0.5">
            74.2 <span class="text-xs font-bold text-slate-400">%</span>
          </div>
        </div>
        <div class="w-9 h-9 rounded-lg bg-emerald-500/10 text-emerald-600 flex items-center justify-center text-lg">📈</div>
      </div>

      <!-- 3. 활성 충전 세션 -->
      <div class="bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl p-3 flex items-center justify-between shadow-xs">
        <div>
          <span class="text-[11px] font-semibold text-slate-500">동시 충전 세션</span>
          <div class="text-lg sm:text-xl font-black text-violet-600 dark:text-violet-400 mt-0.5">
            {{ store.summary.chargingCount }} <span class="text-xs font-bold text-slate-400">기 / 2,000</span>
          </div>
        </div>
        <div class="w-9 h-9 rounded-lg bg-violet-500/10 text-violet-600 flex items-center justify-center text-lg">🔌</div>
      </div>

      <!-- 4. 실시간 TPS 처리량 -->
      <div class="bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl p-3 flex items-center justify-between shadow-xs">
        <div>
          <span class="text-[11px] font-semibold text-slate-500">초당 패킷 처리량</span>
          <div class="text-lg sm:text-xl font-black text-amber-600 dark:text-amber-400 mt-0.5">
            {{ store.summary.liveTps.toLocaleString() }} <span class="text-xs font-bold text-slate-400">TPS</span>
          </div>
        </div>
        <div class="w-9 h-9 rounded-lg bg-amber-500/10 text-amber-600 flex items-center justify-center text-lg">🚀</div>
      </div>
    </header>

    <!-- ========================================================================= -->
    <!-- [중앙] 2단 분할: 좌측 에너지 시계열 분석 & 우측 실시간 단말/패킷 모니터링 -->
    <!-- ========================================================================= -->
    <main class="flex-1 grid grid-cols-1 lg:grid-cols-12 gap-2 md:gap-3 overflow-hidden min-h-0">
      
      <!-- 좌측 (8/12): ECharts 전력 및 단가 분석 패널 -->
      <section class="lg:col-span-8 bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl p-3 flex flex-col overflow-hidden shadow-xs">
        <div class="flex items-center justify-between pb-2 border-b border-[var(--border-glass)] mb-2 flex-shrink-0">
          <div class="flex items-center gap-2">
            <span class="text-base">📊</span>
            <h3 class="text-xs sm:text-sm font-bold text-[var(--text-bright)]">실시간 전력 부하 & 계시별(TOU) 단가 시계열</h3>
          </div>
          <div class="flex items-center gap-1 text-[11px]">
            <button
              @click="activeTab = 'chart'"
              class="px-2 py-0.5 rounded font-bold transition-all"
              :class="activeTab === 'chart' ? 'bg-sky-600 text-white shadow-xs' : 'bg-[var(--bg-surface-2)] text-slate-500'"
            >
              전력 차트
            </button>
            <button
              @click="activeTab = 'logs'"
              class="px-2 py-0.5 rounded font-bold transition-all"
              :class="activeTab === 'logs' ? 'bg-sky-600 text-white shadow-xs' : 'bg-[var(--bg-surface-2)] text-slate-500'"
            >
              OCPP 패킷
            </button>
          </div>
        </div>

        <!-- 탭 컨텐츠: 차트 또는 로그 -->
        <div class="flex-1 overflow-y-auto min-h-0">
          <MeterValuesView v-if="activeTab === 'chart'" />
          <LiveLogsView v-else />
        </div>
      </section>

      <!-- 우측 (4/12): 실시간 급속 충전 세션 모니터링 카드 -->
      <section class="lg:col-span-4 bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl p-3 flex flex-col overflow-hidden shadow-xs">
        <div class="flex items-center justify-between pb-2 border-b border-[var(--border-glass)] mb-2 flex-shrink-0">
          <div class="flex items-center gap-1.5">
            <span class="text-base">🔋</span>
            <h3 class="text-xs sm:text-sm font-bold text-[var(--text-bright)]">급속 충전 세션</h3>
          </div>
          <span class="text-[11px] font-mono text-slate-400">{{ activeChargers.length }}건 출력 중</span>
        </div>

        <!-- 활성 충전 세션 리스트 -->
        <div class="flex-1 overflow-y-auto space-y-2 pr-1 min-h-0">
          <div
            v-for="chg in activeChargers"
            :key="chg.id"
            class="p-2.5 rounded-xl bg-[var(--bg-surface-2)] border border-[var(--border-glass)] hover:border-sky-500/50 transition-all text-xs space-y-1.5"
          >
            <div class="flex items-center justify-between">
              <span class="font-black text-[var(--text-bright)]">{{ chg.chargeBoxId }}</span>
              <span class="font-bold text-sky-600 dark:text-sky-400 font-mono">{{ chg.powerKw }} kW</span>
            </div>
            <div class="text-[11px] text-slate-500 truncate">{{ chg.stationName }}</div>
            
            <!-- SoC 게이지 바 -->
            <div class="space-y-1">
              <div class="flex items-center justify-between text-[10px] text-slate-400 font-mono">
                <span>SoC: {{ chg.socPercent }}%</span>
                <span>{{ chg.accumulatedKwh.toFixed(1) }} kWh</span>
              </div>
              <div class="w-full h-1.5 bg-slate-200 dark:bg-slate-800 rounded-full overflow-hidden">
                <div
                  class="h-full bg-gradient-to-r from-sky-500 to-emerald-500 rounded-full transition-all duration-300"
                  :style="{ width: `${chg.socPercent}%` }"
                ></div>
              </div>
            </div>
          </div>
        </div>
      </section>

    </main>

  </div>
</template>

<style scoped>
.concept3-container {
  position: relative;
}
</style>
