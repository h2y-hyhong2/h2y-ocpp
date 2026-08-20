@echo off
setlocal
chcp 65001 > nul

set SCRIPT_DIR=%~dp0
set KAFKA_HOME=%SCRIPT_DIR%..\binaries\kafka_2.13-4.3.1
set BOOTSTRAP=localhost:9092

echo ========================================================
echo  [ELVIS-CSMS] Kafka 4대 핵심 토픽 생성 스크립트
echo ========================================================

:: 1. ocpp-raw-events (Inbound, 8 Partitions)
echo [1/4] ocpp-raw-events 토픽 생성 (Partitions: 8)...
call "%KAFKA_HOME%\bin\windows\kafka-topics.bat" --create --if-not-exists --bootstrap-server %BOOTSTRAP% --topic ocpp-raw-events --partitions 8 --replication-factor 1

:: 2. ocpp-outbound-commands (Outbound, 4 Partitions)
echo [2/4] ocpp-outbound-commands 토픽 생성 (Partitions: 4)...
call "%KAFKA_HOME%\bin\windows\kafka-topics.bat" --create --if-not-exists --bootstrap-server %BOOTSTRAP% --topic ocpp-outbound-commands --partitions 4 --replication-factor 1

:: 3. ocpp-ui-notifications (UI Push, 4 Partitions)
echo [3/4] ocpp-ui-notifications 토픽 생성 (Partitions: 4)...
call "%KAFKA_HOME%\bin\windows\kafka-topics.bat" --create --if-not-exists --bootstrap-server %BOOTSTRAP% --topic ocpp-ui-notifications --partitions 4 --replication-factor 1

:: 4. ocpp-raw-events.DLT (Dead Letter, 2 Partitions)
echo [4/4] ocpp-raw-events.DLT 토픽 생성 (Partitions: 2)...
call "%KAFKA_HOME%\bin\windows\kafka-topics.bat" --create --if-not-exists --bootstrap-server %BOOTSTRAP% --topic ocpp-raw-events.DLT --partitions 2 --replication-factor 1

echo ========================================================
echo  토픽 목록 확인:
call "%KAFKA_HOME%\bin\windows\kafka-topics.bat" --list --bootstrap-server %BOOTSTRAP%
echo ========================================================
pause
