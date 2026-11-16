package com.lselink.elvis.control.repository;

import com.lselink.elvis.control.dto.CdrSearchCondition;
import com.lselink.elvis.control.dto.CdrSummaryDto;
import com.lselink.elvis.control.entity.TransactionCdrEntity;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.lselink.elvis.control.entity.QChargerEntity.chargerEntity;
import static com.lselink.elvis.control.entity.QStationEntity.stationEntity;
import static com.lselink.elvis.control.entity.QTransactionCdrEntity.transactionCdrEntity;

/**
 * 충전 거래 이력(CDR) QueryDSL 커스텀 리포지토리 구현체
 */
@Repository
public class TransactionCdrRepositoryImpl implements TransactionCdrRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public TransactionCdrRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<TransactionCdrEntity> searchCdr(CdrSearchCondition condition, Pageable pageable) {
        BooleanExpression[] predicates = buildPredicates(condition);

        List<TransactionCdrEntity> content = queryFactory
                .selectFrom(transactionCdrEntity)
                .leftJoin(chargerEntity).on(transactionCdrEntity.chargeBoxId.eq(chargerEntity.chargeBoxId))
                .leftJoin(stationEntity).on(chargerEntity.stId.eq(stationEntity.stId))
                .where(predicates)
                .orderBy(resolveOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(transactionCdrEntity.count())
                .from(transactionCdrEntity)
                .leftJoin(chargerEntity).on(transactionCdrEntity.chargeBoxId.eq(chargerEntity.chargeBoxId))
                .leftJoin(stationEntity).on(chargerEntity.stId.eq(stationEntity.stId))
                .where(predicates);

        return PageableExecutionUtils.getPage(content, pageable, () -> {
            Long total = countQuery.fetchOne();
            return total != null ? total : 0L;
        });
    }

    @Override
    public CdrSummaryDto getCdrSummary(CdrSearchCondition condition) {
        BooleanExpression[] predicates = buildPredicates(condition);

        Tuple agg = queryFactory
                .select(
                        transactionCdrEntity.count(),
                        transactionCdrEntity.totalKwh.sum().coalesce(BigDecimal.ZERO),
                        transactionCdrEntity.totalAmount.sum().coalesce(BigDecimal.ZERO)
                )
                .from(transactionCdrEntity)
                .leftJoin(chargerEntity).on(transactionCdrEntity.chargeBoxId.eq(chargerEntity.chargeBoxId))
                .leftJoin(stationEntity).on(chargerEntity.stId.eq(stationEntity.stId))
                .where(predicates)
                .fetchOne();

        long totalCount = 0L;
        BigDecimal totalKwh = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (agg != null) {
            Long countVal = agg.get(0, Long.class);
            BigDecimal kwhVal = agg.get(1, BigDecimal.class);
            BigDecimal amountVal = agg.get(2, BigDecimal.class);

            totalCount = countVal != null ? countVal : 0L;
            totalKwh = kwhVal != null ? kwhVal : BigDecimal.ZERO;
            totalAmount = amountVal != null ? amountVal : BigDecimal.ZERO;
        }

        Double avgSeconds = queryFactory
                .select(
                        Expressions.numberTemplate(
                                Double.class,
                                "timestampdiff(SECOND, {0}, {1})",
                                transactionCdrEntity.startTime,
                                transactionCdrEntity.stopTime
                        ).avg()
                )
                .from(transactionCdrEntity)
                .leftJoin(chargerEntity).on(transactionCdrEntity.chargeBoxId.eq(chargerEntity.chargeBoxId))
                .leftJoin(stationEntity).on(chargerEntity.stId.eq(stationEntity.stId))
                .where(predicates)
                .where(
                        transactionCdrEntity.stopTime.isNotNull(),
                        transactionCdrEntity.stopTime.goe(transactionCdrEntity.startTime)
                )
                .fetchOne();

        double avgMinutes = 0.0;
        if (avgSeconds != null && avgSeconds > 0.0) {
            avgMinutes = Math.round((avgSeconds / 60.0) * 10.0) / 10.0;
        }

        return CdrSummaryDto.builder()
                .totalCount(totalCount)
                .totalKwh(totalKwh.setScale(2, RoundingMode.HALF_UP))
                .totalAmount(totalAmount.setScale(0, RoundingMode.HALF_UP))
                .avgMinutes(avgMinutes)
                .build();
    }

