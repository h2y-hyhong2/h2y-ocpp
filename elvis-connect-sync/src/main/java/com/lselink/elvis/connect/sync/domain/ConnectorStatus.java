package com.lselink.elvis.connect.sync.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * 충전기 커넥터 실시간 상태 엔티티
 * <p>
 * StatusNotification / Heartbeat 수신 시 charge_box_id + connector_id 복합키로
 * 단건 Upsert 갱신되는 최신 상태 레코드.
 * </p>
 */
@Entity
@Table(name = "tb_connector_status",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_charge_box_connector",
                columnNames = {"charge_box_id", "connector_id"}
        ))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate // 변경된 컬럼만 UPDATE (불필요한 컬럼 갱신 방지)
public class ConnectorStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 충전기 식별자 (ChargeBoxId) */
    @Column(name = "charge_box_id", length = 64, nullable = false)
    private String chargeBoxId;

    /** 커넥터 번호 (0=전체기, 1~N=개별 커넥터) */
    @Column(name = "connector_id", nullable = false)
    private int connectorId;

    /** OCPP ChargePointStatus */
    @Column(name = "status", length = 32, nullable = false)
    private String status;

    /** OCPP ErrorCode */
    @Column(name = "error_code", length = 32)
    private String errorCode;

    /** StatusNotification.vendorId */
    @Column(name = "vendor_id", length = 128)
    private String vendorId;

    /** StatusNotification.info */
    @Column(name = "info", length = 255)
    private String info;

    /** 최근 Heartbeat 수신 일시 */
    @Column(name = "heartbeat_at")
    private LocalDateTime heartbeatAt;

    /** 최근 StatusNotification 수신 일시 */
    @Column(name = "status_at")
    private LocalDateTime statusAt;

    /** 낙관적 락 버전 컬럼 */
    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
