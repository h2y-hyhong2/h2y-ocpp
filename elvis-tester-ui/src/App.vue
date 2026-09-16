<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import KafkaSender from './components/KafkaSender.vue'
import LoadRunner from './components/LoadRunner.vue'
import LiveConsole from './components/LiveConsole.vue'
import SystemLauncher from './components/SystemLauncher.vue'

const currentTab = ref<'sender' | 'load'>('sender')
const backendConnected = ref<boolean | null>(null)
const currentTheme = ref<'light' | 'cyber'>('light')
let healthTimer: any = null

function initTheme() {
  const saved = (localStorage.getItem('theme') || 'light') as 'light' | 'cyber'
  onThemeChange(saved)
}

function onThemeChange(theme: 'light' | 'cyber') {
  currentTheme.value = theme
  localStorage.setItem('theme', theme)
  document.documentElement.setAttribute('data-theme', theme)
  if (theme === 'cyber') {
    document.documentElement.classList.add('dark')
  } else {
    document.documentElement.classList.remove('dark')
  }
}

function toggleTheme() {
  const nextTheme = currentTheme.value === 'light' ? 'cyber' : 'light'
  onThemeChange(nextTheme)
}

async function checkBackend() {
  try {
    const res = await fetch('/api/tester/load/status', { signal: AbortSignal.timeout(2000) })
    backendConnected.value = res.ok
  } catch {
    backendConnected.value = false
  }
}

onMounted(() => {
  initTheme()
  checkBackend()
  healthTimer = setInterval(checkBackend, 3000)
})

onUnmounted(() => {
  if (healthTimer) clearInterval(healthTimer)
})
</script>

<template>
  <div class="min-h-screen bg-slate-100 dark:bg-slate-950 text-slate-800 dark:text-slate-100 flex flex-col font-sans selection:bg-indigo-500 selection:text-white transition-colors duration-200">
    <!-- 상단 글로벌 네비게이션 헤더 (elvis-control-ui 스타일 48px 컴팩트 헤더) -->
    <header class="border-b border-slate-200 dark:border-slate-800 bg-white/85 dark:bg-slate-900/80 backdrop-blur-md sticky top-0 z-40 px-4 h-12 min-h-[48px] flex items-center justify-between transition-colors duration-200 shadow-2xs">
      <div class="flex items-center gap-2 sm:gap-2.5">
        <img
          src="/ELVIS-TESTER-SMALL-NOTEXT.svg"
          class="h-7 w-7 rounded-md object-contain shadow-xs flex-shrink-0"
          alt="ELVIS-TESTER"
        />
        <div class="h-3.5 w-px bg-slate-300 dark:bg-slate-700 flex-shrink-0"></div>
        <div class="flex items-center gap-1.5">
          <h1 class="font-black text-xs sm:text-sm tracking-tight text-slate-900 dark:text-white whitespace-nowrap">ELVIS-TESTER</h1>
          <span class="text-[9px] px-1.5 py-0.5 rounded-full font-bold bg-sky-500/10 text-sky-600 dark:text-sky-400 border border-sky-500/20 font-mono">
            v1.0-KAFKA
          </span>
        </div>
        <div class="hidden xl:flex items-center gap-1.5 pl-2 border-l border-slate-200 dark:border-slate-800 text-[11px] text-slate-500 dark:text-slate-400">
          전기차 충전기(OCPP) & Kafka 테스트 하네스
        </div>
      </div>

      <!-- 우측 상태 배지 및 테마 선택 컨트롤러 (컴팩트 일렬 배치) -->
      <div class="flex items-center gap-2 text-xs font-mono">
        <div class="hidden sm:flex items-center gap-1.5 bg-slate-100 dark:bg-slate-950 px-2.5 py-1 rounded-lg border border-slate-200 dark:border-slate-800 text-[11px]">
          <span class="text-slate-400 dark:text-slate-500">Broker:</span>
          <span class="text-emerald-600 dark:text-emerald-400 font-semibold">9092</span>
        </div>
        
        <!-- 백엔드 연결 실시간 감지 램프 -->
        <div class="flex items-center gap-1.5 bg-slate-100 dark:bg-slate-950 px-2.5 py-1 rounded-lg border border-slate-200 dark:border-slate-800 text-[11px]">
          <span
            class="w-2 h-2 rounded-full"
            :class="{
              'bg-emerald-500 ring-2 ring-emerald-500/30 animate-pulse': backendConnected === true,
              'bg-rose-500': backendConnected === false,
              'bg-amber-400 animate-ping': backendConnected === null
            }"
          ></span>
          <span class="text-slate-400 dark:text-slate-500">Backend:</span>
          <span
            class="font-semibold"
            :class="backendConnected === true ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-500 dark:text-rose-400'"
          >
            {{ backendConnected === true ? ':8085' : backendConnected === false ? 'OFF' : '...' }}
          </span>
        </div>

        <!-- 테마 선택 드롭다운 (elvis-control-ui 동일 규격) -->
        <div class="flex items-center gap-1 px-1.5 py-0.5 rounded-lg bg-slate-100 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 flex-shrink-0 shadow-2xs font-sans">
          <select
            :value="currentTheme"
            @change="onThemeChange(($event.target as HTMLSelectElement).value as any)"
            class="bg-transparent text-sky-600 dark:text-sky-400 font-bold outline-none cursor-pointer text-xs"
          >
            <option value="light">☀️ Clean</option>
            <option value="cyber">⚡ Cyber</option>
          </select>
        </div>
      </div>
    </header>

    <!-- 메인 컨텐츠 영역 -->
    <main class="flex-1 max-w-7xl w-full mx-auto p-6 flex flex-col gap-6">
      <!-- 최상단 원클릭 서비스 런처 & 상태 카드 -->
      <SystemLauncher />

      <!-- 상단 탭 전환 바 -->
      <div class="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-2">
        <div class="flex gap-2">
          <button
            @click="currentTab = 'sender'"
            class="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition cursor-pointer"
            :class="currentTab === 'sender'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/25'
              : 'bg-white dark:bg-slate-900 text-slate-600 dark:text-slate-400 border border-slate-200 dark:border-transparent hover:text-slate-900 dark:hover:text-slate-200 hover:bg-slate-200/50 dark:hover:bg-slate-800'"
          >
            <span>🚀 단건 패킷 발송기 (Direct Injector)</span>
          </button>
          <button
            @click="currentTab = 'load'"
            class="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition cursor-pointer"
            :class="currentTab === 'load'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/25'
              : 'bg-white dark:bg-slate-900 text-slate-600 dark:text-slate-400 border border-slate-200 dark:border-transparent hover:text-slate-900 dark:hover:text-slate-200 hover:bg-slate-200/50 dark:hover:bg-slate-800'"
          >
            <span>⚡ 부하 스트레스 발생기 (Load Generator)</span>
          </button>
        </div>
      </div>

      <!-- 상단 활성 도구 패널 (스위칭) -->
      <div class="transition-all duration-200">
        <KafkaSender v-if="currentTab === 'sender'" />
        <LoadRunner v-if="currentTab === 'load'" />
      </div>

      <!-- 하단 실시간 Kafka 메시지 스트림 콘솔 (상시 노출) -->
      <LiveConsole />
    </main>

    <!-- 하단 푸터 -->
    <footer class="border-t border-slate-200 dark:border-slate-800/80 px-6 py-3 text-center text-xs text-slate-500 font-mono transition-colors duration-200">
      ELVIS CSMS Platform © 2026 LSELink Corp. Test Harness Suite.
    </footer>
  </div>
</template>
