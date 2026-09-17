@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..

echo ========================================================
echo  [ELVIS-TESTER] 백엔드 테스트 하네스 기동 (포트: 8085)
echo ========================================================

cd /d "%PROJECT_ROOT%"
call gradlew :elvis-tester:bootRun

pause
