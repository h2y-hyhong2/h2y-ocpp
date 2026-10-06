<template>
  <div class="min-h-screen flex flex-col bg-slate-50 text-slate-800 font-sans">
    <!-- 1. GNB 헤더 (라이트 모드) -->
    <header class="bg-white border-b border-slate-200 sticky top-0 z-50 px-6 py-3 flex items-center justify-between shadow-xs">
      <div class="flex items-center gap-3">
        <div class="w-9 h-9 rounded-xl bg-gradient-to-tr from-[#0A1E5A] to-[#0284C7] flex items-center justify-center text-lg text-white shadow-sm">
          🚀
        </div>
        <div>
          <div class="flex items-center gap-2">
            <h1 class="font-black text-base tracking-tight text-slate-900 flex items-center gap-1.5">
              <span>ELVIS Launcher</span>
              <span class="text-[10px] font-mono font-bold px-2 py-0.5 rounded-full bg-sky-100 text-sky-700 border border-sky-200">PORTABLE</span>
            </h1>
          </div>
          <p class="text-xs text-slate-500">인프라 미들웨어(MySQL 9.71 & Kafka KRaft) 시스템 트레이 상주 관리자</p>
        </div>
      </div>

      <!-- 우측 종합 상태 & 바로가기 -->
      <div class="flex items-center gap-2.5">
        <!-- 전체 헬스 상태 -->
        <div
          class="px-3 py-1.5 rounded-xl border flex items-center gap-2 text-xs font-bold transition-all shadow-xs"
          :class="(infra.mysql && infra.kafka) ? 'bg-emerald-50 border-emerald-200 text-emerald-700' : 'bg-amber-50 border-amber-200 text-amber-700'"
        >
          <span class="w-2 h-2 rounded-full" :class="(infra.mysql && infra.kafka) ? 'bg-emerald-500 animate-pulse' : 'bg-amber-500'"></span>
          <span>{{ (infra.mysql && infra.kafka) ? '미들웨어 정상 가동 중' : '미들웨어 시작 필요' }}</span>
        </div>

        <!-- 관제 UI 바로가기 -->
        <a
          href="http://localhost:1420"
          target="_blank"
          class="px-3.5 py-1.5 rounded-xl bg-[#0A1E5A] hover:bg-[#071644] text-white text-xs font-bold transition-all flex items-center gap-1.5 shadow-sm shadow-slate-300"
          title="포트 1420에서 실행 중인 CSMS 관제 UI 열기"
        >
          <span>📊</span>
          <span>관제 UI (:1420)</span>
          <svg class="w-3 h-3 opacity-70" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"></path></svg>
        </a>

        <!-- 새로고침 버튼 -->
        <button
          @click="checkStatus"
          :disabled="isChecking"
          class="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 transition-all text-xs cursor-pointer border border-slate-200"
          title="상태 새로고침"
        >
          <svg class="w-4 h-4 text-slate-600" :class="{ 'animate-spin': isChecking }" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
        </button>
      </div>
    </header>

    <!-- 2. 메인 컨텐츠 영역 (심플 1화면 레이아웃) -->
    <main class="flex-1 max-w-6xl w-full mx-auto p-5 space-y-4">
      <!-- 2-1. 상단 원클릭 빠른 액션 툴바 -->
      <div class="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div class="flex items-center gap-2">
          <span class="text-xs font-bold text-slate-700">원클릭 일괄 제어:</span>
          <span class="text-xs text-slate-500">창을 닫아도 백그라운드에서 계속 유지됩니다.</span>
        </div>

        <div class="flex items-center gap-2 flex-wrap">
          <button
            @click="startAll(true)"
            :disabled="isExecuting || (infra.mysql && infra.kafka)"
            class="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-extrabold shadow-sm transition-all flex items-center gap-1.5 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
          >
            <span>▶</span>
            <span>전체 시작</span>
          </button>

          <button
            @click="restartBackground"
            :disabled="isExecuting"
            class="px-3.5 py-2 rounded-xl bg-sky-50 hover:bg-sky-100 text-sky-700 border border-sky-200 text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
            title="기존 프로세스를 정리하고 백그라운드로 안전하게 재기동합니다"
          >
            <span>🔄</span>
            <span>재기동 (로그 연동)</span>
          </button>

          <button
            @click="stopAll"
            :disabled="isExecuting || (!infra.mysql && !infra.kafka)"
            class="px-3.5 py-2 rounded-xl bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
          >
            <span>⏹</span>
            <span>전체 중지</span>
          </button>
        </div>
      </div>

      <!-- 2-2. 2대 핵심 미들웨어 카드 (MySQL & Kafka 나란히 배치) -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <!-- 1) MySQL 9.71 카드 -->
        <div class="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs flex flex-col justify-between hover:shadow-sm transition-shadow">
          <div>
            <div class="flex items-start justify-between">
              <div class="flex items-center gap-3">
                <div class="w-11 h-11 rounded-xl bg-sky-50 border border-sky-200 flex items-center justify-center text-2xl">
                  🐬
                </div>
                <div>
                  <div class="flex items-center gap-2">
                    <h3 class="font-bold text-sm text-slate-900">MySQL 9.71 중앙 DB</h3>
                    <span class="text-[11px] font-mono px-2 py-0.5 rounded-md bg-slate-100 text-slate-600 font-semibold">Port {{ ports.mysql }}</span>
                  </div>
                  <p class="text-xs text-slate-500 mt-0.5">충전 자산, 실시간 상태 및 과금 원장(CDR) 보관</p>
                </div>
              </div>

              <!-- 상태 배지 -->
              <span
                class="px-2.5 py-1 rounded-full text-[11px] font-mono font-bold flex items-center gap-1.5 border"
                :class="infra.mysql ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-100 text-slate-500 border-slate-200'"
              >
                <span class="w-1.5 h-1.5 rounded-full" :class="infra.mysql ? 'bg-emerald-500 animate-pulse' : 'bg-slate-400'"></span>
                {{ infra.mysql ? 'ONLINE' : 'OFFLINE' }}
              </span>
            </div>
          </div>

          <!-- 하단 컨트롤 버튼 군 -->
          <div class="mt-5 pt-4 border-t border-slate-100 flex items-center gap-2">
            <button
              @click="control('start', 'mysql', true)"
              :disabled="infra.mysql || isExecuting"
              class="flex-1 py-2 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1.5 shadow-xs cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
              :class="infra.mysql ? 'bg-slate-100 text-slate-400' : 'bg-emerald-600 hover:bg-emerald-500 text-white'"
            >
              <span>▶</span>
              <span>시작</span>
            </button>

            <button
              @click="control('stop', 'mysql')"
              :disabled="!infra.mysql || isExecuting"
              class="px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
              :class="!infra.mysql ? 'bg-slate-100 text-slate-400' : 'bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200'"
            >
              <span>⏹</span>
              <span>중지</span>
            </button>

            <button
              @click="loadSchema"
              :disabled="!infra.mysql || isExecuting"
              class="px-3 py-2 rounded-xl text-xs font-semibold bg-slate-50 hover:bg-slate-100 text-slate-700 border border-slate-200 transition-all flex items-center gap-1 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
              title="초기 DDL 스키마 및 시드 데이터를 MySQL에 적재합니다"
            >
              <span>🗄️</span>
              <span>DDL 적재</span>
            </button>
          </div>
        </div>

        <!-- 2) Apache Kafka KRaft 카드 -->
        <div class="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs flex flex-col justify-between hover:shadow-sm transition-shadow">
          <div>
            <div class="flex items-start justify-between">
              <div class="flex items-center gap-3">
                <div class="w-11 h-11 rounded-xl bg-amber-50 border border-amber-200 flex items-center justify-center text-2xl">
                  📨
                </div>
                <div>
                  <div class="flex items-center gap-2">
                    <h3 class="font-bold text-sm text-slate-900">Apache Kafka (KRaft)</h3>
                    <span class="text-[11px] font-mono px-2 py-0.5 rounded-md bg-slate-100 text-slate-600 font-semibold">Port {{ ports.kafka }}</span>
                  </div>
                  <p class="text-xs text-slate-500 mt-0.5">OCPP 원시 패킷 및 텔레메트리 스트림 버퍼링</p>
                </div>
              </div>

              <!-- 상태 배지 -->
              <span
                class="px-2.5 py-1 rounded-full text-[11px] font-mono font-bold flex items-center gap-1.5 border"
                :class="infra.kafka ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-100 text-slate-500 border-slate-200'"
              >
                <span class="w-1.5 h-1.5 rounded-full" :class="infra.kafka ? 'bg-emerald-500 animate-pulse' : 'bg-slate-400'"></span>
                {{ infra.kafka ? 'ONLINE' : 'OFFLINE' }}
              </span>
            </div>
          </div>

          <!-- 하단 컨트롤 버튼 군 -->
          <div class="mt-5 pt-4 border-t border-slate-100 flex items-center gap-2">
            <button
              @click="control('start', 'kafka', true)"
              :disabled="infra.kafka || isExecuting"
              class="flex-1 py-2 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1.5 shadow-xs cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
              :class="infra.kafka ? 'bg-slate-100 text-slate-400' : 'bg-emerald-600 hover:bg-emerald-500 text-white'"
            >
              <span>▶</span>
              <span>시작</span>
            </button>

            <button
              @click="control('stop', 'kafka')"
              :disabled="!infra.kafka || isExecuting"
              class="px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
              :class="!infra.kafka ? 'bg-slate-100 text-slate-400' : 'bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200'"
            >
              <span>⏹</span>
              <span>중지</span>
            </button>

            <button
              @click="createTopics"
              :disabled="!infra.kafka || isExecuting"
              class="px-3 py-2 rounded-xl text-xs font-semibold bg-slate-50 hover:bg-slate-100 text-slate-700 border border-slate-200 transition-all flex items-center gap-1 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
              title="Kafka 4대 핵심 토픽(ocpp-raw-events 등)을 자동 생성합니다"
            >
              <span>⚡</span>
              <span>토픽 생성</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 2-3. 실시간 콘솔 로그 터미널 (외관은 깔끔한 라이트 카드, 내부는 눈 편한 딥 슬레이트 뷰어) -->
      <div class="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs flex flex-col gap-3">
        <!-- 툴바 -->
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-100">
          <div class="flex items-center gap-3">
            <span class="text-xs font-bold text-slate-800 flex items-center gap-1.5">
              <span>🖥️ 실시간 콘솔 로그:</span>
            </span>

            <!-- 세그먼트 탭 전환 (MySQL / Kafka) -->
            <div class="flex items-center p-1 rounded-xl bg-slate-100 border border-slate-200">
              <button
                @click="selectedLogService = 'mysql'"
                class="px-3 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer"
                :class="selectedLogService === 'mysql' ? 'bg-white text-sky-700 shadow-xs' : 'text-slate-500 hover:text-slate-800'"
              >
                🐬 MySQL 9.71
              </button>
              <button
                @click="selectedLogService = 'kafka'"
                class="px-3 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer"
                :class="selectedLogService === 'kafka' ? 'bg-white text-emerald-700 shadow-xs' : 'text-slate-500 hover:text-slate-800'"
              >
                📨 Kafka KRaft
              </button>
            </div>
          </div>

          <!-- 터미널 옵션 & 제어 버튼 -->
          <div class="flex items-center gap-2 flex-wrap">
            <!-- 자동 갱신 -->
            <label class="inline-flex items-center gap-1.5 cursor-pointer select-none text-xs text-slate-600 px-2.5 py-1 rounded-lg bg-slate-50 border border-slate-200">
              <input type="checkbox" v-model="autoRefreshLogs" class="rounded text-sky-600 focus:ring-0 cursor-pointer" />
              <span class="text-[11px] font-medium">자동 갱신 (2초)</span>
            </label>

            <!-- 자동 스크롤 -->
            <label class="inline-flex items-center gap-1.5 cursor-pointer select-none text-xs text-slate-600 px-2.5 py-1 rounded-lg bg-slate-50 border border-slate-200">
              <input type="checkbox" v-model="autoScrollLogs" class="rounded text-emerald-600 focus:ring-0 cursor-pointer" />
              <span class="text-[11px] font-medium">자동 스크롤</span>
            </label>

            <!-- 새로고침 -->
            <button
              @click="fetchLogs"
              :disabled="isFetchingLogs"
              class="p-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-600 transition-all text-xs cursor-pointer border border-slate-200"
              title="로그 지금 새로고침"
            >
              <svg class="w-3.5 h-3.5" :class="{ 'animate-spin': isFetchingLogs }" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
            </button>

            <!-- 복사 -->
            <button
              @click="copyLogs"
              class="px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-slate-200 border border-slate-200 text-[11px] font-bold text-slate-700 transition-all flex items-center gap-1 cursor-pointer"
              title="로그 전체 클립보드 복사"
            >
              <span>📋</span>
              <span>복사</span>
            </button>

            <!-- 비우기 -->
            <button
              @click="clearLogs"
              class="px-2.5 py-1 rounded-lg bg-rose-50 hover:bg-rose-100 border border-rose-200 text-[11px] font-bold text-rose-700 transition-all flex items-center gap-1 cursor-pointer"
              title="로그 파일 비우기"
            >
              <span>🗑️</span>
              <span>비우기</span>
            </button>
          </div>
        </div>

        <!-- 터미널 본문 (가독성 높은 딥 슬레이트 콘솔) -->
        <div
          ref="logContainerRef"
          class="h-80 overflow-y-auto rounded-xl p-4 font-mono text-[11px] leading-relaxed bg-[#0F172A] text-slate-300 border border-slate-800 shadow-inner select-text"
        >
          <div v-if="logLines.length === 0" class="h-full flex flex-col items-center justify-center text-slate-500 gap-2 select-none">
            <span class="text-2xl">💤</span>
            <span>수집된 {{ selectedLogService.toUpperCase() }} 실시간 콘솔 로그가 없습니다.</span>
            <span class="text-[11px] text-slate-500">서비스가 시작되면 실시간 stdout/stderr 스트림이 여기에 표시됩니다.</span>
          </div>
          <div v-else class="space-y-0.5">
            <div
              v-for="(line, idx) in logLines"
              :key="idx"
              class="flex items-start gap-2 hover:bg-white/5 px-1 py-0.2 rounded transition-colors"
            >
              <span class="text-slate-600 select-none text-[10px] w-8 text-right shrink-0">{{ idx + 1 }}</span>
              <span :class="formatLogLine(line)" class="break-all whitespace-pre-wrap flex-1">{{ line }}</span>
            </div>
          </div>
        </div>

        <!-- 터미널 하단 정보 -->
        <div class="flex items-center justify-between text-[11px] text-slate-500 px-1 pt-1">
          <div class="flex items-center gap-3">
            <span>로그 파일: <code class="font-mono text-sky-700 font-semibold">{{ paths.logDir || 'D:\\elvis-lite\\logs' }}\{{ selectedLogService }}.log</code></span>
            <span>크기: <strong class="text-slate-700">{{ formatBytes(logFileSize) }}</strong></span>
            <span>라인: <strong class="text-slate-700">{{ logLines.length }}</strong>줄</span>
          </div>
          <div v-if="logLastModified">
            마지막 수집: {{ new Date(logLastModified).toLocaleTimeString() }}
          </div>
        </div>
      </div>
    </main>

    <!-- 3. 토스트 알림 컴포넌트 (라이트 테마) -->
    <div class="fixed bottom-5 right-5 z-50 flex flex-col gap-2 pointer-events-none">
      <transition-group name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="pointer-events-auto px-4 py-3 rounded-xl bg-white border shadow-lg flex items-center gap-2.5 text-xs font-medium max-w-md"
          :class="toast.type === 'success' ? 'border-emerald-300 text-emerald-900 shadow-emerald-500/10' : toast.type === 'error' ? 'border-rose-300 text-rose-900 shadow-rose-500/10' : 'border-sky-300 text-sky-900 shadow-sky-500/10'"
        >
          <span class="text-sm">{{ toast.type === 'success' ? '✅' : toast.type === 'error' ? '⚠️' : 'ℹ️' }}</span>
          <div class="flex flex-col">
            <strong class="font-bold text-slate-900">{{ toast.title }}</strong>
            <span class="text-[11px] text-slate-600">{{ toast.message }}</span>
          </div>
        </div>
      </transition-group>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch, nextTick } from 'vue'

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

