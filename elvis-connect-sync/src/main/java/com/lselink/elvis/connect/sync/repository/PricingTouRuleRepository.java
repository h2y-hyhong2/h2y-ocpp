package com.lselink.elvis.connect.sync.repository;

import com.lselink.elvis.connect.sync.domain.DayType;
import com.lselink.elvis.connect.sync.domain.PricingTouRule;
import com.lselink.elvis.connect.sync.domain.SeasonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TOU 단가 규칙 리포지토리 (Sprint 21: S21-SYNC-0080-CO)
 */
@Repository
public interface PricingTouRuleRepository extends JpaRepository<PricingTouRule, Long> {

    /**
     * 정책 ID, 계절, 요일 조건에 부합하는 시간대별 단가 규칙 목록 조회 (시작시각 오름차순)
     */
    List<PricingTouRule> findByPolicyIdAndSeasonTypeAndDayTypeOrderByStartHourAsc(
            String policyId,
            SeasonType seasonType,
            DayType dayType
    );

    /**
     * 특정 정책의 모든 TOU 규칙 목록 조회
     */
    List<PricingTouRule> findByPolicyIdOrderBySeasonTypeAscDayTypeAscStartHourAsc(String policyId);
}
