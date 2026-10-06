package com.lselink.elvis.connect.sync.ocpp;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

/**
 * OCPP raw JSON 파싱 결과 DTO
 */
@Getter
@Builder
public class OcppParsedMessage {

    /** 충전기 식별자 */
    private final String chargeBoxId;

    /** OCPP 메시지 고유 ID */
    private final String uniqueId;

    /** OCPP Action 이름 (StatusNotification, Heartbeat, BootNotification 등) */
    private final String action;

    /** OCPP payload JSON 노드 */
    private final JsonNode payload;
}
