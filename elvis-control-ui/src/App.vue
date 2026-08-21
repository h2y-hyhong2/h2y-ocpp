<template>
  <div class="h-screen w-screen flex flex-col overflow-hidden font-sans select-none bg-[var(--bg-base)] text-[var(--text-bright)]" :data-theme="store.currentTheme">
    
    <!-- ========================================================================= -->
    <!-- 1. GNB 컴팩트 상단 헤더 (48px) - 프로토타입 Header.js 100% 동일 구현      -->
    <!-- ========================================================================= -->
    <header class="window-titlebar compact-header flex items-center justify-between px-2 sm:px-3 md:px-4 h-12 min-h-[48px] bg-[var(--bg-surface-1)] border-b border-[var(--border-glass)] z-30 flex-shrink-0 shadow-xs gap-2 sm:gap-3">
      
      <!-- Left: 로고 및 타이틀 & 햄버거 토글 & 4대 모듈 퀵 네비게이션 -->
      <div class="flex items-center gap-1.5 sm:gap-2 flex-shrink-0">
        <!-- 모바일/태블릿용 사이드바 토글 버튼 (1024px 미만에서 노출) -->
        <button
          @click="store.toggleSidebar()"
          class="lg:hidden p-1 rounded-md text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 focus:outline-none flex items-center justify-center flex-shrink-0"
          title="충전소 목록 토글"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path></svg>
        </button>

        <!-- ELVIS Lite CI 로고 & 타이틀 -->
        <div class="flex items-center gap-1.5">
          <img src="/ELVIS-LITE-SMALL-NOTEXT.svg" class="h-7 w-7 rounded-md object-contain flex-shrink-0 shadow-xs" alt="ELVIS-LITE" />
          <div class="h-3.5 w-px bg-slate-300 dark:bg-slate-700 flex-shrink-0"></div>
          <div class="flex items-center gap-1.5">
            <span class="text-xs sm:text-sm font-black text-[var(--text-bright)] tracking-tight whitespace-nowrap">ELVIS-LITE</span>
            <span class="hidden md:inline-block text-[9px] px-1.5 py-0.2 rounded-full font-bold bg-sky-500/10 text-sky-600 dark:text-sky-400 border border-sky-500/20">v0.0.1</span>
          </div>
        </div>

        <!-- 4대 모듈 퀵 네비게이션 (대화면에서 표시) -->
        <div class="hidden xl:flex items-center gap-1 pl-2 border-l border-[var(--border-glass)] text-[11px]">
          <span class="flex items-center gap-1 px-2 py-0.5 rounded bg-sky-500/15 text-sky-600 dark:text-sky-400 font-black border border-sky-500/30 cursor-pointer" title="ELVIS-CONTROL: 관제 및 충전기 제어">
            <span>⚡</span>
            <span>CONTROL</span>
          </span>
          <span class="flex items-center gap-1 px-2 py-0.5 rounded hover:bg-[var(--bg-surface-2)] text-slate-500 hover:text-emerald-600 font-bold transition-all cursor-pointer opacity-75 hover:opacity-100" title="ELVIS-CONNECT: WebSocket 통신 게이트웨이">
            <span>🟢</span>
            <span>CONNECT</span>
          </span>
          <span class="flex items-center gap-1 px-2 py-0.5 rounded hover:bg-[var(--bg-surface-2)] text-slate-500 hover:text-violet-600 font-bold transition-all cursor-pointer opacity-75 hover:opacity-100" title="ELVIS-DATA: 시계열 및 데이터 파이프라인">
            <span>🟣</span>
            <span>DATA</span>
          </span>
          <span class="flex items-center gap-1 px-2 py-0.5 rounded hover:bg-[var(--bg-surface-2)] text-slate-500 hover:text-amber-600 font-bold transition-all cursor-pointer opacity-75 hover:opacity-100" title="ELVIS-TESTER: 통합 시뮬레이터">
            <span>🟡</span>
            <span>TESTER</span>
          </span>
        </div>
      </div>

      <!-- Center: 🔍 글로벌 통합 검색바 (Ctrl+K 단축키 지원) -->
      <div class="flex-1 min-w-[120px] max-w-xl mx-1 sm:mx-2 min-w-0">
        <div class="relative w-full">
          <div class="absolute inset-y-0 left-0 pl-2.5 flex items-center pointer-events-none text-slate-400">
            <svg class="w-3.5 h-3.5 text-sky-600 dark:text-sky-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
          </div>

          <input
            type="text"
            v-model="store.searchQuery"
            placeholder="충전소, ID, 법인, 충전기..."
            class="global-search-input w-full bg-[var(--bg-surface-2)] hover:bg-[var(--bg-surface-1)] focus:bg-[var(--bg-surface-1)] border border-[var(--border-glass)] focus:border-sky-500 rounded-lg pl-8 pr-12 sm:pr-14 py-1 text-xs font-semibold text-[var(--text-bright)] outline-none focus:ring-1 focus:ring-sky-500 transition-all placeholder:text-slate-400 shadow-2xs"
          />

          <div class="absolute inset-y-0 right-0 pr-1.5 flex items-center gap-1">
            <!-- 검색어 지우기 (X) 버튼 -->
            <button
              v-if="store.searchQuery && store.searchQuery.length > 0"
              type="button"
              @click="store.searchQuery = ''"
              class="text-slate-400 hover:text-slate-600 p-0.5 rounded-full"
              title="검색어 지우기"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
            </button>
            <!-- 단축키 뱃지 -->
            <kbd class="hidden lg:inline-flex items-center px-1.5 py-0.2 text-[9px] font-bold text-slate-400 bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded shadow-2xs font-mono">Ctrl+K</kbd>
          </div>
        </div>
      </div>

      <!-- Right: 우측 컨트롤 & 지표 (줄바꿈 없이 일렬 배치) -->
      <div class="flex items-center gap-1.5 sm:gap-2 text-xs font-semibold flex-shrink-0">
        
        <!-- 소형 창/분할 화면 전용 [단말 제어반 열기] 버튼 (1280px 미만에서 노출) -->
        <button
          @click="store.toggleControlDrawer(true)"
          class="xl:hidden flex items-center gap-1 px-2 py-1 rounded-lg bg-sky-50 dark:bg-sky-950/50 hover:bg-sky-100 dark:hover:bg-sky-900/60 text-sky-700 dark:text-sky-300 border border-sky-200 dark:border-sky-800 font-bold text-xs shadow-xs transition-all whitespace-nowrap"
          title="단말 계측치 및 제어반 열기"
        >
          <svg class="w-3.5 h-3.5 text-sky-600 dark:text-sky-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
          <span>제어반</span>
        </button>

        <!-- 테마 선택 드롭다운 -->
        <div class="flex items-center gap-1 px-1.5 py-0.5 rounded-lg bg-[var(--bg-surface-2)] border border-[var(--border-glass)] flex-shrink-0">
          <select
            :value="store.currentTheme"
            @change="onThemeChange(($event.target as HTMLSelectElement).value)"
            class="bg-transparent text-sky-600 dark:text-sky-400 font-bold outline-none cursor-pointer text-xs"
          >
            <option value="light">☀️ Clean</option>
            <option value="cyber">⚡ Cyber</option>
          </select>
        </div>

        <!-- 총 공급 전력 지표 (대형 모니터 1400px 이상에서만 표시) -->
        <div class="hidden 2xl:flex items-center gap-1 px-2 py-0.5 rounded-lg bg-[var(--bg-surface-2)] border border-[var(--border-glass)] text-xs flex-shrink-0">
          <span class="text-slate-500 font-normal text-[11px]">총 공급:</span>
          <span class="font-bold text-sky-600 dark:text-sky-400">248.5 MW</span>
        </div>

        <!-- TPS 지표 (중형 640px 이상에서 표시) -->
        <div class="hidden sm:flex items-center gap-1 font-bold text-xs flex-shrink-0">
          <svg class="w-3 h-3 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          <span class="text-emerald-600 dark:text-emerald-400">{{ store.summary.liveTps.toLocaleString() }} TPS</span>
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

    <!-- ========================================================================= -->
    <!-- 2. MAIN CONTAINER: 전체 뷰포트 100% 통합 관제 라우터 뷰                   -->
    <!-- ========================================================================= -->
    <main class="flex-1 overflow-hidden p-2 md:p-3 bg-[var(--bg-base)] relative">
      <router-view />
    </main>

    <!-- ========================================================================= -->
    <!-- 3. 전역 최상단 고정 백드롭 (반응형 모바일/태블릿용 블러 제거 딤 처리)       -->
    <!-- ========================================================================= -->
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

    <!-- ========================================================================= -->
    <!-- 4. 글로벌 토스트 알림 컨테이너                                           -->
    <!-- ========================================================================= -->
    <div class="fixed bottom-5 right-5 flex flex-col gap-2 z-50 pointer-events-none">
      <div
        v-for="toast in store.toasts"
        :key="toast.id"
        class="pointer-events-auto bg-[var(--bg-surface-1)] border border-[var(--border-glass)] p-3 flex items-start gap-2.5 w-76 rounded-xl shadow-xl border-l-4 animate-bounce-short text-xs"
        :class="toast.type === 'success' ? 'border-l-emerald-500' : toast.type === 'error' ? 'border-l-rose-500' : 'border-l-sky-500'"
      >
        <span class="text-base">
          {{ toast.type === 'success' ? '✅' : toast.type === 'error' ? '❌' : 'ℹ️' }}
        </span>
        <div class="flex-1 min-w-0">
          <div class="font-bold text-[var(--text-bright)]">{{ toast.title }}</div>
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

onMounted(() => {
  updateTime()
  timer = window.setInterval(updateTime, 1000)
  document.documentElement.setAttribute('data-theme', store.currentTheme)

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

  // 윈도우 리사이즈 시 데스크톱(1280px / 1024px 이상) 복귀 시 드로어 자동 닫기
  window.addEventListener('resize', () => {
    if (window.innerWidth >= 1280) store.toggleControlDrawer(false)
    if (window.innerWidth >= 1024) store.toggleSidebar(false)
  })
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.window-titlebar {
  -webkit-user-select: none;
  user-select: none;
}
</style>
