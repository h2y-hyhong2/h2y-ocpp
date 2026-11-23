<template>
  <!-- 메인 런처 제어판 -->
  <!-- 2. 메인 런처 창 모드 (컴팩트 5대 서비스 리모컨)                   -->
  <!-- ================================================================= -->
  <div class="min-h-screen flex flex-col bg-slate-50 text-slate-800 font-sans selection:bg-sky-500/20 selection:text-sky-900">
    <!-- 초슬림 GNB 헤더 -->
    <header class="flex-shrink-0 bg-white/95 backdrop-blur-md border-b border-slate-200/90 sticky top-0 z-50 px-3 py-1.5 flex items-center justify-between shadow-sm">
      <div class="flex items-center gap-2.5">
        <!-- 모던 미니멀 로고마크 -->
        <div class="w-6 h-6 rounded-md bg-gradient-to-br from-sky-500 to-indigo-600 flex items-center justify-center text-white shadow-sm">
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M5 12h14M5 12l4-4m-4 4l4 4M19 12l-4-4m4 4l-4 4" />
          </svg>
        </div>

        <div class="flex items-center gap-2">
          <h1 class="font-bold text-xs tracking-tight text-slate-900 flex items-center gap-1.5">
            <span class="font-mono tracking-wider font-semibold">ELVIS</span>
            <span class="text-slate-500 font-normal">Launcher</span>
          </h1>
          <!-- 전체 가동 상태 배지 -->
          <span
            class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[12px] font-mono font-medium border"
            :class="runningCount > 0 ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-100 text-slate-600 border-slate-200'"
          >
            <span class="relative flex h-2 w-2 items-center justify-center">
              <span v-if="runningCount > 0" class="absolute inline-flex h-full w-full rounded-full bg-emerald-400 launcher-live-ring"></span>
              <span class="relative inline-flex rounded-full h-1.5 w-1.5" :class="runningCount > 0 ? 'bg-emerald-500' : 'bg-slate-400'"></span>
            </span>
            <span>{{ runningCount }}/{{ totalCount }} RUNNING</span>
          </span>
        </div>
      </div>

      <!-- 상단 액션 툴바: 전체 로그 일괄 열기 + 프로세스 진단 + 상태 새로고침 -->
      <div class="flex items-center gap-1.5">
        <div class="inline-flex items-center rounded-md bg-slate-100/90 p-0.5 border border-slate-200/80 shadow-sm">
          <button
            @click="openLogsBatch('all', 'all')"
            :disabled="isOpeningLogs"
            class="px-2 py-0.5 rounded text-[12px] font-medium text-slate-700 hover:text-slate-900 hover:bg-white transition-all cursor-pointer disabled:opacity-40 flex items-center gap-1"
            title="ELVIS 통합 로그 뷰어 열기 (5분할 / 개별 / 프로세스 전환)"
          >
            <svg v-if="isOpeningLogs" class="w-3 h-3 animate-spin text-sky-600" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
            <svg v-else class="w-3 h-3 text-slate-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
            <span>로그</span>
          </button>
          <div class="w-px h-3 bg-slate-200 mx-0.5"></div>
          <button
            @click="openLogsBatch('all', 'error')"
            :disabled="isOpeningLogs"
            class="px-2 py-0.5 rounded text-[12px] font-medium text-rose-600 hover:text-rose-700 hover:bg-white transition-all cursor-pointer disabled:opacity-40"
            title="ELVIS 통합 에러/예외 로그 뷰어 열기"
          >
            에러로그
          </button>
        </div>

        <button
          @click="openProcessStatus('all')"
          class="px-2 py-1 rounded-md text-[12px] font-medium bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200/90 shadow-sm transition-all cursor-pointer flex items-center gap-1"
          title="전체 포트 및 프로세스(PID/메모리) 실시간 진단 뷰어 열기"
        >
          <svg class="w-3 h-3 text-emerald-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" /></svg>
          <span>프로세스</span>
        </button>

        <button
          @click="checkStatus"
          :disabled="isChecking"
          class="px-2 py-1 rounded-md bg-white hover:bg-slate-50 text-slate-600 hover:text-slate-900 transition-all text-xs cursor-pointer border border-slate-200 shadow-sm flex items-center gap-1.5"
          title="포트 점유 상태 새로고침"
        >
          <svg class="w-3 h-3" :class="{ 'animate-spin': isChecking }" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" /></svg>
          <span class="text-[13px] font-medium">새로고침</span>
        </button>
      </div>
    </header>

    <!-- 메인 컨텐츠 영역 (컴팩트 카드 레이아웃) -->
    <main class="w-full max-w-[700px] mx-auto p-2 flex flex-col">
      <!-- 서비스 통합 리스트 카드 (5개 서비스) -->
      <div class="bg-white rounded-xl border border-slate-200 shadow-sm flex flex-col overflow-hidden">
        
        <!-- A. 미들웨어 그룹 -->
        <div class="flex-shrink-0 px-3 py-1.5 bg-slate-50/70 border-b border-slate-200/80 flex items-center justify-between">
          <div class="flex items-center gap-2">
            <span class="w-1.5 h-1.5 rounded-full bg-sky-500"></span>
            <span class="text-[13px] font-semibold uppercase tracking-wider text-slate-700">미들웨어</span>
            <span
              class="px-1.5 py-0.5 rounded-full text-[11px] font-mono font-medium border"
              :class="infraRunningCount > 0 ? 'bg-sky-50 text-sky-700 border-sky-200' : 'bg-slate-100 text-slate-500 border-slate-200'"
            >
              {{ infraRunningCount }}/2
            </span>
          </div>

          <!-- 미들웨어 헤더 전용 제어 버튼군: 시작 / 종료 / 재기동 -->
          <div class="flex items-center gap-1.5">
            <button
              @click="startInfra"
              :disabled="isExecuting || (infra.mysql && infra.kafka)"
              class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[12px] font-medium bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200/90 shadow-sm transition-all cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-1"
              title="미들웨어(MySQL, Kafka) 시작"
            >
              <svg v-if="pendingAction.infra === 'start'" class="w-2.5 h-2.5 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
              <span>{{ pendingAction.infra === 'start' ? '기동중' : '시작' }}</span>
            </button>
            <button
              @click="stopInfra"
              :disabled="isExecuting || (!infra.mysql && !infra.kafka)"
              class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[12px] font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-200 shadow-sm transition-all cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-1"
              title="미들웨어(MySQL, Kafka) 종료"
            >
              <svg v-if="pendingAction.infra === 'stop'" class="w-2.5 h-2.5 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
              <span>{{ pendingAction.infra === 'stop' ? '종료중' : '종료' }}</span>
            </button>
            <button
              @click="restartInfra"
              :disabled="isExecuting"
              class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[12px] font-medium bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 hover:border-slate-300 shadow-sm transition-all cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-1"
              title="미들웨어(MySQL, Kafka)만 순차 재기동"
            >
              <svg v-if="pendingAction.infra === 'restart'" class="w-2.5 h-2.5 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
              <span>{{ pendingAction.infra === 'restart' ? '재기동중' : '재기동' }}</span>
            </button>
          </div>
        </div>

        <div class="flex flex-col divide-y divide-slate-100">
          <!-- 1) MySQL -->
          <div class="px-3 py-2 flex items-center justify-between gap-3 hover:bg-slate-50/60 transition-colors">
            <div class="flex items-center gap-2.5">
              <span class="relative flex h-2.5 w-2.5 items-center justify-center flex-shrink-0">
                <span
                  v-if="pendingAction.mysql"
                  class="absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75 animate-ping"
                ></span>
                <span
                  v-else-if="infra.mysql"
                  class="absolute inline-flex h-full w-full rounded-full bg-emerald-400 launcher-live-ring"
                ></span>
                <span
                  class="relative inline-flex rounded-full h-2 w-2"
                  :class="pendingAction.mysql ? 'bg-amber-500' : (infra.mysql ? 'bg-emerald-500' : 'bg-slate-300')"
                ></span>
              </span>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">MySQL 9.71</span>
                </div>
                <p class="text-[12.5px] text-slate-500 leading-tight">충전 자산, 세션, 단가, 트랜잭션 원장 보관</p>
              </div>
            </div>

            <!-- 정돈된 액션 버튼군: [상태] + [포트] + [시작/종료] + [재기동] -->
            <div class="flex items-center gap-1.5 flex-shrink-0 justify-end">
              <span
                class="min-w-[56px] text-right whitespace-nowrap text-[11px] font-mono font-semibold"
                :class="pendingAction.mysql ? 'text-amber-600' : (infra.mysql ? 'text-emerald-600' : 'text-slate-400')"
              >
                {{ pendingAction.mysql === 'start' ? 'STARTING...' : pendingAction.mysql === 'stop' ? 'STOPPING...' : pendingAction.mysql === 'restart' ? 'RESTARTING...' : (infra.mysql ? 'ONLINE' : 'OFFLINE') }}
              </span>
              <span class="min-w-[82px] text-center text-[12px] font-mono px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 border border-slate-200">Port {{ ports.mysql }}</span>

              <!-- 서비스 제어: 시작 / 종료 (항상 표시, 불가 상태 비활성화) -->
              <button
                @click="control('start', 'mysql', true)"
                :disabled="isExecuting || pendingAction.mysql !== null || infra.mysql"
                class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[13px] font-semibold bg-emerald-600 hover:bg-emerald-700 text-white border border-emerald-600 shadow-sm cursor-pointer transition-all disabled:opacity-35 disabled:cursor-not-allowed disabled:hover:bg-emerald-600 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.mysql === 'start'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.mysql === 'start' ? '기동중' : '시작' }}</span>
              </button>
              <button
                @click="control('stop', 'mysql')"
                :disabled="isExecuting || pendingAction.mysql !== null || !infra.mysql"
                class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[13px] font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-300 shadow-sm cursor-pointer transition-all disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-white disabled:hover:border-slate-200 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.mysql === 'stop'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.mysql === 'stop' ? '종료중' : '종료' }}</span>
              </button>

              <!-- 재기동 -->
              <button
                @click="restartService('mysql')"
                :disabled="isExecuting || pendingAction.mysql !== null"
                class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[13px] font-medium bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 hover:border-slate-300 shadow-sm cursor-pointer transition-all disabled:opacity-50 flex items-center justify-center gap-1"
                title="MySQL 재기동"
              >
                <svg v-if="pendingAction.mysql === 'restart'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.mysql === 'restart' ? '재기동중' : '재기동' }}</span>
              </button>
            </div>
          </div>

          <!-- 2) Kafka -->
          <div class="px-3 py-2 flex items-center justify-between gap-3 hover:bg-slate-50/60 transition-colors">
            <div class="flex items-center gap-2.5">
              <span class="relative flex h-2.5 w-2.5 items-center justify-center flex-shrink-0">
                <span
                  v-if="pendingAction.kafka"
                  class="absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75 animate-ping"
                ></span>
                <span
                  v-else-if="infra.kafka"
                  class="absolute inline-flex h-full w-full rounded-full bg-emerald-400 launcher-live-ring"
                ></span>
                <span
                  class="relative inline-flex rounded-full h-2 w-2"
                  :class="pendingAction.kafka ? 'bg-amber-500' : (infra.kafka ? 'bg-emerald-500' : 'bg-slate-300')"
                ></span>
              </span>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">Apache Kafka (KRaft)</span>
                </div>
                <p class="text-[12.5px] text-slate-500 leading-tight">OCPP 이벤트 스트리밍 및 메시지 버퍼</p>
              </div>
            </div>

            <!-- 정돈된 액션 버튼군: [상태] + [포트] + [시작/종료] + [재기동] -->
            <div class="flex items-center gap-1.5 flex-shrink-0 justify-end">
              <span
                class="min-w-[56px] text-right whitespace-nowrap text-[11px] font-mono font-semibold"
                :class="pendingAction.kafka ? 'text-amber-600' : (infra.kafka ? 'text-emerald-600' : 'text-slate-400')"
              >
                {{ pendingAction.kafka === 'start' ? 'STARTING...' : pendingAction.kafka === 'stop' ? 'STOPPING...' : pendingAction.kafka === 'restart' ? 'RESTARTING...' : (infra.kafka ? 'ONLINE' : 'OFFLINE') }}
              </span>
              <span class="min-w-[82px] text-center text-[12px] font-mono px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 border border-slate-200">Port {{ ports.kafka }}</span>

              <!-- 서비스 제어: 시작 / 종료 (항상 표시, 불가 상태 비활성화) -->
              <button
                @click="control('start', 'kafka', true)"
                :disabled="isExecuting || pendingAction.kafka !== null || infra.kafka"
                class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[13px] font-semibold bg-emerald-600 hover:bg-emerald-700 text-white border border-emerald-600 shadow-sm cursor-pointer transition-all disabled:opacity-35 disabled:cursor-not-allowed disabled:hover:bg-emerald-600 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.kafka === 'start'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.kafka === 'start' ? '기동중' : '시작' }}</span>
              </button>
              <button
                @click="control('stop', 'kafka')"
                :disabled="isExecuting || pendingAction.kafka !== null || !infra.kafka"
                class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[13px] font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-300 shadow-sm cursor-pointer transition-all disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-white disabled:hover:border-slate-200 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.kafka === 'stop'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.kafka === 'stop' ? '종료중' : '종료' }}</span>
              </button>

              <!-- 재기동 -->
              <button
                @click="restartService('kafka')"
                :disabled="isExecuting || pendingAction.kafka !== null"
                class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[13px] font-medium bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 hover:border-slate-300 shadow-sm cursor-pointer transition-all disabled:opacity-50 flex items-center justify-center gap-1"
                title="Kafka 재기동"
              >
                <svg v-if="pendingAction.kafka === 'restart'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.kafka === 'restart' ? '재기동중' : '재기동' }}</span>
              </button>
            </div>
          </div>
        </div>

        <!-- B. 코어 엔진 그룹 -->
        <div class="flex-shrink-0 px-3 py-1.5 bg-slate-50/70 border-t border-b border-slate-200/80 flex items-center justify-between">
          <div class="flex items-center gap-2">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
            <span class="text-[13px] font-semibold uppercase tracking-wider text-slate-700">코어 엔진</span>
            <span
              class="px-1.5 py-0.5 rounded-full text-[11px] font-mono font-medium border"
              :class="coreRunningCount > 0 ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-100 text-slate-500 border-slate-200'"
            >
              {{ coreRunningCount }}/3
            </span>
          </div>

          <!-- 코어 엔진 헤더 전용 제어 버튼군: 시작 / 종료 / 재기동 -->
          <div class="flex items-center gap-1.5">
            <button
              @click="startServices"
              :disabled="isExecuting || (infra.connectWs && infra.connectSync && infra.controlApi)"
              class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[12px] font-medium bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200/90 shadow-sm transition-all cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-1"
              title="코어 엔진(WS, Sync, API) 순차 시작"
            >
              <svg v-if="pendingAction.services === 'start'" class="w-2.5 h-2.5 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
              <span>{{ pendingAction.services === 'start' ? '기동중' : '시작' }}</span>
            </button>
            <button
              @click="stopServices"
              :disabled="isExecuting || (!infra.connectWs && !infra.connectSync && !infra.controlApi)"
              class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[12px] font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-200 shadow-sm transition-all cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-1"
              title="코어 엔진(WS, Sync, API) 일괄 종료"
            >
              <svg v-if="pendingAction.services === 'stop'" class="w-2.5 h-2.5 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
              <span>{{ pendingAction.services === 'stop' ? '종료중' : '종료' }}</span>
            </button>
            <button
              @click="restartServices"
              :disabled="isExecuting"
              class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[12px] font-medium bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 hover:border-slate-300 shadow-sm transition-all cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-1"
              title="미들웨어(MySQL, Kafka)는 유지하고 코어 엔진만 순차 재기동"
            >
              <svg v-if="pendingAction.services === 'restart'" class="w-2.5 h-2.5 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
              <span>{{ pendingAction.services === 'restart' ? '재기동중' : '재기동' }}</span>
            </button>
          </div>
        </div>

        <div class="flex flex-col divide-y divide-slate-100">
          <!-- 3) elvis-connect-ws -->
          <div class="px-3 py-2 flex items-center justify-between gap-3 hover:bg-slate-50/60 transition-colors">
            <div class="flex items-center gap-2.5">
              <span class="relative flex h-2.5 w-2.5 items-center justify-center flex-shrink-0">
                <span
                  v-if="pendingAction.ws"
                  class="absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75 animate-ping"
                ></span>
                <span
                  v-else-if="infra.connectWs"
                  class="absolute inline-flex h-full w-full rounded-full bg-emerald-400 launcher-live-ring"
                ></span>
                <span
                  class="relative inline-flex rounded-full h-2 w-2"
                  :class="pendingAction.ws ? 'bg-amber-500' : (infra.connectWs ? 'bg-emerald-500' : 'bg-slate-300')"
                ></span>
              </span>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">elvis-connect-ws</span>
                </div>
                <p class="text-[12.5px] text-slate-500 leading-tight">충전기 단말 WebSocket 커넥션 수용 게이트웨이</p>
              </div>
            </div>

            <!-- 정돈된 액션 버튼군: [상태] + [포트] + [시작/종료] + [재기동] -->
            <div class="flex items-center gap-1.5 flex-shrink-0 justify-end">
              <span
                class="min-w-[56px] text-right whitespace-nowrap text-[11px] font-mono font-semibold"
                :class="pendingAction.ws ? 'text-amber-600' : (infra.connectWs ? 'text-emerald-600' : 'text-slate-400')"
              >
                {{ pendingAction.ws === 'start' ? 'STARTING...' : pendingAction.ws === 'stop' ? 'STOPPING...' : pendingAction.ws === 'restart' ? 'RESTARTING...' : (infra.connectWs ? 'ONLINE' : 'OFFLINE') }}
              </span>
              <span class="min-w-[82px] text-center text-[12px] font-mono px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 border border-slate-200">Port {{ ports.ws }}</span>

              <!-- 서비스 제어: 시작 / 종료 (항상 표시, 불가 상태 비활성화) -->
              <button
                @click="control('start', 'ws', true)"
                :disabled="isExecuting || pendingAction.ws !== null || infra.connectWs"
                class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[13px] font-semibold bg-emerald-600 hover:bg-emerald-700 text-white border border-emerald-600 shadow-sm cursor-pointer transition-all disabled:opacity-35 disabled:cursor-not-allowed disabled:hover:bg-emerald-600 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.ws === 'start'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.ws === 'start' ? '기동중' : '시작' }}</span>
              </button>
              <button
                @click="control('stop', 'ws')"
                :disabled="isExecuting || pendingAction.ws !== null || !infra.connectWs"
                class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[13px] font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-300 shadow-sm cursor-pointer transition-all disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-white disabled:hover:border-slate-200 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.ws === 'stop'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.ws === 'stop' ? '종료중' : '종료' }}</span>
              </button>

              <!-- 재기동 -->
              <button
                @click="restartService('ws')"
                :disabled="isExecuting || pendingAction.ws !== null"
                class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[13px] font-medium bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 hover:border-slate-300 shadow-sm cursor-pointer transition-all disabled:opacity-50 flex items-center justify-center gap-1"
                title="WS 게이트웨이 재기동"
              >
                <svg v-if="pendingAction.ws === 'restart'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.ws === 'restart' ? '재기동중' : '재기동' }}</span>
              </button>
            </div>
          </div>

          <!-- 4) elvis-connect-sync -->
          <div class="px-3 py-2 flex items-center justify-between gap-3 hover:bg-slate-50/60 transition-colors">
            <div class="flex items-center gap-2.5">
              <span class="relative flex h-2.5 w-2.5 items-center justify-center flex-shrink-0">
                <span
                  v-if="pendingAction.sync"
                  class="absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75 animate-ping"
                ></span>
                <span
                  v-else-if="infra.connectSync"
                  class="absolute inline-flex h-full w-full rounded-full bg-emerald-400 launcher-live-ring"
                ></span>
                <span
                  class="relative inline-flex rounded-full h-2 w-2"
                  :class="pendingAction.sync ? 'bg-amber-500' : (infra.connectSync ? 'bg-emerald-500' : 'bg-slate-300')"
                ></span>
              </span>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">elvis-connect-sync</span>
                </div>
                <p class="text-[12.5px] text-slate-500 leading-tight">Kafka 이벤트 컨슘, 상태 UPDATE 및 원장 DB 적재</p>
              </div>
            </div>

            <!-- 정돈된 액션 버튼군: [상태] + [포트] + [시작/종료] + [재기동] -->
            <div class="flex items-center gap-1.5 flex-shrink-0 justify-end">
              <span
                class="min-w-[56px] text-right whitespace-nowrap text-[11px] font-mono font-semibold"
                :class="pendingAction.sync ? 'text-amber-600' : (infra.connectSync ? 'text-emerald-600' : 'text-slate-400')"
              >
                {{ pendingAction.sync === 'start' ? 'STARTING...' : pendingAction.sync === 'stop' ? 'STOPPING...' : pendingAction.sync === 'restart' ? 'RESTARTING...' : (infra.connectSync ? 'ONLINE' : 'OFFLINE') }}
              </span>
              <span class="min-w-[82px] text-center text-[12px] font-mono px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 border border-slate-200">Port {{ ports.sync }}</span>

              <!-- 서비스 제어: 시작 / 종료 (항상 표시, 불가 상태 비활성화) -->
              <button
                @click="control('start', 'sync', true)"
                :disabled="isExecuting || pendingAction.sync !== null || infra.connectSync"
                class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[13px] font-semibold bg-emerald-600 hover:bg-emerald-700 text-white border border-emerald-600 shadow-sm cursor-pointer transition-all disabled:opacity-35 disabled:cursor-not-allowed disabled:hover:bg-emerald-600 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.sync === 'start'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.sync === 'start' ? '기동중' : '시작' }}</span>
              </button>
              <button
                @click="control('stop', 'sync')"
                :disabled="isExecuting || pendingAction.sync !== null || !infra.connectSync"
                class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[13px] font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-300 shadow-sm cursor-pointer transition-all disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-white disabled:hover:border-slate-200 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.sync === 'stop'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.sync === 'stop' ? '종료중' : '종료' }}</span>
              </button>

              <!-- 재기동 -->
              <button
                @click="restartService('sync')"
                :disabled="isExecuting || pendingAction.sync !== null"
                class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[13px] font-medium bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 hover:border-slate-300 shadow-sm cursor-pointer transition-all disabled:opacity-50 flex items-center justify-center gap-1"
                title="Sync 엔진 재기동"
              >
                <svg v-if="pendingAction.sync === 'restart'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.sync === 'restart' ? '재기동중' : '재기동' }}</span>
              </button>
            </div>
          </div>

          <!-- 5) elvis-control-api -->
          <div class="px-3 py-2 flex items-center justify-between gap-3 hover:bg-slate-50/60 transition-colors">
            <div class="flex items-center gap-2.5">
              <span class="relative flex h-2.5 w-2.5 items-center justify-center flex-shrink-0">
                <span
                  v-if="pendingAction.api"
                  class="absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75 animate-ping"
                ></span>
                <span
                  v-else-if="infra.controlApi"
                  class="absolute inline-flex h-full w-full rounded-full bg-emerald-400 launcher-live-ring"
                ></span>
                <span
                  class="relative inline-flex rounded-full h-2 w-2"
                  :class="pendingAction.api ? 'bg-amber-500' : (infra.controlApi ? 'bg-emerald-500' : 'bg-slate-300')"
                ></span>
              </span>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">elvis-control-api</span>
                </div>
                <p class="text-[12.5px] text-slate-500 leading-tight">관제 어드민 REST API 서버 & 실시간 웹소켓 푸시</p>
              </div>
            </div>

            <!-- 정돈된 액션 버튼군: [상태] + [포트] + [시작/종료] + [재기동] -->
            <div class="flex items-center gap-1.5 flex-shrink-0 justify-end">
              <span
                class="min-w-[56px] text-right whitespace-nowrap text-[11px] font-mono font-semibold"
                :class="pendingAction.api ? 'text-amber-600' : (infra.controlApi ? 'text-emerald-600' : 'text-slate-400')"
              >
                {{ pendingAction.api === 'start' ? 'STARTING...' : pendingAction.api === 'stop' ? 'STOPPING...' : pendingAction.api === 'restart' ? 'RESTARTING...' : (infra.controlApi ? 'ONLINE' : 'OFFLINE') }}
              </span>
              <span class="min-w-[82px] text-center text-[12px] font-mono px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 border border-slate-200">Port {{ ports.api }}</span>

              <!-- 서비스 제어: 시작 / 종료 (항상 표시, 불가 상태 비활성화) -->
              <button
                @click="control('start', 'api', true)"
                :disabled="isExecuting || pendingAction.api !== null || infra.controlApi"
                class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[13px] font-semibold bg-emerald-600 hover:bg-emerald-700 text-white border border-emerald-600 shadow-sm cursor-pointer transition-all disabled:opacity-35 disabled:cursor-not-allowed disabled:hover:bg-emerald-600 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.api === 'start'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.api === 'start' ? '기동중' : '시작' }}</span>
              </button>
              <button
                @click="control('stop', 'api')"
                :disabled="isExecuting || pendingAction.api !== null || !infra.controlApi"
                class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[13px] font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-300 shadow-sm cursor-pointer transition-all disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-white disabled:hover:border-slate-200 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.api === 'stop'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.api === 'stop' ? '종료중' : '종료' }}</span>
              </button>

              <!-- 재기동 -->
              <button
                @click="restartService('api')"
                :disabled="isExecuting || pendingAction.api !== null"
                class="min-w-[54px] px-2 py-0.5 rounded-md text-center text-[13px] font-medium bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 hover:border-slate-300 shadow-sm cursor-pointer transition-all disabled:opacity-50 flex items-center justify-center gap-1"
                title="Control API 재기동"
              >
                <svg v-if="pendingAction.api === 'restart'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.api === 'restart' ? '재기동중' : '재기동' }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 토스트 알림 컨테이너 -->
    <div class="fixed bottom-3 right-3 z-50 flex flex-col gap-1.5 max-w-xs pointer-events-none">
      <transition-group name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="p-2 rounded-lg shadow-md border text-xs pointer-events-auto flex items-start gap-2 backdrop-blur-md"
          :class="{
            'bg-emerald-50/95 border-emerald-300 text-emerald-900': toast.type === 'success',
            'bg-rose-50/95 border-rose-300 text-rose-900': toast.type === 'error',
            'bg-slate-50/95 border-slate-300 text-slate-900': toast.type === 'info'
          }"
        >
          <div class="flex-1">
            <div class="font-bold text-[13px]">{{ toast.title }}</div>
            <div class="text-[12px] opacity-90">{{ toast.message }}</div>
          </div>
        </div>
      </transition-group>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { invoke } from '@tauri-apps/api/core'
