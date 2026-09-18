<template>
  <div class="h-full w-full flex flex-col p-4 md:p-6 overflow-y-auto font-sans bg-[var(--bg-base)] text-[var(--text-bright)]">
    <!-- 헤더 & 네비게이션 브레드크럼 -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-5 border-b border-[var(--border-glass)]">
      <div>
        <div class="flex items-center gap-2 text-xs text-slate-500 mb-1">
          <router-link to="/" class="hover:text-sky-500 transition-colors">관제 센터</router-link>
          <span>/</span>
          <span class="text-[var(--text-bright)] font-semibold">시스템 관리자</span>
          <span>/</span>
          <span class="text-sky-600 dark:text-sky-400 font-bold">인프라 및 환경 설정</span>
        </div>
        <h1 class="text-xl md:text-2xl font-black tracking-tight flex items-center gap-2.5">
          <span class="p-2 rounded-xl bg-gradient-to-br from-[#0A1E5A]/20 to-[#E96600]/20 border border-[#E96600]/30 text-amber-500 text-lg shadow-2xs">⚙️</span>
          <span>ELVIS 인프라 & 시스템 관리자 센터</span>
        </h1>
        <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">
          로컬 미들웨어(MySQL 9.71, Kafka), 데이터/로그 스토리지 마운트 경로, DB 스키마 및 토픽을 종합 제어합니다.
        </p>
      </div>

      <!-- 우측 빠른 액션 버튼 -->
      <div class="flex items-center gap-2">
        <button
          @click="checkStatus"
          :disabled="isChecking"
          class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-[var(--border-glass)] bg-[var(--bg-surface-1)] hover:bg-[var(--bg-surface-2)] text-xs font-bold transition-all shadow-xs disabled:opacity-50"
        >
          <svg class="w-3.5 h-3.5 text-sky-500" :class="{ 'animate-spin': isChecking }" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
          <span>상태 새로고침</span>
        </button>
        <button
          @click="startAllMiddleware"
          :disabled="isExecuting"
          class="flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg bg-gradient-to-r from-[#0A1E5A] to-[#0569A0] hover:opacity-90 text-white text-xs font-black shadow-md hover:shadow-sky-500/20 transition-all disabled:opacity-50"
        >
          <span class="text-[#E96600]">⚡</span>
          <span>전체 서비스 일괄 기동</span>
        </button>
      </div>
    </div>

    <!-- 3대 탭 메뉴 -->
    <div class="flex items-center gap-2 mt-5 border-b border-[var(--border-glass)]">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        @click="activeTab = tab.id"
        class="flex items-center gap-2 px-4 py-2.5 text-xs font-bold border-b-2 transition-all cursor-pointer"
        :class="activeTab === tab.id
          ? 'border-sky-500 text-sky-600 dark:text-sky-400 bg-sky-500/5'
          : 'border-transparent text-slate-500 hover:text-[var(--text-bright)]'"
      >
        <span>{{ tab.icon }}</span>
        <span>{{ tab.label }}</span>
        <span
          v-if="tab.badge"
          class="px-1.5 py-0.2 rounded-full text-[10px] font-mono"
          :class="tab.badgeClass"
        >
          {{ tab.badge }}
        </span>
      </button>
    </div>

    <!-- 탭 1: 미들웨어 & 서비스 제어 -->
    <div v-if="activeTab === 'middleware'" class="mt-6 flex flex-col gap-6">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <!-- 1. MySQL 9.71 -->
        <div class="p-5 rounded-2xl bg-[var(--bg-surface-1)] border border-[var(--border-glass)] shadow-sm flex flex-col justify-between relative overflow-hidden">
          <div class="absolute top-0 right-0 p-4">
            <span
              class="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold flex items-center gap-1"
              :class="infra.mysql ? 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/15 text-rose-500 border border-rose-500/30'"
            >
              <span class="w-1.5 h-1.5 rounded-full" :class="infra.mysql ? 'bg-emerald-500 animate-pulse' : 'bg-rose-500'"></span>
              {{ infra.mysql ? 'ONLINE' : 'OFFLINE' }}
            </span>
          </div>

          <div>
            <div class="w-10 h-10 rounded-xl bg-sky-500/10 border border-sky-500/20 flex items-center justify-center text-xl mb-3">
              🐬
            </div>
            <h3 class="text-sm font-bold text-[var(--text-bright)] flex items-center gap-1.5">
              <span>MySQL 9.71 중앙 DB</span>
              <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-500/10 text-slate-400">포트 {{ ports.mysql }}</span>
            </h3>
            <p class="text-xs text-slate-500 dark:text-slate-400 mt-1 leading-relaxed">
              충전소, 충전기 정보, 실시간 텔레메트리 상태 및 과금 원장(CDR)을 보관하는 중앙 영속 RDBMS입니다.
            </p>
          </div>

          <div class="mt-6 pt-4 border-t border-[var(--border-glass)] flex items-center gap-2">
            <button
              @click="controlService('start', 'mysql')"
              :disabled="infra.mysql || isExecuting"
              class="flex-1 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center justify-center gap-1"
              :class="infra.mysql ? 'bg-slate-500/10 text-slate-400 cursor-not-allowed' : 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-sm'"
            >
              <span>▶</span>
              <span>기동</span>
            </button>
            <button
              @click="controlService('stop', 'mysql')"
              :disabled="!infra.mysql || isExecuting"
              class="flex-1 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center justify-center gap-1"
              :class="!infra.mysql ? 'bg-slate-500/10 text-slate-400 cursor-not-allowed' : 'bg-rose-600 hover:bg-rose-500 text-white shadow-sm'"
            >
              <span>⏹</span>
              <span>중지</span>
            </button>
          </div>
        </div>

        <!-- 2. Apache Kafka -->
        <div class="p-5 rounded-2xl bg-[var(--bg-surface-1)] border border-[var(--border-glass)] shadow-sm flex flex-col justify-between relative overflow-hidden">
          <div class="absolute top-0 right-0 p-4">
            <span
              class="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold flex items-center gap-1"
              :class="infra.kafka ? 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/15 text-rose-500 border border-rose-500/30'"
            >
              <span class="w-1.5 h-1.5 rounded-full" :class="infra.kafka ? 'bg-emerald-500 animate-pulse' : 'bg-rose-500'"></span>
              {{ infra.kafka ? 'ONLINE' : 'OFFLINE' }}
            </span>
          </div>

          <div>
            <div class="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-xl mb-3">
              📨
            </div>
            <h3 class="text-sm font-bold text-[var(--text-bright)] flex items-center gap-1.5">
              <span>Apache Kafka (KRaft)</span>
              <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-500/10 text-slate-400">포트 {{ ports.kafka }}</span>
            </h3>
            <p class="text-xs text-slate-500 dark:text-slate-400 mt-1 leading-relaxed">
              OCPP 원시 패킷, 미터값 스트림, 상태 변경 알림을 초고속으로 버퍼링하는 대용량 메시지 브로커입니다.
            </p>
          </div>

          <div class="mt-6 pt-4 border-t border-[var(--border-glass)] flex items-center gap-2">
            <button
              @click="controlService('start', 'kafka')"
              :disabled="infra.kafka || isExecuting"
              class="flex-1 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center justify-center gap-1"
              :class="infra.kafka ? 'bg-slate-500/10 text-slate-400 cursor-not-allowed' : 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-sm'"
            >
              <span>▶</span>
              <span>기동</span>
            </button>
            <button
              @click="controlService('stop', 'kafka')"
              :disabled="!infra.kafka || isExecuting"
              class="flex-1 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center justify-center gap-1"
              :class="!infra.kafka ? 'bg-slate-500/10 text-slate-400 cursor-not-allowed' : 'bg-rose-600 hover:bg-rose-500 text-white shadow-sm'"
            >
              <span>⏹</span>
              <span>중지</span>
            </button>
          </div>
        </div>

        <!-- 3. Spring Boot Control API -->
        <div class="p-5 rounded-2xl bg-[var(--bg-surface-1)] border border-[var(--border-glass)] shadow-sm flex flex-col justify-between relative overflow-hidden">
          <div class="absolute top-0 right-0 p-4">
            <span
              class="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold flex items-center gap-1"
              :class="infra.controlApi ? 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30' : 'bg-amber-500/15 text-amber-500 border border-amber-500/30'"
            >
              <span class="w-1.5 h-1.5 rounded-full" :class="infra.controlApi ? 'bg-emerald-500 animate-pulse' : 'bg-amber-500'"></span>
              {{ infra.controlApi ? 'ONLINE' : 'STANDBY' }}
            </span>
          </div>

          <div>
            <div class="w-10 h-10 rounded-xl bg-violet-500/10 border border-violet-500/20 flex items-center justify-center text-xl mb-3">
              ☕
            </div>
            <h3 class="text-sm font-bold text-[var(--text-bright)] flex items-center gap-1.5">
              <span>ELVIS Control API</span>
              <span class="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-500/10 text-slate-400">포트 {{ ports.api }}</span>
            </h3>
            <p class="text-xs text-slate-500 dark:text-slate-400 mt-1 leading-relaxed">
              Spring Boot 기반 3계층 비즈니스 REST API 서버로 MySQL 9.71과 직접 통신하여 정산 및 관제 데이터를 서빙합니다.
            </p>
          </div>

          <div class="mt-6 pt-4 border-t border-[var(--border-glass)] flex items-center gap-2">
            <span class="text-[11px] text-slate-500">
              {{ infra.controlApi ? '✅ 정상 연동 중 (/api/v1/*)' : '💡 Mock Fallback 모드로 실행 중' }}
            </span>
          </div>
        </div>
      </div>

      <!-- 독립 백그라운드 구동 안내 배너 -->
      <div class="p-4 rounded-xl bg-sky-500/5 border border-sky-500/20 flex items-start gap-3">
        <span class="text-lg">🛡️</span>
        <div class="text-xs leading-relaxed text-slate-600 dark:text-slate-300">
          <strong class="text-sky-600 dark:text-sky-400">미들웨어 24시간 독립 유지 안내:</strong>
          Tauri 데스크톱 관제 UI 창을 닫아도 <code class="font-mono bg-sky-500/10 px-1 py-0.5 rounded text-sky-600 dark:text-sky-400">DETACHED_PROCESS</code> 기술로 인해 백그라운드 미들웨어(MySQL, Kafka)는 종료되지 않고 안전하게 유지됩니다. 완전히 종료하려면 시스템 트레이 메뉴의 [종료] 또는 상단 중지 버튼을 사용하세요.
        </div>
      </div>
    </div>

    <!-- 탭 2: 네트워크 및 포트 설정 (신규) -->
    <div v-if="activeTab === 'ports'" class="mt-6 flex flex-col gap-6 max-w-4xl">
      <!-- 1. 원클릭 프리셋 배너 -->
      <div class="p-5 rounded-2xl bg-[var(--bg-surface-1)] border border-[var(--border-glass)] shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h3 class="text-xs font-bold text-[var(--text-bright)] flex items-center gap-1.5">
            <span class="text-indigo-500">⚡</span>
            <span>포트 설정 원클릭 프리셋</span>
          </h3>
          <p class="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
            고객사 PC 포트 충돌을 회피하는 권장 전용 포트 또는 순수 표준 포트로 일괄 전환합니다. (수동 개별 수정도 가능)
          </p>
        </div>
        <div class="flex items-center gap-2 flex-wrap">
          <button
            @click="applyPreset('elvis')"
            class="px-3 py-1.5 rounded-lg bg-indigo-600/15 hover:bg-indigo-600/25 border border-indigo-500/30 text-indigo-600 dark:text-indigo-400 text-xs font-bold transition-all flex items-center gap-1.5"
            title="MySQL: 13306, Kafka: 19092, API: 18081, WS: 18080"
          >
            <span>🔒</span>
            <span>ELVIS 전용 포트 (권장)</span>
          </button>
          <button
            @click="applyPreset('standard')"
            class="px-3 py-1.5 rounded-lg bg-slate-500/10 hover:bg-slate-500/20 border border-[var(--border-glass)] text-slate-600 dark:text-slate-300 text-xs font-bold transition-all flex items-center gap-1.5"
            title="MySQL: 3306, Kafka: 9092, API: 8081, WS: 8080"
          >
            <span>⚙️</span>
            <span>표준 포트</span>
          </button>
          <button
            @click="checkAllPorts"
            :disabled="isCheckingAllPorts"
            class="px-3 py-1.5 rounded-lg border border-[var(--border-glass)] bg-[var(--bg-surface-2)] hover:bg-[var(--bg-surface-1)] text-xs font-bold text-slate-600 dark:text-slate-300 transition-all flex items-center gap-1.5 disabled:opacity-50"
          >
            <svg class="w-3.5 h-3.5 text-indigo-500" :class="{ 'animate-spin': isCheckingAllPorts }" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
            <span>전체 점유 확인</span>
          </button>
        </div>
      </div>

      <!-- 2. 4대 서비스별 포트 설정 카드 그리드 -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <!-- 1) MySQL 포트 -->
        <div class="p-5 rounded-2xl bg-[var(--bg-surface-1)] border border-[var(--border-glass)] shadow-sm flex flex-col justify-between gap-4">
          <div>
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="text-xl">🐬</span>
                <div>
                  <h4 class="text-xs font-bold text-[var(--text-bright)]">MySQL 9.71 중앙 DB</h4>
                  <span class="text-[10px] text-slate-400 font-mono">기본: 13306 (표준: 3306)</span>
                </div>
              </div>
              <span
                v-if="portStatus.mysql"
                class="text-[10px] font-bold px-2 py-0.5 rounded-full"
                :class="portStatus.mysql.isOccupied ? 'bg-amber-500/15 text-amber-500 border border-amber-500/30' : 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30'"
              >
                {{ portStatus.mysql.isOccupied ? '⚠️ 현재 점유/실행 중' : '✓ 사용 가능 (IDLE)' }}
              </span>
            </div>
            <p class="text-[11px] text-slate-500 dark:text-slate-400 mt-2">
              충전 자산, 실시간 상태 및 과금 원장을 저장하는 영속 RDBMS 포트입니다.
            </p>
          </div>

          <div class="flex items-center gap-2 pt-2 border-t border-[var(--border-glass)]">
            <span class="text-xs font-mono text-slate-500">Port :</span>
            <input
              type="number"
              v-model.number="editPorts.mysql"
              min="1024"
              max="65535"
              placeholder="13306"
              class="flex-1 px-3 py-1.5 text-xs font-mono rounded-lg bg-[var(--bg-surface-2)] border border-[var(--border-glass)] focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 outline-none transition-all text-[var(--text-bright)]"
            />
            <button
              @click="checkSinglePort('mysql')"
              :disabled="portStatus.mysql?.loading"
              class="px-2.5 py-1.5 text-xs font-semibold rounded-lg border border-[var(--border-glass)] hover:bg-[var(--bg-surface-2)] text-slate-500 transition-all"
            >
              {{ portStatus.mysql?.loading ? '검사 중...' : '점유 검사' }}
            </button>
          </div>
        </div>

        <!-- 2) Kafka 포트 -->
        <div class="p-5 rounded-2xl bg-[var(--bg-surface-1)] border border-[var(--border-glass)] shadow-sm flex flex-col justify-between gap-4">
          <div>
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="text-xl">📨</span>
                <div>
                  <h4 class="text-xs font-bold text-[var(--text-bright)]">Apache Kafka 브로커</h4>
                  <span class="text-[10px] text-slate-400 font-mono">기본: 19092 (표준: 9092)</span>
                </div>
              </div>
              <span
                v-if="portStatus.kafka"
                class="text-[10px] font-bold px-2 py-0.5 rounded-full"
                :class="portStatus.kafka.isOccupied ? 'bg-amber-500/15 text-amber-500 border border-amber-500/30' : 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30'"
              >
                {{ portStatus.kafka.isOccupied ? '⚠️ 현재 점유/실행 중' : '✓ 사용 가능 (IDLE)' }}
              </span>
            </div>
            <p class="text-[11px] text-slate-500 dark:text-slate-400 mt-2">
              실시간 충전기 패킷 및 대용량 텔레메트리 스트림을 버퍼링하는 메시지 브로커 포트입니다.
            </p>
          </div>

          <div class="flex items-center gap-2 pt-2 border-t border-[var(--border-glass)]">
            <span class="text-xs font-mono text-slate-500">Port :</span>
            <input
              type="number"
              v-model.number="editPorts.kafka"
              min="1024"
              max="65535"
              placeholder="19092"
              class="flex-1 px-3 py-1.5 text-xs font-mono rounded-lg bg-[var(--bg-surface-2)] border border-[var(--border-glass)] focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 outline-none transition-all text-[var(--text-bright)]"
            />
            <button
              @click="checkSinglePort('kafka')"
              :disabled="portStatus.kafka?.loading"
              class="px-2.5 py-1.5 text-xs font-semibold rounded-lg border border-[var(--border-glass)] hover:bg-[var(--bg-surface-2)] text-slate-500 transition-all"
            >
              {{ portStatus.kafka?.loading ? '검사 중...' : '점유 검사' }}
            </button>
          </div>
        </div>

        <!-- 3) CSMS Control API 포트 -->
        <div class="p-5 rounded-2xl bg-[var(--bg-surface-1)] border border-[var(--border-glass)] shadow-sm flex flex-col justify-between gap-4">
          <div>
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="text-xl">☕</span>
                <div>
                  <h4 class="text-xs font-bold text-[var(--text-bright)]">CSMS Control Admin API</h4>
                  <span class="text-[10px] text-slate-400 font-mono">기본: 18081 (표준: 8081)</span>
                </div>
              </div>
              <span
                v-if="portStatus.api"
                class="text-[10px] font-bold px-2 py-0.5 rounded-full"
                :class="portStatus.api.isOccupied ? 'bg-amber-500/15 text-amber-500 border border-amber-500/30' : 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30'"
              >
                {{ portStatus.api.isOccupied ? '⚠️ 현재 점유/실행 중' : '✓ 사용 가능 (IDLE)' }}
              </span>
            </div>
            <p class="text-[11px] text-slate-500 dark:text-slate-400 mt-2">
              Spring Boot 기반 관리자 REST API 서버로 관제 UI와 통신하는 백엔드 포트입니다.
            </p>
          </div>

          <div class="flex items-center gap-2 pt-2 border-t border-[var(--border-glass)]">
            <span class="text-xs font-mono text-slate-500">Port :</span>
            <input
              type="number"
              v-model.number="editPorts.api"
              min="1024"
              max="65535"
              placeholder="18081"
              class="flex-1 px-3 py-1.5 text-xs font-mono rounded-lg bg-[var(--bg-surface-2)] border border-[var(--border-glass)] focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 outline-none transition-all text-[var(--text-bright)]"
            />
            <button
              @click="checkSinglePort('api')"
              :disabled="portStatus.api?.loading"
              class="px-2.5 py-1.5 text-xs font-semibold rounded-lg border border-[var(--border-glass)] hover:bg-[var(--bg-surface-2)] text-slate-500 transition-all"
            >
              {{ portStatus.api?.loading ? '검사 중...' : '점유 검사' }}
            </button>
          </div>
        </div>

        <!-- 4) WebSocket Gateway 포트 -->
        <div class="p-5 rounded-2xl bg-[var(--bg-surface-1)] border border-[var(--border-glass)] shadow-sm flex flex-col justify-between gap-4">
          <div>
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="text-xl">🔌</span>
                <div>
                  <h4 class="text-xs font-bold text-[var(--text-bright)]">WebSocket Gateway (OCPP)</h4>
                  <span class="text-[10px] text-slate-400 font-mono">기본: 18080 (표준: 8080)</span>
                </div>
              </div>
              <span
                v-if="portStatus.ws"
                class="text-[10px] font-bold px-2 py-0.5 rounded-full"
                :class="portStatus.ws.isOccupied ? 'bg-amber-500/15 text-amber-500 border border-amber-500/30' : 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30'"
              >
                {{ portStatus.ws.isOccupied ? '⚠️ 현재 점유/실행 중' : '✓ 사용 가능 (IDLE)' }}
              </span>
            </div>
            <p class="text-[11px] text-slate-500 dark:text-slate-400 mt-2">
              실제 현장 전기차 충전기들이 웹소켓(OCPP-J 1.6/2.0.1) 프로토콜로 연결되는 관문 포트입니다.
            </p>
          </div>

          <div class="flex items-center gap-2 pt-2 border-t border-[var(--border-glass)]">
            <span class="text-xs font-mono text-slate-500">Port :</span>
            <input
              type="number"
              v-model.number="editPorts.ws"
              min="1024"
              max="65535"
              placeholder="18080"
              class="flex-1 px-3 py-1.5 text-xs font-mono rounded-lg bg-[var(--bg-surface-2)] border border-[var(--border-glass)] focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 outline-none transition-all text-[var(--text-bright)]"
            />
            <button
              @click="checkSinglePort('ws')"
              :disabled="portStatus.ws?.loading"
              class="px-2.5 py-1.5 text-xs font-semibold rounded-lg border border-[var(--border-glass)] hover:bg-[var(--bg-surface-2)] text-slate-500 transition-all"
            >
              {{ portStatus.ws?.loading ? '검사 중...' : '점유 검사' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 3. 저장 안내 및 저장 버튼 -->
      <div class="p-4 rounded-2xl bg-[var(--bg-surface-1)] border border-[var(--border-glass)] flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div class="text-xs text-slate-500">
          * 저장 시 <code class="font-mono text-indigo-500">paths.env</code>, <code class="font-mono text-indigo-500">my.ini</code>, <code class="font-mono text-indigo-500">server.properties</code>, <code class="font-mono text-indigo-500">application.yml</code>이 자동 동기화됩니다.<br/>
          * 이미 실행 중인 미들웨어는 중지 후 재기동해야 새 포트가 바인딩됩니다.
        </div>
        <button
          @click="savePorts"
          :disabled="isSavingPorts"
          class="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold shadow-md hover:shadow-indigo-500/20 transition-all flex items-center justify-center gap-2 disabled:opacity-50 shrink-0"
        >
          <span>💾</span>
          <span>{{ isSavingPorts ? '저장 및 동기화 중...' : '포트 설정 저장 및 설정 파일 동기화' }}</span>
        </button>
      </div>
    </div>

    <!-- 탭 3: 스토리지 & 로그 경로 설정 -->
    <div v-if="activeTab === 'storage'" class="mt-6 flex flex-col gap-6 max-w-4xl">
      <div class="p-6 rounded-2xl bg-[var(--bg-surface-1)] border border-[var(--border-glass)] shadow-sm flex flex-col gap-5">
        <div>
          <h2 class="text-base font-bold text-[var(--text-bright)] flex items-center gap-2">
            <span>📂</span>
            <span>데이터 및 로그 스토리지 경로 설정</span>
          </h2>
          <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">
            C 드라이브 용량 부족 및 OS 포맷 시 유실 위험을 방지하기 위해 데이터와 로그를 D 드라이브 또는 외장 스토리지로 분리할 수 있습니다.
          </p>
        </div>

        <!-- 1. 데이터 저장 디렉터리 -->
        <div class="flex flex-col gap-2">
          <label class="text-xs font-bold text-[var(--text-bright)] flex items-center justify-between">
            <span class="flex items-center gap-1.5">
              <span>💾 DB & 메시지 영속 데이터 디렉터리 (Data Directory)</span>
            </span>
            <span
              class="text-[10px] font-bold px-2 py-0.5 rounded-full"
              :class="paths.dataExists ? 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400' : 'bg-amber-500/15 text-amber-500'"
            >
              {{ paths.dataExists ? '✓ 디렉터리 존재함' : '⚠️ 저장 시 자동 생성됨' }}
            </span>
          </label>
          <div class="flex items-center gap-2">
            <input
              type="text"
              v-model="editPaths.dataDir"
              placeholder="예: D:\elvis-lite\data"
              class="flex-1 px-3 py-2 text-xs font-mono rounded-lg bg-[var(--bg-surface-2)] border border-[var(--border-glass)] focus:border-sky-500 focus:ring-1 focus:ring-sky-500 outline-none transition-all text-[var(--text-bright)]"
            />
            <button
              @click="editPaths.dataDir = 'D:\\elvis-lite\\data'"
              class="px-2.5 py-2 text-xs font-semibold rounded-lg border border-[var(--border-glass)] hover:bg-[var(--bg-surface-2)] text-slate-500"
              title="D 드라이브 기본값으로 복원"
            >
              D: 기본값
            </button>
          </div>
          <span class="text-[11px] text-slate-400">MySQL InnoDB 데이터 파일 및 Kafka 토픽 로그 세그먼트가 저장되는 위치입니다.</span>
        </div>

        <!-- 2. 로그 저장 디렉터리 -->
        <div class="flex flex-col gap-2">
          <label class="text-xs font-bold text-[var(--text-bright)] flex items-center justify-between">
            <span class="flex items-center gap-1.5">
              <span>📋 시스템 및 패킷 로그 디렉터리 (Log Directory)</span>
            </span>
            <span
              class="text-[10px] font-bold px-2 py-0.5 rounded-full"
              :class="paths.logExists ? 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400' : 'bg-amber-500/15 text-amber-500'"
            >
              {{ paths.logExists ? '✓ 디렉터리 존재함' : '⚠️ 저장 시 자동 생성됨' }}
            </span>
          </label>
          <div class="flex items-center gap-2">
            <input
              type="text"
              v-model="editPaths.logDir"
              placeholder="예: D:\elvis-lite\logs"
              class="flex-1 px-3 py-2 text-xs font-mono rounded-lg bg-[var(--bg-surface-2)] border border-[var(--border-glass)] focus:border-sky-500 focus:ring-1 focus:ring-sky-500 outline-none transition-all text-[var(--text-bright)]"
            />
            <button
              @click="editPaths.logDir = 'D:\\elvis-lite\\logs'"
              class="px-2.5 py-2 text-xs font-semibold rounded-lg border border-[var(--border-glass)] hover:bg-[var(--bg-surface-2)] text-slate-500"
              title="D 드라이브 기본값으로 복원"
            >
              D: 기본값
            </button>
          </div>
          <span class="text-[11px] text-slate-400">OCPP 통신 로그, Spring Boot 애플리케이션 로그, 에러 덤프가 보관되는 위치입니다.</span>
        </div>

        <!-- 저장 안내 및 버튼 -->
        <div class="pt-4 border-t border-[var(--border-glass)] flex items-center justify-between">
          <div class="text-xs text-slate-500">
            * 변경 사항은 <code class="font-mono text-sky-600 dark:text-sky-400">config/paths.env</code> 파일에 영구 저장됩니다.
          </div>
          <button
            @click="savePaths"
            :disabled="isSavingPaths"
            class="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold shadow-md hover:shadow-sky-500/20 transition-all flex items-center gap-2 disabled:opacity-50"
          >
            <span>💾</span>
            <span>{{ isSavingPaths ? '저장 중...' : '경로 저장 및 디렉터리 생성' }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 탭 3: DB 스키마 & 토픽 도구 -->
    <div v-if="activeTab === 'database'" class="mt-6 flex flex-col gap-6 max-w-4xl">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <!-- 1. MySQL 9.71 DDL 스키마 & 시드 데이터 적재 도구 -->
        <div class="p-6 rounded-2xl bg-[var(--bg-surface-1)] border border-[var(--border-glass)] shadow-sm flex flex-col justify-between">
          <div>
            <div class="w-10 h-10 rounded-xl bg-sky-500/10 border border-sky-500/20 flex items-center justify-center text-xl mb-3">
              🗄️
            </div>
            <h3 class="text-sm font-bold text-[var(--text-bright)]">MySQL 스키마 & 시드 원클릭 적재</h3>
            <p class="text-xs text-slate-500 dark:text-slate-400 mt-1 leading-relaxed">
              DDL 스키마 및 4대 법인, 주요 20개 충전소, 100기 충전기, 실시간 텔레메트리, 정산 CDR 초기 데이터를 적재합니다.
            </p>

            <div class="mt-4 p-3 rounded-xl bg-[var(--bg-surface-2)] border border-[var(--border-glass)] text-[11px] flex flex-col gap-1.5">
              <div class="font-bold text-slate-400">적재 대상 테이블:</div>
              <div class="font-mono text-sky-600 dark:text-sky-400 flex flex-wrap gap-1">
                <span class="px-1.5 py-0.5 rounded bg-sky-500/10">TBL_CORP</span>
                <span class="px-1.5 py-0.5 rounded bg-sky-500/10">TBL_STATION</span>
                <span class="px-1.5 py-0.5 rounded bg-sky-500/10">TBL_CHARGER</span>
                <span class="px-1.5 py-0.5 rounded bg-sky-500/10">TBL_CONNECTOR_STATUS</span>
                <span class="px-1.5 py-0.5 rounded bg-sky-500/10">TBL_TRANSACTION_CDR</span>
              </div>
            </div>
          </div>

          <div class="mt-6 pt-4 border-t border-[var(--border-glass)]">
            <button
              @click="executeSchemaLoad"
              :disabled="!infra.mysql || isExecutingSchema"
              class="w-full py-2.5 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-2 shadow-sm"
              :class="!infra.mysql ? 'bg-slate-500/10 text-slate-400 cursor-not-allowed' : 'bg-sky-600 hover:bg-sky-500 text-white'"
            >
              <span>🚀</span>
              <span>{{ isExecutingSchema ? '적재 프로세스 실행 중...' : 'MySQL 9.71 스키마 & 시드 적재 실행' }}</span>
            </button>
            <div v-if="!infra.mysql" class="text-[10px] text-rose-500 mt-1.5 text-center">
              * 먼저 MySQL 서버({{ ports.mysql }})를 기동해야 적재할 수 있습니다.
            </div>
          </div>
        </div>

        <!-- 2. Kafka 핵심 토픽 생성 도구 -->
        <div class="p-6 rounded-2xl bg-[var(--bg-surface-1)] border border-[var(--border-glass)] shadow-sm flex flex-col justify-between">
          <div>
            <div class="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-xl mb-3">
              📬
            </div>
            <h3 class="text-sm font-bold text-[var(--text-bright)]">Kafka 4대 핵심 토픽 생성</h3>
            <p class="text-xs text-slate-500 dark:text-slate-400 mt-1 leading-relaxed">
              대용량 실시간 파이프라인 처리를 위한 파티션 3개, 복제본 1개 사양의 핵심 토픽들을 자동 생성합니다.
            </p>

            <div class="mt-4 p-3 rounded-xl bg-[var(--bg-surface-2)] border border-[var(--border-glass)] text-[11px] flex flex-col gap-1.5">
              <div class="font-bold text-slate-400">생성 대상 토픽:</div>
              <div class="font-mono text-emerald-600 dark:text-emerald-400 flex flex-wrap gap-1">
                <span class="px-1.5 py-0.5 rounded bg-emerald-500/10">ocpp-raw-events</span>
                <span class="px-1.5 py-0.5 rounded bg-emerald-500/10">ocpp-meter-values</span>
                <span class="px-1.5 py-0.5 rounded bg-emerald-500/10">ocpp-status-notifications</span>
                <span class="px-1.5 py-0.5 rounded bg-emerald-500/10">ocpp-cdr-records</span>
              </div>
            </div>
          </div>

          <div class="mt-6 pt-4 border-t border-[var(--border-glass)]">
            <button
              @click="controlService('start', 'topics')"
              :disabled="!infra.kafka || isExecuting"
              class="w-full py-2.5 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-2 shadow-sm"
              :class="!infra.kafka ? 'bg-slate-500/10 text-slate-400 cursor-not-allowed' : 'bg-emerald-600 hover:bg-emerald-500 text-white'"
            >
              <span>⚡</span>
              <span>Kafka 4대 핵심 토픽 자동 생성</span>
            </button>
            <div v-if="!infra.kafka" class="text-[10px] text-rose-500 mt-1.5 text-center">
              * 먼저 Kafka 서버({{ ports.kafka }})를 기동해야 토픽을 생성할 수 있습니다.
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useCsmsStore } from '@/stores/csmsStore'

const store = useCsmsStore()

// 탭 상태
const activeTab = ref<'middleware' | 'ports' | 'storage' | 'database'>('middleware')

const infra = reactive({
  kafka: false,
  mysql: false,
  controlApi: false
})

// 현재 활성 포트
const ports = reactive({
  mysql: 13306,
  kafka: 19092,
  api: 18081,
  ws: 18080
})

// 포트 수정 폼
const editPorts = reactive({
  mysql: 13306,
  kafka: 19092,
  api: 18081,
  ws: 18080
})

// 포트별 실시간 점유 상태 검사 결과
const portStatus = reactive({
  mysql: null as { loading: boolean; isOccupied: boolean } | null,
  kafka: null as { loading: boolean; isOccupied: boolean } | null,
  api: null as { loading: boolean; isOccupied: boolean } | null,
  ws: null as { loading: boolean; isOccupied: boolean } | null
})

const paths = reactive({
  dataDir: '',
  logDir: '',
  dataExists: false,
  logExists: false
})

const editPaths = reactive({
  dataDir: '',
  logDir: ''
})

const isChecking = ref(false)
const isExecuting = ref(false)
const isSavingPaths = ref(false)
const isSavingPorts = ref(false)
const isCheckingAllPorts = ref(false)
const isExecutingSchema = ref(false)
let pollingTimer: any = null

const tabs = computed(() => [
  {
    id: 'middleware' as const,
    label: '미들웨어 제어반',
    icon: '⚡',
    badge: (infra.mysql && infra.kafka) ? '정상 작동' : '점검 필요',
    badgeClass: (infra.mysql && infra.kafka)
      ? 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400'
      : 'bg-amber-500/15 text-amber-500'
  },
  {
    id: 'ports' as const,
    label: '네트워크 & 포트 설정',
    icon: '🔌',
    badge: `MySQL:${ports.mysql}`,
    badgeClass: 'bg-indigo-500/15 text-indigo-600 dark:text-indigo-400'
  },
  {
    id: 'storage' as const,
    label: '스토리지 & 로그 경로',
    icon: '📂',
    badge: (paths.dataExists && paths.logExists) ? '정상' : '확인 요망',
    badgeClass: (paths.dataExists && paths.logExists)
      ? 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400'
      : 'bg-slate-500/10 text-slate-400'
  },
  {
    id: 'database' as const,
    label: 'DB 스키마 & 토픽 도구',
    icon: '🗄️',
    badge: 'MySQL 9.71',
    badgeClass: 'bg-sky-500/15 text-sky-600 dark:text-sky-400'
  }
])

// 1. 상태 조회
async function checkStatus() {
  isChecking.value = true
  try {
    const res = await fetch('/api/infra/status')
    if (res.ok) {
      const data = await res.json()
      infra.kafka = !!data.kafka
      infra.mysql = !!data.mysql
      infra.controlApi = !!data.controlApi
      if (data.ports) {
        ports.mysql = data.ports.mysql || ports.mysql
        ports.kafka = data.ports.kafka || ports.kafka
        ports.api = data.ports.api || ports.api
        ports.ws = data.ports.ws || ports.ws
      }
    }
  } catch (err) {
    console.error('인프라 상태 조회 실패:', err)
  } finally {
    isChecking.value = false
  }
}

// 2. 스토리지 경로 조회
async function fetchPaths() {
  try {
    const res = await fetch('/api/infra/paths')
    if (res.ok) {
      const data = await res.json()
      paths.dataDir = data.dataDir || 'D:\\elvis-lite\\data'
      paths.logDir = data.logDir || 'D:\\elvis-lite\\logs'
      paths.dataExists = !!data.dataExists
      paths.logExists = !!data.logExists

      editPaths.dataDir = paths.dataDir
      editPaths.logDir = paths.logDir
    }
  } catch (err) {
    console.error('스토리지 경로 조회 실패:', err)
  }
}

// 3. 스토리지 경로 저장
async function savePaths() {
  if (!editPaths.dataDir.trim() || !editPaths.logDir.trim()) {
    store.addToast('경로 입력 필요', '데이터 및 로그 디렉터리 경로를 모두 입력하세요.', 'error')
    return
  }

  isSavingPaths.value = true
  try {
    const res = await fetch('/api/infra/paths', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        dataDir: editPaths.dataDir.trim(),
        logDir: editPaths.logDir.trim()
      })
    })

    const result = await res.json()
    if (result.success) {
      store.addToast('경로 저장 완료', '새로운 스토리지 경로가 저장되었습니다.', 'success')
      await fetchPaths()
    } else {
      store.addToast('저장 실패', result.message || '경로 저장 중 오류가 발생했습니다.', 'error')
    }
  } catch (err: any) {
    store.addToast('통신 오류', err.message, 'error')
  } finally {
    isSavingPaths.value = false
  }
}

