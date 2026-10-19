package com.lselink.elvis.data.clickhouse.repository;

import com.lselink.elvis.data.clickhouse.model.MeterValueEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

/**
 * ClickHouse JDBC Batch Bulk 적재 리포지토리
 */
@Repository
@ConditionalOnProperty(name = "elvis.clickhouse.enabled", havingValue = "true", matchIfMissing = true)
public class ClickHouseBulkRepository {

    private static final Logger log = LoggerFactory.getLogger(ClickHouseBulkRepository.class);

    private static final String INSERT_SQL = """
            INSERT INTO elvis_poc.meter_value_history
            (event_time, charge_box_id, connector_id, transaction_id, voltage, current_import, power_active, energy_active, soc, raw_json)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final DataSource dataSource;

    public ClickHouseBulkRepository(@Qualifier("clickHouseDataSource") DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 계측값 엔티티 리스트를 ClickHouse에 일괄 배치 적재
     *
     * @param entities 적재 대상 엔티티 리스트
     * @return 적재 성공 건수
     */
    public int insertBatch(List<MeterValueEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }

        long startTime = System.currentTimeMillis();
        int targetSize = entities.size();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {

            for (MeterValueEntity entity : entities) {
                // 1. event_time (DateTime64)
                Timestamp eventTimestamp = entity.getEventTime() != null
                        ? Timestamp.valueOf(entity.getEventTime())
                        : new Timestamp(System.currentTimeMillis());
                pstmt.setTimestamp(1, eventTimestamp);

                // 2. charge_box_id (String)
                String chargeBoxId = entity.getChargeBoxId() != null ? entity.getChargeBoxId() : "";
                pstmt.setString(2, chargeBoxId);

                // 3. connector_id (UInt8)
                int connectorId = entity.getConnectorId() != null ? entity.getConnectorId() : 1;
                pstmt.setInt(3, connectorId);

                // 4. transaction_id (Nullable(UInt32))
                Long transactionId = entity.getTransactionId();
                if (transactionId != null) {
                    pstmt.setLong(4, transactionId);
                } else {
                    pstmt.setNull(4, Types.BIGINT);
                }

                // 5. voltage (Float32)
                float voltage = entity.getVoltage() != null ? entity.getVoltage() : 0.0f;
                pstmt.setFloat(5, voltage);

                // 6. current_import (Float32)
                float currentImport = entity.getCurrentImport() != null ? entity.getCurrentImport() : 0.0f;
                pstmt.setFloat(6, currentImport);

                // 7. power_active (Float32)
                float powerActive = entity.getPowerActive() != null ? entity.getPowerActive() : 0.0f;
                pstmt.setFloat(7, powerActive);

                // 8. energy_active (UInt64)
                long energyActive = entity.getEnergyActive() != null ? entity.getEnergyActive() : 0L;
                pstmt.setLong(8, energyActive);

                // 9. soc (UInt8)
                int soc = entity.getSoc() != null ? entity.getSoc() : 0;
                pstmt.setInt(9, soc);

                // 10. raw_json (String)
                String rawJson = entity.getRawJson() != null ? entity.getRawJson() : "";
                pstmt.setString(10, rawJson);

                pstmt.addBatch();
            }

            int[] results = pstmt.executeBatch();
            long elapsedMs = System.currentTimeMillis() - startTime;
            int insertedCount = results.length;

            log.info("[ClickHouse] 벌크 적재 완료: {}건 실행 (소요시간: {}ms)", insertedCount, elapsedMs);
            return insertedCount;

        } catch (SQLException ex) {
            long failedElapsed = System.currentTimeMillis() - startTime;
            log.error("[ClickHouse] 벌크 적재 실패 (대상 {}건, 소요 {}ms): SQLState={}, ErrorCode={}, Message={}",
                    targetSize, failedElapsed, ex.getSQLState(), ex.getErrorCode(), ex.getMessage(), ex);
            throw new RuntimeException("ClickHouse 벌크 적재 중 SQL 예외 발생", ex);
        }
    }
}
