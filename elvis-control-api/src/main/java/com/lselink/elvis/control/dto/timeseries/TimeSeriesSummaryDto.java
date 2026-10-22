package com.lselink.elvis.control.dto.timeseries;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 전력 및 충전 상태 요약 통계 DTO
 */
public class TimeSeriesSummaryDto {

    private String chargeBoxId;
    private Long totalSamples;
    private Double avgPowerKw;
    private Double peakPowerKw;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime peakPowerTime;

    private Double peakCurrentA;
    private Double consumedKwh;
    private Integer startSoc;
    private Integer currentSoc;

    public TimeSeriesSummaryDto() {
    }

    public TimeSeriesSummaryDto(String chargeBoxId, Long totalSamples, Double avgPowerKw,
                                Double peakPowerKw, LocalDateTime peakPowerTime, Double peakCurrentA,
                                Double consumedKwh, Integer startSoc, Integer currentSoc) {
        this.chargeBoxId = chargeBoxId;
        this.totalSamples = totalSamples;
        this.avgPowerKw = avgPowerKw;
        this.peakPowerKw = peakPowerKw;
        this.peakPowerTime = peakPowerTime;
        this.peakCurrentA = peakCurrentA;
        this.consumedKwh = consumedKwh;
        this.startSoc = startSoc;
        this.currentSoc = currentSoc;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public void setChargeBoxId(String chargeBoxId) {
        this.chargeBoxId = chargeBoxId;
    }

    public Long getTotalSamples() {
        return totalSamples;
    }

    public void setTotalSamples(Long totalSamples) {
        this.totalSamples = totalSamples;
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

    public LocalDateTime getPeakPowerTime() {
        return peakPowerTime;
    }

    public void setPeakPowerTime(LocalDateTime peakPowerTime) {
        this.peakPowerTime = peakPowerTime;
    }

    public Double getPeakCurrentA() {
        return peakCurrentA;
    }

    public void setPeakCurrentA(Double peakCurrentA) {
        this.peakCurrentA = peakCurrentA;
    }

    public Double getConsumedKwh() {
        return consumedKwh;
    }

    public void setConsumedKwh(Double consumedKwh) {
        this.consumedKwh = consumedKwh;
    }

    public Integer getStartSoc() {
        return startSoc;
    }

    public void setStartSoc(Integer startSoc) {
        this.startSoc = startSoc;
    }

    public Integer getCurrentSoc() {
        return currentSoc;
    }

    public void setCurrentSoc(Integer currentSoc) {
        this.currentSoc = currentSoc;
    }
}
