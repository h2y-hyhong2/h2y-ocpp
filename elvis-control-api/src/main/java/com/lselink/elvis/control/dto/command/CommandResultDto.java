package com.lselink.elvis.control.dto.command;

import java.time.Instant;

/**
 * 원격 제어 명령 실행 결과 DTO
 */
public class CommandResultDto {

    /** 발송된 명령 고유 식별자 (OCPP messageId 매핑) */
    private String commandId;

    /** 대상 충전기 식별자 (예: "00000001") */
    private String chargeBoxId;

    /** 실행 명령 액션명 (예: "RemoteStartTransaction", "Reset") */
    private String action;

    /** 명령 전송 상태 ("SENT", "FAILED") */
    private String status;

    /** 상세 메시지 또는 실패 사유 */
    private String message;

    /** 명령 발송 일시 (UTC Epoch) */
    private Instant sentAt;

    public CommandResultDto() {
    }

    public CommandResultDto(String commandId, String chargeBoxId, String action, String status, String message, Instant sentAt) {
        this.commandId = commandId;
        this.chargeBoxId = chargeBoxId;
        this.action = action;
        this.status = status;
        this.message = message;
        this.sentAt = sentAt != null ? sentAt : Instant.now();
    }

    public static CommandResultDto sent(String commandId, String chargeBoxId, String action, String message) {
        return new CommandResultDto(commandId, chargeBoxId, action, "SENT", message, Instant.now());
    }

    public static CommandResultDto failed(String commandId, String chargeBoxId, String action, String message) {
        return new CommandResultDto(commandId, chargeBoxId, action, "FAILED", message, Instant.now());
    }

    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public void setChargeBoxId(String chargeBoxId) {
        this.chargeBoxId = chargeBoxId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
