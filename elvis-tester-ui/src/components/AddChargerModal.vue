<script setup lang="ts">
import { ref, computed } from 'vue'
import { useVirtualCharger } from '../composables/useVirtualCharger'

const emit = defineEmits<{
  (e: 'close'): void
}>()

const store = useVirtualCharger()

const stationId = ref('000000')
const chargerId = ref('11')
const name = ref('')
const wsUrl = ref('ws://127.0.0.1:9080/ocpp')
const vendor = ref('LS E-Link')
const model = ref('ELVIS-HPC-350K')
const powerKw = ref(350)

const combinedId = computed(() => {
  return `${stationId.value.padStart(6, '0')}${chargerId.value.padStart(2, '0')}`
})

function handleSave() {
  const fullId = combinedId.value
  if (fullId.length !== 8) {
    alert('충전소 6자리 + 충전기 2자리(총 8자리)를 올바르게 입력해주세요.')
    return
  }

  const defaultName = name.value.trim() || `충전소 ${stationId.value} - ${chargerId.value}호기 (${powerKw.value}kW)`

  const success = store.addCharger({
    id: fullId,
    stationId: stationId.value.padStart(6, '0'),
    chargerId: chargerId.value.padStart(2, '0'),
    name: defaultName,
    wsUrl: wsUrl.value.trim(),
    vendor: vendor.value.trim(),
    model: model.value.trim(),
    powerKw: Number(powerKw.value) || 100
  })

  if (success) {
    emit('close')
  }
}
</script>

<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs select-none p-4">
    <div class="bg-white rounded-xl shadow-2xl border border-slate-200 w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-150">
      <!-- 모달 헤더 -->
      <div class="px-4 py-3 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <span class="text-base">➕</span>
          <h3 class="text-xs font-bold text-slate-900 tracking-tight">신규 가상 충전기 인스턴스 등록</h3>
        </div>
        <button
          @click="emit('close')"
          class="text-slate-400 hover:text-slate-600 rounded-md p-1 transition-colors text-sm"
        >
          ✕
        </button>
      </div>

      <!-- 모달 바디 -->
      <div class="p-4 space-y-3.5 text-xs">
        <!-- 결합 식별자 프리뷰 -->
        <div class="p-2.5 rounded-lg bg-sky-50 border border-sky-200 flex items-center justify-between font-mono">
          <div class="flex flex-col">
            <span class="text-[10px] text-sky-700 font-bold uppercase tracking-wider">충전기 결합 식별자 (chargeBoxId)</span>
            <span class="text-sm font-black text-sky-900">{{ combinedId }}</span>
          </div>
          <span class="text-[10px] px-1.5 py-0.5 rounded bg-sky-200 text-sky-800 font-bold">
            CSMS Lite 8자리
          </span>
        </div>

        <!-- 충전소 ID & 충전기 ID -->
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label class="block text-[11px] font-bold text-slate-700 mb-1">
              충전소 ID (6자리) <span class="text-rose-500">*</span>
            </label>
            <input
              v-model="stationId"
              maxlength="6"
              placeholder="000000"
              class="w-full bg-slate-50 border border-slate-200 rounded-md px-2.5 py-1.5 font-mono text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500"
            />
          </div>
          <div>
            <label class="block text-[11px] font-bold text-slate-700 mb-1">
              충전기 ID (2자리) <span class="text-rose-500">*</span>
            </label>
            <input
              v-model="chargerId"
              maxlength="2"
              placeholder="01"
              class="w-full bg-slate-50 border border-slate-200 rounded-md px-2.5 py-1.5 font-mono text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500"
            />
          </div>
        </div>

        <!-- 충전기 명칭 -->
        <div>
          <label class="block text-[11px] font-bold text-slate-700 mb-1">
            충전기 명칭
          </label>
          <input
            v-model="name"
            placeholder="예: 강남 테헤란 11호기 (350kW 초급속)"
            class="w-full bg-slate-50 border border-slate-200 rounded-md px-2.5 py-1.5 text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500"
          />
        </div>

        <!-- WebSocket URL -->
        <div>
          <label class="block text-[11px] font-bold text-slate-700 mb-1">
            CSMS WebSocket 엔드포인트 URL
          </label>
          <input
            v-model="wsUrl"
            class="w-full bg-slate-50 border border-slate-200 rounded-md px-2.5 py-1.5 font-mono text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500"
          />
        </div>

        <!-- 제조사 / 모델 / 용량 -->
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-2">
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
          등록 완료
        </button>
      </div>
    </div>
  </div>
</template>
