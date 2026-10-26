package com.lselink.elvis.control.dto.command;

/**
 * 충전기 전문 즉시 보고 트리거 요청 DTO (TriggerMessage)
 */
public class TriggerMessageCommandRequest {

    /** 대상 충전기 식별자 (예: "00000001") */
    private String chargeBoxId;

    /** 요청할 전문 유형 (예: "StatusNotification", "MeterValues", "Heartbeat") */
    private String requestedMessage;

    /** 특정 커넥터 대상일 경우 번호 (선택적: 1, 2) */
    private Integer connectorId;

    public TriggerMessageCommandRequest() {
    }

    public TriggerMessageCommandRequest(String chargeBoxId, String requestedMessage, Integer connectorId) {
        this.chargeBoxId = chargeBoxId;
        this.requestedMessage = requestedMessage;
        this.connectorId = connectorId;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public void setChargeBoxId(String chargeBoxId) {
        this.chargeBoxId = chargeBoxId;
    }

    public String getRequestedMessage() {
        return requestedMessage;
    }

    public void setRequestedMessage(String requestedMessage) {
        this.requestedMessage = requestedMessage;
    }

    public Integer getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(Integer connectorId) {
        this.connectorId = connectorId;
    }
}
