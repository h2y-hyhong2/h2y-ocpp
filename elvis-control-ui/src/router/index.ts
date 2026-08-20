import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '@/views/DashboardView.vue'
import ChargersView from '@/views/ChargersView.vue'
import MeterValuesView from '@/views/MeterValuesView.vue'
import BillingCdrView from '@/views/BillingCdrView.vue'
import LiveLogsView from '@/views/LiveLogsView.vue'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: DashboardView,
    meta: { title: '실시간 관제 대시보드' }
  },
  {
    path: '/chargers',
    name: 'Chargers',
    component: ChargersView,
    meta: { title: '충전기 모니터링 & 원격 제어' }
  },
  {
    path: '/metrics',
    name: 'MeterValues',
    component: MeterValuesView,
    meta: { title: '1초 시계열 미터값 분석' }
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
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