// 4. 서비스 개별 제어 (기동/중지)
async function controlService(action: 'start' | 'stop', service: string) {
  isExecuting.value = true
  try {
    const endpoint = `/api/infra/${action}?service=${service}`
    const res = await fetch(endpoint, { method: 'POST' })
    const result = await res.json()

    if (result.success) {
      store.addToast(`${service} 제어`, result.message, 'success')
      setTimeout(checkStatus, 1500)
    } else {
      store.addToast('제어 실패', result.message, 'error')
    }
  } catch (err: any) {
    store.addToast('오류 발생', err.message, 'error')
  } finally {
    isExecuting.value = false
  }
}

// 5. 전체 미들웨어 기동
async function startAllMiddleware() {
  isExecuting.value = true
  try {
    const res = await fetch('/api/infra/start?service=all', { method: 'POST' })
    const result = await res.json()
    if (result.success) {
      store.addToast('전체 기동', 'Kafka 및 MySQL 전체 기동 배치가 실행되었습니다.', 'success')
      setTimeout(checkStatus, 2500)
    }
  } catch (err: any) {
    store.addToast('기동 실패', err.message, 'error')
  } finally {
    isExecuting.value = false
  }
}

// 6. DB 스키마 & 시드 데이터 적재
async function executeSchemaLoad() {
  if (!infra.mysql) {
    store.addToast('MySQL 오프라인', '먼저 MySQL 서버를 기동해주세요.', 'error')
    return
  }

  isExecutingSchema.value = true
  try {
    const res = await fetch('/api/infra/schema', { method: 'POST' })
    const result = await res.json()
    if (result.success) {
      store.addToast('스키마 적재 시작', '명령 프롬프트에서 스키마 및 시드 적재가 진행 중입니다.', 'success')
    } else {
      store.addToast('스키마 적재 실패', result.message, 'error')
    }
  } catch (err: any) {
    store.addToast('오류 발생', err.message, 'error')
  } finally {
    isExecutingSchema.value = false
  }
}

