<template>
  <div class="space-y-5 pb-6">
    <!-- Top Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <h1 class="text-xl font-bold font-display text-slate-800 dark:text-slate-100 tracking-tight">1초 시계열 미터값 분석 (ClickHouse OLAP)</h1>
        <span class="font-mono text-xs px-2 py-0.5 rounded-md font-semibold bg-sky-500/10 text-sky-600 dark:text-sky-400 border border-sky-500/30">PAGE-SESSION-01</span>
      </div>
      <div class="flex items-center gap-2 text-xs text-slate-500 font-mono">
        <span class="live-indicator"></span>
        <span class="text-emerald-600 dark:text-emerald-400 font-medium">1초 주기 고속 시계열 파이프라인</span>
      </div>
    </div>

    <!-- 1. 세션 선택 바 -->
    <div class="glass-card p-4 flex flex-wrap items-center justify-between gap-4">
      <div class="flex items-center gap-3">
        <label class="text-xs font-semibold text-slate-600 dark:text-slate-400">분석 대상 세션:</label>
        <select v-model="selectedTx" class="input-theme font-mono font-semibold text-sky-600 dark:text-sky-400">
          <option :value="98124">Tx #98124 - [CP-SEOUL-001] 서울 강남 충전소 #1 (충전중)</option>
          <option :value="98125">Tx #98125 - [CP-SEOUL-002] 서울 강남 충전소 #2 (충전중)</option>
          <option :value="98122">Tx #98122 - [CP-BUSAN-101] 부산 해운대 터미널 #1 (완료)</option>
        </select>
      </div>

      <div class="flex items-center gap-4 text-xs font-mono text-slate-500">
        <div>단말: <span class="font-semibold text-slate-800 dark:text-slate-200">CP-SEOUL-001</span></div>
        <div>샘플링 주기: <span class="text-emerald-600 dark:text-emerald-400 font-bold">1초 (1,000ms)</span></div>
        <div>DB 엔진: <span class="text-sky-600 dark:text-sky-400 font-bold">ClickHouse MergeTree</span></div>
      </div>
    </div>

    <!-- 2. 1초 시계열 멀티 라인 ECharts 차트 -->
    <div class="glass-card p-5">
      <div class="flex items-center justify-between mb-3">
        <div>
          <h2 class="text-sm font-bold font-display text-slate-800 dark:text-slate-200">초 단위 전력(kW) / 전압(V) / 전류(A) / SoC(%) 동기화 그래프</h2>
          <p class="text-xs text-slate-500">ClickHouse 시계열 컬럼스토리지 실시간 인출</p>
        </div>
        <div class="flex items-center gap-3 text-xs font-mono">
          <span class="flex items-center gap-1 text-sky-500"><span class="w-2.5 h-2.5 rounded-sm bg-sky-500 inline-block"></span> 전력 (kW)</span>
          <span class="flex items-center gap-1 text-emerald-500"><span class="w-2.5 h-2.5 rounded-sm bg-emerald-500 inline-block"></span> 전압 (V)</span>
          <span class="flex items-center gap-1 text-amber-500"><span class="w-2.5 h-2.5 rounded-sm bg-amber-500 inline-block"></span> 전류 (A)</span>
          <span class="flex items-center gap-1 text-violet-500"><span class="w-2.5 h-2.5 rounded-sm bg-violet-500 inline-block"></span> SoC (%)</span>
        </div>
      </div>
      <div ref="chartRef" class="w-full h-72"></div>
    </div>

    <!-- 3. 1초 Raw MeterValues 데이터 그리드 -->
    <div class="glass-card p-5">
      <div class="flex items-center justify-between mb-3">
        <h2 class="text-sm font-bold font-display text-slate-800 dark:text-slate-200">ClickHouse 적재 Raw MeterValues 로그 그리드</h2>
        <span class="text-xs text-slate-500 font-mono">최신 10건 실시간 표출</span>
      </div>

      <div class="overflow-x-auto">
        <table class="table-theme font-mono text-xs">
          <thead>
            <tr>
              <th>수신 타임스탬프</th>
              <th>트랜잭션 ID</th>
              <th>유효전력 (kW)</th>
              <th>전압 (V)</th>
              <th>전류 (A)</th>
              <th>배터리 SoC (%)</th>
              <th>온도 (℃)</th>
              <th>적재 상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="pt in store.meterTimeSeries" :key="pt.time">
              <td class="text-slate-500">{{ pt.time }}.000</td>
              <td class="font-bold text-sky-600 dark:text-sky-400">#{{ selectedTx }}</td>
              <td class="font-bold text-slate-800 dark:text-slate-100">{{ pt.powerKw.toFixed(1) }} kW</td>
              <td>{{ pt.voltageV.toFixed(1) }} V</td>
              <td>{{ pt.currentA.toFixed(1) }} A</td>
              <td class="font-semibold text-violet-600 dark:text-violet-400">{{ pt.socPercent }}%</td>
              <td class="text-slate-400">32.4 ℃</td>
              <td><span class="badge badge-available text-[10px]">Bulk Inserted</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useCsmsStore } from '@/stores/csmsStore'
import * as echarts from 'echarts'

const store = useCsmsStore()
const selectedTx = ref(98124)
const chartRef = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null

const renderChart = () => {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)

  const isDark = document.documentElement.getAttribute('data-theme') !== 'light'
  const textColor = isDark ? '#94a3b8' : '#475569'
  const splitLineColor = isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)'

  const times = store.meterTimeSeries.map(p => p.time)
  const powers = store.meterTimeSeries.map(p => p.powerKw)
  const voltages = store.meterTimeSeries.map(p => p.voltageV)
  const currents = store.meterTimeSeries.map(p => p.currentA)
  const socs = store.meterTimeSeries.map(p => p.socPercent)

  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '8%', top: '12%', containLabel: true },
    xAxis: {
      type: 'category',
      data: times,
      axisLabel: { color: textColor, fontSize: 11 },
      axisLine: { lineStyle: { color: splitLineColor } }
    },
    yAxis: [
      {
        type: 'value',
        name: 'kW / A',
        axisLabel: { color: textColor, fontSize: 11 },
        splitLine: { lineStyle: { color: splitLineColor } }
      },
      {
        type: 'value',
        name: 'V / SoC(%)',
        axisLabel: { color: textColor, fontSize: 11 },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '전력 (kW)',
        type: 'line',
        smooth: true,
        data: powers,
        itemStyle: { color: '#0284c7' }
      },
      {
        name: '전압 (V)',
        type: 'line',
        smooth: true,
        data: voltages,
        yAxisIndex: 1,
        itemStyle: { color: '#10b981' }
      },
      {
        name: '전류 (A)',
        type: 'line',
        smooth: true,
        data: currents,
        itemStyle: { color: '#f59e0b' }
      },
      {
        name: 'SoC (%)',
        type: 'line',
        smooth: true,
        data: socs,
        yAxisIndex: 1,
        itemStyle: { color: '#8b5cf6' },
        lineStyle: { width: 3 }
      }
    ]
  })
}

onMounted(() => {
  renderChart()
  window.addEventListener('resize', handleResize)
})

const handleResize = () => {
  chart?.resize()
}

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>
