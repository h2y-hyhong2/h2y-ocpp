@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0
set LAUNCHER_ROOT=%SCRIPT_DIR%..\elvis-launcher

where fnm >nul 2>nul
if %ERRORLEVEL% equ 0 (
    call fnm use 24 >nul 2>nul
)

echo ========================================================
echo  [ELVIS-LAUNCHER] Rust Tauri 시스템 트레이 인프라 관리자
echo  MySQL 9.71 & Apache Kafka KRaft 24시간 백그라운드 운용반
echo ========================================================
echo.
echo  [1] 🚀 Tauri 시스템 트레이 데스크톱 앱 실행 (권장: 작업표시줄 상주)
echo  [2] 🌐 브라우저 웹 모드 실행 (http://127.0.0.1:1422)
echo.

set /p LAUNCHER_MODE="실행 모드를 선택하세요 (1 또는 2) [기본값: 1]: "
if "%LAUNCHER_MODE%"=="" set LAUNCHER_MODE=1
if "%LAUNCHER_MODE%"==" " set LAUNCHER_MODE=1

cd /d "%LAUNCHER_ROOT%"
title ELVIS Infrastructure Launcher

if "%LAUNCHER_MODE%"=="2" (
    echo [INFO] 웹 브라우저에서 ELVIS Launcher를 엽니다...
    start http://localhost:1422
    echo [INFO] Vite 런처 웹 서버 기동 중...
    call pnpm dev
) else (
    echo [INFO] Rust Tauri 시스템 트레이 애플리케이션 기동 중...
    call pnpm tauri:dev
)

pause