// 인프라 점유 상태
const infra = reactive({
  kafka: false,
  mysql: false,
  controlApi: false
})

const ports = reactive({
  mysql: 13306,
  kafka: 19092,
  api: 18088,
  ws: 18080
})

const paths = reactive({
  dataDir: '',
  logDir: ''
})

const isChecking = ref(false)
const isExecuting = ref(false)

// 실시간 콘솔 로그 상태
const selectedLogService = ref<'mysql' | 'kafka'>('mysql')
const logLines = ref<string[]>([])
const logFileSize = ref(0)
const logLastModified = ref<string | null>(null)
const isFetchingLogs = ref(false)
const autoRefreshLogs = ref(true)
const autoScrollLogs = ref(true)
const logContainerRef = ref<HTMLElement | null>(null)
let statusPollingTimer: any = null
let logPollingTimer: any = null

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
    if (data.ports) {
      ports.mysql = data.ports.mysql || ports.mysql
      ports.kafka = data.ports.kafka || ports.kafka
      ports.api = data.ports.api || ports.api
      ports.ws = data.ports.ws || ports.ws
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
      if (action === 'start') {
        if (service === 'mysql' || service === 'kafka') {
          selectedLogService.value = service as any
        }
        setTimeout(fetchLogs, 800)
      }
    } else {
      addToast('제어 실패', data.message, 'error')
    }
  } catch (err: any) {
    addToast('통신 오류', err.message, 'error')
  } finally {
    isExecuting.value = false
  }
}

