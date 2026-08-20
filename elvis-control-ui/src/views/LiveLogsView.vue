<template>
  <div class="space-y-4 flex flex-col h-[calc(100vh-100px)] pb-4">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <h1 class="text-xl font-bold font-display text-slate-800 dark:text-slate-100 tracking-tight">OCPP 실시간 패킷 로그 (WireTap)</h1>
        <span class="font-mono text-xs px-2 py-0.5 rounded-md font-semibold bg-sky-500/10 text-sky-600 dark:text-sky-400 border border-sky-500/30">WIRETAP-01</span>
      </div>

      <div class="flex items-center gap-3">
        <!-- Pure Tailwind Toggle Switch -->
        <label class="inline-flex items-center gap-2 cursor-pointer select-none text-xs text-slate-500">
          <input type="checkbox" v-model="autoScroll" class="sr-only peer" />
          <div class="w-8 h-4 bg-slate-300 dark:bg-slate-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-3 after:w-3 after:transition-all peer-checked:bg-sky-500 relative"></div>
          <span>자동 스크롤</span>
        </label>

        <button
          @click="store.recentLogs = []"
          class="px-3 py-1.5 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-600 dark:text-rose-400 border border-rose-500/30 text-xs font-semibold flex items-center gap-1 transition-all"
        >
          <span>🗑️ 로그 비우기</span>
        </button>
      </div>
    </div>

    <!-- Packet Stream Table / Terminal View -->
    <div class="glass-card flex-1 p-4 overflow-hidden flex flex-col font-mono text-xs">
      <div class="overflow-y-auto flex-1 space-y-2.5 p-2 bg-slate-100/70 dark:bg-slate-950/80 rounded-xl border border-slate-200 dark:border-slate-800">
        <div
          v-for="log in store.recentLogs"
          :key="log.id"
          class="p-3 rounded-lg bg-white/90 dark:bg-slate-900/90 border border-slate-200 dark:border-slate-800 flex flex-col gap-2 transition-all hover:border-sky-500/50 shadow-sm"
        >
          <!-- Log Meta Header -->
          <div class="flex items-center justify-between text-[11px]">
            <div class="flex items-center gap-2">
              <span class="text-slate-400">{{ log.timestamp }}</span>
              <span
                :class="log.direction === 'INBOUND' ? 'bg-sky-500/15 text-sky-600 dark:text-sky-400 border-sky-500/30' : 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30'"
                class="px-1.5 py-0.2 rounded border font-bold"
              >
                {{ log.direction === 'INBOUND' ? '⬆ INBOUND' : '⬇ OUTBOUND' }}
              </span>
              <span class="font-bold text-slate-800 dark:text-slate-200">{{ log.chargeBoxId }}</span>
              <span class="text-violet-600 dark:text-violet-400 font-bold">[{{ log.action }}]</span>
            </div>
            <span class="text-slate-400 text-[10px]">MsgType: {{ log.messageType === 2 ? 'Call (2)' : 'CallResult (3)' }}</span>
          </div>

          <!-- Log JSON Payload -->
          <pre class="text-slate-700 dark:text-slate-300 bg-slate-100/90 dark:bg-slate-950 p-2.5 rounded-lg text-[11px] overflow-x-auto border border-slate-200 dark:border-slate-900">{{ formatJson(log.payloadJson) }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useCsmsStore } from '@/stores/csmsStore'

const store = useCsmsStore()
const autoScroll = ref(true)

const formatJson = (jsonStr: string) => {
  try {
    const parsed = JSON.parse(jsonStr)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return jsonStr
  }
}
</script>
