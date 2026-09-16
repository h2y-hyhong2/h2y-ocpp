package com.lselink.elvis.tester.service;

import com.lselink.elvis.tester.dto.TemplateDto;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class TemplateRegistry {

    private final List<TemplateDto> templates = new ArrayList<>();

    public TemplateRegistry() {
        initTemplates();
    }

    public List<TemplateDto> getAllTemplates() {
        return templates;
    }

    private void initTemplates() {
        templates.add(TemplateDto.builder()
                .id("boot-notification")
                .name("BootNotification")
                .action("BootNotification")
                .defaultTopic("ocpp-raw-events")
                .description("충전기 부팅 및 게이트웨이 최초 등록 알림")
                .samplePayload("""
                        [2, "%s", "BootNotification", {
                          "chargePointVendor": "LSELINK",
                          "chargePointModel": "ELVIS-FAST-200",
                          "chargePointSerialNumber": "SN-2026-0001",
                          "firmwareVersion": "v1.2.0"
                        }]
                        """.formatted(UUID.randomUUID().toString()))
                .build());

        templates.add(TemplateDto.builder()
                .id("heartbeat")
                .name("Heartbeat")
                .action("Heartbeat")
                .defaultTopic("ocpp-raw-events")
                .description("충전기 생존 신호 (주기 전송)")
                .samplePayload("""
                        [2, "%s", "Heartbeat", {}]
                        """.formatted(UUID.randomUUID().toString()))
                .build());

        templates.add(TemplateDto.builder()
                .id("status-available")
                .name("StatusNotification (Available)")
                .action("StatusNotification")
                .defaultTopic("ocpp-raw-events")
                .description("커넥터 충전 대기 상태 (Available)")
                .samplePayload("""
                        [2, "%s", "StatusNotification", {
                          "connectorId": 1,
                          "status": "Available",
                          "errorCode": "NoError",
                          "timestamp": "%s"
                        }]
                        """.formatted(UUID.randomUUID().toString(), Instant.now().toString()))
                .build());

        templates.add(TemplateDto.builder()
                .id("status-charging")
                .name("StatusNotification (Charging)")
                .action("StatusNotification")
                .defaultTopic("ocpp-raw-events")
                .description("커넥터 충전 진행 상태 (Charging)")
                .samplePayload("""
                        [2, "%s", "StatusNotification", {
                          "connectorId": 1,
                          "status": "Charging",
                          "errorCode": "NoError",
                          "timestamp": "%s"
                        }]
                        """.formatted(UUID.randomUUID().toString(), Instant.now().toString()))
                .build());

        templates.add(TemplateDto.builder()
                .id("start-transaction")
                .name("StartTransaction")
                .action("StartTransaction")
                .defaultTopic("ocpp-raw-events")
                .description("충전 시작 알림 및 미터 시작값 전송")
                .samplePayload("""
                        [2, "%s", "StartTransaction", {
                          "connectorId": 1,
                          "idTag": "TAG-USER-9988",
                          "meterStart": 12500,
                          "timestamp": "%s"
                        }]
                        """.formatted(UUID.randomUUID().toString(), Instant.now().toString()))
                .build());

        templates.add(TemplateDto.builder()
                .id("meter-values")
                .name("MeterValues (전력량)")
                .action("MeterValues")
                .defaultTopic("ocpp-raw-events")
                .description("실시간 충전 전력량 및 전압/전류 계측값")
                .samplePayload("""
                        [2, "%s", "MeterValues", {
                          "connectorId": 1,
                          "transactionId": 10001,
                          "meterValue": [{
                            "timestamp": "%s",
                            "sampledValue": [
                              { "value": "13520", "context": "Sample.Periodic", "measurand": "Energy.Active.Import.Register", "unit": "Wh" },
                              { "value": "228.5", "measurand": "Voltage", "unit": "V" },
                              { "value": "31.2", "measurand": "Current.Import", "unit": "A" },
                              { "value": "7130", "measurand": "Power.Active.Import", "unit": "W" }
                            ]
                          }]
                        }]
                        """.formatted(UUID.randomUUID().toString(), Instant.now().toString()))
                .build());

        templates.add(TemplateDto.builder()
                .id("stop-transaction")
                .name("StopTransaction")
                .action("StopTransaction")
                .defaultTopic("ocpp-raw-events")
                .description("충전 종료 및 최종 정산 계측값 보고")
                .samplePayload("""
                        [2, "%s", "StopTransaction", {
                          "transactionId": 10001,
                          "idTag": "TAG-USER-9988",
                          "meterStop": 25800,
                          "timestamp": "%s",
                          "reason": "EVDisconnected"
                        }]
                        """.formatted(UUID.randomUUID().toString(), Instant.now().toString()))
                .build());

        templates.add(TemplateDto.builder()
                .id("remote-start")
                .name("RemoteStartTransaction (하향)")
                .action("RemoteStartTransaction")
                .defaultTopic("ocpp-outbound-commands")
                .description("CSMS -> 단말 충전 원격 시작 제어 명령")
                .samplePayload("""
                        {
                          "chargeBoxId": "CP_1001",
                          "messageId": "%s",
                          "action": "RemoteStartTransaction",
                          "payload": "[2, \\"%s\\", \\"RemoteStartTransaction\\", {\\"connectorId\\": 1, \\"idTag\\": \\"APP-REMOTE-01\\"}]"
                        }
                        """.formatted(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .build());

        templates.add(TemplateDto.builder()
                .id("reset-command")
                .name("Reset (하향)")
                .action("Reset")
                .defaultTopic("ocpp-outbound-commands")
                .description("CSMS -> 단말 리셋(Soft/Hard) 원격 제어 명령")
                .samplePayload("""
                        {
                          "chargeBoxId": "CP_1001",
                          "messageId": "%s",
                          "action": "Reset",
                          "payload": "[2, \\"%s\\", \\"Reset\\", {\\"type\\": \\"Soft\\"}]"
                        }
                        """.formatted(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .build());
    }
}
