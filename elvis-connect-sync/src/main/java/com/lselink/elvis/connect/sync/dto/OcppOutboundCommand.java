package com.lselink.elvis.connect.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * CSMS → 충전기 하향 응답/명령 Kafka 전송 래퍼 (Sprint 20: S20-SYNC-0070-DE)
 * Kafka Topic: ocpp-outbound-commands
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcppOutboundCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 대상 충전기 고유 식별자 */
    private String chargeBoxId;

    /** 전송할 OCPP 응답/명령 JSON 문자열 (예: [3, "<MessageId>", {"idTagInfo":{...}}]) */
    private String commandPayload;

    /** 명령 생성 시각 (UTC Epoch) */
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** Action 유형 (Authorize, RemoteStartTransaction 등) */
    private String action;
}
