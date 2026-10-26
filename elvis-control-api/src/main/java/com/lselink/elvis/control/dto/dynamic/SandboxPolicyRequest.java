package com.lselink.elvis.control.dto.dynamic;

/**
 * 트래픽 격리 정책 요청 DTO (Traffic Sandbox)
 */
public class SandboxPolicyRequest {

    /** 대상 충전기 식별자 (예: "00000001") */
    private String chargeBoxId;

    /** 격리 활성화 여부 (true / false) */
    private Boolean enabled;

    /** 격리 정책 모드 ("ISOLATED", "DROP", "MOCK") */
    private String policyMode;

    /** 격리 사유 (예: "전문 폭증 감지", "단말 펌웨어 테스트") */
    private String reason;

    /** 초 단위 유지 시간 (TTL, 기본 3600초, 0이면 수동 해제 시까지) */
    private Long ttlSeconds;

    public SandboxPolicyRequest() {
    }

    public SandboxPolicyRequest(String chargeBoxId, Boolean enabled, String policyMode, String reason, Long ttlSeconds) {
        this.chargeBoxId = chargeBoxId;
        this.enabled = enabled;
        this.policyMode = policyMode;
        this.reason = reason;
        this.ttlSeconds = ttlSeconds;
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

    public Long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(Long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
}
