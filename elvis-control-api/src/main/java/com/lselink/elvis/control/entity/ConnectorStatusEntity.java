package com.lselink.elvis.control.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_connector_status")
@IdClass(ConnectorStatusId.class)
public class ConnectorStatusEntity {

    @Id
    @Column(name = "CHARGE_BOX_ID", nullable = false, length = 50)
    private String chargeBoxId;

    @Id
    @Column(name = "CONNECTOR_ID", nullable = false)
    private Integer connectorId = 1;

    @Column(name = "STATUS", nullable = false, length = 32)
    private String status;

    @Column(name = "POWER_KW", precision = 8, scale = 2)
    private BigDecimal powerKw = BigDecimal.ZERO;

    @Column(name = "VOLTAGE_V", precision = 8, scale = 2)
    private BigDecimal voltageV = BigDecimal.ZERO;

    @Column(name = "CURRENT_A", precision = 8, scale = 2)
    private BigDecimal currentA = BigDecimal.ZERO;

    @Column(name = "SOC_PERCENT")
    private Integer socPercent = 0;

    @Column(name = "BATTERY_TEMP_C")
    private Integer batteryTempC = 25;

    @Column(name = "CAR_MODEL", length = 50)
    private String carModel;

    @Column(name = "USER_TAG", length = 50)
    private String userTag;

    @Column(name = "ACCUMULATED_KWH", precision = 10, scale = 2)
    private BigDecimal accumulatedKwh = BigDecimal.ZERO;

    @Column(name = "CHARGING_MINUTES")
    private Integer chargingMinutes = 0;

    @Column(name = "LAST_HEARTBEAT")
    private LocalDateTime lastHeartbeat;

    @Column(name = "UPDT_DT")
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
