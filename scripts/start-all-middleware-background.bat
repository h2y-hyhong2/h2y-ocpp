@echo off
setlocal
chcp 65001 > nul
set SCRIPT_DIR=%~dp0

set MYSQL_PORT=13306
set KAFKA_PORT=19092
set CH_HTTP_PORT=8123

if exist "%SCRIPT_DIR%..\config\paths.env" (
    for /f "usebackq tokens=1,2 delims==" %%A in ("%SCRIPT_DIR%..\config\paths.env") do (
        if "%%A"=="ELVIS_MYSQL_PORT" set MYSQL_PORT=%%B
        if "%%A"=="ELVIS_KAFKA_PORT" set KAFKA_PORT=%%B
        if "%%A"=="ELVIS_CLICKHOUSE_HTTP_PORT" set CH_HTTP_PORT=%%B
    )
)

if not exist "D:\elvis-lite\logs" mkdir "D:\elvis-lite\logs"

echo ========================================================
echo  [ELVIS-CSMS] 인프라 미들웨어 3대 백그라운드 독립 기동
echo  - Apache Kafka KRaft (Port: %KAFKA_PORT%)
echo  - MySQL 9.71 Server  (Port: %MYSQL_PORT%)
echo  - ClickHouse v26.7   (Port: %CH_HTTP_PORT%)
echo  - 로그 경로: D:\elvis-lite\logs\
echo ========================================================

echo [1/3] Apache Kafka KRaft 백그라운드 기동 중...
powershell -NoProfile -Command "Start-Process cmd.exe -ArgumentList '/c', 'call \"%SCRIPT_DIR%start-kafka-kraft.bat\" >> \"D:\elvis-lite\logs\kafka.log\" 2>&1' -WorkingDirectory '%SCRIPT_DIR%' -WindowStyle Hidden"

ping 127.0.0.1 -n 3 > nul

echo [2/3] MySQL 9.71 백그라운드 기동 중...
powershell -NoProfile -Command "Start-Process cmd.exe -ArgumentList '/c', 'call \"%SCRIPT_DIR%start-mysql.bat\" >> \"D:\elvis-lite\logs\mysql.log\" 2>&1' -WorkingDirectory '%SCRIPT_DIR%' -WindowStyle Hidden"

ping 127.0.0.1 -n 3 > nul

echo [3/3] ClickHouse v26.7 백그라운드 기동 중...
powershell -NoProfile -Command "Start-Process cmd.exe -ArgumentList '/c', 'call \"%SCRIPT_DIR%start-clickhouse.bat\" >> \"D:\elvis-lite\logs\clickhouse.log\" 2>&1' -WorkingDirectory '%SCRIPT_DIR%' -WindowStyle Hidden"

echo ========================================================
echo  인프라 미들웨어 3종이 완전 독립 백그라운드 프로세스로 기동되었습니다.
echo  (런처 창이나 터미널이 종료되어도 백그라운드에서 계속 유지됩니다.)
echo ========================================================