import { listen, type UnlistenFn } from '@tauri-apps/api/event'

interface Toast {
  id: number
  title: string
  message: string
  type: 'success' | 'error' | 'info'
}

const isTauriRuntime = () => typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window

const toasts = ref<Toast[]>([])
let toastSeq = 0
function addToast(title: string, message: string, type: 'success' | 'error' | 'info' = 'info') {
  const id = ++toastSeq
  toasts.value.push({ id, title, message, type })
  setTimeout(() => {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }, 3500)
}

// 핵심 서비스 점유 상태
const infra = reactive({
  mysql: false,
  kafka: false,
  connectWs: false,
  connectSync: false,
  controlApi: false
})

const ports = reactive({
  mysql: 13306,
  kafka: 19092,
  ws: 9080,
  sync: 9081,
  api: 9082
})

const isChecking = ref(false)
const isExecuting = ref(false)
const isOpeningLogs = ref(false)

// 서비스별 비동기 작업 진행 상태 추적 (스피너 및 '기동중/종료중' 텍스트 표시용)
const pendingAction = reactive<Record<string, string | null>>({
  mysql: null,
  kafka: null,
  ws: null,
  sync: null,
  api: null,
  infra: null,
  services: null,
  all: null
})

const totalCount = computed(() => 5)

