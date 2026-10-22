<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useCsmsStore, STATUS_MAP, type ChargerItem } from '../stores/csmsStore'

const store = useCsmsStore()

onMounted(() => {
  store.fetchInitialData()
})

const searchQuery = ref<string>('')

// 법인 필터링된 충전소 목록
const filteredStations = computed(() => {
  if (store.curCorpFilter === 'ALL') return store.stations
  return store.stations.filter(s => s.corpId === store.curCorpFilter)
})

// 현재 선택된 충전소 정보
const currentStationInfo = computed(() => {
  if (store.curStFilter === 'ALL' || store.stations.length === 0) {
    return {
      id: 'ALL',
      name: store.stations.length === 0 ? '등록된 충전소 없음 (DB 초기화 상태)' : '전체 충전소 통합 관제',
      corpName: store.corps.length === 0 ? '미등록' : '전체 법인',
      count: store.chargers.length
    }
  }
  const st = store.stations.find(s => s.id === store.curStFilter)
  return st ? { id: st.id, name: st.name, corpName: st.corpName, count: st.chargerCount } : { id: '-', name: store.stations[0]?.name || '등록된 충전소 없음', corpName: store.stations[0]?.corpName || '미등록', count: store.stations[0]?.chargerCount || 0 }
})

// 현재 선택된 충전소에 소속된 충전기 목록
const currentStationChargers = computed(() => {
  if (store.curStFilter === 'ALL') return store.chargers
  return store.chargers.filter(c => c.stId === store.curStFilter)
})

// 필터링 및 검색된 충전기 목록
const filteredChargers = computed(() => {
  const q = (searchQuery.value || '').toLowerCase().trim()
  return currentStationChargers.value.filter((chg) => {
    const matchStatus = store.curStatusFilter === 'ALL' || chg.status === store.curStatusFilter
    if (!q) return matchStatus
    const matchQuery =
      (chg.chargeBoxId || '').toLowerCase().includes(q) ||
      (chg.stationName || '').toLowerCase().includes(q) ||
      (chg.carModel || '').toLowerCase().includes(q)
    return matchStatus && matchQuery
  })
})

// 상태별 카운트 계산
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
  store.addToast({ type: 'info', title: 'ID 복사 완료', detail: `충전소 ID [${id}]가 클립보드에 복사되었습니다.` })
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
  store.sendRemoteAction(action)
}
</script>

