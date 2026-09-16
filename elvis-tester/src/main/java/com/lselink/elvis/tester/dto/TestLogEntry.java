package com.lselink.elvis.tester.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestLogEntry {
    private String id;
    private Instant timestamp;
    private String direction; // "INBOUND" | "OUTBOUND" | "MONITOR"
    private String topic;
    private String chargeBoxId;
    private String action;
    private String payload;
    private String status; // "SUCCESS" | "FAILED"
    private String message;
}
