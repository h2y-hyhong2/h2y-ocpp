@echo off
setlocal
chcp 65001 > nul
if "%NO_PAUSE%"=="" set NO_PAUSE=1

set SCRIPT_DIR=%~dp0
set BIN_DIR=%SCRIPT_DIR%..\binaries\clickhouse-v26.7.34.3-stable

set CH_HTTP_PORT=8123
set CH_TCP_PORT=9000

if exist "%SCRIPT_DIR%..\config\paths.env" (
    for /f "usebackq tokens=1,2 delims==" %%A in ("%SCRIPT_DIR%..\config\paths.env") do (
        if "%%A"=="ELVIS_CLICKHOUSE_HTTP_PORT" set CH_HTTP_PORT=%%B
        if "%%A"=="ELVIS_CLICKHOUSE_TCP_PORT" set CH_TCP_PORT=%%B
    )
)

if not exist "D:\elvis-lite\logs" mkdir "D:\elvis-lite\logs"

echo ========================================================
echo  [ELVIS-CSMS] ClickHouse v26.7 OLAP 서버 기동 (WSL2 Ubuntu-24.04)
echo  - HTTP 포트: %CH_HTTP_PORT% (JDBC / REST API)
echo  - TCP 포트:  %CH_TCP_PORT% (Client CLI)
echo  - 로그 파일: D:\elvis-lite\logs\clickhouse.log
echo ========================================================

title ELVIS - ClickHouse v26.7 Server (Port: %CH_HTTP_PORT%)
color 0E

echo [INFO] Starting ClickHouse Server in WSL2 Ubuntu-24.04...
echo [INFO] Press Ctrl+C to stop.
echo.

wsl.exe -d Ubuntu-24.04 -u root bash /mnt/d/project/lselink/ocpp-lite/git/h2y-ocpp/scripts/run-clickhouse.sh %CH_HTTP_PORT% %CH_TCP_PORT%

if not "%NO_PAUSE%"=="1" pause
