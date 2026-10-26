<template>
  <div class="h-screen w-screen flex flex-col overflow-hidden font-sans select-none bg-[var(--bg-base)] text-[var(--text-bright)]" :data-theme="store.currentTheme">
    
    <!-- LS E-LINK 마스터 브랜드 시그니처 악센트 라인 -->
    <div class="h-[2.5px] w-full bg-gradient-to-r from-[#0A1E5A] via-[#FA002D] to-[#E96600] flex-shrink-0 z-40"></div>

    <!-- GNB 컴팩트 상단 헤더 (48px) -->
    <header class="window-titlebar flex items-center justify-between px-3 md:px-4 h-12 min-h-[48px] bg-white/95 backdrop-blur-md border-b border-slate-200/90 z-30 flex-shrink-0 shadow-2xs gap-3">
      
      <!-- Left: 로고 및 타이틀 & 관제 메뉴 네비게이션 -->
      <div class="flex items-center gap-3 flex-shrink-0">
        <!-- 모바일/태블릿용 사이드바 토글 버튼 (1024px 미만에서 노출) -->
        <button
          @click="store.toggleSidebar()"
          class="lg:hidden p-1.5 rounded-lg text-slate-600 hover:bg-slate-100 focus:outline-hidden flex items-center justify-center flex-shrink-0 border border-slate-200 cursor-pointer"
          title="충전소 목록 토글"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 6h16M4 12h16M4 18h16" /></svg>
        </button>

        <!-- ELVIS Lite CI 로고 & 타이틀 -->
        <router-link to="/" class="flex items-center gap-2 group cursor-pointer">
          <div class="w-7 h-7 rounded-lg bg-gradient-to-br from-sky-500 to-indigo-600 flex items-center justify-center text-white shadow-xs">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <div class="flex items-center gap-1.5">
            <span class="text-sm font-black text-slate-900 tracking-tight whitespace-nowrap">
              <span>ELVIS</span>
              <span class="text-sky-600 mx-0.5">•</span>
              <span class="text-slate-500 font-semibold">LITE</span>
            </span>
            <span class="hidden md:inline-block text-[10px] px-1.5 py-0.2 rounded font-mono font-bold bg-slate-100 text-slate-600 border border-slate-200">v0.0.1</span>
            
            <!-- MySQL 실시간 연동 뱃지 -->
            <span
              v-if="store.isDbConnected"
              class="hidden lg:inline-flex items-center gap-1 text-[10px] px-2 py-0.5 rounded-full font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 shadow-2xs font-mono"
              title="MySQL 9.71 실시간 연동 상태"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              <span>DB LIVE</span>
            </span>
          </div>
        </router-link>

        <div class="h-4 w-px bg-slate-200 mx-1 hidden sm:block"></div>

        <!-- 관제 메뉴 탭 (심플 2대 메뉴) -->
        <nav class="hidden sm:flex items-center gap-1 text-xs">
          <router-link
            to="/"
            exact-active-class="bg-sky-50 text-sky-700 font-bold border-sky-200/90 shadow-2xs"
            class="px-2.5 py-1 rounded-lg border border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-100 font-medium transition-all flex items-center gap-1.5 cursor-pointer"
            title="실시간 충전기 통합 관제"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
            <span>충전기 관제</span>
          </router-link>

          <router-link
            to="/billing"
            active-class="bg-sky-50 text-sky-700 font-bold border-sky-200/90 shadow-2xs"
            class="px-2.5 py-1 rounded-lg border border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-100 font-medium transition-all flex items-center gap-1.5 cursor-pointer"
            title="충전 거래 세션 및 과금 원장"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <span>과금 원장</span>
          </router-link>
        </nav>
      </div>

      <!-- Center: 글로벌 통합 검색바 (Ctrl+K) -->
      <div class="flex-1 min-w-[120px] max-w-md mx-2">
        <div class="relative w-full">
          <div class="absolute inset-y-0 left-0 pl-2.5 flex items-center pointer-events-none text-slate-400">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
          </div>

          <input
            type="text"
            v-model="store.searchQuery"
            placeholder="충전소, ID, 법인, 충전기 검색..."
            class="global-search-input w-full bg-slate-100 hover:bg-slate-50 focus:bg-white border border-slate-200 focus:border-sky-500 rounded-lg pl-8 pr-12 py-1 text-xs font-semibold text-slate-800 outline-hidden focus:ring-1 focus:ring-sky-500 transition-all placeholder:text-slate-400 shadow-2xs"
          />

          <div class="absolute inset-y-0 right-0 pr-1.5 flex items-center gap-1">
            <button
              v-if="store.searchQuery && store.searchQuery.length > 0"
              type="button"
              @click="store.searchQuery = ''"
              class="text-slate-400 hover:text-slate-600 p-0.5 rounded-full cursor-pointer"
              title="검색어 지우기"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
            </button>
            <kbd class="hidden lg:inline-flex items-center px-1.5 py-0.2 text-[9px] font-bold text-slate-400 bg-white border border-slate-200 rounded shadow-2xs font-mono">Ctrl+K</kbd>
          </div>
        </div>
      </div>

      <!-- Right: 지표 & 컨트롤 -->
      <div class="flex items-center gap-2 text-xs font-semibold flex-shrink-0">
        <!-- 소형 창 전용 [단말 제어반 열기] 버튼 (1280px 미만에서 노출) -->
        <button
          @click="store.toggleControlDrawer(true)"
          class="xl:hidden flex items-center gap-1 px-2 py-1 rounded-lg bg-sky-50 hover:bg-sky-100 text-sky-700 border border-sky-200 font-bold text-xs shadow-2xs transition-all whitespace-nowrap cursor-pointer"
          title="단말 계측치 및 제어반 열기"
        >
          <svg class="w-3.5 h-3.5 text-sky-600 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" /></svg>
          <span>제어반</span>
        </button>

        <!-- 테마 셀렉터 (라이트 기본) -->
        <div class="hidden md:flex items-center gap-1">
          <select
            :value="store.currentTheme"
            @change="onThemeChange(($event.target as HTMLSelectElement).value)"
            class="bg-slate-100 hover:bg-slate-200/80 border border-slate-200 text-slate-700 font-medium rounded-lg text-xs px-2 py-1 outline-hidden cursor-pointer shadow-2xs transition-all"
            title="UI 컬러 테마 전환"
          >
            <option value="light">라이트 (기본)</option>
            <option value="dark">다크</option>
            <option value="navy">네이비</option>
            <option value="emerald">에메랄드</option>
          </select>
        </div>

        <!-- TPS 실시간 지표 -->
        <div class="hidden sm:flex items-center gap-1 font-bold text-xs flex-shrink-0 px-2.5 py-1 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-700 shadow-2xs font-mono">
          <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
          <span>{{ store.summary.liveTps.toLocaleString() }} TPS</span>
        </div>

        <!-- 시계 -->
        <div class="font-mono text-slate-500 text-[11px] hidden md:inline">{{ currentTime }}</div>
        
        <!-- Tauri Window Controls -->
        <div class="window-controls flex items-center gap-1 ml-1">
          <button class="window-btn btn-minimize" title="최소화" @click="handleMinimize"></button>
          <button class="window-btn btn-maximize" title="최대화" @click="handleMaximize"></button>
          <button class="window-btn btn-close" title="닫기" @click="handleClose"></button>
        </div>
      </div>
    </header>

    <!-- 메인 컨테이너 -->
    <main class="flex-1 overflow-hidden p-2 md:p-3 bg-[var(--bg-base)] relative">
      <router-view />
    </main>

    <!-- 반응형 모바일/태블릿용 백드롭 -->
    <div
      v-if="store.isSidebarOpen"
      @click="store.toggleSidebar(false)"
      class="fixed inset-0 bg-slate-900/40 z-40 lg:hidden"
    ></div>

    <div
      v-if="store.isControlDrawerOpen"
      @click="store.toggleControlDrawer(false)"
      class="fixed inset-0 bg-slate-900/40 z-40 xl:hidden"
    ></div>

    <!-- 글로벌 토스트 알림 컨테이너 -->
    <div class="fixed bottom-5 right-5 flex flex-col gap-2 z-50 pointer-events-none">
      <div
        v-for="toast in store.toasts"
        :key="toast.id"
        class="pointer-events-auto bg-white/95 border p-3 flex items-start gap-2.5 w-80 rounded-xl shadow-lg text-xs backdrop-blur-md transition-all border-l-4"
        :class="toast.type === 'success' ? 'border-slate-200 border-l-emerald-500' : toast.type === 'error' ? 'border-slate-200 border-l-rose-500' : 'border-slate-200 border-l-sky-500'"
      >
        <div class="w-5 h-5 rounded-full flex items-center justify-center shrink-0 mt-0.5"
          :class="toast.type === 'success' ? 'bg-emerald-50 text-emerald-600' : toast.type === 'error' ? 'bg-rose-50 text-rose-600' : 'bg-sky-50 text-sky-600'"
        >
          <svg v-if="toast.type === 'success'" class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" /></svg>
          <svg v-else-if="toast.type === 'error'" class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
          <svg v-else class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
        </div>
        <div class="flex-1 min-w-0">
          <div class="font-bold text-slate-900">{{ toast.title }}</div>
          <div class="text-slate-500 mt-0.5 truncate">{{ toast.detail }}</div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useCsmsStore } from '@/stores/csmsStore'

