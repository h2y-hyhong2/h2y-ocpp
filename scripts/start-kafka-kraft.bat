@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0
set KAFKA_HOME=%SCRIPT_DIR%..\binaries\kafka_2.13-4.3.1
set KRAFT_CONFIG=%SCRIPT_DIR%..\config\kafka\server.properties

if exist "d:\project\lselink\ocpp\jdk\jdk-25.0.2+10" (
    set "JAVA_HOME=d:\project\lselink\ocpp\jdk\jdk-25.0.2+10"
    set "PATH=d:\project\lselink\ocpp\jdk\jdk-25.0.2+10\bin;%PATH%"
)
set "KAFKA_HEAP_OPTS=-Xmx1G -Xms512M"

if not exist "%KRAFT_CONFIG%" (
    set KRAFT_CONFIG=%KAFKA_HOME%\config\server.properties
)
set CLUSTER_ID=4L622nShTWWkrFuMm50BAg

echo ========================================================
echo  [ELVIS-CSMS] Apache Kafka 4.3.1 KRaft Local Server
echo  KAFKA_HOME:   %KAFKA_HOME%
echo  CONFIG_FILE:  %KRAFT_CONFIG%
echo ========================================================

if not exist "%KAFKA_HOME%\bin\windows\kafka-server-start.bat" (
    echo [ERROR] Cannot find Kafka binary: %KAFKA_HOME%
    if not "%NO_PAUSE%"=="1" pause
    exit /b 1
)

title ELVIS - Apache Kafka 4.3.1 KRaft (Port: 19092)
color 0E

rem [KRaft Windows 방어] 파일 잠금 및 ReadOnly 속성 해제 (AccessDeniedException 방지)
if exist "D:\elvis-lite\data\kafka" (
    attrib -r "D:\elvis-lite\data\kafka\*" /s > nul 2>&1
    powershell -NoProfile -Command "Get-ChildItem -Path 'D:\elvis-lite\data\kafka' -Filter '*.deleted*' -Recurse -ErrorAction SilentlyContinue | ForEach-Object { $_.IsReadOnly = $false; Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue }" > nul 2>&1
)

echo [1/2] Checking KRaft storage format...
call "%KAFKA_HOME%\bin\windows\kafka-storage.bat" format -t %CLUSTER_ID% -c "%KRAFT_CONFIG%" --ignore-formatted

echo [2/2] Starting Kafka Broker (Port: 19092)...
call "%KAFKA_HOME%\bin\windows\kafka-server-start.bat" "%KRAFT_CONFIG%"

if not "%NO_PAUSE%"=="1" pause