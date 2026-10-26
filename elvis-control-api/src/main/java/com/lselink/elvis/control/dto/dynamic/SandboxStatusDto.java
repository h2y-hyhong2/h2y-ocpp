package com.lselink.elvis.control.dto.dynamic;

import java.time.Instant;

/**
 * 트래픽 격리 정책 상태 응답 DTO
 */
public class SandboxStatusDto {

    /** 대상 충전기 식별자 */
    private String chargeBoxId;

    /** 격리 활성화 여부 */
    private boolean enabled;

    /** 격리 정책 모드 ("ISOLATED", "DROP", "MOCK") */
    private String policyMode;

    /** 격리 사유 */
    private String reason;

    /** 활성화 일시 (UTC Epoch) */
    private Instant activatedAt;

    /** 만료 예정 일시 (UTC Epoch, null이면 무기한) */
    private Instant expiresAt;

    /** 현재 만료 여부 */
    private boolean expired;

    public SandboxStatusDto() {
    }

    public SandboxStatusDto(String chargeBoxId, boolean enabled, String policyMode, String reason,
                            Instant activatedAt, Instant expiresAt) {
        this.chargeBoxId = chargeBoxId;
        this.enabled = enabled;
        this.policyMode = policyMode;
        this.reason = reason;
        this.activatedAt = activatedAt != null ? activatedAt : Instant.now();
        this.expiresAt = expiresAt;
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            this.expired = true;
        } else {
            this.expired = false;
        }
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public void setChargeBoxId(String chargeBoxId) {
        this.chargeBoxId = chargeBoxId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPolicyMode() {
        return policyMode;
    }

    public void setPolicyMode(String policyMode) {
        this.policyMode = policyMode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(Instant activatedAt) {
        this.activatedAt = activatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            this.expired = true;
        } else {
            this.expired = false;
        }
    }

    public boolean isExpired() {
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            return true;
        }
        return false;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }
}
