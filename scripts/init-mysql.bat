@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0
set MYSQL_HOME=%SCRIPT_DIR%..\binaries\mysql-9.7.1-winx64
set DATA_DIR=%MYSQL_HOME%\data

echo ========================================================
echo  [ELVIS-CSMS] MySQL 9.71 데이터 디렉토리 초기화
echo  MYSQL_HOME: %MYSQL_HOME%
echo  DATA_DIR:   %DATA_DIR%
echo ========================================================

if exist "%DATA_DIR%" (
    echo [INFO] 이미 data 디렉토리가 존재합니다: %DATA_DIR%
    echo 초기화를 다시 하려면 기존 data 폴더를 삭제 후 실행해주세요.
    pause
    exit /b 0
)

echo [1/1] MySQL 9.71 초기화 진행 중 (insecure mode)...
"%MYSQL_HOME%\bin\mysqld.exe" --initialize-insecure --basedir="%MYSQL_HOME%" --datadir="%DATA_DIR%" --console

if %ERRORLEVEL% equ 0 (
    echo ========================================================
    echo  MySQL 초기화가 완료되었습니다! (root 비밀번호: 없음)
    echo  start-mysql.bat 을 실행하여 서버를 시작하세요.
    echo ========================================================
) else (
    echo [ERROR] MySQL 초기화 중 오류가 발생했습니다.
)

pause
