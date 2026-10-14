<template>
  <div class="h-screen w-screen flex flex-col overflow-hidden font-sans select-none bg-[var(--bg-base)] text-[var(--text-bright)]" :data-theme="store.currentTheme">
    
    <!-- ========================================================================= -->
    <!-- 0. LS E-LINK 마스터 브랜드 시그니처 2.5px 악센트 라인                      -->
    <!-- ========================================================================= -->
    <div class="h-[2.5px] w-full bg-gradient-to-r from-[#0A1E5A] via-[#FA002D] to-[#E96600] flex-shrink-0 z-40"></div>

    <!-- ========================================================================= -->
    <!-- 1. GNB 컴팩트 상단 헤더 (48px) - 프로토타입 Header.js 100% 동일 구현      -->
    <!-- ========================================================================= -->
    <header class="window-titlebar flex items-center justify-between px-3 md:px-4 h-12 min-h-[48px] bg-white/95 backdrop-blur-md border-b border-slate-200/90 z-30 flex-shrink-0 shadow-2xs gap-3">
      
      <!-- Left: 로고 및 타이틀 & 핵심 관제 메뉴 네비게이션 -->
      <div class="flex items-center gap-3 flex-shrink-0">
        <!-- 모바일/태블릿용 사이드바 토글 버튼 (1024px 미만에서 노출) -->
        <button
          @click="store.toggleSidebar()"
          class="lg:hidden p-1.5 rounded-lg text-slate-600 hover:bg-slate-100 focus:outline-none flex items-center justify-center flex-shrink-0 border border-slate-200"
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

        <!-- 🧭 핵심 관제 메뉴 탭 (시안 분기 완전 제거 및 정규 관제 탭으로 단일화) -->
        <nav class="hidden sm:flex items-center gap-1 text-xs">
          <router-link
            to="/"
            exact-active-class="bg-sky-50 text-sky-700 font-bold border-sky-200/90 shadow-2xs"
            class="px-2.5 py-1 rounded-lg border border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-100 font-medium transition-all flex items-center gap-1.5"
            title="충전기 실시간 상태 및 원격 제어"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" /></svg>
            <span>충전기 관제</span>
          </router-link>

          <router-link
            to="/metrics"
            active-class="bg-sky-50 text-sky-700 font-bold border-sky-200/90 shadow-2xs"
            class="px-2.5 py-1 rounded-lg border border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-100 font-medium transition-all flex items-center gap-1.5"
            title="1초 시계열 미터값 분석"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" /></svg>
            <span>미터값 분석</span>
          </router-link>

          <router-link
            to="/billing"
            active-class="bg-sky-50 text-sky-700 font-bold border-sky-200/90 shadow-2xs"
            class="px-2.5 py-1 rounded-lg border border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-100 font-medium transition-all flex items-center gap-1.5"
            title="충전 세션 및 과금 원장"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
            <span>과금 원장</span>
          </router-link>

          <router-link
            to="/logs"
            active-class="bg-sky-50 text-sky-700 font-bold border-sky-200/90 shadow-2xs"
            class="px-2.5 py-1 rounded-lg border border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-100 font-medium transition-all flex items-center gap-1.5"
            title="OCPP 실시간 패킷 로그"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
            <span>패킷 로그</span>
          </router-link>

          <router-link
            to="/settings"
            active-class="bg-sky-50 text-sky-700 font-bold border-sky-200/90 shadow-2xs"
            class="px-2.5 py-1 rounded-lg border border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-100 font-medium transition-all flex items-center gap-1.5"
            title="시스템 관리자 및 인프라 설정"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" /><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
            <span>관리자 설정</span>
          </router-link>
        </nav>
      </div>

      <!-- Center: 글로벌 통합 검색바 (Ctrl+K) -->
      <div class="flex-1 min-w-[120px] max-w-md mx-2 min-w-0">
        <div class="relative w-full">
          <div class="absolute inset-y-0 left-0 pl-2.5 flex items-center pointer-events-none text-slate-400">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
          </div>

          <input
            type="text"
            v-model="store.searchQuery"
            placeholder="충전소, ID, 법인, 충전기..."
            class="global-search-input w-full bg-slate-100 hover:bg-slate-50 focus:bg-white border border-slate-200 focus:border-sky-500 rounded-lg pl-8 pr-12 py-1 text-xs font-semibold text-slate-800 outline-none focus:ring-1 focus:ring-sky-500 transition-all placeholder:text-slate-400 shadow-2xs"
          />

          <div class="absolute inset-y-0 right-0 pr-1.5 flex items-center gap-1">
            <button
              v-if="store.searchQuery && store.searchQuery.length > 0"
              type="button"
              @click="store.searchQuery = ''"
              class="text-slate-400 hover:text-slate-600 p-0.5 rounded-full"
              title="검색어 지우기"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
            </button>
            <kbd class="hidden lg:inline-flex items-center px-1.5 py-0.2 text-[9px] font-bold text-slate-400 bg-white border border-slate-200 rounded shadow-2xs font-mono">Ctrl+K</kbd>
          </div>
        </div>
      </div>

      <!-- Right: 우측 컨트롤 & 지표 (줄바꿈 없이 일렬 배치) -->
      <div class="flex items-center gap-2 text-xs font-semibold flex-shrink-0">
        
        <!-- 소형 창 전용 [단말 제어반 열기] 버튼 (1280px 미만에서 노출) -->
        <button
          @click="store.toggleControlDrawer(true)"
          class="xl:hidden flex items-center gap-1 px-2 py-1 rounded-lg bg-sky-50 hover:bg-sky-100 text-sky-700 border border-sky-200 font-bold text-xs shadow-2xs transition-all whitespace-nowrap"
          title="단말 계측치 및 제어반 열기"
        >
          <svg class="w-3.5 h-3.5 text-sky-600 flex-shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" /></svg>
          <span>제어반</span>
        </button>

        <!-- 미들웨어(Kafka & MySQL) 인프라 상태 팝오버 버튼 -->
        <div class="relative">
          <button
            @click="isInfraPanelOpen = !isInfraPanelOpen"
            class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg border font-bold text-xs transition-all shadow-2xs cursor-pointer"
            :class="(infraStatus.kafka || infraStatus.mysql) ? 'bg-emerald-50 border-emerald-200 text-emerald-700' : 'bg-slate-100 border-slate-200 text-slate-500'"
            title="로컬 미들웨어 (Kafka & MySQL) 상태 제어"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 12h14M5 12l4-4m-4 4l4 4M19 12l-4-4m4 4l-4 4" /></svg>
            <span class="hidden md:inline text-[11px]">인프라</span>
            <span class="flex items-center gap-1 ml-0.5">
              <span class="w-2 h-2 rounded-full transition-all" :class="infraStatus.kafka ? 'bg-emerald-500 animate-pulse' : 'bg-slate-400'" title="Kafka (Port: 19092)"></span>
              <span class="w-2 h-2 rounded-full transition-all" :class="infraStatus.mysql ? 'bg-emerald-500 animate-pulse' : 'bg-slate-400'" title="MySQL (Port: 13306)"></span>
            </span>
          </button>

          <!-- 팝오버 드롭다운 (모던 라이트 테마) -->
          <div
            v-if="isInfraPanelOpen"
            class="absolute right-0 mt-2 w-80 p-3 rounded-xl bg-white border border-slate-200 shadow-xl z-50 flex flex-col gap-2.5 backdrop-blur-md"
          >
            <!-- 팝오버 헤더 -->
            <div class="flex items-center justify-between pb-2 border-b border-slate-100">
              <div class="flex items-center gap-1.5 font-bold text-xs text-slate-800">
                <svg class="w-3.5 h-3.5 text-sky-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" /></svg>
                <span>로컬 미들웨어 상태</span>
              </div>
              <div class="flex items-center gap-1">
                <button
                  @click="openMiddlewareConsole"
                  :disabled="infraLoading"
                  class="px-2 py-0.5 rounded bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-[10px] border border-slate-200 shadow-2xs transition-all disabled:opacity-50 cursor-pointer"
                  title="미들웨어 콘솔 창 동시 실행"
                >
                  콘솔 창
                </button>
                <button
                  @click="controlInfra('start', 'all')"
                  :disabled="infraLoading"
                  class="px-2 py-0.5 rounded bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-[10px] shadow-xs transition-all disabled:opacity-50 cursor-pointer"
                >
                  전체 기동
                </button>
              </div>
            </div>

            <!-- Kafka 제어 카드 -->
            <div class="p-2.5 rounded-lg bg-slate-50 border border-slate-200/80 flex flex-col gap-1.5">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-1.5">
                  <span class="w-2 h-2 rounded-full" :class="infraStatus.kafka ? 'bg-emerald-500 animate-pulse' : 'bg-slate-400'"></span>
                  <span class="font-bold text-xs text-slate-800">Apache Kafka</span>
                  <span class="text-[9px] font-mono px-1 py-0.2 rounded bg-white text-slate-500 border border-slate-200">Port 19092</span>
                </div>
                <span
                  class="text-[10px] font-mono font-bold px-1.5 py-0.2 rounded-full"
                  :class="infraStatus.kafka ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-slate-200 text-slate-500'"
                >
                  {{ infraStatus.kafka ? 'RUNNING' : 'STOPPED' }}
                </span>
              </div>
              <div class="flex items-center gap-1 pt-1">
                <button
                  @click="controlInfra('start', 'kafka')"
                  :disabled="infraStatus.kafka || infraLoading"
                  class="flex-1 py-1 rounded text-[11px] font-medium transition-all"
                  :class="infraStatus.kafka ? 'bg-slate-200 text-slate-400 cursor-not-allowed' : 'bg-emerald-600 hover:bg-emerald-700 text-white shadow-xs'"
                >
                  기동
                </button>
                <button
                  @click="controlInfra('stop', 'kafka')"
                  :disabled="!infraStatus.kafka || infraLoading"
                  class="flex-1 py-1 rounded text-[11px] font-medium transition-all"
                  :class="!infraStatus.kafka ? 'bg-slate-200 text-slate-400 cursor-not-allowed' : 'bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-200 shadow-2xs'"
                >
                  중지
                </button>
                <button
                  @click="controlInfra('start', 'topics')"
                  :disabled="!infraStatus.kafka || infraLoading"
                  class="px-2 py-1 rounded text-[11px] font-medium transition-all bg-white hover:bg-slate-100 text-slate-700 border border-slate-200 shadow-2xs"
                  title="ocpp-raw-events 등 4대 핵심 토픽 생성"
                >
                  토픽 생성
                </button>
              </div>
            </div>

            <!-- MySQL 제어 카드 -->
            <div class="p-2.5 rounded-lg bg-slate-50 border border-slate-200/80 flex flex-col gap-1.5">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-1.5">
                  <span class="w-2 h-2 rounded-full" :class="infraStatus.mysql ? 'bg-emerald-500 animate-pulse' : 'bg-slate-400'"></span>
                  <span class="font-bold text-xs text-slate-800">MySQL 9.71</span>
                  <span class="text-[9px] font-mono px-1 py-0.2 rounded bg-white text-slate-500 border border-slate-200">Port 13306</span>
                </div>
                <span
                  class="text-[10px] font-mono font-bold px-1.5 py-0.2 rounded-full"
                  :class="infraStatus.mysql ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-slate-200 text-slate-500'"
                >
                  {{ infraStatus.mysql ? 'RUNNING' : 'STOPPED' }}
                </span>
              </div>
              <div class="flex items-center gap-1 pt-1">
                <button
                  @click="controlInfra('start', 'mysql')"
                  :disabled="infraStatus.mysql || infraLoading"
                  class="flex-1 py-1 rounded text-[11px] font-medium transition-all"
                  :class="infraStatus.mysql ? 'bg-slate-200 text-slate-400 cursor-not-allowed' : 'bg-emerald-600 hover:bg-emerald-700 text-white shadow-xs'"
                >
                  기동
                </button>
                <button
                  @click="controlInfra('stop', 'mysql')"
                  :disabled="!infraStatus.mysql || infraLoading"
                  class="flex-1 py-1 rounded text-[11px] font-medium transition-all"
                  :class="!infraStatus.mysql ? 'bg-slate-200 text-slate-400 cursor-not-allowed' : 'bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-200 shadow-2xs'"
                >
                  중지
                </button>
              </div>
            </div>

            <!-- 관리자 상세 설정 링크 -->
            <router-link
              to="/settings"
              @click="isInfraPanelOpen = false"
              class="w-full text-center py-1.5 rounded-lg bg-slate-50 hover:bg-slate-100 text-slate-700 font-semibold text-[11px] border border-slate-200 transition-all flex items-center justify-center gap-1 cursor-pointer"
            >
              <span>관리자 설정 열기</span>
            </router-link>

            <!-- 하단 정보 -->
            <div class="flex items-center justify-between text-[10px] text-slate-400 px-0.5">
              <span>* 3초 주기로 헬스체크</span>
              <button @click="checkInfraStatus" class="hover:text-sky-600 underline cursor-pointer">새로고침</button>
            </div>
          </div>
        </div>

        <!-- 테마 선택 드롭다운 (기본 라이트 테마) -->
        <div class="flex items-center gap-1 px-2 py-1 rounded-lg bg-white border border-slate-200 shadow-2xs flex-shrink-0">
          <select
            :value="store.currentTheme"
            @change="onThemeChange(($event.target as HTMLSelectElement).value)"
            class="bg-transparent text-slate-700 font-medium outline-none cursor-pointer text-xs"
          >
            <option value="light">Light Theme</option>
            <option value="cyber">Dark Theme</option>
          </select>
        </div>

        <!-- TPS 실시간 지표 -->
        <div class="hidden sm:flex items-center gap-1 font-bold text-xs flex-shrink-0 px-2 py-1 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-700 shadow-2xs font-mono">
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

    <!-- ========================================================================= -->
    <!-- 5. 필수 미들웨어 (MySQL & Kafka) 미실행 감지 및 기동 유도 모달             -->
    <!-- ========================================================================= -->
    <div
      v-if="showInfraPrompt && (!infraStatus.mysql || !infraStatus.kafka)"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm"
    >
      <div class="relative w-full max-w-lg bg-white border border-slate-200 rounded-2xl shadow-2xl p-6 overflow-hidden">
        <div class="h-1.5 absolute top-0 left-0 right-0 bg-gradient-to-r from-sky-500 via-indigo-600 to-amber-500"></div>

        <div class="flex items-start gap-4">
          <div class="w-10 h-10 rounded-xl bg-amber-50 border border-amber-200 flex items-center justify-center text-amber-600 flex-shrink-0 shadow-xs">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
          </div>
          <div class="flex-1 min-w-0">
            <h3 class="text-base font-bold text-slate-900 flex items-center gap-2">
              <span>CSMS 미들웨어 점검</span>
              <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-amber-50 text-amber-700 font-bold border border-amber-200">기동 필요</span>
            </h3>
            <p class="text-xs text-slate-500 mt-1.5 leading-relaxed">
              충전기 실시간 통신 및 정산 원장을 정상 처리하려면 <strong>MySQL DB</strong>와 <strong>Kafka 브로커</strong>가 실행되어야 합니다.
            </p>
          </div>
        </div>

        <!-- 2대 미들웨어 실시간 상태 카드 -->
        <div class="mt-4 grid grid-cols-2 gap-3 text-xs">
          <!-- MySQL 카드 -->
          <div class="p-3.5 rounded-xl bg-slate-50 border border-slate-200 flex flex-col justify-between gap-2.5">
            <div class="flex items-center justify-between">
              <span class="font-bold text-slate-900">MySQL 9.71</span>
              <span
                class="text-[9px] font-mono font-bold px-1.5 py-0.2 rounded-full"
                :class="infraStatus.mysql ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-slate-200 text-slate-600'"
              >
                {{ infraStatus.mysql ? 'RUNNING' : 'STOPPED' }}
              </span>
            </div>
            <div class="flex items-center justify-between text-[11px] text-slate-500">
              <span class="font-mono">Port 13306</span>
              <button
                v-if="!infraStatus.mysql"
                @click="startInfraFromPrompt('mysql')"
                :disabled="infraLoading"
                class="px-2.5 py-1 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-medium text-[10px] cursor-pointer shadow-xs"
              >
                기동
              </button>
            </div>
          </div>

          <!-- Kafka 카드 -->
          <div class="p-3.5 rounded-xl bg-slate-50 border border-slate-200 flex flex-col justify-between gap-2.5">
            <div class="flex items-center justify-between">
              <span class="font-bold text-slate-900">Apache Kafka</span>
              <span
                class="text-[9px] font-mono font-bold px-1.5 py-0.2 rounded-full"
                :class="infraStatus.kafka ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-slate-200 text-slate-600'"
              >
                {{ infraStatus.kafka ? 'RUNNING' : 'STOPPED' }}
              </span>
            </div>
            <div class="flex items-center justify-between text-[11px] text-slate-500">
              <span class="font-mono">Port 19092</span>
              <button
                v-if="!infraStatus.kafka"
                @click="startInfraFromPrompt('kafka')"
                :disabled="infraLoading"
                class="px-2.5 py-1 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-medium text-[10px] cursor-pointer shadow-xs"
              >
                기동
              </button>
            </div>
          </div>
        </div>

        <!-- 하단 액션 버튼 -->
        <div class="mt-5 flex items-center justify-end gap-2">
          <button
            @click="showInfraPrompt = false"
            class="px-3 py-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 text-slate-600 text-xs font-medium cursor-pointer shadow-2xs"
          >
            닫기
          </button>
          <button
            @click="startInfraFromPrompt('all')"
            :disabled="infraLoading"
            class="px-4 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold cursor-pointer shadow-xs flex items-center gap-1.5 disabled:opacity-50"
          >
            <svg class="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24"><path d="M8 5v14l11-7z" /></svg>
            <span>전체 기동</span>
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

// 🖥️ 미들웨어 콘솔 윈도우 동시 띄우기 (MySQL 9.71 & Kafka KRaft)
async function openMiddlewareConsole() {
  infraLoading.value = true
  try {
    const res = await fetch('/api/infra/open-console', { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      store.addToast({
        title: '미들웨어 콘솔 창 실행',
        detail: data.message || 'MySQL 및 Kafka 콘솔 윈도우 창이 화면에 실행되었습니다.',
        type: 'success'
      })
      setTimeout(checkInfraStatus, 2000)
    } else {
      store.addToast({
        title: '콘솔 창 실행 실패',
        detail: data.message || '콘솔 창 실행 중 오류가 발생했습니다.',
        type: 'error'
      })
    }
  } catch (err: any) {
    store.addToast({
      title: '콘솔 실행 오류',
      detail: err.message || '서버 통신 실패',
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
  store.fetchInitialData()
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
