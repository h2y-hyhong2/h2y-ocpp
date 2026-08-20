package com.lselink.elvis.connect.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * 충전기 → CSMS 수신 OCPP Raw JSON 패킷 Kafka 전송 래퍼
 * Kafka Topic: ocpp-raw-events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcppRawEventEnvelope implements Serializable {

    /** 충전기 고유 식별자 */
    private String chargeBoxId;

    /** 수신된 OCPP 원본 JSON 문자열 */
    private String rawPayload;

    /** 수신 시각 (UTC Epoch) */
    private Instant receivedAt;

    /** WebSocket 세션 식별자 */
    private String sessionId;
}
