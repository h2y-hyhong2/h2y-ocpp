@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"

rem JDK 경로 탐색 및 환경변수 설정 (JDK 21 LTS 우선)
if exist "D:\jdk\windows\jdk-21.0.9+10" (
    set "JAVA_HOME=D:\jdk\windows\jdk-21.0.9+10"
    set "PATH=D:\jdk\windows\jdk-21.0.9+10\bin;%PATH%"
) else if exist "D:\project\lselink\elvis-v2\jdk\jdk-25.0.3+9" (
    set "JAVA_HOME=D:\project\lselink\elvis-v2\jdk\jdk-25.0.3+9"
    set "PATH=D:\project\lselink\elvis-v2\jdk\jdk-25.0.3+9\bin;%PATH%"
) else if exist "d:\project\lselink\ocpp\jdk\jdk-25.0.2+10" (
    set "JAVA_HOME=d:\project\lselink\ocpp\jdk\jdk-25.0.2+10"
    set "PATH=d:\project\lselink\ocpp\jdk\jdk-25.0.2+10\bin;%PATH%"
)

set API_PORT=18082
if exist "%PROJECT_ROOT%\config\paths.env" (
    for /f "usebackq tokens=1,2 delims==" %%A in ("%PROJECT_ROOT%\config\paths.env") do (
        if "%%A"=="ELVIS_API_PORT" set API_PORT=%%B
    )
)

echo ========================================================
echo  [ELVIS-CONTROL] 관제 REST API 서버 기동 (Port: %API_PORT%)
echo  PROJECT_ROOT: %PROJECT_ROOT%
echo ========================================================

title ELVIS - Control API Server (Port: %API_PORT%)
color 0C

cd /d "%PROJECT_ROOT%"
call "%PROJECT_ROOT%\gradlew.bat" :elvis-control-api:bootRun --args="--server.port=%API_PORT%"

if not "%NO_PAUSE%"=="1" pause
