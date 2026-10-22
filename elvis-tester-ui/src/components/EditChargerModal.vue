<script setup lang="ts">
import ChargerMasterModal from './ChargerMasterModal.vue'
import type { ChargerInstance } from '../types/ocpp'

defineProps<{
  charger: ChargerInstance | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'saved', charger: ChargerInstance): void
}>()
</script>

<template>
  <ChargerMasterModal
    :charger="charger"
    @close="emit('close')"
    @saved="(c) => emit('saved', c)"
  />
</template>
