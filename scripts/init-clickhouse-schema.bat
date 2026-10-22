@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0
set SCHEMA_FILE=/mnt/d/project/lselink/ocpp-lite/git/h2y-ocpp/elvis-data-clickhouse/src/main/resources/schema-clickhouse.sql

set CH_HTTP_PORT=8123

if exist "%SCRIPT_DIR%..\config\paths.env" (
    for /f "usebackq tokens=1,2 delims==" %%A in ("%SCRIPT_DIR%..\config\paths.env") do (
        if "%%A"=="ELVIS_CLICKHOUSE_HTTP_PORT" set CH_HTTP_PORT=%%B
    )
)

echo ========================================================
echo  [ELVIS-CSMS] ClickHouse 초기 DDL 스키마 적용 (elvis_poc)
echo ========================================================

echo [1/3] ClickHouse 서버(http://localhost:%CH_HTTP_PORT%) 헬스체크 중...
curl.exe -s http://localhost:%CH_HTTP_PORT%/ping > nul
if errorlevel 1 (
    echo.
    echo [ERROR] ClickHouse 서버(http://localhost:%CH_HTTP_PORT%)가 응답하지 않습니다.
    echo 먼저 scripts\start-clickhouse.bat 을 실행하여 ClickHouse 서버를 기동해주세요!
    echo.
    pause
    exit /b 1
)
echo [OK] ClickHouse 서버 응답 확인 완료!

echo [2/3] DDL 스키마를 적용합니다...
echo 대상 스크립트: %SCHEMA_FILE%
wsl.exe -d Ubuntu-24.04 bash -c "cd /mnt/d/project/lselink/ocpp-lite/git/h2y-ocpp/binaries/clickhouse-v26.7.34.3-stable && ./clickhouse client --multiquery < %SCHEMA_FILE%"

echo.
echo [3/3] 테이블 생성 결과 검증:
curl.exe -s "http://localhost:%CH_HTTP_PORT%/?query=SHOW+TABLES+FROM+elvis_poc"
echo.
echo ========================================================
echo  ClickHouse 초기 DDL 스키마 적용이 성공적으로 완료되었습니다!
echo ========================================================

pause