// 7. 포트 설정 조회
async function fetchPorts() {
  try {
    const res = await fetch('/api/infra/ports')
    if (res.ok) {
      const data = await res.json()
      if (data.ports) {
        ports.mysql = data.ports.mysql || 13306
        ports.kafka = data.ports.kafka || 19092
        ports.api = data.ports.api || 18081
        ports.ws = data.ports.ws || 18080

        editPorts.mysql = ports.mysql
        editPorts.kafka = ports.kafka
        editPorts.api = ports.api
        editPorts.ws = ports.ws
      }
    }
  } catch (err) {
    console.error('포트 설정 조회 실패:', err)
  }
}

// 8. 단일 포트 실시간 점유 검사
async function checkSinglePort(service: 'mysql' | 'kafka' | 'api' | 'ws') {
  const port = editPorts[service]
  if (!port || port < 1024 || port > 65535) {
    store.addToast('포트 범위 오류', '1024~65535 사이의 유효한 포트를 입력하세요.', 'error')
    return
  }

  portStatus[service] = { loading: true, isOccupied: false }
  try {
    const res = await fetch(`/api/infra/check-port?port=${port}`)
    if (res.ok) {
      const data = await res.json()
      portStatus[service] = { loading: false, isOccupied: !!data.isOccupied }
    } else {
      portStatus[service] = null
    }
  } catch (err) {
    portStatus[service] = null
  }
}

