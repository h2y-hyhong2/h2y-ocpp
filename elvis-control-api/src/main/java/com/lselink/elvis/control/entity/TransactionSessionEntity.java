package com.lselink.elvis.control.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 충전 거래 세션 읽기 전용 엔티티 (elvis-connect-sync 적재 테이블: transaction_session)
 */
@Entity
@Table(name = "transaction_session")
public class TransactionSessionEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "charge_box_id", nullable = false, length = 50)
    private String chargeBoxId;

    @Column(name = "connector_id", nullable = false)
    private Integer connectorId;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "id_tag", nullable = false, length = 64)
    private String idTag;

    @Column(name = "parent_id_tag", length = 64)
    private String parentIdTag;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "meter_start", nullable = false)
    private Long meterStart;

    @Column(name = "meter_stop")
    private Long meterStop;

    @Column(name = "total_energy_kwh", precision = 10, scale = 3)
    private BigDecimal totalEnergyKwh;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "stop_time")
    private LocalDateTime stopTime;

    @Column(name = "charging_minutes")
    private Integer chargingMinutes;

    @Column(name = "stop_reason", length = 64)
    private String stopReason;

    @Column(name = "reg_dt")
    private LocalDateTime regDt;

    protected TransactionSessionEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public Integer getConnectorId() {
        return connectorId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public String getIdTag() {
        return idTag;
    }

    public String getParentIdTag() {
        return parentIdTag;
    }

    public String getStatus() {
        return status;
    }

    public Long getMeterStart() {
        return meterStart;
    }

    public Long getMeterStop() {
        return meterStop;
    }

    public BigDecimal getTotalEnergyKwh() {
        return totalEnergyKwh;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getStopTime() {
        return stopTime;
    }

    public Integer getChargingMinutes() {
        return chargingMinutes;
    }

    public String getStopReason() {
        return stopReason;
    }

    public LocalDateTime getRegDt() {
        return regDt;
    }
}