// 가동 중인 전체 서비스 수 계산 (총 5개 기준)
const runningCount = computed(() => {
  return [
    infra.mysql,
    infra.kafka,
    infra.connectWs,
    infra.connectSync,
    infra.controlApi
  ].filter(Boolean).length
})

// 미들웨어 가동 수 (2개 기준)
const infraRunningCount = computed(() => {
  return [infra.mysql, infra.kafka].filter(Boolean).length
})

// 코어 엔진 가동 수 (3개 기준)
const coreRunningCount = computed(() => {
  return [infra.connectWs, infra.connectSync, infra.controlApi].filter(Boolean).length
})

let statusPollingTimer: any = null

async function startServiceCmd(service: string, isBackground = true): Promise<{ success: boolean; message: string }> {
  if (isTauriRuntime()) {
    const msg = await invoke<string>('start_middleware_detached', { service })
    return { success: true, message: msg }
  }
  const res = await fetch(`/api/infra/start?service=${encodeURIComponent(service)}&background=${isBackground}`, { method: 'POST' })
  return await res.json()
}

async function stopServiceCmd(service: string): Promise<{ success: boolean; message: string }> {
  if (isTauriRuntime()) {
    const msg = await invoke<string>('stop_middleware_detached', { service })
    return { success: true, message: msg }
  }
  const res = await fetch(`/api/infra/stop?service=${encodeURIComponent(service)}`, { method: 'POST' })
  return await res.json()
}