// 9. 전체 포트 일괄 점유 검사
async function checkAllPorts() {
  isCheckingAllPorts.value = true
  try {
    await Promise.all([
      checkSinglePort('mysql'),
      checkSinglePort('kafka'),
      checkSinglePort('api'),
      checkSinglePort('ws')
    ])
    store.addToast('검사 완료', '4대 서비스 포트 점유 상태 확인을 완료했습니다.', 'info')
  } finally {
    isCheckingAllPorts.value = false
  }
}

// 10. 프리셋 일괄 적용
function applyPreset(type: 'elvis' | 'standard') {
  if (type === 'elvis') {
    editPorts.mysql = 13306
    editPorts.kafka = 19092
    editPorts.api = 18081
    editPorts.ws = 18080
    store.addToast('ELVIS 권장 포트', '충돌 방지 전용 포트(13306, 19092, 18081, 18080)가 입력되었습니다.', 'info')
  } else {
    editPorts.mysql = 3306
    editPorts.kafka = 9092
    editPorts.api = 8081
    editPorts.ws = 8080
    store.addToast('표준 포트', '표준 포트(3306, 9092, 8081, 8080)가 입력되었습니다.', 'info')
  }
  portStatus.mysql = null
  portStatus.kafka = null
  portStatus.api = null
  portStatus.ws = null
}

