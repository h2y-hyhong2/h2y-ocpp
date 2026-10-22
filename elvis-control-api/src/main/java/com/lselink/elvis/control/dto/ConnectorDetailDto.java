package com.lselink.elvis.control.dto;

import java.math.BigDecimal;

public class ConnectorDetailDto {
    private Integer connectorId;
    private String status;
    private BigDecimal powerKw;
    private BigDecimal voltageV;
    private BigDecimal currentA;
    private Integer socPercent;
    private Integer batteryTempC;
    private String carModel;
    private String userTag;
    private BigDecimal accumulatedKwh;
    private Integer chargingMinutes;
    private String lastHeartbeat;

    public ConnectorDetailDto() {
    }

    public ConnectorDetailDto(Integer connectorId, String status, BigDecimal powerKw,
                              BigDecimal voltageV, BigDecimal currentA, Integer socPercent,
                              Integer batteryTempC, String carModel, String userTag,
                              BigDecimal accumulatedKwh, Integer chargingMinutes, String lastHeartbeat) {
        this.connectorId = connectorId;
        this.status = status;
        this.powerKw = powerKw;
        this.voltageV = voltageV;
        this.currentA = currentA;
        this.socPercent = socPercent;
        this.batteryTempC = batteryTempC;
        this.carModel = carModel;
        this.userTag = userTag;
        this.accumulatedKwh = accumulatedKwh;
        this.chargingMinutes = chargingMinutes;
        this.lastHeartbeat = lastHeartbeat;
    }

    public Integer getConnectorId() { return connectorId; }
    public void setConnectorId(Integer connectorId) { this.connectorId = connectorId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getPowerKw() { return powerKw; }
    public void setPowerKw(BigDecimal powerKw) { this.powerKw = powerKw; }

    public BigDecimal getVoltageV() { return voltageV; }
    public void setVoltageV(BigDecimal voltageV) { this.voltageV = voltageV; }

    public BigDecimal getCurrentA() { return currentA; }
    public void setCurrentA(BigDecimal currentA) { this.currentA = currentA; }

    public Integer getSocPercent() { return socPercent; }
    public void setSocPercent(Integer socPercent) { this.socPercent = socPercent; }

    public Integer getBatteryTempC() { return batteryTempC; }
    public void setBatteryTempC(Integer batteryTempC) { this.batteryTempC = batteryTempC; }

    public String getCarModel() { return carModel; }
    public void setCarModel(String carModel) { this.carModel = carModel; }

    public String getUserTag() { return userTag; }
    public void setUserTag(String userTag) { this.userTag = userTag; }

    public BigDecimal getAccumulatedKwh() { return accumulatedKwh; }
    public void setAccumulatedKwh(BigDecimal accumulatedKwh) { this.accumulatedKwh = accumulatedKwh; }

    public Integer getChargingMinutes() { return chargingMinutes; }
    public void setChargingMinutes(Integer chargingMinutes) { this.chargingMinutes = chargingMinutes; }

    public String getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(String lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer connectorId;
        private String status;
        private BigDecimal powerKw;
        private BigDecimal voltageV;
        private BigDecimal currentA;
        private Integer socPercent;
        private Integer batteryTempC;
        private String carModel;
        private String userTag;
        private BigDecimal accumulatedKwh;
        private Integer chargingMinutes;
        private String lastHeartbeat;

        public Builder connectorId(Integer connectorId) { this.connectorId = connectorId; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder powerKw(BigDecimal powerKw) { this.powerKw = powerKw; return this; }
        public Builder voltageV(BigDecimal voltageV) { this.voltageV = voltageV; return this; }
        public Builder currentA(BigDecimal currentA) { this.currentA = currentA; return this; }
        public Builder socPercent(Integer socPercent) { this.socPercent = socPercent; return this; }
        public Builder batteryTempC(Integer batteryTempC) { this.batteryTempC = batteryTempC; return this; }
        public Builder carModel(String carModel) { this.carModel = carModel; return this; }
        public Builder userTag(String userTag) { this.userTag = userTag; return this; }
        public Builder accumulatedKwh(BigDecimal accumulatedKwh) { this.accumulatedKwh = accumulatedKwh; return this; }
        public Builder chargingMinutes(Integer chargingMinutes) { this.chargingMinutes = chargingMinutes; return this; }
        public Builder lastHeartbeat(String lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; return this; }

        public ConnectorDetailDto build() {
            return new ConnectorDetailDto(connectorId, status, powerKw, voltageV, currentA, socPercent,
                    batteryTempC, carModel, userTag, accumulatedKwh, chargingMinutes, lastHeartbeat);
        }
    }
}
