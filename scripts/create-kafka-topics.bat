@echo off
setlocal
chcp 65001 > nul

set "SCRIPT_DIR=%~dp0"
if not defined ELVIS_HOME (
    for %%I in ("%SCRIPT_DIR%..") do set "ELVIS_HOME=%%~fI"
)
if "%ELVIS_HOME:~-1%"=="\" set "ELVIS_HOME=%ELVIS_HOME:~0,-1%"

set "KAFKA_HOME=%ELVIS_HOME%\binaries\kafka_2.13-4.3.1"
set "KAFKA_PORT=19092"

if exist "%ELVIS_HOME%\config\paths.env" (
    for /f "usebackq tokens=1,* delims==" %%A in ("%ELVIS_HOME%\config\paths.env") do (
        if /I "%%A"=="ELVIS_KAFKA_PORT" set "KAFKA_PORT=%%B"
    )
)
set "BOOTSTRAP=localhost:%KAFKA_PORT%"

rem 1순위: 제품 메인 경로 내장 JDK 우선 탐색
if exist "%ELVIS_HOME%\binaries\jdk\bin\java.exe" (
    set "JAVA_HOME=%ELVIS_HOME%\binaries\jdk"
    set "PATH=%ELVIS_HOME%\binaries\jdk\bin;%PATH%"
) else (
    for /d %%D in ("%ELVIS_HOME%\binaries\jdk-*") do (
        if exist "%%~fD\bin\java.exe" (
            set "JAVA_HOME=%%~fD"
            set "PATH=%%~fD\bin;%PATH%"
        )
    )
)
if not defined JAVA_HOME (
    if exist "D:\project\lselink\ocpp-lite\jdk\jdk-25.0.4.1+1\bin\java.exe" (
        set "JAVA_HOME=D:\project\lselink\ocpp-lite\jdk\jdk-25.0.4.1+1"
        set "PATH=D:\project\lselink\ocpp-lite\jdk\jdk-25.0.4.1+1\bin;%PATH%"
    ) else if exist "D:\jdk\windows\jdk-25.0.4.1+1\bin\java.exe" (
        set "JAVA_HOME=D:\jdk\windows\jdk-25.0.4.1+1"
        set "PATH=D:\jdk\windows\jdk-25.0.4.1+1\bin;%PATH%"
    ) else if exist "D:\jdk\windows\jdk-21.0.12.1+1\bin\java.exe" (
        set "JAVA_HOME=D:\jdk\windows\jdk-21.0.12.1+1"
        set "PATH=D:\jdk\windows\jdk-21.0.12.1+1\bin;%PATH%"
    ) else if exist "D:\jdk\windows\jdk-17.0.20.1+1\bin\java.exe" (
        set "JAVA_HOME=D:\jdk\windows\jdk-17.0.20.1+1"
        set "PATH=D:\jdk\windows\jdk-17.0.20.1+1\bin;%PATH%"
    )
)

echo ========================================================
echo  [ELVIS-CSMS] Kafka 4대 핵심 토픽 생성 스크립트
echo  ELVIS_HOME: %ELVIS_HOME%
echo ========================================================

:: 1. ocpp-raw-events (Inbound, 8 Partitions)
echo [1/4] ocpp-raw-events 토픽 생성 (Partitions: 8)...
call "%KAFKA_HOME%\bin\windows\kafka-topics.bat" --create --if-not-exists --bootstrap-server %BOOTSTRAP% --topic ocpp-raw-events --partitions 8 --replication-factor 1

:: 2. ocpp-outbound-commands (Outbound, 4 Partitions)
echo [2/4] ocpp-outbound-commands 토픽 생성 (Partitions: 4)...
call "%KAFKA_HOME%\bin\windows\kafka-topics.bat" --create --if-not-exists --bootstrap-server %BOOTSTRAP% --topic ocpp-outbound-commands --partitions 4 --replication-factor 1

:: 3. ocpp-ui-notifications (UI Push, 4 Partitions)
echo [3/4] ocpp-ui-notifications 토픽 생성 (Partitions: 4)...
call "%KAFKA_HOME%\bin\windows\kafka-topics.bat" --create --if-not-exists --bootstrap-server %BOOTSTRAP% --topic ocpp-ui-notifications --partitions 4 --replication-factor 1

:: 4. ocpp-dlt (Dead Letter, 2 Partitions)
echo [4/4] ocpp-dlt 토픽 생성 (Partitions: 2)...
call "%KAFKA_HOME%\bin\windows\kafka-topics.bat" --create --if-not-exists --bootstrap-server %BOOTSTRAP% --topic ocpp-dlt --partitions 2 --replication-factor 1

echo ========================================================
echo  토픽 목록 확인:
call "%KAFKA_HOME%\bin\windows\kafka-topics.bat" --list --bootstrap-server %BOOTSTRAP%
echo ========================================================
if not "%NO_PAUSE%"=="1" pause
