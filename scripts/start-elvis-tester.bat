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

set TESTER_PORT=8085
if exist "%PROJECT_ROOT%\config\paths.env" (
    for /f "usebackq tokens=1,2 delims==" %%A in ("%PROJECT_ROOT%\config\paths.env") do (
        if "%%A"=="ELVIS_TESTER_PORT" set TESTER_PORT=%%B
    )
)

echo ========================================================
echo  [ELVIS-TESTER] 백엔드 테스트 하네스 기동 (포트: %TESTER_PORT%)
echo  PROJECT_ROOT: %PROJECT_ROOT%
echo ========================================================

title ELVIS - Tester Harness (Port: %TESTER_PORT%)
color 0D

cd /d "%PROJECT_ROOT%"
call "%PROJECT_ROOT%\gradlew.bat" :elvis-tester:bootRun --args="--server.port=%TESTER_PORT%"

if not "%NO_PAUSE%"=="1" pause
