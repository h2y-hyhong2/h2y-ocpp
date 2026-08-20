# ELVIS-CSMS 관제 데스크톱 센터 트러블슈팅 가이드 (TROUBLESHOOTING.md)

이 문서는 **ELVIS-CSMS 데스크톱 관제 센터(`elvis-control-ui`)**를 설치, 빌드 및 실행하는 과정에서 발생할 수 있는 주요 오류 상황과 그에 대한 원인 분석 및 해결 방법을 정리한 문서입니다.

---

## 📌 목차
1. [Rust & Tauri 프레임워크 관련 오류](#1-rust--tauri-프레임워크-관련-오류)
   - [ISSUE-01: `can't find library 'elvis_control_ui_lib'` (Cargo 매니페스트 파싱 실패)](#issue-01-cant-find-library-elvis_control_ui_lib-cargo-매니페스트-파싱-실패)
   - [ISSUE-02: `Found version mismatched Tauri packages` (NPM vs Cargo 버전 불일치)](#issue-02-found-version-mismatched-tauri-packages-npm-vs-cargo-버전-불일치)
   - [ISSUE-03: `link.exe not found` 또는 MSVC C++ 링크 오류](#issue-03-linkexe-not-found-또는-msvc-c-링크-오류)
   - [ISSUE-04: `rustc`, `cargo` 명령어를 찾을 수 없음 (CommandNotFoundException)](#issue-04-rustc-cargo-명령어를-찾을-수-없음-commandnotfoundexception)
   - [ISSUE-09: `icons/icon.ico not found` (Windows Resource 파일 생성 실패)](#issue-09-iconsiconico-not-found-windows-resource-파일-생성-실패)
2. [Node.js & Vite 웹 개발 서버 관련 오류](#2-nodejs--vite-웹-개발-서버-관련-오류)
   - [ISSUE-05: `TypeError: crypto$2.getRandomValues is not a function` (Node 16 호환성 오류)](#issue-05-typeerror-crypto2getrandomvalues-is-not-a-function-node-16-호환성-오류)
   - [ISSUE-06: 포트 충돌 (`Port 1420 is in use`)](#issue-06-포트-충돌-port-1420-is-in-use)
   - [ISSUE-08: `Error: EBUSY: resource busy or locked, watch ... src-tauri\target` (Vite 감시기 충돌)](#issue-08-error-ebusy-resource-busy-or-locked-watch--src-tauritarget-vite-감시기-충돌)
   - [ISSUE-10: Tailwind CSS 미설정으로 인한 관제 화면 레이아웃 깨짐 현상](#issue-10-tailwind-css-미설정으로-인한-관제-화면-레이아웃-깨짐-현상)
3. [윈도우 런타임 & 배포 관련 오류](#3-윈도우-런타임--배포-관련-오류)
   - [ISSUE-07: Microsoft Edge WebView2 런타임 누락 (앱 기동 시 백지 화면)](#issue-07-microsoft-edge-webview2-런타임-누락-앱-기동-시-백지-화면)

---

## 1. Rust & Tauri 프레임워크 관련 오류

---

### ISSUE-01: `can't find library 'elvis_control_ui_lib'` (Cargo 매니페스트 파싱 실패)

#### 🔴 증상 및 에러 메시지
```text
failed to run 'cargo metadata' command to get workspace directory: failed to run command cargo metadata:
error: failed to parse manifest at `D:\project\...\src-tauri\Cargo.toml`

Caused by:
  can't find library `elvis_control_ui_lib`, rename file to `src/lib.rs` or specify lib.path
```

#### 🔍 원인 분석
Tauri v2 프로젝트는 크로스 플랫폼(모바일/데스크톱) 호환을 위해 `Cargo.toml`에 `[lib]` 섹션(`name = "elvis_control_ui_lib"`)을 기본 선언합니다. 이때 라이브러리 진입점인 `src-tauri/src/lib.rs` 파일이 누락되어 있으면 Cargo가 프로젝트 구조를 파싱하지 못하고 중단됩니다.

#### ✅ 해결 방법
1. **`src-tauri/src/lib.rs` 파일 생성**:
   ```rust
   #[cfg_attr(mobile, tauri::mobile_entry_point)]
   pub fn run() {
       tauri::Builder::default()
           .plugin(tauri_plugin_shell::init())
           .run(tauri::generate_context!())
           .expect("error while running ELVIS-CSMS desktop application");
   }
   ```
2. **`src-tauri/src/main.rs` 수정**:
   ```rust
   #![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

   fn main() {
       elvis_control_ui_lib::run()
   }
   ```
3. **`src-tauri/build.rs` 빌드 스크립트 생성**:
   ```rust
   fn main() {
       tauri_build::build()
   }
   ```

---

### ISSUE-02: `Found version mismatched Tauri packages` (NPM vs Cargo 버전 불일치)

#### 🔴 증상 및 에러 메시지
```text
Error Found version mismatched Tauri packages. Make sure the NPM package and Rust crate versions are on the same major/minor releases:
tauri (v2.0.0) : @tauri-apps/api (v2.11.1)
tauri-plugin-shell (v2.0.0) : @tauri-apps/plugin-shell (v2.3.5)
```

#### 🔍 원인 분석
NPM 패키지(`package.json`)의 Tauri 관련 라이브러리는 `^2.x` 버전으로 최신 패치/마이너 버전을 참조하고 있으나, Rust의 `Cargo.toml`에서 `version = "2.0.0"`으로 엄격하게 고정되어 있어 버전 불일치 경고가 발생합니다.

#### ✅ 해결 방법
`src-tauri/Cargo.toml`의 의존성 버전을 `2`로 유연하게 지정하여 메이저 버전 내 최신 릴리스가 자동 매칭되도록 수정합니다.

```toml
[build-dependencies]
tauri-build = { version = "2", features = [] }

[dependencies]
tauri = { version = "2", features = [] }
tauri-plugin-shell = "2"
serde = { version = "1", features = ["derive"] }
serde_json = "1"
```

---

### ISSUE-03: `link.exe not found` 또는 MSVC C++ 링크 오류

#### 🔴 증상 및 에러 메시지
```text
error: linker `link.exe` not found
  |
  = note: the msvc target requires a C++ compiler and linker (link.exe)
```

#### 🔍 원인 분석
Windows에서 Rust 및 Tauri 바이너리를 링크(Link)하기 위해 필요한 **Visual Studio C++ 컴파일러/링커 도구**가 설치되어 있지 않거나 환경변수에 등록되지 않은 상태입니다.

#### ✅ 해결 방법
1. [Visual Studio Build Tools 다운로드 페이지](https://visualstudio.microsoft.com/visual-cpp-build-tools/)로 이동하여 설치 관리자 실행
2. 워크로드 목록에서 **`C++를 사용한 데스크톱 개발 (Desktop development with C++)`** 체크 후 설치
3. 설치 완료 후 터미널 및 VS Code 재시작

---

### ISSUE-04: `rustc`, `cargo` 명령어를 찾을 수 없음 (CommandNotFoundException)

#### 🔴 증상 및 에러 메시지
```text
cargo : 'cargo' 용어가 cmdlet, 함수, 스크립트 파일 또는 실행할 수 있는 프로그램 이름으로 인식되지 않습니다.
```

#### 🔍 원인 분석
`rustup` 설치 완료 직후, 현재 열려 있는 터미널 세션의 시스템 환경변수(`PATH`)에 Cargo 바이너리 경로(`~/.cargo/bin`)가 즉시 갱신되지 않아 발생합니다.

#### ✅ 해결 방법
1. 현재 열려 있는 모든 터미널 창과 VS Code를 완전히 종료한 후 다시 실행합니다.
2. 정상 반영 확인:
   ```powershell
   rustc --version
   cargo --version
   ```

---

### ISSUE-09: `icons/icon.ico not found` (Windows Resource 파일 생성 실패)

#### 🔴 증상 및 에러 메시지
```text
error: failed to run custom build command for `elvis-control-ui v1.0.0`
...
`icons/icon.ico` not found; required for generating a Windows Resource file during tauri-build
warning: build failed, waiting for other jobs to finish...
```

#### 🔍 원인 분석
Windows 플랫폼에서 Tauri 빌드 스크립트(`tauri-build`)가 실행 파일의 아이콘과 버전 정보를 담은 Windows Resource 파일(`.rc`)을 컴파일할 때, `src-tauri/icons/icon.ico` 파일이 누락되어 발생합니다.

#### ✅ 해결 방법
1. 원본 PNG 이미지를 준비합니다 (`ELVIS-LITE-border.png`).
2. Tauri CLI의 아이콘 일괄 생성 명령어를 실행하여 Windows(`.ico`), macOS(`.icns`), Android/iOS 및 웹용 PNG 세트를 자동 생성합니다:
   ```powershell
   pnpm tauri icon ./ELVIS-LITE-border.png
   ```
3. `src-tauri/icons/` 내에 `icon.ico` 등 모든 크기별 아이콘이 생성된 후 `pnpm tauri:dev`를 재실행합니다.

---

## 2. Node.js & Vite 웹 개발 서버 관련 오류

---

### ISSUE-05: `TypeError: crypto$2.getRandomValues is not a function` (Node 16 호환성 오류)

#### 🔴 증상 및 에러 메시지
```text
error when starting dev server:
TypeError: crypto$2.getRandomValues is not a function
    at resolveConfig (file:///.../node_modules/vite/dist/node/chunks/dep-BK3b2jBa.js:66671:16)
    at async _createServer (...)
Error The "beforeDevCommand" terminated with a non-zero status code.
```

#### 🔍 원인 분석
시스템 기본 Node.js가 **`v16.x`** 버전으로 설정되어 있는 경우 발생합니다. 최신 **Vite 5**는 표준 Web Crypto API(`globalThis.crypto.getRandomValues`)를 필요로 하는데, Node 16에서는 이 기능이 기본 활성화되어 있지 않아 Vite 개발 서버 구동 중 즉시 크래시가 발생합니다.

#### ✅ 해결 방법

- **FNM(Fast Node Manager)을 통한 버전 전환 (권장)**:
  현재 환경에 설치된 Node 24 이상 버전으로 즉시 전환합니다.
  ```powershell
  # 현재 세션에서 Node 24 사용
  fnm use 24

  # 기본 버전 변경
  fnm default 24
  ```

- **프로젝트 `.node-version` 설정**:
  `elvis-control-ui` 디렉터리에 `.node-version` 파일을 생성하여 `24`를 기재하면 디렉터리 진입 시 자동으로 해당 버전을 사용합니다.
  ```powershell
  Set-Content -Path .node-version -Value "24"
  ```

---

### ISSUE-06: 포트 충돌 (`Port 1420 is in use`)

#### 🔴 증상 및 에러 메시지
```text
Port 1420 is in use, trying another one...
Tauri devUrl http://localhost:1420 does not match server port.
```

#### 🔍 원인 분석
이전에 실행했던 `vite` 프로세스가 정상 종료되지 않고 백그라운드에 남아있거나, 다른 프로세스가 1420 포트를 점유하고 있는 경우입니다.

#### ✅ 해결 방법
1. **1420 포트를 점유 중인 프로세스 조회 및 종료 (PowerShell)**:
   ```powershell
   Get-Process -Id (Get-NetTCPConnection -LocalPort 1420).OwningProcess | Stop-Process -Force
   ```
2. 프로세스 정리 후 `pnpm tauri:dev` (또는 `pnpm dev`) 재실행

---

### ISSUE-08: `Error: EBUSY: resource busy or locked, watch ... src-tauri\target` (Vite 감시기 충돌)

#### 🔴 증상 및 에러 메시지
```text
Error: EBUSY: resource busy or locked, watch 'D:\project\...\src-tauri\target\debug\build\...\build_script_build-xxx.exe'
    at FSWatcher.<computed> (node:internal/fs/watchers:323:19)
    at Object.watch (node:fs:2609:36)
Error The "beforeDevCommand" terminated with a non-zero status code.
```

#### 🔍 원인 분석
Vite 개발 서버의 파일 변경 감시기(Watcher)가 기본적으로 프로젝트 전체 하위 디렉토리를 감시합니다. 이때 Rust 컴파일러(Cargo)가 `src-tauri/target/` 폴더 내에 수많은 임시 `.exe` 및 빌드 아티팩트를 생성/수정/삭제하는 과정에서 Windows의 파일 잠금(EBUSY) 충돌이 발생하여 Vite가 중단됩니다.

#### ✅ 해결 방법
`vite.config.ts`의 `server.watch` 설정에 `src-tauri/**` 디렉토리를 감시 제외(`ignored`) 목록으로 추가합니다.

```typescript
// vite.config.ts
export default defineConfig({
  // ...
  server: {
    port: 1420,
    strictPort: true,
    host: '127.0.0.1',
    watch: {
      ignored: ['**/src-tauri/**']  // Rust 빌드 산출물 감시 제외
    },
    // ...
  }
})
```

---

### ISSUE-10: Tailwind CSS 미설정으로 인한 관제 화면 레이아웃 깨짐 현상

#### 🔴 증상 및 에러 메시지
브라우저(`http://localhost:1420`) 또는 데스크톱 창에서 화면이 열리지만, 사이드바/KPI 카드/그리드 등이 세로로 길게 늘어지거나 스타일이 전혀 적용되지 않고 텍스트 형태로 깨져서 렌더링됨.

#### 🔍 원인 분석
Vue 컴포넌트(`App.vue`, `DashboardView.vue` 등)에 `flex`, `grid`, `space-y-4`, `w-64`, `bg-slate-950` 등 Tailwind CSS 유틸리티 클래스가 사용되었으나, 프로젝트에 Tailwind CSS 관련 패키지(`tailwindcss`, `postcss`, `autoprefixer`) 및 설정 파일이 누락되어 브라우저에서 스타일이 빌드되지 않은 상태입니다.

#### ✅ 해결 방법
1. **필수 패키지 설치 (pnpm)**:
   ```powershell
   cd elvis-control-ui
   pnpm add -D tailwindcss postcss autoprefixer
   ```
2. **`tailwind.config.js` 및 `postcss.config.js` 생성**:
   - `tailwind.config.js`:
     ```javascript
     /** @type {import('tailwindcss').Config} */
     export default {
       content: [
         "./index.html",
         "./src/**/*.{vue,js,ts,jsx,tsx}",
       ],
       darkMode: 'class',
       theme: {
         extend: {},
       },
       plugins: [],
     }
     ```
   - `postcss.config.js`:
     ```javascript
     export default {
       plugins: {
         tailwindcss: {},
         autoprefixer: {},
       },
     }
     ```
3. **`src/styles/main.css` 상단에 지시어 추가**:
   ```css
   @tailwind base;
   @tailwind components;
   @tailwind utilities;
   ```
4. `pnpm dev` 또는 `pnpm tauri:dev` 재실행

---


## 3. 윈도우 런타임 & 배포 관련 오류

---

### ISSUE-07: Microsoft Edge WebView2 런타임 누락 (앱 기동 시 백지 화면)

#### 🔴 증상 및 에러 메시지
데스크톱 실행 파일(`.exe`) 실행 시 창은 뜨지만 내부 화면이 흰색(Blank)으로 유지되고 아무런 UI가 표시되지 않음.

#### 🔍 원인 분석
일부 Windows 10 초기 빌드 또는 폐쇄망/임베디드 Windows 환경에서 **Microsoft Edge WebView2 Evergreen Runtime**이 설치되어 있지 않은 경우 발생합니다.

#### ✅ 해결 방법
1. [Microsoft WebView2 공식 다운로드](https://developer.microsoft.com/en-us/microsoft-edge/webview2/)에서 **Evergreen Bootstrapper / Standalone Installer**를 다운로드하여 설치합니다.
2. 폐쇄망 배포 시에는 WebView2 Fixed Version 바이너리를 함께 패키징하여 배포합니다.
