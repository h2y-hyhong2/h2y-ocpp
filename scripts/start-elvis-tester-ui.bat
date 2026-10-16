@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0
set UI_ROOT=%SCRIPT_DIR%..\elvis-tester-ui

echo ========================================================
echo  [ELVIS-TESTER-UI] 프론트엔드 콘솔 기동 (http://localhost:1422)
echo ========================================================

rem 기존 잔류 1422 포트 점유 프로세스 자동 정리
powershell -Command "Get-NetTCPConnection -LocalPort 1422 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }"

cd /d "%UI_ROOT%"
if not exist "node_modules" (
    echo [INFO] 의존성 패키지 설치 진행 중... (npm install)
    call npm install
)

echo [INFO] Vite 개발 서버 기동 및 브라우저 오픈...
call npm run dev -- --open

pause
