package com.lselink.elvis.connect.sync.repository;

import com.lselink.elvis.connect.sync.domain.FaultHistory;
import com.lselink.elvis.connect.sync.domain.QFaultHistory;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 충전기 장애 이력 QueryDSL 커스텀 Repository 구현체 (AGENTS.md §5)
 */
@Repository
public class FaultHistoryRepositoryImpl implements FaultHistoryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public FaultHistoryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Optional<FaultHistory> findLatestUnresolvedFault(String chargeBoxId, int connectorId) {
        QFaultHistory fault = QFaultHistory.faultHistory;
        FaultHistory result = queryFactory
                .selectFrom(fault)
                .where(
                        eqChargeBoxId(chargeBoxId),
                        eqConnectorId(connectorId),
                        eqIsResolved("N")
                )
                .orderBy(fault.faultId.desc())
                .fetchFirst();
        return Optional.ofNullable(result);
    }

    @Override
    public boolean existsActiveFault(String chargeBoxId, int connectorId) {
        QFaultHistory fault = QFaultHistory.faultHistory;
        Integer found = queryFactory
                .selectOne()
                .from(fault)
                .where(
                        eqChargeBoxId(chargeBoxId),
                        eqConnectorId(connectorId),
                        eqIsResolved("N")
                )
                .fetchFirst();
        return found != null;
    }

    private BooleanExpression eqChargeBoxId(String chargeBoxId) {
        if (!StringUtils.hasText(chargeBoxId)) {
            return null;
        }
        return QFaultHistory.faultHistory.chargeBoxId.eq(chargeBoxId);
    }

    private BooleanExpression eqConnectorId(int connectorId) {
        return QFaultHistory.faultHistory.connectorId.eq(connectorId);
    }

    private BooleanExpression eqIsResolved(String isResolved) {
        if (!StringUtils.hasText(isResolved)) {
            return null;
        }
        return QFaultHistory.faultHistory.isResolved.eq(isResolved);
    }
}
