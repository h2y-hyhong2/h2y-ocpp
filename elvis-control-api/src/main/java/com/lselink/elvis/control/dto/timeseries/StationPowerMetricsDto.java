package com.lselink.elvis.control.dto.timeseries;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 충전소 단위 전체 전력 부하 실시간 합산 DTO
 */
public class StationPowerMetricsDto {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private String stationId;
    private Double totalPowerKw;
    private Double avgVoltageV;
    private Integer activeChargersCount;

    public StationPowerMetricsDto() {
    }

    public StationPowerMetricsDto(LocalDateTime timestamp, String stationId, Double totalPowerKw,
                                  Double avgVoltageV, Integer activeChargersCount) {
        this.timestamp = timestamp;
        this.stationId = stationId;
        this.totalPowerKw = totalPowerKw;
        this.avgVoltageV = avgVoltageV;
        this.activeChargersCount = activeChargersCount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public Double getTotalPowerKw() {
        return totalPowerKw;
    }

    public void setTotalPowerKw(Double totalPowerKw) {
        this.totalPowerKw = totalPowerKw;
    }

    public Double getAvgVoltageV() {
        return avgVoltageV;
    }

    public void setAvgVoltageV(Double avgVoltageV) {
        this.avgVoltageV = avgVoltageV;
    }

    public Integer getActiveChargersCount() {
        return activeChargersCount;
    }

    public void setActiveChargersCount(Integer activeChargersCount) {
        this.activeChargersCount = activeChargersCount;
    }
}
