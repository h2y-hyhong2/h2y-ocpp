<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface Template {
  id: string
  name: string
  action: string
  defaultTopic: string
  description: string
  samplePayload: string
}

const emit = defineEmits<{
  (e: 'sent', result: any): void
}>()

const topics = [
  { id: 'ocpp-raw-events', name: 'ocpp-raw-events (Inbound/상향)' },
  { id: 'ocpp-outbound-commands', name: 'ocpp-outbound-commands (Outbound/하향)' },
  { id: 'ocpp-ui-notifications', name: 'ocpp-ui-notifications (UI 알림)' },
  { id: 'ocpp-raw-events.DLT', name: 'ocpp-raw-events.DLT (데드레터)' }
]

const templates = ref<Template[]>([])
const selectedTopic = ref('ocpp-raw-events')
const chargeBoxId = ref('CP_TEST_001')
const selectedTemplateId = ref('')
const payloadText = ref('')
const sending = ref(false)
const statusMessage = ref<{ type: 'success' | 'error'; text: string } | null>(null)

onMounted(async () => {
  try {
    const res = await fetch('/api/tester/templates')
    if (res.ok) {
      templates.value = await res.json()
      if (templates.value.length > 0) {
        selectTemplate(templates.value[0])
      }
    }
  } catch (err) {
    console.error('템플릿 조회 실패:', err)
  }
})

function selectTemplate(tmpl: Template) {
  selectedTemplateId.value = tmpl.id
  selectedTopic.value = tmpl.defaultTopic
  payloadText.value = tmpl.samplePayload
}

function onTemplateChange() {
  const found = templates.value.find(t => t.id === selectedTemplateId.value)
  if (found) {
    selectTemplate(found)
  }
}

async function handleSend() {
  sending.value = true
  statusMessage.value = null
  try {
    const res = await fetch('/api/tester/send', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        topic: selectedTopic.value,
        chargeBoxId: chargeBoxId.value,
        payload: payloadText.value
      })
    })

    const result = await res.json()
    if (res.ok && result.status === 'SUCCESS') {
      statusMessage.value = { type: 'success', text: `발송 완료 (토픽: ${selectedTopic.value}, ID: ${result.id.substring(0, 8)})` }
      emit('sent', result)
    } else {
      statusMessage.value = { type: 'error', text: `발송 실패: ${result.message || '서버 오류'}` }
    }
  } catch (e: any) {
    statusMessage.value = { type: 'error', text: `네트워크 에러: ${e.message}` }
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-xs dark:shadow-xl flex flex-col gap-5 transition-colors duration-200">
    <div class="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-4">
      <div class="flex items-center gap-2">
        <span class="w-3 h-3 rounded-full bg-emerald-500 animate-pulse"></span>
        <h2 class="text-lg font-semibold text-slate-800 dark:text-slate-100">Kafka 패킷 즉시 발송기 (Direct Injector)</h2>
      </div>
      <span class="text-xs font-mono text-slate-500 dark:text-slate-400 bg-slate-100 dark:bg-slate-800/80 px-2.5 py-1 rounded border border-slate-200 dark:border-transparent">Single Message Producer</span>
    </div>

    <!-- 설정 영역 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <!-- 템플릿 선택 -->
      <div>
        <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1.5">OCPP 메시지 템플릿</label>
        <select
          v-model="selectedTemplateId"
          @change="onTemplateChange"
          class="w-full bg-slate-50 dark:bg-slate-950 border border-slate-300 dark:border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-800 dark:text-slate-200 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition"
        >
          <option v-for="t in templates" :key="t.id" :value="t.id">
            {{ t.name }}
          </option>
        </select>
      </div>

      <!-- 타겟 토픽 -->
      <div>
        <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1.5">발행 대상 Kafka 토픽</label>
        <select
          v-model="selectedTopic"
          class="w-full bg-slate-50 dark:bg-slate-950 border border-slate-300 dark:border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-800 dark:text-slate-200 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 font-mono transition"
        >
          <option v-for="topic in topics" :key="topic.id" :value="topic.id">
            {{ topic.name }}
          </option>
        </select>
      </div>

      <!-- 충전기 ID -->
      <div>
        <label class="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1.5">파티션 키 (ChargeBoxId)</label>
        <input
          v-model="chargeBoxId"
          type="text"
          placeholder="예: CP_TEST_001"
          class="w-full bg-slate-50 dark:bg-slate-950 border border-slate-300 dark:border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-800 dark:text-slate-200 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 font-mono transition"
        />
      </div>
    </div>

    <!-- 페이로드 에디터 -->
    <div class="flex flex-col gap-1.5">
      <div class="flex justify-between items-center">
        <label class="text-xs font-medium text-slate-600 dark:text-slate-400">JSON Payload (직접 수정 가능)</label>
        <button
          @click="onTemplateChange"
          class="text-xs text-indigo-600 dark:text-indigo-400 hover:underline transition"
        >
          템플릿 초기화
        </button>
      </div>
      <textarea
        v-model="payloadText"
        rows="9"
        class="w-full bg-slate-950 border border-slate-800 dark:border-slate-700 rounded-lg p-3 text-xs font-mono text-emerald-400 focus:outline-none focus:border-indigo-500 resize-none shadow-inner"
        spellcheck="false"
      ></textarea>
    </div>

    <!-- 하단 액션 버튼 및 상태 메시지 -->
    <div class="flex items-center justify-between pt-2 border-t border-slate-200 dark:border-slate-800/80">
      <div class="text-xs">
        <span v-if="statusMessage" :class="statusMessage.type === 'success' ? 'text-emerald-600 dark:text-emerald-400 font-medium' : 'text-rose-600 dark:text-rose-400 font-medium'">
          {{ statusMessage.text }}
        </span>
      </div>
      <button
        @click="handleSend"
        :disabled="sending"
        class="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-400 dark:disabled:bg-slate-700 text-white font-medium text-sm px-5 py-2.5 rounded-lg shadow-md shadow-indigo-600/20 transition active:scale-95 cursor-pointer disabled:cursor-not-allowed"
      >
        <svg v-if="sending" class="animate-spin h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"></path>
        </svg>
        <span>{{ sending ? '발송 중...' : '🚀 Kafka로 즉시 발송' }}</span>
      </button>
    </div>
  </div>
</template>
