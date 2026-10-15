<template>
  <!-- 메인 런처 제어판 -->
  <!-- 2. 메인 런처 창 모드 (초슬림 & 제로 스크롤 리모컨)                  -->
  <!-- ================================================================= -->
  <div class="min-h-screen flex flex-col bg-slate-50 text-slate-800 font-sans selection:bg-sky-500/20 selection:text-sky-900">
    <!-- 초슬림 GNB 헤더 -->
    <header class="bg-white/95 backdrop-blur-md border-b border-slate-200/90 sticky top-0 z-50 px-4 py-2 flex items-center justify-between shadow-2xs">
      <div class="flex items-center gap-2.5">
        <!-- 모던 미니멀 로고마크 -->
        <div class="w-6 h-6 rounded-md bg-gradient-to-br from-sky-500 to-indigo-600 flex items-center justify-center text-white shadow-2xs">
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
            class="inline-flex items-center gap-1.5 px-2 py-0.2 rounded-full text-[10px] font-mono font-medium border"
            :class="runningCount > 0 ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-100 text-slate-600 border-slate-200'"
          >
            <span class="w-1.5 h-1.5 rounded-full" :class="runningCount > 0 ? 'bg-emerald-500 animate-pulse' : 'bg-slate-400'"></span>
            <span>{{ runningCount }}/5 RUNNING</span>
          </span>
        </div>
      </div>

      <!-- 상단 액션 툴바: 상태 새로고침 -->
      <div class="flex items-center gap-1.5">
        <button
          @click="checkStatus"
          :disabled="isChecking"
          class="px-2 py-1 rounded-md bg-white hover:bg-slate-50 text-slate-600 hover:text-slate-900 transition-all text-xs cursor-pointer border border-slate-200 shadow-2xs flex items-center gap-1.5"
          title="포트 점유 상태 새로고침"
        >
          <svg class="w-3 h-3" :class="{ 'animate-spin': isChecking }" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" /></svg>
          <span class="text-[11px] font-medium">새로고침</span>
        </button>
      </div>
    </header>

    <!-- 메인 컨텐츠 영역 (슬림 콤팩트 리모컨) -->
    <main class="flex-1 max-w-xl w-full mx-auto p-2.5 space-y-2">
      <!-- 서비스 통합 리스트 카드 -->
      <div class="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
        
        <!-- A. 인프라 미들웨어 그룹 -->
        <div class="px-3.5 py-1.5 bg-slate-50/70 border-b border-slate-200/80 flex items-center justify-between">
          <div class="flex items-center gap-2">
            <span class="w-1.5 h-1.5 rounded-full bg-sky-500"></span>
            <span class="text-[11px] font-semibold uppercase tracking-wider text-slate-700">인프라 미들웨어</span>
            <span
              class="px-1.5 py-0.2 rounded-full text-[9px] font-mono font-medium border"
              :class="infraRunningCount > 0 ? 'bg-sky-50 text-sky-700 border-sky-200' : 'bg-slate-100 text-slate-500 border-slate-200'"
            >
              {{ infraRunningCount }}/2
            </span>
          </div>

          <!-- 인프라 헤더 전용 제어 버튼군: 시작 / 종료 / 재기동 / 프로세스 확인 -->
          <div class="flex items-center gap-1">
            <button
              @click="startInfra"
              :disabled="isExecuting || (infra.mysql && infra.kafka)"
              class="px-2 py-0.5 rounded text-[10px] font-medium bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 transition-colors cursor-pointer disabled:opacity-40 flex items-center gap-1"
              title="인프라(MySQL, Kafka) 시작"
            >
              <svg v-if="pendingAction.infra === 'start'" class="w-2.5 h-2.5 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
              <span>{{ pendingAction.infra === 'start' ? '기동중...' : '시작' }}</span>
            </button>
            <button
              @click="stopInfra"
              :disabled="isExecuting || (!infra.mysql && !infra.kafka)"
              class="px-2 py-0.5 rounded text-[10px] font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-200 transition-colors cursor-pointer disabled:opacity-40 flex items-center gap-1"
              title="인프라(MySQL, Kafka) 종료"
            >
              <svg v-if="pendingAction.infra === 'stop'" class="w-2.5 h-2.5 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
              <span>{{ pendingAction.infra === 'stop' ? '종료중...' : '종료' }}</span>
            </button>
            <button
              @click="restartInfra"
              :disabled="isExecuting"
              class="px-2 py-0.5 rounded text-[10px] font-medium bg-white hover:bg-slate-100 text-slate-700 border border-slate-200 transition-colors cursor-pointer disabled:opacity-40 flex items-center gap-1"
              title="인프라(MySQL, Kafka)만 순차 재기동"
            >
              <svg v-if="pendingAction.infra === 'restart'" class="w-2.5 h-2.5 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
              <span>{{ pendingAction.infra === 'restart' ? '재기동중...' : '재기동' }}</span>
            </button>
            <button
              @click="openProcessStatus('infra')"
              class="px-1.5 py-0.5 rounded text-[10px] font-medium bg-sky-50 hover:bg-sky-100 text-sky-700 border border-sky-200 transition-colors cursor-pointer flex items-center gap-0.5"
              title="인프라 포트(13306, 19092) 및 프로세스 진단"
            >
              <svg class="w-2.5 h-2.5 text-sky-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
              <span>프로세스</span>
            </button>
          </div>
        </div>

        <div class="divide-y divide-slate-100">
          <!-- 1) MySQL -->
          <div class="px-3.5 py-2.5 flex items-center justify-between hover:bg-slate-50/60 transition-colors">
            <div class="flex items-center gap-2.5">
              <span
                class="w-2 h-2 rounded-full flex-shrink-0"
                :class="pendingAction.mysql ? 'bg-amber-400 ring-2 ring-amber-200 animate-ping' : (infra.mysql ? 'bg-emerald-500 ring-2 ring-emerald-100 animate-pulse' : 'bg-slate-300')"
              ></span>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">MySQL 9.71</span>
                  <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 border border-slate-200">Port {{ ports.mysql }}</span>
                  <span
                    class="text-[9px] font-mono font-medium"
                    :class="pendingAction.mysql ? 'text-amber-600' : (infra.mysql ? 'text-emerald-600' : 'text-slate-400')"
                  >
                    {{ pendingAction.mysql === 'start' ? 'STARTING...' : pendingAction.mysql === 'stop' ? 'STOPPING...' : (infra.mysql ? 'ONLINE' : 'OFFLINE') }}
                  </span>
                  <!-- DDL 적재 버튼을 타이틀 라인으로 배치 -->
                  <button
                    @click="loadSchema"
                    :disabled="!infra.mysql || isExecuting"
                    class="px-1.5 py-0.2 rounded text-[10px] font-medium bg-sky-50 hover:bg-sky-100 text-sky-700 border border-sky-200/80 transition-colors cursor-pointer disabled:opacity-40"
                    title="초기 DDL 스키마 및 시드 데이터 적재"
                  >
                    DDL 적재
                  </button>
                </div>
                <p class="text-[11px] text-slate-500">충전 자산, 세션, 단가, 트랜잭션 원장 보관</p>
              </div>
            </div>

            <!-- 통일된 2단 액션 버튼군 (로그 & 시작/종료 동일 규격 w-16) -->
            <div class="flex items-center gap-1.5 flex-shrink-0 w-36 justify-end">
              <button
                @click="openLogWindow('mysql')"
                class="w-14 py-1.5 rounded-lg text-center text-xs font-medium bg-slate-50 hover:bg-slate-100 text-slate-600 hover:text-slate-900 border border-slate-200 cursor-pointer transition-colors shadow-2xs"
                title="MySQL 로그 독립 창 열기"
              >
                로그
              </button>
              <button
                v-if="!infra.mysql"
                @click="control('start', 'mysql', true)"
                :disabled="isExecuting || pendingAction.mysql !== null"
                class="w-16 py-1.5 rounded-lg text-center text-xs font-medium bg-emerald-600 hover:bg-emerald-700 text-white shadow-2xs cursor-pointer transition-all disabled:opacity-50 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.mysql === 'start'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.mysql === 'start' ? '기동중' : '시작' }}</span>
              </button>
              <button
                v-else
                @click="control('stop', 'mysql')"
                :disabled="isExecuting || pendingAction.mysql !== null"
                class="w-16 py-1.5 rounded-lg text-center text-xs font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-300 shadow-2xs cursor-pointer transition-all disabled:opacity-50 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.mysql === 'stop'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.mysql === 'stop' ? '종료중' : '종료' }}</span>
              </button>
            </div>
          </div>

          <!-- 2) Kafka -->
          <div class="px-3.5 py-2.5 flex items-center justify-between hover:bg-slate-50/60 transition-colors">
            <div class="flex items-center gap-2.5">
              <span
                class="w-2 h-2 rounded-full flex-shrink-0"
                :class="pendingAction.kafka ? 'bg-amber-400 ring-2 ring-amber-200 animate-ping' : (infra.kafka ? 'bg-emerald-500 ring-2 ring-emerald-100 animate-pulse' : 'bg-slate-300')"
              ></span>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">Apache Kafka (KRaft)</span>
                  <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 border border-slate-200">Port {{ ports.kafka }}</span>
                  <span
                    class="text-[9px] font-mono font-medium"
                    :class="pendingAction.kafka ? 'text-amber-600' : (infra.kafka ? 'text-emerald-600' : 'text-slate-400')"
                  >
                    {{ pendingAction.kafka === 'start' ? 'STARTING...' : pendingAction.kafka === 'stop' ? 'STOPPING...' : (infra.kafka ? 'ONLINE' : 'OFFLINE') }}
                  </span>
                  <!-- 토픽 생성 버튼을 타이틀 라인으로 배치 -->
                  <button
                    @click="createTopics"
                    :disabled="!infra.kafka || isExecuting"
                    class="px-1.5 py-0.2 rounded text-[10px] font-medium bg-indigo-50 hover:bg-indigo-100 text-indigo-700 border border-indigo-200/80 transition-colors cursor-pointer disabled:opacity-40"
                    title="4대 핵심 토픽 자동 생성"
                  >
                    토픽 생성
                  </button>
                </div>
                <p class="text-[11px] text-slate-500">OCPP 이벤트 스트리밍 및 메시지 버퍼</p>
              </div>
            </div>

            <!-- 통일된 2단 액션 버튼군 (로그 & 시작/종료 동일 규격 w-16) -->
            <div class="flex items-center gap-1.5 flex-shrink-0 w-36 justify-end">
              <button
                @click="openLogWindow('kafka')"
                class="w-14 py-1.5 rounded-lg text-center text-xs font-medium bg-slate-50 hover:bg-slate-100 text-slate-600 hover:text-slate-900 border border-slate-200 cursor-pointer transition-colors shadow-2xs"
                title="Kafka 로그 독립 창 열기"
              >
                로그
              </button>
              <button
                v-if="!infra.kafka"
                @click="control('start', 'kafka', true)"
                :disabled="isExecuting || pendingAction.kafka !== null"
                class="w-16 py-1.5 rounded-lg text-center text-xs font-medium bg-emerald-600 hover:bg-emerald-700 text-white shadow-2xs cursor-pointer transition-all disabled:opacity-50 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.kafka === 'start'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.kafka === 'start' ? '기동중' : '시작' }}</span>
              </button>
              <button
                v-else
                @click="control('stop', 'kafka')"
                :disabled="isExecuting || pendingAction.kafka !== null"
                class="w-16 py-1.5 rounded-lg text-center text-xs font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-300 shadow-2xs cursor-pointer transition-all disabled:opacity-50 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.kafka === 'stop'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.kafka === 'stop' ? '종료중' : '종료' }}</span>
              </button>
            </div>
          </div>
        </div>

        <!-- B. 코어 백엔드 서비스 그룹 -->
        <div class="px-3.5 py-1.5 bg-slate-50/70 border-t border-b border-slate-200/80 flex items-center justify-between">
          <div class="flex items-center gap-2">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
            <span class="text-[11px] font-semibold uppercase tracking-wider text-slate-700">코어 백엔드 엔진</span>
            <span
              class="px-1.5 py-0.2 rounded-full text-[9px] font-mono font-medium border"
              :class="coreRunningCount > 0 ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-100 text-slate-500 border-slate-200'"
            >
              {{ coreRunningCount }}/3
            </span>
          </div>

          <!-- 코어 엔진 헤더 전용 제어 버튼군: 시작 / 종료 / 재기동 / 프로세스 -->
          <div class="flex items-center gap-1">
            <button
              @click="startServices"
              :disabled="isExecuting || (infra.connectWs && infra.connectSync && infra.controlApi)"
              class="px-2 py-0.5 rounded text-[10px] font-medium bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 transition-colors cursor-pointer disabled:opacity-40 flex items-center gap-1"
              title="코어 백엔드 엔진(WS, Sync, API) 순차 시작"
            >
              <svg v-if="pendingAction.services === 'start'" class="w-2.5 h-2.5 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
              <span>{{ pendingAction.services === 'start' ? '기동중...' : '시작' }}</span>
            </button>
            <button
              @click="stopServices"
              :disabled="isExecuting || (!infra.connectWs && !infra.connectSync && !infra.controlApi)"
              class="px-2 py-0.5 rounded text-[10px] font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-200 transition-colors cursor-pointer disabled:opacity-40 flex items-center gap-1"
              title="코어 백엔드 엔진(WS, Sync, API) 일괄 종료"
            >
              <svg v-if="pendingAction.services === 'stop'" class="w-2.5 h-2.5 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
              <span>{{ pendingAction.services === 'stop' ? '종료중...' : '종료' }}</span>
            </button>
            <button
              @click="restartServices"
              :disabled="isExecuting"
              class="px-2 py-0.5 rounded text-[10px] font-medium bg-white hover:bg-slate-100 text-slate-700 border border-slate-200 transition-colors cursor-pointer disabled:opacity-40 flex items-center gap-1"
              title="인프라(MySQL, Kafka)는 유지하고 백엔드 엔진만 순차 재기동"
            >
              <svg v-if="pendingAction.services === 'restart'" class="w-2.5 h-2.5 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
              <span>{{ pendingAction.services === 'restart' ? '재기동중...' : '재기동' }}</span>
            </button>
            <button
              @click="openProcessStatus('core')"
              class="px-1.5 py-0.5 rounded text-[10px] font-medium bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 transition-colors cursor-pointer flex items-center gap-0.5"
              title="코어 엔진 포트(18080, 18081, 18082) 및 Java PID 진단"
            >
              <svg class="w-2.5 h-2.5 text-emerald-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
              <span>프로세스</span>
            </button>
          </div>
        </div>

        <div class="divide-y divide-slate-100">
          <!-- 3) elvis-connect-ws -->
          <div class="px-3.5 py-2.5 flex items-center justify-between hover:bg-slate-50/60 transition-colors">
            <div class="flex items-center gap-2.5">
              <span
                class="w-2 h-2 rounded-full flex-shrink-0"
                :class="pendingAction.ws ? 'bg-amber-400 ring-2 ring-amber-200 animate-ping' : (infra.connectWs ? 'bg-emerald-500 ring-2 ring-emerald-100 animate-pulse' : 'bg-slate-300')"
              ></span>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">elvis-connect-ws</span>
                  <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 border border-slate-200">Port {{ ports.ws }}</span>
                  <span
                    class="text-[9px] font-mono font-medium"
                    :class="pendingAction.ws ? 'text-amber-600' : (infra.connectWs ? 'text-emerald-600' : 'text-slate-400')"
                  >
                    {{ pendingAction.ws === 'start' ? 'STARTING...' : pendingAction.ws === 'stop' ? 'STOPPING...' : (infra.connectWs ? 'ONLINE' : 'OFFLINE') }}
                  </span>
                </div>
                <p class="text-[11px] text-slate-500">충전기 단말 WebSocket 커넥션 수용 게이트웨이</p>
              </div>
            </div>

            <!-- 통일된 2단 액션 버튼군 (로그 & 시작/종료 동일 규격 w-16) -->
            <div class="flex items-center gap-1.5 flex-shrink-0 w-36 justify-end">
              <button
                @click="openLogWindow('ws')"
                class="w-14 py-1.5 rounded-lg text-center text-xs font-medium bg-slate-50 hover:bg-slate-100 text-slate-600 hover:text-slate-900 border border-slate-200 cursor-pointer transition-colors shadow-2xs"
                title="WS 게이트웨이 로그 독립 창 열기"
              >
                로그
              </button>
              <button
                v-if="!infra.connectWs"
                @click="control('start', 'ws', true)"
                :disabled="isExecuting || pendingAction.ws !== null"
                class="w-16 py-1.5 rounded-lg text-center text-xs font-medium bg-emerald-600 hover:bg-emerald-700 text-white shadow-2xs cursor-pointer transition-all disabled:opacity-50 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.ws === 'start'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.ws === 'start' ? '기동중' : '시작' }}</span>
              </button>
              <button
                v-else
                @click="control('stop', 'ws')"
                :disabled="isExecuting || pendingAction.ws !== null"
                class="w-16 py-1.5 rounded-lg text-center text-xs font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-300 shadow-2xs cursor-pointer transition-all disabled:opacity-50 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.ws === 'stop'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.ws === 'stop' ? '종료중' : '종료' }}</span>
              </button>
            </div>
          </div>

          <!-- 4) elvis-connect-sync -->
          <div class="px-3.5 py-2.5 flex items-center justify-between hover:bg-slate-50/60 transition-colors">
            <div class="flex items-center gap-2.5">
              <span
                class="w-2 h-2 rounded-full flex-shrink-0"
                :class="pendingAction.sync ? 'bg-amber-400 ring-2 ring-amber-200 animate-ping' : (infra.connectSync ? 'bg-emerald-500 ring-2 ring-emerald-100 animate-pulse' : 'bg-slate-300')"
              ></span>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">elvis-connect-sync</span>
                  <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 border border-slate-200">Port {{ ports.sync }}</span>
                  <span
                    class="text-[9px] font-mono font-medium"
                    :class="pendingAction.sync ? 'text-amber-600' : (infra.connectSync ? 'text-emerald-600' : 'text-slate-400')"
                  >
                    {{ pendingAction.sync === 'start' ? 'STARTING...' : pendingAction.sync === 'stop' ? 'STOPPING...' : (infra.connectSync ? 'ONLINE' : 'OFFLINE') }}
                  </span>
                </div>
                <p class="text-[11px] text-slate-500">Kafka 이벤트 컨슘, 상태 UPDATE 및 원장 DB 적재</p>
              </div>
            </div>

            <!-- 통일된 2단 액션 버튼군 (로그 & 시작/종료 동일 규격 w-16) -->
            <div class="flex items-center gap-1.5 flex-shrink-0 w-36 justify-end">
              <button
                @click="openLogWindow('sync')"
                class="w-14 py-1.5 rounded-lg text-center text-xs font-medium bg-slate-50 hover:bg-slate-100 text-slate-600 hover:text-slate-900 border border-slate-200 cursor-pointer transition-colors shadow-2xs"
                title="Sync 엔진 로그 독립 창 열기"
              >
                로그
              </button>
              <button
                v-if="!infra.connectSync"
                @click="control('start', 'sync', true)"
                :disabled="isExecuting || pendingAction.sync !== null"
                class="w-16 py-1.5 rounded-lg text-center text-xs font-medium bg-emerald-600 hover:bg-emerald-700 text-white shadow-2xs cursor-pointer transition-all disabled:opacity-50 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.sync === 'start'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.sync === 'start' ? '기동중' : '시작' }}</span>
              </button>
              <button
                v-else
                @click="control('stop', 'sync')"
                :disabled="isExecuting || pendingAction.sync !== null"
                class="w-16 py-1.5 rounded-lg text-center text-xs font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-300 shadow-2xs cursor-pointer transition-all disabled:opacity-50 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.sync === 'stop'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.sync === 'stop' ? '종료중' : '종료' }}</span>
              </button>
            </div>
          </div>

          <!-- 5) elvis-control-api -->
          <div class="px-3.5 py-2.5 flex items-center justify-between hover:bg-slate-50/60 transition-colors">
            <div class="flex items-center gap-2.5">
              <span
                class="w-2 h-2 rounded-full flex-shrink-0"
                :class="pendingAction.api ? 'bg-amber-400 ring-2 ring-amber-200 animate-ping' : (infra.controlApi ? 'bg-emerald-500 ring-2 ring-emerald-100 animate-pulse' : 'bg-slate-300')"
              ></span>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">elvis-control-api</span>
                  <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 border border-slate-200">Port {{ ports.api }}</span>
                  <span
                    class="text-[9px] font-mono font-medium"
                    :class="pendingAction.api ? 'text-amber-600' : (infra.controlApi ? 'text-emerald-600' : 'text-slate-400')"
                  >
                    {{ pendingAction.api === 'start' ? 'STARTING...' : pendingAction.api === 'stop' ? 'STOPPING...' : (infra.controlApi ? 'ONLINE' : 'OFFLINE') }}
                  </span>
                </div>
                <p class="text-[11px] text-slate-500">관제 어드민 REST API 서버 & 실시간 웹소켓 푸시</p>
              </div>
            </div>

            <!-- 통일된 2단 액션 버튼군 (로그 & 시작/종료 동일 규격 w-16) -->
            <div class="flex items-center gap-1.5 flex-shrink-0 w-36 justify-end">
              <button
                @click="openLogWindow('api')"
                class="w-14 py-1.5 rounded-lg text-center text-xs font-medium bg-slate-50 hover:bg-slate-100 text-slate-600 hover:text-slate-900 border border-slate-200 cursor-pointer transition-colors shadow-2xs"
                title="Control API 로그 독립 창 열기"
              >
                로그
              </button>
              <button
                v-if="!infra.controlApi"
                @click="control('start', 'api', true)"
                :disabled="isExecuting || pendingAction.api !== null"
                class="w-16 py-1.5 rounded-lg text-center text-xs font-medium bg-emerald-600 hover:bg-emerald-700 text-white shadow-2xs cursor-pointer transition-all disabled:opacity-50 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.api === 'start'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.api === 'start' ? '기동중' : '시작' }}</span>
              </button>
              <button
                v-else
                @click="control('stop', 'api')"
                :disabled="isExecuting || pendingAction.api !== null"
                class="w-16 py-1.5 rounded-lg text-center text-xs font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-300 shadow-2xs cursor-pointer transition-all disabled:opacity-50 flex items-center justify-center gap-1"
              >
                <svg v-if="pendingAction.api === 'stop'" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg>
                <span>{{ pendingAction.api === 'stop' ? '종료중' : '종료' }}</span>
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
            <div class="font-bold text-[11px]">{{ toast.title }}</div>
            <div class="text-[10px] opacity-90">{{ toast.message }}</div>
          </div>
        </div>
      </transition-group>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { invoke } from '@tauri-apps/api/core'
