<script setup lang="ts">
import { ref, computed } from 'vue'
import { useCsmsStore, type ChargerData } from '../stores/csmsStore'

const store = useCsmsStore()
const searchQuery = ref('')
const selectedStatus = ref<string>('ALL')
const viewMode = ref<'matrix' | 'table' | 'cockpit'>('matrix')

const filterTabs = [
  { label: '전체 (2,000)', value: 'ALL' },
  { label: '충전중 (842)', value: 'Charging' },
  { label: '대기중 (1,028)', value: 'Available' },
  { label: '장애/고장 (98)', value: 'Faulted' },
  { label: '준비/점검 (32)', value: 'Preparing' }
]

const filteredChargers = computed(() => {
  return store.chargers.filter((chg) => {
    const matchQuery =
      chg.chargeBoxId.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      chg.stationName.toLowerCase().includes(searchQuery.value.toLowerCase())
    const matchStatus =
      selectedStatus.value === 'ALL' || chg.status === selectedStatus.value
    return matchQuery && matchStatus
  })
})

const selectedCockpitCharger = ref<ChargerData>(store.chargers[0] || null)

// Modal State
const isModalOpen = ref(false)
const targetCharger = ref<ChargerData | null>(null)
const actionFeedback = ref<string | null>(null)

function openControlModal(charger: ChargerData) {
  targetCharger.value = charger
  actionFeedback.value = null
  isModalOpen.value = true
}

function closeControlModal() {
  isModalOpen.value = false
  targetCharger.value = null
}

function executeRemoteCommand(action: string) {
  if (!targetCharger.value) return
  actionFeedback.value = `[${action}] 커맨드가 Kafka Outbound 토픽으로 발행되었습니다.`
}

function getStatusBadgeClass(status: string) {
  switch (status) {
    case 'Charging':
      return 'badge-charging'
    case 'Available':
      return 'badge-available'
    case 'Faulted':
      return 'badge-faulted'
    case 'Preparing':
      return 'badge-preparing'
    default:
      return 'badge-available'
  }
}

function getStatusDotClass(status: string) {
  switch (status) {
    case 'Charging':
      return 'bg-violet-500'
    case 'Available':
      return 'bg-emerald-500'
    case 'Faulted':
      return 'bg-rose-500'
    case 'Preparing':
      return 'bg-amber-500'
    default:
      return 'bg-emerald-500'
  }
}
</script>

