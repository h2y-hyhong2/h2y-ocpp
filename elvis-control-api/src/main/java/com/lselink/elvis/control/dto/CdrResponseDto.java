package com.lselink.elvis.control.dto;

import java.math.BigDecimal;

/**
 * 충전 거래 이력(CDR) 그리드 조회 응답 DTO
 */
public class CdrResponseDto {

    private Long transactionId;
    private String chargeBoxId;
    private String stationId;
    private String stationName;
    private Integer connectorId;
    private String userTag;
    private String startTime;
    private String stopTime;
    private BigDecimal kwh;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private String stopReason;
    private Double durationMinutes;

    public CdrResponseDto() {
    }

    public CdrResponseDto(Long transactionId, String chargeBoxId, String stationId, String stationName,
                          Integer connectorId, String userTag, String startTime, String stopTime,
                          BigDecimal kwh, BigDecimal unitPrice, BigDecimal totalAmount,
                          String paymentStatus, String stopReason, Double durationMinutes) {
        this.transactionId = transactionId;
        this.chargeBoxId = chargeBoxId;
        this.stationId = stationId;
        this.stationName = stationName;
        this.connectorId = connectorId;
        this.userTag = userTag;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.kwh = kwh;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.paymentStatus = paymentStatus;
        this.stopReason = stopReason;
        this.durationMinutes = durationMinutes;
    }

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

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
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

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getStopTime() {
        return stopTime;
    }

    public void setStopTime(String stopTime) {
        this.stopTime = stopTime;
    }

    public BigDecimal getKwh() {
        return kwh;
    }

    public void setKwh(BigDecimal kwh) {
        this.kwh = kwh;
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

    public Double getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Double durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long transactionId;
        private String chargeBoxId;
        private String stationId;
        private String stationName;
        private Integer connectorId;
        private String userTag;
        private String startTime;
        private String stopTime;
        private BigDecimal kwh;
        private BigDecimal unitPrice;
        private BigDecimal totalAmount;
        private String paymentStatus;
        private String stopReason;
        private Double durationMinutes;

        public Builder transactionId(Long transactionId) { this.transactionId = transactionId; return this; }
        public Builder chargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; return this; }
        public Builder stationId(String stationId) { this.stationId = stationId; return this; }
        public Builder stationName(String stationName) { this.stationName = stationName; return this; }
        public Builder connectorId(Integer connectorId) { this.connectorId = connectorId; return this; }
        public Builder userTag(String userTag) { this.userTag = userTag; return this; }
        public Builder startTime(String startTime) { this.startTime = startTime; return this; }
        public Builder stopTime(String stopTime) { this.stopTime = stopTime; return this; }
        public Builder kwh(BigDecimal kwh) { this.kwh = kwh; return this; }
        public Builder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public Builder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public Builder paymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; return this; }
        public Builder stopReason(String stopReason) { this.stopReason = stopReason; return this; }
        public Builder durationMinutes(Double durationMinutes) { this.durationMinutes = durationMinutes; return this; }

        public CdrResponseDto build() {
            return new CdrResponseDto(transactionId, chargeBoxId, stationId, stationName, connectorId, userTag,
                    startTime, stopTime, kwh, unitPrice, totalAmount, paymentStatus, stopReason, durationMinutes);
        }
    }
}