import { listen, type UnlistenFn } from '@tauri-apps/api/event'
import { getCurrentWebviewWindow } from '@tauri-apps/api/webviewWindow'

interface Toast {
  id: number
  title: string
  message: string
  type: 'success' | 'error' | 'info'
}

const toasts = ref<Toast[]>([])
let toastSeq = 0
function addToast(title: string, message: string, type: 'success' | 'error' | 'info' = 'info') {
  const id = ++toastSeq
  toasts.value.push({ id, title, message, type })
  setTimeout(() => {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }, 3500)
}

type LogServiceType = 'mysql' | 'kafka' | 'ws' | 'sync' | 'api'

// 1. 현재 창의 역할 판별 (URL 쿼리스트링 및 Tauri 창 라벨 상호 검증)
const urlParams = new URLSearchParams(window.location.search)
let detectedView = urlParams.get('view') || (window as any).__INITIAL_VIEW__ || 'main'
let detectedService = (urlParams.get('service') || (window as any).__INITIAL_SERVICE__ || 'mysql') as LogServiceType

try {
  const currentWin = getCurrentWebviewWindow()
  if (currentWin && currentWin.label && currentWin.label.startsWith('log_')) {
    detectedView = 'logs'
    const labelSvc = currentWin.label.replace('log_', '') as LogServiceType
    if (['mysql', 'kafka', 'ws', 'sync', 'api'].includes(labelSvc)) {
      detectedService = labelSvc
    }
  }
} catch (_) {}

