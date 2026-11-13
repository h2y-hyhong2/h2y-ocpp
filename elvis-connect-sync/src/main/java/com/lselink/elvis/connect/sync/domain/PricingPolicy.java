package com.lselink.elvis.connect.sync.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 충전 요금 정책 마스터 엔티티 (Sprint 21: S21-SYNC-0080-CO)
 * [전사 코딩 표준 준수: Lombok 배제, 순수 자바 엔티티 및 Builder 패턴 구현]
 */
@Entity
@Table(name = "tb_pricing_policy")
public class PricingPolicy {

    /**
     * 요금 정책 식별자 (PK)
     */
    @Id
    @Column(name = "POLICY_ID", length = 32, nullable = false)
    private String policyId;

    /**
     * 요금 정책 명칭
     */
    @Column(name = "POLICY_NAME", length = 64, nullable = false)
    private String policyName;

    /**
     * 정책 유형 (TOU, FLAT, DYNAMIC)
     */
    @Column(name = "POLICY_TYPE", length = 32, nullable = false)
    private String policyType = "TOU";

    /**
     * 기본 요금 (접속료, 원)
     */
    @Column(name = "BASIC_FEE", precision = 12, scale = 2, nullable = false)
    private BigDecimal basicFee = BigDecimal.ZERO.setScale(2);

    /**
     * 무료 충전 제공 시간 (분 단위)
     */
    @Column(name = "FREE_MINUTES", nullable = false)
    private int freeMinutes = 0;

    /**
     * 완충 후 미출차 점유 페널티 단가 (원/분)
     */
    @Column(name = "PENALTY_PER_MINUTE", precision = 12, scale = 2, nullable = false)
    private BigDecimal penaltyPerMinute = BigDecimal.ZERO.setScale(2);

    /**
     * 활성 여부
     */
    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean isActive = true;

    /**
     * 정책 상세 설명
     */
    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt;

    @UpdateTimestamp
    @Column(name = "UPDT_DT", nullable = false)
    private LocalDateTime updtDt;

    protected PricingPolicy() {}

    public PricingPolicy(String policyId, String policyName, String policyType, BigDecimal basicFee,
            int freeMinutes, BigDecimal penaltyPerMinute, boolean isActive,
            String description, LocalDateTime regDt, LocalDateTime updtDt) {
        this.policyId = policyId;
        this.policyName = policyName;
        this.policyType = policyType != null ? policyType : "TOU";
        this.basicFee = basicFee != null ? basicFee : BigDecimal.ZERO.setScale(2);
        this.freeMinutes = freeMinutes;
        this.penaltyPerMinute =
                penaltyPerMinute != null ? penaltyPerMinute : BigDecimal.ZERO.setScale(2);
        this.isActive = isActive;
        this.description = description;
        this.regDt = regDt;
        this.updtDt = updtDt;
    }

    public String getPolicyId() {
        return policyId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public String getPolicyType() {
        return policyType;
    }

    public BigDecimal getBasicFee() {
        return basicFee;
    }

    public int getFreeMinutes() {
        return freeMinutes;
    }

    public BigDecimal getPenaltyPerMinute() {
        return penaltyPerMinute;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getRegDt() {
        return regDt;
    }

    public LocalDateTime getUpdtDt() {
        return updtDt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String policyId;
        private String policyName;
        private String policyType = "TOU";
        private BigDecimal basicFee = BigDecimal.ZERO.setScale(2);
        private int freeMinutes = 0;
        private BigDecimal penaltyPerMinute = BigDecimal.ZERO.setScale(2);
        private boolean isActive = true;
        private String description;
        private LocalDateTime regDt;
        private LocalDateTime updtDt;

        public Builder policyId(String policyId) {
            this.policyId = policyId;
            return this;
        }

        public Builder policyName(String policyName) {
            this.policyName = policyName;
            return this;
        }

        public Builder policyType(String policyType) {
            this.policyType = policyType;
            return this;
        }

        public Builder basicFee(BigDecimal basicFee) {
            this.basicFee = basicFee;
            return this;
        }

        public Builder freeMinutes(int freeMinutes) {
            this.freeMinutes = freeMinutes;
            return this;
        }

        public Builder penaltyPerMinute(BigDecimal penaltyPerMinute) {
            this.penaltyPerMinute = penaltyPerMinute;
            return this;
        }

        public Builder isActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder regDt(LocalDateTime regDt) {
            this.regDt = regDt;
            return this;
        }

        public Builder updtDt(LocalDateTime updtDt) {
            this.updtDt = updtDt;
            return this;
        }

        public PricingPolicy build() {
            return new PricingPolicy(policyId, policyName, policyType, basicFee, freeMinutes,
                    penaltyPerMinute, isActive, description, regDt, updtDt);
        }
    }
}
