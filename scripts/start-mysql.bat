@echo off
setlocal
chcp 65001 > nul
if "%NO_PAUSE%"=="" set NO_PAUSE=1

set SCRIPT_DIR=%~dp0
set MYSQL_HOME=%SCRIPT_DIR%..\binaries\mysql-9.7.1-winx64
set DATA_DIR=D:\elvis-lite\data\mysql
set LOG_DIR=D:\elvis-lite\logs

set MYSQL_PORT=13306

rem paths.env 설정 파일이 존재하면 동적 경로 및 포트 반영
if exist "%SCRIPT_DIR%..\config\paths.env" (
    for /f "usebackq tokens=1,2 delims==" %%A in ("%SCRIPT_DIR%..\config\paths.env") do (
        if "%%A"=="ELVIS_DATA_DIR" set DATA_DIR=%%B\mysql
        if "%%A"=="ELVIS_LOG_DIR" set LOG_DIR=%%B
        if "%%A"=="ELVIS_MYSQL_PORT" set MYSQL_PORT=%%B
    )
)

if "%DATA_DIR:~-1%"=="\" set "DATA_DIR=%DATA_DIR:~0,-1%"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo ========================================================
echo  [ELVIS-CSMS] MySQL 9.71 로컬 서버 기동 (Port: %MYSQL_PORT%)
echo  MYSQL_HOME: %MYSQL_HOME%
echo  DATA_DIR:   %DATA_DIR%
echo  PORT:       %MYSQL_PORT%
echo ========================================================

set "NEED_SCHEMA_INIT=0"
if not exist "%DATA_DIR%\mysql" (
    echo [WARN] 초기화된 data 디렉토리가 없습니다. init-mysql.bat을 먼저 실행합니다.
    call "%SCRIPT_DIR%init-mysql.bat"
    if errorlevel 1 (
        echo [ERROR] MySQL 데이터 디렉토리 초기화에 실패하여 서버를 기동할 수 없습니다.
        if not "%NO_PAUSE%"=="1" pause
        exit /b 1
    )
    set "NEED_SCHEMA_INIT=1"
)

if "%NEED_SCHEMA_INIT%"=="1" (
    echo [INFO] 최초 기동 감지 - 서버 준비 후 elvis_lite 스키마 및 시드 데이터를 자동 적재합니다.
    start /b "" powershell -NoProfile -ExecutionPolicy Bypass -Command "$mysqlExe = '%MYSQL_HOME%\bin\mysql.exe'; $schemaBat = '%SCRIPT_DIR%load-mysql-schema.bat'; $ready = $false; for ($i = 0; $i -lt 30; $i++) { Start-Sleep -Seconds 1; try { & $mysqlExe -h 127.0.0.1 -P %MYSQL_PORT% -u root -e 'SELECT 1' > $null 2>&1; if ($LASTEXITCODE -eq 0) { $ready = $true; break } } catch {} }; if ($ready) { $env:NO_PAUSE = '1'; & cmd.exe /c call $schemaBat } else { Write-Output '[WARN] MySQL 응답 대기 시간 초과로 자동 스키마 적재를 건너뛰었습니다.' }"
)

title ELVIS - MySQL 9.71 Server (Port: %MYSQL_PORT%)
color 0B

echo MySQL 서버를 콘솔 모드로 기동합니다...
set MYSQL_INI=%SCRIPT_DIR%..\config\mysql\my.ini
if not exist "%MYSQL_INI%" (
    set MYSQL_INI=%MYSQL_HOME%\my.ini
)

if exist "%MYSQL_INI%" (
    echo [INFO] 설정 파일 적용: %MYSQL_INI%
    "%MYSQL_HOME%\bin\mysqld.exe" --defaults-file="%MYSQL_INI%" --datadir="%DATA_DIR%" --port=%MYSQL_PORT% --console
) else (
    echo [WARN] my.ini 파일이 없어 기본 CLI 인자로 기동합니다.
    "%MYSQL_HOME%\bin\mysqld.exe" --basedir="%MYSQL_HOME%" --datadir="%DATA_DIR%" --port=%MYSQL_PORT% --console
)

if not "%NO_PAUSE%"=="1" pause
