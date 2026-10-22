import { ref, computed } from 'vue'

export interface UserMaster {
  userNo: string
  userNm: string
  corpId: string
  userTypeCd: 'C' | 'K' | 'M' | 'N' // C: 법인, K: 로밍, M: 모바일/일반, N: 현장 비회원
  phone: string
  useYn: 'Y' | 'N'
  createdAt: string
}

export interface VehicleMaster {
  vhrNo: string // 차량번호 (PK)
  vhrId: string // 차량 식별ID
  corpId: string
  userNo: string
  modelName: string
  batteryCapacity: number // kWh
  optimalSoc: number // %
  useYn: 'Y' | 'N'
  createdAt: string
}

export interface AuthCertBase {
  idtag: string
  certType: 'RFID' | 'VHRNO' | 'PCID'
  corpId: string
  userNo: string
  payRightYn: 'Y' | 'N'
  useYn: 'Y' | 'N'
  createdAt: string
}

export interface AuthRfidItem extends AuthCertBase {
  certType: 'RFID'
  cardNo: string
  cardIssuer: string
}

export interface AuthVhrnoItem extends AuthCertBase {
  certType: 'VHRNO'
  vhrNo: string
  autoVhrYn: 'Y' | 'N'
}

export interface AuthPcidItem extends AuthCertBase {
  certType: 'PCID'
  emaid: string
  certExpireDt: string
}

const STORAGE_KEY = 'elvis_master_data_v1'

const DEFAULT_USERS: UserMaster[] = [
  {
    userNo: 'U1001-01',
    userNm: '(주)엘에스링크 여객운수 (법인)',
    corpId: '1001',
    userTypeCd: 'C',
    phone: '02-1234-5678',
    useYn: 'Y',
    createdAt: '2026-10-21 10:00:00'
  },
  {
    userNo: 'UMOB-8801',
    userNm: '홍길동 (일반 회원)',
    corpId: '0000',
    userTypeCd: 'M',
    phone: '010-9988-7766',
    useYn: 'Y',
    createdAt: '2026-10-21 10:00:00'
  },
  {
    userNo: 'UKME-7701',
    userNm: '환경부/한전 로밍회원',
    corpId: '0000',
    userTypeCd: 'K',
    phone: '1661-0000',
    useYn: 'Y',
    createdAt: '2026-10-21 10:00:00'
  },
  {
    userNo: 'UGUEST-01',
    userNm: '현장 비회원 (현장결제)',
    corpId: '0000',
    userTypeCd: 'N',
    phone: '-',
    useYn: 'Y',
    createdAt: '2026-10-21 10:00:00'
  }
]

const DEFAULT_VEHICLES: VehicleMaster[] = [
  {
    vhrNo: '서울70사1234',
    vhrId: 'VHR-1001-01',
    corpId: '1001',
    userNo: 'U1001-01',
    modelName: '현대 일렉시티 더블데커 (350kWh)',
    batteryCapacity: 350,
    optimalSoc: 85,
    useYn: 'Y',
    createdAt: '2026-10-21 10:00:00'
  },
  {
    vhrNo: '경기75바5678',
    vhrId: 'VHR-1001-02',
    corpId: '1001',
    userNo: 'U1001-01',
    modelName: '현대 일렉시티 굴절버스 (388kWh)',
    batteryCapacity: 388,
    optimalSoc: 80,
    useYn: 'Y',
    createdAt: '2026-10-21 10:00:00'
  },
  {
    vhrNo: '12가3456',
    vhrId: 'VHR-MOB-01',
    corpId: '0000',
    userNo: 'UMOB-8801',
    modelName: '기아 EV6 GT (77.4kWh)',
    batteryCapacity: 77.4,
    optimalSoc: 90,
    useYn: 'Y',
    createdAt: '2026-10-21 10:00:00'
  }
]

