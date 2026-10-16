package com.lselink.elvis.connect.sync.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Authorize 인증 감사 이력 엔티티 (Sprint 20: S20-SYNC-0070-DE)
 * <p>
 * 충전기로부터 인입된 Authorize 요청에 대한 최종 판정 결과 및 처리 레이턴시를
 * 비동기(Fire-and-Forget)로 기록하여 감사업무 및 성능 모니터링을 지원합니다.
 * </p>
 */
@Entity
@Table(
        name = "tb_auth_hist",
        indexes = {
                @Index(name = "IX_AUTH_HIST_CHARGER_TIME", columnList = "CHARGE_BOX_ID, REG_DT"),
                @Index(name = "IX_AUTH_HIST_ID_TAG_TIME", columnList = "ID_TAG, REG_DT"),
                @Index(name = "IX_AUTH_HIST_TIME", columnList = "REG_DT")
        }
)
public class AuthHist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /** 요청 충전기 식별자 */
    @Column(name = "CHARGE_BOX_ID", nullable = false, length = 64)
    private String chargeBoxId;

    /** OCPP MessageId */
    @Column(name = "MESSAGE_ID", nullable = false, length = 64)
    private String messageId;

    /** 요청 RFID 태그 식별자 */
    @Column(name = "ID_TAG", nullable = false, length = 64)
    private String idTag;

    /** 인증 판정 결과 */
    @Enumerated(EnumType.STRING)
    @Column(name = "AUTH_STATUS", nullable = false, length = 32)
    private AuthorizationStatus authStatus;

    /** 응답된 상위 태그 식별자 */
    @Column(name = "PARENT_ID_TAG", length = 64)
    private String parentIdTag;

    /** 인증 실패/차단 상세 사유 */
    @Column(name = "FAIL_REASON", length = 128)
    private String failReason;

    /** 캐시 적중 계층 (L1_CAFFEINE, L2_REDIS, L3_DB, MISS) */
    @Column(name = "CACHE_TIER", nullable = false, length = 16)
    private String cacheTier = "L2_REDIS";

    /** 판정 소요 시간 (밀리초) */
    @Column(name = "LATENCY_MS", nullable = false, precision = 8, scale = 2)
    private BigDecimal latencyMs = BigDecimal.ZERO;

    /** 인증 요청 수신 일시 */
    @CreationTimestamp
    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt;

    protected AuthHist() {
    }

    public AuthHist(Long id, String chargeBoxId, String messageId, String idTag, AuthorizationStatus authStatus,
                    String parentIdTag, String failReason, String cacheTier, BigDecimal latencyMs, LocalDateTime regDt) {
        this.id = id;
        this.chargeBoxId = chargeBoxId;
        this.messageId = messageId;
        this.idTag = idTag;
        this.authStatus = authStatus;
        this.parentIdTag = parentIdTag;
        this.failReason = failReason;
        this.cacheTier = cacheTier != null ? cacheTier : "L2_REDIS";
        this.latencyMs = latencyMs != null ? latencyMs : BigDecimal.ZERO;
        this.regDt = regDt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getChargeBoxId() { return chargeBoxId; }
    public void setChargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getIdTag() { return idTag; }
    public void setIdTag(String idTag) { this.idTag = idTag; }

    public AuthorizationStatus getAuthStatus() { return authStatus; }
    public void setAuthStatus(AuthorizationStatus authStatus) { this.authStatus = authStatus; }

    public String getParentIdTag() { return parentIdTag; }
    public void setParentIdTag(String parentIdTag) { this.parentIdTag = parentIdTag; }

    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }

    public String getCacheTier() { return cacheTier; }
    public void setCacheTier(String cacheTier) { this.cacheTier = cacheTier; }

    public BigDecimal getLatencyMs() { return latencyMs; }
    public void setLatencyMs(BigDecimal latencyMs) { this.latencyMs = latencyMs; }

    public LocalDateTime getRegDt() { return regDt; }
    public void setRegDt(LocalDateTime regDt) { this.regDt = regDt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String chargeBoxId;
        private String messageId;
        private String idTag;
        private AuthorizationStatus authStatus;
        private String parentIdTag;
        private String failReason;
        private String cacheTier = "L2_REDIS";
        private BigDecimal latencyMs = BigDecimal.ZERO;
        private LocalDateTime regDt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder chargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; return this; }
        public Builder messageId(String messageId) { this.messageId = messageId; return this; }
        public Builder idTag(String idTag) { this.idTag = idTag; return this; }
        public Builder authStatus(AuthorizationStatus authStatus) { this.authStatus = authStatus; return this; }
        public Builder parentIdTag(String parentIdTag) { this.parentIdTag = parentIdTag; return this; }
        public Builder failReason(String failReason) { this.failReason = failReason; return this; }
        public Builder cacheTier(String cacheTier) { this.cacheTier = cacheTier; return this; }
        public Builder latencyMs(BigDecimal latencyMs) { this.latencyMs = latencyMs; return this; }
        public Builder regDt(LocalDateTime regDt) { this.regDt = regDt; return this; }

        public AuthHist build() {
            return new AuthHist(id, chargeBoxId, messageId, idTag, authStatus, parentIdTag, failReason, cacheTier, latencyMs, regDt);
        }
    }
}
