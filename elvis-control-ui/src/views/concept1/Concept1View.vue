<script setup lang="ts">
/**
 * ============================================================================
 * [디자인 컨셉 1] 표준 분할 제어형 (Split View)
 * ============================================================================
 * - 특징: 좌측 충전소 트리 + 중앙 충전기 상태 그리드 + 우측 슬라이드 제어반
 * - 디자인 테마: Enterprise Modern (./concept1.css)
 * - 사용자가 ./concept1.css 파일 및 이 컴포넌트의 HTML/Tailwind 클래스를 수정하여 디자인을 자유롭게 정의할 수 있습니다.
 */
import ChargersView from '../ChargersView.vue'
import './concept1.css'
</script>

<template>
  <!-- 컨셉 1 루트 래퍼 (concept1-theme 스코프 적용) -->
  <div class="concept1-theme concept1-container h-full w-full">
    <ChargersView />
  </div>
</template>

<style scoped>
/* 컨셉 1 전용 커스텀 스타일 (필요 시 수정) */
.concept1-container {
  position: relative;
}
</style>
