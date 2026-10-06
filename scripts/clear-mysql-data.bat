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
echo  [ELVIS-CSMS] MySQL 9.71 elvis-lite DB 데이터 전체 클리어 (Port: %MYSQL_PORT%)
echo ========================================================

if not exist "%MYSQL_BIN%" (
    echo [ERROR] MySQL 클라이언트 바이너리를 찾을 수 없습니다: %MYSQL_BIN%
    pause
    exit /b 1
)

echo 5대 테이블 데이터를 모두 삭제합니다 (TRUNCATE)...
"%MYSQL_BIN%" -h 127.0.0.1 -P %MYSQL_PORT% -u elvis -pelvis1234! --default-character-set=utf8mb4 elvis-lite -e "SET FOREIGN_KEY_CHECKS = 0; TRUNCATE TABLE tb_transaction_cdr; TRUNCATE TABLE tb_connector_status; TRUNCATE TABLE tb_charger; TRUNCATE TABLE tb_station; TRUNCATE TABLE tb_corp; SET FOREIGN_KEY_CHECKS = 1;"

if %ERRORLEVEL% equ 0 (
    echo [OK] elvis-lite 데이터 전체 클리어 완료 (초기 0건 상태)
    echo ========================================================
) else (
    echo [ERROR] 데이터 클리어 중 오류 발생
)

exit /b 0
