package com.lselink.elvis.connect.sync.repository;

import com.lselink.elvis.connect.sync.domain.ConnectorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 충전기 커넥터 실시간 상태 Repository
 */
public interface ConnectorStatusRepository extends JpaRepository<ConnectorStatus, Long> {

    Optional<ConnectorStatus> findByChargeBoxIdAndConnectorId(String chargeBoxId, int connectorId);

    /**
     * StatusNotification 수신 시 상태 Upsert
     * INSERT INTO ... ON DUPLICATE KEY UPDATE 구문으로 원자적 처리
     */
    @Modifying
    @Query(value = """
            INSERT INTO connector_status
                (charge_box_id, connector_id, status, error_code, vendor_id, info, status_at, last_heartbeat, reg_dt, updt_dt)
            VALUES
                (:chargeBoxId, :connectorId, :status, :errorCode, :vendorId, :info, :statusAt, NOW(3), NOW(3), NOW(3))
            ON DUPLICATE KEY UPDATE
                status         = VALUES(status),
                error_code     = VALUES(error_code),
                vendor_id      = VALUES(vendor_id),
                info           = VALUES(info),
                status_at      = VALUES(status_at),
                power_kw       = CASE WHEN VALUES(status) IN ('Available', '연결안됨', 'Unavailable', 'Faulted', 'Maintenance') THEN 0.00 ELSE power_kw END,
                voltage_v      = CASE WHEN VALUES(status) IN ('Available', '연결안됨', 'Unavailable', 'Faulted', 'Maintenance') THEN 0.00 ELSE voltage_v END,
                current_a      = CASE WHEN VALUES(status) IN ('Available', '연결안됨', 'Unavailable', 'Faulted', 'Maintenance') THEN 0.00 ELSE current_a END,
                last_heartbeat = NOW(3),
                version        = version + 1,
                updt_dt        = NOW(3)
            """, nativeQuery = true)
    void upsertStatus(
            @Param("chargeBoxId") String chargeBoxId,
            @Param("connectorId") int connectorId,
            @Param("status") String status,
            @Param("errorCode") String errorCode,
            @Param("vendorId") String vendorId,
            @Param("info") String info,
            @Param("statusAt") LocalDateTime statusAt
    );

    /**
     * Heartbeat 수신 시 heartbeat_at 및 last_heartbeat 갱신
     */
    @Modifying
    @Query(value = """
            INSERT INTO connector_status
                (charge_box_id, connector_id, status, heartbeat_at, last_heartbeat, reg_dt, updt_dt)
            VALUES
                (:chargeBoxId, 0, 'Available', :heartbeatAt, :heartbeatAt, NOW(3), NOW(3))
            ON DUPLICATE KEY UPDATE
                status         = CASE WHEN status = '연결안됨' THEN 'Available' ELSE status END,
                heartbeat_at   = VALUES(heartbeat_at),
                last_heartbeat = VALUES(last_heartbeat),
                version        = version + 1,
                updt_dt        = NOW(3)
            """, nativeQuery = true)
    void upsertHeartbeat(
            @Param("chargeBoxId") String chargeBoxId,
            @Param("heartbeatAt") LocalDateTime heartbeatAt
    );

