@echo off
setlocal
chcp 65001 > nul
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0check-process-status.ps1" -Target "%~1"
