import { defineStore } from 'pinia'

// 1. 법인 및 충전소 데이터 상수 (프로토타입 constants.js 100% 동일)
export const CORP_LIST = [
  { id: 'CORP_001', name: '(주)LS E-Link (본사 직영)', shortName: 'LS E-Link', tag: '직영', colorClass: 'sky', startIdx: 0, endIdx: 80, badge: '80개소 (400기)' },
  { id: 'CORP_002', name: '서울에너지공사 (공공제휴)', shortName: '서울에너지', tag: '공공', colorClass: 'emerald', startIdx: 80, endIdx: 130, badge: '50개소 (250기)' },
  { id: 'CORP_003', name: '한국모빌리티네트웍스 (민간CPO)', shortName: '모빌리티N', tag: 'CPO', colorClass: 'indigo', startIdx: 130, endIdx: 170, badge: '40개소 (200기)' },
  { id: 'CORP_004', name: '경기운수/물류 하이퍼차징', shortName: '경기운수', tag: '운수', colorClass: 'amber', startIdx: 170, endIdx: 200, badge: '30개소 (150기)' }
]

export const REGION_NAMES_POOL = [
  '서울 강남 테헤란로 하이퍼차징 스테이션', '서울 서초 양재 만남의광장 메가스테이션', '서울 송파 롯데월드몰 스마트존', '서울 영등포 여의도 IFC 복합스테이션',
  '서울 마포 상암 DMC 디지털허브', '서울 용산 KTX 복합환승허브', '서울 종로 광화문 행정복합스테이션', '서울 중구 서울역 메가환승허브',
  '서울 성동 성수 IT밸리 하이퍼존', '서울 강동 고덕 비즈밸리 스마트파크', '서울 구로 G밸리 메가차징존', '서울 금천 가산 디지털단지 허브',
  '서울 강서 마곡 R&D 메가파크', '서울 양천 목동 스마트차징허브', '서울 노원 상계 스마트스테이션', '서울 성북 길음 복합충전센터',
  '서울 서대문 신촌 모빌리티존', '서울 동작 노량진 수산시장 허브', '서울 관악 서울대 벤처타운 허브', '서울 광진 건대입구 복합스테이션',
  '경기 판교 테크노밸리 메가허브', '경기 성남 분당 서현 스마트스테이션', '경기 수원 광교 호수공원 허브', '경기 수원 영통 스마트차징파크',
  '경기 고양 킨텍스 메가스테이션', '경기 고양 일산 호수공원 허브', '경기 화성 동탄역 SRT 메가스테이션', '경기 화성 향남 복합충전센터',
  '경기 용인 기흥 ICT 스마트허브', '경기 용인 수지 복합충전소', '경기 안양 평촌 스마트허브', '경기 안양 인덕원 환승스테이션',
  '경기 부천 중동 스마트복합허브', '경기 광명 KTX역 메가차징존', '경기 평택 고덕 삼성스마트허브', '경기 평택 지제역 SRT 환승센터',
  '경기 남양주 다산 스마트스테이션', '경기 하남 미사 메가차징허브', '경기 파주 운정 스마트스테이션', '경기 시흥 배곧 스마트모빌리티존',
  '경기 김포 한강신도시 차징허브', '경기 의정부 민락 복합스테이션', '경기 과천 지식정보타운 메가허브', '경기 군포 산본 스마트허브',
  '경기 이천 하이닉스 테크노존', '경기 안산 중앙역 환승스테이션', '경기 안성 스타필드 메가스테이션', '경기 오산 세교 복합충전소',
  '경기 양주 옥정 스마트스테이션', '경기 구리 갈매 차징파크',
  '인천 송도 바이오클러스터 메가허브', '인천 청라 로봇랜드 스마트스테이션', '인천공항 T1 하이퍼차징존', '인천공항 T2 복합터미널',
  '인천 부평 국가산단 스마트스테이션', '인천 남동공단 테크노허브', '인천 구월 아시아드 복합존', '인천 검단신도시 스마트스테이션',
  '인천 주안 국가산단 차징존', '인천 계양 테크노밸리 허브',
  '대전 둔산 정부청사 복합허브', '대전 유성 카이스트 스마트허브', '대전 대덕 R&D특구 메가스테이션', '대전 복합터미널 초급속 스테이션',
  '대전 서대전역 환승센터', '세종 정부청사 복합 차징허브', '세종 스마트국가산단 스테이션', '세종 보람 시청 스마트센터',
  '세종 조치원역 환승허브', '세종 나성 모빌리티라운지',
  '충북 청주 오송 KTX 바이오허브', '충북 청주 오창 이차전지특구 허브', '충북 충주 기업도시 스마트스테이션', '충북 제천 KTX 복합충전소',
  '충남 천안아산 KTX 복합환승센터', '충남 천안 불당 스마트스테이션', '충남 아산 탕정 디스플레이시티 허브', '충남 서산 대산산단 스마트허브',
  '충남 당진 현대제철 복합차징존', '충남 보령 대천해수욕장 모빌리티파크',
  '대구 동대구역 복합환승센터', '대구 수성 알파시티 스마트허브', '대구 엑스코 메가스테이션', '대구 성서산단 스마트허브',
  '대구 서대구 KTX 환승복합센터', '대구 칠곡 스마트스테이션', '대구 달서 월배 복합차징파크', '대구 테크노폴리스 R&D 허브',
  '경북 포항 포스코 이차전지허브', '경북 구미 국가산단 스마트스테이션', '경북 경주 KTX역 복합차징존', '경북 경산 스마트테크노허브',
  '경북 안동 경북도청 행정허브', '경북 김천 혁신도시 KTX허브', '경북 영주 스마트스테이션', '경북 칠곡 왜관산단 차징파크',
  '부산 해운대 센텀 하이퍼차징 허브', '부산 해운대 엘시티 스마트허브', '부산 서면 환승센터 초급속 허브', '부산역 KTX 메가환승허브',
  '부산 강서 명지국제신도시 메가허브', '부산 에코델타 스마트시티 스테이션', '부산 사상 스마트물류단지 차징존', '부산 기장 오시리아 관광단지 허브',
  '부산 북항 재개발 메가허브', '부산 동래 온천 스마트스테이션',
  '울산 삼산 메가차징 파크', '울산 현대차 전기차특구 허브', '울산 KTX역 복합환승센터', '울산 온산 모빌리티허브', '울산 북구 매곡산단 차징존',
  '경남 창원 국가산단 스마트스테이션', '경남 김해 장유 율하 스마트허브', '경남 양산 물금 복합차징존', '경남 진주 혁신도시 KTX허브',
  '경남 거제 옥포 모빌리티스테이션', '경남 사천 항공우주 스마트허브', '경남 통영 마리나 모빌리티파크', '경남 밀양 나노산단 차징허브',
  '광주 상무지구 시청 스마트허브', '광주 빛그린 전기차클러스터', '광주 첨단 R&D 테크노허브', '광주 수완 스마트차징파크', '광주송정 KTX 복합터미널',
  '전북 전주 혁신도시 복합스테이션', '전북 익산 KTX역 환승센터', '전북 군산 새만금 모빌리티허브', '전북 완주 테크노밸리 차징존', '전북 정읍 스마트파크',
  '전남 나주 빛가람 한전스마트허브', '전남 여수 국가산단 메가스테이션', '전남 순천만 국가정원 차징존', '전남 목포 남악 행정타운 스테이션',
  '전남 광양 항만물류 스마트허브', '전남 무안공항 복합차징존',
  '강원 강릉 KTX역 초급속 허브', '강원 원주 기업도시 스마트스테이션', '강원 원주 혁신도시 차징허브', '강원 춘천 남춘천역 스마트존',
  '강원 속초 해변 모빌리티파크', '강원 양양 고속도로 랜드허브', '강원 동해 KTX 스마트스테이션', '강원 삼척 수소모빌리티파크',
  '제주 제주시 공항 메가차징 파크', '제주 서귀포 중문 관광단지 허브', '제주 첨단과학기술단지 허브', '제주 성산 일출봉 스마트스테이션',
  '제주 애월 해안 모빌리티파크', '제주 한림 협재 스마트존', '제주 표선 해비치 차징허브', '제주 조천 함덕 모빌리티파크',
  '경부선 안성휴게소(서울방향) 메가허브', '경부선 안성휴게소(부산방향) 메가허브', '경부선 기흥휴게소 초급속 허브', '경부선 옥산휴게소 스마트존',
  '경부선 망향휴게소 하이퍼존', '경부선 천안삼거리휴게소 차징센터', '경부선 금강휴게소 파크허브', '경부선 칠곡휴게소 메가스테이션',
  '경부선 평사휴게소 초급속존', '경부선 통도사휴게소 스마트파크',
  '서해안선 매송휴게소 하이퍼차징', '서해안선 화성휴게소 복합센터', '서해안선 행담도휴게소 메가허브', '서해안선 서산휴게소 스마트존',
  '서해안선 대천휴게소 복합파크', '서해안선 군산휴게소 스마트존', '서해안선 고창고인돌휴게소 초급속존', '서해안선 함평천지휴게소 허브',
  '영동선 덕평자연휴게소 메가파크', '영동선 용인휴게소 초급속허브', '영동선 여주휴게소 스마트스테이션', '영동선 문막휴게소 초급속존',
  '영동선 횡성휴게소 스마트허브', '영동선 평창휴게소 올림픽허브', '영동선 강릉휴게소 KTX연계존',
  '중부선 마장프리미엄 메가스테이션', '중부선 하남드림휴게소 차징파크', '중부선 이천휴게소 스마트허브', '중부선 음성휴게소 복합센터'
]

