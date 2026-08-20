<template>
  <div class="space-y-5 pb-6">
    <!-- Top Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <h1 class="text-xl font-bold font-display text-slate-800 dark:text-slate-100 tracking-tight">실시간 통합 관제 대시보드</h1>
        <span class="font-mono text-xs px-2 py-0.5 rounded-md font-semibold bg-sky-500/10 text-sky-600 dark:text-sky-400 border border-sky-500/30">PAGE-DASH-01</span>
      </div>
      <div class="flex items-center gap-2 text-xs text-slate-500 font-mono">
        <span class="live-indicator"></span>
        <span class="text-emerald-600 dark:text-emerald-400 font-medium">1초 주기 시계열 스트리밍 활성화</span>
      </div>
    </div>

    <!-- 1. KPI 4대 요약 카드 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <!-- Card 1: 총 충전기 -->
      <div class="glass-card p-4">
        <div class="flex items-center justify-between text-xs text-slate-500 dark:text-slate-400 font-medium">
          <span>총 관제 충전기</span>
          <span class="text-sky-500 text-base">⚡</span>
        </div>
        <div class="mt-2 flex items-baseline justify-between">
          <span class="text-2xl font-bold font-display tracking-tight text-slate-900 dark:text-slate-100">
            {{ store.summary.totalChargers.toLocaleString() }} <span class="text-xs font-normal text-slate-500">대</span>
          </span>
          <span class="text-xs font-semibold font-mono text-emerald-600 dark:text-emerald-400">95.1% 정상 가동</span>
        </div>
        <div class="mt-3 w-full bg-slate-200 dark:bg-slate-800 h-1.5 rounded-full overflow-hidden">
          <div class="bg-gradient-to-r from-sky-500 to-emerald-400 h-full rounded-full" style="width: 95.1%"></div>
        </div>
      </div>

      <!-- Card 2: 실시간 충전중 -->
      <div class="glass-card p-4">
        <div class="flex items-center justify-between text-xs text-slate-500 dark:text-slate-400 font-medium">
          <span>실시간 충전 세션</span>
          <span class="text-violet-500 text-base">🔌</span>
        </div>
        <div class="mt-2 flex items-baseline justify-between">
          <span class="text-2xl font-bold font-display tracking-tight text-slate-900 dark:text-slate-100">
            {{ store.summary.chargingCount.toLocaleString() }} <span class="text-xs font-normal text-slate-500">세션</span>
          </span>
          <span class="text-xs font-semibold font-mono text-violet-600 dark:text-violet-400">{{ (store.summary.totalPowerKw / 1000).toFixed(1) }} MW 공급중</span>
        </div>
        <div class="mt-3 w-full bg-slate-200 dark:bg-slate-800 h-1.5 rounded-full overflow-hidden">
          <div class="bg-gradient-to-r from-violet-500 to-fuchsia-400 h-full rounded-full" style="width: 42.1%"></div>
        </div>
      </div>

      <!-- Card 3: 실시간 수용 TPS -->
      <div class="glass-card p-4">
        <div class="flex items-center justify-between text-xs text-slate-500 dark:text-slate-400 font-medium">
          <span>실시간 처리 패킷 (TPS)</span>
          <span class="text-emerald-500 text-base">🚀</span>
        </div>
        <div class="mt-2 flex items-baseline justify-between">
          <span class="text-2xl font-bold font-display tracking-tight text-slate-900 dark:text-slate-100">
            {{ currentTps.toLocaleString() }} <span class="text-xs font-normal text-slate-500">TPS</span>
          </span>
          <span class="text-xs font-semibold font-mono text-emerald-600 dark:text-emerald-400">지연 &lt; 2ms</span>
        </div>
        <div class="mt-3 w-full bg-slate-200 dark:bg-slate-800 h-1.5 rounded-full overflow-hidden">
          <div class="bg-gradient-to-r from-emerald-500 to-teal-400 h-full rounded-full" style="width: 96%"></div>
        </div>
      </div>

      <!-- Card 4: 오늘 누적 충전량 -->
      <div class="glass-card p-4">
        <div class="flex items-center justify-between text-xs text-slate-500 dark:text-slate-400 font-medium">
          <span>금일 누적 공급 전력량</span>
          <span class="text-amber-500 text-base">🔋</span>
        </div>
        <div class="mt-2 flex items-baseline justify-between">
          <span class="text-2xl font-bold font-display tracking-tight text-slate-900 dark:text-slate-100">
            {{ (store.summary.todayTotalKwh / 1000).toFixed(1) }} <span class="text-xs font-normal text-slate-500">MWh</span>
          </span>
          <span class="text-xs font-semibold font-mono text-amber-600 dark:text-amber-400">+12.4% vs 전일</span>
        </div>
        <div class="mt-3 w-full bg-slate-200 dark:bg-slate-800 h-1.5 rounded-full overflow-hidden">
          <div class="bg-gradient-to-r from-amber-500 to-orange-400 h-full rounded-full" style="width: 78%"></div>
        </div>
      </div>
    </div>

    <!-- 2. 차트 영역 (좌측: 실시간 전력/단가 복합 차트, 우측: 상태 도넛) -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-5">
      <!-- 실시간 전력 & TOU 단가 차트 (2 cols) -->
      <div class="glass-card p-5 lg:col-span-2 flex flex-col justify-between">
        <div class="flex items-center justify-between mb-3">
          <div>
            <h2 class="text-sm font-bold font-display text-slate-800 dark:text-slate-200">실시간 전력 부하(kW) & TOU 단가(원/kWh)</h2>
            <p class="text-xs text-slate-500">1초 주기 시계열 OLAP 집계 스트림</p>
          </div>
          <div class="flex items-center gap-3 text-xs font-mono">
            <span class="flex items-center gap-1 text-sky-500"><span class="w-2.5 h-2.5 rounded-sm bg-sky-500 inline-block"></span> 공급전력 (kW)</span>
            <span class="flex items-center gap-1 text-violet-500"><span class="w-2.5 h-2.5 rounded-sm bg-violet-500 inline-block"></span> 실시간 단가 (원)</span>
          </div>
        </div>
        <div ref="lineChartRef" class="w-full h-64"></div>
      </div>

      <!-- 충전기 상태별 도넛 차트 (1 col) -->
      <div class="glass-card p-5 flex flex-col justify-between">
        <div class="flex items-center justify-between mb-3">
          <h2 class="text-sm font-bold font-display text-slate-800 dark:text-slate-200">충전기 상태 분포</h2>
          <span class="text-xs text-slate-500 font-mono">총 2,000대</span>
        </div>
        <div ref="pieChartRef" class="w-full h-48"></div>
        <div class="grid grid-cols-2 gap-2 mt-2 pt-3 border-t border-slate-200 dark:border-slate-800 text-xs">
          <div class="flex items-center justify-between">
            <span class="text-slate-500 flex items-center gap-1.5"><span class="w-2 h-2 rounded-full bg-violet-500"></span>충전중</span>
            <span class="font-mono font-semibold text-slate-800 dark:text-slate-200">{{ store.summary.chargingCount }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-slate-500 flex items-center gap-1.5"><span class="w-2 h-2 rounded-full bg-emerald-500"></span>대기중</span>
            <span class="font-mono font-semibold text-slate-800 dark:text-slate-200">{{ store.summary.availableCount }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-slate-500 flex items-center gap-1.5"><span class="w-2 h-2 rounded-full bg-rose-500"></span>고장/장애</span>
            <span class="font-mono font-semibold text-rose-500">{{ store.summary.faultedCount }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-slate-500 flex items-center gap-1.5"><span class="w-2 h-2 rounded-full bg-amber-500"></span>준비/점검</span>
            <span class="font-mono font-semibold text-amber-500">{{ store.summary.preparingCount }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. 하단 실시간 단말 이벤트 피드 -->
    <div class="glass-card p-5">
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-2">
          <h2 class="text-sm font-bold font-display text-slate-800 dark:text-slate-200">실시간 단말 통신 & 장애 이벤트 피드</h2>
          <span class="live-indicator"></span>
        </div>
        <span class="text-xs text-slate-500 font-mono">초당 수신 이벤트 실시간 표출</span>
      </div>

      <div class="overflow-x-auto">
        <table class="table-theme">
          <thead>
            <tr>
              <th>시간</th>
              <th>구분</th>
              <th>충전기 ID</th>
              <th>OCPP Action</th>
              <th>메시지 상세 내용</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in store.recentLogs" :key="log.id">
              <td class="font-mono text-xs text-slate-500">{{ log.timestamp }}</td>
              <td>
                <span :class="log.direction === 'INBOUND' ? 'text-sky-500 bg-sky-500/10 border-sky-500/30' : 'text-violet-500 bg-violet-500/10 border-violet-500/30'" class="px-2 py-0.5 rounded text-[11px] font-mono border">
                  {{ log.direction }}
                </span>
              </td>
              <td class="font-mono font-semibold text-slate-800 dark:text-slate-200">{{ log.chargeBoxId }}</td>
              <td class="font-mono font-bold text-sky-600 dark:text-sky-400">{{ log.action }}</td>
              <td class="font-mono text-xs text-slate-600 dark:text-slate-400 truncate max-w-md">{{ log.payloadJson }}</td>
              <td>
                <span class="badge badge-available text-[10px]">정상 수신</span>
              </td>
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
const currentTps = ref(1920)

const lineChartRef = ref<HTMLDivElement | null>(null)
const pieChartRef = ref<HTMLDivElement | null>(null)

let lineChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
let intervalTimer: number | null = null

// 시계열 데이터 버퍼
const timeLabels: string[] = []
const powerData: number[] = []
const priceData: number[] = []

// 초기 데이터 10개 생성
const initData = () => {
  const now = new Date()
  for (let i = 9; i >= 0; i--) {
    const t = new Date(now.getTime() - i * 1000)
    timeLabels.push(t.toTimeString().split(' ')[0])
    powerData.push(Math.round(28400 + Math.random() * 200))
    priceData.push(Math.round(280 + Math.sin(i) * 20))
  }
}

const renderLineChart = () => {
  if (!lineChartRef.value) return
  if (!lineChart) lineChart = echarts.init(lineChartRef.value)

  const isDark = document.documentElement.getAttribute('data-theme') !== 'light'
  const textColor = isDark ? '#94a3b8' : '#475569'
  const splitLineColor = isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)'

  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '8%', top: '12%', containLabel: true },
    xAxis: {
      type: 'category',
      data: timeLabels,
      axisLabel: { color: textColor, fontSize: 11 },
      axisLine: { lineStyle: { color: splitLineColor } }
    },
    yAxis: [
      {
        type: 'value',
        name: 'kW',
        min: 28000,
        axisLabel: { color: textColor, fontSize: 11 },
        splitLine: { lineStyle: { color: splitLineColor } }
      },
      {
        type: 'value',
        name: '원',
        min: 200,
        max: 400,
        axisLabel: { color: textColor, fontSize: 11 },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '공급전력 (kW)',
        type: 'line',
        smooth: true,
        data: powerData,
        yAxisIndex: 0,
        itemStyle: { color: '#0284c7' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(2, 132, 199, 0.35)' },
            { offset: 1, color: 'rgba(2, 132, 199, 0.0)' }
          ])
        }
      },
      {
        name: '실시간 단가 (원)',
        type: 'line',
        smooth: true,
        data: priceData,
        yAxisIndex: 1,
        itemStyle: { color: '#7c3aed' },
        lineStyle: { width: 2, type: 'dashed' }
      }
    ]
  })
}

