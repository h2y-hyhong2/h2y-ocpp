package com.lselink.elvis.connect.sync.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lselink.elvis.connect.sync.domain.AuthorizationStatus;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OCPP 1.6-J Authorize.conf 응답의 idTagInfo 페이로드 DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdTagInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 인증 상태 (Accepted, Blocked, Expired, Invalid, ConcurrentTx) */
    private AuthorizationStatus status;

    /** 카드 유효기간 만료일시 (UTC ISO-8601 포맷) */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime expiryDate;

    /** 상위/법인 대표 태그 식별자 (선택) */
    private String parentIdTag;

    /** 캐시 적중 계층 메타데이터 (L1_CAFFEINE, L2_REDIS, L3_DB 등 내부 추적용) */
    private transient String cacheTier;

    /** 판정 소요시간 (밀리초) */
    private transient double latencyMs;

    public IdTagInfo() {
    }

    public IdTagInfo(AuthorizationStatus status, LocalDateTime expiryDate, String parentIdTag, String cacheTier, double latencyMs) {
        this.status = status;
        this.expiryDate = expiryDate;
        this.parentIdTag = parentIdTag;
        this.cacheTier = cacheTier;
        this.latencyMs = latencyMs;
    }

    public AuthorizationStatus getStatus() { return status; }
    public void setStatus(AuthorizationStatus status) { this.status = status; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public String getParentIdTag() { return parentIdTag; }
    public void setParentIdTag(String parentIdTag) { this.parentIdTag = parentIdTag; }

    public String getCacheTier() { return cacheTier; }
    public void setCacheTier(String cacheTier) { this.cacheTier = cacheTier; }

    public double getLatencyMs() { return latencyMs; }
    public void setLatencyMs(double latencyMs) { this.latencyMs = latencyMs; }

    public static IdTagInfo of(AuthorizationStatus status) {
        return IdTagInfo.builder()
                .status(status)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AuthorizationStatus status;
        private LocalDateTime expiryDate;
        private String parentIdTag;
        private String cacheTier;
        private double latencyMs;

        public Builder status(AuthorizationStatus status) { this.status = status; return this; }
        public Builder expiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; return this; }
        public Builder parentIdTag(String parentIdTag) { this.parentIdTag = parentIdTag; return this; }
        public Builder cacheTier(String cacheTier) { this.cacheTier = cacheTier; return this; }
        public Builder latencyMs(double latencyMs) { this.latencyMs = latencyMs; return this; }

        public IdTagInfo build() {
            return new IdTagInfo(status, expiryDate, parentIdTag, cacheTier, latencyMs);
        }
    }

    public static IdTagInfo invalid() {
        return IdTagInfo.builder()
                .status(AuthorizationStatus.Invalid)
                .build();
    }
}
