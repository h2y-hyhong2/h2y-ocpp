@echo off
setlocal
chcp 65001 > nul

set "TARGET_DIR=%~1"
if "%TARGET_DIR%"=="" set "TARGET_DIR=D:\elvis-lite"

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy-elvis-lite.ps1" -TargetDir "%TARGET_DIR%" %2 %3 %4
exit /b %ERRORLEVEL%
