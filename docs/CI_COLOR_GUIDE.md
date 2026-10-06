# 🎨 LS E-Link 통합 브랜드 컬러 가이드 (CI/BI Master Color Guide)

본 문서는 **E-LINK 브랜드**와 **LS 브랜드**의 공식 컬러 시스템을 통합 정리한 마스터 가이드입니다.  
디지털 관제 시스템(CSMS), 웹/앱 개발, 디자인 시스템 및 인쇄물 제작 시 표준 지침으로 활용합니다.

---

## 1. 브랜드별 컬러 규격 마스터 시트

### 1) E-LINK 브랜드 컬러 (E-LINK Brand Colors)

| 분류 | 색상명 | PANTONE | Process Color (CMYK) | RGB Color | Hex Code | 주요 용도 |
| :---: | :--- | :--- | :--- | :--- | :---: | :--- |
| **메인** | **E-LINK Blue** | PANTONE 281C | C100 M80 Y25 K0 | R10 G30 B90 | `#0A1E5A` | 브랜드 코어 앵커, 심볼 마크 |
| **서브** | **E-LINK Orange** | PANTONE 1669C | C0 M72 Y100 K3 | R233 G102 B0 | `#E96600` | 아이덴티티 포인트, 주의/대기 뱃지 |
| **서브** | **E-LINK Yellow** | PANTONE 108C | C0 M15 Y100 K0 | R255 G216 B0 | `#FFD800` | 보조 포인트, 예약/준비 인디케이터 |

### 2) LS 브랜드 컬러 (LS Brand Colors)

| 분류 | 색상명 | PANTONE | Process Color (CMYK) | RGB Color | Hex Code | 주요 용도 |
| :---: | :--- | :--- | :--- | :--- | :---: | :--- |
| **메인** | **LS BLUE** | PANTONE 281C | C100 M80 Y25 K0 | R10 G30 B90 | `#0A1E5A` | 그룹 공통 메인 아이덴티티 |
| **메인** | **LS RED** | PANTONE 199C | C5 M100 Y80 K0 | R250 G0 B45 | `#FA002D` | 시그니처 포인트, 긴급/장애 알람 |
| **서브** | **GREEN** | PANTONE 312C | C100 M0 Y15 K0 | R0 G155 B180 | `#009B74` | 친환경/에너지, 충전 중 상태 |
| **서브** | **BLUE** | PANTONE 3005C | C100 M30 Y0 K5 | R5 G105 B160 | `#0569A0` | 인터랙티브 액션, 사용 가능 상태 |
| **서브** | **GRAY** | PANTONE 430C | C5 M0 Y0 K50 | R125 G130 B130 | `#7D8282` | 보조 텍스트, 비활성(Disabled) |
| **서브** | **SILVER (메탈릭)** | PANTONE 877C | C40 M30 Y30 K13 | R135 G130 B125 | `#87827D` | 특수 인쇄, 메탈릭 질감 |
| **서브** | **GOLD (메탈릭)** | PANTONE 872C | C30 M40 Y80 K18 | R125 G13 B13 | `#7D0D0D` | 프리미엄 엠블럼, 인증 마크 |

---

## 2. 통합 분석 및 디지털 디자인 원칙

### 1) 핵심 공통 색상 (Core Identity)
* **`PANTONE 281C` (`#0A1E5A`)**: E-LINK Blue와 LS BLUE는 완전히 동일한 색상입니다.
* 그룹 및 합작 브랜드의 결합 시 가장 신뢰감을 주는 시각적 연결 고리(Identity Anchor) 역할을 합니다.

### 2) 관제 시스템(CSMS) "60-30-10" 컬러 위계 원칙
화면이 지나치게 원색 위주로 어지러워지는 것을 방지하고 눈의 피로도를 최소화하기 위해 다음 배분 원칙을 적용합니다:
* **Base (60%)**: 딥 다크 네이비 (`#0A0E1A`) / 다크 슬레이트 (`#1E293B`) / 클린 그레이 (`#F8FAFC`) 등 차분한 뉴트럴 톤으로 전체 화면 바탕 구성.
* **Brand Identity (30%)**: 공통 메인 컬러인 **`#0A1E5A` (LS/E-LINK Blue)**를 헤더(GNB), 사이드바 기본 톤으로 사용하여 신뢰감 부여.
* **Accent & Status (10%)**: 유채색(Red, Orange, Yellow, Green)은 **상태 표현(Semantic Status)** 및 **로고 마이크로 포인트**로만 엄격히 제한.

