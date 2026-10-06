package com.lselink.elvis.connect.sync.repository;

import com.lselink.elvis.connect.sync.domain.ConnectorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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
            INSERT INTO tb_connector_status
                (charge_box_id, connector_id, status, error_code, vendor_id, info, status_at, created_at, updated_at)
            VALUES
                (:chargeBoxId, :connectorId, :status, :errorCode, :vendorId, :info, :statusAt, NOW(3), NOW(3))
            ON DUPLICATE KEY UPDATE
                status     = VALUES(status),
                error_code = VALUES(error_code),
                vendor_id  = VALUES(vendor_id),
                info       = VALUES(info),
                status_at  = VALUES(status_at),
                version    = version + 1,
                updated_at = NOW(3)
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
     * Heartbeat 수신 시 heartbeat_at 갱신
     */
    @Modifying
    @Query(value = """
            INSERT INTO tb_connector_status
                (charge_box_id, connector_id, status, heartbeat_at, created_at, updated_at)
            VALUES
                (:chargeBoxId, 0, 'Available', :heartbeatAt, NOW(3), NOW(3))
            ON DUPLICATE KEY UPDATE
                heartbeat_at = VALUES(heartbeat_at),
                version      = version + 1,
                updated_at   = NOW(3)
            """, nativeQuery = true)
    void upsertHeartbeat(
            @Param("chargeBoxId") String chargeBoxId,
            @Param("heartbeatAt") LocalDateTime heartbeatAt
    );
}
