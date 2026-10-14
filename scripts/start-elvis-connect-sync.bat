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

set SYNC_PORT=8082
if exist "%PROJECT_ROOT%\config\paths.env" (
    for /f "usebackq tokens=1,2 delims==" %%A in ("%PROJECT_ROOT%\config\paths.env") do (
        if "%%A"=="ELVIS_SYNC_PORT" set SYNC_PORT=%%B
    )
)

echo ========================================================
echo  [ELVIS-CONNECT] Sync 이벤트 컨슈머 및 DB 동기화 엔진 기동 (Port: %SYNC_PORT%)
echo  PROJECT_ROOT: %PROJECT_ROOT%
echo ========================================================

title ELVIS - Connect Sync Engine (Port: %SYNC_PORT%)
color 0B

cd /d "%PROJECT_ROOT%"
call "%PROJECT_ROOT%\gradlew.bat" :elvis-connect-sync:bootRun --args="--server.port=%SYNC_PORT%"

if not "%NO_PAUSE%"=="1" pause
