package com.lselink.elvis.tester.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadTestRequest {
    @Builder.Default
    private String topic = "ocpp-raw-events";
    @Builder.Default
    private String chargeBoxPrefix = "CP_TEST_";
    @Builder.Default
    private int deviceCount = 100;
    @Builder.Default
    private int tps = 50;
    @Builder.Default
    private int durationSeconds = 30;
    @Builder.Default
    private String templateName = "MeterValues";
}