const store = useCsmsStore()
const currentTime = ref('')
let timer: number | null = null

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('ko-KR', { hour12: false })
}

function onThemeChange(theme: string) {
  store.setTheme(theme)
  document.documentElement.setAttribute('data-theme', theme)
}

function handleMinimize() {
  // @ts-ignore
  if (window.__TAURI__) {
    // @ts-ignore
    import('@tauri-apps/api/window').then(({ appWindow }) => appWindow.minimize())
  }
}

function handleMaximize() {
  // @ts-ignore
  if (window.__TAURI__) {
    // @ts-ignore
    import('@tauri-apps/api/window').then(({ appWindow }) => appWindow.toggleMaximize())
  }
}

function handleClose() {
  // @ts-ignore
  if (window.__TAURI__) {
    // @ts-ignore
    import('@tauri-apps/api/window').then(({ appWindow }) => appWindow.close())
  }
}

let syncTimer: number | null = null

onMounted(() => {
  updateTime()
  timer = window.setInterval(updateTime, 1000)
  document.documentElement.setAttribute('data-theme', store.currentTheme)
  store.fetchInitialData()

  // 3초 주기 실시간 DB 데이터 자동 동기화 (Polling)
  syncTimer = window.setInterval(() => {
    store.fetchInitialData(true)
  }, 3000)

  // 단축키 Ctrl+K 글로벌 검색 포커스
  window.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault()
      const searchInput = document.querySelector('.global-search-input') as HTMLInputElement
      if (searchInput) {
        searchInput.focus()
        searchInput.select()
      }
    }
  })

  // 윈도우 리사이즈 시 데스크톱 복귀 시 드로어 자동 닫기
  window.addEventListener('resize', () => {
    if (window.innerWidth >= 1280) store.toggleControlDrawer(false)
    if (window.innerWidth >= 1024) store.toggleSidebar(false)
  })
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (syncTimer) clearInterval(syncTimer)
})
</script>

<style scoped>
.window-titlebar {
  -webkit-user-select: none;
  user-select: none;
}
</style>
