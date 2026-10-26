package com.lselink.elvis.control.dto.dynamic;

import java.time.Instant;

/**
 * 실시간 패킷 감시(WireTap) 상태 응답 DTO
 */
public class WireTapStatusDto {

    /** 대상 충전기 식별자 */
    private String chargeBoxId;

    /** WireTap 활성화 여부 */
    private boolean enabled;

    /** 패킷 캡처 레벨 ("ALL", "CALL_ONLY", "ERROR_ONLY") */
    private String tapLevel;

    /** 감시 활성화 일시 (UTC Epoch) */
    private Instant activatedAt;

    /** 만료 예정 일시 (UTC Epoch) */
    private Instant expiresAt;

    /** 현재 만료 여부 */
    private boolean expired;

    public WireTapStatusDto() {
    }

    public WireTapStatusDto(String chargeBoxId, boolean enabled, String tapLevel,
                            Instant activatedAt, Instant expiresAt) {
        this.chargeBoxId = chargeBoxId;
        this.enabled = enabled;
        this.tapLevel = tapLevel;
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

    public String getTapLevel() {
        return tapLevel;
    }

    public void setTapLevel(String tapLevel) {
        this.tapLevel = tapLevel;
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
