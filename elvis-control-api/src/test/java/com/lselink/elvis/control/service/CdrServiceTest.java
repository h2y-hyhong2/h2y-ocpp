package com.lselink.elvis.control.service;

import com.lselink.elvis.control.common.PageResponse;
import com.lselink.elvis.control.dto.CdrResponseDto;
import com.lselink.elvis.control.dto.CdrSearchCondition;
import com.lselink.elvis.control.dto.CdrSummaryDto;
import com.lselink.elvis.control.entity.ChargerEntity;
import com.lselink.elvis.control.entity.StationEntity;
import com.lselink.elvis.control.entity.TransactionCdrEntity;
import com.lselink.elvis.control.repository.ChargerRepository;
import com.lselink.elvis.control.repository.StationRepository;
import com.lselink.elvis.control.repository.TransactionCdrRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 충전 거래 이력(CDR) 비즈니스 서비스 단위 테스트 (Sprint 29: S29-API-0030-TE)
 */
@ExtendWith(MockitoExtension.class)
class CdrServiceTest {

    @Mock
    private TransactionCdrRepository transactionCdrRepository;

    @Mock
    private StationRepository stationRepository;

    @Mock
    private ChargerRepository chargerRepository;

    @InjectMocks
    private CdrServiceImpl cdrService;

    private TransactionCdrEntity sampleCdr;
    private ChargerEntity sampleCharger;
    private StationEntity sampleStation;

    @BeforeEach
    void setUp() {
        LocalDateTime startTime = LocalDateTime.of(2026, 10, 26, 10, 0, 0);
        LocalDateTime stopTime = LocalDateTime.of(2026, 10, 26, 10, 45, 0);

        sampleCdr = new TransactionCdrEntity();
        sampleCdr.setTransactionId(1001L);
        sampleCdr.setChargeBoxId("00000001");
        sampleCdr.setConnectorId(1);
        sampleCdr.setUserTag("RFID_CARD_01");
        sampleCdr.setStartTime(startTime);
        sampleCdr.setStopTime(stopTime);
        sampleCdr.setMeterStartWh(10000L);
        sampleCdr.setMeterStopWh(35500L);
        sampleCdr.setTotalKwh(new BigDecimal("25.50"));
        sampleCdr.setUnitPrice(new BigDecimal("345.00"));
        sampleCdr.setTotalAmount(new BigDecimal("8797.50"));
        sampleCdr.setPaymentStatus("PAID");
        sampleCdr.setStopReason("Local");

        sampleCharger = new ChargerEntity();
        sampleCharger.setChargeBoxId("00000001");
        sampleCharger.setStId("000000");
        sampleCharger.setCpId("01");
        sampleCharger.setVendor("LS E-Link");
        sampleCharger.setModel("EL-200");

        sampleStation = new StationEntity();
        sampleStation.setStId("000000");
        sampleStation.setName("안양 스마트하이퍼 차징 스테이션");
        sampleStation.setCorpId("CORP-01");
    }

    @Test
    @DisplayName("다차원 조건 검색 및 서버사이드 페이징(getPagedCdr) 조회 검증")
    void testGetPagedCdr() {
        // given
        CdrSearchCondition cond = new CdrSearchCondition();
        cond.setChargeBoxId("00000001");
        cond.setPaymentStatus("PAID");

        Page<TransactionCdrEntity> pageResult = new PageImpl<>(Collections.singletonList(sampleCdr));
        when(transactionCdrRepository.searchCdrPaged(eq("00000001"), isNull(), eq("PAID"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(pageResult);
        when(chargerRepository.findAllById(any())).thenReturn(Collections.singletonList(sampleCharger));
        when(stationRepository.findAll()).thenReturn(Collections.singletonList(sampleStation));

        // when
        PageResponse<CdrResponseDto> response = cdrService.getPagedCdr(cond, 0, 20);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        CdrResponseDto dto = response.getContent().get(0);

        assertThat(dto.getTransactionId()).isEqualTo(1001L);
        assertThat(dto.getChargeBoxId()).isEqualTo("00000001");
        assertThat(dto.getStationId()).isEqualTo("000000");
        assertThat(dto.getStationName()).isEqualTo("안양 스마트하이퍼 차징 스테이션");
        assertThat(dto.getConnectorId()).isEqualTo(1);
        assertThat(dto.getUserTag()).isEqualTo("RFID_CARD_01");
        assertThat(dto.getKwh()).isEqualTo(new BigDecimal("25.50"));
        assertThat(dto.getPaymentStatus()).isEqualTo("PAID");
        assertThat(dto.getDurationMinutes()).isEqualTo(45.0); // 45분
        assertThat(response.getTotalElements()).isEqualTo(1L);

        verify(transactionCdrRepository, times(1)).searchCdrPaged(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("조건 검색 4대 KPI 요약 통계(getCdrSummary) 산출 검증")
    void testGetCdrSummary() {
        // given
        CdrSearchCondition cond = new CdrSearchCondition();
        cond.setPaymentStatus("PAID");

        Object[] rawAgg = new Object[]{ 150L, new BigDecimal("3540.25"), new BigDecimal("1225000.00") };
        when(transactionCdrRepository.getCdrAggregateSummaryByCondition(isNull(), isNull(), eq("PAID"), isNull(), isNull()))
                .thenReturn(new Object[]{ rawAgg });

        // 평균 시간 산출용 목록
        when(transactionCdrRepository.findAllFiltered(isNull(), isNull(), eq("PAID"), isNull(), isNull()))
                .thenReturn(Collections.singletonList(sampleCdr));

        // when
        CdrSummaryDto summary = cdrService.getCdrSummary(cond);

        // then
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalCount()).isEqualTo(150L);
        assertThat(summary.getTotalKwh()).isEqualTo(new BigDecimal("3540.25"));
        assertThat(summary.getTotalAmount()).isEqualTo(new BigDecimal("1225000"));
        assertThat(summary.getAvgMinutes()).isEqualTo(45.0); // 45분 평균
    }

    @Test
    @DisplayName("대용량 CSV 스트리밍용 전체 필터링 목록(getAllFilteredCdr) 조회 검증")
    void testGetAllFilteredCdr() {
        // given
        CdrSearchCondition cond = new CdrSearchCondition();
        cond.setChargeBoxId("00000001");

        when(transactionCdrRepository.findAllFiltered(eq("00000001"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Collections.singletonList(sampleCdr));
        when(chargerRepository.findAllById(any())).thenReturn(Collections.singletonList(sampleCharger));
        when(stationRepository.findAll()).thenReturn(Collections.singletonList(sampleStation));

        // when
        List<CdrResponseDto> list = cdrService.getAllFilteredCdr(cond);

        // then
        assertThat(list).hasSize(1);
        CdrResponseDto dto = list.get(0);
        assertThat(dto.getChargeBoxId()).isEqualTo("00000001");
        assertThat(dto.getStationName()).isEqualTo("안양 스마트하이퍼 차징 스테이션");
        assertThat(dto.getDurationMinutes()).isEqualTo(45.0);
    }
}
