@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0
set MYSQL_HOME=%SCRIPT_DIR%..\binaries\mysql-9.7.1-winx64
set MYSQL_BIN=%MYSQL_HOME%\bin\mysql.exe

set MYSQL_PORT=13306
if exist "%SCRIPT_DIR%..\config\paths.env" (
    for /f "usebackq tokens=1,2 delims==" %%A in ("%SCRIPT_DIR%..\config\paths.env") do (
        if "%%A"=="ELVIS_MYSQL_PORT" set MYSQL_PORT=%%B
    )
)

echo ========================================================
echo  [ELVIS-CSMS] MySQL 9.71 스키마 및 초기 시드 데이터 적재 (Port: %MYSQL_PORT%)
echo ========================================================

if not exist "%MYSQL_BIN%" (
    echo [ERROR] MySQL 클라이언트 바이너리를 찾을 수 없습니다: %MYSQL_BIN%
    pause
    exit /b 1
)

echo [1/2] DDL 스키마 적재 (config/mysql/01_schema.sql)...
"%MYSQL_BIN%" -h 127.0.0.1 -P %MYSQL_PORT% -u root < "%SCRIPT_DIR%..\config\mysql\01_schema.sql"

if %ERRORLEVEL% equ 0 (
    echo [OK] 01_schema.sql 적용 완료!
) else (
    echo [ERROR] 스키마 적용 중 오류 발생 - MySQL 서버 %MYSQL_PORT% 포트 실행 상태를 확인하세요
    pause
    exit /b 1
)

echo [2/2] 초기 시드 데이터 적재 (config/mysql/02_seed_data.sql)...
"%MYSQL_BIN%" -h 127.0.0.1 -P %MYSQL_PORT% -u root < "%SCRIPT_DIR%..\config\mysql\02_seed_data.sql"

if %ERRORLEVEL% equ 0 (
    echo [OK] 02_seed_data.sql 적용 완료!
    echo ========================================================
    echo  elvis-lite 데이터베이스 스키마 및 시드 데이터 적재 성공!
    echo ========================================================
) else (
    echo [ERROR] 시드 데이터 적재 중 오류 발생
)

pause
exit /b 0
