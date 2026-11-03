import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface LoginResult {
  success: boolean
  message?: string
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem('csms_access_token'))
  const refreshToken = ref<string | null>(localStorage.getItem('csms_refresh_token'))
  const username = ref<string | null>(localStorage.getItem('csms_username'))
  const role = ref<string | null>(localStorage.getItem('csms_role'))

  const isAuthenticated = computed(() => !!accessToken.value)

  async function login(u: string, p: string): Promise<LoginResult> {
    try {
      const res = await fetch('/api/v1/auth/login', {
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
        const data = result.data
        accessToken.value = data.accessToken
        refreshToken.value = data.refreshToken
        username.value = data.username
        role.value = data.role

        localStorage.setItem('csms_access_token', data.accessToken)
        localStorage.setItem('csms_refresh_token', data.refreshToken)
        localStorage.setItem('csms_username', data.username)
        localStorage.setItem('csms_role', data.role)

        return { success: true }
      } else {
        return {
          success: false,
          message: result.message || '아이디 또는 비밀번호가 올바르지 않습니다.'
        }
      }
    } catch (err: any) {
      console.error('[authStore] 로그인 요청 오류:', err)
      return {
        success: false,
        message: '서버와 통신할 수 없습니다. 백엔드 상태를 확인해 주십시오.'
      }
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
    logout
  }
})

