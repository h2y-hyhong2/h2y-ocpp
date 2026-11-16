package com.lselink.elvis.connect.sync.repository;

import com.lselink.elvis.connect.sync.domain.DayType;
import com.lselink.elvis.connect.sync.domain.PricingTouRule;
import com.lselink.elvis.connect.sync.domain.SeasonType;

import java.util.List;

/**
 * TOU 단가 규칙 QueryDSL 커스텀 Repository 인터페이스 (AGENTS.md §5)
 */
public interface PricingTouRuleRepositoryCustom {

    /**
     * 정책 ID, 계절, 요일 조건에 부합하는 시간대별 단가 규칙 목록 조회 (시작시각 오름차순)
     */
    List<PricingTouRule> findRulesByCondition(String policyId, SeasonType seasonType, DayType dayType);

    /**
     * 특정 정책의 모든 TOU 규칙 목록 조회 (계절/요일/시작시각 오름차순)
     */
    List<PricingTouRule> findAllByPolicyId(String policyId);
}