// 11. 포트 설정 저장 및 설정 파일 동기화
async function savePorts() {
  const portList = [editPorts.mysql, editPorts.kafka, editPorts.api, editPorts.ws]
  for (const p of portList) {
    if (!p || p < 1024 || p > 65535) {
      store.addToast('포트 범위 오류', '모든 포트는 1024~65535 범위 내의 숫자여야 합니다.', 'error')
      return
    }
  }

  const unique = new Set(portList)
  if (unique.size !== portList.length) {
    store.addToast('포트 중복 오류', '각 서비스의 포트 번호는 서로 달라야 합니다.', 'error')
    return
  }

  isSavingPorts.value = true
  try {
    const res = await fetch('/api/infra/ports', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        mysql: editPorts.mysql,
        kafka: editPorts.kafka,
        api: editPorts.api,
        ws: editPorts.ws
      })
    })

    const result = await res.json()
    if (result.success) {
      store.addToast('포트 저장 및 동기화 완료', result.message, 'success')
      await fetchPorts()
      await checkStatus()
    } else {
      store.addToast('저장 실패', result.message || '포트 저장 중 오류가 발생했습니다.', 'error')
    }
  } catch (err: any) {
    store.addToast('통신 오류', err.message, 'error')
  } finally {
    isSavingPorts.value = false
  }
}

onMounted(() => {
  checkStatus()
  fetchPaths()
  fetchPorts()
  pollingTimer = setInterval(checkStatus, 3000)
})

onUnmounted(() => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
  }
})
</script>
