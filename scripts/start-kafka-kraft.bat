@echo off
setlocal
chcp 65001 > nul
if "%NO_PAUSE%"=="" set NO_PAUSE=1

set SCRIPT_DIR=%~dp0
set KAFKA_HOME=%SCRIPT_DIR%..\binaries\kafka_2.13-4.3.1
set KRAFT_CONFIG=%SCRIPT_DIR%..\config\kafka\server.properties
set DATA_DIR=D:\elvis-lite\data
set KAFKA_PORT=19092

if exist "%SCRIPT_DIR%..\config\paths.env" (
    for /f "usebackq tokens=1,2 delims==" %%A in ("%SCRIPT_DIR%..\config\paths.env") do (
        if "%%A"=="ELVIS_DATA_DIR" set DATA_DIR=%%B
        if "%%A"=="ELVIS_KAFKA_PORT" set KAFKA_PORT=%%B
    )
)

rem 1순위: JDK 25 LTS 우선 강제 지정
if exist "D:\project\lselink\ocpp-lite\jdk\jdk-25.0.4.1+1" (
    set "JAVA_HOME=D:\project\lselink\ocpp-lite\jdk\jdk-25.0.4.1+1"
    set "PATH=D:\project\lselink\ocpp-lite\jdk\jdk-25.0.4.1+1\bin;%PATH%"
) else if exist "D:\jdk\windows\jdk-21.0.12.1+1" (
    set "JAVA_HOME=D:\jdk\windows\jdk-21.0.12.1+1"
    set "PATH=D:\jdk\windows\jdk-21.0.12.1+1\bin;%PATH%"
) else if exist "D:\jdk\windows\jdk-25.0.4.1+1" (
    set "JAVA_HOME=D:\jdk\windows\jdk-25.0.4.1+1"
    set "PATH=D:\jdk\windows\jdk-25.0.4.1+1\bin;%PATH%"
) else if exist "D:\jdk\windows\jdk-17.0.20.1+1" (
    set "JAVA_HOME=D:\jdk\windows\jdk-17.0.20.1+1"
    set "PATH=D:\jdk\windows\jdk-17.0.20.1+1\bin;%PATH%"
) else if exist "d:\project\lselink\ocpp\jdk\jdk-17.0.20.1+1" (
    set "JAVA_HOME=d:\project\lselink\ocpp\jdk\jdk-17.0.20.1+1"
    set "PATH=d:\project\lselink\ocpp\jdk\jdk-17.0.20.1+1\bin;%PATH%"
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

title ELVIS - Apache Kafka 4.3.1 KRaft (Port: %KAFKA_PORT%)
color 0E

rem [KRaft Windows 방어] 파일 잠금 및 ReadOnly 속성 해제 (AccessDeniedException 방지)
if exist "%DATA_DIR%\kafka" (
    attrib -r "%DATA_DIR%\kafka\*" /s > nul 2>&1
    powershell -NoProfile -Command "Get-ChildItem -Path '%DATA_DIR%\kafka' -Filter '*.deleted*' -Recurse -ErrorAction SilentlyContinue | ForEach-Object { $_.IsReadOnly = $false; Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue }" > nul 2>&1
)

echo [1/2] Checking KRaft storage format...
call "%KAFKA_HOME%\bin\windows\kafka-storage.bat" format -t %CLUSTER_ID% -c "%KRAFT_CONFIG%" --ignore-formatted

echo [2/2] Starting Kafka Broker (Port: %KAFKA_PORT%)...
call "%KAFKA_HOME%\bin\windows\kafka-server-start.bat" "%KRAFT_CONFIG%"

if not "%NO_PAUSE%"=="1" pause