export const CAR_MODELS = [
  '아이오닉 5 (77.4kWh)', '아이오닉 6 (77.4kWh)', 'EV6 GT-Line (77.4kWh)', 'EV9 4WD (99.8kWh)',
  'GV60 퍼포먼스 (77.4kWh)', 'GV70 전동화 (77.4kWh)', '타이칸 4S (93.4kWh)', '모델 Y RWD (60kWh)',
  'EQE 350+ (90.6kWh)', 'i4 eDrive40 (84kWh)', '포터2 일렉트릭 (58.8kWh)', '봉고3 EV (58.8kWh)',
  '일렉시티 대형전기버스 (290kWh)', 'e-Citaro 전기버스 (396kWh)', '볼보 FE 전동트럭 (264kWh)'
]

export const STATUS_MAP = {
  '충전대기':   { key: 'standby',    name: '충전대기',   color: '#d97706', darkHex: '#9a3412', cellClass: 'cell-standby',    badgeClass: 'badge-standby' },
  '알수없음':   { key: 'unknown',    name: '알수없음',   color: '#64748b', darkHex: '#475569', cellClass: 'cell-unknown',    badgeClass: 'badge-unknown' },
  '충전준비중': { key: 'preparing',  name: '충전준비중', color: '#0d9488', darkHex: '#115e59', cellClass: 'cell-preparing',  badgeClass: 'badge-preparing' },
  '통신이상':   { key: 'commerr',    name: '통신이상',   color: '#dc2626', darkHex: '#991b1b', cellClass: 'cell-commerr',    badgeClass: 'badge-commerr' },
  '충전중':     { key: 'charging',   name: '충전중',     color: '#3b82f6', darkHex: '#1e40af', cellClass: 'cell-charging',   badgeClass: 'badge-charging' },
  '운영중지':   { key: 'suspended',  name: '운영중지',   color: '#7c3aed', darkHex: '#6b21a8', cellClass: 'cell-suspended',  badgeClass: 'badge-suspended' },
  '예약중':     { key: 'reserved',   name: '예약중',     color: '#4d7c0f', darkHex: '#3f6212', cellClass: 'cell-reserved',   badgeClass: 'badge-reserved' },
  '점검중':     { key: 'inspecting', name: '점검중',     color: '#b45309', darkHex: '#854d0e', cellClass: 'cell-inspecting', badgeClass: 'badge-inspecting' },
  '충전완료':   { key: 'finished',   name: '충전완료',   color: '#059669', darkHex: '#166534', cellClass: 'cell-finished',   badgeClass: 'badge-finished' },
  '일시중지':   { key: 'paused',     name: '일시중지',   color: '#be123c', darkHex: '#9f1239', cellClass: 'cell-paused',     badgeClass: 'badge-paused' }
}

