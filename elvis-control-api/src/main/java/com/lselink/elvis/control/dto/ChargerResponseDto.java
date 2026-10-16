package com.lselink.elvis.control.dto;

import java.math.BigDecimal;

public class ChargerResponseDto {
    private String id;
    private String chargeBoxId;
    private String stId;
    private String stationName;
    private String corpId;
    private String corpName;
    private String corpShortName;
    private String cpId;
    private Integer connectorId;
    private String status;
    private String spec;
    private BigDecimal powerKw;
    private BigDecimal voltageV;
    private BigDecimal currentA;
    private Integer socPercent;
    private Integer batteryTempC;
    private String vendor;
    private String model;
    private String carModel;
    private String userTag;
    private String protocol;
    private String lastHeartbeat;
    private BigDecimal accumulatedKwh;
    private Integer chargingMinutes;

    public ChargerResponseDto() {
    }

    public ChargerResponseDto(String id, String chargeBoxId, String stId, String stationName, String corpId,
                              String corpName, String corpShortName, String cpId, Integer connectorId,
                              String status, String spec, BigDecimal powerKw, BigDecimal voltageV,
                              BigDecimal currentA, Integer socPercent, Integer batteryTempC, String vendor,
                              String model, String carModel, String userTag, String protocol,
                              String lastHeartbeat, BigDecimal accumulatedKwh, Integer chargingMinutes) {
        this.id = id;
        this.chargeBoxId = chargeBoxId;
        this.stId = stId;
        this.stationName = stationName;
        this.corpId = corpId;
        this.corpName = corpName;
        this.corpShortName = corpShortName;
        this.cpId = cpId;
        this.connectorId = connectorId;
        this.status = status;
        this.spec = spec;
        this.powerKw = powerKw;
        this.voltageV = voltageV;
        this.currentA = currentA;
        this.socPercent = socPercent;
        this.batteryTempC = batteryTempC;
        this.vendor = vendor;
        this.model = model;
        this.carModel = carModel;
        this.userTag = userTag;
        this.protocol = protocol;
        this.lastHeartbeat = lastHeartbeat;
        this.accumulatedKwh = accumulatedKwh;
        this.chargingMinutes = chargingMinutes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getChargeBoxId() { return chargeBoxId; }
    public void setChargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; }

    public String getStId() { return stId; }
    public void setStId(String stId) { this.stId = stId; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public String getCorpId() { return corpId; }
    public void setCorpId(String corpId) { this.corpId = corpId; }

    public String getCorpName() { return corpName; }
    public void setCorpName(String corpName) { this.corpName = corpName; }

    public String getCorpShortName() { return corpShortName; }
    public void setCorpShortName(String corpShortName) { this.corpShortName = corpShortName; }

    public String getCpId() { return cpId; }
    public void setCpId(String cpId) { this.cpId = cpId; }

    public Integer getConnectorId() { return connectorId; }
    public void setConnectorId(Integer connectorId) { this.connectorId = connectorId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }

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

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getCarModel() { return carModel; }
    public void setCarModel(String carModel) { this.carModel = carModel; }

    public String getUserTag() { return userTag; }
    public void setUserTag(String userTag) { this.userTag = userTag; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(String lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public BigDecimal getAccumulatedKwh() { return accumulatedKwh; }
    public void setAccumulatedKwh(BigDecimal accumulatedKwh) { this.accumulatedKwh = accumulatedKwh; }

    public Integer getChargingMinutes() { return chargingMinutes; }
    public void setChargingMinutes(Integer chargingMinutes) { this.chargingMinutes = chargingMinutes; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String chargeBoxId;
        private String stId;
        private String stationName;
        private String corpId;
        private String corpName;
        private String corpShortName;
        private String cpId;
        private Integer connectorId;
        private String status;
        private String spec;
        private BigDecimal powerKw;
        private BigDecimal voltageV;
        private BigDecimal currentA;
        private Integer socPercent;
        private Integer batteryTempC;
        private String vendor;
        private String model;
        private String carModel;
        private String userTag;
        private String protocol;
        private String lastHeartbeat;
        private BigDecimal accumulatedKwh;
        private Integer chargingMinutes;

        public Builder id(String id) { this.id = id; return this; }
        public Builder chargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; return this; }
        public Builder stId(String stId) { this.stId = stId; return this; }
        public Builder stationName(String stationName) { this.stationName = stationName; return this; }
        public Builder corpId(String corpId) { this.corpId = corpId; return this; }
        public Builder corpName(String corpName) { this.corpName = corpName; return this; }
        public Builder corpShortName(String corpShortName) { this.corpShortName = corpShortName; return this; }
        public Builder cpId(String cpId) { this.cpId = cpId; return this; }
        public Builder connectorId(Integer connectorId) { this.connectorId = connectorId; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder spec(String spec) { this.spec = spec; return this; }
        public Builder powerKw(BigDecimal powerKw) { this.powerKw = powerKw; return this; }
        public Builder voltageV(BigDecimal voltageV) { this.voltageV = voltageV; return this; }
        public Builder currentA(BigDecimal currentA) { this.currentA = currentA; return this; }
        public Builder socPercent(Integer socPercent) { this.socPercent = socPercent; return this; }
        public Builder batteryTempC(Integer batteryTempC) { this.batteryTempC = batteryTempC; return this; }
        public Builder vendor(String vendor) { this.vendor = vendor; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder carModel(String carModel) { this.carModel = carModel; return this; }
        public Builder userTag(String userTag) { this.userTag = userTag; return this; }
        public Builder protocol(String protocol) { this.protocol = protocol; return this; }
        public Builder lastHeartbeat(String lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; return this; }
        public Builder accumulatedKwh(BigDecimal accumulatedKwh) { this.accumulatedKwh = accumulatedKwh; return this; }
        public Builder chargingMinutes(Integer chargingMinutes) { this.chargingMinutes = chargingMinutes; return this; }

        public ChargerResponseDto build() {
            return new ChargerResponseDto(id, chargeBoxId, stId, stationName, corpId, corpName, corpShortName, cpId,
                    connectorId, status, spec, powerKw, voltageV, currentA, socPercent, batteryTempC, vendor, model,
                    carModel, userTag, protocol, lastHeartbeat, accumulatedKwh, chargingMinutes);
        }
    }
}
