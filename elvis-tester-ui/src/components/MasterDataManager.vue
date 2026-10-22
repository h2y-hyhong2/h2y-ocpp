<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  useMasterData,
  type UserMaster,
  type VehicleMaster,
  type AuthRfidItem,
  type AuthVhrnoItem,
  type AuthPcidItem
} from '../composables/useMasterData'

const props = defineProps<{
  chargerId?: string
}>()

const emit = defineEmits<{
  (e: 'apply-idtag', idtag: string): void
}>()

const master = useMasterData()

type SubTab = 'user' | 'vehicle' | 'rfid' | 'vhrno' | 'pcid'
const activeSubTab = ref<SubTab>('user')
const searchQuery = ref('')

// 모달 상태
const isModalOpen = ref(false)
const isEditMode = ref(false)
const editingOldKey = ref('')

// 폼 모델들
const userForm = ref<UserMaster>({
  userNo: '',
  userNm: '',
  corpId: '1001',
  userTypeCd: 'C',
  phone: '',
  useYn: 'Y',
  createdAt: ''
})

const vehicleForm = ref<VehicleMaster>({
  vhrNo: '',
  vhrId: '',
  corpId: '1001',
  userNo: 'U1001-01',
  modelName: '',
  batteryCapacity: 100,
  optimalSoc: 80,
  useYn: 'Y',
  createdAt: ''
})

const rfidForm = ref<AuthRfidItem>({
  idtag: '',
  certType: 'RFID',
  cardNo: '',
  cardIssuer: '신한카드',
  corpId: '1001',
  userNo: 'U1001-01',
  payRightYn: 'Y',
  useYn: 'Y',
  createdAt: ''
})

const vhrnoForm = ref<AuthVhrnoItem>({
  idtag: '',
  certType: 'VHRNO',
  vhrNo: '',
  corpId: '1001',
  userNo: 'U1001-01',
  autoVhrYn: 'Y',
  payRightYn: 'Y',
  useYn: 'Y',
  createdAt: ''
})

const pcidForm = ref<AuthPcidItem>({
  idtag: '',
  certType: 'PCID',
  emaid: '',
  certExpireDt: '2029-12-31',
  corpId: '1001',
  userNo: 'U1001-01',
  payRightYn: 'Y',
  useYn: 'Y',
  createdAt: ''
})

// 필터링된 목록들
const filteredUsers = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return master.users.value
  return master.users.value.filter(
    u => u.userNo.toLowerCase().includes(q) || u.userNm.toLowerCase().includes(q) || u.corpId.toLowerCase().includes(q)
  )
})

const filteredVehicles = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return master.vehicles.value
  return master.vehicles.value.filter(
    v => v.vhrNo.toLowerCase().includes(q) || v.vhrId.toLowerCase().includes(q) || v.modelName.toLowerCase().includes(q)
  )
})

const filteredRfids = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return master.rfidCards.value
  return master.rfidCards.value.filter(
    r => r.idtag.toLowerCase().includes(q) || r.cardNo.toLowerCase().includes(q) || r.cardIssuer.toLowerCase().includes(q)
  )
})

const filteredVhrnos = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return master.vhrnoCerts.value
  return master.vhrnoCerts.value.filter(
    v => v.idtag.toLowerCase().includes(q) || v.vhrNo.toLowerCase().includes(q)
  )
})

const filteredPcids = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return master.pcidCerts.value
  return master.pcidCerts.value.filter(
    p => p.idtag.toLowerCase().includes(q) || p.emaid.toLowerCase().includes(q)
  )
})