function isGroupStopped(group: 'all-infra' | 'all-services' | 'all'): boolean {
  if (group === 'all-infra') {
    return !infra.mysql && !infra.kafka
  }
  if (group === 'all-services') {
    return !infra.connectWs && !infra.connectSync && !infra.controlApi
  }
  return !infra.mysql && !infra.kafka && !infra.connectWs && !infra.connectSync && !infra.controlApi
}

async function restartServiceCmd(service: string, isBackground = true): Promise<{ success: boolean; message: string }> {
  const stopRes = await stopServiceCmd(service)
  if (!stopRes.success) {
    return stopRes
  }
  await waitForCondition(() => !getServiceStatus(service), 10000, 400)
  return await startServiceCmd(service, isBackground)
}

async function restartGroupCmd(group: 'all-infra' | 'all-services' | 'all'): Promise<{ success: boolean; message: string }> {
  const stopRes = await stopServiceCmd(group)
  if (!stopRes.success) {
    return stopRes
  }
  await waitForCondition(() => isGroupStopped(group), 12000, 400)
  return await startServiceCmd(group, true)
}

function clearTauriTrayPending() {
  if (isTauriRuntime()) {
    invoke('reset_tray_pending').catch(() => {})
  }
}

// 서비스별 상태 조회 헬퍼
function getServiceStatus(service: string): boolean {
  if (service === 'mysql') return !!infra.mysql
  if (service === 'kafka') return !!infra.kafka
  if (service === 'ws') return !!infra.connectWs
  if (service === 'sync') return !!infra.connectSync
  if (service === 'api') return !!infra.controlApi
  return false
}

