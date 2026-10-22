package com.lselink.elvis.control.dto.timeseries;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 시간 버킷 단위 시계열 다운샘플링 집계 DTO
 */
public class TimeSeriesBucketDto {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private String chargeBoxId;
    private Integer connectorId;
    private Double avgPowerKw;
    private Double peakPowerKw;
    private Double avgVoltageV;
    private Double avgCurrentA;
    private Integer maxSoc;
    private Long sampleCount;

    public TimeSeriesBucketDto() {
    }

    public TimeSeriesBucketDto(LocalDateTime timestamp, String chargeBoxId, Integer connectorId,
                               Double avgPowerKw, Double peakPowerKw, Double avgVoltageV,
                               Double avgCurrentA, Integer maxSoc, Long sampleCount) {
        this.timestamp = timestamp;
        this.chargeBoxId = chargeBoxId;
        this.connectorId = connectorId;
        this.avgPowerKw = avgPowerKw;
        this.peakPowerKw = peakPowerKw;
        this.avgVoltageV = avgVoltageV;
        this.avgCurrentA = avgCurrentA;
        this.maxSoc = maxSoc;
        this.sampleCount = sampleCount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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

    public Double getAvgPowerKw() {
        return avgPowerKw;
    }

    public void setAvgPowerKw(Double avgPowerKw) {
        this.avgPowerKw = avgPowerKw;
    }

    public Double getPeakPowerKw() {
        return peakPowerKw;
    }

    public void setPeakPowerKw(Double peakPowerKw) {
        this.peakPowerKw = peakPowerKw;
    }

    public Double getAvgVoltageV() {
        return avgVoltageV;
    }

    public void setAvgVoltageV(Double avgVoltageV) {
        this.avgVoltageV = avgVoltageV;
    }

    public Double getAvgCurrentA() {
        return avgCurrentA;
    }

    public void setAvgCurrentA(Double avgCurrentA) {
        this.avgCurrentA = avgCurrentA;
    }

    public Integer getMaxSoc() {
        return maxSoc;
    }

    public void setMaxSoc(Integer maxSoc) {
        this.maxSoc = maxSoc;
    }

    public Long getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(Long sampleCount) {
        this.sampleCount = sampleCount;
    }
}
