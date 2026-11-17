@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"

rem 1순위: 워크스페이스 전용 Gradle 9.9.1 및 JDK 25 우선 지정 (Spring Boot 4.1.2)
if not defined GRADLE_USER_HOME if exist "D:\project\lselink\ocpp-lite\gradle" (
    set "GRADLE_USER_HOME=D:\project\lselink\ocpp-lite\gradle\.gradle"
)
set "GRADLE_CMD="
if exist "D:\project\lselink\ocpp-lite\gradle\gradle-9.9.1\bin\gradle.bat" (
    set "GRADLE_HOME=D:\project\lselink\ocpp-lite\gradle\gradle-9.9.1"
    set "PATH=D:\project\lselink\ocpp-lite\gradle\gradle-9.9.1\bin;%PATH%"
    set "GRADLE_CMD=D:\project\lselink\ocpp-lite\gradle\gradle-9.9.1\bin\gradle.bat"
) else (
    for /d %%D in ("%PROJECT_ROOT%\binaries\gradle-*") do (
        if exist "%%~fD\bin\gradle.bat" (
            set "GRADLE_HOME=%%~fD"
            set "PATH=%%~fD\bin;%PATH%"
            set "GRADLE_CMD=%%~fD\bin\gradle.bat"
        )
    )
)
if not defined GRADLE_CMD (
    set "GRADLE_CMD=%PROJECT_ROOT%\gradlew.bat"
)

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

set WS_PORT=9080
if exist "%PROJECT_ROOT%\config\paths.env" (
    for /f "usebackq tokens=1,2 delims==" %%A in ("%PROJECT_ROOT%\config\paths.env") do (
        if "%%A"=="ELVIS_WS_PORT" set WS_PORT=%%B
    )
)

echo ========================================================
echo  [ELVIS-CONNECT] WebSocket 게이트웨이 기동 (Port: %WS_PORT%)
echo  PROJECT_ROOT: %PROJECT_ROOT%
echo ========================================================

title ELVIS - Connect WS Gateway (Port: %WS_PORT%)
color 0A

cd /d "%PROJECT_ROOT%"
call "%GRADLE_CMD%" :elvis-connect-ws:bootRun --args="--server.port=%WS_PORT%"

if not "%NO_PAUSE%"=="1" pause