// 특정 상태 조건이 충족될 때까지 실시간 폴링 검사 (완전 기동 / 완전 종료 / 완전 재가동 대기)
async function waitForCondition(
  predicate: () => boolean,
  timeoutMs: number = 45000,
  intervalMs: number = 700
): Promise<boolean> {
  const startTime = Date.now()
  while (Date.now() - startTime < timeoutMs) {
    await checkStatus()
    if (predicate()) {
      return true
    }
    await new Promise((resolve) => setTimeout(resolve, intervalMs))
  }
  await checkStatus()
  return predicate()
}

// 1. 상태 새로고침
async function checkStatus() {
  if (isChecking.value) return
  isChecking.value = true
  try {
    const data = isTauriRuntime()
      ? await invoke<any>('check_middleware_status')
      : await (await fetch('/api/infra/status')).json()
    infra.mysql = !!data.mysql
    infra.kafka = !!data.kafka
    infra.controlApi = !!data.controlApi
    infra.connectWs = !!data.connectWs
    infra.connectSync = !!data.connectSync

    if (data.ports) {
      ports.mysql = data.ports.mysql || ports.mysql
      ports.kafka = data.ports.kafka || ports.kafka
      ports.api = data.ports.api || ports.api
      ports.ws = data.ports.ws || ports.ws
      ports.sync = data.ports.sync || ports.sync
    }

    // 서비스가 완전히 기동되거나 종료되면 개별 pending 상태 자동 해제
    if (infra.mysql && pendingAction.mysql === 'start') pendingAction.mysql = null
    if (!infra.mysql && pendingAction.mysql === 'stop') pendingAction.mysql = null

    if (infra.kafka && pendingAction.kafka === 'start') pendingAction.kafka = null
    if (!infra.kafka && pendingAction.kafka === 'stop') pendingAction.kafka = null

    if (infra.connectWs && pendingAction.ws === 'start') pendingAction.ws = null
    if (!infra.connectWs && pendingAction.ws === 'stop') pendingAction.ws = null

    if (infra.connectSync && pendingAction.sync === 'start') pendingAction.sync = null
    if (!infra.connectSync && pendingAction.sync === 'stop') pendingAction.sync = null

    if (infra.controlApi && pendingAction.api === 'start') pendingAction.api = null
    if (!infra.controlApi && pendingAction.api === 'stop') pendingAction.api = null
  } catch (e: any) {
    console.warn('[Status-Check-Error]', e.message)
  } finally {
    isChecking.value = false
  }
}

// 2. 서비스 단독 제어 (완전히 기동/종료될 때까지 계속 진행중 표시 유지)
async function control(action: 'start' | 'stop', service: string, isBackground = true) {
  isExecuting.value = true
  pendingAction[service] = action
  const targetState = action === 'start'
  const actionText = targetState ? '기동' : '종료'
  try {
    const data = targetState ? await startServiceCmd(service, isBackground) : await stopServiceCmd(service)
    if (data.success) {
      addToast(`${service.toUpperCase()} ${actionText} 명령`, `${service.toUpperCase()} 서비스 ${actionText} 중... 완전히 반영될 때까지 대기합니다.`, 'info')

      // 완전히 포트가 열리거나(start) 닫힐(stop) 때까지 폴링 검사
      const timeout = targetState ? 45000 : 15000
      const ok = await waitForCondition(() => getServiceStatus(service) === targetState, timeout, 700)
      if (ok) {
        addToast(`${service.toUpperCase()} ${actionText} 완료`, `${service.toUpperCase()} 서비스가 정상적으로 ${actionText}되었습니다.`, 'success')
      } else {
        addToast(`${service.toUpperCase()} 진행 중`, `${service.toUpperCase()} 서비스가 백그라운드에서 진행되고 있습니다.`, 'info')
      }
    } else {
      addToast('제어 실패', data.message, 'error')
    }
  } catch (err: any) {
    addToast('통신 오류', err.message || String(err), 'error')
  } finally {
    pendingAction[service] = null
    isExecuting.value = false
    clearTauriTrayPending()
    await checkStatus()
  }
}