const DEFAULT_RFIDS: AuthRfidItem[] = [
  {
    idtag: 'CARD-CORP-9901',
    certType: 'RFID',
    cardNo: '1010-0020-9901-8821',
    cardIssuer: '신한카드 / LS E-Link',
    corpId: '1001',
    userNo: 'U1001-01',
    payRightYn: 'Y',
    useYn: 'Y',
    createdAt: '2026-10-21 10:00:00'
  },
  {
    idtag: 'CARD-USER-1001',
    certType: 'RFID',
    cardNo: '9411-5021-1001-7734',
    cardIssuer: 'KB국민카드',
    corpId: '0000',
    userNo: 'UMOB-8801',
    payRightYn: 'Y',
    useYn: 'Y',
    createdAt: '2026-10-21 10:00:00'
  },
  {
    idtag: 'ROAM-ME-7701',
    certType: 'RFID',
    cardNo: '8801-7701-0001-9999',
    cardIssuer: '환경부 / 한국환경공단',
    corpId: '0000',
    userNo: 'UKME-7701',
    payRightYn: 'Y',
    useYn: 'Y',
    createdAt: '2026-10-21 10:00:00'
  },
  {
    idtag: 'CREDIT-IC-01',
    certType: 'RFID',
    cardNo: '5424-1234-9900-1122',
    cardIssuer: '현장 비회원 IC 신용카드',
    corpId: '0000',
    userNo: 'UGUEST-01',
    payRightYn: 'Y',
    useYn: 'Y',
    createdAt: '2026-10-21 10:00:00'
  }
]

const DEFAULT_VHRNOS: AuthVhrnoItem[] = [
  {
    idtag: 'VHR-SEOUL-1234',
    certType: 'VHRNO',
    vhrNo: '서울70사1234',
    corpId: '1001',
    userNo: 'U1001-01',
    autoVhrYn: 'Y',
    payRightYn: 'Y',
    useYn: 'Y',
    createdAt: '2026-10-21 10:00:00'
  },
  {
    idtag: 'VHR-GYEONGGI-5678',
    certType: 'VHRNO',
    vhrNo: '경기75바5678',
    corpId: '1001',
    userNo: 'U1001-01',
    autoVhrYn: 'Y',
    payRightYn: 'Y',
    useYn: 'Y',
    createdAt: '2026-10-21 10:00:00'
  }
]

const DEFAULT_PCIDS: AuthPcidItem[] = [
  {
    idtag: 'PCID-KR-LSE-001',
    certType: 'PCID',
    emaid: 'KR-LSE-C12345678-X',
    certExpireDt: '2029-12-31',
    corpId: '1001',
    userNo: 'U1001-01',
    payRightYn: 'Y',
    useYn: 'Y',
    createdAt: '2026-10-21 10:00:00'
  }
]

// 싱글톤 상태
const users = ref<UserMaster[]>([])
const vehicles = ref<VehicleMaster[]>([])
const rfidCards = ref<AuthRfidItem[]>([])
const vhrnoCerts = ref<AuthVhrnoItem[]>([])
const pcidCerts = ref<AuthPcidItem[]>([])
const isInitialized = ref(false)

function saveToStorage() {
  const data = {
    users: users.value,
    vehicles: vehicles.value,
    rfidCards: rfidCards.value,
    vhrnoCerts: vhrnoCerts.value,
    pcidCerts: pcidCerts.value
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
}

function loadFromStorage() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (raw) {
    try {
      const parsed = JSON.parse(raw)
      users.value = parsed.users || DEFAULT_USERS
      vehicles.value = parsed.vehicles || DEFAULT_VEHICLES
      rfidCards.value = parsed.rfidCards || DEFAULT_RFIDS
      vhrnoCerts.value = parsed.vhrnoCerts || DEFAULT_VHRNOS
      pcidCerts.value = parsed.pcidCerts || DEFAULT_PCIDS
      isInitialized.value = true
      return
    } catch (e) {
      console.warn('마스터 데이터 로드 실패, 기본값으로 초기화:', e)
    }
  }
  // 기본 프리셋 로드
  users.value = [...DEFAULT_USERS]
  vehicles.value = [...DEFAULT_VEHICLES]
  rfidCards.value = [...DEFAULT_RFIDS]
  vhrnoCerts.value = [...DEFAULT_VHRNOS]
  pcidCerts.value = [...DEFAULT_PCIDS]
  isInitialized.value = true
  saveToStorage()
}

// 최초 1회 로드
if (!isInitialized.value) {
  loadFromStorage()
}

