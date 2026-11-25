@echo off
setlocal
chcp 65001 > nul

set "SCRIPT_DIR=%~dp0"
if not defined ELVIS_HOME (
    for %%I in ("%SCRIPT_DIR%..") do set "ELVIS_HOME=%%~fI"
)
if "%ELVIS_HOME:~-1%"=="\" set "ELVIS_HOME=%ELVIS_HOME:~0,-1%"

set "MYSQL_HOME=%ELVIS_HOME%\binaries\mysql-9.7.1-winx64"
set "DATA_DIR=%ELVIS_HOME%\data\mysql"

rem paths.env 설정 파일이 존재하면 동적 경로 반영
if exist "%ELVIS_HOME%\config\paths.env" (
    for /f "usebackq tokens=1,* delims==" %%A in ("%ELVIS_HOME%\config\paths.env") do (
        if /I "%%A"=="ELVIS_DATA_DIR" set "DATA_DIR=%%B\mysql"
    )
)

call set "DATA_DIR=%DATA_DIR%"
if "%DATA_DIR:~-1%"=="\" set "DATA_DIR=%DATA_DIR:~0,-1%"

echo ========================================================
echo  [ELVIS-CSMS] MySQL 9.71 데이터 디렉토리 초기화
echo  ELVIS_HOME: %ELVIS_HOME%
echo  MYSQL_HOME: %MYSQL_HOME%
echo  DATA_DIR:   %DATA_DIR%
echo ========================================================

if not exist "%MYSQL_HOME%\bin\mysqld.exe" (
    echo [ERROR] MySQL 바이너리를 찾을 수 없습니다: %MYSQL_HOME%\bin\mysqld.exe
    if not "%NO_PAUSE%"=="1" pause
    exit /b 1
)

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
