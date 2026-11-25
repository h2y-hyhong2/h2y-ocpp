@echo off
setlocal
chcp 65001 > nul
set "SCRIPT_DIR=%~dp0"
if not defined ELVIS_HOME (
    for %%I in ("%SCRIPT_DIR%..") do set "ELVIS_HOME=%%~fI"
)
if "%ELVIS_HOME:~-1%"=="\" set "ELVIS_HOME=%ELVIS_HOME:~0,-1%"

set "MYSQL_PORT=13306"
set "KAFKA_PORT=19092"
set "LOG_DIR=%ELVIS_HOME%\logs"

if exist "%ELVIS_HOME%\config\paths.env" (
    for /f "usebackq tokens=1,* delims==" %%A in ("%ELVIS_HOME%\config\paths.env") do (
        if /I "%%A"=="ELVIS_MYSQL_PORT" set "MYSQL_PORT=%%B"
        if /I "%%A"=="ELVIS_KAFKA_PORT" set "KAFKA_PORT=%%B"
        if /I "%%A"=="ELVIS_LOG_DIR" set "LOG_DIR=%%B"
    )
)

call set "LOG_DIR=%LOG_DIR%"
if "%LOG_DIR:~-1%"=="\" set "LOG_DIR=%LOG_DIR:~0,-1%"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo ========================================================
echo  [ELVIS-CSMS] 인프라 미들웨어 2대 백그라운드 독립 기동
echo  - ELVIS_HOME: %ELVIS_HOME%
echo  - Apache Kafka KRaft (Port: %KAFKA_PORT%)
echo  - MySQL 9.71 Server  (Port: %MYSQL_PORT%)
echo  - 로그 경로: %LOG_DIR%\
echo ========================================================

echo [1/2] Apache Kafka KRaft 백그라운드 기동 중...
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%run-daemon.ps1" -BatName "start-kafka-kraft.bat" -ServiceName "kafka" -ElvisHome "%ELVIS_HOME%"

ping 127.0.0.1 -n 3 > nul

echo [2/2] MySQL 9.71 백그라운드 기동 중...
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%run-daemon.ps1" -BatName "start-mysql.bat" -ServiceName "mysql" -ElvisHome "%ELVIS_HOME%"

echo ========================================================
echo  인프라 미들웨어 2종이 완전 독립 백그라운드 프로세스로 기동되었습니다.
echo  (런처 창이나 터미널이 종료되어도 백그라운드에서 계속 유지됩니다.)
echo ========================================================
