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
              class="px-1.5 py-0.5 rounded text-[10px] font-medium bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 transition-colors cursor-pointer disabled:opacity-40"
              title="인프라(MySQL, Kafka) 시작"
            >
              시작
            </button>
            <button
              @click="stopInfra"
              :disabled="isExecuting || (!infra.mysql && !infra.kafka)"
              class="px-1.5 py-0.5 rounded text-[10px] font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-200 transition-colors cursor-pointer disabled:opacity-40"
              title="인프라(MySQL, Kafka) 종료"
            >
              종료
            </button>
            <button
              @click="restartInfra"
              :disabled="isExecuting"
              class="px-1.5 py-0.5 rounded text-[10px] font-medium bg-white hover:bg-slate-100 text-slate-700 border border-slate-200 transition-colors cursor-pointer disabled:opacity-40"
              title="인프라(MySQL, Kafka)만 순차 재기동"
            >
              재기동
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
              <span class="w-2 h-2 rounded-full flex-shrink-0" :class="infra.mysql ? 'bg-emerald-500 ring-2 ring-emerald-100 animate-pulse' : 'bg-slate-300'"></span>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">MySQL 9.71</span>
                  <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 border border-slate-200">Port {{ ports.mysql }}</span>
                  <span class="text-[9px] font-mono" :class="infra.mysql ? 'text-emerald-600 font-medium' : 'text-slate-400'">
                    {{ infra.mysql ? 'ONLINE' : 'OFFLINE' }}
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

            <!-- 통일된 2단 액션 버튼군 (로그 & 시작/종료 동일 규격 w-14) -->
            <div class="flex items-center gap-1.5 flex-shrink-0 w-32 justify-end">
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
                :disabled="isExecuting"
                class="w-14 py-1.5 rounded-lg text-center text-xs font-medium bg-emerald-600 hover:bg-emerald-700 text-white shadow-2xs cursor-pointer transition-all disabled:opacity-40"
              >
                시작
              </button>
              <button
                v-else
                @click="control('stop', 'mysql')"
                :disabled="isExecuting"
                class="w-14 py-1.5 rounded-lg text-center text-xs font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-300 shadow-2xs cursor-pointer transition-all disabled:opacity-40"
              >
                종료
              </button>
            </div>
          </div>

          <!-- 2) Kafka -->
          <div class="px-3.5 py-2.5 flex items-center justify-between hover:bg-slate-50/60 transition-colors">
            <div class="flex items-center gap-2.5">
              <span class="w-2 h-2 rounded-full flex-shrink-0" :class="infra.kafka ? 'bg-emerald-500 ring-2 ring-emerald-100 animate-pulse' : 'bg-slate-300'"></span>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">Apache Kafka (KRaft)</span>
                  <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 border border-slate-200">Port {{ ports.kafka }}</span>
                  <span class="text-[9px] font-mono" :class="infra.kafka ? 'text-emerald-600 font-medium' : 'text-slate-400'">
                    {{ infra.kafka ? 'ONLINE' : 'OFFLINE' }}
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

            <!-- 통일된 2단 액션 버튼군 (로그 & 시작/종료 동일 규격 w-14) -->
            <div class="flex items-center gap-1.5 flex-shrink-0 w-32 justify-end">
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
                :disabled="isExecuting"
                class="w-14 py-1.5 rounded-lg text-center text-xs font-medium bg-emerald-600 hover:bg-emerald-700 text-white shadow-2xs cursor-pointer transition-all disabled:opacity-40"
              >
                시작
              </button>
              <button
                v-else
                @click="control('stop', 'kafka')"
                :disabled="isExecuting"
                class="w-14 py-1.5 rounded-lg text-center text-xs font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-300 shadow-2xs cursor-pointer transition-all disabled:opacity-40"
              >
                종료
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
              class="px-1.5 py-0.5 rounded text-[10px] font-medium bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 transition-colors cursor-pointer disabled:opacity-40"
              title="코어 백엔드 엔진(WS, Sync, API) 순차 시작"
            >
              시작
            </button>
            <button
              @click="stopServices"
              :disabled="isExecuting || (!infra.connectWs && !infra.connectSync && !infra.controlApi)"
              class="px-1.5 py-0.5 rounded text-[10px] font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-200 transition-colors cursor-pointer disabled:opacity-40"
              title="코어 백엔드 엔진(WS, Sync, API) 일괄 종료"
            >
              종료
            </button>
            <button
              @click="restartServices"
              :disabled="isExecuting"
              class="px-1.5 py-0.5 rounded text-[10px] font-medium bg-white hover:bg-slate-100 text-slate-700 border border-slate-200 transition-colors cursor-pointer disabled:opacity-40"
              title="인프라(MySQL, Kafka)는 유지하고 백엔드 엔진만 순차 재기동"
            >
              재기동
            </button>
            <button
              @click="openProcessStatus('core')"
              class="px-1.5 py-0.5 rounded text-[10px] font-medium bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 transition-colors cursor-pointer flex items-center gap-0.5"
              title="코어 엔진 포트(18080, 8082, 18088) 및 Java PID 진단"
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
              <span class="w-2 h-2 rounded-full flex-shrink-0" :class="infra.connectWs ? 'bg-emerald-500 ring-2 ring-emerald-100 animate-pulse' : 'bg-slate-300'"></span>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">elvis-connect-ws</span>
                  <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 border border-slate-200">Port {{ ports.ws }}</span>
                  <span class="text-[9px] font-mono" :class="infra.connectWs ? 'text-emerald-600 font-medium' : 'text-slate-400'">
                    {{ infra.connectWs ? 'ONLINE' : 'OFFLINE' }}
                  </span>
                </div>
                <p class="text-[11px] text-slate-500">충전기 단말 WebSocket 커넥션 수용 게이트웨이</p>
              </div>
            </div>

            <!-- 통일된 2단 액션 버튼군 (로그 & 시작/종료 동일 규격 w-14) -->
            <div class="flex items-center gap-1.5 flex-shrink-0 w-32 justify-end">
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
                :disabled="isExecuting"
                class="w-14 py-1.5 rounded-lg text-center text-xs font-medium bg-emerald-600 hover:bg-emerald-700 text-white shadow-2xs cursor-pointer transition-all disabled:opacity-40"
              >
                시작
              </button>
              <button
                v-else
                @click="control('stop', 'ws')"
                :disabled="isExecuting"
                class="w-14 py-1.5 rounded-lg text-center text-xs font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-300 shadow-2xs cursor-pointer transition-all disabled:opacity-40"
              >
                종료
              </button>
            </div>
          </div>

          <!-- 4) elvis-connect-sync -->
          <div class="px-3.5 py-2.5 flex items-center justify-between hover:bg-slate-50/60 transition-colors">
            <div class="flex items-center gap-2.5">
              <span class="w-2 h-2 rounded-full flex-shrink-0" :class="infra.connectSync ? 'bg-emerald-500 ring-2 ring-emerald-100 animate-pulse' : 'bg-slate-300'"></span>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">elvis-connect-sync</span>
                  <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 border border-slate-200">Port {{ ports.sync }}</span>
                  <span class="text-[9px] font-mono" :class="infra.connectSync ? 'text-emerald-600 font-medium' : 'text-slate-400'">
                    {{ infra.connectSync ? 'ONLINE' : 'OFFLINE' }}
                  </span>
                </div>
                <p class="text-[11px] text-slate-500">Kafka 이벤트 컨슘, 상태 UPDATE 및 원장 DB 적재</p>
              </div>
            </div>

            <!-- 통일된 2단 액션 버튼군 (로그 & 시작/종료 동일 규격 w-14) -->
            <div class="flex items-center gap-1.5 flex-shrink-0 w-32 justify-end">
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
                :disabled="isExecuting"
                class="w-14 py-1.5 rounded-lg text-center text-xs font-medium bg-emerald-600 hover:bg-emerald-700 text-white shadow-2xs cursor-pointer transition-all disabled:opacity-40"
              >
                시작
              </button>
              <button
                v-else
                @click="control('stop', 'sync')"
                :disabled="isExecuting"
                class="w-14 py-1.5 rounded-lg text-center text-xs font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-300 shadow-2xs cursor-pointer transition-all disabled:opacity-40"
              >
                종료
              </button>
            </div>
          </div>

          <!-- 5) elvis-control-api -->
          <div class="px-3.5 py-2.5 flex items-center justify-between hover:bg-slate-50/60 transition-colors">
            <div class="flex items-center gap-2.5">
              <span class="w-2 h-2 rounded-full flex-shrink-0" :class="infra.controlApi ? 'bg-emerald-500 ring-2 ring-emerald-100 animate-pulse' : 'bg-slate-300'"></span>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">elvis-control-api</span>
                  <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 border border-slate-200">Port {{ ports.api }}</span>
                  <span class="text-[9px] font-mono" :class="infra.controlApi ? 'text-emerald-600 font-medium' : 'text-slate-400'">
                    {{ infra.controlApi ? 'ONLINE' : 'OFFLINE' }}
                  </span>
                </div>
                <p class="text-[11px] text-slate-500">관제 어드민 REST API 서버 & 실시간 웹소켓 푸시</p>
              </div>
            </div>

            <!-- 통일된 2단 액션 버튼군 (로그 & 시작/종료 동일 규격 w-14) -->
            <div class="flex items-center gap-1.5 flex-shrink-0 w-32 justify-end">
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
                :disabled="isExecuting"
                class="w-14 py-1.5 rounded-lg text-center text-xs font-medium bg-emerald-600 hover:bg-emerald-700 text-white shadow-2xs cursor-pointer transition-all disabled:opacity-40"
              >
                시작
              </button>
              <button
                v-else
                @click="control('stop', 'api')"
                :disabled="isExecuting"
                class="w-14 py-1.5 rounded-lg text-center text-xs font-medium bg-white hover:bg-rose-50 text-rose-600 border border-slate-200 hover:border-rose-300 shadow-2xs cursor-pointer transition-all disabled:opacity-40"
              >
                종료
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
  sync: 8082,
  api: 18088
})

