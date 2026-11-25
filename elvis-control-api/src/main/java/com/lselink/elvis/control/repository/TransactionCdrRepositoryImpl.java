package com.lselink.elvis.control.repository;

import com.lselink.elvis.control.dto.CdrSearchCondition;
import com.lselink.elvis.control.dto.CdrSummaryDto;
import com.lselink.elvis.control.entity.ChargeDetailRecordEntity;
import com.lselink.elvis.control.entity.TransactionCdrEntity;
import com.lselink.elvis.control.entity.TransactionSessionEntity;
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

import static com.lselink.elvis.control.entity.QChargeDetailRecordEntity.chargeDetailRecordEntity;
import static com.lselink.elvis.control.entity.QChargerEntity.chargerEntity;
import static com.lselink.elvis.control.entity.QStationEntity.stationEntity;
import static com.lselink.elvis.control.entity.QTransactionSessionEntity.transactionSessionEntity;

/**
 * 충전 거래 이력(CDR) QueryDSL 커스텀 리포지토리 구현체
 * <p>
 * elvis-connect-sync가 실제 영속화하는 {@code transaction_session}과
 * {@code charge_detail_record}를 LEFT OUTER JOIN하여 진행중 세션과 정산 완료 과금 원장을 통합 조회합니다.
 * </p>
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

        List<Tuple> tuples = queryFactory
                .select(transactionSessionEntity, chargeDetailRecordEntity)
                .from(transactionSessionEntity)
                .leftJoin(chargeDetailRecordEntity).on(
                        chargeDetailRecordEntity.chargeBoxId.eq(transactionSessionEntity.chargeBoxId)
                                .and(chargeDetailRecordEntity.transactionId.eq(transactionSessionEntity.transactionId))
                )
                .leftJoin(chargerEntity).on(transactionSessionEntity.chargeBoxId.eq(chargerEntity.chargeBoxId))
                .leftJoin(stationEntity).on(chargerEntity.stId.eq(stationEntity.stId))
                .where(predicates)
                .orderBy(resolveOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<TransactionCdrEntity> content = new ArrayList<>(tuples.size());
        for (Tuple tuple : tuples) {
            TransactionSessionEntity session = tuple.get(transactionSessionEntity);
            ChargeDetailRecordEntity cdr = tuple.get(chargeDetailRecordEntity);
            if (session != null) {
                content.add(toMergedCdrEntity(session, cdr));
            }
        }

        JPAQuery<Long> countQuery = queryFactory
                .select(transactionSessionEntity.count())
                .from(transactionSessionEntity)
                .leftJoin(chargeDetailRecordEntity).on(
                        chargeDetailRecordEntity.chargeBoxId.eq(transactionSessionEntity.chargeBoxId)
                                .and(chargeDetailRecordEntity.transactionId.eq(transactionSessionEntity.transactionId))
                )
                .leftJoin(chargerEntity).on(transactionSessionEntity.chargeBoxId.eq(chargerEntity.chargeBoxId))
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
                        transactionSessionEntity.count(),
                        chargeDetailRecordEntity.totalEnergyKwh
                                .coalesce(transactionSessionEntity.totalEnergyKwh)
                                .coalesce(BigDecimal.ZERO)
                                .sum()
                                .coalesce(BigDecimal.ZERO),
                        chargeDetailRecordEntity.totalAmount
                                .coalesce(BigDecimal.ZERO)
                                .sum()
                                .coalesce(BigDecimal.ZERO)
                )
                .from(transactionSessionEntity)
                .leftJoin(chargeDetailRecordEntity).on(
                        chargeDetailRecordEntity.chargeBoxId.eq(transactionSessionEntity.chargeBoxId)
                                .and(chargeDetailRecordEntity.transactionId.eq(transactionSessionEntity.transactionId))
                )
                .leftJoin(chargerEntity).on(transactionSessionEntity.chargeBoxId.eq(chargerEntity.chargeBoxId))
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
                                transactionSessionEntity.startTime,
                                transactionSessionEntity.stopTime
                        ).avg()
                )
                .from(transactionSessionEntity)
                .leftJoin(chargeDetailRecordEntity).on(
                        chargeDetailRecordEntity.chargeBoxId.eq(transactionSessionEntity.chargeBoxId)
                                .and(chargeDetailRecordEntity.transactionId.eq(transactionSessionEntity.transactionId))
                )
                .leftJoin(chargerEntity).on(transactionSessionEntity.chargeBoxId.eq(chargerEntity.chargeBoxId))
                .leftJoin(stationEntity).on(chargerEntity.stId.eq(stationEntity.stId))
                .where(predicates)
                .where(
                        transactionSessionEntity.stopTime.isNotNull(),
                        transactionSessionEntity.stopTime.goe(transactionSessionEntity.startTime)
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

        List<Tuple> tuples = queryFactory
                .select(transactionSessionEntity, chargeDetailRecordEntity)
                .from(transactionSessionEntity)
                .leftJoin(chargeDetailRecordEntity).on(
                        chargeDetailRecordEntity.chargeBoxId.eq(transactionSessionEntity.chargeBoxId)
                                .and(chargeDetailRecordEntity.transactionId.eq(transactionSessionEntity.transactionId))
                )
                .leftJoin(chargerEntity).on(transactionSessionEntity.chargeBoxId.eq(chargerEntity.chargeBoxId))
                .leftJoin(stationEntity).on(chargerEntity.stId.eq(stationEntity.stId))
                .where(predicates)
                .orderBy(transactionSessionEntity.startTime.desc(), transactionSessionEntity.transactionId.desc())
                .fetch();

        List<TransactionCdrEntity> result = new ArrayList<>(tuples.size());
        for (Tuple tuple : tuples) {
            TransactionSessionEntity session = tuple.get(transactionSessionEntity);
            ChargeDetailRecordEntity cdr = tuple.get(chargeDetailRecordEntity);
            if (session != null) {
                result.add(toMergedCdrEntity(session, cdr));
            }
        }
        return result;
    }

    private TransactionCdrEntity toMergedCdrEntity(TransactionSessionEntity session, ChargeDetailRecordEntity cdr) {
        TransactionCdrEntity row = new TransactionCdrEntity();
        row.setTransactionId(session.getTransactionId());
        row.setChargeBoxId(session.getChargeBoxId());
        row.setConnectorId(session.getConnectorId());
        row.setUserTag(session.getIdTag());
        row.setStartTime(session.getStartTime());
        row.setStopTime(cdr != null && cdr.getStopTime() != null ? cdr.getStopTime() : session.getStopTime());
        row.setMeterStartWh(cdr != null && cdr.getStartMeterWh() != null ? cdr.getStartMeterWh() : session.getMeterStart());
        row.setMeterStopWh(cdr != null && cdr.getStopMeterWh() != null ? cdr.getStopMeterWh() : session.getMeterStop());

        BigDecimal kwh = cdr != null && cdr.getTotalEnergyKwh() != null
                ? cdr.getTotalEnergyKwh()
                : (session.getTotalEnergyKwh() != null ? session.getTotalEnergyKwh() : BigDecimal.ZERO);
        BigDecimal totalAmount = cdr != null && cdr.getTotalAmount() != null
                ? cdr.getTotalAmount()
                : BigDecimal.ZERO;

        BigDecimal unitPrice = BigDecimal.ZERO;
        if (cdr != null && kwh.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal baseFee = cdr.getEnergyFee() != null && cdr.getEnergyFee().compareTo(BigDecimal.ZERO) > 0
                    ? cdr.getEnergyFee()
                    : totalAmount;
            unitPrice = baseFee.divide(kwh, 1, RoundingMode.HALF_UP);
        }

        row.setTotalKwh(kwh.setScale(2, RoundingMode.HALF_UP));
        row.setUnitPrice(unitPrice);
        row.setTotalAmount(totalAmount.setScale(0, RoundingMode.HALF_UP));
        row.setPaymentStatus(resolvePaymentStatus(session, cdr));
        row.setStopReason(session.getStopReason());
        row.setRegDt(cdr != null && cdr.getRegDt() != null ? cdr.getRegDt() : session.getRegDt());
        return row;
    }

    private String resolvePaymentStatus(TransactionSessionEntity session, ChargeDetailRecordEntity cdr) {
        if (cdr != null && StringUtils.hasText(cdr.getPaymentStatus())) {
            return cdr.getPaymentStatus().trim();
        }
        if (StringUtils.hasText(session.getStatus())) {
            String st = session.getStatus().trim().toUpperCase();
            if ("CHARGING".equals(st) || "STARTING".equals(st)) {
                return "CHARGING";
            }
            if ("ABORTED".equals(st)) {
                return "ABORTED";
            }
            if ("STOPPED".equals(st) || "COMPLETED".equals(st)) {
                return "PENDING";
            }
            return st;
        }
        return session.getStopTime() == null ? "CHARGING" : "PENDING";
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
        return transactionSessionEntity.chargeBoxId.eq(chargeBoxId.trim());
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
        return transactionSessionEntity.idTag.containsIgnoreCase(userTag.trim());
    }

    private BooleanExpression eqPaymentStatus(String paymentStatus) {
        if (!StringUtils.hasText(paymentStatus) || "ALL".equalsIgnoreCase(paymentStatus.trim())) {
            return null;
        }
        String trimmed = paymentStatus.trim().toUpperCase();
        if ("CHARGING".equals(trimmed)) {
            return chargeDetailRecordEntity.id.isNull()
                    .and(transactionSessionEntity.status.equalsIgnoreCase("CHARGING"));
        }
        if ("ABORTED".equals(trimmed)) {
            return chargeDetailRecordEntity.id.isNull()
                    .and(transactionSessionEntity.status.equalsIgnoreCase("ABORTED"));
        }
        return chargeDetailRecordEntity.paymentStatus.equalsIgnoreCase(trimmed);
    }

    private BooleanExpression betweenStartTime(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null) {
            return transactionSessionEntity.startTime.between(startDate, endDate);
        }
        if (startDate != null) {
            return transactionSessionEntity.startTime.goe(startDate);
        }
        if (endDate != null) {
            return transactionSessionEntity.startTime.loe(endDate);
        }
        return null;
    }

    private BooleanExpression containsKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String trimmed = keyword.trim();
        BooleanExpression expr = transactionSessionEntity.chargeBoxId.containsIgnoreCase(trimmed)
                .or(transactionSessionEntity.idTag.containsIgnoreCase(trimmed))
                .or(transactionSessionEntity.stopReason.containsIgnoreCase(trimmed))
                .or(stationEntity.name.containsIgnoreCase(trimmed))
                .or(chargerEntity.stId.containsIgnoreCase(trimmed))
                .or(chargeDetailRecordEntity.cdrNo.containsIgnoreCase(trimmed));
        try {
            Long parsedTxId = Long.parseLong(trimmed.replace("#", ""));
            expr = expr.or(transactionSessionEntity.transactionId.eq(parsedTxId));
        } catch (NumberFormatException ignored) {
        }
        return expr;
    }

    private OrderSpecifier<?>[] resolveOrderSpecifiers(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return new OrderSpecifier<?>[]{
                    transactionSessionEntity.startTime.desc(),
                    transactionSessionEntity.transactionId.desc()
            };
        }

        List<OrderSpecifier<?>> orders = new ArrayList<>();
        for (Sort.Order sortOrder : sort) {
            Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;
            switch (sortOrder.getProperty()) {
                case "transactionId" -> orders.add(new OrderSpecifier<>(direction, transactionSessionEntity.transactionId));
                case "chargeBoxId" -> orders.add(new OrderSpecifier<>(direction, transactionSessionEntity.chargeBoxId));
                case "connectorId" -> orders.add(new OrderSpecifier<>(direction, transactionSessionEntity.connectorId));
                case "userTag" -> orders.add(new OrderSpecifier<>(direction, transactionSessionEntity.idTag));
                case "startTime" -> orders.add(new OrderSpecifier<>(direction, transactionSessionEntity.startTime));
                case "stopTime" -> orders.add(new OrderSpecifier<>(direction, transactionSessionEntity.stopTime));
                case "totalKwh", "kwh" -> orders.add(new OrderSpecifier<>(direction, chargeDetailRecordEntity.totalEnergyKwh));
                case "totalAmount" -> orders.add(new OrderSpecifier<>(direction, chargeDetailRecordEntity.totalAmount));
                case "paymentStatus" -> orders.add(new OrderSpecifier<>(direction, chargeDetailRecordEntity.paymentStatus));
                default -> { }
            }
        }

        if (orders.isEmpty()) {
            orders.add(transactionSessionEntity.startTime.desc());
            orders.add(transactionSessionEntity.transactionId.desc());
        }
        return orders.toArray(new OrderSpecifier<?>[0]);
    }
}
