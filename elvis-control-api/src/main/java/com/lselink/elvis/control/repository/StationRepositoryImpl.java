package com.lselink.elvis.control.repository;

import com.lselink.elvis.control.entity.StationEntity;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
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

import static com.lselink.elvis.control.entity.QCorpEntity.corpEntity;
import static com.lselink.elvis.control.entity.QStationEntity.stationEntity;

/**
 * 충전소 마스터 QueryDSL 커스텀 리포지토리 구현체
 */
@Repository
public class StationRepositoryImpl implements StationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public StationRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<StationEntity> searchStations(String corpId, String keyword, Pageable pageable) {
        BooleanExpression[] predicates = new BooleanExpression[]{
                eqCorpId(corpId),
                containsKeyword(keyword)
        };

        List<StationEntity> content = queryFactory
                .selectFrom(stationEntity)
                .leftJoin(corpEntity).on(stationEntity.corpId.eq(corpEntity.corpId))
                .where(predicates)
                .orderBy(resolveOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(stationEntity.count())
                .from(stationEntity)
                .leftJoin(corpEntity).on(stationEntity.corpId.eq(corpEntity.corpId))
                .where(predicates);

        return PageableExecutionUtils.getPage(content, pageable, () -> {
            Long total = countQuery.fetchOne();
            return total != null ? total : 0L;
        });
    }

    private BooleanExpression eqCorpId(String corpId) {
        if (!StringUtils.hasText(corpId) || "ALL".equalsIgnoreCase(corpId.trim())) {
            return null;
        }
        return stationEntity.corpId.eq(corpId.trim());
    }

    private BooleanExpression containsKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String trimmed = keyword.trim();
        return stationEntity.name.containsIgnoreCase(trimmed)
                .or(stationEntity.stId.containsIgnoreCase(trimmed))
                .or(corpEntity.corpName.containsIgnoreCase(trimmed))
                .or(corpEntity.shortName.containsIgnoreCase(trimmed));
    }

    private OrderSpecifier<?>[] resolveOrderSpecifiers(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return new OrderSpecifier<?>[]{stationEntity.stId.asc()};
        }

        List<OrderSpecifier<?>> orders = new ArrayList<>();
        for (Sort.Order sortOrder : sort) {
            Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;
            switch (sortOrder.getProperty()) {
                case "stId", "id" -> orders.add(new OrderSpecifier<>(direction, stationEntity.stId));
                case "name" -> orders.add(new OrderSpecifier<>(direction, stationEntity.name));
                case "corpId" -> orders.add(new OrderSpecifier<>(direction, stationEntity.corpId));
                case "chargerCount" -> orders.add(new OrderSpecifier<>(direction, stationEntity.chargerCount));
                default -> { }
            }
        }

        if (orders.isEmpty()) {
            orders.add(stationEntity.stId.asc());
        }
        return orders.toArray(new OrderSpecifier<?>[0]);
    }
}
