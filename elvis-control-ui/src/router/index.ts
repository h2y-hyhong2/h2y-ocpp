import { createRouter, createWebHistory } from 'vue-router'
import ConceptHubView from '@/views/ConceptHubView.vue'
import Concept1View from '@/views/concept1/Concept1View.vue'
import Concept2View from '@/views/concept2/Concept2View.vue'
import Concept3View from '@/views/concept3/Concept3View.vue'
import DashboardView from '@/views/DashboardView.vue'
import ChargersView from '@/views/ChargersView.vue'
import MeterValuesView from '@/views/MeterValuesView.vue'
import BillingCdrView from '@/views/BillingCdrView.vue'
import LiveLogsView from '@/views/LiveLogsView.vue'

const routes = [
  {
    path: '/',
    name: 'ConceptHub',
    component: ConceptHubView,
    meta: { title: 'ELVIS 통합 관제 센터 (3대 시안)' }
  },
  {
    path: '/concept1',
    name: 'Concept1',
    component: Concept1View,
    meta: { title: '시안 1: 분할 제어형 관제' }
  },
  {
    path: '/concept2',
    name: 'Concept2',
    component: Concept2View,
    meta: { title: '시안 2: 대형 상황판 매트릭스' }
  },
  {
    path: '/concept3',
    name: 'Concept3',
    component: Concept3View,
    meta: { title: '시안 3: 전력 콕핏 & 분석' }
  },
  {
    path: '/chargers',
    name: 'ChargersDirect',
    component: ChargersView,
    meta: { title: '충전기 모니터링 & 원격 제어 (기존)' }
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
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
