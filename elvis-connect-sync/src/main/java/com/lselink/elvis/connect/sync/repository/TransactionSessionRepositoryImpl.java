package com.lselink.elvis.connect.sync.repository;

import com.lselink.elvis.connect.sync.domain.QTransactionSession;
import com.lselink.elvis.connect.sync.domain.SessionStatus;
import com.lselink.elvis.connect.sync.domain.TransactionSession;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Optional;

/**
 * 충전 거래 세션 QueryDSL 커스텀 Repository 구현체 (AGENTS.md §5)
 */
@Repository
public class TransactionSessionRepositoryImpl implements TransactionSessionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public TransactionSessionRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Optional<TransactionSession> findLatestActiveSession(
            String chargeBoxId, int connectorId, Collection<SessionStatus> statuses) {
        QTransactionSession session = QTransactionSession.transactionSession;
        TransactionSession result = queryFactory
                .selectFrom(session)
                .where(
                        eqChargeBoxId(chargeBoxId),
                        eqConnectorId(connectorId),
                        inStatuses(statuses)
                )
                .orderBy(session.startTime.desc())
                .fetchFirst();
        return Optional.ofNullable(result);
    }

    private BooleanExpression eqChargeBoxId(String chargeBoxId) {
        if (!StringUtils.hasText(chargeBoxId)) {
            return null;
        }
        return QTransactionSession.transactionSession.chargeBoxId.eq(chargeBoxId);
    }

    private BooleanExpression eqConnectorId(int connectorId) {
        return QTransactionSession.transactionSession.connectorId.eq(connectorId);
    }

    private BooleanExpression inStatuses(Collection<SessionStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        return QTransactionSession.transactionSession.status.in(statuses);
    }
}
