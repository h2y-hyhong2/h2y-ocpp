<script setup lang="ts">
import { ref } from 'vue'
import SimpleHeader from './components/SimpleHeader.vue'
import SimpleWorkspaceView from './views/SimpleWorkspaceView.vue'

const isTerminalOpen = ref(true)
</script>

<template>
  <div class="h-screen w-screen flex flex-col overflow-hidden bg-slate-100 text-slate-800 font-sans select-none selection:bg-sky-500/20 selection:text-sky-900">
    <!-- LS E-LINK 마스터 브랜드 시그니처 악센트 라인 -->
    <div class="h-[2.5px] w-full bg-gradient-to-r from-[#0A1E5A] via-[#FA002D] to-[#E96600] flex-shrink-0 z-50"></div>

    <!-- 컴팩트 상단 GNB 헤더 -->
    <SimpleHeader
      :is-terminal-open="isTerminalOpen"
      @toggle-terminal="isTerminalOpen = !isTerminalOpen"
    />

    <!-- 메인 3단 워크스페이스 영역 (좌측 충전기 목록 + 중앙 제어 패널 + 우측 실시간 패킷 터미널) -->
    <div class="flex-1 flex overflow-hidden min-h-0 relative">
      <SimpleWorkspaceView
        :is-terminal-open="isTerminalOpen"
        @update:is-terminal-open="val => isTerminalOpen = val"
      />
    </div>
  </div>
</template>
