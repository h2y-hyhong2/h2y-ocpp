@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-ui.ps1"
if %ERRORLEVEL% neq 0 pause
