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
) else if exist "D:\jdk\windows\jdk-25.0.4.1+1" (
    set "JAVA_HOME=D:\jdk\windows\jdk-25.0.4.1+1"
    set "PATH=D:\jdk\windows\jdk-25.0.4.1+1\bin;%PATH%"
) else if exist "D:\jdk\windows\jdk-21.0.12.1+1" (
    set "JAVA_HOME=D:\jdk\windows\jdk-21.0.12.1+1"
    set "PATH=D:\jdk\windows\jdk-21.0.12.1+1\bin;%PATH%"
)

echo ========================================================
echo  [ELVIS-BACKEND] Gradle Continuous Auto-Compile Watcher
echo  - Java/YAML 저장 시 변경 모듈만 증분 컴파일합니다.
echo  - Spring Boot DevTools가 변경된 .class를 감지해 자동 재시작합니다.
echo  PROJECT_ROOT: %PROJECT_ROOT%
echo ========================================================

title ELVIS - Backend DevTools Auto-Compile Watcher

cd /d "%PROJECT_ROOT%"
call "%GRADLE_CMD%" :elvis-connect-ws:classes :elvis-connect-sync:classes :elvis-control-api:classes --continuous -x test
