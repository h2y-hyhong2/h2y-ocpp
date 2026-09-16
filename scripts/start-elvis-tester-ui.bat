@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0
set UI_ROOT=%SCRIPT_DIR%..\elvis-tester-ui

echo ========================================================
echo  [ELVIS-TESTER-UI] 프론트엔드 콘솔 기동 (http://localhost:1421)
echo ========================================================

cd /d "%UI_ROOT%"
if not exist "node_modules" (
    echo [INFO] 의존성 패키지 설치 진행 중... (npm install)
    call npm install
)

echo [INFO] Vite 개발 서버 기동...
call npm run dev

pause
