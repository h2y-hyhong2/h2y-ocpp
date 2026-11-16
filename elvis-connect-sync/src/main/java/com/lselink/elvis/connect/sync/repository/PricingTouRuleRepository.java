package com.lselink.elvis.connect.sync.repository;

import com.lselink.elvis.connect.sync.domain.PricingTouRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * TOU 단가 규칙 리포지토리 (Sprint 21: S21-SYNC-0080-CO)
 */
@Repository
public interface PricingTouRuleRepository
        extends JpaRepository<PricingTouRule, Long>, PricingTouRuleRepositoryCustom {
}