export function useMasterData() {
  // ─────────────────────────────────────────────
  // 1. 사용자 (User) CRUD
  // ─────────────────────────────────────────────
  function addUser(user: UserMaster) {
    users.value.unshift({ ...user, createdAt: new Date().toLocaleString('ko-KR') })
    saveToStorage()
  }

  function updateUser(userNo: string, updated: Partial<UserMaster>) {
    const idx = users.value.findIndex(u => u.userNo === userNo)
    if (idx !== -1) {
      users.value[idx] = { ...users.value[idx], ...updated }
      saveToStorage()
    }
  }

  function deleteUser(userNo: string) {
    users.value = users.value.filter(u => u.userNo !== userNo)
    saveToStorage()
  }

  // ─────────────────────────────────────────────
  // 2. 차량 (Vehicle) CRUD
  // ─────────────────────────────────────────────
  function addVehicle(vhr: VehicleMaster) {
    vehicles.value.unshift({ ...vhr, createdAt: new Date().toLocaleString('ko-KR') })
    saveToStorage()
  }

  function updateVehicle(vhrNo: string, updated: Partial<VehicleMaster>) {
    const idx = vehicles.value.findIndex(v => v.vhrNo === vhrNo)
    if (idx !== -1) {
      vehicles.value[idx] = { ...vehicles.value[idx], ...updated }
      saveToStorage()
    }
  }

  function deleteVehicle(vhrNo: string) {
    vehicles.value = vehicles.value.filter(v => v.vhrNo !== vhrNo)
    saveToStorage()
  }

  // ─────────────────────────────────────────────
  // 3. 인증 (Auth Cert) CRUD
  // ─────────────────────────────────────────────
  function addRfid(rfid: AuthRfidItem) {
    rfidCards.value.unshift({ ...rfid, certType: 'RFID', createdAt: new Date().toLocaleString('ko-KR') })
    saveToStorage()
  }

  function updateRfid(idtag: string, updated: Partial<AuthRfidItem>) {
    const idx = rfidCards.value.findIndex(r => r.idtag === idtag)
    if (idx !== -1) {
      rfidCards.value[idx] = { ...rfidCards.value[idx], ...updated }
      saveToStorage()
    }
  }

  function deleteRfid(idtag: string) {
    rfidCards.value = rfidCards.value.filter(r => r.idtag !== idtag)
    saveToStorage()
  }

  function addVhrno(vhrno: AuthVhrnoItem) {
    vhrnoCerts.value.unshift({ ...vhrno, certType: 'VHRNO', createdAt: new Date().toLocaleString('ko-KR') })
    saveToStorage()
  }

  function updateVhrno(idtag: string, updated: Partial<AuthVhrnoItem>) {
    const idx = vhrnoCerts.value.findIndex(v => v.idtag === idtag)
    if (idx !== -1) {
      vhrnoCerts.value[idx] = { ...vhrnoCerts.value[idx], ...updated }
      saveToStorage()
    }
  }

  function deleteVhrno(idtag: string) {
    vhrnoCerts.value = vhrnoCerts.value.filter(v => v.idtag !== idtag)
    saveToStorage()
  }

  function addPcid(pcid: AuthPcidItem) {
    pcidCerts.value.unshift({ ...pcid, certType: 'PCID', createdAt: new Date().toLocaleString('ko-KR') })
    saveToStorage()
  }

  function updatePcid(idtag: string, updated: Partial<AuthPcidItem>) {
    const idx = pcidCerts.value.findIndex(p => p.idtag === idtag)
    if (idx !== -1) {
      pcidCerts.value[idx] = { ...pcidCerts.value[idx], ...updated }
      saveToStorage()
    }
  }

  function deletePcid(idtag: string) {
    pcidCerts.value = pcidCerts.value.filter(p => p.idtag !== idtag)
    saveToStorage()
  }

  function resetToDefaults() {
    users.value = [...DEFAULT_USERS]
    vehicles.value = [...DEFAULT_VEHICLES]
    rfidCards.value = [...DEFAULT_RFIDS]
    vhrnoCerts.value = [...DEFAULT_VHRNOS]
    pcidCerts.value = [...DEFAULT_PCIDS]
    saveToStorage()
  }

  return {
    users,
    vehicles,
    rfidCards,
    vhrnoCerts,
    pcidCerts,
    addUser,
    updateUser,
    deleteUser,
    addVehicle,
    updateVehicle,
    deleteVehicle,
    addRfid,
    updateRfid,
    deleteRfid,
    addVhrno,
    updateVhrno,
    deleteVhrno,
    addPcid,
    updatePcid,
    deletePcid,
    resetToDefaults
  }
}
