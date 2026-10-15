<script setup lang="ts">
import { ref } from 'vue'
import { useVirtualCharger } from './composables/useVirtualCharger'
import SimpleHeader from './components/SimpleHeader.vue'
import ActionControlPanel from './components/ActionControlPanel.vue'
import FleetRunner from './components/FleetRunner.vue'
import PacketTerminal from './components/PacketTerminal.vue'

const currentTab = ref<'single' | 'fleet'>('single')
const isTerminalOpen = ref(true)
const charger = useVirtualCharger()
</script>

<template>
  <div class="h-screen w-screen flex flex-col overflow-hidden bg-slate-50 text-slate-800 font-sans select-none selection:bg-sky-500/20 selection:text-sky-900">
    <!-- LS E-LINK 마스터 브랜드 시그니처 악센트 라인 -->
    <div class="h-[2.5px] w-full bg-gradient-to-r from-[#0A1E5A] via-[#FA002D] to-[#E96600] flex-shrink-0 z-50"></div>

    <!-- 컴팩트 상단 GNB 헤더 -->
    <SimpleHeader
      :state="charger.state"
      :current-tab="currentTab"
      :is-terminal-open="isTerminalOpen"
      :packet-count="charger.logs.value.length"
      @update:current-tab="tab => currentTab = tab"
      @toggle-terminal="isTerminalOpen = !isTerminalOpen"
    />

    <!-- 메인 워크스페이스 영역 (좌측 제어 패널 + 우측 실시간 패킷 터미널) -->
    <main class="flex-1 flex overflow-hidden p-3 gap-3 min-h-0">
      <!-- 좌측: 컨트롤 패널 (단일 단말 또는 플릿 모드) -->
      <section
        class="overflow-y-auto flex-1 min-h-0 transition-all duration-200"
        :class="isTerminalOpen ? 'lg:w-7/12' : 'w-full'"
      >
        <ActionControlPanel v-if="currentTab === 'single'" :charger="charger" />
        <FleetRunner v-else />
      </section>

      <!-- 우측: 실시간 WireTap 패킷 터미널 -->
      <aside
        v-if="isTerminalOpen"
        class="hidden lg:flex lg:w-5/12 min-h-0 h-full flex-col flex-shrink-0 transition-all duration-200"
      >
        <PacketTerminal
          :logs="charger.logs.value"
          :is-side-panel="true"
          @clear="charger.clearLogs"
          @close="isTerminalOpen = false"
        />
      </aside>
    </main>

    <!-- 모바일/태블릿 화면용 하단 패킷 터미널 모달 (1024px 미만에서 터미널 활성화 시 노출) -->
    <div
      v-if="isTerminalOpen"
      class="lg:hidden fixed inset-x-0 bottom-0 z-50 h-[70vh] bg-white border-t border-slate-300 shadow-2xl p-2"
    >
      <PacketTerminal
        :logs="charger.logs.value"
        :is-side-panel="true"
        @clear="charger.clearLogs"
        @close="isTerminalOpen = false"
      />
    </div>
  </div>
</template>
