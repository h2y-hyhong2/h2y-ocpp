@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0

echo ========================================================
echo  [ELVIS-TESTER] 가상 충전기 테스터 UI 단일 기동 (포트: 1422)
echo ========================================================

echo [1/1] elvis-tester-ui (포트: 1422) 새 콘솔 창에서 기동...
start "ELVIS-TESTER UI (Port: 1422)" cmd /k "call "%SCRIPT_DIR%start-elvis-tester-ui.bat""

echo ========================================================
echo  가상 충전기 테스터 UI(:1422)가 실행되었습니다!
echo  브라우저(http://127.0.0.1:1422)가 자동으로 열립니다.
echo ========================================================
