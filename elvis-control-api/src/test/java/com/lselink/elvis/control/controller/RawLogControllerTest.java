package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.common.ApiResponse;
import com.lselink.elvis.control.common.PageResponse;
import com.lselink.elvis.control.dto.RawLogResponseDto;
import com.lselink.elvis.control.dto.RawLogSearchCondition;
import com.lselink.elvis.control.service.RawLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ClickHouse 원본 통신로그 REST 컨트롤러 단위 테스트 (Sprint 30: S30-DATA-0030-TE)
 */
@ExtendWith(MockitoExtension.class)
class RawLogControllerTest {

    @Mock
    private RawLogService rawLogService;

    @InjectMocks
    private RawLogController rawLogController;

    private RawLogResponseDto sampleLog;
    private RawLogSearchCondition sampleCondition;

    @BeforeEach
    void setUp() {
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
                .rawJson("{\"action\":\"MeterValues\",\"chargeBoxId\":\"00000001\"}")
                .build();

        sampleCondition = RawLogSearchCondition.builder()
                .chargeBoxId("00000001")
                .connectorId(1)
                .page(0)
                .size(20)
                .build();
    }

    @Test
    @DisplayName("[엔드포인트 검증] GET /api/v1/logs/raw/paged 페이징 검색 정상 응답 확인")
    void searchRawLogsPaged_Success() {
        // given
        PageResponse<RawLogResponseDto> mockPage = PageResponse.of(List.of(sampleLog), 0, 20, 1L);
        when(rawLogService.searchRawLogs(any())).thenReturn(mockPage);

        // when
        ApiResponse<PageResponse<RawLogResponseDto>> response = rawLogController.searchRawLogsPaged(sampleCondition);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getTotalElements()).isEqualTo(1L);
        assertThat(response.getData().getContent().get(0).getChargeBoxId()).isEqualTo("00000001");

        verify(rawLogService, times(1)).searchRawLogs(sampleCondition);
    }

    @Test
    @DisplayName("[엔드포인트 검증] GET /api/v1/logs/raw/latest 최근 로그 N건 조회 정상 응답 확인")
    void getLatestLogs_Success() {
        // given
        when(rawLogService.getLatestLogs(eq("00000001"), eq(20)))
                .thenReturn(List.of(sampleLog));

        // when
        ApiResponse<List<RawLogResponseDto>> response = rawLogController.getLatestLogs("00000001", 20);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getChargeBoxId()).isEqualTo("00000001");

        verify(rawLogService, times(1)).getLatestLogs("00000001", 20);
    }
}