// 3. 인프라만 시작 (MySQL & Kafka가 둘 다 완전히 기동될 때까지 대기)
async function startInfra() {
  isExecuting.value = true
  pendingAction.infra = 'start'
  if (!infra.mysql) pendingAction.mysql = 'start'
  if (!infra.kafka) pendingAction.kafka = 'start'
  try {
    const data = await startServiceCmd('all-infra', true)
    if (data.success) {
      addToast('인프라 기동 요청', 'MySQL 및 Kafka가 완전히 뜰 때까지 대기합니다.', 'info')
      const ok = await waitForCondition(() => infra.mysql && infra.kafka, 45000, 800)
      if (ok) {
        addToast('인프라 기동 완료', 'MySQL 및 Kafka가 모두 정상 가동되었습니다.', 'success')
      }
    } else {
      addToast('기동 실패', data.message, 'error')
    }
  } catch (err: any) {
    addToast('기동 실패', err.message || String(err), 'error')
  } finally {
    pendingAction.infra = null
    pendingAction.mysql = null
    pendingAction.kafka = null
    isExecuting.value = false
    clearTauriTrayPending()
    await checkStatus()
  }
}

// 3-1. 인프라만 중지 (MySQL & Kafka가 둘 다 완전히 종료될 때까지 대기)
async function stopInfra() {
  isExecuting.value = true
  pendingAction.infra = 'stop'
  if (infra.mysql) pendingAction.mysql = 'stop'
  if (infra.kafka) pendingAction.kafka = 'stop'
  try {
    const data = await stopServiceCmd('all-infra')
    if (data.success) {
      addToast('인프라 중지 요청', 'MySQL 및 Kafka가 완전히 중지될 때까지 대기합니다.', 'info')
      const ok = await waitForCondition(() => !infra.mysql && !infra.kafka, 15000, 500)
      if (ok) {
        addToast('인프라 중지 완료', 'MySQL 및 Kafka가 모두 완전히 중지되었습니다.', 'success')
      }
    } else {
      addToast('중지 실패', data.message, 'error')
    }
  } catch (err: any) {
    addToast('중지 실패', err.message || String(err), 'error')
  } finally {
    pendingAction.infra = null
    pendingAction.mysql = null
    pendingAction.kafka = null
    isExecuting.value = false
    clearTauriTrayPending()
    await checkStatus()
  }
}

// 3-2. 인프라만 재기동 (완전히 재가동될 때까지 대기)
async function restartInfra() {
  isExecuting.value = true
  pendingAction.infra = 'restart'
  pendingAction.mysql = 'restart'
  pendingAction.kafka = 'restart'
  try {
    const data = await restartGroupCmd('all-infra')
    if (data.success) {
      addToast('인프라 재기동', '기존 인프라 정리 후 재기동 중입니다...', 'info')
      const ok = await waitForCondition(() => infra.mysql && infra.kafka, 50000, 800)
      if (ok) {
        addToast('인프라 재기동 완료', 'MySQL 및 Kafka가 정상 재기동되었습니다.', 'success')
      }
    } else {
      addToast('재기동 오류', data.message, 'error')
    }
  } catch (err: any) {
    addToast('재기동 오류', err.message || String(err), 'error')
  } finally {
    pendingAction.infra = null
    pendingAction.mysql = null
    pendingAction.kafka = null
    isExecuting.value = false
    clearTauriTrayPending()
    await checkStatus()
  }
}

// 3-3. 개별 단일 서비스 재기동 (MySQL, Kafka, WS, Sync, API)
async function restartService(service: string) {
  isExecuting.value = true
  pendingAction[service] = 'restart'
  const serviceNames: Record<string, string> = {
    mysql: 'MySQL',
    kafka: 'Kafka',
    ws: 'WS 게이트웨이',
    sync: 'Sync 엔진',
    api: 'Control API'
  }
  const svcName = serviceNames[service] || service.toUpperCase()

  try {
    const isCurrentlyRunning = getServiceStatus(service)

    // 만약 서비스가 현재 꺼져 있다면 바로 단독 기동 처리
    if (!isCurrentlyRunning) {
      addToast(`${svcName} 기동`, `${svcName} 서비스가 현재 중지 상태이므로 즉시 기동합니다.`, 'info')
      await control('start', service, true)
      return
    }

    // 서비스가 켜져 있는 경우 안전 재기동 API 호출
    addToast(`${svcName} 재기동`, `${svcName} 서비스 종료 후 재기동 중입니다...`, 'info')
    const data = await restartServiceCmd(service, true)
    if (data.success) {
      // 프로세스가 가동되어 포트가 열릴 때까지 대기 (최대 45초)
      const ok = await waitForCondition(() => getServiceStatus(service), 45000, 700)
      if (ok) {
        addToast(`${svcName} 재기동 완료`, `${svcName} 서비스가 정상적으로 재기동되었습니다.`, 'success')
      } else {
        addToast(`${svcName} 기동 대기 중`, `${svcName} 서비스가 백그라운드에서 진행 중입니다.`, 'info')
      }
    } else {
      addToast('재기동 실패', data.message, 'error')
    }
  } catch (err: any) {
    addToast('재기동 오류', err.message || String(err), 'error')
  } finally {
    pendingAction[service] = null
    isExecuting.value = false
    clearTauriTrayPending()
    await checkStatus()
  }
}

// 4. 코어 서비스만 시작 (WS, Sync, API 3대가 모두 완전히 기동될 때까지 대기)
async function startServices() {
  isExecuting.value = true
  pendingAction.services = 'start'
  if (!infra.connectWs) pendingAction.ws = 'start'
  if (!infra.connectSync) pendingAction.sync = 'start'
  if (!infra.controlApi) pendingAction.api = 'start'
  try {
    const data = await startServiceCmd('all-services', true)
    if (data.success) {
      addToast('코어 엔진 기동', 'WS -> Sync -> API 순차 기동 중입니다...', 'info')
      const ok = await waitForCondition(() => infra.connectWs && infra.connectSync && infra.controlApi, 60000, 1000)
      if (ok) {
        addToast('코어 엔진 기동 완료', '3대 코어 엔진이 모두 정상 기동되었습니다.', 'success')
      }
    } else {
      addToast('기동 실패', data.message, 'error')
    }
  } catch (err: any) {
    addToast('기동 실패', err.message || String(err), 'error')
  } finally {
    pendingAction.services = null
    pendingAction.ws = null
    pendingAction.sync = null
    pendingAction.api = null
    isExecuting.value = false
    clearTauriTrayPending()
    await checkStatus()
  }
}

