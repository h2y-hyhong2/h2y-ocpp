package com.lselink.elvis.control.dto.command;

/**
 * 충전기 설정 파라미터 변경 요청 DTO (ChangeConfiguration)
 */
public class ChangeConfigurationCommandRequest {

    /** 대상 충전기 식별자 (예: "00000001") */
    private String chargeBoxId;

    /** 설정 키 (예: "HeartbeatInterval", "MeterValueSampleInterval") */
    private String key;

    /** 설정 값 (예: "60") */
    private String value;

    public ChangeConfigurationCommandRequest() {
    }

    public ChangeConfigurationCommandRequest(String chargeBoxId, String key, String value) {
        this.chargeBoxId = chargeBoxId;
        this.key = key;
        this.value = value;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public void setChargeBoxId(String chargeBoxId) {
        this.chargeBoxId = chargeBoxId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
