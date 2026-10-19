package com.lselink.elvis.connect.sync.repository;

import com.lselink.elvis.connect.sync.domain.PricingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 요금 정책 마스터 리포지토리 (Sprint 21: S21-SYNC-0080-CO)
 */
@Repository
public interface PricingPolicyRepository extends JpaRepository<PricingPolicy, String> {

    /**
     * 활성화된 정책 식별자로 요금 정책 조회
     */
    Optional<PricingPolicy> findByPolicyIdAndIsActiveTrue(String policyId);
}
