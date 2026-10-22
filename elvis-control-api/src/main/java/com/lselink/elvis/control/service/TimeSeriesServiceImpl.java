package com.lselink.elvis.control.service;

import com.lselink.elvis.control.dto.timeseries.StationPowerMetricsDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesBucketDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesChartResponseDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesSessionPointDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesSummaryDto;
import com.lselink.elvis.control.repository.timeseries.ClickHouseTimeSeriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 시계열 통계 및 차트 데이터 비즈니스 서비스 구현체
 */
@Service
public class TimeSeriesServiceImpl implements TimeSeriesService {

    private static final Logger log = LoggerFactory.getLogger(TimeSeriesServiceImpl.class);

    private final ClickHouseTimeSeriesRepository clickHouseRepository;

    public TimeSeriesServiceImpl(
            @Autowired(required = false) ClickHouseTimeSeriesRepository clickHouseRepository) {
        this.clickHouseRepository = clickHouseRepository;
    }

    @Override
    public TimeSeriesChartResponseDto getDownsampledChart(String chargeBoxId, Integer connectorId, String range) {
        String normalizedRange = "24h";
        if (range != null && !range.trim().isEmpty()) {
            normalizedRange = range.trim().toLowerCase();
        }

        Integer normalizedConnectorId = 1;
        if (connectorId != null) {
            normalizedConnectorId = connectorId;
        }

        // 조회 기간별 버킷 인터벌 및 시작시각 산출
        int intervalMinutes = calculateIntervalMinutes(normalizedRange);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = calculateStartTime(now, normalizedRange);
        String intervalDesc = intervalMinutes + "m";

        // ClickHouse 미가동 또는 미탑재 환경에 대한 Graceful Degradation 방어
        if (clickHouseRepository == null) {
            log.warn("[TimeSeriesService] ClickHouse 저장소가 비활성화되어 기본 빈 응답을 반환합니다.");
            TimeSeriesSummaryDto emptySummary = createEmptySummary(chargeBoxId);
            return new TimeSeriesChartResponseDto(
                    chargeBoxId, normalizedConnectorId, normalizedRange, intervalDesc, emptySummary, new ArrayList<>());
        }

        // 1. 다운샘플링 포인트 목록 조회 (외부 I/O 변수 분리)
        List<TimeSeriesBucketDto> points = clickHouseRepository.findDownsampledSeries(
                chargeBoxId, normalizedConnectorId, intervalMinutes, startTime, now);

        // 2. 전력 요약 통계 조회 (외부 I/O 변수 분리)
        Optional<TimeSeriesSummaryDto> summaryOpt = clickHouseRepository.findTimeSeriesSummary(
                chargeBoxId, normalizedConnectorId, startTime, now);

        TimeSeriesSummaryDto summary = null;
        if (summaryOpt.isPresent()) {
            summary = summaryOpt.get();
        } else {
            summary = createEmptySummary(chargeBoxId);
        }

        return new TimeSeriesChartResponseDto(
                chargeBoxId, normalizedConnectorId, normalizedRange, intervalDesc, summary, points);
    }

    @Override
    public TimeSeriesSummaryDto getTimeSeriesSummary(String chargeBoxId, Integer connectorId, String range) {
        String normalizedRange = "24h";
        if (range != null && !range.trim().isEmpty()) {
            normalizedRange = range.trim().toLowerCase();
        }

        Integer normalizedConnectorId = 1;
        if (connectorId != null) {
            normalizedConnectorId = connectorId;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = calculateStartTime(now, normalizedRange);

        if (clickHouseRepository == null) {
            return createEmptySummary(chargeBoxId);
        }

        Optional<TimeSeriesSummaryDto> summaryOpt = clickHouseRepository.findTimeSeriesSummary(
                chargeBoxId, normalizedConnectorId, startTime, now);

        if (summaryOpt.isPresent()) {
            return summaryOpt.get();
        }
        return createEmptySummary(chargeBoxId);
    }

    @Override
    public List<TimeSeriesSessionPointDto> getSessionProfile(String chargeBoxId, Integer connectorId, Long transactionId) {
        Integer normalizedConnectorId = 1;
        if (connectorId != null) {
            normalizedConnectorId = connectorId;
        }

        if (clickHouseRepository == null || transactionId == null) {
            return Collections.emptyList();
        }

        // 충전 세션 상세는 10초 버킷으로 정밀 샘플링
        int intervalSeconds = 10;
        List<TimeSeriesSessionPointDto> profile = clickHouseRepository.findSessionProfile(
                chargeBoxId, normalizedConnectorId, transactionId, intervalSeconds);

        return profile;
    }

    @Override
    public List<StationPowerMetricsDto> getStationPowerMetrics(String stationId) {
        if (clickHouseRepository == null || stationId == null) {
            return Collections.emptyList();
        }

        // 최근 1시간 실시간 충전소 부하 합산
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);

        List<StationPowerMetricsDto> metrics = clickHouseRepository.findStationPowerMetrics(
                stationId, oneHourAgo);

        return metrics;
    }

    private int calculateIntervalMinutes(String range) {
        if ("1h".equals(range)) {
            return 1;   // 1시간 조회 시 1분 버킷 (60 포인트)
        }
        if ("7d".equals(range)) {
            return 30;  // 7일 조회 시 30분 버킷 (336 포인트)
        }
        if ("30d".equals(range)) {
            return 60;  // 30일 조회 시 1시간 버킷 (720 포인트)
        }
        // 기본 24h: 5분 버킷 (288 포인트)
        return 5;
    }

    private LocalDateTime calculateStartTime(LocalDateTime now, String range) {
        if ("1h".equals(range)) {
            return now.minusHours(1);
        }
        if ("7d".equals(range)) {
            return now.minusDays(7);
        }
        if ("30d".equals(range)) {
            return now.minusDays(30);
        }
        // 기본 24h
        return now.minusHours(24);
    }

    private TimeSeriesSummaryDto createEmptySummary(String chargeBoxId) {
        return new TimeSeriesSummaryDto(
                chargeBoxId, 0L, 0.0, 0.0, null, 0.0, 0.0, 0, 0);
    }
}