// 4-1. 코어 서비스만 중지 (WS, Sync, API 3대가 모두 완전히 종료될 때까지 대기)
async function stopServices() {
  isExecuting.value = true
  pendingAction.services = 'stop'
  if (infra.connectWs) pendingAction.ws = 'stop'
  if (infra.connectSync) pendingAction.sync = 'stop'
  if (infra.controlApi) pendingAction.api = 'stop'
  try {
    const data = await stopServiceCmd('all-services')
    if (data.success) {
      addToast('코어 엔진 중지', '3대 코어 엔진 중지 중입니다...', 'info')
      const ok = await waitForCondition(() => !infra.connectWs && !infra.connectSync && !infra.controlApi, 15000, 500)
      if (ok) {
        addToast('코어 엔진 중지 완료', '3대 코어 엔진이 모두 완전히 중지되었습니다.', 'success')
      }
    } else {
      addToast('중지 실패', data.message, 'error')
    }
  } catch (err: any) {
    addToast('중지 실패', err.message || String(err), 'error')
  } finally {
    pendingAction.services = null
    pendingAction.ws = null
    pendingAction.sync = null
    pendingAction.api = null
    isExecuting.value = false
    clearTauriTrayPending()
    await checkStatus()
  }
}

// 4-2. 코어 서비스만 재기동 (WS, Sync, API 3대가 모두 완전히 재가동될 때까지 대기)
async function restartServices() {
  isExecuting.value = true
  pendingAction.services = 'restart'
  pendingAction.ws = 'restart'
  pendingAction.sync = 'restart'
  pendingAction.api = 'restart'
  try {
    const data = await restartGroupCmd('all-services')
    if (data.success) {
      addToast('엔진 재기동', '백엔드 엔진(WS, Sync, API) 순차 재기동 중입니다...', 'info')
      const ok = await waitForCondition(() => infra.connectWs && infra.connectSync && infra.controlApi, 60000, 1000)
      if (ok) {
        addToast('엔진 재기동 완료', '백엔드 엔진이 모두 정상 재기동되었습니다.', 'success')
      }
    } else {
      addToast('재기동 오류', data.message, 'error')
    }
  } catch (err: any) {
    addToast('재기동 오류', err.message || String(err), 'error')
  } finally {
    pendingAction.services = null
    pendingAction.ws = null
    pendingAction.sync = null
    pendingAction.api = null
    isExecuting.value = false
    clearTauriTrayPending()
    await checkStatus()
  }
}

// 5. 전체 기동 (인프라 + 코어 서비스 일괄 순차 부팅: 5대 모두 뜰 때까지 대기)
async function startAll(isBackground = true) {
  isExecuting.value = true
  pendingAction.all = 'start'
  if (!infra.mysql) pendingAction.mysql = 'start'
  if (!infra.kafka) pendingAction.kafka = 'start'
  if (!infra.connectWs) pendingAction.ws = 'start'
  if (!infra.connectSync) pendingAction.sync = 'start'
  if (!infra.controlApi) pendingAction.api = 'start'
  try {
    const data = await startServiceCmd('all', isBackground)
    if (data.success) {
      addToast('전체 기동', '5대 서비스가 순차 기동 중입니다...', 'info')
      const ok = await waitForCondition(
        () => infra.mysql && infra.kafka && infra.connectWs && infra.connectSync && infra.controlApi,
        75000,
        1000
      )
      if (ok) {
        addToast('전체 기동 완료', '모든 5대 서비스가 정상 기동되었습니다!', 'success')
      }
    }
  } catch (err: any) {
    addToast('기동 실패', err.message || String(err), 'error')
  } finally {
    pendingAction.all = null
    pendingAction.mysql = null
    pendingAction.kafka = null
    pendingAction.ws = null
    pendingAction.sync = null
    pendingAction.api = null
    isExecuting.value = false
    clearTauriTrayPending()
    await checkStatus()
  }
}

// 6. 전체 중지 (5대 모두 꺼질 때까지 대기)
async function stopAll() {
  isExecuting.value = true
  pendingAction.all = 'stop'
  if (infra.mysql) pendingAction.mysql = 'stop'
  if (infra.kafka) pendingAction.kafka = 'stop'
  if (infra.connectWs) pendingAction.ws = 'stop'
  if (infra.connectSync) pendingAction.sync = 'stop'
  if (infra.controlApi) pendingAction.api = 'stop'
  try {
    const data = await stopServiceCmd('all')
    if (data.success) {
      addToast('전체 중지', '전체 서비스 중지 중입니다...', 'info')
      const ok = await waitForCondition(
        () => !infra.mysql && !infra.kafka && !infra.connectWs && !infra.connectSync && !infra.controlApi,
        18000,
        600
      )
      if (ok) {
        addToast('전체 중지 완료', '모든 서비스가 완전히 중지되었습니다.', 'success')
      }
    }
  } catch (err: any) {
    addToast('중지 실패', err.message || String(err), 'error')
  } finally {
    pendingAction.all = null
    pendingAction.mysql = null
    pendingAction.kafka = null
    pendingAction.ws = null
    pendingAction.sync = null
    pendingAction.api = null
    isExecuting.value = false
    clearTauriTrayPending()
    await checkStatus()
  }
}

// 7. 전체 백그라운드 전환 재기동 (5대 모두 다시 뜰 때까지 대기)
async function restartBackground() {
  isExecuting.value = true
  pendingAction.all = 'restart'
  pendingAction.mysql = 'restart'
  pendingAction.kafka = 'restart'
  pendingAction.ws = 'restart'
  pendingAction.sync = 'restart'
  pendingAction.api = 'restart'
  try {
    const data = await restartGroupCmd('all')
    if (data.success) {
      addToast('전체 재기동', '기존 프로세스 정리 후 전체 서비스가 순차 재기동됩니다...', 'info')
      const ok = await waitForCondition(
        () => infra.mysql && infra.kafka && infra.connectWs && infra.connectSync && infra.controlApi,
        80000,
        1000
      )
      if (ok) {
        addToast('전체 재기동 완료', '모든 5대 서비스가 성공적으로 재기동되었습니다.', 'success')
      }
    }
  } catch (err: any) {
    addToast('재기동 오류', err.message || String(err), 'error')
  } finally {
    pendingAction.all = null
    pendingAction.mysql = null
    pendingAction.kafka = null
    pendingAction.ws = null
    pendingAction.sync = null
    pendingAction.api = null
    isExecuting.value = false
    clearTauriTrayPending()
    await checkStatus()
  }
}

