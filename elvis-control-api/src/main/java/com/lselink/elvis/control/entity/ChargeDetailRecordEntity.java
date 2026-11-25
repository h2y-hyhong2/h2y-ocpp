package com.lselink.elvis.control.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 충전 과금 원장 읽기 전용 엔티티 (elvis-connect-sync 적재 테이블: charge_detail_record)
 */
@Entity
@Table(name = "charge_detail_record")
public class ChargeDetailRecordEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "cdr_no", nullable = false, length = 64)
    private String cdrNo;

    @Column(name = "charge_box_id", nullable = false, length = 50)
    private String chargeBoxId;

    @Column(name = "connector_id", nullable = false)
    private Integer connectorId;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "id_tag", nullable = false, length = 64)
    private String idTag;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "stop_time", nullable = false)
    private LocalDateTime stopTime;

    @Column(name = "charging_seconds")
    private Long chargingSeconds;

    @Column(name = "start_meter_wh")
    private Long startMeterWh;

    @Column(name = "stop_meter_wh")
    private Long stopMeterWh;

    @Column(name = "total_energy_kwh", precision = 10, scale = 3)
    private BigDecimal totalEnergyKwh;

    @Column(name = "policy_id", length = 32)
    private String policyId;

    @Column(name = "energy_fee", precision = 12, scale = 2)
    private BigDecimal energyFee;

    @Column(name = "basic_fee", precision = 12, scale = 2)
    private BigDecimal basicFee;

    @Column(name = "discount_fee", precision = 12, scale = 2)
    private BigDecimal discountFee;

    @Column(name = "vat", precision = 12, scale = 2)
    private BigDecimal vat;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_status", length = 32)
    private String paymentStatus;

    @Column(name = "reg_dt")
    private LocalDateTime regDt;

    protected ChargeDetailRecordEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getCdrNo() {
        return cdrNo;
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

    public Long getSessionId() {
        return sessionId;
    }

    public String getIdTag() {
        return idTag;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getStopTime() {
        return stopTime;
    }

    public Long getChargingSeconds() {
        return chargingSeconds;
    }

    public Long getStartMeterWh() {
        return startMeterWh;
    }

    public Long getStopMeterWh() {
        return stopMeterWh;
    }

    public BigDecimal getTotalEnergyKwh() {
        return totalEnergyKwh;
    }

    public String getPolicyId() {
        return policyId;
    }

    public BigDecimal getEnergyFee() {
        return energyFee;
    }

    public BigDecimal getBasicFee() {
        return basicFee;
    }

    public BigDecimal getDiscountFee() {
        return discountFee;
    }

    public BigDecimal getVat() {
        return vat;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public LocalDateTime getRegDt() {
        return regDt;
    }
}
