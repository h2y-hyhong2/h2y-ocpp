package com.lselink.elvis.control.service;

import com.lselink.elvis.control.dto.timeseries.StationPowerMetricsDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesChartResponseDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesSessionPointDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesSummaryDto;

import java.util.List;

/**
 * 시계열 통계 및 차트 데이터 비즈니스 서비스 인터페이스
 */
public interface TimeSeriesService {

    /**
     * 특정 충전기/커넥터의 기간별 다운샘플링 차트 데이터 조회
     *
     * @param chargeBoxId 충전기 식별자 (8자리)
     * @param connectorId 커넥터 번호 (1 또는 2)
     * @param range       조회 기간 ("1h", "24h", "7d", "30d")
     * @return 다운샘플링 포인트 및 요약 통계 응답 DTO
     */
    TimeSeriesChartResponseDto getDownsampledChart(String chargeBoxId, Integer connectorId, String range);

    /**
     * 특정 충전기/커넥터의 기간별 전력 요약 통계 조회
     */
    TimeSeriesSummaryDto getTimeSeriesSummary(String chargeBoxId, Integer connectorId, String range);

    /**
     * 특정 충전 세션(트랜잭션)의 충전 프로파일 곡선 조회
     */
    List<TimeSeriesSessionPointDto> getSessionProfile(String chargeBoxId, Integer connectorId, Long transactionId);

    /**
     * 특정 충전소의 전체 실시간 전력 부하 합산 목록 조회 (최근 1시간)
     */
    List<StationPowerMetricsDto> getStationPowerMetrics(String stationId);
}