    @Override
    public List<TransactionCdrEntity> findAllByCondition(CdrSearchCondition condition) {
        BooleanExpression[] predicates = buildPredicates(condition);

        return queryFactory
                .selectFrom(transactionCdrEntity)
                .leftJoin(chargerEntity).on(transactionCdrEntity.chargeBoxId.eq(chargerEntity.chargeBoxId))
                .leftJoin(stationEntity).on(chargerEntity.stId.eq(stationEntity.stId))
                .where(predicates)
                .orderBy(transactionCdrEntity.startTime.desc())
                .fetch();
    }

    private BooleanExpression[] buildPredicates(CdrSearchCondition condition) {
        if (condition == null) {
            return new BooleanExpression[0];
        }
        return new BooleanExpression[]{
                eqChargeBoxId(condition.getChargeBoxId()),
                eqStationId(condition.getStId()),
                containsUserTag(condition.getUserTag()),
                eqPaymentStatus(condition.getPaymentStatus()),
                betweenStartTime(condition.getParsedStartDate(), condition.getParsedEndDate()),
                containsKeyword(condition.getKeyword())
        };
    }

    private BooleanExpression eqChargeBoxId(String chargeBoxId) {
        if (!StringUtils.hasText(chargeBoxId) || "ALL".equalsIgnoreCase(chargeBoxId.trim())) {
            return null;
        }
        return transactionCdrEntity.chargeBoxId.eq(chargeBoxId.trim());
    }

    private BooleanExpression eqStationId(String stId) {
        if (!StringUtils.hasText(stId) || "ALL".equalsIgnoreCase(stId.trim())) {
            return null;
        }
        return chargerEntity.stId.eq(stId.trim());
    }

    private BooleanExpression containsUserTag(String userTag) {
        if (!StringUtils.hasText(userTag)) {
            return null;
        }
        return transactionCdrEntity.userTag.containsIgnoreCase(userTag.trim());
    }

    private BooleanExpression eqPaymentStatus(String paymentStatus) {
        if (!StringUtils.hasText(paymentStatus) || "ALL".equalsIgnoreCase(paymentStatus.trim())) {
            return null;
        }
        return transactionCdrEntity.paymentStatus.eq(paymentStatus.trim());
    }

    private BooleanExpression betweenStartTime(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null) {
            return transactionCdrEntity.startTime.between(startDate, endDate);
        }
        if (startDate != null) {
            return transactionCdrEntity.startTime.goe(startDate);
        }
        if (endDate != null) {
            return transactionCdrEntity.startTime.loe(endDate);
        }
        return null;
    }

    private BooleanExpression containsKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String trimmed = keyword.trim();
        return transactionCdrEntity.chargeBoxId.containsIgnoreCase(trimmed)
                .or(transactionCdrEntity.userTag.containsIgnoreCase(trimmed))
                .or(transactionCdrEntity.stopReason.containsIgnoreCase(trimmed))
                .or(stationEntity.name.containsIgnoreCase(trimmed))
                .or(chargerEntity.stId.containsIgnoreCase(trimmed));
    }

    private OrderSpecifier<?>[] resolveOrderSpecifiers(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return new OrderSpecifier<?>[]{transactionCdrEntity.startTime.desc()};
        }

        List<OrderSpecifier<?>> orders = new ArrayList<>();
        for (Sort.Order sortOrder : sort) {
            Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;
            switch (sortOrder.getProperty()) {
                case "transactionId" -> orders.add(new OrderSpecifier<>(direction, transactionCdrEntity.transactionId));
                case "chargeBoxId" -> orders.add(new OrderSpecifier<>(direction, transactionCdrEntity.chargeBoxId));
                case "connectorId" -> orders.add(new OrderSpecifier<>(direction, transactionCdrEntity.connectorId));
                case "userTag" -> orders.add(new OrderSpecifier<>(direction, transactionCdrEntity.userTag));
                case "startTime" -> orders.add(new OrderSpecifier<>(direction, transactionCdrEntity.startTime));
                case "stopTime" -> orders.add(new OrderSpecifier<>(direction, transactionCdrEntity.stopTime));
                case "totalKwh", "kwh" -> orders.add(new OrderSpecifier<>(direction, transactionCdrEntity.totalKwh));
                case "unitPrice" -> orders.add(new OrderSpecifier<>(direction, transactionCdrEntity.unitPrice));
                case "totalAmount" -> orders.add(new OrderSpecifier<>(direction, transactionCdrEntity.totalAmount));
                case "paymentStatus" -> orders.add(new OrderSpecifier<>(direction, transactionCdrEntity.paymentStatus));
                default -> { }
            }
        }

        if (orders.isEmpty()) {
            orders.add(transactionCdrEntity.startTime.desc());
        }
        return orders.toArray(new OrderSpecifier<?>[0]);
    }
}