const currentView = ref(detectedView)
const selectedLogService = ref<LogServiceType>(detectedService)

// 5대 핵심 서비스 점유 상태
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
  ws: 18080,
  sync: 18081,
  api: 18082
})

const isChecking = ref(false)
const isExecuting = ref(false)

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

// 가동 중인 서비스 수 계산 (5대 컴포넌트 기준)
const runningCount = computed(() => {
  return [
    infra.mysql,
    infra.kafka,
    infra.connectWs,
    infra.connectSync,
    infra.controlApi
  ].filter(Boolean).length
})

// 인프라 미들웨어 가동 수 (2개 기준)
const infraRunningCount = computed(() => {
  return [infra.mysql, infra.kafka].filter(Boolean).length
})

// 코어 백엔드 엔진 가동 수 (3개 기준)
const coreRunningCount = computed(() => {
  return [infra.connectWs, infra.connectSync, infra.controlApi].filter(Boolean).length
})

const isAllRunning = computed(() => {
  return infra.mysql && infra.kafka && infra.connectWs && infra.connectSync && infra.controlApi
})
const logLines = ref<string[]>([])
const searchKeyword = ref('')
const logFileSize = ref(0)
const logLastModified = ref<string | null>(null)
const isFetchingLogs = ref(false)
const autoRefreshLogs = ref(true)
const autoScrollLogs = ref(true)
const logContainerRef = ref<HTMLElement | null>(null)
let statusPollingTimer: any = null
let logPollingTimer: any = null

