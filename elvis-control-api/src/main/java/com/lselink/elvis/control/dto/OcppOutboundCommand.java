package com.lselink.elvis.control.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * CSMS → 충전기 하향 명령 Kafka 전송 래퍼
 * <p>
 * Kafka Topic: ocpp-outbound-commands
 * 게이트웨이(elvis-connect-ws)의 OutboundCommandConsumer에서 역직렬화되는 규격과 100% 일치합니다.
 * </p>
 */
public class OcppOutboundCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 대상 충전기 고유 식별자 */
    private String chargeBoxId;

    /** 전송할 OCPP 명령 JSON 문자열 (예: [2, "<messageId>", "<Action>", {...}]) */
    private String commandPayload;

    /** 명령 생성 시각 (UTC Epoch) */
    private Instant createdAt = Instant.now();

    /** 명령 유형 (예: RemoteStartTransaction, Reset 등) */
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

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public void setChargeBoxId(String chargeBoxId) {
        this.chargeBoxId = chargeBoxId;
    }

    public String getCommandPayload() {
        return commandPayload;
    }

    public void setCommandPayload(String commandPayload) {
        this.commandPayload = commandPayload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getPayload() {
        return commandPayload;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String chargeBoxId;
        private String commandPayload;
        private Instant createdAt;
        private String action;
        private String messageId;

        public Builder chargeBoxId(String chargeBoxId) {
            this.chargeBoxId = chargeBoxId;
            return this;
        }

        public Builder commandPayload(String commandPayload) {
            this.commandPayload = commandPayload;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public OcppOutboundCommand build() {
            return new OcppOutboundCommand(chargeBoxId, commandPayload, createdAt, action, messageId);
        }
    }
}
