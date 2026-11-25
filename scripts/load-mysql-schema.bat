@echo off
setlocal
chcp 65001 > nul

set "SCRIPT_DIR=%~dp0"
if not defined ELVIS_HOME (
    for %%I in ("%SCRIPT_DIR%..") do set "ELVIS_HOME=%%~fI"
)
if "%ELVIS_HOME:~-1%"=="\" set "ELVIS_HOME=%ELVIS_HOME:~0,-1%"

set "MYSQL_HOME=%ELVIS_HOME%\binaries\mysql-9.7.1-winx64"
set "MYSQL_BIN=%MYSQL_HOME%\bin\mysql.exe"
set "MYSQL_PORT=13306"

if exist "%ELVIS_HOME%\config\paths.env" (
    for /f "usebackq tokens=1,* delims==" %%A in ("%ELVIS_HOME%\config\paths.env") do (
        if /I "%%A"=="ELVIS_MYSQL_PORT" set "MYSQL_PORT=%%B"
    )
)

echo ========================================================
echo  [ELVIS-CSMS] MySQL 9.71 elvis_lite 스키마 및 초기 시드 데이터 적재 (Port: %MYSQL_PORT%)
echo  ELVIS_HOME: %ELVIS_HOME%
echo ========================================================

if not exist "%MYSQL_BIN%" (
    echo [ERROR] MySQL 클라이언트 바이너리를 찾을 수 없습니다: %MYSQL_BIN%
    if not "%NO_PAUSE%"=="1" pause
    exit /b 1
)

echo [1/2] DDL 스키마 적재 (config/mysql/01_schema.sql)...
"%MYSQL_BIN%" -h 127.0.0.1 -P %MYSQL_PORT% -u root --default-character-set=utf8mb4 < "%ELVIS_HOME%\config\mysql\01_schema.sql"

if %ERRORLEVEL% equ 0 (
    echo [OK] 01_schema.sql 적용 완료!
) else (
    echo [ERROR] 스키마 적용 중 오류 발생 - MySQL 서버 %MYSQL_PORT% 포트 실행 상태를 확인하세요
    if not "%NO_PAUSE%"=="1" pause
    exit /b 1
)

echo [2/2] 초기 시드 데이터 적재 (config/mysql/02_seed_data.sql)...
"%MYSQL_BIN%" -h 127.0.0.1 -P %MYSQL_PORT% -u root --default-character-set=utf8mb4 < "%ELVIS_HOME%\config\mysql\02_seed_data.sql"

if %ERRORLEVEL% equ 0 (
    echo [OK] 02_seed_data.sql 적용 완료!
    echo ========================================================
    echo  elvis_lite 데이터베이스 스키마 및 시드 데이터 적재 성공!
    echo ========================================================
) else (
    echo [ERROR] 시드 데이터 적재 중 오류 발생
)

if not "%NO_PAUSE%"=="1" pause
exit /b 0