    /**
     * 표준 MeterValues 및 LS DataTransfer(MeterValues) 실시간 계측치 단일 문장 Upsert
     * null로 전달된 계측 항목은 기존 DB 값을 유지(COALESCE)하여 부분 계측 보고 시 데이터 유실 방지
     */
    @Modifying
    @Query(value = """
            INSERT INTO connector_status
                (charge_box_id, connector_id, status, power_kw, voltage_v, current_a,
                 soc_percent, battery_temp_c, user_tag, accumulated_kwh, charging_minutes,
                 last_heartbeat, reg_dt, updt_dt)
            VALUES
                (:chargeBoxId, :connectorId, 'Charging',
                 COALESCE(:powerKw, 0.00), COALESCE(:voltageV, 0.00), COALESCE(:currentA, 0.00),
                 COALESCE(:socPercent, 0), COALESCE(:batteryTempC, 25), :userTag,
                 COALESCE(:accumulatedKwh, 0.00), COALESCE(:chargingMinutes, 0),
                 NOW(3), NOW(3), NOW(3))
            ON DUPLICATE KEY UPDATE
                power_kw         = COALESCE(:powerKw, power_kw),
                voltage_v        = COALESCE(:voltageV, voltage_v),
                current_a        = COALESCE(:currentA, current_a),
                soc_percent      = COALESCE(:socPercent, soc_percent),
                battery_temp_c   = COALESCE(:batteryTempC, battery_temp_c),
                user_tag         = COALESCE(:userTag, user_tag),
                accumulated_kwh  = COALESCE(:accumulatedKwh, accumulated_kwh),
                charging_minutes = COALESCE(:chargingMinutes, charging_minutes),
                last_heartbeat   = NOW(3),
                version          = version + 1,
                updt_dt          = NOW(3)
            """, nativeQuery = true)
    void upsertTelemetry(
            @Param("chargeBoxId") String chargeBoxId,
            @Param("connectorId") int connectorId,
            @Param("powerKw") BigDecimal powerKw,
            @Param("voltageV") BigDecimal voltageV,
            @Param("currentA") BigDecimal currentA,
            @Param("socPercent") Integer socPercent,
            @Param("batteryTempC") Integer batteryTempC,
            @Param("userTag") String userTag,
            @Param("accumulatedKwh") BigDecimal accumulatedKwh,
            @Param("chargingMinutes") Integer chargingMinutes
    );

    /**
     * WebSocket 연결 종료 시 해당 충전기의 모든 커넥터를 '연결안됨' 상태로 전환하고 순시 계측치 초기화
     */
    @Modifying
    @Query(value = """
            UPDATE connector_status
            SET status           = '연결안됨',
                power_kw         = 0.00,
                voltage_v        = 0.00,
                current_a        = 0.00,
                soc_percent      = 0,
                charging_minutes = 0,
                version          = version + 1,
                updt_dt          = NOW(3)
            WHERE charge_box_id = :chargeBoxId
            AND status <> '연결안됨'
            """, nativeQuery = true)
    int markDisconnectedByChargeBoxId(@Param("chargeBoxId") String chargeBoxId);

    /**
     * 장시간 Heartbeat 미수신(오프라인 만료) 상태이면서 여전히 연결 상태가 남아있는 충전기 ID 목록 조회
     */
    @Query(value = """
            SELECT charge_box_id
            FROM connector_status
            GROUP BY charge_box_id
            HAVING SUM(CASE WHEN status <> '연결안됨' THEN 1 ELSE 0 END) > 0
            AND MAX(GREATEST(
                    COALESCE(last_heartbeat, TIMESTAMP('1970-01-01 00:00:00')),
                    COALESCE(heartbeat_at, TIMESTAMP('1970-01-01 00:00:00'))
                )) < :threshold
            """, nativeQuery = true)
    List<String> findStaleOnlineChargeBoxIds(@Param("threshold") LocalDateTime threshold);

    /**
     * 오프라인 만료 충전기 목록의 모든 커넥터를 '연결안됨' 상태로 일괄 전환
     */
    @Modifying
    @Query(value = """
            UPDATE connector_status
            SET status           = '연결안됨',
                power_kw         = 0.00,
                voltage_v        = 0.00,
                current_a        = 0.00,
                soc_percent      = 0,
                charging_minutes = 0,
                version          = version + 1,
                updt_dt          = NOW(3)
            WHERE charge_box_id IN (:chargeBoxIds)
            AND status <> '연결안됨'
            """, nativeQuery = true)
    int markDisconnectedByChargeBoxIds(@Param("chargeBoxIds") Collection<String> chargeBoxIds);
}
