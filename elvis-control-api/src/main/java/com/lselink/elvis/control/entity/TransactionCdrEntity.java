package com.lselink.elvis.control.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_transaction_cdr")
public class TransactionCdrEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_ID")
    private Long transactionId;

    @Column(name = "CHARGE_BOX_ID", nullable = false, length = 50)
    private String chargeBoxId;

    @Column(name = "CONNECTOR_ID", nullable = false)
    private Integer connectorId;

    @Column(name = "USER_TAG", nullable = false, length = 50)
    private String userTag;

    @Column(name = "START_TIME", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "STOP_TIME")
    private LocalDateTime stopTime;

    @Column(name = "METER_START_WH")
    private Long meterStartWh;

    @Column(name = "METER_STOP_WH")
    private Long meterStopWh;

    @Column(name = "TOTAL_KWH", precision = 10, scale = 2)
    private BigDecimal totalKwh;

    @Column(name = "UNIT_PRICE", precision = 8, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "TOTAL_AMOUNT", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "PAYMENT_STATUS", length = 20)
    private String paymentStatus;

    @Column(name = "STOP_REASON", length = 50)
    private String stopReason;

    @Column(name = "REG_DT")
    private LocalDateTime regDt;

    public TransactionCdrEntity() {
    }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public String getChargeBoxId() { return chargeBoxId; }
    public void setChargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; }

    public Integer getConnectorId() { return connectorId; }
    public void setConnectorId(Integer connectorId) { this.connectorId = connectorId; }

    public String getUserTag() { return userTag; }
    public void setUserTag(String userTag) { this.userTag = userTag; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getStopTime() { return stopTime; }
    public void setStopTime(LocalDateTime stopTime) { this.stopTime = stopTime; }

    public Long getMeterStartWh() { return meterStartWh; }
    public void setMeterStartWh(Long meterStartWh) { this.meterStartWh = meterStartWh; }

    public Long getMeterStopWh() { return meterStopWh; }
    public void setMeterStopWh(Long meterStopWh) { this.meterStopWh = meterStopWh; }

    public BigDecimal getTotalKwh() { return totalKwh; }
    public void setTotalKwh(BigDecimal totalKwh) { this.totalKwh = totalKwh; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getStopReason() { return stopReason; }
    public void setStopReason(String stopReason) { this.stopReason = stopReason; }

    public LocalDateTime getRegDt() { return regDt; }
    public void setRegDt(LocalDateTime regDt) { this.regDt = regDt; }
}
