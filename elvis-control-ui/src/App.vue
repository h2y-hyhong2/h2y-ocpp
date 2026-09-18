<template>
  <div class="h-screen w-screen flex flex-col overflow-hidden font-sans select-none bg-[var(--bg-base)] text-[var(--text-bright)]" :data-theme="store.currentTheme">
    
    <!-- ========================================================================= -->
    <!-- 0. LS E-LINK 마스터 브랜드 시그니처 2.5px 악센트 라인                      -->
    <!-- ========================================================================= -->
    <div class="h-[2.5px] w-full bg-gradient-to-r from-[#0A1E5A] via-[#FA002D] to-[#E96600] flex-shrink-0 z-40"></div>

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
        <div class="flex items-center gap-2 group cursor-pointer">
          <div class="relative flex items-center justify-center p-0.5 rounded-lg bg-gradient-to-br from-[#0A1E5A]/20 via-[#FA002D]/15 to-[#E96600]/20 border border-[var(--border-glass)] shadow-2xs group-hover:border-[#E96600]/50 transition-all">
            <img src="/ELVIS-LITE-SMALL-NOTEXT.svg" class="h-6 w-6 rounded object-contain flex-shrink-0" alt="ELVIS-LITE" />
          </div>
          <div class="h-3.5 w-px bg-slate-300 dark:bg-slate-700 flex-shrink-0"></div>
          <div class="flex items-center gap-1.5">
            <span class="text-xs sm:text-sm font-black text-[var(--text-bright)] tracking-tight whitespace-nowrap flex items-center">
              <span>ELVIS</span>
              <span class="text-[#E96600] text-xs mx-0.5 font-bold">•</span>
              <span>LITE</span>
            </span>
            <span class="hidden md:inline-block text-[9px] px-1.5 py-0.2 rounded-full font-bold bg-[#E96600]/10 text-[#E96600] dark:text-[#FFA04D] border border-[#E96600]/30 shadow-2xs font-mono">v0.0.1</span>
          </div>
        </div>

        <!-- 4대 모듈 퀵 네비게이션 (대화면에서 표시) -->
        <div class="hidden xl:flex items-center gap-1 pl-2 border-l border-[var(--border-glass)] text-[11px]">
          <span class="flex items-center gap-1 px-2 py-0.5 rounded bg-gradient-to-r from-[#0A1E5A]/15 to-sky-500/15 text-sky-700 dark:text-sky-300 font-black border border-[#0A1E5A]/30 dark:border-sky-500/30 cursor-pointer shadow-2xs" title="ELVIS-CONTROL: 관제 및 충전기 제어">
            <span class="text-[#E96600]">⚡</span>
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

        <!-- 🌟 3대 디자인 컨셉 스위처 탭 버튼 -->
        <div class="flex items-center p-0.5 rounded-lg bg-[var(--bg-surface-2)] border border-[var(--border-glass)] flex-shrink-0 gap-0.5">
          <button
            @click="store.setConcept('concept1')"
            class="px-2 py-0.5 rounded text-[11px] font-bold transition-all"
            :class="store.activeConcept === 'concept1' ? 'bg-sky-600 text-white shadow-xs' : 'text-slate-500 hover:text-[var(--text-bright)]'"
            title="시안 1: 표준 분할 제어형 (좌측 트리 + 그리드 + 우측 제어반)"
          >
            🖥️ 시안 1
          </button>
          <button
            @click="store.setConcept('concept2')"
            class="px-2 py-0.5 rounded text-[11px] font-bold transition-all"
            :class="store.activeConcept === 'concept2' ? 'bg-sky-600 text-white shadow-xs' : 'text-slate-500 hover:text-[var(--text-bright)]'"
            title="시안 2: 대형 상황판 매트릭스형 (Wall View 히트맵)"
          >
            📊 시안 2
          </button>
          <button
            @click="store.setConcept('concept3')"
            class="px-2 py-0.5 rounded text-[11px] font-bold transition-all"
            :class="store.activeConcept === 'concept3' ? 'bg-sky-600 text-white shadow-xs' : 'text-slate-500 hover:text-[var(--text-bright)]'"
            title="시안 3: 전력 콕핏 & 분석형 (ECharts + 패킷)"
          >
            ⚡ 시안 3
          </button>
        </div>

        <!-- ⚙️ 관리자 설정 링크 버튼 -->
        <router-link
          to="/settings"
          class="flex items-center gap-1 px-2.5 py-1 rounded-lg border border-[var(--border-glass)] bg-[var(--bg-surface-2)] hover:bg-[var(--bg-surface-1)] text-slate-600 dark:text-slate-300 hover:text-sky-500 font-bold text-xs transition-all shadow-2xs"
          active-class="bg-sky-500/15 border-sky-500/30 text-sky-600 dark:text-sky-400"
          title="미들웨어, 스토리지 경로, DB 스키마 관리자 센터"
        >
          <span class="text-xs">⚙️</span>
          <span class="hidden md:inline text-[11px]">관리자 설정</span>
        </router-link>

        <!-- 🛠️ 미들웨어(Kafka & MySQL) 인프라 제어 팝오버 -->
        <div class="relative">
          <button
            @click="isInfraPanelOpen = !isInfraPanelOpen"
            class="flex items-center gap-1.5 px-2 py-0.5 rounded-lg border font-bold text-xs transition-all shadow-2xs"
            :class="(infraStatus.kafka || infraStatus.mysql) ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-600 dark:text-emerald-400' : 'bg-slate-500/10 border-slate-500/20 text-slate-500'"
            title="로컬 미들웨어 (Kafka & MySQL) 제어반"
          >
            <span class="text-xs">🛠️</span>
            <span class="hidden md:inline text-[11px]">미들웨어</span>
            <span class="flex items-center gap-1 ml-0.5">
              <span class="w-2 h-2 rounded-full transition-all" :class="infraStatus.kafka ? 'bg-emerald-500 ring-2 ring-emerald-500/30 animate-pulse' : 'bg-rose-400'" title="Kafka (Port: 9092)"></span>
              <span class="w-2 h-2 rounded-full transition-all" :class="infraStatus.mysql ? 'bg-emerald-500 ring-2 ring-emerald-500/30 animate-pulse' : 'bg-rose-400'" title="MySQL (Port: 3306)"></span>
            </span>
          </button>

          <!-- 팝오버 드롭다운 -->
          <div
            v-if="isInfraPanelOpen"
            class="absolute right-0 mt-2 w-80 p-3 rounded-xl bg-[var(--bg-surface-1)] border border-[var(--border-glass)] shadow-2xl z-50 flex flex-col gap-2.5 backdrop-blur-md"
          >
            <!-- 팝오버 헤더 -->
            <div class="flex items-center justify-between pb-2 border-b border-[var(--border-glass)]">
              <div class="flex items-center gap-1.5 font-black text-xs text-[var(--text-bright)]">
                <span>⚙️</span>
                <span>로컬 미들웨어 제어 센터</span>
              </div>
              <button
                @click="controlInfra('start', 'all')"
                :disabled="infraLoading"
                class="px-2 py-0.5 rounded bg-sky-600 hover:bg-sky-500 text-white font-bold text-[10px] shadow-xs transition-all disabled:opacity-50"
              >
                ⚡ 전체 기동
              </button>
            </div>

            <!-- Kafka 제어 카드 -->
            <div class="p-2 rounded-lg bg-[var(--bg-surface-2)] border border-[var(--border-glass)] flex flex-col gap-1.5">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-1.5">
                  <span class="w-2 h-2 rounded-full" :class="infraStatus.kafka ? 'bg-emerald-500 ring-2 ring-emerald-500/30 animate-pulse' : 'bg-rose-400'"></span>
                  <span class="font-bold text-xs text-[var(--text-bright)]">Apache Kafka</span>
                  <span class="text-[9px] font-mono px-1 py-0.2 rounded bg-slate-500/10 text-slate-400">9092</span>
                </div>
                <span
                  class="text-[10px] font-extrabold px-1.5 py-0.2 rounded-full"
                  :class="infraStatus.kafka ? 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/15 text-rose-500 border border-rose-500/30'"
                >
                  {{ infraStatus.kafka ? '🟢 RUNNING' : '🔴 STOPPED' }}
                </span>
              </div>
              <div class="flex items-center gap-1 pt-0.5">
                <button
                  @click="controlInfra('start', 'kafka')"
                  :disabled="infraStatus.kafka || infraLoading"
                  class="flex-1 py-1 rounded text-[11px] font-bold transition-all"
                  :class="infraStatus.kafka ? 'bg-slate-500/10 text-slate-400 cursor-not-allowed' : 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-xs'"
                >
                  ▶ 기동
                </button>
                <button
                  @click="controlInfra('stop', 'kafka')"
                  :disabled="!infraStatus.kafka || infraLoading"
                  class="flex-1 py-1 rounded text-[11px] font-bold transition-all"
                  :class="!infraStatus.kafka ? 'bg-slate-500/10 text-slate-400 cursor-not-allowed' : 'bg-rose-600 hover:bg-rose-500 text-white shadow-xs'"
                >
                  ⏹ 중지
                </button>
                <button
                  @click="controlInfra('start', 'topics')"
                  :disabled="!infraStatus.kafka || infraLoading"
                  class="px-2 py-1 rounded text-[11px] font-bold transition-all"
                  :class="!infraStatus.kafka ? 'bg-slate-500/10 text-slate-400 cursor-not-allowed' : 'bg-sky-600/20 hover:bg-sky-600/30 text-sky-600 dark:text-sky-400 border border-sky-500/30'"
                  title="ocpp-raw-events 등 4대 핵심 토픽 생성"
                >
                  토픽 생성
                </button>
              </div>
            </div>

            <!-- MySQL 제어 카드 -->
            <div class="p-2 rounded-lg bg-[var(--bg-surface-2)] border border-[var(--border-glass)] flex flex-col gap-1.5">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-1.5">
                  <span class="w-2 h-2 rounded-full" :class="infraStatus.mysql ? 'bg-emerald-500 ring-2 ring-emerald-500/30 animate-pulse' : 'bg-rose-400'"></span>
                  <span class="font-bold text-xs text-[var(--text-bright)]">MySQL 9.71</span>
                  <span class="text-[9px] font-mono px-1 py-0.2 rounded bg-slate-500/10 text-slate-400">3306</span>
                </div>
                <span
                  class="text-[10px] font-extrabold px-1.5 py-0.2 rounded-full"
                  :class="infraStatus.mysql ? 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/15 text-rose-500 border border-rose-500/30'"
                >
                  {{ infraStatus.mysql ? '🟢 RUNNING' : '🔴 STOPPED' }}
                </span>
              </div>
              <div class="flex items-center gap-1 pt-0.5">
                <button
                  @click="controlInfra('start', 'mysql')"
                  :disabled="infraStatus.mysql || infraLoading"
                  class="flex-1 py-1 rounded text-[11px] font-bold transition-all"
                  :class="infraStatus.mysql ? 'bg-slate-500/10 text-slate-400 cursor-not-allowed' : 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-xs'"
                >
                  ▶ 기동
                </button>
                <button
                  @click="controlInfra('stop', 'mysql')"
                  :disabled="!infraStatus.mysql || infraLoading"
                  class="flex-1 py-1 rounded text-[11px] font-bold transition-all"
                  :class="!infraStatus.mysql ? 'bg-slate-500/10 text-slate-400 cursor-not-allowed' : 'bg-rose-600 hover:bg-rose-500 text-white shadow-xs'"
                >
                  ⏹ 중지
                </button>
              </div>
            </div>

            <!-- 관리자 상세 설정 링크 -->
            <router-link
              to="/settings"
              @click="isInfraPanelOpen = false"
              class="w-full text-center py-1.5 rounded-lg bg-sky-500/10 hover:bg-sky-500/20 text-sky-600 dark:text-sky-400 font-bold text-[11px] border border-sky-500/20 transition-all flex items-center justify-center gap-1 cursor-pointer"
            >
              <span>⚙️ 전용 관리자 센터 (스토리지/DB 도구)</span>
            </router-link>

            <!-- 하단 정보 -->
            <div class="flex items-center justify-between text-[10px] text-slate-400 px-0.5">
              <span>* 3초 주기로 포트 헬스체크</span>
              <button @click="checkInfraStatus" class="hover:text-sky-500 underline">새로고침</button>
            </div>
          </div>
        </div>

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

    <!-- ========================================================================= -->
    <!-- 5. 필수 미들웨어 (MySQL & Kafka) 미실행 감지 및 원클릭 기동 유도 모달      -->
    <!-- ========================================================================= -->
    <div
      v-if="showInfraPrompt && (!infraStatus.mysql || !infraStatus.kafka)"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/75 backdrop-blur-sm"
    >
      <div class="relative w-full max-w-lg bg-[var(--bg-surface-1)] border border-[var(--border-glass)] rounded-2xl shadow-2xl p-6 overflow-hidden animate-fade-in">
        <!-- 상단 LS E-Link 시그니처 그라디언트 라인 -->
        <div class="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-[#0A1E5A] via-[#FA002D] to-[#E96600]"></div>

        <div class="flex items-start gap-4">
          <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-[#0A1E5A]/20 to-[#E96600]/20 border border-[var(--border-glass)] flex items-center justify-center text-2xl flex-shrink-0 shadow-inner">
            ⚡
          </div>
          <div class="flex-1 min-w-0">
            <h3 class="text-base font-bold text-[var(--text-bright)] flex items-center gap-2">
              <span>CSMS 필수 미들웨어 점검</span>
              <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-amber-500/15 text-amber-500 font-bold border border-amber-500/30">인프라 미기동</span>
            </h3>
            <p class="text-xs text-slate-500 dark:text-slate-400 mt-1.5 leading-relaxed">
              관제 센터가 충전기 실시간 통신 및 정산 원장을 정상 처리하려면 <strong class="text-[var(--text-bright)]">MySQL DB</strong>와 <strong class="text-[var(--text-bright)]">Kafka 브로커</strong>가 백그라운드에서 실행되어야 합니다.
            </p>
          </div>
        </div>

        <!-- 2대 미들웨어 실시간 상태 카드 -->
        <div class="mt-4 grid grid-cols-2 gap-2 text-xs">
          <!-- MySQL 카드 -->
          <div class="p-3 rounded-xl bg-[var(--bg-surface-2)] border border-[var(--border-glass)] flex flex-col justify-between gap-2">
            <div class="flex items-center justify-between">
              <span class="font-bold text-[var(--text-bright)] flex items-center gap-1.5">
                <span>🐬</span> MySQL 9.71
              </span>
              <span
                class="text-[9px] font-bold px-1.5 py-0.2 rounded-full"
                :class="infraStatus.mysql ? 'bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/15 text-rose-500 border border-rose-500/30'"
              >
                {{ infraStatus.mysql ? '🟢 RUNNING' : '🔴 STOPPED' }}
              </span>
            </div>
            <div class="flex items-center justify-between text-[11px] text-slate-500">
              <span>Port: 3306</span>
              <button
                v-if="!infraStatus.mysql"
                @click="startInfraFromPrompt('mysql')"
                :disabled="infraLoading"
                class="px-2 py-0.5 rounded bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-[10px] cursor-pointer"
              >
                ▶ 기동
              </button>
            </div>
          </div>

          <!-- Kafka 카드 -->
          <div class="p-3 rounded-xl bg-[var(--bg-surface-2)] border border-[var(--border-glass)] flex flex-col justify-between gap-2">
            <div class="flex items-center justify-between">
              <span class="font-bold text-[var(--text-bright)] flex items-center gap-1.5">
                <span>⚡</span> Apache Kafka
              </span>
              <span
                class="text-[9px] font-bold px-1.5 py-0.2 rounded-full"
                :class="infraStatus.kafka ? 'bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/15 text-rose-500 border border-rose-500/30'"
              >
                {{ infraStatus.kafka ? '🟢 RUNNING' : '🔴 STOPPED' }}
              </span>
            </div>
            <div class="flex items-center justify-between text-[11px] text-slate-500">
              <span>Port: 9092 (KRaft)</span>
              <button
                v-if="!infraStatus.kafka"
                @click="startInfraFromPrompt('kafka')"
                :disabled="infraLoading"
                class="px-2 py-0.5 rounded bg-sky-600 hover:bg-sky-500 text-white font-bold text-[10px] cursor-pointer"
              >
                ▶ 기동
              </button>
            </div>
          </div>
        </div>

        <!-- 액션 버튼 -->
        <div class="mt-5 flex flex-col sm:flex-row gap-2">
          <button
            @click="startInfraFromPrompt('all')"
            :disabled="infraLoading || (infraStatus.mysql && infraStatus.kafka)"
            class="flex-1 py-2.5 px-4 rounded-xl bg-gradient-to-r from-sky-600 to-emerald-600 hover:from-sky-500 hover:to-emerald-500 text-white font-bold text-xs shadow-lg shadow-sky-500/20 flex items-center justify-center gap-1.5 transition-all cursor-pointer disabled:opacity-50"
          >
            <span v-if="infraLoading" class="animate-spin">⏳</span>
            <span v-else>🚀</span>
            <span>전체 일괄 기동</span>
          </button>
          <router-link
            to="/settings"
            @click="showInfraPrompt = false"
            class="py-2.5 px-3 rounded-xl bg-sky-500/10 hover:bg-sky-500/20 text-sky-600 dark:text-sky-400 font-bold text-xs border border-sky-500/30 flex items-center justify-center gap-1 transition-all cursor-pointer"
          >
            ⚙️ 관리자 센터
          </router-link>
          <button
            @click="showInfraPrompt = false"
            class="py-2.5 px-3 rounded-xl bg-[var(--bg-surface-2)] hover:bg-[var(--bg-surface-3)] text-slate-400 hover:text-[var(--text-bright)] font-semibold text-xs border border-[var(--border-glass)] transition-all cursor-pointer"
          >
            Mock 모드로 계속
          </button>
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

// 🛠️ 로컬 미들웨어 (Kafka & MySQL) 인프라 제어 상태
const infraStatus = ref<{ kafka: boolean; mysql: boolean }>({ kafka: false, mysql: false })
const isInfraPanelOpen = ref(false)
const infraLoading = ref(false)
let infraTimer: number | null = null

// ⚡🐬 필수 인프라 (MySQL & Kafka) 미실행 감지 및 기동 유도 모달 상태
const showInfraPrompt = ref(false)

async function startInfraFromPrompt(service: 'all' | 'mysql' | 'kafka') {
  await controlInfra('start', service)
  setTimeout(async () => {
    await checkInfraStatus()
    if (infraStatus.value.mysql && infraStatus.value.kafka) {
      showInfraPrompt.value = false
      store.fetchInitialData()
    }
  }, 2500)
}

import { invoke } from '@tauri-apps/api/core'

const isTauri = typeof window !== 'undefined' && ('__TAURI_INTERNALS__' in window || '__TAURI__' in window)

// 포트 상태 조회 API (Tauri Native 우선, Web HTTP Fallback)
async function checkInfraStatus() {
  if (isTauri) {
    try {
      const data: any = await invoke('check_middleware_status')
      if (data) {
        infraStatus.value = { kafka: !!data.kafka, mysql: !!data.mysql }
        if (infraStatus.value.mysql && infraStatus.value.kafka && showInfraPrompt.value) {
          showInfraPrompt.value = false
          store.fetchInitialData()
        }
        return
      }
    } catch {
      // Tauri invoke 실패 시 웹 API 폴백
    }
  }

  try {
    const res = await fetch('/api/infra/status')
    if (res.ok) {
      const data = await res.json()
      infraStatus.value = { kafka: !!data.kafka, mysql: !!data.mysql }
      if (infraStatus.value.mysql && infraStatus.value.kafka && showInfraPrompt.value) {
        showInfraPrompt.value = false
        store.fetchInitialData()
      }
    }
  } catch (err) {
    // API 연결 실패 시 무시 (데스크톱 오프라인 등)
  }
}

// 미들웨어 제어 (시작/중지/토픽생성 - Rust 백그라운드 독립 기동)
async function controlInfra(action: 'start' | 'stop', service: 'kafka' | 'mysql' | 'all' | 'topics') {
  infraLoading.value = true
  try {
    let success = false
    let resultMsg = ''

    if (isTauri) {
      try {
        if (action === 'start') {
          resultMsg = await invoke('start_middleware_detached', { service })
        } else {
          resultMsg = await invoke('stop_middleware_detached', { service })
        }
        success = true
      } catch (tErr: any) {
        console.warn('Tauri invoke error, falling back to HTTP:', tErr)
      }
    }

    if (!success) {
      const res = await fetch(`/api/infra/${action}?service=${service}`, { method: 'POST' })
      if (res.ok) {
        const data = await res.json()
        resultMsg = data.message || '명령이 전송되었습니다.'
        success = true
      }
    }

    if (success) {
      const actionName = action === 'start' ? '백그라운드 독립 기동' : '중지 요청'
      const serviceName = service === 'all' ? '전체 미들웨어' : service === 'topics' ? 'Kafka 토픽' : service.toUpperCase()
      store.addToast({
        title: `${serviceName} ${actionName}`,
        detail: resultMsg,
        type: 'success'
      })
      // 1.5초 후 상태 갱신
      setTimeout(checkInfraStatus, 1500)
    }
  } catch (err: any) {
    store.addToast({
      title: '인프라 제어 오류',
      detail: err.message || '명령 전송에 실패했습니다.',
      type: 'error'
    })
  } finally {
    infraLoading.value = false
  }
}

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

  // 미들웨어 초기 상태 확인 및 3초 주기 자동 감지
  checkInfraStatus()
  infraTimer = window.setInterval(checkInfraStatus, 3000)

  // ⚡🐬 앱 실행 1.5초 후 MySQL 또는 Kafka 미실행 상태이면 원클릭 기동 유도 모달 자동 표출
  setTimeout(() => {
    if (!infraStatus.value.mysql || !infraStatus.value.kafka) {
      showInfraPrompt.value = true
    }
  }, 1500)

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
  if (infraTimer) clearInterval(infraTimer)
})
</script>

<style scoped>
.window-titlebar {
  -webkit-user-select: none;
  user-select: none;
}
</style>
