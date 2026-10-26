import { createRouter, createWebHistory } from 'vue-router'
import ChargersView from '@/views/ChargersView.vue'
import BillingCdrView from '@/views/BillingCdrView.vue'

/**
 * CSMS Lite 관제 라우터 테이블 (심플 2대 핵심 메뉴 체계)
 */
const routes = [
  {
    path: '/',
    name: 'Chargers',
    component: ChargersView,
    meta: { title: '실시간 충전기 통합 관제' }
  },
  {
    path: '/billing',
    name: 'BillingCdr',
    component: BillingCdrView,
    meta: { title: '충전 세션 & 과금 원장' }
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

export default router
