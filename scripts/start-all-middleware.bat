@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0

echo ========================================================
echo  [ELVIS-CSMS] 로컬 미들웨어 (Kafka & MySQL) 일괄 기동
echo ========================================================

echo [1/2] Kafka (KRaft Mode, Port: 9092) 새 창에서 기동...
start "ELVIS - Apache Kafka" cmd /k "call "%SCRIPT_DIR%start-kafka-kraft.bat""

timeout /t 3 /nobreak > nul

echo [2/2] MySQL 9.71 (Port: 3306) 새 창에서 기동...
start "ELVIS - MySQL 9.71" cmd /k "call "%SCRIPT_DIR%start-mysql.bat""

echo ========================================================
echo  Kafka와 MySQL이 각각 독립된 콘솔 창에서 기동되었습니다.
echo  Kafka 토픽 생성이 필요하면 create-kafka-topics.bat 을 실행하세요.
echo ========================================================