<template>
  <div class="flex h-full w-full overflow-hidden gap-2 md:gap-3 relative select-none">
    
    <!-- LEFT: 충전소 목록 사이드바 (260px) -->
    <aside
      id="sidebarStation"
      class="fixed inset-y-0 left-0 z-50 w-72 bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl p-3 flex flex-col overflow-hidden select-none transition-transform duration-200 ease-in-out shadow-2xl
             lg:static lg:translate-x-0 lg:w-56 xl:w-64 lg:min-w-[180px] lg:max-w-xs lg:flex-shrink lg:shadow-none"
      :class="store.isSidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'"
    >
      <!-- 1. 사이드바 헤더 -->
      <div class="pb-2.5 border-b border-[var(--border-glass)] space-y-1.5 flex-shrink-0">
        <div class="flex items-center justify-between text-xs font-bold text-[var(--text-bright)]">
          <div class="flex items-center gap-1.5 truncate">
            <svg class="w-4 h-4 text-sky-600 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"></path></svg>
            <span>충전소 목록</span>
          </div>
          <div class="flex items-center gap-1">
            <span class="text-[10px] text-sky-600 font-bold bg-sky-50 px-1.5 py-0.2 rounded border border-sky-200">
              {{ store.stations.length }}개소
            </span>
            <button
              @click="store.toggleSidebar(false)"
              class="lg:hidden text-slate-400 hover:text-slate-800 p-1 font-bold text-base leading-none cursor-pointer"
              title="사이드바 닫기"
            >✕</button>
          </div>
        </div>

        <!-- 법인 선택 드롭다운 -->
        <div class="pt-0.5">
          <select
            :value="store.curCorpFilter"
            @change="store.setCorpFilter(($event.target as HTMLSelectElement).value)"
            class="w-full bg-[var(--bg-surface-2)] border border-[var(--border-glass)] rounded-lg px-2 py-1.5 text-xs font-semibold text-[var(--text-bright)] outline-hidden focus:border-sky-500 cursor-pointer shadow-xs"
          >
            <option value="ALL">전체 법인 통합 ({{ store.stations.length }}개소)</option>
            <option v-for="corp in store.corps" :key="corp.id" :value="corp.id">
              {{ corp.name }}
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

      <!-- 3. 충전소 목록 -->
      <div class="mt-2 space-y-1.5 overflow-y-auto flex-1 text-xs pr-1">
        <div v-if="filteredStations.length === 0" class="p-4 text-center text-slate-400 space-y-1 my-4">
          <svg class="w-8 h-8 mx-auto text-slate-300" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" /></svg>
          <div class="font-bold text-[11px] text-slate-600">등록된 충전소가 없습니다</div>
          <div class="text-[10px] text-slate-500">DB 초기화 상태 (0건)</div>
        </div>

        <div
          v-for="st in filteredStations"
          :key="st.id"
          @click="onSelectStation(st.id)"
          class="station-mini-card cursor-pointer"
          :class="store.curStFilter === st.id ? 'station-mini-card-active' : ''"
        >
          <div class="flex items-center justify-between gap-1 mb-0.5">
            <div class="flex items-center gap-1">
              <span v-if="store.curCorpFilter === 'ALL'" class="text-[9px] font-bold px-1 py-0.2 rounded bg-slate-200/50">
                {{ st.corpShortName }}
              </span>
              <button
                type="button"
                @click="copyStationId(st.id, $event)"
                class="text-[10px] font-mono font-bold px-1.5 py-0.2 rounded border border-[var(--border-glass)] hover:bg-sky-50 flex items-center gap-1 cursor-pointer"
                title="충전소 ID 복사"
              >
                <span>{{ st.id }}</span>
                <svg class="w-2.5 h-2.5 opacity-60" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" /></svg>
              </button>
            </div>
            <span class="text-[9px] font-bold px-1.5 py-0.2 rounded bg-sky-500/10 text-sky-600 border border-sky-500/20">
              {{ st.chargerCount }}기
            </span>
          </div>
          <div class="text-[11px] font-bold leading-snug truncate">
            {{ st.name }}
          </div>
        </div>
      </div>
    </aside>

    <!-- CENTER: 메인 충전기 데이터 테이블 영역 -->
    <main class="flex-1 flex flex-col gap-2 md:gap-3 overflow-hidden min-w-0">
      
      <!-- 1. 중앙 상단 실시간 상태 요약 카드 바 -->
      <section class="bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl p-3 space-y-2 flex-shrink-0 shadow-xs">
        
        <!-- 충전소 타이틀 바 -->
        <div class="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--border-glass)] pb-2">
          <div class="flex items-center gap-1.5 min-w-0">
            <span class="text-[10px] font-bold text-sky-600 bg-sky-50 px-1.5 py-0.2 rounded border border-sky-200">
              {{ currentStationInfo.corpName }}
            </span>
            <button
              v-if="currentStationInfo.id !== 'ALL'"
              type="button"
              @click="copyStationId(currentStationInfo.id)"
              class="text-[10px] font-mono font-black text-sky-700 bg-[var(--bg-surface-2)] px-1.5 py-0.2 rounded border border-[var(--border-glass)] cursor-pointer"
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
        </div>

        <!-- 상태 칩 바 (클릭 시 즉시 해당 상태 필터링) -->
        <div class="flex flex-wrap items-center gap-1.5 pt-0.5">
          <button
            @click="store.setStatusFilter('ALL')"
            class="px-2.5 py-1 rounded-lg border text-xs font-bold transition-all cursor-pointer"
            :class="store.curStatusFilter === 'ALL' ? 'bg-slate-900 text-white border-slate-900 shadow-2xs' : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'"
          >
            전체 ({{ currentStationChargers.length }})
          </button>

          <button
            v-for="(info, key) in STATUS_MAP"
            :key="key"
            @click="store.setStatusFilter(key)"
            class="px-2.5 py-1 rounded-lg border text-xs font-medium transition-all flex items-center gap-1.5 cursor-pointer"
            :class="store.curStatusFilter === key ? 'ring-2 ring-sky-500 font-bold ' + info.badgeClass : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'"
          >
            <span class="w-1.5 h-1.5 rounded-full" :class="info.dotClass"></span>
            <span>{{ key }}</span>
            <span class="font-mono font-bold text-[11px]">{{ statusCounts[key] || 0 }}</span>
          </button>
        </div>
      </section>

      <!-- 2. 중앙 하단 데이터 그리드 (단일 모던 테이블) -->
      <section class="flex-1 bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl p-3 flex flex-col overflow-hidden shadow-xs">
        
        <!-- 검색 및 건수 요약 바 -->
        <div class="flex items-center justify-between pb-2 border-b border-[var(--border-glass)] gap-2 flex-shrink-0">
          <div class="flex items-center gap-2">
            <span class="text-xs font-bold text-[var(--text-bright)]">충전기 목록</span>
            <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 border border-slate-200">
              {{ filteredChargers.length }}기
            </span>
          </div>

          <div class="flex items-center gap-2">
            <input
              v-model="searchQuery"
              placeholder="단말ID, 충전소명 검색..."
              class="w-48 bg-[var(--bg-surface-2)] border border-[var(--border-glass)] rounded-lg px-2.5 py-1 text-xs outline-hidden focus:border-sky-500 placeholder:text-slate-400"
            />
          </div>
        </div>

        <!-- 단일 고성능 데이터 테이블 -->
        <div class="flex-1 overflow-y-auto mt-2 pr-0.5">
          <table class="w-full text-left text-xs border-collapse">
            <thead class="sticky top-0 bg-[var(--bg-surface-1)] border-b border-slate-200 text-slate-500 font-semibold text-[11px] z-10">
              <tr>
                <th class="py-2 px-2.5">충전기 ID</th>
                <th class="py-2 px-2">소속 충전소</th>
                <th class="py-2 px-2">제조사 / 모델</th>
                <th class="py-2 px-2">실시간 상태</th>
                <th class="py-2 px-2">현재 출력</th>
                <th class="py-2 px-2">전압 / 전류</th>
                <th class="py-2 px-2">SoC</th>
                <th class="py-2 px-2.5 text-right">최종 수신 일시</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100 font-mono text-[11px]">
              <tr v-if="filteredChargers.length === 0">
                <td colspan="8" class="py-16 text-center text-slate-400">
                  <svg class="w-10 h-10 mx-auto text-slate-300 mb-2" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" /></svg>
                  <div class="font-bold text-xs text-[var(--text-bright)]">조회된 충전기가 없습니다</div>
                  <div class="text-[11px] text-slate-500 mt-0.5">데이터베이스가 비어 있거나 검색 조건에 맞는 충전기가 없습니다.</div>
                </td>
              </tr>
              <tr
                v-for="chg in filteredChargers"
                :key="chg.id"
                @click="onSelectCharger(chg)"
                class="hover:bg-slate-50/80 cursor-pointer transition-colors"
                :class="store.selectedCharger?.id === chg.id ? 'bg-sky-50/60 font-semibold' : ''"
              >
                <td class="py-2.5 px-2.5 font-bold text-sky-700 whitespace-nowrap">{{ chg.chargeBoxId }}</td>
                <td class="py-2.5 px-2 font-medium truncate max-w-[140px] whitespace-nowrap">{{ chg.stationName }}</td>
                <td class="py-2.5 px-2 text-slate-600 truncate max-w-[130px] whitespace-nowrap">{{ chg.vendor }} {{ chg.model }}</td>
                <td class="py-2.5 px-2 whitespace-nowrap">
                  <span :class="STATUS_MAP[chg.status]?.badgeClass" class="px-2 py-0.5 rounded text-[10px] font-bold border whitespace-nowrap inline-flex items-center gap-1">
                    <span class="w-1.5 h-1.5 rounded-full" :class="STATUS_MAP[chg.status]?.dotClass"></span>
                    <span>{{ chg.status }}</span>
                  </span>
                </td>
                <td class="py-2.5 px-2 font-bold whitespace-nowrap">{{ chg.powerKw > 0 ? `${chg.powerKw.toFixed(1)} kW` : '-' }}</td>
                <td class="py-2.5 px-2 text-slate-500 whitespace-nowrap">{{ chg.powerKw > 0 ? `${chg.voltageV.toFixed(0)}V / ${chg.currentA.toFixed(0)}A` : '-' }}</td>
                <td class="py-2.5 px-2 whitespace-nowrap">
                  <div v-if="chg.status === '충전중'" class="w-20">
                    <div class="flex justify-between text-[10px] mb-0.5 font-bold text-emerald-600">
                      <span>{{ chg.socPercent }}%</span>
                    </div>
                    <div class="h-1.5 bg-slate-200 rounded-full overflow-hidden">
                      <div class="h-full bg-emerald-500" :style="{ width: `${chg.socPercent}%` }"></div>
                    </div>
                  </div>
                  <span v-else class="text-slate-400">-</span>
                </td>
                <td class="py-2.5 px-2.5 text-right text-[10px] text-slate-500 whitespace-nowrap">{{ chg.lastHeartbeat }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>

    <!-- RIGHT: 단말 계측치 및 원격 제어반 드로어 (260px) -->
    <aside
      id="controlDrawer"
      class="fixed inset-y-0 right-0 z-50 w-80 bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl p-3 flex flex-col justify-between overflow-y-auto select-none transition-transform duration-200 ease-in-out shadow-2xl
             xl:static xl:translate-x-0 xl:w-60 2xl:w-68 xl:min-w-[200px] xl:max-w-xs xl:flex-shrink xl:shadow-none"
      :class="store.isControlDrawerOpen ? 'translate-x-0' : 'translate-x-full xl:translate-x-0'"
    >
      <div class="space-y-3">
        <!-- 헤더 영역 -->
        <div class="flex items-center justify-between pb-2 border-b border-[var(--border-glass)]">
          <div class="flex items-center gap-1.5 truncate">
            <svg class="w-4 h-4 text-sky-600 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"></path></svg>
            <h2 class="text-xs font-bold text-[var(--text-bright)] truncate">단말 계측 · 원격제어</h2>
          </div>
          <div class="flex items-center gap-1.5">
            <span v-if="store.selectedCharger" :class="STATUS_MAP[store.selectedCharger.status]?.badgeClass" class="text-[10px] px-1.5 py-0.2 rounded border font-bold">
              {{ store.selectedCharger.status }}
            </span>
            <button
              @click="store.toggleControlDrawer(false)"
              class="xl:hidden p-1 rounded-md text-slate-400 hover:text-slate-800 font-bold text-base leading-none cursor-pointer"
              title="제어반 닫기"
            >✕</button>
          </div>
        </div>

        <div v-if="store.selectedCharger" class="space-y-2.5 text-xs">
          <!-- 단말 기본 정보 -->
          <div class="p-2.5 rounded-xl bg-[var(--bg-surface-2)] border border-[var(--border-glass)] space-y-1">
            <div class="flex items-center justify-between text-xs">
              <span class="font-bold text-sky-700 font-mono">{{ store.selectedCharger.chargeBoxId }}</span>
              <span class="text-[10px] font-bold text-slate-500">{{ store.selectedCharger.spec }}</span>
            </div>
            <div class="text-[11px] font-bold text-[var(--text-bright)] truncate">{{ store.selectedCharger.stationName }}</div>
            <div class="text-[10px] text-slate-500 font-mono flex justify-between pt-0.5">
              <span>{{ store.selectedCharger.vendor }}</span>
              <span>{{ store.selectedCharger.protocol }}</span>
            </div>
          </div>

          <!-- 실시간 텔레메트리 계측치 -->
          <div class="p-2.5 rounded-xl bg-[var(--bg-surface-2)] border border-[var(--border-glass)] space-y-2">
            <div class="flex items-center justify-between text-[11px] font-bold text-slate-500">
              <span>실시간 충전 계측치</span>
              <span class="text-emerald-600 font-mono text-[10px]">1초 스트리밍</span>
            </div>

            <!-- 순시 출력 -->
            <div class="space-y-1">
              <div class="flex items-baseline justify-between">
                <span class="text-[11px] text-slate-500">순시 출력</span>
                <span class="text-sm font-black font-mono text-sky-700">{{ store.selectedCharger.powerKw.toFixed(1) }} kW</span>
              </div>
              <div class="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden">
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

            <!-- 배터리 SoC -->
            <div class="space-y-1 pt-1">
              <div class="flex items-baseline justify-between text-[11px]">
                <span class="text-slate-500">배터리 SoC</span>
                <span class="font-bold font-mono text-emerald-600">{{ store.selectedCharger.socPercent }}%</span>
              </div>
              <div class="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden">
                <div class="bg-emerald-500 h-full rounded-full" :style="{ width: `${store.selectedCharger.socPercent}%` }"></div>
              </div>
            </div>
          </div>

          <!-- OCPP 원격 제어 커맨드 -->
          <div class="space-y-1.5 pt-1">
            <div class="text-[11px] font-bold text-slate-500">OCPP 원격 제어</div>
            <div class="grid grid-cols-2 gap-1.5 font-bold text-[11px]">
              <button
                type="button"
                @click="sendRemoteCommand('RemoteReset')"
                class="p-2 rounded-lg bg-[var(--bg-surface-2)] hover:bg-slate-200 text-slate-800 border border-[var(--border-glass)] transition-all flex items-center justify-center gap-1.5 shadow-xs cursor-pointer"
              >
                <svg class="w-3.5 h-3.5 text-slate-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" /></svg>
                <span>단말 리셋</span>
              </button>
              <button
                type="button"
                @click="sendRemoteCommand('UnlockConnector')"
                class="p-2 rounded-lg bg-[var(--bg-surface-2)] hover:bg-slate-200 text-slate-800 border border-[var(--border-glass)] transition-all flex items-center justify-center gap-1.5 shadow-xs cursor-pointer"
              >
                <svg class="w-3.5 h-3.5 text-slate-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8 11V7a4 4 0 118 0m-4 8v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2z" /></svg>
                <span>잠금 해제</span>
              </button>
              <button
                type="button"
                @click="sendRemoteCommand('RemoteStartTransaction')"
                class="p-2 rounded-lg bg-sky-600 hover:bg-sky-700 text-white transition-all flex items-center justify-center gap-1.5 shadow-xs font-semibold cursor-pointer"
              >
                <svg class="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24"><path d="M8 5v14l11-7z" /></svg>
                <span>충전 시작</span>
              </button>
              <button
                type="button"
                @click="sendRemoteCommand('RemoteStopTransaction')"
                class="p-2 rounded-lg bg-rose-600 hover:bg-rose-700 text-white transition-all flex items-center justify-center gap-1.5 shadow-xs font-semibold cursor-pointer"
              >
                <svg class="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24"><path d="M6 6h12v12H6z" /></svg>
                <span>충전 종료</span>
              </button>
            </div>
          </div>
        </div>

        <!-- 미선택 상태 -->
        <div v-else class="py-16 text-center text-slate-400 space-y-2">
          <svg class="w-10 h-10 mx-auto text-slate-300" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" /></svg>
          <div class="font-bold text-xs text-[var(--text-bright)]">선택된 충전기 없음</div>
          <div class="text-[10px] text-slate-500 leading-relaxed px-3">
            목록에서 충전기를 선택하면 상세 계측치 및 원격 제어반이 활성화됩니다.
          </div>
        </div>
      </div>
    </aside>
  </div>
</template>
