package com.lselink.elvis.control.dto.dynamic;

/**
 * 실시간 패킷 감시(WireTap) 정책 요청 DTO
 */
public class WireTapPolicyRequest {

    /** 대상 충전기 식별자 (예: "00000001") */
    private String chargeBoxId;

    /** WireTap 활성화 여부 (true / false) */
    private Boolean enabled;

    /** 패킷 캡처 레벨 ("ALL", "CALL_ONLY", "ERROR_ONLY") */
    private String tapLevel;

    /** 최대 감시 유지 시간 (초 단위, 기본 1800초, 최대 3600초) */
    private Long maxDurationSeconds;

    public WireTapPolicyRequest() {
    }

    public WireTapPolicyRequest(String chargeBoxId, Boolean enabled, String tapLevel, Long maxDurationSeconds) {
        this.chargeBoxId = chargeBoxId;
        this.enabled = enabled;
        this.tapLevel = tapLevel;
        this.maxDurationSeconds = maxDurationSeconds;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public void setChargeBoxId(String chargeBoxId) {
        this.chargeBoxId = chargeBoxId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getTapLevel() {
        return tapLevel;
    }

    public void setTapLevel(String tapLevel) {
        this.tapLevel = tapLevel;
    }

    public Long getMaxDurationSeconds() {
        return maxDurationSeconds;
    }

    public void setMaxDurationSeconds(Long maxDurationSeconds) {
        this.maxDurationSeconds = maxDurationSeconds;
    }
}
