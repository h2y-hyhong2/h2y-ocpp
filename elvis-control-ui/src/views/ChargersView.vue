<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  useCsmsStore,
  STATUS_MAP,
  PRIMARY_STATUS_KEYS,
  getStatusMeta,
  isChargerConnected,
  getChargerConnectors,
  getPrimaryConnector,
  getChargerTotalPowerKw,
  type ChargerItem,
  type ConnectorDetail
} from '../stores/csmsStore'

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

// 현재 선택된 충전소에 소속된 충전기 목록 (법인 필터 포함)
const currentStationChargers = computed(() => {
  return store.chargers.filter(c => {
    const matchCorp = store.curCorpFilter === 'ALL' || c.corpId === store.curCorpFilter
    const matchSt = store.curStFilter === 'ALL' || c.stId === store.curStFilter
    return matchCorp && matchSt
  })
})

// 현재 범위의 전체 커넥터 수 (커넥터 1~N 기준)
const totalConnectorCount = computed(() => {
  return currentStationChargers.value.reduce((acc, chg) => acc + getChargerConnectors(chg).length, 0)
})

function matchesStatusFilter(status: string | undefined | null, filter: string): boolean {
  if (filter === 'ALL') return true
  return getStatusMeta(status).key === getStatusMeta(filter).key || status === filter
}

function matchesChargerKeyword(chg: ChargerItem, q: string): boolean {
  if (!q) return true
  const conns = getChargerConnectors(chg)
  const matchConnectorMeta = conns.some(
    c =>
      (c.carModel || '').toLowerCase().includes(q) ||
      (c.userTag || '').toLowerCase().includes(q)
  )
  return (
    (chg.chargeBoxId || '').toLowerCase().includes(q) ||
    (chg.stationName || '').toLowerCase().includes(q) ||
    (chg.corpName || '').toLowerCase().includes(q) ||
    (chg.vendor || '').toLowerCase().includes(q) ||
    (chg.model || '').toLowerCase().includes(q) ||
    (chg.carModel || '').toLowerCase().includes(q) ||
    (chg.userTag || '').toLowerCase().includes(q) ||
    matchConnectorMeta
  )
}

// 필터링 및 검색된 충전기 목록 (커넥터 중 하나라도 선택된 상태와 일치하면 노출)
const filteredChargers = computed(() => {
  const localQ = (searchQuery.value || '').toLowerCase().trim()
  const globalQ = (store.searchQuery || '').toLowerCase().trim()

  return currentStationChargers.value.filter((chg) => {
    const conns = getChargerConnectors(chg)
    const matchStatus =
      store.curStatusFilter === 'ALL' ||
      conns.some(conn => matchesStatusFilter(conn.status, store.curStatusFilter))

    if (!matchStatus) return false
    return matchesChargerKeyword(chg, localQ) && matchesChargerKeyword(chg, globalQ)
  })
})

// 상태별 커넥터 카운트 계산 (한글/OCPP 영문 상태 통합 집계)
const statusCounts = computed(() => {
  const counts: Record<string, number> = {}
  PRIMARY_STATUS_KEYS.forEach(k => { counts[k] = 0 })

  const keyToPrimary: Record<string, string> = {}
  PRIMARY_STATUS_KEYS.forEach(k => {
    const meta = STATUS_MAP[k]
    if (meta) {
      keyToPrimary[meta.key] = k
    }
  })

  currentStationChargers.value.forEach(chg => {
    getChargerConnectors(chg).forEach(conn => {
      if (counts[conn.status] !== undefined) {
        counts[conn.status]++
        return
      }
      const primary = keyToPrimary[getStatusMeta(conn.status).key]
      if (primary && counts[primary] !== undefined) {
        counts[primary]++
      }
    })
  })
  return counts
})

// 행 요약 계측치 표시용 대표 커넥터 (상태 필터가 있으면 해당 상태 커넥터 우선)
function getRowDisplayConnector(chg: ChargerItem): ConnectorDetail | null {
  const conns = getChargerConnectors(chg)
  if (store.curStatusFilter !== 'ALL') {
    const filteredMatch = conns.find(c => matchesStatusFilter(c.status, store.curStatusFilter))
    if (filteredMatch) return filteredMatch
  }
  return getPrimaryConnector(chg)
}

const selectedChargerConnectors = computed(() => getChargerConnectors(store.selectedCharger))

