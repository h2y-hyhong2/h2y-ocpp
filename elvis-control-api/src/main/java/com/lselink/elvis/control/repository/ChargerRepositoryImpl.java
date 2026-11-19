package com.lselink.elvis.control.repository;

import com.lselink.elvis.control.entity.ChargerEntity;
import com.lselink.elvis.control.entity.QConnectorStatusEntity;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.lselink.elvis.control.entity.QChargerEntity.chargerEntity;
import static com.lselink.elvis.control.entity.QCorpEntity.corpEntity;
import static com.lselink.elvis.control.entity.QStationEntity.stationEntity;

/**
 * 충전기 마스터 QueryDSL 커스텀 리포지토리 구현체
 */
@Repository
public class ChargerRepositoryImpl implements ChargerRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public ChargerRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<ChargerEntity> searchChargers(String stId, String status, String keyword, Pageable pageable) {
        BooleanExpression[] predicates = new BooleanExpression[]{
                eqStationId(stId),
                eqStatus(status),
                containsKeyword(keyword)
        };

        List<ChargerEntity> content = queryFactory
                .selectFrom(chargerEntity)
                .leftJoin(stationEntity).on(chargerEntity.stId.eq(stationEntity.stId))
                .leftJoin(corpEntity).on(stationEntity.corpId.eq(corpEntity.corpId))
                .where(predicates)
                .orderBy(resolveOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(chargerEntity.count())
                .from(chargerEntity)
                .leftJoin(stationEntity).on(chargerEntity.stId.eq(stationEntity.stId))
                .leftJoin(corpEntity).on(stationEntity.corpId.eq(corpEntity.corpId))
                .where(predicates);

        return PageableExecutionUtils.getPage(content, pageable, () -> {
            Long total = countQuery.fetchOne();
            return total != null ? total : 0L;
        });
    }

    private BooleanExpression eqStationId(String stId) {
        if (!StringUtils.hasText(stId) || "ALL".equalsIgnoreCase(stId.trim())) {
            return null;
        }
        return chargerEntity.stId.eq(stId.trim());
    }

    private BooleanExpression eqStatus(String status) {
        if (!StringUtils.hasText(status) || "ALL".equalsIgnoreCase(status.trim())) {
            return null;
        }

        String trimmedStatus = status.trim();
        QConnectorStatusEntity channelConn = new QConnectorStatusEntity("channelConn");
        QConnectorStatusEntity anyConn = new QConnectorStatusEntity("anyConn");

        BooleanExpression hasChannelConn = JPAExpressions.selectOne()
                .from(anyConn)
                .where(
                        anyConn.chargeBoxId.eq(chargerEntity.chargeBoxId),
                        anyConn.connectorId.gt(0)
                )
                .exists();

        BooleanExpression matchesChannel = JPAExpressions.selectOne()
                .from(channelConn)
                .where(
                        channelConn.chargeBoxId.eq(chargerEntity.chargeBoxId),
                        channelConn.connectorId.gt(0),
                        channelConn.status.eq(trimmedStatus)
                )
                .exists();

        BooleanExpression matchesFallbackBody = hasChannelConn.not().and(
                JPAExpressions.selectOne()
                        .from(channelConn)
                        .where(
                                channelConn.chargeBoxId.eq(chargerEntity.chargeBoxId),
                                channelConn.status.eq(trimmedStatus)
                        )
                        .exists()
        );

        BooleanExpression matchesAny = matchesChannel.or(matchesFallbackBody);

        if ("연결안됨".equals(trimmedStatus)) {
            BooleanExpression noConnectors = JPAExpressions.selectOne()
                    .from(anyConn)
                    .where(anyConn.chargeBoxId.eq(chargerEntity.chargeBoxId))
                    .notExists();
            return matchesAny.or(noConnectors);
        }

        return matchesAny;
    }

    private BooleanExpression containsKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String trimmed = keyword.trim();
        return chargerEntity.chargeBoxId.containsIgnoreCase(trimmed)
                .or(stationEntity.name.containsIgnoreCase(trimmed))
                .or(corpEntity.corpName.containsIgnoreCase(trimmed))
                .or(chargerEntity.vendor.containsIgnoreCase(trimmed))
                .or(chargerEntity.model.containsIgnoreCase(trimmed));
    }

    private OrderSpecifier<?>[] resolveOrderSpecifiers(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return new OrderSpecifier<?>[]{chargerEntity.chargeBoxId.asc()};
        }

        List<OrderSpecifier<?>> orders = new ArrayList<>();
        for (Sort.Order sortOrder : sort) {
            Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;
            switch (sortOrder.getProperty()) {
                case "chargeBoxId" -> orders.add(new OrderSpecifier<>(direction, chargerEntity.chargeBoxId));
                case "stId" -> orders.add(new OrderSpecifier<>(direction, chargerEntity.stId));
                case "cpId" -> orders.add(new OrderSpecifier<>(direction, chargerEntity.cpId));
                case "vendor" -> orders.add(new OrderSpecifier<>(direction, chargerEntity.vendor));
                case "model" -> orders.add(new OrderSpecifier<>(direction, chargerEntity.model));
                default -> { }
            }
        }

        if (orders.isEmpty()) {
            orders.add(chargerEntity.chargeBoxId.asc());
        }
        return orders.toArray(new OrderSpecifier<?>[0]);
    }
}
