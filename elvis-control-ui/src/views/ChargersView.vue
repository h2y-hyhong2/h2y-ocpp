<script setup lang="ts">
import { ref, computed } from 'vue'
import { useCsmsStore, STATUS_MAP, type ChargerItem, type StatusKey } from '../stores/csmsStore'

const store = useCsmsStore()

// 뷰 모드 상태 (프로토타입 LAYOUT-OPTION-B.html 기준)
const topViewMode = ref<'progress' | 'heatmap'>('progress') // 프로그래스바가 1순위로 먼저 나오도록 기본 세팅
const gridViewMode = ref<'list' | 'card'>('list')
const searchQuery = ref<string>('')

// 법인 필터링된 충전소 목록
const filteredStations = computed(() => {
  if (store.curCorpFilter === 'ALL') return store.stations
  return store.stations.filter(s => s.corpId === store.curCorpFilter)
})

// 현재 선택된 충전소 정보
const currentStationInfo = computed(() => {
  if (store.curStFilter === 'ALL') {
    return { id: 'ALL', name: '전체 충전소 통합 관제', corpName: '전체 법인', count: store.chargers.length }
  }
  const st = store.stations.find(s => s.id === store.curStFilter)
  return st ? { id: st.id, name: st.name, corpName: st.corpName, count: st.chargerCount } : { id: '10001', name: store.stations[0]?.name || '', corpName: store.stations[0]?.corpName || '', count: store.stations[0]?.chargerCount || 10 }
})

// 현재 선택된 충전소에 소속된 충전기 목록
const currentStationChargers = computed(() => {
  if (store.curStFilter === 'ALL') return store.chargers
  return store.chargers.filter(c => c.stId === store.curStFilter)
})

// 필터링 및 검색된 충전기 목록 (중앙 하단 데이터 그리드용)
const filteredChargers = computed(() => {
  return currentStationChargers.value.filter((chg) => {
    const matchStatus = store.curStatusFilter === 'ALL' || chg.status === store.curStatusFilter
    const matchQuery =
      searchQuery.value === '' ||
      chg.chargeBoxId.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      chg.stationName.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      chg.carModel.toLowerCase().includes(searchQuery.value.toLowerCase())
    return matchStatus && matchQuery
  })
})

// 현재 충전소의 10대 상태별 카운트 계산
const statusCounts = computed(() => {
  const counts: Record<string, number> = {}
  Object.keys(STATUS_MAP).forEach(k => counts[k] = 0)
  currentStationChargers.value.forEach(chg => {
    if (counts[chg.status] !== undefined) counts[chg.status]++
  })
  return counts
})

// 충전소 ID 복사
function copyStationId(id: string, e?: Event) {
  if (e) e.stopPropagation()
  navigator.clipboard?.writeText(id)
  store.addToast('info', 'ID 복사 완료', `충전소 ID [${id}]가 클립보드에 복사되었습니다.`)
}

// 충전소 선택
function onSelectStation(stId: string) {
  store.setStationFilter(stId)
  if (window.innerWidth < 1024) store.toggleSidebar(false)
}

// 충전기 선택 및 제어반 열기
function onSelectCharger(chg: ChargerItem) {
  store.setSelectedCharger(chg)
  if (window.innerWidth < 1280) store.toggleControlDrawer(true)
}

// 원격 제어 커맨드 전송
function sendRemoteCommand(action: string) {
  if (!store.selectedCharger) return
  store.addToast('success', `${action} 명령 발행`, `[${store.selectedCharger.chargeBoxId}] 단말에 ${action} 커맨드가 성공적으로 전달되었습니다.`)
}
</script>

