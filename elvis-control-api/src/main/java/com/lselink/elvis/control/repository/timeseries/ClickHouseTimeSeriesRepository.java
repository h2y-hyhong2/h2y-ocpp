package com.lselink.elvis.control.repository.timeseries;

import com.lselink.elvis.control.dto.timeseries.StationPowerMetricsDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesBucketDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesSessionPointDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesSummaryDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ClickHouse 시계열 집계 쿼리 Repository 인터페이스
 */
public interface ClickHouseTimeSeriesRepository {

    /**
     * 특정 충전기/커넥터의 기간별 다운샘플링 집계 포인트 목록 조회
     */
    List<TimeSeriesBucketDto> findDownsampledSeries(
            String chargeBoxId, Integer connectorId, int intervalMinutes,
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 특정 충전기/커넥터의 기간 내 전력 요약 통계 조회 (피크, 소비량 등)
     */
    Optional<TimeSeriesSummaryDto> findTimeSeriesSummary(
            String chargeBoxId, Integer connectorId,
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 특정 충전 세션(트랜잭션)의 충전 프로파일 곡선 포인트 목록 조회
     */
    List<TimeSeriesSessionPointDto> findSessionProfile(
            String chargeBoxId, Integer connectorId, Long transactionId, int intervalSeconds);

    /**
     * 특정 충전소의 전체 실시간 전력 부하 합산 목록 조회 (최근 1시간 등)
     */
    List<StationPowerMetricsDto> findStationPowerMetrics(String stationId, LocalDateTime startTime);
}
