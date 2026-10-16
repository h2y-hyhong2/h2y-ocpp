package com.lselink.elvis.connect.sync.dto;

import java.time.Instant;

/**
 * 게이트웨이로부터 Kafka ocpp-raw-events 토픽으로 전달된 Raw JSON 패킷 봉투 DTO
 */
public class OcppRawEventEnvelope {

    private String chargeBoxId;
    private String sessionId;
    private String remoteAddress;
    private String rawPayload;
    private Instant receivedAt;

    public OcppRawEventEnvelope() {
    }

    public OcppRawEventEnvelope(String chargeBoxId, String sessionId, String remoteAddress, String rawPayload, Instant receivedAt) {
        this.chargeBoxId = chargeBoxId;
        this.sessionId = sessionId;
        this.remoteAddress = remoteAddress;
        this.rawPayload = rawPayload;
        this.receivedAt = receivedAt;
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
        private String chargeBoxId;
        private String sessionId;
        private String remoteAddress;
        private String rawPayload;
        private Instant receivedAt;

        public Builder chargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; return this; }
        public Builder sessionId(String sessionId) { this.sessionId = sessionId; return this; }
        public Builder remoteAddress(String remoteAddress) { this.remoteAddress = remoteAddress; return this; }
        public Builder rawPayload(String rawPayload) { this.rawPayload = rawPayload; return this; }
        public Builder receivedAt(Instant receivedAt) { this.receivedAt = receivedAt; return this; }

        public OcppRawEventEnvelope build() {
            return new OcppRawEventEnvelope(chargeBoxId, sessionId, remoteAddress, rawPayload, receivedAt);
        }
    }
}
