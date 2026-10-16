package com.lselink.elvis.connect.sync.domain;

import jakarta.persistence.*;
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

    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDT_DT", nullable = false)
    private LocalDateTime updatedAt;

    public ConnectorStatus() {
    }

    public ConnectorStatus(Long id, String chargeBoxId, int connectorId, String status, String errorCode,
                           String vendorId, String info, LocalDateTime heartbeatAt, LocalDateTime statusAt,
                           int version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.chargeBoxId = chargeBoxId;
        this.connectorId = connectorId;
        this.status = status;
        this.errorCode = errorCode;
        this.vendorId = vendorId;
        this.info = info;
        this.heartbeatAt = heartbeatAt;
        this.statusAt = statusAt;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getChargeBoxId() { return chargeBoxId; }
    public void setChargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; }

    public int getConnectorId() { return connectorId; }
    public void setConnectorId(int connectorId) { this.connectorId = connectorId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getVendorId() { return vendorId; }
    public void setVendorId(String vendorId) { this.vendorId = vendorId; }

    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }

    public LocalDateTime getHeartbeatAt() { return heartbeatAt; }
    public void setHeartbeatAt(LocalDateTime heartbeatAt) { this.heartbeatAt = heartbeatAt; }

    public LocalDateTime getStatusAt() { return statusAt; }
    public void setStatusAt(LocalDateTime statusAt) { this.statusAt = statusAt; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String chargeBoxId;
        private int connectorId;
        private String status;
        private String errorCode;
        private String vendorId;
        private String info;
        private LocalDateTime heartbeatAt;
        private LocalDateTime statusAt;
        private int version;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder chargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; return this; }
        public Builder connectorId(int connectorId) { this.connectorId = connectorId; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder errorCode(String errorCode) { this.errorCode = errorCode; return this; }
        public Builder vendorId(String vendorId) { this.vendorId = vendorId; return this; }
        public Builder info(String info) { this.info = info; return this; }
        public Builder heartbeatAt(LocalDateTime heartbeatAt) { this.heartbeatAt = heartbeatAt; return this; }
        public Builder statusAt(LocalDateTime statusAt) { this.statusAt = statusAt; return this; }
        public Builder version(int version) { this.version = version; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ConnectorStatus build() {
            return new ConnectorStatus(id, chargeBoxId, connectorId, status, errorCode, vendorId, info, heartbeatAt, statusAt, version, createdAt, updatedAt);
        }
    }

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
