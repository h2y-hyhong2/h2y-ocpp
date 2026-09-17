@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0

echo ========================================================
echo  [ELVIS-TESTER] 백엔드 & UI 일괄 원클릭 기동
echo ========================================================

echo [1/2] elvis-tester 백엔드 (포트: 8085) 새 콘솔 창에서 기동...
start "ELVIS-TESTER Backend (Port: 8085)" cmd /k "call "%SCRIPT_DIR%start-elvis-tester.bat""

echo [INFO] 백엔드 초기화 대기 중 (5초)...
timeout /t 5 /nobreak > nul

echo [2/2] elvis-tester-ui 프론트엔드 (포트: 1421) 새 콘솔 창에서 기동...
start "ELVIS-TESTER UI (Port: 1421)" cmd /k "call "%SCRIPT_DIR%start-elvis-tester-ui.bat""

timeout /t 3 /nobreak > nul

echo [INFO] 웹 브라우저에서 테스터 콘솔 열기...
start http://localhost:1421

echo ========================================================
echo  백엔드(:8085)와 프론트엔드(:1421)가 모두 실행되었습니다!
echo  브라우저(http://localhost:1421)에서 테스트를 시작하세요.
echo ========================================================