<template>
  <div class="space-y-5 pb-6">
    <!-- Top Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-3">
      <div>
        <div class="flex items-center gap-3">
          <h1 class="text-xl font-bold font-display text-slate-800 dark:text-slate-100 tracking-tight">충전기 종합 관제 & 제어 센터</h1>
          <span class="font-mono text-xs px-2 py-0.5 rounded-md font-semibold bg-sky-500/10 text-sky-600 dark:text-sky-400 border border-sky-500/30">CHARGER-CENTER</span>
        </div>
        <p class="text-xs text-slate-500 mt-0.5">2,000대 충전기 실시간 텔레메트리 모니터링, 단말 제어(Reset/Unlock) 및 시계열 분석</p>
      </div>

      <!-- View Switcher -->
      <div class="flex items-center gap-1 bg-slate-200/70 dark:bg-slate-800/70 p-1 rounded-xl border border-slate-300/40 dark:border-slate-700/50 text-xs font-bold font-mono">
        <button
          @click="viewMode = 'matrix'"
          class="px-3 py-1.5 rounded-lg flex items-center gap-1.5 transition-all"
          :class="viewMode === 'matrix' ? 'bg-sky-600 text-white shadow-sm' : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'"
        >
          <span>🗂️</span>
          <span>카드 뷰</span>
        </button>
        <button
          @click="viewMode = 'table'"
          class="px-3 py-1.5 rounded-lg flex items-center gap-1.5 transition-all"
          :class="viewMode === 'table' ? 'bg-sky-600 text-white shadow-sm' : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'"
        >
          <span>📋</span>
          <span>테이블 뷰</span>
        </button>
        <button
          @click="viewMode = 'cockpit'"
          class="px-3 py-1.5 rounded-lg flex items-center gap-1.5 transition-all"
          :class="viewMode === 'cockpit' ? 'bg-sky-600 text-white shadow-sm' : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'"
        >
          <span>🖥️</span>
          <span>콕핏 분할 뷰</span>
        </button>
      </div>
    </div>

    <!-- 1. 충전기 현황 요약 KPI 티커 -->
    <div class="grid grid-cols-2 md:grid-cols-5 gap-3">
      <div class="glass-card p-3 cursor-pointer" @click="selectedStatus = 'ALL'">
        <div class="text-[11px] text-slate-500 font-medium">총 관제 충전기</div>
        <div class="text-xl font-bold font-mono text-slate-900 dark:text-slate-100 mt-1">2,000 <span class="text-xs font-normal text-slate-500">대</span></div>
        <div class="text-[10px] text-sky-600 dark:text-sky-400 font-semibold font-mono mt-0.5">100% 정상 수용</div>
      </div>
      <div class="glass-card p-3 cursor-pointer" @click="selectedStatus = 'Charging'">
        <div class="text-[11px] text-slate-500 font-medium">⚡ 실시간 충전중</div>
        <div class="text-xl font-bold font-mono text-violet-600 dark:text-violet-400 mt-1">842 <span class="text-xs font-normal text-slate-500">대</span></div>
        <div class="text-[10px] text-violet-500 font-semibold font-mono mt-0.5">28.5 MW 부하</div>
      </div>
      <div class="glass-card p-3 cursor-pointer" @click="selectedStatus = 'Available'">
        <div class="text-[11px] text-slate-500 font-medium">🌿 대기중 (충전가능)</div>
        <div class="text-xl font-bold font-mono text-emerald-600 dark:text-emerald-400 mt-1">1,028 <span class="text-xs font-normal text-slate-500">대</span></div>
        <div class="text-[10px] text-emerald-500 font-semibold font-mono mt-0.5">51.4% 가용율</div>
      </div>
      <div class="glass-card p-3 cursor-pointer" @click="selectedStatus = 'Faulted'">
        <div class="text-[11px] text-rose-500 font-medium">🚨 고장 / 통신장애</div>
        <div class="text-xl font-bold font-mono text-rose-600 dark:text-rose-400 mt-1">98 <span class="text-xs font-normal text-slate-500">대</span></div>
        <div class="text-[10px] text-rose-500 font-semibold font-mono mt-0.5">즉각 조치 필요</div>
      </div>
      <div class="glass-card p-3 cursor-pointer" @click="selectedStatus = 'Preparing'">
        <div class="text-[11px] text-amber-500 font-medium">🔧 준비 / 점검중</div>
        <div class="text-xl font-bold font-mono text-amber-600 dark:text-amber-400 mt-1">32 <span class="text-xs font-normal text-slate-500">대</span></div>
        <div class="text-[10px] text-amber-500 font-semibold font-mono mt-0.5">커넥터 체결 대기</div>
      </div>
    </div>

    <!-- 2. 필터 및 검색 바 -->
    <div class="glass-card p-4 flex flex-wrap items-center justify-between gap-4">
      <div class="flex items-center gap-3 flex-1 min-w-[280px]">
        <div class="relative flex-1">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="충전기 ID 또는 충전소명 검색..."
            class="input-theme w-full pl-9 pr-3 text-xs"
          />
          <span class="absolute left-3 top-2.5 text-slate-400 text-sm">🔍</span>
        </div>
      </div>

      <!-- 상태 필터 탭 -->
      <div class="flex items-center gap-1 bg-slate-200/60 dark:bg-slate-800/60 p-1 rounded-lg border border-slate-300/40 dark:border-slate-700/50 text-xs font-semibold">
        <button
          v-for="tab in filterTabs"
          :key="tab.value"
          @click="selectedStatus = tab.value"
          class="px-3 py-1.5 rounded-md transition-all duration-150"
          :class="selectedStatus === tab.value ? 'bg-sky-500 text-white shadow-sm' : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'"
        >
          {{ tab.label }}
        </button>
      </div>
    </div>

    <!-- 3. 메인 관제 뷰 (3개 모드) -->

    <!-- MODE 1: 카드 매트릭스 뷰 -->
    <div v-if="viewMode === 'matrix'" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      <div
        v-for="chg in filteredChargers"
        :key="chg.id"
        class="glass-card p-4 flex flex-col justify-between transition-all"
      >
        <div>
          <div class="flex items-center justify-between">
            <span class="font-mono font-bold text-sky-600 dark:text-sky-400 text-sm">{{ chg.chargeBoxId }}</span>
            <span :class="getStatusBadgeClass(chg.status)" class="badge">
              <span class="w-1.5 h-1.5 rounded-full" :class="getStatusDotClass(chg.status)"></span>
              {{ chg.status }}
            </span>
          </div>
          <div class="text-xs font-semibold text-slate-800 dark:text-slate-200 mt-1.5 truncate">{{ chg.stationName }}</div>
          <div class="text-[11px] text-slate-500 font-mono mt-0.5">{{ chg.vendor }} | {{ chg.model }}</div>

          <div class="mt-3 p-2.5 rounded-xl bg-slate-100/70 dark:bg-slate-900/60 space-y-1.5 text-xs font-mono">
            <div class="flex justify-between">
              <span class="text-slate-500">실시간 출력:</span>
              <span class="font-bold" :class="chg.powerKw > 0 ? 'text-violet-600 dark:text-violet-400' : 'text-slate-400'">
                {{ chg.powerKw.toFixed(1)} } kW
              </span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">전압 / 전류:</span>
              <span class="text-slate-600 dark:text-slate-400">{{ chg.powerKw > 0 ? `${chg.voltageV.toFixed(0)}V / ${chg.currentA.toFixed(0)}A` : '-' }}</span>
            </div>
            <div v-if="chg.status === 'Charging'" class="pt-1">
              <div class="flex justify-between text-[11px] mb-1">
                <span class="text-slate-500">배터리 SoC:</span>
                <span class="font-bold text-emerald-600 dark:text-emerald-400">{{ chg.socPercent }}%</span>
              </div>
              <div class="w-full bg-slate-200 dark:bg-slate-700 h-1.5 rounded-full overflow-hidden">
                <div class="bg-gradient-to-r from-violet-500 to-emerald-400 h-full rounded-full" :style="{ width: `${chg.socPercent}%` }"></div>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-4 pt-3 border-t border-[var(--border-glass)] flex items-center justify-between">
          <span class="text-[11px] font-mono text-slate-500">{{ chg.lastHeartbeat }}</span>
          <button @click="openControlModal(chg)" class="btn-theme-action flex items-center gap-1">
            <span>⚡ 원격 제어</span>
          </button>
        </div>
      </div>
    </div>

    <!-- MODE 2: 데이터 테이블 뷰 -->
    <div v-else-if="viewMode === 'table'" class="glass-card p-5">
      <div class="overflow-x-auto">
        <table class="table-theme">
          <thead>
            <tr>
              <th>충전기 ID</th>
              <th>충전소명 / 위치</th>
              <th>제조사 / 모델</th>
              <th>상태</th>
              <th>현재 출력 (kW)</th>
              <th>전압 / 전류</th>
              <th>배터리 SoC</th>
              <th>최근 통신</th>
              <th>원격 제어</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="chg in filteredChargers" :key="chg.id">
              <td class="font-mono font-bold text-sky-600 dark:text-sky-400">{{ chg.chargeBoxId }}</td>
              <td class="font-medium text-slate-800 dark:text-slate-200">{{ chg.stationName }}</td>
              <td class="text-xs text-slate-500">{{ chg.vendor }} / {{ chg.model }}</td>
              <td>
                <span :class="getStatusBadgeClass(chg.status)" class="badge">
                  <span class="w-1.5 h-1.5 rounded-full" :class="getStatusDotClass(chg.status)"></span>
                  {{ chg.status }}
                </span>
              </td>
              <td class="font-mono font-bold" :class="chg.powerKw > 0 ? 'text-violet-600 dark:text-violet-400' : 'text-slate-400'">
                {{ chg.powerKw.toFixed(1) }} kW
              </td>
              <td class="font-mono text-xs text-slate-500">
                <span v-if="chg.powerKw > 0">{{ chg.voltageV.toFixed(0) }}V / {{ chg.currentA.toFixed(0) }}A</span>
                <span v-else>-</span>
              </td>
              <td>
                <div v-if="chg.status === 'Charging'" class="flex items-center gap-2">
                  <div class="w-16 bg-slate-200 dark:bg-slate-700 h-1.5 rounded-full overflow-hidden">
                    <div class="bg-violet-500 h-full rounded-full" :style="{ width: `${chg.socPercent}%` }"></div>
                  </div>
                  <span class="font-mono text-xs font-semibold text-violet-600 dark:text-violet-400">{{ chg.socPercent }}%</span>
                </div>
                <span v-else class="text-xs text-slate-400 font-mono">-</span>
              </td>
              <td class="text-xs font-mono text-slate-500">{{ chg.lastHeartbeat }}</td>
              <td>
                <button
                  @click="openControlModal(chg)"
                  class="btn-theme-action flex items-center gap-1.5"
                >
                  <span>⚡ 제어</span>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- MODE 3: 콕핏 분할 뷰 -->
    <div v-else-if="viewMode === 'cockpit'" class="grid grid-cols-1 lg:grid-cols-12 gap-4">
      <div class="lg:col-span-4 glass-card p-4 flex flex-col h-[520px]">
        <div class="flex items-center justify-between pb-2 border-b border-[var(--border-glass)]">
          <h3 class="text-xs font-bold font-display text-slate-800 dark:text-slate-200">단말 선택</h3>
          <span class="text-[10px] font-mono text-slate-500">{{ filteredChargers.length }}대</span>
        </div>
        <div class="space-y-2 overflow-y-auto flex-1 mt-2 pr-1 font-mono text-xs">
          <div
            v-for="chg in filteredChargers"
            :key="chg.id"
            @click="selectedCockpitCharger = chg"
            class="p-2.5 rounded-xl border cursor-pointer transition-all"
            :class="selectedCockpitCharger?.chargeBoxId === chg.chargeBoxId ? 'bg-sky-500/15 border-sky-500/40 shadow-sm' : 'bg-slate-100/50 dark:bg-slate-900/50 border-[var(--border-glass)] hover:border-sky-500/30'"
          >
            <div class="flex items-center justify-between">
              <span class="font-bold" :class="selectedCockpitCharger?.chargeBoxId === chg.chargeBoxId ? 'text-sky-600' : 'text-slate-800 dark:text-slate-200'">{{ chg.chargeBoxId }}</span>
              <span :class="getStatusBadgeClass(chg.status)" class="badge">{{ chg.status }}</span>
            </div>
            <div class="text-[11px] text-slate-500 mt-1 truncate">{{ chg.stationName }}</div>
            <div class="flex items-center justify-between mt-1 text-[11px]">
              <span class="font-bold" :class="chg.powerKw > 0 ? 'text-violet-600' : 'text-slate-400'">{{ chg.powerKw.toFixed(1) }} kW</span>
              <span class="font-semibold text-emerald-600">{{ chg.socPercent > 0 ? `SoC ${chg.socPercent}%` : '' }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="lg:col-span-8 flex flex-col gap-4">
        <div v-if="selectedCockpitCharger" class="glass-card p-4 flex flex-col md:flex-row items-center justify-between gap-3">
          <div>
            <div class="flex items-center gap-2">
              <span class="font-mono font-bold text-base text-sky-600">{{ selectedCockpitCharger.chargeBoxId }}</span>
              <span :class="getStatusBadgeClass(selectedCockpitCharger.status)" class="badge">{{ selectedCockpitCharger.status }}</span>
            </div>
            <p class="text-xs text-slate-500 font-mono mt-0.5">{{ selectedCockpitCharger.stationName }} | {{ selectedCockpitCharger.vendor }} {{ selectedCockpitCharger.model }}</p>
          </div>

          <div class="flex items-center gap-1.5 text-xs font-bold">
            <button @click="openControlModal(selectedCockpitCharger)" class="px-3 py-1.5 rounded-lg bg-sky-600 text-white shadow-sm flex items-center gap-1">
              <span>⚡ 원격 제어반 열기</span>
            </button>
          </div>
        </div>

        <div class="glass-card p-4 flex-1 flex flex-col justify-center items-center text-center p-8 text-slate-500 text-xs">
          <div class="text-2xl mb-2">📈</div>
          <div class="font-bold text-slate-700 dark:text-slate-300">ClickHouse OLAP 1초 시계열 스트림 대기중</div>
          <p class="mt-1">선택된 단말의 초 단위 전력/전압/전류/SoC 텔레메트리가 실시간으로 갱신됩니다.</p>
        </div>
      </div>
    </div>

    <!-- 4. 원격 제어 모달 (POPUP-CHG-CONTROL-01) -->
    <div
      v-if="isModalOpen && targetCharger"
      class="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4"
    >
      <div class="glass-card max-w-lg w-full p-6 space-y-4 border border-sky-500/40 shadow-2xl">
        <div class="flex items-center justify-between border-b border-[var(--border-glass)] pb-3">
          <div class="flex items-center gap-2">
            <span class="text-xl">⚡</span>
            <div>
              <h3 class="text-base font-bold font-display text-slate-800 dark:text-slate-100">충전기 원격 제어</h3>
              <p class="text-xs text-slate-500 font-mono">{{ targetCharger.chargeBoxId }} ({{ targetCharger.stationName }})</p>
            </div>
          </div>
          <button @click="closeControlModal" class="text-slate-400 hover:text-slate-600 text-lg">&times;</button>
        </div>

        <!-- 단말 상태 요약 뱃지 -->
        <div class="grid grid-cols-3 gap-2 p-3 rounded-xl bg-slate-100/60 dark:bg-slate-900/60 text-xs font-mono">
          <div>상태: <span class="font-bold text-violet-500">{{ targetCharger.status }}</span></div>
          <div>출력: <span class="font-bold text-sky-500">{{ targetCharger.powerKw.toFixed(1) }} kW</span></div>
          <div>SoC: <span class="font-bold text-emerald-500">{{ targetCharger.socPercent }}%</span></div>
        </div>

        <!-- 커맨드 액션 버튼 그리드 -->
        <div class="space-y-3 pt-2">
          <label class="text-xs font-bold text-slate-700 dark:text-slate-300">OCPP 1.6J / 2.0.1 제어 커맨드 전송:</label>
          <div class="grid grid-cols-2 gap-2 text-xs">
            <button
              @click="executeRemoteCommand('Reset (Hard)')"
              class="p-3 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/30 text-rose-600 dark:text-rose-400 font-bold flex flex-col items-center gap-1 transition-all"
            >
              <span class="text-base">🔄</span>
              <span>하드 리셋 (Hard)</span>
            </button>

            <button
              @click="executeRemoteCommand('Reset (Soft)')"
              class="p-3 rounded-xl bg-amber-500/10 hover:bg-amber-500/20 border border-amber-500/30 text-amber-600 dark:text-amber-400 font-bold flex flex-col items-center gap-1 transition-all"
            >
              <span class="text-base">♻️</span>
              <span>소프트 리셋 (Soft)</span>
            </button>

            <button
              @click="executeRemoteCommand('UnlockConnector')"
              class="p-3 rounded-xl bg-sky-500/10 hover:bg-sky-500/20 border border-sky-500/30 text-sky-600 dark:text-sky-400 font-bold flex flex-col items-center gap-1 transition-all"
            >
              <span class="text-base">🔓</span>
              <span>커넥터 잠금 해제</span>
            </button>

            <button
              @click="executeRemoteCommand('RemoteStopTransaction')"
              class="p-3 rounded-xl bg-violet-500/10 hover:bg-violet-500/20 border border-violet-500/30 text-violet-600 dark:text-violet-400 font-bold flex flex-col items-center gap-1 transition-all"
            >
              <span class="text-base">⏹️</span>
              <span>원격 충전 중지</span>
            </button>
          </div>
        </div>

        <!-- 피드백 메시지 -->
        <div v-if="actionFeedback" class="p-3 rounded-xl bg-emerald-500/15 border border-emerald-500/40 text-emerald-600 dark:text-emerald-400 text-xs font-mono font-semibold flex items-center gap-2">
          <span>✅</span>
          <span>{{ actionFeedback }}</span>
        </div>

        <div class="flex justify-end pt-2">
          <button
            @click="closeControlModal"
            class="px-4 py-2 rounded-lg bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300 text-xs font-semibold hover:bg-slate-300 transition-all"
          >
            닫기
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