export type StatusKey = keyof typeof STATUS_MAP

export interface ChargerItem {
  id: string
  chargeBoxId: string
  stId: string
  stationName: string
  corpId: string
  corpName: string
  corpShortName: string
  cpId: string
  connectorId: number
  status: StatusKey
  spec: string
  powerKw: number
  voltageV: number
  currentA: number
  socPercent: number
  batteryTempC: number
  vendor: string
  model: string
  carModel: string
  userTag: string
  protocol: string
  lastHeartbeat: string
  accumulatedKwh: number
  chargingMinutes: number
}

export interface StationItem {
  id: string
  name: string
  corpId: string
  corpName: string
  corpShortName: string
  chargerCount: number
  startIdx: number
}

export interface ToastMessage {
  id: string
  type: 'success' | 'info' | 'error'
  title: string
  detail: string
}

// 2. 초기 200개 충전소 및 충전기 목 데이터 일괄 생성 함수
function generateInitialStations(): StationItem[] {
  const list: StationItem[] = []
  for (let i = 0; i < 200; i++) {
    const stId = String(10001 + i)
    let corp = CORP_LIST[0]
    for (const c of CORP_LIST) {
      if (i >= c.startIdx && i < c.endIdx) {
        corp = c
        break
      }
    }
    const name = i < REGION_NAMES_POOL.length ? REGION_NAMES_POOL[i] : `충전 거점 인프라 #${i - REGION_NAMES_POOL.length + 1} 스마트스테이션`
    const chargerCount = (i === 0) ? 30 : (i === 1) ? 10 : (i < 10) ? 5 : 5
    list.push({
      id: stId,
      name,
      corpId: corp.id,
      corpName: corp.name,
      corpShortName: corp.shortName,
      chargerCount,
      startIdx: i
    })
  }
  return list
}