// 8. 프로세스 및 포트 점유 실시간 진단 창 열기 (all | infra | core)
async function openProcessStatus(target: string = 'all') {
  const targetLabel = target === 'infra' ? '미들웨어' : target === 'core' ? '코어 엔진' : '전체'
  try {
    // 1) Tauri 데스크톱 환경: Rust 커맨드로 elvis-log-viewer 프로세스 진단 모드(또는 폴백 터미널) 기동
    const msg = await invoke<string>('open_process_status_window', { target })
    addToast('프로세스 진단', msg || `[${targetLabel}] 프로세스 및 포트 진단 뷰어를 열었습니다.`, 'info')
    return
  } catch (_) {}

  // 2) 브라우저 / fallback 환경: Vite 백엔드 API로 기동
  try {
    const res = await fetch(`/api/infra/open-process-monitor?target=${encodeURIComponent(target)}`, { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('프로세스 진단', data.message, 'info')
    } else {
      addToast('진단창 기동 실패', data.message, 'error')
    }
  } catch (err: any) {
    addToast('오류 발생', err.message, 'error')
  }
}

// 9. 독립 실시간 콘솔 터미널 창 띄우기 (PowerShell 실시간 스트리밍: 서비스별 단일 창 보장, 에러 모드 지원)
async function openSingleLogWindow(targetService: string, logType: 'all' | 'error'): Promise<{ ok: boolean; message: string }> {
  try {
    // 1) Tauri 데스크톱 환경: Rust 커맨드로 PowerShell 콘솔 창 실행
    const msg = await invoke<string>('open_log_window', { service: targetService, logType, wordWrap: false })
    return { ok: true, message: msg }
  } catch (_) {}

  // 2) 브라우저 / fallback 환경: Vite 백엔드 API로 PowerShell 콘솔 창 실행
  try {
    const res = await fetch(`/api/infra/logs/open-terminal?service=${targetService}&type=${logType}&wrap=0`, { method: 'POST' })
    const data = await res.json()
    return { ok: !!data.success, message: data.message || '' }
  } catch (err: any) {
    return { ok: false, message: err.message || String(err) }
  }
}

async function openLogWindow(svc?: string, logType: 'all' | 'error' = 'all') {
  const targetService = svc || 'mysql'
  const isError = logType === 'error'
  const modeTitle = isError ? '에러 콘솔' : '로그 콘솔'

  const result = await openSingleLogWindow(targetService, logType)
  if (result.ok) {
    addToast(modeTitle, result.message, isError ? 'error' : 'info')
  } else {
    addToast('터미널 기동 실패', result.message, 'error')
  }
}

// 9-1. 로그 콘솔 일괄 열기 (Windows Terminal 화면 분할 우선, 미설치 시 개별 창 순차 폴백)
// - all (5분할): 1:WS, 3:SYNC, 2+4:API, 5:MYSQL, 6:KAFKA
// - core (3분할): 1:WS, 3:SYNC, 2+4:API
// - infra (2분할): 5:MYSQL, 6:KAFKA
async function openGroupLogWindow(group: 'all' | 'infra' | 'core', logType: 'all' | 'error'): Promise<{ ok: boolean; message: string }> {
  if (isTauriRuntime()) {
    try {
      const msg = await invoke<string>('open_log_group_window', { group, logType, wordWrap: false })
      return { ok: true, message: msg }
    } catch (err: any) {
      return { ok: false, message: String(err) }
    }
  }

  try {
    const res = await fetch(`/api/infra/logs/open-group?group=${encodeURIComponent(group)}&type=${logType}&wrap=0`, { method: 'POST' })
    const data = await res.json()
    return { ok: !!data.success, message: data.message || '' }
  } catch (err: any) {
    return { ok: false, message: err.message || String(err) }
  }
}

async function openLogsBatch(group: 'all' | 'infra' | 'core', logType: 'all' | 'error' = 'all') {
  if (isOpeningLogs.value) return
  isOpeningLogs.value = true

  const services = group === 'infra'
    ? ['mysql', 'kafka']
    : group === 'core'
      ? ['ws', 'sync', 'api']
      : ['mysql', 'kafka', 'ws', 'sync', 'api']

  const groupLabel = group === 'infra' ? '미들웨어(2종)' : group === 'core' ? '코어 엔진(3종)' : '전체(5종)'
  const modeLabel = logType === 'error' ? '에러 로그' : '실시간 로그'

  try {
    // 1) Windows Terminal(wt.exe) 단일 창 분할 배치 우선 시도
    const splitRes = await openGroupLogWindow(group, logType)
    if (splitRes.ok) {
      addToast(`${groupLabel} ${modeLabel}`, splitRes.message, logType === 'error' ? 'error' : 'info')
      return
    }

    // 2) wt.exe 미설치/실패 시 개별 PowerShell 창 순차 열기 폴백
    let successCount = 0
    const failedServices: string[] = []

    for (let i = 0; i < services.length; i++) {
      const svc = services[i]
      const res = await openSingleLogWindow(svc, logType)
      if (res.ok) {
        successCount++
      } else {
        failedServices.push(svc)
      }
      if (i < services.length - 1) {
        await new Promise((resolve) => setTimeout(resolve, 180))
      }
    }

    if (failedServices.length === 0) {
      addToast(
        `${groupLabel} ${modeLabel} 일괄 열기`,
        `${successCount}개 서비스(${services.join(', ')})의 콘솔 창을 열었습니다.`,
        logType === 'error' ? 'error' : 'info'
      )
    } else {
      addToast(
        `${groupLabel} ${modeLabel} 일부 실패`,
        `성공 ${successCount}건 / 실패: ${failedServices.join(', ')}`,
        'error'
      )
    }
  } finally {
    isOpeningLogs.value = false
  }
}

let unlistenTray: UnlistenFn | null = null
let lastTrayCommandTime = 0
let lastTrayCommandKey = ''

function executeTrayCommand(action: string, service?: string) {
  const now = Date.now()
  const key = `${action}_${service || ''}`

  // 1초 내에 동일한 트레이 명령이 중복 전달되면 무시 (단일 실행 보장)
  if (now - lastTrayCommandTime < 1000 && lastTrayCommandKey === key) {
    console.log('[Tray-Command-Debounced-Skip]', key)
    return
  }

  if (isExecuting.value) {
    console.warn('[Tray-Command-Ignored] 이미 다른 작업이 진행 중입니다.', action, service)
    clearTauriTrayPending()
    return
  }

  lastTrayCommandTime = now
  lastTrayCommandKey = key
  console.log('[Tray-Command-Execute]', action, service)

  if (action === 'start' && service) {
    control('start', service, true)
  } else if (action === 'stop' && service) {
    control('stop', service)
  } else if (action === 'start_all' || action === 'start-all') {
    startAll(true)
  } else if (action === 'stop_all' || action === 'stop-all') {
    stopAll()
  } else if (action === 'restart_all' || action === 'restart-all') {
    restartBackground()
  } else if (action === 'restart_infra' || action === 'restart-infra') {
    restartInfra()
  } else if (action === 'restart_backend' || action === 'restart-backend') {
    restartServices()
  } else {
    clearTauriTrayPending()
  }
}

// 전역 window 바인딩 (Tauri window.eval 직접 호출 대응)
if (typeof window !== 'undefined') {
  ;(window as any).__triggerTrayCommand = executeTrayCommand
}

onMounted(async () => {
  document.title = 'ELVIS Launcher'
  checkStatus()
  statusPollingTimer = setInterval(checkStatus, 3000)

  try {
    unlistenTray = await listen<{ action: string; service?: string }>('tray-command', (event) => {
      const { action, service } = event.payload
      executeTrayCommand(action, service)
    })
  } catch (err) {
    console.warn('[Tray-Listener-Init-Warn]', err)
  }
})

onUnmounted(() => {
  if (statusPollingTimer) clearInterval(statusPollingTimer)
  if (unlistenTray) unlistenTray()
})
</script>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateY(20px);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>
