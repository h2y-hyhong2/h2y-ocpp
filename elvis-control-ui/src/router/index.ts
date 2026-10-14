import { createRouter, createWebHistory } from 'vue-router'
import ChargersView from '@/views/ChargersView.vue'
import DashboardView from '@/views/DashboardView.vue'
import MeterValuesView from '@/views/MeterValuesView.vue'
import BillingCdrView from '@/views/BillingCdrView.vue'
import LiveLogsView from '@/views/LiveLogsView.vue'
import AdminSettingsView from '@/views/AdminSettingsView.vue'

const routes = [
  {
    path: '/',
    name: 'Chargers',
    component: ChargersView,
    meta: { title: '실시간 충전기 통합 관제' }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: DashboardView,
    meta: { title: '실시간 관제 대시보드' }
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
  },
  {
    path: '/settings',
    name: 'AdminSettings',
    component: AdminSettingsView,
    meta: { title: '시스템 관리자 & 인프라 설정' }
  },
  // 이전 시안 경로 접속 시 루트(/)로 리다이렉트
  {
    path: '/concept1',
    redirect: '/'
  },
  {
    path: '/concept2',
    redirect: '/'
  },
  {
    path: '/concept3',
    redirect: '/'
  },
  {
    path: '/chargers',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
