package com.lselink.elvis.control.dto.command;

/**
 * 커넥터 원격 잠금 해제 요청 DTO (UnlockConnector)
 */
public class UnlockConnectorCommandRequest {

    /** 대상 충전기 식별자 (예: "00000001") */
    private String chargeBoxId;

    /** 커넥터 번호 (1 또는 2) */
    private Integer connectorId;

    public UnlockConnectorCommandRequest() {
    }

    public UnlockConnectorCommandRequest(String chargeBoxId, Integer connectorId) {
        this.chargeBoxId = chargeBoxId;
        this.connectorId = connectorId;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public void setChargeBoxId(String chargeBoxId) {
        this.chargeBoxId = chargeBoxId;
    }

    public Integer getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(Integer connectorId) {
        this.connectorId = connectorId;
    }
}