function generateInitialChargers(stations: StationItem[]): ChargerItem[] {
  const list: ChargerItem[] = []
  let globalSeq = 1

  stations.forEach((st) => {
    for (let u = 1; u <= st.chargerCount; u++) {
      const cpId = String(u).padStart(2, '0')
      const chargeBoxId = `CP-${st.id.slice(-3)}-${cpId}`
      const spec = (u % 2 === 1) ? '350kW 초급속' : '200kW 급속'
      const vendor = (u % 3 === 0) ? 'LS E-Link Power' : (u % 3 === 1) ? 'SK Signet' : '대영채비 (Chaevi)'
      const model = (u % 2 === 1) ? 'ELVIS-HPC-350K' : 'ELVIS-DC-200K'
      const carModel = CAR_MODELS[(globalSeq + u) % CAR_MODELS.length]
      const userTag = `RFID-${String(100000 + (globalSeq * 17) % 900000)}`

      // 10대 상태 모의 분포
      const mod10 = (globalSeq * 7 + u) % 10
      let status: StatusKey = '충전대기'
      let powerKw = 0
      let voltageV = 0
      let currentA = 0
      let socPercent = 0
      let batteryTempC = 25
      let accumulatedKwh = 0
      let chargingMinutes = 0

      if (mod10 === 0 || mod10 === 1 || mod10 === 2) {
        status = '충전중'
        powerKw = 120 + (u * 11) % 190
        voltageV = 720 + (u * 3) % 80
        currentA = powerKw > 0 ? (powerKw * 1000) / voltageV : 0
        socPercent = 20 + (u * 7) % 75
        batteryTempC = 34 + (u % 9)
        accumulatedKwh = 15.5 + (u * 3.7) % 60
        chargingMinutes = 12 + (u * 5) % 45
      } else if (mod10 === 3) {
        status = '충전준비중'
        socPercent = 15
        batteryTempC = 26
      } else if (mod10 === 4) {
        status = '충전완료'
        socPercent = 100
        accumulatedKwh = 68.4
        batteryTempC = 28
      } else if (mod10 === 5) {
        status = '예약중'
        socPercent = 0
      } else if (mod10 === 6) {
        status = '통신이상'
        socPercent = 0
      } else if (mod10 === 7) {
        status = '점검중'
        socPercent = 0
      } else if (mod10 === 8) {
        status = '일시중지'
        socPercent = 45
        batteryTempC = 38
      } else {
        status = '충전대기'
        socPercent = 0
      }

      list.push({
        id: `${st.id}-${cpId}-1`,
        chargeBoxId,
        stId: st.id,
        stationName: st.name,
        corpId: st.corpId,
        corpName: st.corpName,
        corpShortName: st.corpShortName,
        cpId,
        connectorId: 1,
        status,
        spec,
        powerKw,
        voltageV,
        currentA,
        socPercent,
        batteryTempC,
        vendor,
        model,
        carModel,
        userTag,
        protocol: (u % 4 === 0) ? 'OCPP 2.0.1 (JSON)' : 'OCPP 1.6-J (WebSocket)',
        lastHeartbeat: '방금 전 (1초 전)',
        accumulatedKwh,
        chargingMinutes
      })

      globalSeq++
    }
  })

  return list
}