// ─────────────────────────────────────────────
// 모달 열기/닫기
// ─────────────────────────────────────────────
function openCreateModal() {
  isEditMode.value = false
  editingOldKey.value = ''

  if (activeSubTab.value === 'user') {
    userForm.value = {
      userNo: `U${Date.now().toString().slice(-4)}`,
      userNm: '',
      corpId: '1001',
      userTypeCd: 'C',
      phone: '',
      useYn: 'Y',
      createdAt: ''
    }
  } else if (activeSubTab.value === 'vehicle') {
    vehicleForm.value = {
      vhrNo: '',
      vhrId: `VHR-${Date.now().toString().slice(-4)}`,
      corpId: '1001',
      userNo: master.users.value[0]?.userNo || 'U1001-01',
      modelName: '',
      batteryCapacity: 100,
      optimalSoc: 80,
      useYn: 'Y',
      createdAt: ''
    }
  } else if (activeSubTab.value === 'rfid') {
    rfidForm.value = {
      idtag: `CARD-TEST-${Date.now().toString().slice(-4)}`,
      certType: 'RFID',
      cardNo: '1010-0000-0000-0000',
      cardIssuer: '신한카드',
      corpId: '1001',
      userNo: master.users.value[0]?.userNo || 'U1001-01',
      payRightYn: 'Y',
      useYn: 'Y',
      createdAt: ''
    }
  } else if (activeSubTab.value === 'vhrno') {
    vhrnoForm.value = {
      idtag: `VHR-TEST-${Date.now().toString().slice(-4)}`,
      certType: 'VHRNO',
      vhrNo: master.vehicles.value[0]?.vhrNo || '서울70사1234',
      corpId: '1001',
      userNo: master.users.value[0]?.userNo || 'U1001-01',
      autoVhrYn: 'Y',
      payRightYn: 'Y',
      useYn: 'Y',
      createdAt: ''
    }
  } else if (activeSubTab.value === 'pcid') {
    pcidForm.value = {
      idtag: `PCID-TEST-${Date.now().toString().slice(-4)}`,
      certType: 'PCID',
      emaid: `KR-LSE-C${Date.now().toString().slice(-8)}-X`,
      certExpireDt: '2029-12-31',
      corpId: '1001',
      userNo: master.users.value[0]?.userNo || 'U1001-01',
      payRightYn: 'Y',
      useYn: 'Y',
      createdAt: ''
    }
  }

  isModalOpen.value = true
}

function openEditModal(item: any) {
  isEditMode.value = true

  if (activeSubTab.value === 'user') {
    editingOldKey.value = item.userNo
    userForm.value = { ...item }
  } else if (activeSubTab.value === 'vehicle') {
    editingOldKey.value = item.vhrNo
    vehicleForm.value = { ...item }
  } else if (activeSubTab.value === 'rfid') {
    editingOldKey.value = item.idtag
    rfidForm.value = { ...item }
  } else if (activeSubTab.value === 'vhrno') {
    editingOldKey.value = item.idtag
    vhrnoForm.value = { ...item }
  } else if (activeSubTab.value === 'pcid') {
    editingOldKey.value = item.idtag
    pcidForm.value = { ...item }
  }

  isModalOpen.value = true
}

function saveModalForm() {
  if (activeSubTab.value === 'user') {
    if (!userForm.value.userNo || !userForm.value.userNm) {
      alert('회원번호와 회원명은 필수입니다.')
      return
    }
    if (isEditMode.value) {
      master.updateUser(editingOldKey.value, userForm.value)
    } else {
      master.addUser(userForm.value)
    }
  } else if (activeSubTab.value === 'vehicle') {
    if (!vehicleForm.value.vhrNo) {
      alert('차량번호는 필수입니다.')
      return
    }
    if (isEditMode.value) {
      master.updateVehicle(editingOldKey.value, vehicleForm.value)
    } else {
      master.addVehicle(vehicleForm.value)
    }
  } else if (activeSubTab.value === 'rfid') {
    if (!rfidForm.value.idtag) {
      alert('idTag는 필수입니다.')
      return
    }
    if (isEditMode.value) {
      master.updateRfid(editingOldKey.value, rfidForm.value)
    } else {
      master.addRfid(rfidForm.value)
    }
  } else if (activeSubTab.value === 'vhrno') {
    if (!vhrnoForm.value.idtag || !vhrnoForm.value.vhrNo) {
      alert('idTag와 매핑 차량번호는 필수입니다.')
      return
    }
    if (isEditMode.value) {
      master.updateVhrno(editingOldKey.value, vhrnoForm.value)
    } else {
      master.addVhrno(vhrnoForm.value)
    }
  } else if (activeSubTab.value === 'pcid') {
    if (!pcidForm.value.idtag || !pcidForm.value.emaid) {
      alert('idTag와 eMAID는 필수입니다.')
      return
    }
    if (isEditMode.value) {
      master.updatePcid(editingOldKey.value, pcidForm.value)
    } else {
      master.addPcid(pcidForm.value)
    }
  }

  isModalOpen.value = false
}

