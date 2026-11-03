import { useAuthStore } from '@/stores/authStore'
import router from '@/router'

/**
 * CSMS Lite JWT 인증 헤더 자동 주입 및 401 처리 Fetch 유틸리티
 */
export async function authFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const authStore = useAuthStore()

  const headers = new Headers(init?.headers || {})
  if (authStore.accessToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${authStore.accessToken}`)
  }

  const updatedInit: RequestInit = {
    ...init,
    headers
  }

  const response = await fetch(input, updatedInit)

  if (response.status === 401) {
    console.warn('[authFetch] 401 Unauthorized 감지 -> 인증 만료 처리')
    authStore.logout()
    if (router.currentRoute.value.path !== '/login') {
      router.push({
        path: '/login',
        query: { redirect: router.currentRoute.value.fullPath }
      })
    }
  }

  return response
}

