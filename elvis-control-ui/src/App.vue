<template>
  <div class="h-screen w-screen flex flex-col overflow-hidden font-sans select-none" :data-theme="store.currentTheme">
    
    <!-- 1. Tauri Custom Desktop Title Bar + GNB Top Bar -->
    <header class="window-titlebar flex items-center justify-between px-4 h-12 z-50 shadow-sm">
      <div class="flex items-center gap-3">
        <!-- 테두리 없는 깔끔한 LS E-Link CI -->
        <img src="/ci_logo.png" class="h-6 w-auto object-contain" alt="LS E-Link CI" />
        
        <!-- 구분선 -->
        <div class="h-4 w-px bg-slate-300 dark:bg-slate-700"></div>
        
        <!-- ELVIS Lite SVG 아이콘 + 타이틀 -->
        <div class="flex items-center gap-2">
          <img src="/ELVIS-LITE-ICON.svg" class="h-7 w-7 rounded-lg object-contain shadow-sm" alt="ELVIS Lite" />
          <span class="text-xs font-bold font-display tracking-tight text-slate-800 dark:text-slate-100">ELVIS-CSMS 관제 센터</span>
          <span class="text-[10px] px-1.5 py-0.5 rounded font-mono font-bold bg-sky-500/10 text-sky-600 dark:text-sky-400 border border-sky-500/30">v1.0.0 PoC</span>
        </div>
      </div>

      <!-- Right Header Actions (Theme Switcher + Live WS Status + Time + Window Controls) -->
      <div class="flex items-center gap-4 text-xs">
        
        <!-- 5대 멀티 테마 스위처 -->
        <div class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700">
          <span class="text-slate-500 font-medium">🎨 테마:</span>
          <select
            :value="store.currentTheme"
            @change="onThemeChange(($event.target as HTMLSelectElement).value)"
            class="bg-transparent border-none text-sky-600 dark:text-sky-400 font-bold text-xs outline-none cursor-pointer"
          >
            <option value="light">☀️ Clean Slate (기본 라이트)</option>
            <option value="cyber">⚡ Cyber Neon (사이버 다크)</option>
            <option value="emerald">🌿 Deep Emerald (에코 그린)</option>
            <option value="purple">🌌 Midnight Purple (바이올렛)</option>
            <option value="amber">🔥 Sunset Amber (골드 에너지)</option>
          </select>
        </div>

        <!-- 실시간 웹소켓 상태 -->
        <div class="flex items-center gap-1.5 font-mono text-xs">
          <span class="live-indicator"></span>
          <span class="text-emerald-600 dark:text-emerald-400 font-semibold">WS 8080 연결됨</span>
          <span class="text-slate-400">| {{ store.summary.liveTps }} TPS</span>
        </div>

        <div class="font-mono text-slate-500 text-xs">{{ currentTime }}</div>
        
        <!-- Tauri Window Controls -->
        <div class="window-controls flex items-center gap-1.5 ml-1">
          <button class="window-btn btn-minimize" title="최소화" @click="handleMinimize"></button>
          <button class="window-btn btn-maximize" title="최대화" @click="handleMaximize"></button>
          <button class="window-btn btn-close" title="닫기" @click="handleClose"></button>
        </div>
      </div>
    </header>

    <!-- 2. Main Layout (Sidebar LNB + Router Content) -->
    <div class="flex flex-1 overflow-hidden">
      
      <!-- Sidebar LNB -->
      <aside class="w-60 bg-[var(--bg-sidebar)] border-r border-[var(--border-glass)] flex flex-col justify-between p-3 select-none">
        <div class="space-y-4">
          
          <!-- System Status Mini Card -->
          <div class="glass-card p-3 bg-slate-100/50 dark:bg-slate-800/40">
            <div class="text-[11px] text-slate-500 font-medium mb-1">관제 플랫폼 가동률</div>
            <div class="flex items-baseline justify-between">
              <span class="text-base font-bold text-sky-600 dark:text-sky-400 font-mono">
                {{ store.summary.totalChargers.toLocaleString() }} <span class="text-xs font-normal text-slate-500">대</span>
              </span>
              <span class="text-xs text-emerald-600 dark:text-emerald-400 font-mono font-semibold">
                {{ ((store.summary.chargingCount / store.summary.totalChargers) * 100).toFixed(1) }}% 가동
              </span>
            </div>
            <div class="w-full bg-slate-200 dark:bg-slate-700 h-1.5 rounded-full mt-2 overflow-hidden">
              <div class="bg-gradient-to-r from-sky-500 to-emerald-400 h-full rounded-full" :style="{ width: `${(store.summary.chargingCount / store.summary.totalChargers) * 100}%` }"></div>
            </div>
          </div>

          <!-- Nav Menu (4대 프로토타입 화면 + 패킷 로그) -->
          <nav class="space-y-1">
            <router-link
              v-for="item in navItems"
              :key="item.path"
              :to="item.path"
              class="flex items-center gap-2.5 px-3 py-2.5 rounded-xl text-xs font-medium transition-all duration-150"
              :class="$route.path === item.path ? 'bg-sky-500/15 text-sky-600 dark:text-sky-400 font-bold border border-sky-500/30 shadow-sm' : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100 hover:bg-slate-200/40 dark:hover:bg-slate-800/40'"
            >
              <span class="text-base">{{ item.emoji }}</span>
              <span>{{ item.title }}</span>
              <span v-if="item.badge" class="ml-auto px-1.5 py-0.2 rounded text-[10px] bg-sky-500/20 text-sky-600 dark:text-sky-400 border border-sky-500/30 font-mono font-bold">{{ item.badge }}</span>
            </router-link>
          </nav>
        </div>

        <!-- Footer / Environment Infrastructure Ports Info -->
        <div class="p-2.5 rounded-xl bg-slate-100/60 dark:bg-slate-950/60 border border-slate-200 dark:border-slate-800/60 text-[11px] text-slate-500 font-mono space-y-1">
          <div class="flex justify-between">
            <span>Gateway WS:</span>
            <span class="text-slate-700 dark:text-slate-300 font-bold">Port 8080</span>
          </div>
          <div class="flex justify-between">
            <span>Kafka KRaft:</span>
            <span class="text-slate-700 dark:text-slate-300 font-bold">Port 9092</span>
          </div>
          <div class="flex justify-between">
            <span>MySQL 9.71:</span>
            <span class="text-slate-700 dark:text-slate-300 font-bold">Port 3306</span>
          </div>
          <div class="flex justify-between">
            <span>ClickHouse:</span>
            <span class="text-slate-700 dark:text-slate-300 font-bold">Port 8123</span>
          </div>
        </div>
      </aside>

      <!-- Main Router Content -->
      <main class="flex-1 overflow-y-auto p-5 bg-[var(--bg-base)]">
        <router-view />
      </main>

    </div>

    <!-- 3. Global Toast Notifications Container -->
    <div class="fixed bottom-5 right-5 flex flex-col gap-2 z-50">
      <div
        v-for="toast in store.toasts"
        :key="toast.id"
        class="glass-card p-3.5 flex items-start gap-3 w-80 shadow-2xl border-l-4 animate-bounce-short"
        :class="toast.type === 'success' ? 'border-l-emerald-500' : toast.type === 'error' ? 'border-l-rose-500' : 'border-l-sky-500'"
      >
        <span class="text-lg">
          {{ toast.type === 'success' ? '✅' : toast.type === 'error' ? '❌' : 'ℹ️' }}
        </span>
        <div class="text-xs">
          <div class="font-bold text-slate-900 dark:text-slate-100">{{ toast.title }}</div>
          <div class="text-slate-500 mt-0.5">{{ toast.detail }}</div>
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

