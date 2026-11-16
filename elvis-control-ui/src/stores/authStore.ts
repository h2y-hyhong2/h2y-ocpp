import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { resolveApiUrl } from '@/utils/apiClient'

export interface LoginResult {
  success: boolean
  message?: string
}

interface JwtPayload {
  exp?: number
  [key: string]: unknown
}

function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const base64Url = parts[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')
    const binary = atob(padded)
    const bytes = Uint8Array.from(binary, c => c.charCodeAt(0))
    const jsonStr = new TextDecoder().decode(bytes)
    return JSON.parse(jsonStr) as JwtPayload
  } catch {
    return null
  }
}

export function isJwtExpired(token: string | null): boolean {
  if (!token) return true
  const payload = decodeJwtPayload(token)
  if (!payload) return true
  if (typeof payload.exp !== 'number') return false
  return Date.now() >= payload.exp * 1000
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem('csms_access_token'))
  const refreshToken = ref<string | null>(localStorage.getItem('csms_refresh_token'))
  const username = ref<string | null>(localStorage.getItem('csms_username'))
  const role = ref<string | null>(localStorage.getItem('csms_role'))

  const isAuthenticated = computed(() => !!accessToken.value && !isJwtExpired(accessToken.value))

  function persistSession(data: {
    accessToken: string
    refreshToken?: string
    username?: string
    role?: string
  }) {
    accessToken.value = data.accessToken
    localStorage.setItem('csms_access_token', data.accessToken)

    if (data.refreshToken) {
      refreshToken.value = data.refreshToken
      localStorage.setItem('csms_refresh_token', data.refreshToken)
    }
    if (data.username) {
      username.value = data.username
      localStorage.setItem('csms_username', data.username)
    }
    if (data.role) {
      role.value = data.role
      localStorage.setItem('csms_role', data.role)
    }
  }

  async function login(u: string, p: string): Promise<LoginResult> {
    try {
      const res = await fetch(resolveApiUrl('/api/v1/auth/login'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          username: u.trim(),
          password: p.trim()
        })
      })

      const result = await res.json()

      if (res.ok && result.success && result.data) {
        persistSession(result.data)
        return { success: true }
      }

      return {
        success: false,
        message: result.message || '아이디 또는 비밀번호가 올바르지 않습니다.'
      }
    } catch (err: any) {
      console.error('[authStore] 로그인 요청 오류:', err)
      return {
        success: false,
        message: '서버와 통신할 수 없습니다. 백엔드 상태를 확인해 주십시오.'
      }
    }
  }

  async function refreshAccessToken(): Promise<boolean> {
    const currentRefresh = refreshToken.value
    if (!currentRefresh || isJwtExpired(currentRefresh)) {
      return false
    }

    try {
      const res = await fetch(resolveApiUrl('/api/v1/auth/refresh'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          refreshToken: currentRefresh
        })
      })

      if (!res.ok) {
        return false
      }

      const result = await res.json()
      if (result?.success && result?.data?.accessToken) {
        persistSession(result.data)
        return true
      }

      return false
    } catch (err) {
      console.error('[authStore] 토큰 재발급 요청 오류:', err)
      return false
    }
  }

  function logout() {
    accessToken.value = null
    refreshToken.value = null
    username.value = null
    role.value = null

    localStorage.removeItem('csms_access_token')
    localStorage.removeItem('csms_refresh_token')
    localStorage.removeItem('csms_username')
    localStorage.removeItem('csms_role')
  }

  return {
    accessToken,
    refreshToken,
    username,
    role,
    isAuthenticated,
    login,
    refreshAccessToken,
    logout
  }
})
