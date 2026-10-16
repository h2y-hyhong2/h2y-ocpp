<script setup lang="ts">
import { ref, watch } from 'vue'
import { useVirtualCharger } from '../composables/useVirtualCharger'
import type { ChargerInstance } from '../types/ocpp'

const props = defineProps<{
  charger: ChargerInstance | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'saved', charger: ChargerInstance): void
}>()

const store = useVirtualCharger()

const name = ref('')
const wsUrl = ref('')
const vendor = ref('')
const model = ref('')
const powerKw = ref(100)

watch(
  () => props.charger,
  (c) => {
    if (c) {
      name.value = c.name
      wsUrl.value = c.wsUrl
      vendor.value = c.vendor
      model.value = c.model
      powerKw.value = c.powerKw
    }
  },
  { immediate: true }
)

function handleSave() {
  if (!props.charger) return

  store.updateCharger(props.charger.id, {
    name: name.value.trim(),
    wsUrl: wsUrl.value.trim(),
    vendor: vendor.value.trim(),
    model: model.value.trim(),
    powerKw: Number(powerKw.value) || 100
  })

  emit('saved', {
    ...props.charger,
    name: name.value.trim(),
    wsUrl: wsUrl.value.trim(),
    vendor: vendor.value.trim(),
    model: model.value.trim(),
    powerKw: Number(powerKw.value) || 100
  })
  emit('close')
}
</script>

<template>
  <div v-if="charger" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs select-none p-4">
    <div class="bg-white rounded-xl shadow-2xl border border-slate-200 w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-150">
      <!-- 모달 헤더 -->
      <div class="px-4 py-3 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <span class="text-base">✏️</span>
          <h3 class="text-xs font-bold text-slate-900 tracking-tight">가상 충전기 정보 수정</h3>
          <span class="text-[11px] font-mono font-bold text-sky-700 bg-sky-50 px-1.5 py-0.2 rounded border border-sky-200">
            {{ charger.id }}
          </span>
        </div>
        <button
          @click="emit('close')"
          class="text-slate-400 hover:text-slate-600 rounded-md p-1 transition-colors text-sm"
        >
          ✕
        </button>
      </div>

      <!-- 모달 바디 -->
      <div class="p-4 space-y-3 text-xs">
        <!-- 충전기 명칭 -->
        <div>
          <label class="block text-[11px] font-bold text-slate-700 mb-1">
            충전기 명칭
          </label>
          <input
            v-model="name"
            class="w-full bg-slate-50 border border-slate-200 rounded-md px-2.5 py-1.5 text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500"
          />
        </div>

        <!-- WebSocket URL -->
        <div>
          <label class="block text-[11px] font-bold text-slate-700 mb-1">
            WebSocket 엔드포인트 URL
          </label>
          <input
            v-model="wsUrl"
            class="w-full bg-slate-50 border border-slate-200 rounded-md px-2.5 py-1.5 font-mono text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500"
          />
        </div>

        <!-- 제조사 / 모델 / 용량 -->
        <div class="grid grid-cols-3 gap-2">
          <div>
            <label class="block text-[11px] font-medium text-slate-600 mb-1">제조사</label>
            <input
              v-model="vendor"
              class="w-full bg-slate-50 border border-slate-200 rounded-md px-2 py-1.5 text-xs"
            />
          </div>
          <div>
            <label class="block text-[11px] font-medium text-slate-600 mb-1">모델명</label>
            <input
              v-model="model"
              class="w-full bg-slate-50 border border-slate-200 rounded-md px-2 py-1.5 text-xs"
            />
          </div>
          <div>
            <label class="block text-[11px] font-medium text-slate-600 mb-1">용량 (kW)</label>
            <input
              v-model.number="powerKw"
              type="number"
              class="w-full bg-slate-50 border border-slate-200 rounded-md px-2 py-1.5 font-mono text-xs"
            />
          </div>
        </div>
      </div>

      <!-- 모달 푸터 -->
      <div class="px-4 py-2.5 bg-slate-50 border-t border-slate-200 flex items-center justify-end gap-2">
        <button
          @click="emit('close')"
          class="px-3 py-1.5 text-xs font-semibold rounded-md border border-slate-200 text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
        >
          취소
        </button>
        <button
          @click="handleSave"
          class="px-3.5 py-1.5 text-xs font-bold rounded-md bg-sky-600 hover:bg-sky-700 text-white shadow-2xs transition-colors cursor-pointer"
        >
          저장 완료
        </button>
      </div>
    </div>
  </div>
</template>
