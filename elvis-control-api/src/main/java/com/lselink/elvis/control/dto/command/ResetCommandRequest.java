package com.lselink.elvis.control.dto.command;

/**
 * 충전기 원격 리셋 요청 DTO (Reset)
 */
public class ResetCommandRequest {

    /** 대상 충전기 식별자 (예: "00000001") */
    private String chargeBoxId;

    /** 리셋 유형 ("Soft" 또는 "Hard") */
    private String type;

    public ResetCommandRequest() {
    }

    public ResetCommandRequest(String chargeBoxId, String type) {
        this.chargeBoxId = chargeBoxId;
        this.type = type;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public void setChargeBoxId(String chargeBoxId) {
        this.chargeBoxId = chargeBoxId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
