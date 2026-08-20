# ELVIS-CSMS 관제 데스크톱 센터 (`elvis-control-ui`)

**Tauri 2.0 + Vue 3 + Pure Tailwind CSS + ECharts** 기반의 **단독 실행형(Standalone Desktop App)** 관제 센터 구현체입니다.

별도의 무거운 외부 UI 라이브러리 의존성 없이 순수 Tailwind CSS 기반 초경량 Zero-Dependency CSMS 관제 솔루션을 제공합니다.

---

## ✨ 주요 특징

- 🚀 **초경량 데스크톱 네이티브 앱**: WebView2 + Rust 기반으로 메모리 사용량 최소화 및 초고속 기동 속도 제공
- 📊 **고성능 대시보드 시각화**:
  - `Apache ECharts`: 실시간 전력량(kW) & TOU 단가(원/kWh) 시계열 줌/패닝 차트
  - `Pure Tailwind & Pinia`: 2,000대 충전기 실시간 상태 그리드 및 원격 제어(시작/중지/리셋)
  - `OCPP WireTap`: 실시간 Inbound/Outbound OCPP JSON 패킷 스트림 디버거
- 🌙 **글래스모피즘 다크 테마**: 장시간 관제 모니터링 시 피로도를 최소화하는 현대적인 관제실 전용 다크 UI

---

## ⚡ 빠른 시작 (Quick Start)

### 1. 프론트엔드 의존성 설치
```powershell
cd elvis-control-ui
pnpm install
```

### 2. 웹 브라우저 개발 모드 실행
Rust 컴파일 없이 브라우저(`http://localhost:1420`)에서 관제 UI를 즉시 확인합니다.
```powershell
pnpm dev
```

### 3. 데스크톱 앱 개발 모드 실행
Windows 네이티브 앱 창을 띄워 실시간 핫리로드 개발을 진행합니다.
```powershell
pnpm tauri:dev
```

### 4. 앱 아이콘 일괄 생성 (SVG 기반)
```powershell
pnpm tauri icon ./public/ELVIS-LITE-ICON.svg
```

### 5. 독립 실행 배포 파일 빌드 (.exe)
```powershell
pnpm tauri:build
# -> src-tauri/target/release/ELVIS-CSMS-ControlCenter.exe 생성
```

---

## 📚 상세 개발 및 트러블슈팅 가이드

- 🛠️ **[개발 및 빌드 상세 가이드 (DEVELOPMENT.md)](./DEVELOPMENT.md)**: 사전 요구사항, Tauri/Cargo 아키텍처, 빌드/테스트 절차
- 🔍 **[오류 해결 및 트러블슈팅 가이드 (TROUBLESHOOTING.md)](./TROUBLESHOOTING.md)**: Tauri/Rust/Node.js/Vite 오류 사례별 해결법

