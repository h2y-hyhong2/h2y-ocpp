package com.lselink.elvis.connect.ws.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * 충전기 → CSMS 수신 OCPP Raw JSON 패킷 Kafka 전송 래퍼
 * Kafka Topic: ocpp-raw-events
 */
public class OcppRawEventEnvelope implements Serializable {

    /** 충전기 고유 식별자 */
    private String chargeBoxId;

    /** 수신된 OCPP 원본 JSON 문자열 */
    private String rawPayload;

    /** 수신 시각 (UTC Epoch) */
    private Instant receivedAt;

    /** WebSocket 세션 식별자 */
    private String sessionId;

    /** 원격 클라이언트 IP/소켓 주소 */
    private String remoteAddress;

    public OcppRawEventEnvelope() {
    }

    public OcppRawEventEnvelope(String chargeBoxId, String rawPayload, Instant receivedAt, String sessionId, String remoteAddress) {
        this.chargeBoxId = chargeBoxId;
        this.rawPayload = rawPayload;
        this.receivedAt = receivedAt;
        this.sessionId = sessionId;
        this.remoteAddress = remoteAddress;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public void setChargeBoxId(String chargeBoxId) {
        this.chargeBoxId = chargeBoxId;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String chargeBoxId;
        private String rawPayload;
        private Instant receivedAt;
        private String sessionId;
        private String remoteAddress;

        public Builder chargeBoxId(String chargeBoxId) {
            this.chargeBoxId = chargeBoxId;
            return this;
        }

        public Builder rawPayload(String rawPayload) {
            this.rawPayload = rawPayload;
            return this;
        }

        public Builder receivedAt(Instant receivedAt) {
            this.receivedAt = receivedAt;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder remoteAddress(String remoteAddress) {
            this.remoteAddress = remoteAddress;
            return this;
        }

        public OcppRawEventEnvelope build() {
            return new OcppRawEventEnvelope(chargeBoxId, rawPayload, receivedAt, sessionId, remoteAddress);
        }
    }
}
