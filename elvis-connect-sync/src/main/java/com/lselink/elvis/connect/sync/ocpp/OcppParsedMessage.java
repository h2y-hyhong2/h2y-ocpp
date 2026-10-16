package com.lselink.elvis.connect.sync.ocpp;

import com.fasterxml.jackson.databind.JsonNode;
public class OcppParsedMessage {

    /** 충전기 식별자 */
    private final String chargeBoxId;

    /** OCPP 메시지 고유 ID */
    private final String uniqueId;

    /** OCPP Action 이름 (StatusNotification, Heartbeat, BootNotification 등) */
    private final String action;

    /** OCPP payload JSON 노드 */
    private final JsonNode payload;

    public OcppParsedMessage(String chargeBoxId, String uniqueId, String action, JsonNode payload) {
        this.chargeBoxId = chargeBoxId;
        this.uniqueId = uniqueId;
        this.action = action;
        this.payload = payload;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getAction() {
        return action;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String chargeBoxId;
        private String uniqueId;
        private String action;
        private JsonNode payload;

        public Builder chargeBoxId(String chargeBoxId) {
            this.chargeBoxId = chargeBoxId;
            return this;
        }

        public Builder uniqueId(String uniqueId) {
            this.uniqueId = uniqueId;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder payload(JsonNode payload) {
            this.payload = payload;
            return this;
        }

        public OcppParsedMessage build() {
            return new OcppParsedMessage(chargeBoxId, uniqueId, action, payload);
        }
    }
}
