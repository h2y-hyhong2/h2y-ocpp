package com.lselink.elvis.control.repository.timeseries;

import com.lselink.elvis.control.dto.timeseries.StationPowerMetricsDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesBucketDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesSessionPointDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesSummaryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import com.lselink.elvis.control.config.ClickHouseAvailableCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * ClickHouse 시계열 집계 쿼리 Repository 구현체
 */
@Repository
@Conditional(ClickHouseAvailableCondition.class)
public class ClickHouseTimeSeriesRepositoryImpl implements ClickHouseTimeSeriesRepository {

    private static final Logger log = LoggerFactory.getLogger(ClickHouseTimeSeriesRepositoryImpl.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ClickHouseTimeSeriesRepositoryImpl(
            @Qualifier("clickHouseJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TimeSeriesBucketDto> findDownsampledSeries(
            String chargeBoxId, Integer connectorId, int intervalMinutes,
            LocalDateTime startTime, LocalDateTime endTime) {

        String sql = """
            SELECT 
                toStartOfInterval(event_time, toIntervalMinute(:intervalMinutes), 'Asia/Seoul') AS time_bucket,
                charge_box_id,
                connector_id,
                round(avgOrDefault(power_active, 0.0), 2) AS avg_power_kw,
                round(max(power_active), 2)               AS peak_power_kw,
                round(avgOrDefault(voltage, 0.0), 1)      AS avg_voltage_v,
                round(avgOrDefault(current_import, 0.0), 1) AS avg_current_a,
                max(soc)                                  AS max_soc,
                count()                                   AS sample_count
            FROM elvis_poc.meter_value_history
            WHERE charge_box_id = :chargeBoxId
              AND connector_id = :connectorId
              AND event_time >= :startTime
              AND event_time <= :endTime
            GROUP BY time_bucket, charge_box_id, connector_id
            ORDER BY time_bucket ASC
            """;

        Timestamp startTimestamp = Timestamp.valueOf(startTime);
        Timestamp endTimestamp = Timestamp.valueOf(endTime);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("intervalMinutes", intervalMinutes)
                .addValue("chargeBoxId", chargeBoxId)
                .addValue("connectorId", connectorId)
                .addValue("startTime", startTimestamp)
                .addValue("endTime", endTimestamp);

        try {
            return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
                Timestamp ts = rs.getTimestamp("time_bucket");
                LocalDateTime bucketTime = null;
                if (ts != null) {
                    bucketTime = ts.toLocalDateTime();
                }

                String cboxId = rs.getString("charge_box_id");
                int connId = rs.getInt("connector_id");
                double avgPower = rs.getDouble("avg_power_kw");
                double peakPower = rs.getDouble("peak_power_kw");
                double avgVoltage = rs.getDouble("avg_voltage_v");
                double avgCurrent = rs.getDouble("avg_current_a");
                int maxSoc = rs.getInt("max_soc");
                long sampleCount = rs.getLong("sample_count");

                return new TimeSeriesBucketDto(
                        bucketTime, cboxId, connId, avgPower, peakPower,
                        avgVoltage, avgCurrent, maxSoc, sampleCount);
            });
        } catch (Exception e) {
            log.error("[ClickHouse] 다운샘플링 집계 조회 실패 (chargeBoxId={}, 에러={})", chargeBoxId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<TimeSeriesSummaryDto> findTimeSeriesSummary(
            String chargeBoxId, Integer connectorId,
            LocalDateTime startTime, LocalDateTime endTime) {

        String sql = """
            SELECT 
                charge_box_id,
                count()                                                      AS total_samples,
                round(avgOrDefault(power_active, 0.0), 2)                   AS avg_power_kw,
                round(max(power_active), 2)                                  AS peak_power_kw,
                argMax(event_time, power_active)                             AS peak_power_time,
                round(max(current_import), 1)                                AS peak_current_a,
                round((max(energy_active) - min(energy_active)) / 1000.0, 3) AS consumed_kwh,
                min(soc)                                                     AS start_soc,
                max(soc)                                                     AS current_soc
            FROM elvis_poc.meter_value_history
            WHERE charge_box_id = :chargeBoxId
              AND connector_id = :connectorId
              AND event_time >= :startTime
              AND event_time <= :endTime
            GROUP BY charge_box_id
            """;

        Timestamp startTimestamp = Timestamp.valueOf(startTime);
        Timestamp endTimestamp = Timestamp.valueOf(endTime);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("chargeBoxId", chargeBoxId)
                .addValue("connectorId", connectorId)
                .addValue("startTime", startTimestamp)
                .addValue("endTime", endTimestamp);

        try {
            List<TimeSeriesSummaryDto> list = jdbcTemplate.query(sql, params, (rs, rowNum) -> {
                String cboxId = rs.getString("charge_box_id");
                long totalSamples = rs.getLong("total_samples");
                double avgPower = rs.getDouble("avg_power_kw");
                double peakPower = rs.getDouble("peak_power_kw");

                Timestamp peakTs = rs.getTimestamp("peak_power_time");
                LocalDateTime peakPowerTime = null;
                if (peakTs != null) {
                    peakPowerTime = peakTs.toLocalDateTime();
                }

                double peakCurrent = rs.getDouble("peak_current_a");
                double consumedKwh = rs.getDouble("consumed_kwh");
                int startSoc = rs.getInt("start_soc");
                int currentSoc = rs.getInt("current_soc");

                return new TimeSeriesSummaryDto(
                        cboxId, totalSamples, avgPower, peakPower,
                        peakPowerTime, peakCurrent, consumedKwh, startSoc, currentSoc);
            });

            if (list.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(list.get(0));
        } catch (Exception e) {
            log.error("[ClickHouse] 전력 요약 통계 조회 실패 (chargeBoxId={}, 에러={})", chargeBoxId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<TimeSeriesSessionPointDto> findSessionProfile(
            String chargeBoxId, Integer connectorId, Long transactionId, int intervalSeconds) {

        String sql = """
            SELECT 
                toStartOfInterval(event_time, toIntervalSecond(:intervalSeconds), 'Asia/Seoul') AS time_bucket,
                round(avgOrDefault(power_active, 0.0), 2)                                       AS power_kw,
                round(avgOrDefault(voltage, 0.0), 1)                                            AS voltage_v,
                round(avgOrDefault(current_import, 0.0), 1)                                     AS current_a,
                max(soc)                                                                        AS soc,
                max(energy_active)                                                              AS cumulative_wh
            FROM elvis_poc.meter_value_history
            WHERE charge_box_id = :chargeBoxId
              AND connector_id = :connectorId
              AND transaction_id = :transactionId
            GROUP BY time_bucket
            ORDER BY time_bucket ASC
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("intervalSeconds", intervalSeconds)
                .addValue("chargeBoxId", chargeBoxId)
                .addValue("connectorId", connectorId)
                .addValue("transactionId", transactionId);

        try {
            return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
                Timestamp ts = rs.getTimestamp("time_bucket");
                LocalDateTime pointTime = null;
                if (ts != null) {
                    pointTime = ts.toLocalDateTime();
                }

                double powerKw = rs.getDouble("power_kw");
                double voltageV = rs.getDouble("voltage_v");
                double currentA = rs.getDouble("current_a");
                int soc = rs.getInt("soc");
                long cumulativeWh = rs.getLong("cumulative_wh");

                return new TimeSeriesSessionPointDto(
                        pointTime, powerKw, voltageV, currentA, soc, cumulativeWh);
            });
        } catch (Exception e) {
            log.error("[ClickHouse] 충전 세션 프로파일 조회 실패 (transactionId={}, 에러={})", transactionId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<StationPowerMetricsDto> findStationPowerMetrics(String stationId, LocalDateTime startTime) {
        String sql = """
            SELECT 
                toStartOfInterval(event_time, INTERVAL 1 MINUTE, 'Asia/Seoul') AS time_bucket,
                round(sum(power_active), 2)                                    AS total_power_kw,
                round(avg(voltage), 1)                                         AS avg_voltage_v,
                count(DISTINCT charge_box_id)                                  AS active_chargers_count
            FROM elvis_poc.meter_value_history
            WHERE startsWith(charge_box_id, :stationId)
              AND event_time >= :startTime
            GROUP BY time_bucket
            ORDER BY time_bucket ASC
            """;

        Timestamp startTimestamp = Timestamp.valueOf(startTime);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("stationId", stationId)
                .addValue("startTime", startTimestamp);

        try {
            return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
                Timestamp ts = rs.getTimestamp("time_bucket");
                LocalDateTime bucketTime = null;
                if (ts != null) {
                    bucketTime = ts.toLocalDateTime();
                }

                double totalPower = rs.getDouble("total_power_kw");
                double avgVoltage = rs.getDouble("avg_voltage_v");
                int activeCount = rs.getInt("active_chargers_count");

                return new StationPowerMetricsDto(bucketTime, stationId, totalPower, avgVoltage, activeCount);
            });
        } catch (Exception e) {
            log.error("[ClickHouse] 충전소 전력 부하 합산 조회 실패 (stationId={}, 에러={})", stationId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
