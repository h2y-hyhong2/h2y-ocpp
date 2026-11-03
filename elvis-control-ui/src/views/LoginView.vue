<template>
  <div class="min-h-screen w-full flex items-center justify-center bg-slate-50 p-4 font-sans select-none">
    <div class="w-full max-w-sm bg-white border border-slate-200 rounded-xl p-8 shadow-sm">
      
      <!-- 단순 타이틀 영역 (CI 아이콘 포함) -->
      <div class="mb-6 text-center">
        <div class="inline-flex items-center justify-center gap-2 mb-1">
          <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-sky-500 to-indigo-600 flex items-center justify-center text-white shadow-xs">
            <svg class="w-4.5 h-4.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <h1 class="text-xl font-bold text-slate-900 tracking-tight">ELVIS LITE</h1>
        </div>
        <p class="text-xs text-slate-500">충전 관제 시스템 로그인</p>
      </div>

      <!-- 에러 알림 메시지 -->
      <div
        v-if="errorMessage"
        class="mb-4 p-2.5 rounded-lg bg-rose-50 border border-rose-200 text-xs text-rose-600 font-medium flex items-center gap-2"
      >
        <svg class="w-4 h-4 shrink-0 text-rose-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span>{{ errorMessage }}</span>
      </div>

      <!-- 로그인 폼 (아이디, 비밀번호 영역) -->
      <form @submit.prevent="handleLogin" class="space-y-4">
        <div>
          <label class="block text-xs font-semibold text-slate-700 mb-1.5">아이디</label>
          <input
            type="text"
            v-model="username"
            required
            autocomplete="username"
            placeholder="아이디를 입력하세요"
            class="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all font-mono"
          />
        </div>

        <div>
          <label class="block text-xs font-semibold text-slate-700 mb-1.5">비밀번호</label>
          <input
            type="password"
            v-model="password"
            required
            autocomplete="current-password"
            placeholder="비밀번호를 입력하세요"
            class="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all font-mono"
          />
        </div>

        <button
          type="submit"
          :disabled="isLoading"
          class="w-full mt-2 py-2.5 px-4 bg-blue-600 hover:bg-blue-700 active:bg-blue-800 disabled:bg-blue-400 text-white font-semibold text-sm rounded-lg shadow-2xs transition-colors flex items-center justify-center gap-2 cursor-pointer"
        >
          <svg
            v-if="isLoading"
            class="animate-spin w-4 h-4 text-white"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
          </svg>
          <span>{{ isLoading ? '로그인 중...' : '로그인' }}</span>
        </button>
      </form>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const isLoading = ref(false)
const errorMessage = ref('')

async function handleLogin() {
  if (!username.value.trim() || !password.value.trim()) {
    errorMessage.value = '아이디와 비밀번호를 모두 입력해 주십시오.'
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  try {
    const res = await authStore.login(username.value, password.value)
    if (res.success) {
      const redirectPath = (route.query.redirect as string) || '/'
      router.push(redirectPath)
    } else {
      errorMessage.value = res.message || '로그인에 실패했습니다.'
    }
  } catch (err: any) {
    errorMessage.value = '로그인 처리 중 오류가 발생했습니다.'
  } finally {
    isLoading.value = false
  }
}
</script>