const renderPieChart = () => {
  if (!pieChartRef.value) return
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)

  pieChart.setOption({
    tooltip: { trigger: 'item' },
    series: [
      {
        name: '충전기 상태',
        type: 'pie',
        radius: ['55%', '80%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: 'transparent',
          borderWidth: 2
        },
        label: { show: false },
        data: [
          { value: store.summary.chargingCount, name: '충전중', itemStyle: { color: '#7c3aed' } },
          { value: store.summary.availableCount, name: '대기중', itemStyle: { color: '#10b981' } },
          { value: store.summary.faultedCount, name: '장애', itemStyle: { color: '#f43f5e' } },
          { value: store.summary.preparingCount, name: '점검', itemStyle: { color: '#f59e0b' } }
        ]
      }
    ]
  })
}

onMounted(() => {
  initData()
  renderLineChart()
  renderPieChart()

  // 1초 실시간 갱신 루프
  intervalTimer = window.setInterval(() => {
    const now = new Date()
    timeLabels.shift()
    timeLabels.push(now.toTimeString().split(' ')[0])

    const nextPower = Math.round(28400 + Math.random() * 300 - 150)
    powerData.shift()
    powerData.push(nextPower)

    const nextPrice = Math.round(280 + Math.sin(now.getSeconds() / 5) * 20)
    priceData.shift()
    priceData.push(nextPrice)

    currentTps.value = Math.round(1920 + Math.random() * 60 - 30)

    renderLineChart()
  }, 1000)

  window.addEventListener('resize', handleResize)
})

const handleResize = () => {
  lineChart?.resize()
  pieChart?.resize()
}

onUnmounted(() => {
  if (intervalTimer) clearInterval(intervalTimer)
  window.removeEventListener('resize', handleResize)
  lineChart?.dispose()
  pieChart?.dispose()
})
</script>
