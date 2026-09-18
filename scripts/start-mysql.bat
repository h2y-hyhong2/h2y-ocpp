@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0
set MYSQL_HOME=%SCRIPT_DIR%..\binaries\mysql-9.7.1-winx64
set DATA_DIR=D:\elvis-lite\data\mysql

set MYSQL_PORT=13306

rem paths.env 설정 파일이 존재하면 동적 경로 및 포트 반영
if exist "%SCRIPT_DIR%..\config\paths.env" (
    for /f "usebackq tokens=1,2 delims==" %%A in ("%SCRIPT_DIR%..\config\paths.env") do (
        if "%%A"=="ELVIS_DATA_DIR" set DATA_DIR=%%B\mysql
        if "%%A"=="ELVIS_MYSQL_PORT" set MYSQL_PORT=%%B
    )
)

echo ========================================================
echo  [ELVIS-CSMS] MySQL 9.71 로컬 서버 기동 (Port: %MYSQL_PORT%)
echo  MYSQL_HOME: %MYSQL_HOME%
echo  DATA_DIR:   %DATA_DIR%
echo  PORT:       %MYSQL_PORT%
echo ========================================================

if not exist "%DATA_DIR%" (
    echo [WARN] data 디렉토리가 없습니다. init-mysql.bat을 먼저 실행합니다.
    call "%SCRIPT_DIR%init-mysql.bat"
)

echo MySQL 서버를 콘솔 모드로 기동합니다...
set MYSQL_INI=%SCRIPT_DIR%..\config\mysql\my.ini
if not exist "%MYSQL_INI%" (
    set MYSQL_INI=%MYSQL_HOME%\my.ini
)

if exist "%MYSQL_INI%" (
    echo [INFO] 설정 파일 적용: %MYSQL_INI%
    "%MYSQL_HOME%\bin\mysqld.exe" --defaults-file="%MYSQL_INI%" --console
) else (
    echo [WARN] my.ini 파일이 없어 기본 CLI 인자로 기동합니다.
    "%MYSQL_HOME%\bin\mysqld.exe" --basedir="%MYSQL_HOME%" --datadir="%DATA_DIR%" --port=3306 --console
)

pause