<template>
  <div class="flex h-full w-full overflow-hidden gap-2 md:gap-3 relative select-none">
    
    <!-- ========================================================================= -->
    <!-- LEFT: [시안 B] 사이드바 상단 법인 드롭다운 + 평면 충전소 목록           -->
    <!-- (1024px 미만: 슬라이드 드로어, 1024px 이상: 좌측 고정 280px 칼럼)        -->
    <!-- ========================================================================= -->
    <aside
      id="sidebarStation"
      class="fixed inset-y-0 left-0 z-50 w-72 bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl p-3 flex flex-col overflow-hidden select-none transition-transform duration-250 ease-in-out shadow-2xl
             lg:static lg:translate-x-0 lg:w-56 xl:w-64 2xl:w-72 lg:min-w-[180px] lg:max-w-xs lg:flex-shrink lg:shadow-none"
      :class="store.isSidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'"
    >
      <!-- 1. 사이드바 헤더: 충전소 목록 타이틀 + 법인 필터 드롭다운 (시안 B 핵심) -->
      <div class="pb-2.5 border-b border-[var(--border-glass)] space-y-1.5 flex-shrink-0">
        <div class="flex items-center justify-between text-xs font-bold text-[var(--text-bright)]">
          <div class="flex items-center gap-1.5 truncate">
            <svg class="w-4 h-4 text-sky-600 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"></path></svg>
            <span>충전소 목록</span>
          </div>
          <div class="flex items-center gap-1">
            <span class="text-[10px] text-sky-600 dark:text-sky-400 font-bold bg-sky-50 dark:bg-sky-950/50 px-1.5 py-0.2 rounded border border-sky-200 dark:border-sky-800">
              {{ store.stations.length }}개소
            </span>
            <!-- 소형 화면 전용 닫기 (X) 버튼 -->
            <button
              @click="store.toggleSidebar(false)"
              class="lg:hidden text-slate-400 hover:text-slate-800 p-1 font-bold text-base leading-none"
              title="사이드바 닫기"
            >✕</button>
          </div>
        </div>

        <!-- [핵심 UI - 충전소 목록 상단 필터 조건]: 법인 선택 드롭다운 -->
        <div class="space-y-1 pt-0.5">
          <label class="text-[10px] font-bold text-slate-500 flex items-center justify-between">
            <span class="flex items-center gap-1"><span>🏢</span><span>법인 필터 조건:</span></span>
          </label>
          <select
            :value="store.curCorpFilter"
            @change="store.setCorpFilter(($event.target as HTMLSelectElement).value)"
            class="w-full bg-[var(--bg-surface-2)] border border-[var(--border-glass)] rounded-lg px-2 py-1.5 text-xs font-bold text-[var(--text-bright)] outline-none focus:border-sky-500 cursor-pointer shadow-xs"
          >
            <option value="ALL">🌐 전체 법인 통합 (200개소)</option>
            <option v-for="corp in store.corps" :key="corp.id" :value="corp.id">
              🏢 {{ corp.name }}
            </option>
          </select>
        </div>
      </div>

      <!-- 2. 전체 충전소 일괄 관제 퀵 버튼 -->
      <div class="pt-2 pb-1.5 border-b border-[var(--border-glass)] flex-shrink-0">
        <div
          @click="onSelectStation('ALL')"
          class="station-mini-card flex items-center justify-between cursor-pointer"
          :class="store.curStFilter === 'ALL' ? 'station-mini-card-active' : ''"
        >
          <span class="flex items-center gap-1.5 font-bold text-xs truncate">
            <svg class="w-4 h-4 text-sky-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            <span>전체 충전소 관제</span>
          </span>
          <span class="text-[10px] opacity-75 font-bold flex-shrink-0">{{ store.chargers.length }}기</span>
        </div>
      </div>

      <!-- 3. 선택된 법인의 소속 충전소 평면 목록 (스크롤 지원) -->
      <div class="mt-2 space-y-1.5 overflow-y-auto flex-1 text-xs pr-1">
        <div
          v-for="st in filteredStations"
          :key="st.id"
          @click="onSelectStation(st.id)"
          class="station-mini-card"
          :class="store.curStFilter === st.id ? 'station-mini-card-active' : ''"
        >
          <div class="flex items-center justify-between gap-1 mb-0.5">
            <div class="flex items-center gap-1">
              <span v-if="store.curCorpFilter === 'ALL'" class="text-[9px] font-bold px-1 py-0.2 rounded bg-slate-200/50 dark:bg-slate-700/50">
                {{ st.corpShortName }}
              </span>
              <button
                type="button"
                @click="copyStationId(st.id, $event)"
                class="text-[10px] font-mono font-bold px-1.5 py-0.2 rounded border border-[var(--border-glass)] hover:bg-sky-50 dark:hover:bg-sky-950/40 flex items-center gap-0.5"
                title="충전소 ID 복사"
              >
                <span>{{ st.id }}</span>
                <span class="text-[8px] opacity-60">📋</span>
              </button>
            </div>
            <span class="text-[9px] font-bold px-1.5 py-0.2 rounded bg-sky-500/10 text-sky-600 dark:text-sky-400 border border-sky-500/20">
              {{ st.chargerCount }}기
            </span>
          </div>
          <div class="text-[11px] font-bold leading-snug truncate">
            {{ st.name }}
          </div>
        </div>
      </div>
    </aside>

    <!-- ========================================================================= -->
    <!-- CENTER: Main Content (상단 듀얼 뷰 + 하단 데이터 그리드)                  -->
    <!-- ========================================================================= -->
    <main class="flex-1 flex flex-col gap-2 md:gap-3 overflow-hidden min-w-0">
      
      <!-- 1. 중앙 상단 듀얼 뷰 (프로그레스바 ↔ 히트맵) -->
      <section class="bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl p-3 space-y-2 flex-shrink-0 shadow-xs">
        
        <!-- 상단 툴바: 충전소 타이틀 & [📊 프로그레스바 ↔ ▦ 히트맵] 뷰 스위처 -->
        <div class="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--border-glass)] pb-2">
          <div class="flex items-center gap-1.5 min-w-0">
            <span class="text-[10px] font-bold text-sky-600 dark:text-sky-400 bg-sky-50 dark:bg-sky-950/40 px-1.5 py-0.2 rounded border border-sky-200 dark:border-sky-800">
              {{ currentStationInfo.corpName }}
            </span>
            <button
              v-if="currentStationInfo.id !== 'ALL'"
              type="button"
              @click="copyStationId(currentStationInfo.id)"
              class="text-[10px] font-mono font-black text-sky-700 dark:text-sky-300 bg-[var(--bg-surface-2)] px-1.5 py-0.2 rounded border border-[var(--border-glass)]"
            >
              {{ currentStationInfo.id }}
            </button>
            <h2 class="text-xs md:text-sm font-black text-[var(--text-bright)] truncate">
              {{ currentStationInfo.name }}
            </h2>
            <span class="text-[11px] text-slate-500 font-medium hidden sm:inline">
              (총 {{ currentStationInfo.count }}기)
            </span>
          </div>

          <!-- 우측: [프로그레스바 ↔ 히트맵] 스위처 -->
          <div class="flex items-center gap-1 bg-[var(--bg-surface-2)] p-0.5 rounded-lg border border-[var(--border-glass)] text-[11px]">
            <button
              type="button"
              @click="topViewMode = 'progress'"
              class="px-2 py-0.5 rounded-md font-bold transition-all flex items-center gap-1"
              :class="topViewMode === 'progress' ? 'bg-white dark:bg-sky-900/40 text-sky-600 dark:text-sky-300 shadow-xs border border-slate-200 dark:border-sky-800' : 'text-slate-500 hover:text-slate-700'"
            >
              <span>📊</span>
              <span>프로그레스바</span>
            </button>
            <button
              type="button"
              @click="topViewMode = 'heatmap'"
              class="px-2 py-0.5 rounded-md font-bold transition-all flex items-center gap-1"
              :class="topViewMode === 'heatmap' ? 'bg-white dark:bg-sky-900/40 text-sky-600 dark:text-sky-300 shadow-xs border border-slate-200 dark:border-sky-800' : 'text-slate-500 hover:text-slate-700'"
            >
              <span>▦</span>
              <span>히트맵</span>
            </button>
          </div>
        </div>

        <!-- MODE A: 10px 슬림 스택트 바 + 파스텔 필터 칩 -->
        <div v-if="topViewMode === 'progress'" class="space-y-2">
          <!-- 10px 슬림 스택트 프로그레스바 -->
          <div class="h-2.5 w-full bg-[var(--bg-surface-2)] rounded-full overflow-hidden flex border border-[var(--border-glass)]">
            <div
              v-for="(conf, stName) in STATUS_MAP"
              :key="stName"
              :style="{ width: `${(statusCounts[stName] / Math.max(1, currentStationChargers.length)) * 100}%`, backgroundColor: conf.color }"
              class="h-full transition-all cursor-pointer"
              :title="`${stName}: ${statusCounts[stName]}대`"
              @click="store.setStatusFilter(stName)"
            ></div>
          </div>

          <!-- 상태별 인터랙티브 필터 칩 (10대 상태 미네랄 컬러) -->
          <div class="flex flex-wrap items-center gap-1.5 pt-0.5">
            <button
              type="button"
              @click="store.setStatusFilter('ALL')"
              class="status-chip"
              :class="store.curStatusFilter === 'ALL' ? 'status-chip-active' : ''"
            >
              <span>🌐 전체</span>
              <span class="text-[10px] px-1.5 py-0.1 rounded-full bg-slate-200 dark:bg-slate-700 font-mono">{{ currentStationChargers.length }}</span>
            </button>
            <button
              v-for="(conf, stName) in STATUS_MAP"
              :key="stName"
              type="button"
              @click="store.setStatusFilter(stName)"
              class="status-chip"
              :class="store.curStatusFilter === stName ? 'status-chip-active' : ''"
            >
              <span class="w-2 h-2 rounded-full inline-block" :style="{ backgroundColor: conf.color }"></span>
              <span>{{ stName }}</span>
              <span class="text-[10px] px-1.5 py-0.1 rounded-full bg-slate-200 dark:bg-slate-700 font-mono">{{ statusCounts[stName] }}</span>
            </button>
          </div>
        </div>

        <!-- MODE B: 히트맵 매트릭스 범례 및 타일 그리드 (프로토타입 규격 100% 동일) -->
        <div v-else class="space-y-2">
          <!-- 10대 상태 범례 툴바 (도트 + 상태명 + 건수/점유율) -->
          <div class="flex flex-wrap items-center gap-1 overflow-x-auto pb-0.5">
            <button
              v-for="(conf, stName) in STATUS_MAP"
              :key="stName"
              type="button"
              @click="store.setStatusFilter(stName)"
              class="flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] cursor-pointer transition-all border shadow-2xs"
              :class="store.curStatusFilter === stName ? 'border-sky-500 bg-sky-50 dark:bg-sky-950/50 font-black' : 'bg-[var(--bg-surface-2)] border-[var(--border-glass)] text-slate-600 dark:text-slate-400'"
            >
              <span class="w-2 h-2 rounded-full inline-block flex-shrink-0" :style="{ backgroundColor: conf.color }"></span>
              <span class="font-bold text-[var(--text-bright)]">{{ stName }}</span>
              <span class="text-slate-400 font-mono text-[9px]">
                ({{ statusCounts[stName] }}/{{ Math.round((statusCounts[stName] / Math.max(1, currentStationChargers.length)) * 100) }}%)
              </span>
            </button>
          </div>

          <!-- 반응형 26px x 15px 매트릭스 타일 그리드 -->
          <div class="matrix-responsive-grid">
            <div
              v-for="chg in currentStationChargers"
              :key="chg.id"
              @click="onSelectCharger(chg)"
              class="matrix-cell"
              :class="[
                STATUS_MAP[chg.status]?.cellClass || 'cell-unknown',
                store.selectedCharger?.id === chg.id ? 'matrix-active-ring' : '',
                store.curStatusFilter !== 'ALL' && chg.status !== store.curStatusFilter ? 'opacity-25' : ''
              ]"
              :title="`[충전기 ${chg.cpId}호기] 상태: ${chg.status} | 출력: ${chg.powerKw.toFixed(1)}kW | SoC: ${chg.socPercent}%`"
            >
              {{ chg.cpId }}
            </div>

            <!-- 200개 포트 대비 빈 슬롯 채우기 -->
            <div
              v-for="i in Math.max(0, 100 - currentStationChargers.length)"
              :key="`empty-${i}`"
              class="matrix-cell-empty"
            ></div>
          </div>
        </div>
      </section>

      <!-- 2. 중앙 하단 충전기 데이터 그리드 -->
      <section class="bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl p-3 flex-1 flex flex-col overflow-hidden shadow-xs">
        
        <!-- 툴바: 건수 뱃지, 검색창, 리스트/카드 모드 스위처 -->
        <div class="flex flex-wrap items-center justify-between gap-2 pb-2.5 border-b border-[var(--border-glass)] flex-shrink-0">
          <div class="flex items-center gap-2">
            <span class="text-xs font-bold text-[var(--text-bright)]">충전 단말 목록</span>
            <span class="text-[11px] text-slate-500 font-bold bg-[var(--bg-surface-2)] px-1.5 py-0.2 rounded border border-[var(--border-glass)]">
              {{ filteredChargers.length }}건
            </span>
          </div>

          <div class="flex items-center gap-2">
            <!-- 검색창 -->
            <div class="relative w-48 sm:w-60">
              <input
                v-model="searchQuery"
                type="text"
                placeholder="충전기 ID, 차종, 충전소..."
                class="w-full bg-[var(--bg-surface-2)] border border-[var(--border-glass)] rounded-lg pl-7 pr-2 py-1 text-xs outline-none focus:border-sky-500 text-[var(--text-bright)]"
              />
              <span class="absolute left-2 top-1.5 text-slate-400 text-xs">🔍</span>
            </div>

            <!-- 리스트/카드 스위처 -->
            <div class="flex items-center bg-[var(--bg-surface-2)] p-0.5 rounded-lg border border-[var(--border-glass)] text-xs">
              <button
                type="button"
                @click="gridViewMode = 'list'"
                class="px-2 py-0.5 rounded font-bold"
                :class="gridViewMode === 'list' ? 'bg-white dark:bg-sky-900/40 text-sky-600 dark:text-sky-300 shadow-xs border border-slate-200 dark:border-sky-800' : 'text-slate-500'"
              >
                📋
              </button>
              <button
                type="button"
                @click="gridViewMode = 'card'"
                class="px-2 py-0.5 rounded font-bold"
                :class="gridViewMode === 'card' ? 'bg-white dark:bg-sky-900/40 text-sky-600 dark:text-sky-300 shadow-xs border border-slate-200 dark:border-sky-800' : 'text-slate-500'"
              >
                🗂️
              </button>
            </div>
          </div>
        </div>

        <!-- MODE 1: 리스트 테이블 뷰 (창 축소 시 가로 스크롤 & 텍스트 줄바꿈 방지) -->
        <div v-if="gridViewMode === 'list'" class="flex-1 overflow-x-auto overflow-y-auto mt-2 min-w-0">
          <table class="table-theme w-full text-xs min-w-[680px]">
            <thead>
              <tr class="text-left text-slate-500 border-b border-[var(--border-glass)] pb-1">
                <th class="py-1.5 px-2 whitespace-nowrap">충전기 ID</th>
                <th class="py-1.5 px-2 whitespace-nowrap">충전소명</th>
                <th class="py-1.5 px-2 whitespace-nowrap">연결 차종</th>
                <th class="py-1.5 px-2 whitespace-nowrap">상태</th>
                <th class="py-1.5 px-2 whitespace-nowrap">출력(kW)</th>
                <th class="py-1.5 px-2 whitespace-nowrap">전압/전류</th>
                <th class="py-1.5 px-2 whitespace-nowrap">배터리 SoC</th>
                <th class="py-1.5 px-2 whitespace-nowrap">최종 통신</th>
                <th class="py-1.5 px-2 text-right whitespace-nowrap">원격 제어</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="chg in filteredChargers"
                :key="chg.id"
                @click="onSelectCharger(chg)"
                class="border-b border-[var(--border-glass)] hover:bg-[var(--bg-surface-2)] cursor-pointer transition-colors"
                :class="store.selectedCharger?.id === chg.id ? 'bg-sky-500/10 dark:bg-sky-900/30' : ''"
              >
                <td class="py-2 px-2 font-bold text-sky-600 dark:text-sky-400 whitespace-nowrap">{{ chg.chargeBoxId }}</td>
                <td class="py-2 px-2 font-medium truncate max-w-[130px] whitespace-nowrap">{{ chg.stationName }}</td>
                <td class="py-2 px-2 text-slate-600 dark:text-slate-400 truncate max-w-[110px] whitespace-nowrap">{{ chg.carModel }}</td>
                <td class="py-2 px-2 whitespace-nowrap">
                  <span :class="STATUS_MAP[chg.status]?.badgeClass" class="px-2 py-0.5 rounded text-[10px] font-bold border whitespace-nowrap inline-flex items-center">
                    {{ chg.status }}
                  </span>
                </td>
                <td class="py-2 px-2 font-bold whitespace-nowrap">{{ chg.powerKw > 0 ? `${chg.powerKw.toFixed(1)} kW` : '-' }}</td>
                <td class="py-2 px-2 text-slate-500 whitespace-nowrap">{{ chg.powerKw > 0 ? `${chg.voltageV.toFixed(0)}V / ${chg.currentA.toFixed(0)}A` : '-' }}</td>
                <td class="py-2 px-2 whitespace-nowrap">
                  <div v-if="chg.status === '충전중'" class="w-20">
                    <div class="flex justify-between text-[10px] mb-0.5">
                      <span>{{ chg.socPercent }}%</span>
                    </div>
                    <div class="h-1.5 bg-slate-200 dark:bg-slate-700 rounded-full overflow-hidden">
                      <div class="h-full bg-sky-500" :style="{ width: `${chg.socPercent}%` }"></div>
                    </div>
                  </div>
                  <span v-else class="text-slate-400">-</span>
                </td>
                <td class="py-2 px-2 text-[11px] text-slate-500 whitespace-nowrap">{{ chg.lastHeartbeat }}</td>
                <td class="py-2 px-2 text-right whitespace-nowrap">
                  <button
                    @click.stop="onSelectCharger(chg)"
                    class="px-2 py-1 rounded bg-sky-50 dark:bg-sky-950/40 text-sky-600 dark:text-sky-400 border border-sky-200 dark:border-sky-800 text-[10px] font-bold hover:bg-sky-100 whitespace-nowrap"
                  >
                    제어반
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- MODE 2: 카드 그리드 뷰 -->
        <div v-else class="flex-1 overflow-y-auto mt-2 grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-2.5 pr-1">
          <div
            v-for="chg in filteredChargers"
            :key="chg.id"
            @click="onSelectCharger(chg)"
            class="station-mini-card p-2.5 flex flex-col justify-between"
            :class="store.selectedCharger?.id === chg.id ? 'border-sky-500 shadow-md ring-1 ring-sky-500' : ''"
          >
            <div>
              <div class="flex items-center justify-between">
                <span class="font-mono font-bold text-sky-600 dark:text-sky-400 text-xs">{{ chg.chargeBoxId }}</span>
                <span :class="STATUS_MAP[chg.status]?.badgeClass" class="px-1.5 py-0.2 rounded text-[10px] font-bold border">
                  {{ chg.status }}
                </span>
              </div>
              <div class="text-xs font-bold text-[var(--text-bright)] truncate mt-1">{{ chg.stationName }}</div>
              <div class="text-[10px] text-slate-500 truncate">{{ chg.carModel }} ({{ chg.userTag }})</div>
              
              <div class="mt-2 p-2 rounded bg-[var(--bg-surface-2)] font-mono text-[11px] space-y-1">
                <div class="flex justify-between">
                  <span class="text-slate-500">출력:</span>
                  <span class="font-bold text-sky-600 dark:text-sky-400">{{ chg.powerKw > 0 ? `${chg.powerKw.toFixed(1)} kW` : '-' }}</span>
                </div>
                <div v-if="chg.status === '충전중'" class="pt-0.5">
                  <div class="flex justify-between text-[10px] mb-0.5">
                    <span class="text-slate-500">SoC / 배터리온도:</span>
                    <span class="font-bold text-emerald-600">{{ chg.socPercent }}% ({{ chg.batteryTempC }}°C)</span>
                  </div>
                  <div class="h-1.5 bg-slate-200 dark:bg-slate-700 rounded-full overflow-hidden">
                    <div class="h-full bg-emerald-500" :style="{ width: `${chg.socPercent}%` }"></div>
                  </div>
                </div>
              </div>
            </div>

            <div class="mt-2 pt-1.5 border-t border-[var(--border-glass)] flex items-center justify-between text-[10px] font-mono text-slate-500">
              <span>{{ chg.lastHeartbeat }}</span>
              <button class="text-sky-600 font-bold hover:underline">상세 제어 ➔</button>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- ========================================================================= -->
    <!-- RIGHT: [프로토타입 ControlDrawer.js 100% 동일 규격] 실시간 제어반 드로어  -->
    <!-- (1280px 미만: 우측 슬라이드 드로어, 1280px 이상: 우측 고정 3단 칼럼)      -->
    <!-- ========================================================================= -->
    <aside
      id="controlDrawer"
      class="fixed inset-y-0 right-0 z-50 w-80 bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl p-3 flex flex-col justify-between overflow-y-auto select-none transition-transform duration-300 ease-in-out shadow-2xl
             xl:static xl:translate-x-0 xl:w-56 2xl:w-64 xl:min-w-[180px] xl:max-w-xs xl:flex-shrink xl:shadow-none"
      :class="store.isControlDrawerOpen ? 'translate-x-0' : 'translate-x-full xl:translate-x-0'"
    >
      <div class="space-y-2.5">
        <!-- 헤더 영역 -->
        <div class="flex items-center justify-between pb-2 border-b border-[var(--border-glass)]">
          <div class="flex items-center gap-1.5 truncate">
            <svg class="w-4 h-4 text-sky-600 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"></path></svg>
            <h2 class="text-xs font-bold text-[var(--text-bright)] truncate">단말 계측치 · 원격제어</h2>
          </div>
          <div class="flex items-center gap-1.5">
            <span v-if="store.selectedCharger" :class="STATUS_MAP[store.selectedCharger.status]?.badgeClass" class="text-[10px] px-1.5 py-0.2 rounded border font-bold">
              {{ store.selectedCharger.status }}
            </span>
            <!-- 닫기 버튼 (xl: 1280px 미만 소형 창에서 노출) -->
            <button
              @click="store.toggleControlDrawer(false)"
              class="xl:hidden p-1 rounded-md text-slate-400 hover:text-slate-800 font-bold text-base leading-none"
              title="제어반 닫기"
            >✕</button>
          </div>
        </div>

        <div v-if="store.selectedCharger" class="space-y-2.5 text-xs">
          <!-- 단말 기본 정보 (충전소명, ID, 법인, 규격) -->
          <div class="p-2.5 rounded-xl bg-[var(--bg-surface-2)] border border-[var(--border-glass)] space-y-1">
            <div class="flex items-center justify-between text-xs">
              <span class="font-bold text-sky-600 dark:text-sky-400 font-mono">{{ store.selectedCharger.chargeBoxId }}</span>
              <span class="text-[10px] font-bold text-slate-500">{{ store.selectedCharger.spec }}</span>
            </div>
            <div class="text-[11px] font-bold text-[var(--text-bright)] truncate">{{ store.selectedCharger.stationName }}</div>
            <div class="text-[10px] text-slate-500 font-mono flex justify-between pt-0.5">
              <span>{{ store.selectedCharger.vendor }}</span>
              <span>{{ store.selectedCharger.protocol }}</span>
            </div>
            <div class="text-[10px] text-sky-700 dark:text-sky-300 font-bold pt-0.5 flex justify-between">
              <span>연결: {{ store.selectedCharger.carModel }}</span>
            </div>
          </div>

          <!-- 실시간 텔레메트리 계측치 (OPER_RECHGNG_HIST) -->
          <div class="p-2.5 rounded-xl bg-[var(--bg-surface-2)] border border-[var(--border-glass)] space-y-2">
            <div class="flex items-center justify-between text-[11px] font-bold text-slate-500">
              <span>실시간 충전 계측치</span>
              <span class="text-emerald-600 font-mono text-[10px]">1초 스트리밍</span>
            </div>

            <!-- 순시 전력 & 게이지 -->
            <div class="space-y-1">
              <div class="flex items-baseline justify-between">
                <span class="text-[11px] text-slate-500">순시 출력</span>
                <span class="text-sm font-black font-mono text-sky-600 dark:text-sky-400">{{ store.selectedCharger.powerKw.toFixed(1) }} kW</span>
              </div>
              <div class="w-full bg-slate-200 dark:bg-slate-700 h-1.5 rounded-full overflow-hidden">
                <div class="bg-sky-500 h-full rounded-full" :style="{ width: `${Math.min(100, (store.selectedCharger.powerKw / 350) * 100)}%` }"></div>
              </div>
            </div>

            <div class="grid grid-cols-2 gap-1.5 pt-1 font-mono text-[11px]">
              <div class="p-1.5 rounded bg-[var(--bg-surface-1)] border border-[var(--border-glass)]">
                <div class="text-[9px] text-slate-500">충전 전압</div>
                <div class="font-bold">{{ store.selectedCharger.voltageV.toFixed(0) }} V</div>
              </div>
              <div class="p-1.5 rounded bg-[var(--bg-surface-1)] border border-[var(--border-glass)]">
                <div class="text-[9px] text-slate-500">충전 전류</div>
                <div class="font-bold">{{ store.selectedCharger.currentA.toFixed(0) }} A</div>
              </div>
              <div class="p-1.5 rounded bg-[var(--bg-surface-1)] border border-[var(--border-glass)]">
                <div class="text-[9px] text-slate-500">누적 전력량</div>
                <div class="font-bold">{{ store.selectedCharger.accumulatedKwh.toFixed(1) }} kWh</div>
              </div>
              <div class="p-1.5 rounded bg-[var(--bg-surface-1)] border border-[var(--border-glass)]">
                <div class="text-[9px] text-slate-500">배터리 온도</div>
                <div class="font-bold text-amber-600">{{ store.selectedCharger.batteryTempC }} °C</div>
              </div>
            </div>

            <!-- 배터리 SoC 바 -->
            <div class="space-y-1 pt-1">
              <div class="flex items-baseline justify-between text-[11px]">
                <span class="text-slate-500">배터리 SoC</span>
                <span class="font-bold font-mono text-emerald-600 dark:text-emerald-400">{{ store.selectedCharger.socPercent }}%</span>
              </div>
              <div class="w-full bg-slate-200 dark:bg-slate-700 h-1.5 rounded-full overflow-hidden">
                <div class="bg-emerald-500 h-full rounded-full" :style="{ width: `${store.selectedCharger.socPercent}%` }"></div>
              </div>
            </div>
          </div>

          <!-- OCPP 원격 제어 커맨드 4종 버튼 -->
          <div class="space-y-1.5 pt-1">
            <div class="text-[11px] font-bold text-slate-500">OCPP 1.6/2.0.1 원격 제어</div>
            <div class="grid grid-cols-2 gap-1.5 font-bold text-[11px]">
              <button
                type="button"
                @click="sendRemoteCommand('RemoteReset')"
                class="p-2 rounded-lg bg-[var(--bg-surface-2)] hover:bg-slate-200 dark:hover:bg-slate-800 text-[var(--text-bright)] border border-[var(--border-glass)] transition-all flex items-center justify-center gap-1 shadow-xs"
              >
                🔄 단말 리셋
              </button>
              <button
                type="button"
                @click="sendRemoteCommand('UnlockConnector')"
                class="p-2 rounded-lg bg-[var(--bg-surface-2)] hover:bg-slate-200 dark:hover:bg-slate-800 text-[var(--text-bright)] border border-[var(--border-glass)] transition-all flex items-center justify-center gap-1 shadow-xs"
              >
                🔓 커넥터 잠금해제
              </button>
              <button
                type="button"
                @click="sendRemoteCommand('RemoteStartTransaction')"
                class="p-2 rounded-lg bg-sky-600 hover:bg-sky-700 text-white transition-all flex items-center justify-center gap-1 shadow-xs font-black"
              >
                ⚡ 충전 시작
              </button>
              <button
                type="button"
                @click="sendRemoteCommand('RemoteStopTransaction')"
                class="p-2 rounded-lg bg-rose-600 hover:bg-rose-700 text-white transition-all flex items-center justify-center gap-1 shadow-xs font-black"
              >
                🛑 충전 강제종료
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="text-[10px] font-mono text-slate-400 border-t border-[var(--border-glass)] pt-2 text-center">
        Kafka Event Driven Engine Active
      </div>
    </aside>
  </div>
</template>