function handleDelete(key: string) {
  if (!confirm(`정말 삭제하시겠습니까? (${key})`)) return
  if (activeSubTab.value === 'user') {
    master.deleteUser(key)
  } else if (activeSubTab.value === 'vehicle') {
    master.deleteVehicle(key)
  } else if (activeSubTab.value === 'rfid') {
    master.deleteRfid(key)
  } else if (activeSubTab.value === 'vhrno') {
    master.deleteVhrno(key)
  } else if (activeSubTab.value === 'pcid') {
    master.deletePcid(key)
  }
}

function applyIdtagToTester(idtag: string) {
  emit('apply-idtag', idtag)
  navigator.clipboard.writeText(idtag)
  alert(`인증 식별 태그 [${idtag}] 가 클립보드에 복사되었으며, 테스터에 적용되었습니다.`)
}
</script>

<template>
  <div class="h-full flex flex-col bg-slate-50 text-slate-800 text-xs overflow-hidden select-none font-sans">
    <!-- 1. 상단 툴바: 탭 네비게이션 & 검색 & 액션 버튼 -->
    <div class="p-2.5 bg-white border-b border-slate-200 flex flex-wrap items-center justify-between gap-2 flex-shrink-0 shadow-2xs">
      <!-- 5대 서브 탭 버튼군 (가로 부족 시 스크롤 허용) -->
      <div class="flex items-center gap-1 bg-slate-100 p-1 rounded-lg border border-slate-200 overflow-x-auto scrollbar-none max-w-full">
        <button
          @click="activeSubTab = 'user'"
          class="px-2.5 py-1 rounded-md text-xs font-bold transition-all cursor-pointer flex items-center gap-1"
          :class="activeSubTab === 'user' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600 hover:text-slate-900'"
        >
          <span>👤 사용자</span>
          <span class="font-mono text-[10px] px-1 rounded bg-slate-200 text-slate-700">{{ master.users.value.length }}</span>
        </button>

        <button
          @click="activeSubTab = 'vehicle'"
          class="px-2.5 py-1 rounded-md text-xs font-bold transition-all cursor-pointer flex items-center gap-1"
          :class="activeSubTab === 'vehicle' ? 'bg-white text-indigo-700 shadow-2xs' : 'text-slate-600 hover:text-slate-900'"
        >
          <span>🚙 차량</span>
          <span class="font-mono text-[10px] px-1 rounded bg-slate-200 text-slate-700">{{ master.vehicles.value.length }}</span>
        </button>

        <button
          @click="activeSubTab = 'rfid'"
          class="px-2.5 py-1 rounded-md text-xs font-bold transition-all cursor-pointer flex items-center gap-1"
          :class="activeSubTab === 'rfid' ? 'bg-white text-sky-700 shadow-2xs' : 'text-slate-600 hover:text-slate-900'"
        >
          <span>💳 RFID</span>
          <span class="font-mono text-[10px] px-1 rounded bg-slate-200 text-slate-700">{{ master.rfidCards.value.length }}</span>
        </button>

        <button
          @click="activeSubTab = 'vhrno'"
          class="px-2.5 py-1 rounded-md text-xs font-bold transition-all cursor-pointer flex items-center gap-1"
          :class="activeSubTab === 'vhrno' ? 'bg-white text-emerald-700 shadow-2xs' : 'text-slate-600 hover:text-slate-900'"
        >
          <span>🚗 VHRNO</span>
          <span class="font-mono text-[10px] px-1 rounded bg-slate-200 text-slate-700">{{ master.vhrnoCerts.value.length }}</span>
        </button>

        <button
          @click="activeSubTab = 'pcid'"
          class="px-2.5 py-1 rounded-md text-xs font-bold transition-all cursor-pointer flex items-center gap-1"
          :class="activeSubTab === 'pcid' ? 'bg-white text-amber-700 shadow-2xs' : 'text-slate-600 hover:text-slate-900'"
        >
          <span>⚡ PCID (PnC)</span>
          <span class="font-mono text-[10px] px-1 rounded bg-slate-200 text-slate-700">{{ master.pcidCerts.value.length }}</span>
        </button>
      </div>

      <!-- 우측 검색창 및 버튼군 (가로 부족 시 세로 스택) -->
      <div class="flex items-center gap-1.5 flex-1 sm:flex-initial w-full sm:w-auto justify-end">
        <div class="relative w-48">
          <input
            v-model="searchQuery"
            placeholder="마스터 데이터 검색..."
            class="w-full bg-slate-50 border border-slate-200 rounded-md px-2.5 py-1 text-xs focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-sky-500 font-medium"
          />
          <span v-if="searchQuery" @click="searchQuery = ''" class="absolute right-2 top-1.5 text-xs text-slate-400 cursor-pointer">✕</span>
        </div>

        <button
          @click="openCreateModal"
          class="px-2.5 py-1 text-xs font-bold rounded-md bg-slate-900 hover:bg-slate-800 text-white cursor-pointer shadow-2xs flex items-center gap-1 shrink-0"
        >
          <span>➕</span>
          <span>신규 등록</span>
        </button>

        <button
          @click="master.resetToDefaults()"
          class="px-2 py-1 text-xs font-medium rounded-md bg-slate-100 hover:bg-slate-200 text-slate-600 border border-slate-200 cursor-pointer shrink-0"
          title="표준 기본 데이터로 초기화"
        >
          ↺ 리셋
        </button>
      </div>
    </div>

    <!-- 2. 본문: 서브 탭별 테이블 그리드 (가로 부족 시 스크롤 허용) -->
    <div class="flex-1 overflow-y-auto overflow-x-auto p-2.5">
      <!-- 2-1. 사용자 관리 테이블 -->
      <table v-if="activeSubTab === 'user'" class="w-full border-collapse bg-white rounded-lg border border-slate-200 shadow-2xs overflow-hidden">
        <thead class="bg-slate-50 border-b border-slate-200 text-slate-500 font-mono text-[11px] text-left">
          <tr>
            <th class="p-2 font-bold">회원번호 (userNo)</th>
            <th class="p-2 font-bold">회원명 / 상호</th>
            <th class="p-2 font-bold">법인 ID</th>
            <th class="p-2 font-bold">유형</th>
            <th class="p-2 font-bold">연락처</th>
            <th class="p-2 font-bold">상태</th>
            <th class="p-2 font-bold text-center">관리</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100 font-mono text-[11px]">
          <tr v-for="u in filteredUsers" :key="u.userNo" class="hover:bg-slate-50/80 transition-colors">
            <td class="p-2 font-bold text-sky-800">{{ u.userNo }}</td>
            <td class="p-2 text-slate-900 font-sans font-medium">{{ u.userNm }}</td>
            <td class="p-2 text-slate-600">{{ u.corpId }}</td>
            <td class="p-2">
              <span class="px-1.5 py-0.5 rounded text-[10px] font-bold" :class="u.userTypeCd === 'C' ? 'bg-indigo-50 text-indigo-700' : u.userTypeCd === 'M' ? 'bg-sky-50 text-sky-700' : 'bg-slate-100 text-slate-600'">
                {{ u.userTypeCd === 'C' ? '법인(C)' : u.userTypeCd === 'M' ? '일반(M)' : u.userTypeCd === 'K' ? '로밍(K)' : '비회원(N)' }}
              </span>
            </td>
            <td class="p-2 text-slate-500">{{ u.phone || '-' }}</td>
            <td class="p-2">
              <span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700">정상</span>
            </td>
            <td class="p-2 text-center">
              <div class="flex items-center justify-center gap-1">
                <button @click="openEditModal(u)" class="px-1.5 py-0.5 rounded hover:bg-slate-100 text-slate-600">수정</button>
                <button @click="handleDelete(u.userNo)" class="px-1.5 py-0.5 rounded hover:bg-rose-50 text-rose-600">삭제</button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredUsers.length === 0">
            <td colspan="7" class="p-6 text-center text-slate-400 font-sans">등록된 사용자 데이터가 없습니다.</td>
          </tr>
        </tbody>
      </table>

      <!-- 2-2. 차량 관리 테이블 ([1826]) -->
      <table v-else-if="activeSubTab === 'vehicle'" class="w-full border-collapse bg-white rounded-lg border border-slate-200 shadow-2xs overflow-hidden">
        <thead class="bg-slate-50 border-b border-slate-200 text-slate-500 font-mono text-[11px] text-left">
          <tr>
            <th class="p-2 font-bold">차량번호 (vhrNo)</th>
            <th class="p-2 font-bold">차량 식별 ID</th>
            <th class="p-2 font-bold">차종명</th>
            <th class="p-2 font-bold">배터리 용량</th>
            <th class="p-2 font-bold">최적 목표 SOC</th>
            <th class="p-2 font-bold">소속 회원</th>
            <th class="p-2 font-bold text-center">관리</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100 font-mono text-[11px]">
          <tr v-for="v in filteredVehicles" :key="v.vhrNo" class="hover:bg-slate-50/80 transition-colors">
            <td class="p-2 font-bold text-indigo-700">{{ v.vhrNo }}</td>
            <td class="p-2 text-slate-600">{{ v.vhrId }}</td>
            <td class="p-2 text-slate-900 font-sans font-medium">{{ v.modelName }}</td>
            <td class="p-2 font-bold text-slate-700">{{ v.batteryCapacity }} kWh</td>
            <td class="p-2 text-sky-700 font-bold">{{ v.optimalSoc }} %</td>
            <td class="p-2 text-slate-500">{{ v.userNo || '-' }}</td>
            <td class="p-2 text-center">
              <div class="flex items-center justify-center gap-1">
                <button @click="openEditModal(v)" class="px-1.5 py-0.5 rounded hover:bg-slate-100 text-slate-600">수정</button>
                <button @click="handleDelete(v.vhrNo)" class="px-1.5 py-0.5 rounded hover:bg-rose-50 text-rose-600">삭제</button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredVehicles.length === 0">
            <td colspan="7" class="p-6 text-center text-slate-400 font-sans">등록된 차량 데이터가 없습니다.</td>
          </tr>
        </tbody>
      </table>

      <!-- 2-3. RFID 카드 테이블 -->
      <table v-else-if="activeSubTab === 'rfid'" class="w-full border-collapse bg-white rounded-lg border border-slate-200 shadow-2xs overflow-hidden">
        <thead class="bg-slate-50 border-b border-slate-200 text-slate-500 font-mono text-[11px] text-left">
          <tr>
            <th class="p-2 font-bold">OCPP idTag</th>
            <th class="p-2 font-bold">16자리 실물 카드번호</th>
            <th class="p-2 font-bold">카드 발급사</th>
            <th class="p-2 font-bold">소속 회원</th>
            <th class="p-2 font-bold">바로정산</th>
            <th class="p-2 font-bold text-center">테스터 연동</th>
            <th class="p-2 font-bold text-center">관리</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100 font-mono text-[11px]">
          <tr v-for="r in filteredRfids" :key="r.idtag" class="hover:bg-slate-50/80 transition-colors">
            <td class="p-2 font-bold text-sky-700">{{ r.idtag }}</td>
            <td class="p-2 text-slate-600">{{ r.cardNo }}</td>
            <td class="p-2 text-slate-900 font-sans font-medium">{{ r.cardIssuer }}</td>
            <td class="p-2 text-slate-500">{{ r.userNo }}</td>
            <td class="p-2">
              <span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700">Y</span>
            </td>
            <td class="p-2 text-center">
              <button
                @click="applyIdtagToTester(r.idtag)"
                class="px-2 py-0.5 rounded bg-sky-50 hover:bg-sky-100 text-sky-700 border border-sky-200 font-bold cursor-pointer shadow-2xs"
                title="이 RFID 카드를 테스터 발송 태그로 즉시 적용"
              >
                ⚡ 테스터 적용
              </button>
            </td>
            <td class="p-2 text-center">
              <div class="flex items-center justify-center gap-1">
                <button @click="openEditModal(r)" class="px-1.5 py-0.5 rounded hover:bg-slate-100 text-slate-600">수정</button>
                <button @click="handleDelete(r.idtag)" class="px-1.5 py-0.5 rounded hover:bg-rose-50 text-rose-600">삭제</button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredRfids.length === 0">
            <td colspan="7" class="p-6 text-center text-slate-400 font-sans">등록된 RFID 카드 데이터가 없습니다.</td>
          </tr>
        </tbody>
      </table>

      <!-- 2-4. VHRNO 인증 테이블 -->
      <table v-else-if="activeSubTab === 'vhrno'" class="w-full border-collapse bg-white rounded-lg border border-slate-200 shadow-2xs overflow-hidden">
        <thead class="bg-slate-50 border-b border-slate-200 text-slate-500 font-mono text-[11px] text-left">
          <tr>
            <th class="p-2 font-bold">OCPP idTag</th>
            <th class="p-2 font-bold">매핑 차량번호 (vhrNo)</th>
            <th class="p-2 font-bold">소속 회원</th>
            <th class="p-2 font-bold">오토차지 승인</th>
            <th class="p-2 font-bold text-center">테스터 연동</th>
            <th class="p-2 font-bold text-center">관리</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100 font-mono text-[11px]">
          <tr v-for="v in filteredVhrnos" :key="v.idtag" class="hover:bg-slate-50/80 transition-colors">
            <td class="p-2 font-bold text-emerald-800">{{ v.idtag }}</td>
            <td class="p-2 text-slate-900 font-bold">{{ v.vhrNo }}</td>
            <td class="p-2 text-slate-500">{{ v.userNo }}</td>
            <td class="p-2">
              <span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800">ACTIVE</span>
            </td>
            <td class="p-2 text-center">
              <button
                @click="applyIdtagToTester(v.idtag)"
                class="px-2 py-0.5 rounded bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 font-bold cursor-pointer shadow-2xs"
              >
                ⚡ 테스터 적용
              </button>
            </td>
            <td class="p-2 text-center">
              <div class="flex items-center justify-center gap-1">
                <button @click="openEditModal(v)" class="px-1.5 py-0.5 rounded hover:bg-slate-100 text-slate-600">수정</button>
                <button @click="handleDelete(v.idtag)" class="px-1.5 py-0.5 rounded hover:bg-rose-50 text-rose-600">삭제</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 2-5. PCID (PnC) 테이블 -->
      <table v-else-if="activeSubTab === 'pcid'" class="w-full border-collapse bg-white rounded-lg border border-slate-200 shadow-2xs overflow-hidden">
        <thead class="bg-slate-50 border-b border-slate-200 text-slate-500 font-mono text-[11px] text-left">
          <tr>
            <th class="p-2 font-bold">OCPP idTag</th>
            <th class="p-2 font-bold">ISO 15118 eMAID</th>
            <th class="p-2 font-bold">인증서 만료일</th>
            <th class="p-2 font-bold">소속 회원</th>
            <th class="p-2 font-bold text-center">테스터 연동</th>
            <th class="p-2 font-bold text-center">관리</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100 font-mono text-[11px]">
          <tr v-for="p in filteredPcids" :key="p.idtag" class="hover:bg-slate-50/80 transition-colors">
            <td class="p-2 font-bold text-amber-800">{{ p.idtag }}</td>
            <td class="p-2 text-slate-900 font-bold">{{ p.emaid }}</td>
            <td class="p-2 text-slate-600">{{ p.certExpireDt }}</td>
            <td class="p-2 text-slate-500">{{ p.userNo }}</td>
            <td class="p-2 text-center">
              <button
                @click="applyIdtagToTester(p.idtag)"
                class="px-2 py-0.5 rounded bg-amber-50 hover:bg-amber-100 text-amber-800 border border-amber-200 font-bold cursor-pointer shadow-2xs"
              >
                ⚡ 테스터 적용
              </button>
            </td>
            <td class="p-2 text-center">
              <div class="flex items-center justify-center gap-1">
                <button @click="openEditModal(p)" class="px-1.5 py-0.5 rounded hover:bg-slate-100 text-slate-600">수정</button>
                <button @click="handleDelete(p.idtag)" class="px-1.5 py-0.5 rounded hover:bg-rose-50 text-rose-600">삭제</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 3. 등록/수정 모달 팝업 -->
    <div v-if="isModalOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4">
      <div class="bg-white rounded-xl border border-slate-200 shadow-xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        <div class="px-4 py-3 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
          <h3 class="text-xs font-bold text-slate-900">
            {{ isEditMode ? '정보 수정' : '신규 등록' }} -
            {{
              activeSubTab === 'user' ? '사용자(회원)' :
              activeSubTab === 'vehicle' ? '차량([1826])' :
              activeSubTab === 'rfid' ? 'RFID 카드' :
              activeSubTab === 'vhrno' ? 'VHRNO 인증' : 'PnC (PCID)'
            }}
          </h3>
          <button @click="isModalOpen = false" class="text-slate-400 hover:text-slate-700 cursor-pointer">✕</button>
        </div>

        <div class="p-4 space-y-3">
          <!-- 사용자 폼 -->
          <template v-if="activeSubTab === 'user'">
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">회원번호 (userNo)</label>
              <input v-model="userForm.userNo" :disabled="isEditMode" class="w-full bg-slate-50 border border-slate-200 rounded px-2.5 py-1.5 font-mono text-xs" />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">회원명 / 상호</label>
              <input v-model="userForm.userNm" class="w-full bg-white border border-slate-200 rounded px-2.5 py-1.5 text-xs" />
            </div>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
              <div>
                <label class="block text-[11px] font-bold text-slate-700 mb-1">법인 ID</label>
                <input v-model="userForm.corpId" class="w-full bg-white border border-slate-200 rounded px-2.5 py-1.5 font-mono text-xs" />
              </div>
              <div>
                <label class="block text-[11px] font-bold text-slate-700 mb-1">회원 유형</label>
                <select v-model="userForm.userTypeCd" class="w-full bg-white border border-slate-200 rounded px-2 py-1.5 text-xs">
                  <option value="C">법인 (C)</option>
                  <option value="M">모바일 일반 (M)</option>
                  <option value="K">로밍 (K)</option>
                  <option value="N">비회원 (N)</option>
                </select>
              </div>
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">연락처</label>
              <input v-model="userForm.phone" class="w-full bg-white border border-slate-200 rounded px-2.5 py-1.5 text-xs" />
            </div>
          </template>

          <!-- 차량 폼 -->
          <template v-else-if="activeSubTab === 'vehicle'">
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">차량번호 (vhrNo)</label>
              <input v-model="vehicleForm.vhrNo" :disabled="isEditMode" placeholder="예: 서울70사1234" class="w-full bg-slate-50 border border-slate-200 rounded px-2.5 py-1.5 font-mono text-xs" />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">차종명</label>
              <input v-model="vehicleForm.modelName" placeholder="예: 현대 일렉시티 더블데커" class="w-full bg-white border border-slate-200 rounded px-2.5 py-1.5 text-xs" />
            </div>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
              <div>
                <label class="block text-[11px] font-bold text-slate-700 mb-1">배터리 용량 (kWh)</label>
                <input v-model.number="vehicleForm.batteryCapacity" type="number" class="w-full bg-white border border-slate-200 rounded px-2.5 py-1.5 font-mono text-xs" />
              </div>
              <div>
                <label class="block text-[11px] font-bold text-slate-700 mb-1">최적 목표 SOC (%)</label>
                <input v-model.number="vehicleForm.optimalSoc" type="number" class="w-full bg-white border border-slate-200 rounded px-2.5 py-1.5 font-mono text-xs" />
              </div>
            </div>
          </template>

          <!-- RFID 폼 -->
          <template v-else-if="activeSubTab === 'rfid'">
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">OCPP idTag</label>
              <input v-model="rfidForm.idtag" :disabled="isEditMode" class="w-full bg-slate-50 border border-slate-200 rounded px-2.5 py-1.5 font-mono text-xs" />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">실물 카드번호 (16자리)</label>
              <input v-model="rfidForm.cardNo" class="w-full bg-white border border-slate-200 rounded px-2.5 py-1.5 font-mono text-xs" />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">카드 발급사</label>
              <input v-model="rfidForm.cardIssuer" class="w-full bg-white border border-slate-200 rounded px-2.5 py-1.5 text-xs" />
            </div>
          </template>

          <!-- VHRNO 폼 -->
          <template v-else-if="activeSubTab === 'vhrno'">
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">OCPP idTag</label>
              <input v-model="vhrnoForm.idtag" :disabled="isEditMode" class="w-full bg-slate-50 border border-slate-200 rounded px-2.5 py-1.5 font-mono text-xs" />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">매핑 차량번호 (vhrNo)</label>
              <input v-model="vhrnoForm.vhrNo" class="w-full bg-white border border-slate-200 rounded px-2.5 py-1.5 font-mono text-xs" />
            </div>
          </template>

          <!-- PCID 폼 -->
          <template v-else-if="activeSubTab === 'pcid'">
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">OCPP idTag</label>
              <input v-model="pcidForm.idtag" :disabled="isEditMode" class="w-full bg-slate-50 border border-slate-200 rounded px-2.5 py-1.5 font-mono text-xs" />
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-700 mb-1">ISO 15118 eMAID</label>
              <input v-model="pcidForm.emaid" class="w-full bg-white border border-slate-200 rounded px-2.5 py-1.5 font-mono text-xs" />
            </div>
          </template>
        </div>

        <div class="px-4 py-3 bg-slate-50 border-t border-slate-200 flex items-center justify-end gap-2">
          <button @click="isModalOpen = false" class="px-3 py-1.5 rounded-lg bg-white border border-slate-300 text-slate-700 hover:bg-slate-100 font-bold">취소</button>
          <button @click="saveModalForm" class="px-3 py-1.5 rounded-lg bg-slate-900 text-white hover:bg-slate-800 font-bold shadow-2xs">저장</button>
        </div>
      </div>
    </div>
  </div>
</template>
