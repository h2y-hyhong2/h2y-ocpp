package com.lselink.elvis.control.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_cdr")
public class TransactionCdrEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "charge_box_id", nullable = false, length = 50)
    private String chargeBoxId;

    @Column(name = "connector_id", nullable = false)
    private Integer connectorId;

    @Column(name = "user_tag", nullable = false, length = 50)
    private String userTag;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "stop_time")
    private LocalDateTime stopTime;

    @Column(name = "meter_start_wh")
    private Long meterStartWh;

    @Column(name = "meter_stop_wh")
    private Long meterStopWh;

    @Column(name = "total_kwh", precision = 10, scale = 2)
    private BigDecimal totalKwh;

    @Column(name = "unit_price", precision = 8, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus;

    @Column(name = "stop_reason", length = 50)
    private String stopReason;

    @Column(name = "reg_dt")
    private LocalDateTime regDt;

    public TransactionCdrEntity() {}

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public void setChargeBoxId(String chargeBoxId) {
        this.chargeBoxId = chargeBoxId;
    }

    public Integer getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(Integer connectorId) {
        this.connectorId = connectorId;
    }

    public String getUserTag() {
        return userTag;
    }

    public void setUserTag(String userTag) {
        this.userTag = userTag;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getStopTime() {
        return stopTime;
    }

    public void setStopTime(LocalDateTime stopTime) {
        this.stopTime = stopTime;
    }

    public Long getMeterStartWh() {
        return meterStartWh;
    }

    public void setMeterStartWh(Long meterStartWh) {
        this.meterStartWh = meterStartWh;
    }

    public Long getMeterStopWh() {
        return meterStopWh;
    }

    public void setMeterStopWh(Long meterStopWh) {
        this.meterStopWh = meterStopWh;
    }

    public BigDecimal getTotalKwh() {
        return totalKwh;
    }

    public void setTotalKwh(BigDecimal totalKwh) {
        this.totalKwh = totalKwh;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    public LocalDateTime getRegDt() {
        return regDt;
    }

    public void setRegDt(LocalDateTime regDt) {
        this.regDt = regDt;
    }
}
