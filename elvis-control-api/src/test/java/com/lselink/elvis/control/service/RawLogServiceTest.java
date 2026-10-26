package com.lselink.elvis.control.service;

import com.lselink.elvis.control.common.PageResponse;
import com.lselink.elvis.control.dto.RawLogResponseDto;
import com.lselink.elvis.control.dto.RawLogSearchCondition;
import com.lselink.elvis.control.repository.rawlog.RawLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ClickHouse 원본 통신로그 조회 서비스 및 Graceful Degradation 검증 단위 테스트 (Sprint 30: S30-DATA-0030-TE)
 */
@ExtendWith(MockitoExtension.class)
class RawLogServiceTest {

    @Mock
    private RawLogRepository rawLogRepository;

    private RawLogServiceImpl rawLogService;
    private RawLogServiceImpl fallbackService; // Repository가 null인 미가동 환경 서비스

    private RawLogResponseDto sampleLog;
    private RawLogSearchCondition sampleCondition;

    @BeforeEach
    void setUp() {
        rawLogService = new RawLogServiceImpl(rawLogRepository);
        fallbackService = new RawLogServiceImpl(null);

        LocalDateTime now = LocalDateTime.now();

        sampleLog = RawLogResponseDto.builder()
                .eventTime(now)
                .chargeBoxId("00000001")
                .connectorId(1)
                .transactionId(1001L)
                .voltage(380.0)
                .currentImport(125.5)
                .powerActive(47.7)
                .energyActive(12500.0)
                .soc(65.0)
                .rawJson("{\"action\":\"MeterValues\",\"chargeBoxId\":\"00000001\",\"connectorId\":1}")
                .build();

        sampleCondition = RawLogSearchCondition.builder()
                .chargeBoxId("00000001")
                .connectorId(1)
                .transactionId(1001L)
                .page(0)
                .size(20)
                .build();
    }

    @Test
    @DisplayName("[정상 조회] 조건 검색 시 ClickHouse count() 및 페이징 목록 인출 검증")
    void searchRawLogs_Success() {
        // given
        when(rawLogRepository.countByCondition(any())).thenReturn(1L);
        when(rawLogRepository.findPagedByCondition(any())).thenReturn(List.of(sampleLog));

        // when
        PageResponse<RawLogResponseDto> response = rawLogService.searchRawLogs(sampleCondition);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTotalElements()).isEqualTo(1L);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getChargeBoxId()).isEqualTo("00000001");
        assertThat(response.getContent().get(0).getRawJson()).contains("MeterValues");

        verify(rawLogRepository, times(1)).countByCondition(sampleCondition);
        verify(rawLogRepository, times(1)).findPagedByCondition(sampleCondition);
    }

    @Test
    @DisplayName("[조건 부합 0건] 총 건수가 0건인 경우 지연 인출 쿼리 스킵 및 빈 페이징 반환 검증")
    void searchRawLogs_ZeroTotal_SkipsPagedQuery() {
        // given
        when(rawLogRepository.countByCondition(any())).thenReturn(0L);

        // when
        PageResponse<RawLogResponseDto> response = rawLogService.searchRawLogs(sampleCondition);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTotalElements()).isEqualTo(0L);
        assertThat(response.getContent()).isEmpty();

        verify(rawLogRepository, times(1)).countByCondition(sampleCondition);
        verify(rawLogRepository, never()).findPagedByCondition(any());
    }

    @Test
    @DisplayName("[Graceful Degradation] ClickHouse 저장소 미탑재(null) 시 500 에러 없이 빈 페이징 반환 검증")
    void searchRawLogs_FallbackWhenRepositoryNull() {
        // when
        PageResponse<RawLogResponseDto> response = fallbackService.searchRawLogs(sampleCondition);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTotalElements()).isEqualTo(0L);
        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("[Graceful Degradation] ClickHouse 통신 단절(DataAccessException) 시 에러 포획 및 빈 페이징 폴백 검증")
    void searchRawLogs_FallbackWhenDataAccessException() {
        // given
        when(rawLogRepository.countByCondition(any()))
                .thenThrow(new DataAccessResourceFailureException("Connection refused to ClickHouse:8123"));

        // when
        PageResponse<RawLogResponseDto> response = rawLogService.searchRawLogs(sampleCondition);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTotalElements()).isEqualTo(0L);
        assertThat(response.getContent()).isEmpty();

        verify(rawLogRepository, times(1)).countByCondition(sampleCondition);
        verify(rawLogRepository, never()).findPagedByCondition(any());
    }

    @Test
    @DisplayName("[최근 로그] 특정 충전기 최근 통신로그 N건 정상 조회 검증")
    void getLatestLogs_Success() {
        // given
        when(rawLogRepository.findLatestByChargeBoxId(eq("00000001"), eq(10)))
                .thenReturn(List.of(sampleLog));

        // when
        List<RawLogResponseDto> result = rawLogService.getLatestLogs("00000001", 10);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChargeBoxId()).isEqualTo("00000001");
        verify(rawLogRepository, times(1)).findLatestByChargeBoxId("00000001", 10);
    }

    @Test
    @DisplayName("[Graceful Degradation] 최근 통신로그 조회 시 ClickHouse 단절되어도 빈 목록 반환 검증")
    void getLatestLogs_FallbackWhenDataAccessException() {
        // given
        when(rawLogRepository.findLatestByChargeBoxId(eq("00000001"), anyInt()))
                .thenThrow(new DataAccessResourceFailureException("ClickHouse timeout"));

        // when
        List<RawLogResponseDto> result = rawLogService.getLatestLogs("00000001", 20);

        // then
        assertThat(result).isEmpty();
        verify(rawLogRepository, times(1)).findLatestByChargeBoxId("00000001", 20);
    }
}
