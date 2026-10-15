@echo off
setlocal
chcp 65001 > nul

echo ========================================================
echo  [ELVIS-TESTER] 테스터 UI 프로세스 및 포트(1422) 정리
echo ========================================================

rem 1. 테스터 프론트엔드 UI (1422) 점유 프로세스 종료
powershell -Command "Get-NetTCPConnection -LocalPort 1422 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }"

rem 2. 관련 콘솔 창 종료
taskkill /f /fi "WINDOWTITLE eq ELVIS-TESTER*" 2>nul
taskkill /f /fi "WINDOWTITLE eq ELVIS - Tester Harness*" 2>nul

echo ========================================================
echo  [ELVIS-TESTER] 테스터 프로그램이 성공적으로 종료되었습니다.
echo ========================================================
timeout /t 2 > nul
