@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0
set UI_ROOT=%SCRIPT_DIR%..\elvis-tester-ui

echo ========================================================
echo  [ELVIS-TESTER-UI] Tauri 데스크톱 네이티브 앱 실행 모드
echo ========================================================

rem 기존 잔류 1422 포트 점유 프로세스 자동 정리 (포트 충돌로 인한 창 미실행 방지)
powershell -Command "Get-NetTCPConnection -LocalPort 1422 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }"

cd /d "%UI_ROOT%"
if not exist "node_modules" (
    echo [INFO] 의존성 패키지 설치 진행 중... (pnpm install)
    call pnpm install
)

echo [INFO] Tauri 개발 모드 기동 (pnpm tauri:dev)...
call pnpm tauri:dev

pause
