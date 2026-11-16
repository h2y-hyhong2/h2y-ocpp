package com.lselink.elvis.connect.sync.repository;

import com.lselink.elvis.connect.sync.domain.DayType;
import com.lselink.elvis.connect.sync.domain.PricingTouRule;
import com.lselink.elvis.connect.sync.domain.QPricingTouRule;
import com.lselink.elvis.connect.sync.domain.SeasonType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * TOU 단가 규칙 QueryDSL 커스텀 Repository 구현체 (AGENTS.md §5)
 */
@Repository
public class PricingTouRuleRepositoryImpl implements PricingTouRuleRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public PricingTouRuleRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<PricingTouRule> findRulesByCondition(String policyId, SeasonType seasonType, DayType dayType) {
        QPricingTouRule rule = QPricingTouRule.pricingTouRule;
        return queryFactory
                .selectFrom(rule)
                .where(
                        eqPolicyId(policyId),
                        eqSeasonType(seasonType),
                        eqDayType(dayType)
                )
                .orderBy(rule.startHour.asc())
                .fetch();
    }

    @Override
    public List<PricingTouRule> findAllByPolicyId(String policyId) {
        QPricingTouRule rule = QPricingTouRule.pricingTouRule;
        return queryFactory
                .selectFrom(rule)
                .where(eqPolicyId(policyId))
                .orderBy(rule.seasonType.asc(), rule.dayType.asc(), rule.startHour.asc())
                .fetch();
    }

    private BooleanExpression eqPolicyId(String policyId) {
        if (!StringUtils.hasText(policyId)) {
            return null;
        }
        return QPricingTouRule.pricingTouRule.policyId.eq(policyId);
    }

    private BooleanExpression eqSeasonType(SeasonType seasonType) {
        if (seasonType == null) {
            return null;
        }
        return QPricingTouRule.pricingTouRule.seasonType.eq(seasonType);
    }

    private BooleanExpression eqDayType(DayType dayType) {
        if (dayType == null) {
            return null;
        }
        return QPricingTouRule.pricingTouRule.dayType.eq(dayType);
    }
}
