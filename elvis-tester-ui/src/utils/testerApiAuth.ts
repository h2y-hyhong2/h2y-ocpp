export const TESTER_API_BASE_URL = 'http://127.0.0.1:9082'

let cachedAccessToken: string | null = null
let loginInFlight: Promise<string | null> | null = null

const SEED_ACCOUNTS: Array<{ username: string; password: string }> = [
  { username: 'viewer', password: 'viewer123!' },
  { username: 'admin', password: 'admin123!' }
]

async function obtainAccessToken(): Promise<string | null> {
  if (!loginInFlight) {
    loginInFlight = (async () => {
      for (const account of SEED_ACCOUNTS) {
        try {
          const resp = await fetch(`${TESTER_API_BASE_URL}/api/v1/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(account)
          })
          if (!resp.ok) continue
          const json = await resp.json()
          const token = json?.data?.accessToken || json?.accessToken
          if (typeof token === 'string' && token.length > 0) {
            cachedAccessToken = token
            return token
          }
        } catch {
          // 네트워크 연결 실패 시 다음 시도 중단
          return null
        }
      }
      return null
    })().finally(() => {
      loginInFlight = null
    })
  }
  return loginInFlight
}

/**
 * elvis-tester-ui 전용 제어 API 호출 유틸리티
 * - JWT 인증이 필요한 조회 API 호출 시 기본 조회 계정(viewer) 토큰을 자동 발급/주입
 * - 401 응답 시 토큰 캐시를 비우고 1회 재발급 재시도
 */
export async function testerAuthFetch(url: string, init?: RequestInit): Promise<Response> {
  const token = cachedAccessToken || (await obtainAccessToken())
  const headers = new Headers(init?.headers || {})
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const resp = await fetch(url, { ...init, headers })
  if (resp.status !== 401) {
    return resp
  }

  cachedAccessToken = null
  const freshToken = await obtainAccessToken()
  if (!freshToken) {
    return resp
  }

  const retryHeaders = new Headers(init?.headers || {})
  retryHeaders.set('Authorization', `Bearer ${freshToken}`)
  return fetch(url, { ...init, headers: retryHeaders })
}
