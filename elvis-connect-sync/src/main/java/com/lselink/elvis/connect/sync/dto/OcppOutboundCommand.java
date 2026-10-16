package com.lselink.elvis.connect.sync.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * CSMS → 충전기 하향 응답/명령 Kafka 전송 래퍼 (Sprint 20: S20-SYNC-0070-DE)
 * Kafka Topic: ocpp-outbound-commands
 */
public class OcppOutboundCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 대상 충전기 고유 식별자 */
    private String chargeBoxId;

    /** 전송할 OCPP 응답/명령 JSON 문자열 (예: [3, "<MessageId>", {"idTagInfo":{...}}]) */
    private String commandPayload;

    /** 명령 생성 시각 (UTC Epoch) */
    private Instant createdAt = Instant.now();

    /** Action 유형 (Authorize, RemoteStartTransaction 등) */
    private String action;

    /** 메시지 고유 식별자 (Call/CallResult MessageId) */
    private String messageId;

    public OcppOutboundCommand() {
    }

    public OcppOutboundCommand(String chargeBoxId, String commandPayload, Instant createdAt, String action, String messageId) {
        this.chargeBoxId = chargeBoxId;
        this.commandPayload = commandPayload;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.action = action;
        this.messageId = messageId;
    }

    public String getChargeBoxId() { return chargeBoxId; }
    public void setChargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; }

    public String getCommandPayload() { return commandPayload; }
    public void setCommandPayload(String commandPayload) { this.commandPayload = commandPayload; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getPayload() {
        return commandPayload;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String chargeBoxId;
        private String commandPayload;
        private Instant createdAt = Instant.now();
        private String action;
        private String messageId;

        public Builder chargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; return this; }
        public Builder commandPayload(String commandPayload) { this.commandPayload = commandPayload; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder messageId(String messageId) { this.messageId = messageId; return this; }

        public OcppOutboundCommand build() {
            return new OcppOutboundCommand(chargeBoxId, commandPayload, createdAt, action, messageId);
        }
    }
}
