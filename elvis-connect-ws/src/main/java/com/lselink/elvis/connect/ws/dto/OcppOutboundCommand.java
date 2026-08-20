package com.lselink.elvis.connect.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * CSMS → 충전기 하향 명령 Kafka 전송 래퍼
 * Kafka Topic: ocpp-outbound-commands
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcppOutboundCommand implements Serializable {

    /** 대상 충전기 고유 식별자 */
    private String chargeBoxId;

    /** 전송할 OCPP 명령 JSON 문자열 */
    private String commandPayload;

    /** 명령 생성 시각 (UTC Epoch) */
    private Instant createdAt;

    /** 명령 유형 (예: RemoteStartTransaction, Reset 등) */
    private String action;
}