---

## 3. 충전기 실시간 관제 상태(Semantic) 매핑 표준

| 관제 상태 | 적용 브랜드 컬러 | Hex Code | UI 연출 기법 |
| :--- | :--- | :---: | :--- |
| **충전 중 (Charging)** | **LS Green** | `#009B74` | 15% 반투명 틴트 배경 + 그린 텍스트/도트 |
| **대기 / 사용가능 (Available)** | **LS Sub Blue** | `#0569A0` | 부드러운 스카이블루 톤 뱃지 |
| **준비 / 연결 대기 (Preparing)** | **E-LINK Yellow** | `#FFD800` | 옐로우/앰버 텍스트 + 점멸 인디케이터 |
| **점검 중 / 예약 (Suspended/Reserved)** | **E-LINK Orange** | `#E96600` | 오렌지 뱃지 |
| **고장 / 통신 이상 (Faulted/Unavailable)** | **LS RED** | `#FA002D` | 선명한 레드 뱃지 + 경보음/팝업 연동 |
| **오프라인 (Offline/Disabled)** | **LS GRAY** | `#7D8282` | 톤다운 회색 비활성 처리 |

---

## 4. UI 시그니처 포인트 적용 지침

1. **GNB 최상단 2.5px 그라데이션 라인**:
   - `linear-gradient(90deg, #0A1E5A 0%, #FA002D 50%, #E96600 100%)`
   - 대시보드 창 최상단에 은은하게 브랜드 시그니처 바 배치.
2. **로고 영역 및 버전 뱃지**:
   - 로고 주변 은은한 브랜드 3색 틴트 배경.
   - `v0.0.1` 뱃지에 **E-LINK Orange** (`#E96600`) 반투명 테두리 및 텍스트 적용.
3. **핵심 CTA 액션 버튼**:
   - 주요 관제 기동 버튼에 `LS Blue (#0A1E5A)` ~ `Sub Blue (#0569A0)` 그라디언트 적용.

---

## 5. CSS 디자인 토큰 코드 (Web / Desktop)

```css
:root {
  /* ==========================================================================
     LS E-LINK 마스터 브랜드 컬러 토큰
     ========================================================================== */
  --brand-ls-blue:      #0A1E5A;  /* PANTONE 281C (공통 메인 딥 블루) */
  --brand-ls-red:       #FA002D;  /* PANTONE 199C (LS 메인 레드) */
  --brand-elink-orange: #E96600;  /* PANTONE 1669C (E-LINK 서브 오렌지) */
  --brand-elink-yellow: #FFD800;  /* PANTONE 108C (E-LINK 서브 옐로우) */
  --brand-ls-green:     #009B74;  /* PANTONE 312C (LS 서브 그린) */
  --brand-ls-sub-blue:  #0569A0;  /* PANTONE 3005C (LS 서브 블루) */
  --brand-ls-gray:      #7D8282;  /* PANTONE 430C (LS 서브 그레이) */
  --brand-ls-silver:    #87827D;  /* PANTONE 877C (메탈릭 실버) */
  --brand-ls-gold:      #7D0D0D;  /* PANTONE 872C (메탈릭 골드) */

  /* [UI Soft Tint] 눈부심 방지 15% 반투명 뱃지 배경 */
  --tint-ls-blue:      rgba(10, 30, 90, 0.15);
  --tint-ls-red:       rgba(250, 0, 45, 0.15);
  --tint-elink-orange: rgba(233, 102, 0, 0.15);
  --tint-elink-yellow: rgba(255, 216, 0, 0.15);
  --tint-ls-green:     rgba(0, 155, 116, 0.15);
}
```
