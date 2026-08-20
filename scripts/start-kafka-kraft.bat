@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0
set KAFKA_HOME=%SCRIPT_DIR%..\binaries\kafka_2.13-4.3.1
set KRAFT_CONFIG=%SCRIPT_DIR%..\config\kafka\server.properties
if not exist "%KRAFT_CONFIG%" (
    set KRAFT_CONFIG=%KAFKA_HOME%\config\server.properties
)
set CLUSTER_ID=4L622nShTWWkrFuMm50BAg

echo ========================================================
echo  [ELVIS-CSMS] Kafka KRaft 모드 로컬 기동 스크립트
echo  KAFKA_HOME:   %KAFKA_HOME%
echo  CONFIG_FILE:  %KRAFT_CONFIG%
echo ========================================================

if not exist "%KAFKA_HOME%\bin\windows\kafka-server-start.bat" (
    echo [ERROR] Kafka 바이너리를 찾을 수 없습니다: %KAFKA_HOME%
    pause
    exit /b 1
)

:: KRaft 로그 디렉토리 포맷 (최초 1회 실행)
echo [1/2] KRaft 스토리지 디렉토리 점검 및 포맷...
call "%KAFKA_HOME%\bin\windows\kafka-storage.bat" format -t %CLUSTER_ID% -c "%KRAFT_CONFIG%" --ignore-formatted

echo [2/2] Kafka 브로커 기동 (Port: 9092)...
call "%KAFKA_HOME%\bin\windows\kafka-server-start.bat" "%KRAFT_CONFIG%"

pause
