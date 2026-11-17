@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0
set MYSQL_HOME=%SCRIPT_DIR%..\binaries\mysql-9.7.1-winx64
set DATA_DIR=D:\elvis-lite\data\mysql

rem paths.env 설정 파일이 존재하면 동적 경로 반영
if exist "%SCRIPT_DIR%..\config\paths.env" (
    for /f "usebackq tokens=1,2 delims==" %%A in ("%SCRIPT_DIR%..\config\paths.env") do (
        if "%%A"=="ELVIS_DATA_DIR" set DATA_DIR=%%B\mysql
    )
)

if "%DATA_DIR:~-1%"=="\" set "DATA_DIR=%DATA_DIR:~0,-1%"

echo ========================================================
echo  [ELVIS-CSMS] MySQL 9.71 데이터 디렉토리 초기화
echo  MYSQL_HOME: %MYSQL_HOME%
echo  DATA_DIR:   %DATA_DIR%
echo ========================================================

if exist "%DATA_DIR%\mysql" (
    echo [INFO] 이미 초기화된 data 디렉토리가 존재합니다: %DATA_DIR%
    echo 초기화를 다시 하려면 기존 data 폴더를 삭제 후 실행해주세요.
    if not "%NO_PAUSE%"=="1" pause
    exit /b 0
)

for %%I in ("%DATA_DIR%") do set "DATA_PARENT=%%~dpI"
if not exist "%DATA_PARENT%" (
    echo [INFO] 데이터 상위 디렉토리를 생성합니다: %DATA_PARENT%
    mkdir "%DATA_PARENT%"
    if errorlevel 1 (
        echo [ERROR] 데이터 상위 디렉토리 생성에 실패했습니다: %DATA_PARENT%
        if not "%NO_PAUSE%"=="1" pause
        exit /b 1
    )
)

set "HAD_DATA_DIR=0"
if exist "%DATA_DIR%" set "HAD_DATA_DIR=1"

echo [1/1] MySQL 9.71 초기화 진행 중 (insecure mode)...
"%MYSQL_HOME%\bin\mysqld.exe" --initialize-insecure --basedir="%MYSQL_HOME%" --datadir="%DATA_DIR%" --console

if %ERRORLEVEL% equ 0 (
    echo ========================================================
    echo  MySQL 초기화가 완료되었습니다! - root 비밀번호 없음
    echo  start-mysql.bat 을 실행하여 서버를 시작하세요.
    echo ========================================================
    if not "%NO_PAUSE%"=="1" pause
    exit /b 0
) else (
    echo [ERROR] MySQL 초기화 중 오류가 발생했습니다.
    if "%HAD_DATA_DIR%"=="0" if exist "%DATA_DIR%" if not exist "%DATA_DIR%\mysql" (
        rmdir /s /q "%DATA_DIR%" > nul 2>&1
    )
    if not "%NO_PAUSE%"=="1" pause
    exit /b 1
)
