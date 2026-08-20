@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0
set MYSQL_HOME=%SCRIPT_DIR%..\binaries\mysql-9.7.1-winx64
set DATA_DIR=%MYSQL_HOME%\data

echo ========================================================
echo  [ELVIS-CSMS] MySQL 9.71 로컬 서버 기동 (Port: 3306)
echo  MYSQL_HOME: %MYSQL_HOME%
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
