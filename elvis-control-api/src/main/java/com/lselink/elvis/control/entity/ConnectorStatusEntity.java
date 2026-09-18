package com.lselink.elvis.control.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TBL_CONNECTOR_STATUS")
@Getter
@Setter
@NoArgsConstructor
public class ConnectorStatusEntity {

    @EmbeddedId
    private ConnectorStatusId id;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "POWER_KW", precision = 8, scale = 2)
    private BigDecimal powerKw;

    @Column(name = "VOLTAGE_V", precision = 8, scale = 2)
    private BigDecimal voltageV;

    @Column(name = "CURRENT_A", precision = 8, scale = 2)
    private BigDecimal currentA;

    @Column(name = "SOC_PERCENT")
    private Integer socPercent;

    @Column(name = "BATTERY_TEMP_C")
    private Integer batteryTempC;

    @Column(name = "CAR_MODEL", length = 50)
    private String carModel;

    @Column(name = "USER_TAG", length = 50)
    private String userTag;

    @Column(name = "ACCUMULATED_KWH", precision = 10, scale = 2)
    private BigDecimal accumulatedKwh;

    @Column(name = "CHARGING_MINUTES")
    private Integer chargingMinutes;

    @Column(name = "LAST_HEARTBEAT")
    private LocalDateTime lastHeartbeat;

    @Column(name = "UPDT_DT")
    private LocalDateTime updtDt;
}
