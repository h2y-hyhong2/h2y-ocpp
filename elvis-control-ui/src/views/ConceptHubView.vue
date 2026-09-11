<script setup lang="ts">
/**
 * ============================================================================
 * [컨셉 허브 뷰] 3개 디자인 컨셉 통합 전환 래퍼
 * ============================================================================
 * - store.activeConcept 상태에 따라 Concept 1 / 2 / 3을 즉각 스위칭합니다.
 */
import { useCsmsStore } from '@/stores/csmsStore'
import Concept1View from './concept1/Concept1View.vue'
import Concept2View from './concept2/Concept2View.vue'
import Concept3View from './concept3/Concept3View.vue'

const store = useCsmsStore()
</script>

<template>
  <div class="concept-hub-root w-full h-full overflow-hidden">
    <!-- 시안 1: 표준 분할 제어형 -->
    <Concept1View v-if="store.activeConcept === 'concept1'" />

    <!-- 시안 2: 대형 상황판 매트릭스형 -->
    <Concept2View v-else-if="store.activeConcept === 'concept2'" />

    <!-- 시안 3: 전력 콕핏 / 에너지 분석형 -->
    <Concept3View v-else-if="store.activeConcept === 'concept3'" />
  </div>
</template>

<style scoped>
.concept-hub-root {
  position: relative;
}
</style>
