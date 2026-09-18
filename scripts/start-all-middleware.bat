@echo off
setlocal
set SCRIPT_DIR=%~dp0

echo ========================================================
echo  [ELVIS-CSMS] Local Middleware Starter (Kafka and MySQL)
echo ========================================================

echo [1/2] Starting Kafka Broker (Port: 9092)...
start "ELVIS-Kafka" "%SCRIPT_DIR%start-kafka-kraft.bat"

timeout /t 3 /nobreak > nul

echo [2/2] Starting MySQL 9.71 Server (Port: 3306)...
start "ELVIS-MySQL" "%SCRIPT_DIR%start-mysql.bat"

echo ========================================================
echo  Kafka and MySQL successfully started in background!
echo ========================================================
