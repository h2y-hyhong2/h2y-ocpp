package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.common.ApiResponse;
import com.lselink.elvis.control.dto.timeseries.StationPowerMetricsDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesChartResponseDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesSessionPointDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesSummaryDto;
import com.lselink.elvis.control.service.TimeSeriesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ClickHouse 시계열 전력 통계 및 차트 데이터 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/metrics/timeseries")
public class TimeSeriesController {

    private static final Logger log = LoggerFactory.getLogger(TimeSeriesController.class);

    private final TimeSeriesService timeSeriesService;

    public TimeSeriesController(TimeSeriesService timeSeriesService) {
        this.timeSeriesService = timeSeriesService;
    }

    /**
     * 특정 충전기/커넥터의 기간별 다운샘플링 차트 데이터 조회
     * GET /api/v1/metrics/timeseries/chart?chargeBoxId=00000101&connectorId=1&range=24h
     */
    @GetMapping("/chart")
    public ApiResponse<TimeSeriesChartResponseDto> getDownsampledChart(
            @RequestParam String chargeBoxId,
            @RequestParam(required = false, defaultValue = "1") Integer connectorId,
            @RequestParam(required = false, defaultValue = "24h") String range) {

        log.debug("[API] 시계열 차트 데이터 요청: chargeBoxId={}, connectorId={}, range={}",
                chargeBoxId, connectorId, range);

        TimeSeriesChartResponseDto responseDto = timeSeriesService.getDownsampledChart(
                chargeBoxId, connectorId, range);

        return ApiResponse.success(responseDto);
    }

    /**
     * 특정 충전기/커넥터의 기간별 전력 요약 통계 조회
     * GET /api/v1/metrics/timeseries/summary?chargeBoxId=00000101&connectorId=1&range=24h
     */
    @GetMapping("/summary")
    public ApiResponse<TimeSeriesSummaryDto> getTimeSeriesSummary(
            @RequestParam String chargeBoxId,
            @RequestParam(required = false, defaultValue = "1") Integer connectorId,
            @RequestParam(required = false, defaultValue = "24h") String range) {

        log.debug("[API] 전력 요약 통계 요청: chargeBoxId={}, connectorId={}, range={}",
                chargeBoxId, connectorId, range);

        TimeSeriesSummaryDto summaryDto = timeSeriesService.getTimeSeriesSummary(
                chargeBoxId, connectorId, range);

        return ApiResponse.success(summaryDto);
    }

    /**
     * 특정 충전 세션(트랜잭션)의 충전 프로파일 곡선 포인트 조회
     * GET /api/v1/metrics/timeseries/sessions/{transactionId}?chargeBoxId=00000101&connectorId=1
     */
    @GetMapping("/sessions/{transactionId}")
    public ApiResponse<List<TimeSeriesSessionPointDto>> getSessionProfile(
            @PathVariable Long transactionId,
            @RequestParam String chargeBoxId,
            @RequestParam(required = false, defaultValue = "1") Integer connectorId) {

        log.debug("[API] 충전 세션 프로파일 요청: transactionId={}, chargeBoxId={}, connectorId={}",
                transactionId, chargeBoxId, connectorId);

        List<TimeSeriesSessionPointDto> points = timeSeriesService.getSessionProfile(
                chargeBoxId, connectorId, transactionId);

        return ApiResponse.success(points);
    }

    /**
     * 특정 충전소의 전체 실시간 전력 부하 합산 목록 조회 (최근 1시간)
     * GET /api/v1/metrics/timeseries/station/{stationId}
     */
    @GetMapping("/station/{stationId}")
    public ApiResponse<List<StationPowerMetricsDto>> getStationPowerMetrics(
            @PathVariable String stationId) {

        log.debug("[API] 충전소 실시간 전력 부하 요청: stationId={}", stationId);

        List<StationPowerMetricsDto> metrics = timeSeriesService.getStationPowerMetrics(stationId);

        return ApiResponse.success(metrics);
    }
}
