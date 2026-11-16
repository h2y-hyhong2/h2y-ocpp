package com.lselink.elvis.control.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "connector_status")
@IdClass(ConnectorStatusId.class)
public class ConnectorStatusEntity {

    @Id
    @Column(name = "charge_box_id", nullable = false, length = 50)
    private String chargeBoxId;

    @Id
    @Column(name = "connector_id", nullable = false)
    private Integer connectorId = 1;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "power_kw", precision = 8, scale = 2)
    private BigDecimal powerKw = BigDecimal.ZERO;

    @Column(name = "voltage_v", precision = 8, scale = 2)
    private BigDecimal voltageV = BigDecimal.ZERO;

    @Column(name = "current_a", precision = 8, scale = 2)
    private BigDecimal currentA = BigDecimal.ZERO;

    @Column(name = "soc_percent")
    private Integer socPercent = 0;

    @Column(name = "battery_temp_c")
    private Integer batteryTempC = 25;

    @Column(name = "car_model", length = 50)
    private String carModel;

    @Column(name = "user_tag", length = 50)
    private String userTag;

    @Column(name = "accumulated_kwh", precision = 10, scale = 2)
    private BigDecimal accumulatedKwh = BigDecimal.ZERO;

    @Column(name = "charging_minutes")
    private Integer chargingMinutes = 0;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Column(name = "updt_dt")
    private LocalDateTime updtDt;

    public ConnectorStatusEntity() {
    }

    public String getChargeBoxId() { return chargeBoxId; }
    public void setChargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; }

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

    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public LocalDateTime getUpdtDt() { return updtDt; }
    public void setUpdtDt(LocalDateTime updtDt) { this.updtDt = updtDt; }
}
