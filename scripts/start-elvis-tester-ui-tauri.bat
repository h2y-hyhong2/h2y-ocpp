@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0
set UI_ROOT=%SCRIPT_DIR%..\elvis-tester-ui

echo ========================================================
echo  [ELVIS-TESTER-UI] Tauri 데스크톱 네이티브 앱 실행 모드
echo ========================================================

cd /d "%UI_ROOT%"
if not exist "node_modules" (
    echo [INFO] 의존성 패키지 설치 진행 중... (pnpm install)
    call pnpm install
)

echo [INFO] Tauri 개발 모드 기동 (pnpm tauri:dev)...
call pnpm tauri:dev

pause
