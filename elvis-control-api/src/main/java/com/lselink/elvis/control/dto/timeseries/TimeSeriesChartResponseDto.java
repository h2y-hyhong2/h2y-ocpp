package com.lselink.elvis.control.dto.timeseries;

import java.util.List;

/**
 * 프론트엔드 ECharts 연동 시계열 차트 응답 DTO
 */
public class TimeSeriesChartResponseDto {

    private String chargeBoxId;
    private Integer connectorId;
    private String range;
    private String interval;
    private TimeSeriesSummaryDto summary;
    private List<TimeSeriesBucketDto> points;

    public TimeSeriesChartResponseDto() {
    }

    public TimeSeriesChartResponseDto(String chargeBoxId, Integer connectorId, String range,
                                      String interval, TimeSeriesSummaryDto summary,
                                      List<TimeSeriesBucketDto> points) {
        this.chargeBoxId = chargeBoxId;
        this.connectorId = connectorId;
        this.range = range;
        this.interval = interval;
        this.summary = summary;
        this.points = points;
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

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public TimeSeriesSummaryDto getSummary() {
        return summary;
    }

    public void setSummary(TimeSeriesSummaryDto summary) {
        this.summary = summary;
    }

    public List<TimeSeriesBucketDto> getPoints() {
        return points;
    }

    public void setPoints(List<TimeSeriesBucketDto> points) {
        this.points = points;
    }
}
