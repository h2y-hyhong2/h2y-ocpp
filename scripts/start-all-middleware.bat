@echo off
setlocal
chcp 65001 > nul
set SCRIPT_DIR=%~dp0

set MYSQL_PORT=13306
set KAFKA_PORT=19092

if exist "%SCRIPT_DIR%..\config\paths.env" (
    for /f "usebackq tokens=1,2 delims==" %%A in ("%SCRIPT_DIR%..\config\paths.env") do (
        if "%%A"=="ELVIS_MYSQL_PORT" set MYSQL_PORT=%%B
        if "%%A"=="ELVIS_KAFKA_PORT" set KAFKA_PORT=%%B
    )
)

echo ========================================================
echo  [ELVIS-CSMS] 로컬 미들웨어 콘솔 일괄 기동
echo  - Apache Kafka KRaft (Port: %KAFKA_PORT%)
echo  - MySQL 9.71 Server  (Port: %MYSQL_PORT%)
echo ========================================================

echo [1/2] Apache Kafka KRaft 콘솔 창을 실행합니다...
start "ELVIS - Apache Kafka KRaft (Port: %KAFKA_PORT%)" cmd /k "%SCRIPT_DIR%start-kafka-kraft.bat"

ping 127.0.0.1 -n 4 > nul

echo [2/2] MySQL 9.71 콘솔 창을 실행합니다...
start "ELVIS - MySQL 9.71 (Port: %MYSQL_PORT%)" cmd /k "%SCRIPT_DIR%start-mysql.bat"

echo ========================================================
echo  미들웨어 콘솔 윈도우가 화면에 정상적으로 실행되었습니다!
echo ========================================================

