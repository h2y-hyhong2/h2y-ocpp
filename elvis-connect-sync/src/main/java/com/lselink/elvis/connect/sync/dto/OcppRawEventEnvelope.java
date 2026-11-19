package com.lselink.elvis.connect.sync.dto;

import java.time.Instant;

/**
 * 게이트웨이로부터 Kafka ocpp-raw-events 토픽으로 전달된 Raw JSON 패킷 봉투 DTO
 */
public class OcppRawEventEnvelope {

    public static final String EVENT_TYPE_OCPP_MESSAGE = "OCPP_MESSAGE";
    public static final String EVENT_TYPE_DISCONNECTED = "DISCONNECTED";

    private String eventType = EVENT_TYPE_OCPP_MESSAGE;
    private String chargeBoxId;
    private String sessionId;
    private String remoteAddress;
    private String rawPayload;
    private Instant receivedAt;

    public OcppRawEventEnvelope() {
    }

    public OcppRawEventEnvelope(String chargeBoxId, String rawPayload) {
        this(EVENT_TYPE_OCPP_MESSAGE, chargeBoxId, null, null, rawPayload, Instant.now());
    }

    public OcppRawEventEnvelope(String chargeBoxId, String sessionId, String remoteAddress, String rawPayload, Instant receivedAt) {
        this(EVENT_TYPE_OCPP_MESSAGE, chargeBoxId, sessionId, remoteAddress, rawPayload, receivedAt);
    }

    public OcppRawEventEnvelope(String eventType, String chargeBoxId, String sessionId, String remoteAddress, String rawPayload, Instant receivedAt) {
        this.eventType = (eventType != null && !eventType.isBlank()) ? eventType : EVENT_TYPE_OCPP_MESSAGE;
        this.chargeBoxId = chargeBoxId;
        this.sessionId = sessionId;
        this.remoteAddress = remoteAddress;
        this.rawPayload = rawPayload;
        this.receivedAt = receivedAt;
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public boolean isDisconnectedEvent() {
        return EVENT_TYPE_DISCONNECTED.equalsIgnoreCase(this.eventType);
    }

    public String getChargeBoxId() { return chargeBoxId; }
    public void setChargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getRemoteAddress() { return remoteAddress; }
    public void setRemoteAddress(String remoteAddress) { this.remoteAddress = remoteAddress; }

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventType = EVENT_TYPE_OCPP_MESSAGE;
        private String chargeBoxId;
        private String sessionId;
        private String remoteAddress;
        private String rawPayload;
        private Instant receivedAt;

        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        public Builder chargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; return this; }
        public Builder sessionId(String sessionId) { this.sessionId = sessionId; return this; }
        public Builder remoteAddress(String remoteAddress) { this.remoteAddress = remoteAddress; return this; }
        public Builder rawPayload(String rawPayload) { this.rawPayload = rawPayload; return this; }
        public Builder receivedAt(Instant receivedAt) { this.receivedAt = receivedAt; return this; }

        public OcppRawEventEnvelope build() {
            return new OcppRawEventEnvelope(eventType, chargeBoxId, sessionId, remoteAddress, rawPayload, receivedAt);
        }
    }
}