const currentServicePort = computed(() => {
  return ports[selectedLogService.value] || 0
})

// 검색어 필터링된 로그 라인
const filteredLogLines = computed(() => {
  if (!searchKeyword.value.trim()) return logLines.value
  const kw = searchKeyword.value.toLowerCase()
  return logLines.value.filter(line => line.toLowerCase().includes(kw))
})

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
    const res = await fetch('/api/infra/status')
    const data = await res.json()
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
    const bgParam = action === 'start' ? `&background=${isBackground}` : ''
    const res = await fetch(`/api/infra/${action}?service=${service}${bgParam}`, { method: 'POST' })
    const data = await res.json()
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
    addToast('통신 오류', err.message, 'error')
  } finally {
    pendingAction[service] = null
    isExecuting.value = false
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
    const res = await fetch('/api/infra/start?service=all-infra&background=true', { method: 'POST' })
    const data = await res.json()
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
    addToast('기동 실패', err.message, 'error')
  } finally {
    pendingAction.infra = null
    pendingAction.mysql = null
    pendingAction.kafka = null
    isExecuting.value = false
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
    const res = await fetch('/api/infra/stop?service=all-infra', { method: 'POST' })
    const data = await res.json()
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
    addToast('중지 실패', err.message, 'error')
  } finally {
    pendingAction.infra = null
    pendingAction.mysql = null
    pendingAction.kafka = null
    isExecuting.value = false
    await checkStatus()
  }
}