const initialStations = generateInitialStations()
const initialChargers = generateInitialChargers(initialStations)

export const useCsmsStore = defineStore('csms', {
  state: () => ({
    // 테마 설정
    currentTheme: 'light' as 'light' | 'cyber',

    // 반응형 드로어 상태 (프로토타입 LAYOUT-OPTION-B 규격)
    isSidebarOpen: false,
    isControlDrawerOpen: false,

    // 법인 및 충전소 데이터
    corps: CORP_LIST,
    stations: initialStations,
    chargers: initialChargers,

    // 선택 상태
    curCorpFilter: 'ALL',
    curStFilter: '10001',
    curStatusFilter: 'ALL',
    searchQuery: '',
    selectedCharger: initialChargers[0] || null,

    // 시스템 전체 요약 (대시보드 KPI)
    summary: {
      totalChargers: 2000,
      chargingCount: initialChargers.filter(c => c.status === '충전중').length,
      availableCount: initialChargers.filter(c => c.status === '충전대기').length,
      faultedCount: initialChargers.filter(c => c.status === '통신이상').length,
      preparingCount: initialChargers.filter(c => c.status === '충전준비중').length,
      totalPowerKw: initialChargers.reduce((acc, c) => acc + c.powerKw, 0),
      liveTps: 2480,
      activeSessions: initialChargers.filter(c => c.status === '충전중').length,
      todayTotalKwh: 248500.8
    },

    // 토스트 알림 목록
    toasts: [] as ToastMessage[]
  }),

  actions: {
    setTheme(theme: string) {
      this.currentTheme = theme as 'light' | 'cyber'
    },

    toggleSidebar(isOpen?: boolean) {
      this.isSidebarOpen = isOpen !== undefined ? isOpen : !this.isSidebarOpen
    },

    toggleControlDrawer(isOpen?: boolean) {
      this.isControlDrawerOpen = isOpen !== undefined ? isOpen : !this.isControlDrawerOpen
    },

    setCorpFilter(corpId: string) {
      this.curCorpFilter = corpId
      const stInCorp = this.stations.find(s => corpId === 'ALL' || s.corpId === corpId)
      if (stInCorp) {
        this.curStFilter = stInCorp.id
      }
    },

    setStationFilter(stId: string) {
      this.curStFilter = stId
      const targetCharger = this.chargers.find(c => stId === 'ALL' || c.stId === stId)
      if (targetCharger) {
        this.selectedCharger = targetCharger
      }
    },

    setStatusFilter(status: string) {
      this.curStatusFilter = status
    },

    setSelectedCharger(charger: ChargerItem) {
      this.selectedCharger = charger
    },

    addToast(type: 'success' | 'info' | 'error', title: string, detail: string) {
      const id = 'toast-' + Date.now() + Math.random()
      this.toasts.push({ id, type, title, detail })
      setTimeout(() => {
        this.toasts = this.toasts.filter(t => t.id !== id)
      }, 3500)
    }
  }
})
