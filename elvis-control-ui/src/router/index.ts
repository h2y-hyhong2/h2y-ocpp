import { createRouter, createWebHistory } from 'vue-router'
import ChargersView from '@/views/ChargersView.vue'
import BillingCdrView from '@/views/BillingCdrView.vue'
import LoginView from '@/views/LoginView.vue'
import { useAuthStore } from '@/stores/authStore'

/**
 * CSMS Lite 관제 라우터 테이블
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { title: '로그인', requiresAuth: false }
  },
  {
    path: '/',
    name: 'Chargers',
    component: ChargersView,
    meta: { title: '실시간 충전기 통합 관제', requiresAuth: true }
  },
  {
    path: '/billing',
    name: 'BillingCdr',
    component: BillingCdrView,
    meta: { title: '충전 세션 & 과금 원장', requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 네비게이션 가드: 미인증 접근 제어
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth !== false)

  if (requiresAuth && !authStore.isAuthenticated) {
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  } else if (to.path === '/login' && authStore.isAuthenticated) {
    next('/')
  } else {
    next()
  }
})

export default router
