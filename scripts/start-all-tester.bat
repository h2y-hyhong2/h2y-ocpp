@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0

echo ========================================================
echo  [ELVIS-TESTER] 가상 충전기 테스터 UI 단일 기동 (포트: 1422)
echo ========================================================

echo [1/1] elvis-tester-ui (포트: 1422) 새 콘솔 창에서 기동...
start "ELVIS-TESTER UI (Port: 1422)" cmd /k "call "%SCRIPT_DIR%start-elvis-tester-ui.bat""

timeout /t 2 /nobreak > nul

echo [INFO] 웹 브라우저에서 테스터 콘솔 열기...
start http://localhost:1422

echo ========================================================
echo  가상 충전기 테스터 UI(:1422)가 실행되었습니다!
echo  브라우저(http://localhost:1422)에서 OCPP 시뮬레이션을 시작하세요.
echo ========================================================
