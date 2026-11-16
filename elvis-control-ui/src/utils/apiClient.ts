import { useAuthStore } from '@/stores/authStore'
import router from '@/router'

/**
 * 실행 환경에 맞는 API Base URL 반환
 * - VITE_API_BASE_URL 환경변수 우선
 * - Tauri 프로덕션 빌드에서는 Vite 프록시가 없으므로 기본 백엔드 주소(127.0.0.1:9082) 사용
 */
export function getApiBaseUrl(): string {
  const envBase = import.meta.env.VITE_API_BASE_URL as string | undefined
  if (envBase) {
    return envBase.replace(/\/+$/, '')
  }

  const isTauri =
    typeof window !== 'undefined' &&
    ('__TAURI_INTERNALS__' in window || '__TAURI__' in window)

  if (isTauri && !import.meta.env.DEV) {
    return 'http://127.0.0.1:9082'
  }

  return ''
}

export function resolveApiUrl(path: string): string {
  if (!path || /^https?:\/\//i.test(path)) {
    return path
  }
  const base = getApiBaseUrl()
  if (!base) {
    return path
  }
  return path.startsWith('/') ? `${base}${path}` : `${base}/${path}`
}

let refreshInFlight: Promise<boolean> | null = null

async function refreshTokenSingleFlight(): Promise<boolean> {
  if (!refreshInFlight) {
    const authStore = useAuthStore()
    refreshInFlight = authStore.refreshAccessToken().finally(() => {
      refreshInFlight = null
    })
  }
  return refreshInFlight
}

function redirectToLogin() {
  if (router.currentRoute.value.path !== '/login') {
    router.push({
      path: '/login',
      query: { redirect: router.currentRoute.value.fullPath }
    })
  }
}

/**
 * CSMS Lite JWT 인증 헤더 자동 주입 및 401 단일 플라이트 토큰 갱신 Fetch 유틸리티
 */
export async function authFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const authStore = useAuthStore()
  const target: RequestInfo | URL = typeof input === 'string' ? resolveApiUrl(input) : input

  const headers = new Headers(init?.headers || {})
  if (authStore.accessToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${authStore.accessToken}`)
  }

  const response = await fetch(target, {
    ...init,
    headers
  })

  if (response.status !== 401) {
    return response
  }

  const requestUrl = typeof input === 'string' ? input : input instanceof URL ? input.pathname : input.url
  const isAuthEndpoint = requestUrl.includes('/api/v1/auth/login') || requestUrl.includes('/api/v1/auth/refresh')
  if (isAuthEndpoint) {
    return response
  }

  const refreshed = await refreshTokenSingleFlight()
  if (refreshed && authStore.accessToken) {
    const retryHeaders = new Headers(init?.headers || {})
    retryHeaders.set('Authorization', `Bearer ${authStore.accessToken}`)

    const retryResponse = await fetch(target, {
      ...init,
      headers: retryHeaders
    })

    if (retryResponse.status !== 401) {
      return retryResponse
    }

    console.warn('[authFetch] 토큰 갱신 후 재요청에서도 401 응답 -> 로그아웃 처리')
    authStore.logout()
    redirectToLogin()
    return retryResponse
  }

  console.warn('[authFetch] 401 Unauthorized 감지 및 토큰 갱신 불가 -> 인증 만료 처리')
  authStore.logout()
  redirectToLogin()
  return response
}
