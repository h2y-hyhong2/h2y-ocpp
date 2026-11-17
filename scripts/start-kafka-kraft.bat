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

rem [Windows 명령줄 길이 제한 방어] libs\* 108개 JAR 개별 나열 시 8191자 초과로 실패하므로 와일드카드 클래스패스로 보정
set "RUN_CLASS_BAT=%KAFKA_HOME%\bin\windows\kafka-run-class.bat"
if exist "%RUN_CLASS_BAT%" (
    find "for " "%RUN_CLASS_BAT%" | find "libs\*" > nul 2>&1
    if not errorlevel 1 (
        echo [INFO] kafka-run-class.bat 와일드카드 클래스패스 패치를 적용합니다...
        powershell -NoProfile -ExecutionPolicy Bypass -Command "$p = '%RUN_CLASS_BAT%'; $c = Get-Content -LiteralPath $p -Raw; $pct = [char]37; $b = $pct + 'BASE_DIR' + $pct; $ii = $pct + $pct + 'i'; $cr = [char]13; $lf = [char]10; $tab = [char]9; $q = [char]34; $old = 'for ' + $ii + ' in (' + $q + $b + '\libs\*' + $q + ') do (' + $cr + $lf + $tab + 'call :concat ' + $q + $ii + $q + $cr + $lf + ')'; $oldLf = 'for ' + $ii + ' in (' + $q + $b + '\libs\*' + $q + ') do (' + $lf + $tab + 'call :concat ' + $q + $ii + $q + $lf + ')'; $new = 'if exist ' + $q + $b + '\libs\*' + $q + ' (' + $cr + $lf + $tab + 'call :concat ' + $q + $b + '\libs\*' + $q + $cr + $lf + ')'; if ($c.Contains($old)) { Set-Content -LiteralPath $p -Value $c.Replace($old, $new) -NoNewline -Encoding Ascii } elseif ($c.Contains($oldLf)) { Set-Content -LiteralPath $p -Value $c.Replace($oldLf, $new) -NoNewline -Encoding Ascii }" > nul 2>&1
    )
)

rem [KRaft Windows 방어] 파일 잠금 및 ReadOnly 속성 해제 (AccessDeniedException 방지)
if exist "%DATA_DIR%\kafka" (
    attrib -r "%DATA_DIR%\kafka\*" /s > nul 2>&1
    powershell -NoProfile -Command "Get-ChildItem -Path '%DATA_DIR%\kafka' -Filter '*.deleted*' -Recurse -ErrorAction SilentlyContinue | ForEach-Object { $_.IsReadOnly = $false; Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue }" > nul 2>&1
)

echo [1/2] Checking KRaft storage format...
call "%KAFKA_HOME%\bin\windows\kafka-storage.bat" format -t %CLUSTER_ID% -c "%KRAFT_CONFIG%" --ignore-formatted
if errorlevel 1 (
    echo [ERROR] Kafka KRaft 스토리지 포맷 확인 중 오류가 발생했습니다.
    if not "%NO_PAUSE%"=="1" pause
    exit /b 1
)

echo [2/2] Starting Kafka Broker (Port: %KAFKA_PORT%)...
call "%KAFKA_HOME%\bin\windows\kafka-server-start.bat" "%KRAFT_CONFIG%"

if not "%NO_PAUSE%"=="1" pause