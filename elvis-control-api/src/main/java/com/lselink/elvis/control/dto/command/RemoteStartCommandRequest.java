package com.lselink.elvis.control.dto.command;

/**
 * 원격 충전 시작 요청 DTO (RemoteStartTransaction)
 */
public class RemoteStartCommandRequest {

    /** 대상 충전기 식별자 (예: "00000001") */
    private String chargeBoxId;

    /** 커넥터 번호 (1 또는 2) */
    private Integer connectorId;

    /** 사용자 인증 식별자 (예: "RFID-1001") */
    private String idTag;

    public RemoteStartCommandRequest() {
    }

    public RemoteStartCommandRequest(String chargeBoxId, Integer connectorId, String idTag) {
        this.chargeBoxId = chargeBoxId;
        this.connectorId = connectorId;
        this.idTag = idTag;
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

    public String getIdTag() {
        return idTag;
    }

    public void setIdTag(String idTag) {
        this.idTag = idTag;
    }
}
