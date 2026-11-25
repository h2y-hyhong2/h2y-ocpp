@echo off
setlocal EnableDelayedExpansion
chcp 65001 > nul

set "SCRIPT_DIR=%~dp0"
if not defined ELVIS_HOME (
    for %%I in ("%SCRIPT_DIR%..") do set "ELVIS_HOME=%%~fI"
)
if "%ELVIS_HOME:~-1%"=="\" set "ELVIS_HOME=%ELVIS_HOME:~0,-1%"
set "PROJECT_ROOT=%ELVIS_HOME%"

rem 1순위: 제품 메인 경로 내장 JDK 우선 탐색 (binaries\jdk 또는 binaries\jdk-*)
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
    ) else if exist "d:\project\lselink\ocpp\jdk\jdk-17.0.20.1+1\bin\java.exe" (
        set "JAVA_HOME=d:\project\lselink\ocpp\jdk\jdk-17.0.20.1+1"
        set "PATH=d:\project\lselink\ocpp\jdk\jdk-17.0.20.1+1\bin;%PATH%"
    )
)

set "API_PORT=9082"
set "MYSQL_PORT=13306"
set "KAFKA_PORT=19092"
set "LOG_DIR=%ELVIS_HOME%\logs"
if exist "%ELVIS_HOME%\config\paths.env" (
    for /f "usebackq tokens=1,* delims==" %%A in ("%ELVIS_HOME%\config\paths.env") do (
        if /I "%%A"=="ELVIS_API_PORT" set "API_PORT=%%B"
        if /I "%%A"=="ELVIS_MYSQL_PORT" set "MYSQL_PORT=%%B"
        if /I "%%A"=="ELVIS_KAFKA_PORT" set "KAFKA_PORT=%%B"
        if /I "%%A"=="ELVIS_LOG_DIR" set "LOG_DIR=%%B"
    )
)
set "LOG_DIR=!LOG_DIR:%%ELVIS_HOME%%=%ELVIS_HOME%!"
set "ELVIS_LOG_DIR=!LOG_DIR:\=/!"
set "ELVIS_API_PORT=%API_PORT%"
set "KAFKA_BOOTSTRAP_SERVERS=localhost:%KAFKA_PORT%"

rem 1순위: 인스톨 배포 JAR 탐색 (apps\elvis-control-api.jar 또는 apps\elvis-control-api-*.jar, -plain.jar 제외)
set "APP_JAR="
if exist "%ELVIS_HOME%\apps\elvis-control-api.jar" (
    set "APP_JAR=%ELVIS_HOME%\apps\elvis-control-api.jar"
) else (
    for %%F in ("%ELVIS_HOME%\apps\elvis-control-api-*.jar") do (
        set "CAND=%%~nxF"
        if /I "!CAND:-plain.jar=!"=="!CAND!" (
            if not defined APP_JAR set "APP_JAR=%%~fF"
        )
    )
)

title ELVIS - Control API Server (Port: %API_PORT%)
color 0C

if defined APP_JAR (
    set "EXT_CONFIG=%ELVIS_HOME%\config\control-api\application.yml"
    echo ========================================================
    echo  [ELVIS-CONTROL] 관제 REST API 서버 기동 [JAR] (Port: %API_PORT%)
    echo  ELVIS_HOME: %ELVIS_HOME%
    echo  APP_JAR:    %APP_JAR%
    echo ========================================================
    cd /d "%ELVIS_HOME%"
    if exist "%EXT_CONFIG%" (
        java -jar "%APP_JAR%" --server.port=%API_PORT% "--spring.config.additional-location=optional:file:%EXT_CONFIG%"
    ) else (
        java -jar "%APP_JAR%" --server.port=%API_PORT%
    )
    if not "%NO_PAUSE%"=="1" pause
    exit /b %ERRORLEVEL%
)

rem 2순위: 개발 워크스페이스 직접 실행 시 Gradle bootRun 폴백
if not exist "%PROJECT_ROOT%\build.gradle" (
    echo [ERROR] elvis-control-api 실행 JAR가 설치되어 있지 않습니다: %ELVIS_HOME%\apps\elvis-control-api.jar
    if not "%NO_PAUSE%"=="1" pause
    exit /b 1
)

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
if not defined GRADLE_CMD set "GRADLE_CMD=%PROJECT_ROOT%\gradlew.bat"

echo ========================================================
echo  [ELVIS-CONTROL] 관제 REST API 서버 기동 [Gradle] (Port: %API_PORT%)
echo  PROJECT_ROOT: %PROJECT_ROOT%
echo ========================================================

cd /d "%PROJECT_ROOT%"
call "%GRADLE_CMD%" :elvis-control-api:bootRun --args="--server.port=%API_PORT%"

if not "%NO_PAUSE%"=="1" pause
