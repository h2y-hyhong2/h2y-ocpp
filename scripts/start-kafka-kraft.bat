@echo off
setlocal
chcp 65001 > nul
if "%NO_PAUSE%"=="" set NO_PAUSE=1

set "SCRIPT_DIR=%~dp0"
if not defined ELVIS_HOME (
    for %%I in ("%SCRIPT_DIR%..") do set "ELVIS_HOME=%%~fI"
)
if "%ELVIS_HOME:~-1%"=="\" set "ELVIS_HOME=%ELVIS_HOME:~0,-1%"

set "KAFKA_HOME=%ELVIS_HOME%\binaries\kafka_2.13-4.3.1"
set "KRAFT_CONFIG=%ELVIS_HOME%\config\kafka\server.properties"
set "DATA_DIR=%ELVIS_HOME%\data"
set "KAFKA_PORT=19092"

if exist "%ELVIS_HOME%\config\paths.env" (
    for /f "usebackq tokens=1,* delims==" %%A in ("%ELVIS_HOME%\config\paths.env") do (
        if /I "%%A"=="ELVIS_DATA_DIR" set "DATA_DIR=%%B"
        if /I "%%A"=="ELVIS_KAFKA_PORT" set "KAFKA_PORT=%%B"
    )
)
call set "DATA_DIR=%DATA_DIR%"
if "%DATA_DIR:~-1%"=="\" set "DATA_DIR=%DATA_DIR:~0,-1%"

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
set "KAFKA_HEAP_OPTS=-Xmx1G -Xms512M"

if not exist "%KRAFT_CONFIG%" (
    set "KRAFT_CONFIG=%KAFKA_HOME%\config\server.properties"
)
set "CLUSTER_ID=4L622nShTWWkrFuMm50BAg"

echo ========================================================
echo  [ELVIS-CSMS] Apache Kafka 4.3.1 KRaft Local Server
echo  ELVIS_HOME:   %ELVIS_HOME%
echo  KAFKA_HOME:   %KAFKA_HOME%
echo  DATA_DIR:     %DATA_DIR%\kafka
echo  CONFIG_FILE:  %KRAFT_CONFIG%
echo  PORT:         %KAFKA_PORT%
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

rem [동적 경로/포트 반영] DATA_DIR 및 KAFKA_PORT를 런타임 properties에 반영해 설치 경로 이동 시에도 즉시 동작
if not exist "%DATA_DIR%" mkdir "%DATA_DIR%"
set "RUNTIME_CONFIG=%DATA_DIR%\kafka-server.runtime.properties"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$src = '%KRAFT_CONFIG%'; $dst = '%RUNTIME_CONFIG%'; $logDir = ('%DATA_DIR%\kafka').Replace('\', '/'); $port = '%KAFKA_PORT%'; $lines = [System.IO.File]::ReadAllLines($src, [System.Text.Encoding]::UTF8); $out = [string[]]@($lines | ForEach-Object { if ($_ -match '^\s*log\.dirs=') { 'log.dirs=' + $logDir } elseif ($_ -match '^\s*listeners=PLAINTEXT://:\d+') { $_ -replace 'PLAINTEXT://:\d+', ('PLAINTEXT://:' + $port) } elseif ($_ -match '^\s*advertised\.listeners=PLAINTEXT://(localhost|127\.0\.0\.1):\d+') { $_ -replace 'PLAINTEXT://(localhost|127\.0\.0\.1):\d+', ('PLAINTEXT://$1:' + $port) } else { $_ } }); [System.IO.File]::WriteAllLines($dst, $out, [System.Text.UTF8Encoding]::new($false))" > nul 2>&1
if exist "%RUNTIME_CONFIG%" set "KRAFT_CONFIG=%RUNTIME_CONFIG%"

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
