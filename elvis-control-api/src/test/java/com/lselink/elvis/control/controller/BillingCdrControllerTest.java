package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.common.ApiResponse;
import com.lselink.elvis.control.common.PageResponse;
import com.lselink.elvis.control.dto.CdrResponseDto;
import com.lselink.elvis.control.dto.CdrSearchCondition;
import com.lselink.elvis.control.dto.CdrSummaryDto;
import com.lselink.elvis.control.service.CdrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 충전 거래 이력(CDR) REST 컨트롤러 단위 테스트 (Sprint 29: S29-API-0030-TE)
 */
@ExtendWith(MockitoExtension.class)
class BillingCdrControllerTest {

    @Mock
    private CdrService cdrService;

    @InjectMocks
    private BillingCdrController controller;

    private CdrResponseDto sampleItem;
    private CdrSummaryDto sampleSummary;

    @BeforeEach
    void setUp() {
        sampleItem = CdrResponseDto.builder()
                .transactionId(1001L)
                .chargeBoxId("00000001")
                .stationId("000000")
                .stationName("안양 스마트하이퍼 차징 스테이션")
                .connectorId(1)
                .userTag("RFID_CARD_01")
                .startTime("2026-10-26 10:00:00")
                .stopTime("2026-10-26 10:45:00")
                .kwh(new BigDecimal("25.50"))
                .unitPrice(new BigDecimal("345.00"))
                .totalAmount(new BigDecimal("8798.00"))
                .paymentStatus("PAID")
                .stopReason("Local")
                .durationMinutes(45.0)
                .build();

        sampleSummary = CdrSummaryDto.builder()
                .totalCount(1L)
                .totalKwh(new BigDecimal("25.50"))
                .totalAmount(new BigDecimal("8798.00"))
                .avgMinutes(45.0)
                .build();
    }

    @Test
    @DisplayName("조건 검색 및 서버사이드 페이징(GET /api/v1/cdr/paged) 성공 검증")
    void testGetCdrPaged() {
        // given
        PageResponse<CdrResponseDto> pageResponse = PageResponse.of(
                Collections.singletonList(sampleItem), 0, 20, 1L, 1, true
        );
        when(cdrService.getPagedCdr(any(CdrSearchCondition.class), eq(0), eq(20)))
                .thenReturn(pageResponse);

        // when
        ApiResponse<PageResponse<CdrResponseDto>> response = controller.getCdrPaged(new CdrSearchCondition(), 0, 20);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getContent()).hasSize(1);
        assertThat(response.getData().getContent().get(0).getTransactionId()).isEqualTo(1001L);
        assertThat(response.getData().getContent().get(0).getStationName()).isEqualTo("안양 스마트하이퍼 차징 스테이션");

        verify(cdrService, times(1)).getPagedCdr(any(), eq(0), eq(20));
    }

    @Test
    @DisplayName("조건 검색 4대 KPI 요약 통계(GET /api/v1/cdr/summary) 성공 검증")
    void testGetCdrSummary() {
        // given
        when(cdrService.getCdrSummary(any(CdrSearchCondition.class)))
                .thenReturn(sampleSummary);

        // when
        ApiResponse<CdrSummaryDto> response = controller.getCdrSummary(new CdrSearchCondition());

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getTotalCount()).isEqualTo(1L);
        assertThat(response.getData().getTotalKwh()).isEqualTo(new BigDecimal("25.50"));
        assertThat(response.getData().getAvgMinutes()).isEqualTo(45.0);

        verify(cdrService, times(1)).getCdrSummary(any());
    }

    @Test
    @DisplayName("대용량 CSV 스트리밍 다운로드(GET /api/v1/cdr/export) 성공 검증")
    void testExportCdrCsv() throws IOException {
        // given
        when(cdrService.getAllFilteredCdr(any(CdrSearchCondition.class)))
                .thenReturn(Collections.singletonList(sampleItem));

        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        controller.exportCdrCsv(new CdrSearchCondition(), response);

        // then
        assertThat(response.getContentType()).isEqualTo("text/csv; charset=UTF-8");
        assertThat(response.getHeader("Content-Disposition")).contains("attachment; filename=\"cdr_records_");

        String content = response.getContentAsString();
        // BOM 확인
        assertThat(content).startsWith("\uFEFF");
        // 헤더 확인
        assertThat(content).contains("트랜잭션ID,충전기ID,충전소ID,충전소명,커넥터ID,회원태그,시작시각,종료시각,충전량(kWh),단가(원),결제금액(원),결제상태,종료사유,충전시간(분)");
        // 데이터 레코드 확인
        assertThat(content).contains("1001,00000001,000000,\"안양 스마트하이퍼 차징 스테이션\",1,RFID_CARD_01,2026-10-26 10:00:00,2026-10-26 10:45:00,25.50,345.00,8798.00,PAID,Local,45.0");
    }

    @Test
    @DisplayName("레거시 하위호환 엔드포인트(GET /api/v1/cdr) 정상 작동 검증")
    void testGetCdrRecordsLegacy() {
        // given
        PageResponse<CdrResponseDto> pageResponse = PageResponse.of(
                Collections.singletonList(sampleItem), 0, 100, 1L, 1, true
        );
        when(cdrService.getPagedCdr(any(CdrSearchCondition.class), eq(0), eq(100)))
                .thenReturn(pageResponse);
        when(cdrService.getCdrSummary(any(CdrSearchCondition.class)))
                .thenReturn(sampleSummary);

        // when
        ApiResponse<Map<String, Object>> response = controller.getCdrRecords();

        // then
        assertThat(response.isSuccess()).isTrue();
        Map<String, Object> data = response.getData();
        assertThat(data).containsKey("records");
        assertThat(data).containsKey("summary");

        verify(cdrService, times(1)).getPagedCdr(any(), eq(0), eq(100));
        verify(cdrService, times(1)).getCdrSummary(any());
    }
}
