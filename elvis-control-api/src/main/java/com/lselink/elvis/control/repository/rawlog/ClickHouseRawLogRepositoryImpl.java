package com.lselink.elvis.control.repository.rawlog;

import com.lselink.elvis.control.config.ClickHouseAvailableCondition;
import com.lselink.elvis.control.dto.RawLogResponseDto;
import com.lselink.elvis.control.dto.RawLogSearchCondition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * ClickHouse 원본 통신로그 조회 Repository 구현체 (지연 인출 & Late Materialization 최적화)
 */
@Slf4j
@Repository
@Conditional(ClickHouseAvailableCondition.class)
public class ClickHouseRawLogRepositoryImpl implements RawLogRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ClickHouseRawLogRepositoryImpl(
            @Qualifier("clickHouseJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long countByCondition(RawLogSearchCondition condition) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT count() FROM elvis_poc.meter_value_history WHERE 1 = 1");

        MapSqlParameterSource params = new MapSqlParameterSource();
        appendWhereConditions(sqlBuilder, params, condition);

        String sql = sqlBuilder.toString();
        try {
            Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
            long totalCount = 0L;
            if (count != null) {
                totalCount = count;
            }
            return totalCount;
        } catch (Exception ex) {
            log.error("❌ [ClickHouse] 원본 통신로그 총 건수 조회 실패 - SQL: {}, 에러: {}", sql, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public List<RawLogResponseDto> findPagedByCondition(RawLogSearchCondition condition) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("""
            SELECT 
                event_time,
                charge_box_id,
                connector_id,
                transaction_id,
                voltage,
                current_import,
                power_active,
                energy_active,
                soc,
                raw_json
            FROM elvis_poc.meter_value_history
            WHERE 1 = 1
            """);

        MapSqlParameterSource params = new MapSqlParameterSource();
        appendWhereConditions(sqlBuilder, params, condition);

        // 복합 정렬 키 기반 최신순 정렬 및 지연 인출 페이징
        sqlBuilder.append(" ORDER BY event_time DESC, charge_box_id ASC, connector_id ASC");
        sqlBuilder.append(" LIMIT :offset, :limit");

        int offset = condition.getOffset();
        int limit = condition.getLimit();
        params.addValue("offset", offset);
        params.addValue("limit", limit);

        String sql = sqlBuilder.toString();
        try {
            List<RawLogResponseDto> resultList = jdbcTemplate.query(sql, params, new RawLogRowMapper());
            if (resultList == null) {
                return Collections.emptyList();
            }
            return resultList;
        } catch (Exception ex) {
            log.error("❌ [ClickHouse] 원본 통신로그 페이징 조회 실패 - SQL: {}, 에러: {}", sql, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public List<RawLogResponseDto> findLatestByChargeBoxId(String chargeBoxId, int limit) {
        String sql = """
            SELECT 
                event_time,
                charge_box_id,
                connector_id,
                transaction_id,
                voltage,
                current_import,
                power_active,
                energy_active,
                soc,
                raw_json
            FROM elvis_poc.meter_value_history
            WHERE charge_box_id = :chargeBoxId
            ORDER BY event_time DESC
            LIMIT :limit
            """;

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("chargeBoxId", chargeBoxId);
        params.addValue("limit", limit);

        try {
            List<RawLogResponseDto> resultList = jdbcTemplate.query(sql, params, new RawLogRowMapper());
            if (resultList == null) {
                return Collections.emptyList();
            }
            return resultList;
        } catch (Exception ex) {
            log.error("❌ [ClickHouse] 최근 통신로그 조회 실패 - chargeBoxId: {}, 에러: {}", chargeBoxId, ex.getMessage());
            throw ex;
        }
    }

    /**
     * 다차원 검색 조건절 동적 바인딩 (거버넌스 9조 Clean Code 표준)
     */
    private void appendWhereConditions(
            StringBuilder sqlBuilder, MapSqlParameterSource params, RawLogSearchCondition condition) {
        if (condition == null) {
            return;
        }

        // 1. 충전기 결합 식별자 바인딩 (8자리, 예: "00000001")
        String chargeBoxId = condition.getChargeBoxId();
        if (StringUtils.hasText(chargeBoxId)) {
            sqlBuilder.append(" AND charge_box_id = :chargeBoxId");
            params.addValue("chargeBoxId", chargeBoxId.trim());
        }

        // 2. 커넥터 번호 바인딩 (1, 2)
        Integer connectorId = condition.getConnectorId();
        if (connectorId != null && connectorId > 0) {
            sqlBuilder.append(" AND connector_id = :connectorId");
            params.addValue("connectorId", connectorId);
        }

        // 3. 트랜잭션 번호 바인딩
        Long transactionId = condition.getTransactionId();
        if (transactionId != null && transactionId > 0) {
            sqlBuilder.append(" AND transaction_id = :transactionId");
            params.addValue("transactionId", transactionId);
        }

        // 4. 기간 검색 조건 바인딩
        LocalDateTime startDate = condition.getStartDate();
        if (startDate != null) {
            sqlBuilder.append(" AND event_time >= :startDate");
            params.addValue("startDate", Timestamp.valueOf(startDate));
        }

        LocalDateTime endDate = condition.getEndDate();
        if (endDate != null) {
            sqlBuilder.append(" AND event_time <= :endDate");
            params.addValue("endDate", Timestamp.valueOf(endDate));
        }
    }

    /**
     * ClickHouse RowMapper 구현 (Null 안전성 및 명시적 if-else 적용)
     */
    private static class RawLogRowMapper implements RowMapper<RawLogResponseDto> {
        @Override
        public RawLogResponseDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp timestamp = rs.getTimestamp("event_time");
            LocalDateTime eventTime = null;
            if (timestamp != null) {
                eventTime = timestamp.toLocalDateTime();
            }

            String chargeBoxId = rs.getString("charge_box_id");
            int connectorId = rs.getInt("connector_id");
            long transactionId = rs.getLong("transaction_id");

            Double voltage = getDoubleOrNull(rs, "voltage");
            Double currentImport = getDoubleOrNull(rs, "current_import");
            Double powerActive = getDoubleOrNull(rs, "power_active");
            Double energyActive = getDoubleOrNull(rs, "energy_active");
            Double soc = getDoubleOrNull(rs, "soc");
            String rawJson = rs.getString("raw_json");

            return RawLogResponseDto.builder()
                    .eventTime(eventTime)
                    .chargeBoxId(chargeBoxId)
                    .connectorId(connectorId)
                    .transactionId(transactionId)
                    .voltage(voltage)
                    .currentImport(currentImport)
                    .powerActive(powerActive)
                    .energyActive(energyActive)
                    .soc(soc)
                    .rawJson(rawJson)
                    .build();
        }

        private Double getDoubleOrNull(ResultSet rs, String columnName) throws SQLException {
            double value = rs.getDouble(columnName);
            if (rs.wasNull()) {
                return null;
            }
            return value;
        }
    }
}
