# ELVIS-CSMS 관제 데스크톱 센터 개발 가이드 (DEVELOPMENT.md)

이 문서는 **ELVIS-CSMS 데스크톱 관제 센터(`elvis-control-ui`)**의 개발 환경 구축, 구조 설명, 설정(Configuration) 상세, 빌드/테스트 및 배포 절차를 상세히 다룹니다.

---

## 목차
1. [개발 환경 사전 요구사항 (Prerequisites)](#1-개발-환경-사전-요구사항-prerequisites)
2. [프로젝트 아키텍처 및 내부 구조](#2-프로젝트-아키텍처-및-내부-구조)
3. [Tauri 2.0 프레임워크 구성 및 설정 상세](#3-tauri-20-프레임워크-구성-및-설정-상세)
   - [3.1 Tauri 패키지 구성 (`package.json`)](#31-tauri-패키지-구성-packagejson)
   - [3.2 핵심 설정 파일 (`src-tauri/tauri.conf.json`)](#32-핵심-설정-파일-src-tauritauriconfjson)
   - [3.3 데스크톱 필수 아이콘 리소스 생성 가이드 (`src-tauri/icons/`)](#33-데스크톱-필수-아이콘-리소스-생성-가이드-src-tauriicons)
4. [Cargo (Rust) 프로젝트 설정 및 의존성 상세](#4-cargo-rust-프로젝트-설정-및-의존성-상세)
   - [4.1 Cargo 매니페스트 (`src-tauri/Cargo.toml`)](#41-cargo-매니페스트-src-tauricargotoml)
   - [4.2 주요 크레이트(Crates) 및 설정 설명](#42-주요-크레이트crates-및-설정-설명)
   - [4.3 Rust 소스 파일 구조 및 역할](#43-rust-소스-파일-구조-및-역할)
5. [주요 설정 파일 가이드 (Configuration Guide)](#5-주요-설정-파일-가이드-configuration-guide)
   - [5.1 Vite 개발 서버 및 프록시 설정 (`vite.config.ts`)](#51-vite-개발-서버-및-프록시-설정-viteconfigts)
   - [5.2 TypeScript 컴파일러 설정 (`tsconfig.json`)](#52-typescript-컴파일러-설정-tsconfigjson)
   - [5.3 패키지 매니페스트 및 실행 스크립트 (`package.json`)](#53-패키지-매니페스트-및-실행-스크립트-packagejson)
   - [5.4 Tailwind CSS & PostCSS 스타일링 설정 (`tailwind.config.js`, `postcss.config.js`)](#54-tailwind-css--postcss-스타일링-설정-tailwindconfigjs-postcssconfigjs)
   - [5.5 전체 CSMS 백엔드/미들웨어 설정 매핑 (`config/` 디렉터리)](#55-전체-csms-백엔드미들웨어-설정-매핑-config-디렉터리)
6. [로컬 개발 실행 가이드](#6-로컬-개발-실행-가이드)
7. [테스트 및 코드 품질 검증](#7-테스트-및-코드-품질-검증)
8. [프로덕션 패키징 및 배포 (.exe)](#8-프로덕션-패키징-및-배포-exe)
9. [트러블슈팅 및 FAQ](#9-트러블슈팅-및-faq)

---

## 1. 개발 환경 사전 요구사항 (Prerequisites)

Windows 데스크톱 독립 실행 파일(`.exe`)을 빌드하고 Tauri 네이티브 기능을 개발하기 위해 아래 도구들이 필요합니다.

### 1.1 필수 도구 설치

| 도구명 | 요구 버전 | 용도 및 설치 링크 |
| :--- | :--- | :--- |
| **Node.js** | v18.0 이상 (v24.x 권장) | 프론트엔드 런타임 ([공식 다운로드](https://nodejs.org/) 또는 `fnm`/`nvm`) |
| **pnpm** | v9.0 이상 (v10+ 권장) | 고속 패키지 관리자 (`npm i -g pnpm` 또는 `corepack enable`) |
| **MSVC C++ 빌드 도구** | VS 2019 / 2022 | Windows 네이티브 바이너리 컴파일 및 링크 ([Build Tools 다운로드](https://visualstudio.microsoft.com/visual-cpp-build-tools/))<br>※ `C++를 사용한 데스크톱 개발` 워크로드 설치 필수 |
| **Rust 툴체인 (`rustup`)** | 1.80 이상 (최신 안정판) | Tauri 네이티브 백엔드 런타임 ([rustup.rs 다운로드](https://rustup.rs/))<br>※ 설치 프롬프트에서 `1` (기본값) 선택 |
| **Microsoft Edge WebView2** | Evergreen Runtime | 데스크톱 UI 웹 렌더러 (Windows 10/11 기본 탑재, [다운로드](https://developer.microsoft.com/en-us/microsoft-edge/webview2/)) |

> [!IMPORTANT]
> Rust 설치 완료 후 반드시 **VS Code 및 터미널 창을 완전히 재시작**하여 시스템 `PATH` 환경변수를 갱신해야 합니다.

### 1.2 설치 상태 확인 명령어
```powershell
# Node.js 및 pnpm 버전 확인
node -v
pnpm -v

# Rust 컴파일러 및 도구 버전 확인
rustc --version
cargo --version
rustup --version
```

---

## 2. 프로젝트 아키텍처 및 내부 구조

`elvis-control-ui`는 **Tauri v2 + Vue 3 + Pure Tailwind CSS + ECharts + pnpm** 기술 스택으로 구성되어 있습니다. 외부 무거운 UI 컴포넌트 프레임워크 없이 라이트 테마 기본 디자인 시스템을 사용합니다.

```text
elvis-control-ui/
├── src/                          # Vue 3 프론트엔드 소스
│   ├── views/                    # 주요 관제 화면 컴포넌트
│   │   ├── ChargersView.vue      # 실시간 충전기 통합 관제 그리드 및 원격 제어반
│   │   ├── BillingCdrView.vue    # 충전 거래 세션 및 과금 원장 관리
│   │   └── LoginView.vue         # 관리자 JWT 로그인 화면
│   ├── stores/                   # Pinia 전역 상태 관리
│   │   ├── authStore.ts          # JWT 인증, 토큰 만료 검증 및 재발급
│   │   └── csmsStore.ts          # 실시간 텔레메트리, 충전기 상태, 원격 제어 액션
│   ├── utils/                    # 공통 유틸리티
│   │   └── apiClient.ts          # JWT Authorization 자동 주입 및 401 Single-Flight Refresh
│   ├── router/                   # Vue Router 네비게이션 설정
│   │   └── index.ts
│   ├── styles/                   # 글로벌 스타일시트
│   │   └── main.css              # Tailwind 지시어 & 라이트 기본 멀티 테마 디자인 토큰
│   ├── App.vue                   # 데스크톱 커스텀 윈도우 타이틀바 & 레이아웃
│   └── main.ts                   # Vue 3 엔트리 포인트
├── src-tauri/                    # Tauri v2 / Rust 네이티브 백엔드
│   ├── capabilities/             # Tauri v2 창 제어 및 플러그인 권한 설정
│   │   └── default.json
│   ├── icons/                    # 필수 앱 아이콘 리소스 (Windows .ico, macOS .icns, PNG 세트)
│   ├── src/
│   │   ├── lib.rs                # Tauri v2 메인 라이브러리 진입점 (run 함수)
│   │   └── main.rs               # 데스크톱 실행 바이너리 엔트리 포인트
│   ├── build.rs                  # Tauri 빌드 스크립트
│   ├── Cargo.toml                # Rust 의존성 및 패키지 설정
│   └── tauri.conf.json           # 윈도우 크기(1920x1080 FHD), 보안(CSP), 번들링 설정
├── .nvmrc                        # Node.js 버전 고정
├── package.json                  # Node.js 의존성 및 실행 스크립트
├── pnpm-lock.yaml                # pnpm 고정 잠금 파일
├── tailwind.config.js            # Tailwind CSS 설정 파일
├── postcss.config.js             # PostCSS 설정 파일
├── tsconfig.json                 # TypeScript 컴파일 설정
└── vite.config.ts                # Vite 번들러 설정 (포트 1421)
```

---

## 3. Tauri 2.0 프레임워크 구성 및 설정 상세

Tauri는 가벼운 Rust 백엔드와 OS 내장 웹뷰(Windows WebView2)를 결합하여 Electron 대비 적은 메모리 사용량과 경량 실행 파일을 제공하는 데스크톱 앱 프레임워크입니다.

### 3.1 Tauri 패키지 구성 (`package.json`)
- **`@tauri-apps/cli` (`^2.0.0`)**: Tauri 데스크톱 애플리케이션의 개발 서버 실행(`dev`), 바이너리 빌드(`build`), 아이콘 생성 등을 담당하는 CLI 도구입니다.
- **`@tauri-apps/api` (`^2.0.0`)**: 프론트엔드(Vue 3)에서 Tauri Rust 코어 및 윈도우 제어 기능(`getCurrentWindow`)을 호출하기 위한 TypeScript API 라이브러리입니다.
- **`@tauri-apps/plugin-shell` (`^2.0.0`)**: 데스크톱 환경에서 OS 셸 명령 실행 및 외부 기본 브라우저 URL 오픈 기능을 제공하는 공식 플러그인입니다.

### 3.2 핵심 설정 파일 (`src-tauri/tauri.conf.json`)

```json
{
  "$schema": "https://raw.githubusercontent.com/tauri-apps/tauri/2.0.0/crates/tauri-config-schema/schema.json",
  "productName": "ELVIS-CSMS-ControlCenter",
  "version": "1.0.0",
  "identifier": "com.lselink.elvis.csms",
  "build": {
    "beforeDevCommand": "pnpm dev",
    "devUrl": "http://localhost:1421",
    "beforeBuildCommand": "pnpm build",
    "frontendDist": "../dist"
  },
  "app": {
    "windows": [
      {
        "title": "ELVIS-CSMS 관제 센터",
        "width": 1920,
        "height": 1080,
        "minWidth": 1280,
        "minHeight": 720,
        "resizable": true,
        "fullscreen": false,
        "decorations": true,
        "transparent": false,
        "center": true
      }
    ],
    "security": {
      "csp": null
    }
  },
  "bundle": {
    "active": true,
    "targets": "all",
    "icon": [
      "icons/32x32.png",
      "icons/128x128.png",
      "icons/128x128@2x.png",
      "icons/icon.icns",
      "icons/icon.ico"
    ]
  }
}
```

- **`identifier`**: 애플리케이션의 고유 역도메인 식별자(`com.lselink.elvis.csms`)로 OS 시스템 등록 및 설정 격리에 사용됩니다.
- **`build` 파이프라인**:
  - `beforeDevCommand`: `pnpm tauri:dev` 실행 시 프론트엔드 Vite 개발 서버(`pnpm dev`)를 백그라운드에서 자동 기동합니다.
  - `devUrl`: Tauri 윈도우가 실시간 HMR(Hot Module Replacement)로 연결할 프론트엔드 URL(`http://localhost:1421`)입니다.
  - `beforeBuildCommand` & `frontendDist`: 프로덕션 빌드 시 Vue 산출물(`../dist`)을 생성하고 이를 최종 단일 바이너리에 임베딩합니다.
- **`app.windows`**: 관제 대시보드에 최적화된 기본 해상도(`1920x1080 FHD`, 최소 `1280x720`) 및 화면 중앙 배치(`center: true`)를 정의합니다.
- **`bundle.icon`**: Windows 실행 파일 및 각 플랫폼별 번들에 탑재될 아이콘 경로 목록을 지정합니다.

### 3.3 데스크톱 및 웹 앱 필수 아이콘 생성 가이드 (`src-tauri/icons/`)

Tauri 2.0 프레임워크는 Windows 실행 파일(`.exe`)의 리소스 파일(`.rc`), 작업표시줄, macOS 앱 번들(`.icns`) 및 웹 파비콘을 위해 다양한 해상도(16×16 ~ 512×512)의 멀티 플랫폼 아이콘 세트를 요구합니다.

#### 3.3.1 권장 원본 규격
- **권장 원본 포맷**: `1024×1024` 규격의 **SVG 벡터 파일** (`public/ELVIS-LITE-ICON.svg`) 또는 투명 배경 고해상도 PNG

#### 3.3.2 1-Command 자동 일괄 생성 (Tauri CLI)
```powershell
# elvis-control-ui 디렉터리에서 실행
pnpm tauri icon ./public/ELVIS-LITE-ICON.svg
```

#### 3.3.3 웹 파비콘 동기화 복사 명령어
```powershell
Copy-Item "src-tauri/icons/icon.ico" -Destination "public/favicon.ico" -Force
Copy-Item "src-tauri/icons/32x32.png" -Destination "public/favicon.png" -Force
Copy-Item "public/ELVIS-LITE-ICON.svg" -Destination "public/favicon.svg" -Force
```

---

## 4. Cargo (Rust) 프로젝트 설정 및 의존성 상세

### 4.1 Cargo 매니페스트 (`src-tauri/Cargo.toml`)

```toml
[package]
name = "elvis-control-ui"
version = "1.0.0"
description = "ELVIS-CSMS Desktop Standalone Control Center"
authors = ["LS E-Link Platform Team"]
edition = "2021"

[lib]
name = "elvis_control_ui_lib"
crate-type = ["staticlib", "cdylib", "rlib"]

[build-dependencies]
tauri-build = { version = "2", features = [] }

[dependencies]
tauri = { version = "2", features = ["tray-icon"] }
tauri-plugin-shell = "2"
serde = { version = "1", features = ["derive"] }
serde_json = "1"

[features]
devtools = ["tauri/devtools"]
```

### 4.2 Rust 소스 파일 구조 및 역할

- **`src-tauri/build.rs`**: Cargo 빌드 스크립트로 `tauri_build::build()`를 호출하여 Tauri 환경을 사전 구성합니다.
- **`src-tauri/src/lib.rs`**: 앱의 실질적인 백엔드 코어 진입점으로 `tauri::Builder`를 통해 플러그인 초기화, 트레이 메뉴 및 런타임을 구동합니다.
- **`src-tauri/src/main.rs`**: Windows 네이티브 실행 파일의 엔트리 포인트입니다.

---

## 5. 주요 설정 파일 가이드 (Configuration Guide)

### 5.1 Vite 개발 서버 및 프록시 설정 (`vite.config.ts`)

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  clearScreen: false,
  server: {
    port: 1421,
    strictPort: true,
    host: '127.0.0.1',
    watch: {
      ignored: ['**/src-tauri/**']
    },
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:9082',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://127.0.0.1:9080',
        ws: true
      },
      '/ws-push': {
        target: 'ws://127.0.0.1:9082',
        ws: true
      }
    }
  },
  envPrefix: ['VITE_', 'TAURI_ENV_*'],
  build: {
    target: process.env.TAURI_ENV_PLATFORM == 'windows' ? 'chrome105' : 'safari13',
    minify: !process.env.TAURI_ENV_DEBUG ? 'esbuild' : false,
    sourcemap: !!process.env.TAURI_ENV_DEBUG,
    outDir: 'dist'
  }
})
```

---

### 5.2 TypeScript 컴파일러 설정 (`tsconfig.json`)

Vue 3 Composition API 및 TypeScript 환경의 엄격한 타입 검사와 모듈 경로 별칭(`@/*`)을 정의합니다.

---

### 5.3 패키지 매니페스트 및 실행 스크립트 (`package.json`)

- **`pnpm dev`**: 웹 브라우저(`http://127.0.0.1:1421`) 전용 초고속 HMR 모드
- **`pnpm tauri:dev`**: Tauri 데스크톱 네이티브 윈도우 실시간 개발 모드
- **`pnpm tauri:build`**: 독립 실행형 단일 `.exe` 프로덕션 바이너리 생성

---

### 5.4 Tailwind CSS & PostCSS 스타일링 설정 (`tailwind.config.js`, `postcss.config.js`)

관제 센터의 **라이트 테마 기본(Light Theme Default)** 레이아웃과 `Pretendard` / `JetBrains Mono` 타이포그래피를 일관되게 렌더링하기 위해 Tailwind CSS를 사용합니다.

- **`tailwind.config.js`**:
  ```javascript
  /** @type {import('tailwindcss').Config} */
  export default {
    content: [
      "./index.html",
      "./src/**/*.{vue,js,ts,jsx,tsx}",
    ],
    theme: {
      extend: {
        fontFamily: {
          sans: ['Pretendard', '-apple-system', 'BlinkMacSystemFont', 'system-ui', 'sans-serif'],
          mono: ['JetBrains Mono', 'Inconsolata', 'monospace'],
        },
      },
    },
    plugins: [],
  }
  ```

- **`src/styles/main.css`**: 라이트 테마(`:root`, `[data-theme="light"]`)를 기본으로 `cyber`, `emerald`, `purple`, `amber` 디자인 토큰을 제공합니다.

---

### 5.5 전체 CSMS 백엔드/미들웨어 설정 매핑 (`config/` 디렉터리)

관제 UI(`elvis-control-ui`)가 데이터를 주고받는 전체 CSMS 플랫폼의 백엔드 및 미들웨어 설정 파일 구조입니다.

```text
config/
├── control-api/
│   └── application.yml        # Admin REST API (포트 9082, UI의 /api, /ws-push 프록시 대상)
├── connect-ws/
│   └── application.yml        # WebSocket Gateway (포트 9080, UI의 /ws 프록시 대상)
├── connect-sync/
│   └── application.yml        # Parser & Sync Engine (포트 9081, Kafka/MySQL 연동)
├── mysql/
│   └── my.ini                 # MySQL 9.71 인코딩, 타임존(KST), 포트(13306)
└── kafka/
    └── server.properties      # Apache Kafka 4.3.1 KRaft 브로커(19092)
```

- **포트 연계 요약**:
  - `UI (1421)` -> `/api`, `/ws-push` 호출 -> `control-api (9082)`
  - `UI (1421)` -> `/ws` 접속 -> `connect-ws (9080)`
  - `connect-sync (9081)` <-> `Kafka (19092)` / `MySQL (13306)`

---

## 6. 로컬 개발 실행 가이드

### 6.1 프론트엔드 의존성 설치
```powershell
cd elvis-control-ui
pnpm install
```

### 6.2 웹 브라우저 개발 모드 (Vite Dev)
Rust 컴파일 없이 웹 브라우저(`http://127.0.0.1:1421`)에서 UI 컴포넌트와 화면 레이아웃을 즉시 개발/테스트할 수 있습니다.
```powershell
pnpm dev
```

### 6.3 데스크톱 단독 앱 개발 모드 (Tauri Dev)
Rust 백엔드와 Windows 네이티브 창을 결합하여 실시간 핫리로드(Hot-Reload) 상태로 개발합니다.
```powershell
pnpm tauri:dev
```

---

## 7. 테스트 및 코드 품질 검증

### 7.1 Rust 백엔드 검사 (`src-tauri`)
```powershell
cd src-tauri
cargo check
cargo test
```

### 7.2 프론트엔드 검사 및 빌드 검증
```powershell
# elvis-control-ui 루트 디렉토리
pnpm build
```

---

## 8. 프로덕션 패키징 및 배포 (.exe)

Windows 독립 실행형 단일 바이너리(`.exe`)를 생성합니다. 외부 런타임(Node.js 등) 설치 없이 바로 실행되는 Zero-Dependency 파일이 생성됩니다.

```powershell
pnpm tauri:build
```

- **빌드 결과물 생성 경로**: `src-tauri/target/release/`
- **생성 파일**: `ELVIS-CSMS-ControlCenter.exe`

---

## 9. 트러블슈팅 및 FAQ
자주 발생하는 오류 증상별 원인 및 해결 방법은 **[오류 해결 및 트러블슈팅 상세 가이드 (TROUBLESHOOTING.md)](./TROUBLESHOOTING.md)**를 참고하세요.
