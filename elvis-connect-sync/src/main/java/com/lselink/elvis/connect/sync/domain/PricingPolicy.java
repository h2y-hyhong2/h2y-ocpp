package com.lselink.elvis.connect.sync.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 충전 요금 정책 마스터 엔티티 (Sprint 21: S21-SYNC-0080-CO)
 */
@Entity
@Table(name = "tb_pricing_policy")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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
    @Builder.Default
    private String policyType = "TOU";

    /**
     * 기본 요금 (접속료, 원)
     */
    @Column(name = "BASIC_FEE", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal basicFee = BigDecimal.ZERO.setScale(2);

    /**
     * 무료 충전 제공 시간 (분 단위)
     */
    @Column(name = "FREE_MINUTES", nullable = false)
    @Builder.Default
    private int freeMinutes = 0;

    /**
     * 완충 후 미출차 점유 페널티 단가 (원/분)
     */
    @Column(name = "PENALTY_PER_MINUTE", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal penaltyPerMinute = BigDecimal.ZERO.setScale(2);

    /**
     * 활성 여부
     */
    @Column(name = "IS_ACTIVE", nullable = false)
    @Builder.Default
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
}
