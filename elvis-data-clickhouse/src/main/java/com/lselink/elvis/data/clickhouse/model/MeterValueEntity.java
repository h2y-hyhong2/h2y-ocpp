package com.lselink.elvis.data.clickhouse.model;

import java.time.LocalDateTime;

/**
 * ClickHouse 시계열 계측값 저장용 엔티티
 * 대상 테이블: elvis_poc.meter_value_history
 */
public class MeterValueEntity {

    private LocalDateTime eventTime;
    private String chargeBoxId;
    private Integer connectorId;
    private Long transactionId;
    private Float voltage;
    private Float currentImport;
    private Float powerActive;
    private Long energyActive;
    private Integer soc;
    private String rawJson;

    public MeterValueEntity() {
    }

    public MeterValueEntity(
            LocalDateTime eventTime,
            String chargeBoxId,
            Integer connectorId,
            Long transactionId,
            Float voltage,
            Float currentImport,
            Float powerActive,
            Long energyActive,
            Integer soc,
            String rawJson) {
        this.eventTime = eventTime;
        this.chargeBoxId = chargeBoxId;
        this.connectorId = connectorId;
        this.transactionId = transactionId;
        this.voltage = voltage;
        this.currentImport = currentImport;
        this.powerActive = powerActive;
        this.energyActive = energyActive;
        this.soc = soc;
        this.rawJson = rawJson;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
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

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Float getVoltage() {
        return voltage;
    }

    public void setVoltage(Float voltage) {
        this.voltage = voltage;
    }

    public Float getCurrentImport() {
        return currentImport;
    }

    public void setCurrentImport(Float currentImport) {
        this.currentImport = currentImport;
    }

    public Float getPowerActive() {
        return powerActive;
    }

    public void setPowerActive(Float powerActive) {
        this.powerActive = powerActive;
    }

    public Long getEnergyActive() {
        return energyActive;
    }

    public void setEnergyActive(Long energyActive) {
        this.energyActive = energyActive;
    }

    public Integer getSoc() {
        return soc;
    }

    public void setSoc(Integer soc) {
        this.soc = soc;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    @Override
    public String toString() {
        return "MeterValueEntity{" +
                "eventTime=" + eventTime +
                ", chargeBoxId='" + chargeBoxId + '\'' +
                ", connectorId=" + connectorId +
                ", transactionId=" + transactionId +
                ", voltage=" + voltage +
                ", currentImport=" + currentImport +
                ", powerActive=" + powerActive +
                ", energyActive=" + energyActive +
                ", soc=" + soc +
                '}';
    }
}
