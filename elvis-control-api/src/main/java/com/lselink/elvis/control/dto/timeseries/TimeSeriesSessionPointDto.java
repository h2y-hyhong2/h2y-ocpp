package com.lselink.elvis.control.dto.timeseries;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 충전 세션(트랜잭션) 단일 계측 포인트 DTO
 */
public class TimeSeriesSessionPointDto {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private Double powerKw;
    private Double voltageV;
    private Double currentA;
    private Integer soc;
    private Long cumulativeWh;

    public TimeSeriesSessionPointDto() {
    }

    public TimeSeriesSessionPointDto(LocalDateTime timestamp, Double powerKw, Double voltageV,
                                     Double currentA, Integer soc, Long cumulativeWh) {
        this.timestamp = timestamp;
        this.powerKw = powerKw;
        this.voltageV = voltageV;
        this.currentA = currentA;
        this.soc = soc;
        this.cumulativeWh = cumulativeWh;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Double getPowerKw() {
        return powerKw;
    }

    public void setPowerKw(Double powerKw) {
        this.powerKw = powerKw;
    }

    public Double getVoltageV() {
        return voltageV;
    }

    public void setVoltageV(Double voltageV) {
        this.voltageV = voltageV;
    }

    public Double getCurrentA() {
        return currentA;
    }

    public void setCurrentA(Double currentA) {
        this.currentA = currentA;
    }

    public Integer getSoc() {
        return soc;
    }

    public void setSoc(Integer soc) {
        this.soc = soc;
    }

    public Long getCumulativeWh() {
        return cumulativeWh;
    }

    public void setCumulativeWh(Long cumulativeWh) {
        this.cumulativeWh = cumulativeWh;
    }
}