// 3-2. 인프라만 재기동 (완전히 재가동될 때까지 대기)
async function restartInfra() {
  isExecuting.value = true
  pendingAction.infra = 'restart'
  pendingAction.mysql = 'start'
  pendingAction.kafka = 'start'
  try {
    const res = await fetch('/api/infra/restart-infra', { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('인프라 재기동', '기존 인프라 정리 후 재기동 중입니다...', 'info')
      await new Promise((r) => setTimeout(r, 2000))
      const ok = await waitForCondition(() => infra.mysql && infra.kafka, 50000, 800)
      if (ok) {
        addToast('인프라 재기동 완료', 'MySQL 및 Kafka가 정상 재기동되었습니다.', 'success')
      }
    } else {
      addToast('재기동 오류', data.message, 'error')
    }
  } catch (err: any) {
    addToast('재기동 오류', err.message, 'error')
  } finally {
    pendingAction.infra = null
    pendingAction.mysql = null
    pendingAction.kafka = null
    isExecuting.value = false
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
    const res = await fetch('/api/infra/start?service=all-services&background=true', { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('코어 서비스 기동', 'WS -> Sync -> API 순차 기동 중입니다...', 'info')
      const ok = await waitForCondition(() => infra.connectWs && infra.connectSync && infra.controlApi, 60000, 1000)
      if (ok) {
        addToast('코어 서비스 기동 완료', '3대 코어 백엔드 엔진이 모두 정상 기동되었습니다.', 'success')
      }
    } else {
      addToast('기동 실패', data.message, 'error')
    }
  } catch (err: any) {
    addToast('기동 실패', err.message, 'error')
  } finally {
    pendingAction.services = null
    pendingAction.ws = null
    pendingAction.sync = null
    pendingAction.api = null
    isExecuting.value = false
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
    const res = await fetch('/api/infra/stop?service=all-services', { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('코어 서비스 중지', '3대 코어 백엔드 엔진 중지 중입니다...', 'info')
      const ok = await waitForCondition(() => !infra.connectWs && !infra.connectSync && !infra.controlApi, 15000, 500)
      if (ok) {
        addToast('코어 서비스 중지 완료', '3대 코어 백엔드 엔진이 모두 완전히 중지되었습니다.', 'success')
      }
    } else {
      addToast('중지 실패', data.message, 'error')
    }
  } catch (err: any) {
    addToast('중지 실패', err.message, 'error')
  } finally {
    pendingAction.services = null
    pendingAction.ws = null
    pendingAction.sync = null
    pendingAction.api = null
    isExecuting.value = false
    await checkStatus()
  }
}

// 4-2. 코어 서비스만 재기동 (WS, Sync, API 3대가 모두 완전히 재가동될 때까지 대기)
async function restartServices() {
  isExecuting.value = true
  pendingAction.services = 'restart'
  pendingAction.ws = 'start'
  pendingAction.sync = 'start'
  pendingAction.api = 'start'
  try {
    const res = await fetch('/api/infra/restart-services', { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('엔진 재기동', '백엔드 엔진(WS, Sync, API) 순차 재기동 중입니다...', 'info')
      await new Promise((r) => setTimeout(r, 2000))
      const ok = await waitForCondition(() => infra.connectWs && infra.connectSync && infra.controlApi, 60000, 1000)
      if (ok) {
        addToast('엔진 재기동 완료', '백엔드 엔진이 모두 정상 재기동되었습니다.', 'success')
      }
    } else {
      addToast('재기동 오류', data.message, 'error')
    }
  } catch (err: any) {
    addToast('재기동 오류', err.message, 'error')
  } finally {
    pendingAction.services = null
    pendingAction.ws = null
    pendingAction.sync = null
    pendingAction.api = null
    isExecuting.value = false
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
    const res = await fetch(`/api/infra/start?service=all&background=${isBackground}`, { method: 'POST' })
    const data = await res.json()
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
    addToast('기동 실패', err.message, 'error')
  } finally {
    pendingAction.all = null
    pendingAction.mysql = null
    pendingAction.kafka = null
    pendingAction.ws = null
    pendingAction.sync = null
    pendingAction.api = null
    isExecuting.value = false
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
    const res = await fetch('/api/infra/stop?service=all', { method: 'POST' })
    const data = await res.json()
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
    addToast('중지 실패', err.message, 'error')
  } finally {
    pendingAction.all = null
    pendingAction.mysql = null
    pendingAction.kafka = null
    pendingAction.ws = null
    pendingAction.sync = null
    pendingAction.api = null
    isExecuting.value = false
    await checkStatus()
  }
}

// 7. 전체 백그라운드 전환 재기동 (5대 모두 다시 뜰 때까지 대기)
async function restartBackground() {
  isExecuting.value = true
  pendingAction.all = 'restart'
  pendingAction.mysql = 'start'
  pendingAction.kafka = 'start'
  pendingAction.ws = 'start'
  pendingAction.sync = 'start'
  pendingAction.api = 'start'
  try {
    const res = await fetch('/api/infra/restart-background', { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('전체 재기동', '기존 프로세스 정리 후 전체 서비스가 순차 재기동됩니다...', 'info')
      await new Promise((r) => setTimeout(r, 2500))
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
    addToast('재기동 오류', err.message, 'error')
  } finally {
    pendingAction.all = null
    pendingAction.mysql = null
    pendingAction.kafka = null
    pendingAction.ws = null
    pendingAction.sync = null
    pendingAction.api = null
    isExecuting.value = false
    await checkStatus()
  }
}

// 8. 프로세스 및 포트 점유 실시간 진단 콘솔 열기 (all | infra | core)
async function openProcessStatus(target: string = 'all') {
  const targetLabel = target === 'infra' ? '인프라' : target === 'core' ? '코어 엔진' : '전체'
  try {
    // 1) Tauri 데스크톱 환경: Rust 커맨드로 전용 진단 터미널 기동
    await invoke('open_process_status_window', { target })
    addToast('프로세스 진단', `[${targetLabel}] 프로세스 및 포트 점유 실시간 진단 터미널을 열었습니다.`, 'info')
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

// 9. 독립 실시간 콘솔 터미널 창 띄우기 (PowerShell 실시간 스트리밍: 서비스별 단일 창 보장)
async function openLogWindow(svc?: string) {
  const targetService = svc || 'mysql'
  try {
    // 1) Tauri 데스크톱 환경: Rust 커맨드로 PowerShell 콘솔 창 실행
    const msg = await invoke<string>('open_log_window', { service: targetService })
    addToast('로그 콘솔', msg, 'info')
    return
  } catch (_) {}

  // 2) 브라우저 / fallback 환경: Vite 백엔드 API로 PowerShell 콘솔 창 실행
  try {
    const res = await fetch('/api/infra/logs/open-terminal?service=' + targetService, { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('로그 콘솔', data.message, 'info')
    } else {
      addToast('터미널 기동 실패', data.message, 'error')
    }
  } catch (err: any) {
    addToast('오류 발생', err.message, 'error')
  }
}

// 10. 실시간 콘솔 로그 조회 (타임아웃 안전망 2.5초 적용)
async function fetchLogs() {
  if (isFetchingLogs.value) return
  isFetchingLogs.value = true
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), 2500)

  try {
    const res = await fetch(`/api/infra/logs?service=${selectedLogService.value}&tail=300`, {
      signal: controller.signal
    })
    clearTimeout(timeoutId)
    if (!res.ok) {
      console.warn('[Log-Fetch-HTTP-Warn]', res.status)
      return
    }
    const data = await res.json()
    if (data.success) {
      logLines.value = data.lines || []
      logFileSize.value = data.fileSize || 0
      logLastModified.value = data.lastModified
      if (autoScrollLogs.value) {
        nextTick(() => {
          if (logContainerRef.value) {
            logContainerRef.value.scrollTop = logContainerRef.value.scrollHeight
          }
        })
      }
    }
  } catch (e: any) {
    if (e.name !== 'AbortError') {
      console.warn('[Log-Fetch-Warn]', e.message)
    }
  } finally {
    clearTimeout(timeoutId)
    isFetchingLogs.value = false
  }
}

async function clearLogs() {
  try {
    const res = await fetch(`/api/infra/logs/clear?service=${selectedLogService.value}`, { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      logLines.value = []
      logFileSize.value = 0
      addToast('로그 초기화', `${selectedLogService.value.toUpperCase()} 로그를 비웠습니다.`, 'success')
    }
  } catch (err: any) {
    addToast('초기화 실패', err.message, 'error')
  }
}

function copyLogs() {
  const linesToCopy = filteredLogLines.value
  if (!linesToCopy.length) return
  navigator.clipboard.writeText(linesToCopy.join('\n')).then(() => {
    addToast('로그 복사', `${linesToCopy.length}줄이 클립보드에 복사되었습니다.`, 'success')
  }).catch(() => {
    addToast('복사 실패', '클립보드 접근 권한이 없습니다.', 'error')
  })
}

// 11. 스키마 적재
async function loadSchema() {
  isExecuting.value = true
  try {
    const res = await fetch('/api/infra/schema', { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('스키마 적재 완료', data.message, 'success')
    } else {
      addToast('적재 실패', data.message, 'error')
    }
  } catch (err: any) {
    addToast('통신 오류', err.message, 'error')
  } finally {
    isExecuting.value = false
  }
}

// 12. Kafka 토픽 생성
async function createTopics() {
  isExecuting.value = true
  try {
    const res = await fetch('/api/infra/topics', { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('토픽 생성 완료', data.message, 'success')
    } else {
      addToast('생성 실패', data.message, 'error')
    }
  } catch (err: any) {
    addToast('통신 오류', err.message, 'error')
  } finally {
    isExecuting.value = false
  }
}


function formatBytes(bytes: number) {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

function formatLogLine(line: string) {
  if (line.includes('[ERROR]') || line.includes('ERROR') || line.includes('Exception') || line.includes('fatal')) {
    return 'text-rose-400 font-semibold'
  }
  if (line.includes('[WARN]') || line.includes('Warning') || line.includes('[Warning]')) {
    return 'text-amber-400'
  }
  if (line.includes('[System]') || line.includes('[OK]') || line.includes('ready for connections') || line.includes('started') || line.includes('Started')) {
    return 'text-emerald-400'
  }
  if (line.includes('[INFO]') || line.includes('INFO')) {
    return 'text-sky-300'
  }
  return 'text-slate-300'
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
