package com.lselink.elvis.connect.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 게이트웨이로부터 Kafka ocpp-raw-events 토픽으로 전달된 Raw JSON 패킷 봉투 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcppRawEventEnvelope {

    private String chargeBoxId;
    private String sessionId;
    private String remoteAddress;
    private String rawPayload;
    private Instant receivedAt;
}