const navItems = [
  {
    title: '실시간 관제 대시보드',
    path: '/',
    emoji: '📊',
    badge: 'DASH-01'
  },
  {
    title: '충전기 모니터링 & 제어',
    path: '/chargers',
    emoji: '🔌',
    badge: 'CHG-01'
  },
  {
    title: '1초 시계열 미터 분석',
    path: '/metrics',
    emoji: '📈',
    badge: 'SESSION-01'
  },
  {
    title: '충전 세션 & 과금 원장',
    path: '/billing',
    emoji: '📑',
    badge: 'CDR-01'
  },
  {
    title: 'OCPP 실시간 패킷 로그',
    path: '/logs',
    emoji: '📡',
    badge: 'LIVE'
  }
]

const onThemeChange = (theme: string) => {
  store.setTheme(theme as any)
}

const updateClock = () => {
  const now = new Date()
  currentTime.value = now.toLocaleDateString('ko-KR', { month: '2-digit', day: '2-digit', weekday: 'short' }) + ' ' + now.toLocaleTimeString('ko-KR')
}

let timer: number
onMounted(() => {
  updateClock()
  timer = window.setInterval(updateClock, 1000)
  // 초기 테마 설정
  store.setTheme(store.currentTheme)
})

onUnmounted(() => {
  clearInterval(timer)
})

// Tauri Window Controls
const handleMinimize = () => {
  console.log('[Tauri] Window Minimize')
}
const handleMaximize = () => {
  console.log('[Tauri] Window Maximize')
}
const handleClose = () => {
  console.log('[Tauri] App Close')
}
</script>

<style scoped>
@keyframes bounce-short {
  0% { transform: translateY(10px); opacity: 0; }
  100% { transform: translateY(0); opacity: 1; }
}
.animate-bounce-short {
  animation: bounce-short 0.25s ease-out;
}
</style>
