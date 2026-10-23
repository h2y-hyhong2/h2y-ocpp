@echo off
setlocal
chcp 65001 > nul
title ELVIS - Process and Port Status Monitor
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0check-process-status.ps1" -Target "%~1"
if errorlevel 1 pause