// 3. 전체 기동
async function startAll(isBackground = true) {
  isExecuting.value = true
  try {
    const res = await fetch(`/api/infra/start?service=all&background=${isBackground}`, { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('전체 기동', data.message, 'success')
      setTimeout(checkStatus, 2500)
      setTimeout(fetchLogs, 1000)
    }
  } catch (err: any) {
    addToast('기동 실패', err.message, 'error')
  } finally {
    isExecuting.value = false
  }
}

// 4. 전체 중지
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

// 5. 백그라운드 전환 재기동
async function restartBackground() {
  isExecuting.value = true
  try {
    const res = await fetch('/api/infra/restart-background', { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      addToast('재기동 완료', data.message, 'success')
      setTimeout(checkStatus, 2500)
      setTimeout(fetchLogs, 3000)
    }
  } catch (err: any) {
    addToast('재기동 오류', err.message, 'error')
  } finally {
    isExecuting.value = false
  }
}

// 6. 실시간 콘솔 로그 조회
async function fetchLogs() {
  if (isFetchingLogs.value) return
  isFetchingLogs.value = true
  try {
    const res = await fetch(`/api/infra/logs?service=${selectedLogService.value}&tail=250`)
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
    console.warn('[Log-Fetch-Warn]', e.message)
  } finally {
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
  if (!logLines.value.length) return
  navigator.clipboard.writeText(logLines.value.join('\n')).then(() => {
    addToast('로그 복사', '클립보드에 복사되었습니다.', 'success')
  }).catch(() => {
    addToast('복사 실패', '클립보드 접근 권한이 없습니다.', 'error')
  })
}

// 7. 스키마 적재
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
    addToast('적재 오류', err.message, 'error')
  } finally {
    isExecuting.value = false
  }
}

// 8. Kafka 토픽 생성
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
    addToast('생성 오류', err.message, 'error')
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
  if (line.includes('[System]') || line.includes('[OK]') || line.includes('ready for connections') || line.includes('started')) {
    return 'text-emerald-400'
  }
  if (line.includes('[INFO]') || line.includes('INFO')) {
    return 'text-sky-300'
  }
  return 'text-slate-300'
}

watch(selectedLogService, () => {
  fetchLogs()
})

onMounted(() => {
  checkStatus()
  fetchLogs()
  statusPollingTimer = setInterval(checkStatus, 3000)
  logPollingTimer = setInterval(() => {
    if (autoRefreshLogs.value) {
      fetchLogs()
    }
  }, 2000)
})

onUnmounted(() => {
  if (statusPollingTimer) clearInterval(statusPollingTimer)
  if (logPollingTimer) clearInterval(logPollingTimer)
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
