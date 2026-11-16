package com.lselink.elvis.control.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 원격 충전 강제 종료 요청 DTO (RemoteStopTransaction)
 */
public class RemoteStopCommandRequest {

    /** 대상 충전기 식별자 (예: "00000001") */
    @NotBlank(message = "chargeBoxId는 필수입니다.")
    private String chargeBoxId;

    /** 종료 대상 활성 트랜잭션 번호 */
    @NotNull(message = "transactionId는 필수입니다.")
    private Integer transactionId;

    public RemoteStopCommandRequest() {
    }

    public RemoteStopCommandRequest(String chargeBoxId, Integer transactionId) {
        this.chargeBoxId = chargeBoxId;
        this.transactionId = transactionId;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public void setChargeBoxId(String chargeBoxId) {
        this.chargeBoxId = chargeBoxId;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }
}