const isChecking = ref(false)
const isExecuting = ref(false)

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
  } catch (e: any) {
    console.warn('[Status-Check-Error]', e.message)
  } finally {
    isChecking.value = false
  }
}

// 2. 서비스 단독 제어
async function control(action: 'start' | 'stop', service: string, isBackground = true) {
  isExecuting.value = true
  try {
    const bgParam = action === 'start' ? `&background=${isBackground}` : ''
    const res = await fetch(`/api/infra/${action}?service=${service}${bgParam}`, { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast(`${service.toUpperCase()} 제어`, data.message, 'success')
      setTimeout(checkStatus, 1500)
    } else {
      addToast('제어 실패', data.message, 'error')
    }
  } catch (err: any) {
    addToast('통신 오류', err.message, 'error')
  } finally {
    isExecuting.value = false
  }
}

// 3. 인프라만 시작 (MySQL & Kafka)
async function startInfra() {
  isExecuting.value = true
  try {
    const res = await fetch('/api/infra/start?service=all-infra&background=true', { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('인프라 기동', data.message, 'success')
      setTimeout(checkStatus, 2500)
    }
  } catch (err: any) {
    addToast('기동 실패', err.message, 'error')
  } finally {
    isExecuting.value = false
  }
}

// 3-1. 인프라만 중지 (MySQL & Kafka)
async function stopInfra() {
  isExecuting.value = true
  try {
    const res = await fetch('/api/infra/stop?service=all-infra', { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('인프라 중지', data.message, 'success')
      setTimeout(checkStatus, 1500)
    }
  } catch (err: any) {
    addToast('중지 실패', err.message, 'error')
  } finally {
    isExecuting.value = false
  }
}

// 3-2. 인프라만 재기동 (MySQL & Kafka)
async function restartInfra() {
  isExecuting.value = true
  try {
    const res = await fetch('/api/infra/restart-infra', { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('인프라 재기동', data.message, 'success')
      setTimeout(checkStatus, 3000)
    }
  } catch (err: any) {
    addToast('재기동 오류', err.message, 'error')
  } finally {
    isExecuting.value = false
  }
}

// 4. 코어 서비스만 시작 (WS -> Sync -> API)
async function startServices() {
  isExecuting.value = true
  try {
    const res = await fetch('/api/infra/start?service=all-services&background=true', { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('코어 서비스 기동', data.message, 'success')
      setTimeout(checkStatus, 2500)
    }
  } catch (err: any) {
    addToast('기동 실패', err.message, 'error')
  } finally {
    isExecuting.value = false
  }
}

// 4-1. 코어 서비스만 중지
async function stopServices() {
  isExecuting.value = true
  try {
    const res = await fetch('/api/infra/stop?service=all-services', { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('코어 서비스 중지', data.message, 'success')
      setTimeout(checkStatus, 1500)
    }
  } catch (err: any) {
    addToast('중지 실패', err.message, 'error')
  } finally {
    isExecuting.value = false
  }
}

// 4-2. 코어 서비스만 재기동 (WS, Sync, API)
async function restartServices() {
  isExecuting.value = true
  try {
    const res = await fetch('/api/infra/restart-services', { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('엔진 재기동', data.message, 'success')
      setTimeout(checkStatus, 3000)
    }
  } catch (err: any) {
    addToast('재기동 오류', err.message, 'error')
  } finally {
    isExecuting.value = false
  }
}

// 5. 전체 기동 (인프라 + 코어 서비스 일괄 순차 부팅)
async function startAll(isBackground = true) {
  isExecuting.value = true
  try {
    const res = await fetch(`/api/infra/start?service=all&background=${isBackground}`, { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('전체 기동', data.message, 'success')
      setTimeout(checkStatus, 3000)
    }
  } catch (err: any) {
    addToast('기동 실패', err.message, 'error')
  } finally {
    isExecuting.value = false
  }
}

// 6. 전체 중지
async function stopAll() {
  isExecuting.value = true
  try {
    const res = await fetch('/api/infra/stop?service=all', { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('전체 중지', data.message, 'success')
      setTimeout(checkStatus, 1500)
    }
  } catch (err: any) {
    addToast('중지 실패', err.message, 'error')
  } finally {
    isExecuting.value = false
  }
}

// 7. 전체 백그라운드 전환 재기동
async function restartBackground() {
  isExecuting.value = true
  try {
    const res = await fetch('/api/infra/restart-background', { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('전체 재기동', data.message, 'success')
      setTimeout(checkStatus, 3000)
    }
  } catch (err: any) {
    addToast('재기동 오류', err.message, 'error')
  } finally {
    isExecuting.value = false
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

// 9. 독립 실시간 콘솔 터미널 창 띄우기 (PowerShell 실시간 스트리밍)
async function openLogWindow(svc?: string) {
  const targetService = svc || 'mysql'
  try {
    // 1) Tauri 데스크톱 환경: Rust 커맨드로 PowerShell 콘솔 창 실행
    await invoke('open_log_window', { service: targetService })
    return
  } catch (_) {}

  // 2) 브라우저 / fallback 환경: Vite 백엔드 API로 PowerShell 콘솔 창 실행
  try {
    const res = await fetch('/api/infra/logs/open-terminal?service=' + targetService, { method: 'POST' })
    const data = await res.json()
    if (!data.success) {
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

onMounted(() => {
  document.title = 'ELVIS Launcher'
  checkStatus()
  statusPollingTimer = setInterval(checkStatus, 3000)
})

onUnmounted(() => {
  if (statusPollingTimer) clearInterval(statusPollingTimer)
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
