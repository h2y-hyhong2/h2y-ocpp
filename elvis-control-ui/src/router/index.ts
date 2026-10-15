import { createRouter, createWebHistory } from 'vue-router'
import ChargersView from '@/views/ChargersView.vue'
import BillingCdrView from '@/views/BillingCdrView.vue'
import LiveLogsView from '@/views/LiveLogsView.vue'

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
    path: '/logs',
    name: 'LiveLogs',
    component: LiveLogsView,
    meta: { title: 'OCPP 실시간 패킷 로그' }
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
