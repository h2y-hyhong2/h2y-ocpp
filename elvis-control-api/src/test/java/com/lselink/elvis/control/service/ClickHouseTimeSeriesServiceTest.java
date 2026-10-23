package com.lselink.elvis.control.service;

import com.lselink.elvis.control.common.ApiResponse;
import com.lselink.elvis.control.controller.TimeSeriesController;
import com.lselink.elvis.control.dto.timeseries.StationPowerMetricsDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesBucketDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesChartResponseDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesSessionPointDto;
import com.lselink.elvis.control.dto.timeseries.TimeSeriesSummaryDto;
import com.lselink.elvis.control.repository.timeseries.ClickHouseTimeSeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ClickHouse 시계열 통계 API 및 비즈니스 로직(적응형 다운샘플링, Graceful Degradation) 검증 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class ClickHouseTimeSeriesServiceTest {

    @Mock
    private ClickHouseTimeSeriesRepository clickHouseRepository;

    private TimeSeriesServiceImpl timeSeriesService;
    private TimeSeriesServiceImpl fallbackTimeSeriesService; // Repository가 null인 미가동 환경 서비스
    private TimeSeriesController timeSeriesController;

    private TimeSeriesBucketDto mockBucketPoint;
    private TimeSeriesSummaryDto mockSummary;
    private TimeSeriesSessionPointDto mockSessionPoint;
    private StationPowerMetricsDto mockStationMetric;

    @BeforeEach
    void setUp() {
        timeSeriesService = new TimeSeriesServiceImpl(clickHouseRepository);
        fallbackTimeSeriesService = new TimeSeriesServiceImpl(null);
        timeSeriesController = new TimeSeriesController(timeSeriesService);

        LocalDateTime now = LocalDateTime.now();

        mockBucketPoint = new TimeSeriesBucketDto(
                now.minusMinutes(5), "00000101", 1,
                85.5, 98.2, 385.0, 130.2, 75, 30L
        );

        mockSummary = new TimeSeriesSummaryDto(
                "00000101", 1440L, 45.2, 98.5, now.minusMinutes(30), 132.5, 320.5, 20, 85
        );

        mockSessionPoint = new TimeSeriesSessionPointDto(
                now.minusSeconds(10), 92.4, 382.1, 128.5, 68, 15400L
        );

        mockStationMetric = new StationPowerMetricsDto(
                now.minusMinutes(1), "000001", 185.0, 382.5, 2
        );
    }

    @Test
    @DisplayName("[24h 기본 조회] 적응형 다운샘플링 5분 버킷(5m) 산정 및 포인트/요약 매핑 검증")
    void testDownsampledChart_With24hRange_Calculates5mIntervalAndReturnsPoints() {
        // given
        String chargeBoxId = "00000101";
        Integer connectorId = 1;
        String range = "24h";

        when(clickHouseRepository.findDownsampledSeries(eq(chargeBoxId), eq(connectorId), eq(5), any(), any()))
                .thenReturn(Collections.singletonList(mockBucketPoint));
        when(clickHouseRepository.findTimeSeriesSummary(eq(chargeBoxId), eq(connectorId), any(), any()))
                .thenReturn(Optional.of(mockSummary));

        // when
        TimeSeriesChartResponseDto response = timeSeriesService.getDownsampledChart(chargeBoxId, connectorId, range);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getChargeBoxId()).isEqualTo(chargeBoxId);
        assertThat(response.getConnectorId()).isEqualTo(1);
        assertThat(response.getRange()).isEqualTo("24h");
        assertThat(response.getInterval()).isEqualTo("5m");
        assertThat(response.getPoints()).hasSize(1);
        assertThat(response.getPoints().get(0).getAvgPowerKw()).isEqualTo(85.5);
        assertThat(response.getSummary()).isNotNull();
        assertThat(response.getSummary().getPeakPowerKw()).isEqualTo(98.5);
        assertThat(response.getSummary().getConsumedKwh()).isEqualTo(320.5);

        verify(clickHouseRepository).findDownsampledSeries(eq(chargeBoxId), eq(connectorId), eq(5), any(), any());
        verify(clickHouseRepository).findTimeSeriesSummary(eq(chargeBoxId), eq(connectorId), any(), any());
    }

    @Test
    @DisplayName("[1h 단기 조회] 적응형 다운샘플링 1분 버킷(1m) 산정 검증")
    void testDownsampledChart_With1hRange_Calculates1mInterval() {
        // given
        String chargeBoxId = "00000101";
        when(clickHouseRepository.findDownsampledSeries(eq(chargeBoxId), eq(1), eq(1), any(), any()))
                .thenReturn(Collections.singletonList(mockBucketPoint));
        when(clickHouseRepository.findTimeSeriesSummary(eq(chargeBoxId), eq(1), any(), any()))
                .thenReturn(Optional.of(mockSummary));

        // when
        TimeSeriesChartResponseDto response = timeSeriesService.getDownsampledChart(chargeBoxId, null, "1h");

        // then
        assertThat(response.getRange()).isEqualTo("1h");
        assertThat(response.getInterval()).isEqualTo("1m");
        assertThat(response.getPoints()).hasSize(1);

        verify(clickHouseRepository).findDownsampledSeries(eq(chargeBoxId), eq(1), eq(1), any(), any());
    }

    @Test
    @DisplayName("[7d 중기 조회] 적응형 다운샘플링 30분 버킷(30m) 산정 검증")
    void testDownsampledChart_With7dRange_Calculates30mInterval() {
        // given
        String chargeBoxId = "00000101";
        when(clickHouseRepository.findDownsampledSeries(eq(chargeBoxId), eq(1), eq(30), any(), any()))
                .thenReturn(Collections.singletonList(mockBucketPoint));
        when(clickHouseRepository.findTimeSeriesSummary(eq(chargeBoxId), eq(1), any(), any()))
                .thenReturn(Optional.of(mockSummary));

        // when
        TimeSeriesChartResponseDto response = timeSeriesService.getDownsampledChart(chargeBoxId, 1, "7d");

        // then
        assertThat(response.getRange()).isEqualTo("7d");
        assertThat(response.getInterval()).isEqualTo("30m");

        verify(clickHouseRepository).findDownsampledSeries(eq(chargeBoxId), eq(1), eq(30), any(), any());
    }

    @Test
    @DisplayName("[30d 장기 조회] 적응형 다운샘플링 1시간 버킷(60m) 산정 검증")
    void testDownsampledChart_With30dRange_Calculates60mInterval() {
        // given
        String chargeBoxId = "00000101";
        when(clickHouseRepository.findDownsampledSeries(eq(chargeBoxId), eq(1), eq(60), any(), any()))
                .thenReturn(Collections.singletonList(mockBucketPoint));
        when(clickHouseRepository.findTimeSeriesSummary(eq(chargeBoxId), eq(1), any(), any()))
                .thenReturn(Optional.of(mockSummary));

        // when
        TimeSeriesChartResponseDto response = timeSeriesService.getDownsampledChart(chargeBoxId, 1, "30d");

        // then
        assertThat(response.getRange()).isEqualTo("30d");
        assertThat(response.getInterval()).isEqualTo("60m");

        verify(clickHouseRepository).findDownsampledSeries(eq(chargeBoxId), eq(1), eq(60), any(), any());
    }

    @Test
    @DisplayName("[Graceful Degradation] ClickHouse 미가동 환경 시 500 장애 없이 빈 차트 및 기본 요약 응답 반환 검증")
    void testDownsampledChart_GracefulDegradation_WhenRepositoryIsNull() {
        // given (fallbackTimeSeriesService: repository=null)
        String chargeBoxId = "00000101";

        // when
        TimeSeriesChartResponseDto response = fallbackTimeSeriesService.getDownsampledChart(chargeBoxId, 1, "24h");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getChargeBoxId()).isEqualTo(chargeBoxId);
        assertThat(response.getPoints()).isEmpty();
        assertThat(response.getSummary()).isNotNull();
        assertThat(response.getSummary().getTotalSamples()).isEqualTo(0L);
        assertThat(response.getSummary().getConsumedKwh()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("[전력 요약 통계] 기간별 집계 요약 DTO 정상 매핑 검증")
    void testTimeSeriesSummary_Success() {
        // given
        String chargeBoxId = "00000101";
        when(clickHouseRepository.findTimeSeriesSummary(eq(chargeBoxId), eq(1), any(), any()))
                .thenReturn(Optional.of(mockSummary));

        // when
        TimeSeriesSummaryDto result = timeSeriesService.getTimeSeriesSummary(chargeBoxId, 1, "24h");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getChargeBoxId()).isEqualTo(chargeBoxId);
        assertThat(result.getAvgPowerKw()).isEqualTo(45.2);
        assertThat(result.getPeakPowerKw()).isEqualTo(98.5);
        assertThat(result.getStartSoc()).isEqualTo(20);
        assertThat(result.getCurrentSoc()).isEqualTo(85);
    }

    @Test
    @DisplayName("[전력 요약 통계 Graceful Degradation] 미가동 환경 시 기본 0값 요약 반환 검증")
    void testTimeSeriesSummary_GracefulDegradation_WhenRepositoryIsNull() {
        // given
        String chargeBoxId = "00000101";

        // when
        TimeSeriesSummaryDto result = fallbackTimeSeriesService.getTimeSeriesSummary(chargeBoxId, 1, "24h");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalSamples()).isEqualTo(0L);
        assertThat(result.getPeakPowerKw()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("[세션 충전 프로파일] 10초 주기 정밀 계측 곡선 조회 검증")
    void testSessionProfile_Success() {
        // given
        String chargeBoxId = "00000101";
        Long transactionId = 1001L;
        when(clickHouseRepository.findSessionProfile(eq(chargeBoxId), eq(1), eq(transactionId), eq(10)))
                .thenReturn(Arrays.asList(mockSessionPoint));

        // when
        List<TimeSeriesSessionPointDto> points = timeSeriesService.getSessionProfile(chargeBoxId, 1, transactionId);

        // then
        assertThat(points).hasSize(1);
        assertThat(points.get(0).getPowerKw()).isEqualTo(92.4);
        assertThat(points.get(0).getVoltageV()).isEqualTo(382.1);
        assertThat(points.get(0).getSoc()).isEqualTo(68);

        verify(clickHouseRepository).findSessionProfile(eq(chargeBoxId), eq(1), eq(transactionId), eq(10));
    }

    @Test
    @DisplayName("[세션 충전 프로파일] transactionId 누락 또는 미가동 시 빈 목록 반환 검증")
    void testSessionProfile_WhenTransactionIdNullOrRepositoryNull() {
        // when
        List<TimeSeriesSessionPointDto> pointsWithNullTx = timeSeriesService.getSessionProfile("00000101", 1, null);
        List<TimeSeriesSessionPointDto> pointsWithFallback = fallbackTimeSeriesService.getSessionProfile("00000101", 1, 1001L);

        // then
        assertThat(pointsWithNullTx).isEmpty();
        assertThat(pointsWithFallback).isEmpty();
    }

    @Test
    @DisplayName("[충전소 전력 메트릭] 최근 1시간 합산 메트릭 정상 조회 검증")
    void testStationPowerMetrics_Success() {
        // given
        String stationId = "000001";
        when(clickHouseRepository.findStationPowerMetrics(eq(stationId), any()))
                .thenReturn(Collections.singletonList(mockStationMetric));

        // when
        List<StationPowerMetricsDto> metrics = timeSeriesService.getStationPowerMetrics(stationId);

        // then
        assertThat(metrics).hasSize(1);
        assertThat(metrics.get(0).getStationId()).isEqualTo("000001");
        assertThat(metrics.get(0).getTotalPowerKw()).isEqualTo(185.0);
        assertThat(metrics.get(0).getActiveChargersCount()).isEqualTo(2);

        verify(clickHouseRepository).findStationPowerMetrics(eq(stationId), any());
    }

    @Test
    @DisplayName("[컨트롤러 연동 검증] TimeSeriesController REST 엔드포인트 응답 래핑 검증")
    void testController_GetDownsampledChart_Success() {
        // given
        String chargeBoxId = "00000101";
        when(clickHouseRepository.findDownsampledSeries(eq(chargeBoxId), eq(1), eq(5), any(), any()))
                .thenReturn(Collections.singletonList(mockBucketPoint));
        when(clickHouseRepository.findTimeSeriesSummary(eq(chargeBoxId), eq(1), any(), any()))
                .thenReturn(Optional.of(mockSummary));

        // when
        ApiResponse<TimeSeriesChartResponseDto> response = timeSeriesController.getDownsampledChart(
                chargeBoxId, 1, "24h");

        // then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getChargeBoxId()).isEqualTo(chargeBoxId);
        assertThat(response.getData().getPoints()).hasSize(1);
    }
}
