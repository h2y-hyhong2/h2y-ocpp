---
description: [전사/공통 UI 규칙] 웹/앱 UI 디자인 및 기본 테마 표준 규칙
---

# 🎨 UI 디자인 및 테마 표준 규칙

## 1. ☀️ 기본 테마 (Light Theme Default)
* **원칙**: 모든 UI 화면, 데스크톱 런처, 관제 웹 애플리케이션의 기본(Default) 테마는 항상 **라이트 테마 (Light Theme)**로 구성합니다.
* 다크 모드가 필요한 경우 사용자가 선택할 수 있는 부가 옵션(토글)으로 지원할 수 있으나, **초기 로딩 및 기본 상태는 반드시 라이트 테마**여야 합니다.
* 시스템 설정(OS prefers-color-scheme)에 종속되지 않고, 기본 뷰는 밝고 선명한 화이트/슬레이트 라이트 팔레트를 우선 적용합니다.

## 2. 🪄 모던 & 심플 디자인 철학 (Modern & Simple)
1. **시안/프로토타입 요소 배제**:
   - 불필요한 이모지(🚀, ⚡, 📦 등) 및 산만한 가이드 배너를 제거합니다.
   - 직관적이고 미니멀한 인라인 SVG 벡터 아이콘과 정제된 타이포그래피를 적용합니다.
2. **세련된 컬러 시스템**:
   - 메인 배경: `#F8FAFC` (Slate-50) 또는 `#FFFFFF` 기반의 부드럽고 눈이 편안한 밝은 뉴트럴 톤.
   - 서피스/카드: 순백색(`#FFFFFF`), 얇고 정교한 경계선(`border-slate-200`), 은은한 소프트 섀도우(`shadow-xs`, `shadow-sm`).
   - 텍스트 계층: 메인 텍스트(`text-slate-900`), 서브텍스트(`text-slate-500` ~ `text-slate-600`), 레이블(`text-slate-400`).
   - 상태 컬러:
     - 성공/정상/ONLINE: Emerald (`bg-emerald-50`, `text-emerald-700`, `border-emerald-200`)
     - 중지/에러/OFFLINE: Slate/Rose (`bg-slate-100`, `text-slate-600`, `border-slate-200` / `bg-rose-50`, `text-rose-700`)
     - 주요 액션/포커스: Sky/Indigo (`bg-sky-600`, `bg-indigo-600`)
3. **가독성 및 사용성 최우선**:
   - 실시간 로그, 코드 영역 등 전문 엔지니어링 요소는 고대비 폰트(`JetBrains Mono`, `Pretendard`)와 적절한 패딩을 부여하여 최적의 가독성을 보장합니다.