const selectedConnector = computed<ConnectorDetail | null>(() => {
  const conns = selectedChargerConnectors.value
  if (conns.length === 0) return null
  return conns.find(c => c.connectorId === store.selectedConnectorId) ?? getPrimaryConnector(store.selectedCharger) ?? conns[0]
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

// 충전기/커넥터 선택 및 제어반 열기
function onSelectCharger(chg: ChargerItem, connectorId?: number) {
  store.setSelectedCharger(chg, connectorId)
  if (window.innerWidth < 1280) store.toggleControlDrawer(true)
}

// 원격 제어 커맨드 전송
function sendRemoteCommand(action: string) {
  const targetConnId = selectedConnector.value?.connectorId
  store.sendRemoteAction(action, targetConnId)
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
              type="button"
              @click="store.toggleSidebar(false)"
              class="lg:hidden text-slate-400 hover:text-slate-800 p-1 cursor-pointer"
              title="사이드바 닫기"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
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

    <!-- CENTER: 메인 충전기 데이터 테이블 영역 (화면 전체 뷰포트 극대화) -->
    <main class="flex-1 flex flex-col overflow-hidden min-w-0">
      
      <!-- 중앙 풀 뷰포트 데이터 그리드 (단일 모던 테이블) -->
      <section class="flex-1 bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-xl p-3 flex flex-col overflow-hidden shadow-xs">
        
        <!-- 1. 충전소 정보 및 검색 헤더 바 -->
        <div class="flex flex-wrap items-center justify-between pb-2 border-b border-[var(--border-glass)] gap-2 flex-shrink-0">
          <!-- 좌측: 현재 충전소 컨텍스트 & 건수 -->
          <div class="flex items-center gap-2">
            <span class="text-xs font-bold text-[var(--text-bright)]">충전기 · 커넥터 관제 목록</span>
            <div class="flex items-center gap-1.5 bg-slate-100 px-2 py-0.5 rounded-md border border-slate-200">
              <span class="text-[10px] font-bold text-slate-700 truncate max-w-[200px]">
                {{ currentStationInfo.name }}
              </span>
              <span class="text-[10px] font-mono font-bold text-sky-600">
                ({{ filteredChargers.length }} / {{ currentStationChargers.length }}기 · {{ totalConnectorCount }}커넥터)
              </span>
            </div>
          </div>

          <!-- 우측: 검색창 -->
          <div class="flex items-center gap-1.5">
            <input
              v-model="searchQuery"
              placeholder="단말ID, 충전소, 모델 검색..."
              class="w-48 lg:w-56 bg-[var(--bg-surface-2)] border border-[var(--border-glass)] rounded-lg px-2.5 py-1 text-xs outline-hidden focus:border-sky-500 placeholder:text-slate-400"
            />
            <button
              v-if="searchQuery"
              type="button"
              @click="searchQuery = ''"
              class="text-slate-400 hover:text-slate-700 p-1 cursor-pointer"
              title="검색어 초기화"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        <!-- 2. 실시간 상태별 1-Click 필터 칩 바 (커넥터 기준 집계) -->
        <div class="flex flex-wrap items-center gap-1.5 py-2 border-b border-[var(--border-glass)] flex-shrink-0">
          <!-- 전체 보기 칩 -->
          <button
            type="button"
            @click="store.setStatusFilter('ALL')"
            class="px-2.5 py-1 rounded-lg border text-xs font-bold transition-all cursor-pointer inline-flex items-center gap-1.5 shadow-2xs"
            :class="store.curStatusFilter === 'ALL' 
              ? 'bg-slate-900 text-white border-slate-900' 
              : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'"
          >
            <span>전체</span>
            <span class="font-mono text-[11px] opacity-90">{{ totalConnectorCount }}</span>
          </button>

          <!-- 세부 상태 칩 버튼 목록 -->
          <button
            v-for="key in PRIMARY_STATUS_KEYS"
            :key="key"
            type="button"
            @click="store.setStatusFilter(key)"
            class="px-2.5 py-1 rounded-lg border text-xs transition-all flex items-center gap-1.5 cursor-pointer shadow-2xs"
            :class="[
              store.curStatusFilter === key 
                ? 'ring-2 ring-sky-500 font-bold ' + getStatusMeta(key).badgeClass 
                : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50',
              (statusCounts[key] || 0) === 0 ? 'opacity-65' : 'font-semibold'
            ]"
          >
            <span class="w-1.5 h-1.5 rounded-full" :class="getStatusMeta(key).dotClass"></span>
            <span>{{ key }}</span>
            <span 
              class="font-mono text-[11px] px-1 py-0.2 rounded"
              :class="(statusCounts[key] || 0) > 0 ? 'bg-slate-100 font-bold text-slate-800' : 'text-slate-400'"
            >
              {{ statusCounts[key] || 0 }}
            </span>
          </button>
        </div>

        <!-- 단일 고성능 데이터 테이블 -->
        <div class="flex-1 overflow-y-auto mt-2 pr-0.5">
          <table class="w-full text-left text-xs border-collapse">
            <thead class="sticky top-0 bg-[var(--bg-surface-1)] border-b border-slate-200 text-slate-500 font-semibold text-[11px] z-10">
              <tr>
                <th class="py-2 px-2.5">충전기 ID</th>
                <th class="py-2 px-2">소속 충전소</th>
                <th class="py-2 px-2">제조사 / 모델</th>
                <th class="py-2 px-2">커넥터별 실시간 상태</th>
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
                :key="chg.chargeBoxId"
                @click="onSelectCharger(chg)"
                class="hover:bg-slate-50/80 cursor-pointer transition-colors"
                :class="store.selectedCharger?.chargeBoxId === chg.chargeBoxId ? 'bg-sky-50/60 font-semibold' : ''"
              >
                <td class="py-2.5 px-2.5 whitespace-nowrap">
                  <div class="flex items-center gap-1.5">
                    <span
                      class="w-1.5 h-1.5 rounded-full flex-shrink-0"
                      :class="isChargerConnected(chg) ? 'bg-emerald-500' : 'bg-slate-300'"
                      :title="isChargerConnected(chg) ? 'WebSocket 연결됨' : 'WebSocket 연결안됨'"
                    ></span>
                    <span class="font-bold text-sky-700">{{ chg.chargeBoxId }}</span>
                  </div>
                  <div class="text-[9px] text-slate-400 font-sans">{{ getChargerConnectors(chg).length }}채널 · {{ chg.spec }}</div>
                </td>
                <td class="py-2.5 px-2 font-medium truncate max-w-[140px] whitespace-nowrap">{{ chg.stationName }}</td>
                <td class="py-2.5 px-2 text-slate-600 truncate max-w-[130px] whitespace-nowrap">{{ chg.vendor }} {{ chg.model }}</td>
                <td class="py-2.5 px-2">
                  <div class="flex flex-wrap items-center gap-1">
                    <button
                      v-for="conn in getChargerConnectors(chg)"
                      :key="`${chg.chargeBoxId}-${conn.connectorId}`"
                      type="button"
                      @click.stop="onSelectCharger(chg, conn.connectorId)"
                      :class="[
                        getStatusMeta(conn.status).badgeClass,
                        store.selectedCharger?.chargeBoxId === chg.chargeBoxId && store.selectedConnectorId === conn.connectorId
                          ? 'ring-2 ring-sky-500'
                          : ''
                      ]"
                      class="px-1.5 py-0.5 rounded text-[10px] font-bold border whitespace-nowrap inline-flex items-center gap-1 cursor-pointer transition-all hover:opacity-90"
                      :title="`#${conn.connectorId} 커넥터 선택 (${getStatusMeta(conn.status).name})`"
                    >
                      <span class="opacity-70">#{{ conn.connectorId }}</span>
                      <span class="w-1.5 h-1.5 rounded-full" :class="getStatusMeta(conn.status).dotClass"></span>
                      <span>{{ getStatusMeta(conn.status).name }}</span>
                    </button>
                  </div>
                </td>
                <td class="py-2.5 px-2 font-bold whitespace-nowrap">
                  {{ getChargerTotalPowerKw(chg) > 0 ? `${getChargerTotalPowerKw(chg).toFixed(1)} kW` : '-' }}
                </td>
                <td class="py-2.5 px-2 text-slate-500 whitespace-nowrap">
                  <template v-if="getRowDisplayConnector(chg) && (getRowDisplayConnector(chg)?.powerKw ?? 0) > 0">
                    <span v-if="getChargerConnectors(chg).length > 1" class="text-[9px] text-sky-600 mr-1">#{{ getRowDisplayConnector(chg)?.connectorId }}</span>
                    <span>{{ (getRowDisplayConnector(chg)?.voltageV ?? 0).toFixed(0) }}V / {{ (getRowDisplayConnector(chg)?.currentA ?? 0).toFixed(0) }}A</span>
                  </template>
                  <span v-else>-</span>
                </td>
                <td class="py-2.5 px-2 whitespace-nowrap">
                  <div v-if="getRowDisplayConnector(chg) && getStatusMeta(getRowDisplayConnector(chg)?.status).key === 'charging'" class="w-20">
                    <div class="flex justify-between text-[10px] mb-0.5 font-bold text-emerald-600">
                      <span v-if="getChargerConnectors(chg).length > 1" class="text-sky-600">#{{ getRowDisplayConnector(chg)?.connectorId }}</span>
                      <span>{{ getRowDisplayConnector(chg)?.socPercent ?? 0 }}%</span>
                    </div>
                    <div class="h-1.5 bg-slate-200 rounded-full overflow-hidden">
                      <div class="h-full bg-emerald-500" :style="{ width: `${getRowDisplayConnector(chg)?.socPercent ?? 0}%` }"></div>
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
            <span
              v-if="store.selectedCharger"
              :class="getStatusMeta(selectedConnector?.status ?? store.selectedCharger.status).badgeClass"
              class="text-[10px] px-1.5 py-0.2 rounded border font-bold"
            >
              #{{ selectedConnector?.connectorId ?? 1 }} {{ getStatusMeta(selectedConnector?.status ?? store.selectedCharger.status).name }}
            </span>
            <button
              type="button"
              @click="store.toggleControlDrawer(false)"
              class="xl:hidden p-1 rounded-md text-slate-400 hover:text-slate-800 cursor-pointer"
              title="제어반 닫기"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
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

          <!-- 커넥터 채널 선택 -->
          <div class="p-2 rounded-xl bg-[var(--bg-surface-2)] border border-[var(--border-glass)] space-y-1.5">
            <div class="flex items-center justify-between text-[10px] font-bold text-slate-500">
              <span>커넥터 채널 선택</span>
              <span class="font-mono text-sky-600">{{ selectedChargerConnectors.length }}채널</span>
            </div>
            <div class="grid grid-cols-2 gap-1.5">
              <button
                v-for="conn in selectedChargerConnectors"
                :key="conn.connectorId"
                type="button"
                @click="store.setSelectedConnectorId(conn.connectorId)"
                class="p-1.5 rounded-lg border text-left transition-all cursor-pointer"
                :class="selectedConnector?.connectorId === conn.connectorId
                  ? 'bg-sky-50 border-sky-500 ring-1 ring-sky-500 shadow-2xs'
                  : 'bg-white border-slate-200 hover:bg-slate-50'"
              >
                <div class="flex items-center justify-between gap-1">
                  <span class="font-mono font-bold text-[10px] text-slate-700">#{{ conn.connectorId }}</span>
                  <span :class="getStatusMeta(conn.status).badgeClass" class="px-1 py-0.2 rounded text-[9px] font-bold border inline-flex items-center gap-1">
                    <span class="w-1 h-1 rounded-full" :class="getStatusMeta(conn.status).dotClass"></span>
                    <span>{{ getStatusMeta(conn.status).name }}</span>
                  </span>
                </div>
                <div class="text-[10px] font-mono text-slate-500 mt-0.5 truncate">
                  {{ (conn.powerKw ?? 0) > 0 ? `${(conn.powerKw ?? 0).toFixed(1)} kW` : (getStatusMeta(conn.status).key === 'disconnected' ? '미연결' : '대기 0.0 kW') }}
                </div>
              </button>
            </div>
          </div>

          <!-- 실시간 텔레메트리 계측치 (선택 커넥터 기준) -->
          <div class="p-2.5 rounded-xl bg-[var(--bg-surface-2)] border border-[var(--border-glass)] space-y-2">
            <div class="flex items-center justify-between text-[11px] font-bold text-slate-500">
              <span>#{{ selectedConnector?.connectorId ?? 1 }} 커넥터 계측치</span>
              <span
                class="font-mono text-[10px]"
                :class="isChargerConnected(store.selectedCharger) ? 'text-emerald-600' : 'text-slate-400'"
              >
                {{ isChargerConnected(store.selectedCharger) ? '실시간' : '미연결' }}
              </span>
            </div>

            <!-- 순시 출력 -->
            <div class="space-y-1">
              <div class="flex items-baseline justify-between">
                <span class="text-[11px] text-slate-500">순시 출력</span>
                <span class="text-sm font-black font-mono text-sky-700">{{ (selectedConnector?.powerKw ?? 0).toFixed(1) }} kW</span>
              </div>
              <div class="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden">
                <div class="bg-sky-500 h-full rounded-full" :style="{ width: `${Math.min(100, ((selectedConnector?.powerKw ?? 0) / 350) * 100)}%` }"></div>
              </div>
            </div>

            <div class="grid grid-cols-2 gap-1.5 pt-1 font-mono text-[11px]">
              <div class="p-1.5 rounded bg-[var(--bg-surface-1)] border border-[var(--border-glass)]">
                <div class="text-[9px] text-slate-500">충전 전압</div>
                <div class="font-bold">{{ (selectedConnector?.voltageV ?? 0).toFixed(0) }} V</div>
              </div>
              <div class="p-1.5 rounded bg-[var(--bg-surface-1)] border border-[var(--border-glass)]">
                <div class="text-[9px] text-slate-500">충전 전류</div>
                <div class="font-bold">{{ (selectedConnector?.currentA ?? 0).toFixed(0) }} A</div>
              </div>
              <div class="p-1.5 rounded bg-[var(--bg-surface-1)] border border-[var(--border-glass)]">
                <div class="text-[9px] text-slate-500">누적 전력량</div>
                <div class="font-bold">{{ (selectedConnector?.accumulatedKwh ?? 0).toFixed(1) }} kWh</div>
              </div>
              <div class="p-1.5 rounded bg-[var(--bg-surface-1)] border border-[var(--border-glass)]">
                <div class="text-[9px] text-slate-500">배터리 온도</div>
                <div class="font-bold text-amber-600">{{ selectedConnector?.batteryTempC ?? 25 }} °C</div>
              </div>
            </div>

            <!-- 배터리 SoC -->
            <div class="space-y-1 pt-1">
              <div class="flex items-baseline justify-between text-[11px]">
                <span class="text-slate-500">배터리 SoC</span>
                <span class="font-bold font-mono text-emerald-600">{{ selectedConnector?.socPercent ?? 0 }}%</span>
              </div>
              <div class="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden">
                <div class="bg-emerald-500 h-full rounded-full" :style="{ width: `${selectedConnector?.socPercent ?? 0}%` }"></div>
              </div>
            </div>

            <div v-if="selectedConnector?.carModel || selectedConnector?.userTag" class="pt-1 border-t border-slate-200/70 flex items-center justify-between text-[10px] font-mono text-slate-500">
              <span class="truncate">{{ selectedConnector?.carModel || '-' }}</span>
              <span class="truncate">{{ selectedConnector?.userTag || '-' }}</span>
            </div>
          </div>

          <!-- OCPP 원격 제어 커맨드 -->
          <div class="space-y-1.5 pt-1">
            <div class="flex items-center justify-between text-[11px] font-bold text-slate-500">
              <span>OCPP 원격 제어</span>
              <span class="font-mono text-[10px] text-sky-600">#{{ selectedConnector?.connectorId ?? 1 }} 커넥터 대상</span>
            </div>
            <div class="grid grid-cols-2 gap-1.5 font-bold text-[11px]">
              <button
                type="button"
                @click="sendRemoteCommand('Reset')"
                class="p-2 rounded-lg bg-[var(--bg-surface-2)] hover:bg-slate-200 text-slate-800 border border-[var(--border-glass)] transition-all flex items-center justify-center gap-1.5 shadow-xs cursor-pointer"
                title="충전기 본체 전체 리셋"
              >
                <svg class="w-3.5 h-3.5 text-slate-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" /></svg>
                <span>단말 리셋</span>
              </button>
              <button
                type="button"
                @click="sendRemoteCommand('UnlockConnector')"
                class="p-2 rounded-lg bg-[var(--bg-surface-2)] hover:bg-slate-200 text-slate-800 border border-[var(--border-glass)] transition-all flex items-center justify-center gap-1.5 shadow-xs cursor-pointer"
                :title="`#${selectedConnector?.connectorId ?? 1} 커넥터 잠금 해제`"
              >
                <svg class="w-3.5 h-3.5 text-slate-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8 11V7a4 4 0 118 0m-4 8v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2z" /></svg>
                <span>잠금 해제</span>
              </button>
              <button
                type="button"
                @click="sendRemoteCommand('RemoteStartTransaction')"
                class="p-2 rounded-lg bg-sky-600 hover:bg-sky-700 text-white transition-all flex items-center justify-center gap-1.5 shadow-xs font-semibold cursor-pointer"
                :title="`#${selectedConnector?.connectorId ?? 1} 커넥터 원격 충전 시작`"
              >
                <svg class="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24"><path d="M8 5v14l11-7z" /></svg>
                <span>충전 시작</span>
              </button>
              <button
                type="button"
                @click="sendRemoteCommand('RemoteStopTransaction')"
                class="p-2 rounded-lg bg-rose-600 hover:bg-rose-700 text-white transition-all flex items-center justify-center gap-1.5 shadow-xs font-semibold cursor-pointer"
                :title="`#${selectedConnector?.connectorId ?? 1} 커넥터 원격 충전 종료`"
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
