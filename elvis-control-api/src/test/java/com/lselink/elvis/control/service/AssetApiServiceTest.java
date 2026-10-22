package com.lselink.elvis.control.service;

import com.lselink.elvis.control.common.PageResponse;
import com.lselink.elvis.control.dto.ChargerResponseDto;
import com.lselink.elvis.control.dto.ChargerSummaryDto;
import com.lselink.elvis.control.dto.StationResponseDto;
import com.lselink.elvis.control.entity.ChargerEntity;
import com.lselink.elvis.control.entity.ConnectorStatusEntity;
import com.lselink.elvis.control.entity.CorpEntity;
import com.lselink.elvis.control.entity.StationEntity;
import com.lselink.elvis.control.repository.ChargerRepository;
import com.lselink.elvis.control.repository.ConnectorStatusRepository;
import com.lselink.elvis.control.repository.CorpRepository;
import com.lselink.elvis.control.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetApiServiceTest {

    @Mock
    private ChargerRepository chargerRepository;

    @Mock
    private ConnectorStatusRepository connectorStatusRepository;

    @Mock
    private StationRepository stationRepository;

    @Mock
    private CorpRepository corpRepository;

    @InjectMocks
    private ChargerService chargerService;

    @InjectMocks
    private StationService stationService;

    private StationEntity mockStation;
    private CorpEntity mockCorp;
    private ChargerEntity mockCharger;
    private ConnectorStatusEntity mockConnector1;
    private ConnectorStatusEntity mockConnector2;

    @BeforeEach
    void setUp() {
        mockCorp = new CorpEntity();
        mockCorp.setCorpId("CORP01");
        mockCorp.setCorpName("엘에스일렉트릭");
        mockCorp.setShortName("LS");
        mockCorp.setUseYn("Y");

        mockStation = new StationEntity();
        mockStation.setStId("000001");
        mockStation.setName("안양 R&D 충전소");
        mockStation.setCorpId("CORP01");
        mockStation.setChargerCount(2);

        mockCharger = new ChargerEntity();
        mockCharger.setChargeBoxId("00000101");
        mockCharger.setStId("000001");
        mockCharger.setCpId("01");
        mockCharger.setVendor("LSELINK");
        mockCharger.setModel("ELVIS-QC100");
        mockCharger.setSpec("급속 100kW (듀얼건)");
        mockCharger.setProtocol("ocpp1.6");

        mockConnector1 = new ConnectorStatusEntity();
        mockConnector1.setChargeBoxId("00000101");
        mockConnector1.setConnectorId(1);
        mockConnector1.setStatus("충전중");
        mockConnector1.setPowerKw(new BigDecimal("48.50"));
        mockConnector1.setVoltageV(new BigDecimal("380.00"));
        mockConnector1.setCurrentA(new BigDecimal("127.60"));
        mockConnector1.setSocPercent(65);
        mockConnector1.setBatteryTempC(32);
        mockConnector1.setCarModel("아이오닉 6");
        mockConnector1.setUserTag("RFID_LS_999");
        mockConnector1.setAccumulatedKwh(new BigDecimal("32.50"));
        mockConnector1.setChargingMinutes(28);
        mockConnector1.setLastHeartbeat(LocalDateTime.now());

        mockConnector2 = new ConnectorStatusEntity();
        mockConnector2.setChargeBoxId("00000101");
        mockConnector2.setConnectorId(2);
        mockConnector2.setStatus("충전대기");
        mockConnector2.setPowerKw(BigDecimal.ZERO);
        mockConnector2.setAccumulatedKwh(BigDecimal.ZERO);
        mockConnector2.setLastHeartbeat(LocalDateTime.now());
    }

    @Test
    @DisplayName("충전소 페이징 및 키워드 필터링 조회 검증")
    void testStationPagedSearch() {
        when(stationRepository.findAllByOrderByStIdAsc()).thenReturn(Collections.singletonList(mockStation));
        when(corpRepository.findAll()).thenReturn(Collections.singletonList(mockCorp));

        PageResponse<StationResponseDto> result = stationService.getPagedStations("ALL", "안양", 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("안양 R&D 충전소");
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("충전기 단건 상세 조회 및 듀얼 커넥터 목록 매핑 검증")
    void testChargerDetailWithDualConnectors() {
        when(chargerRepository.findById("00000101")).thenReturn(Optional.of(mockCharger));
        when(stationRepository.findById("000001")).thenReturn(Optional.of(mockStation));
        when(corpRepository.findById("CORP01")).thenReturn(Optional.of(mockCorp));
        when(connectorStatusRepository.findByChargeBoxId("00000101"))
                .thenReturn(Arrays.asList(mockConnector1, mockConnector2));

        ChargerResponseDto result = chargerService.getChargerById("00000101");

        assertThat(result.getChargeBoxId()).isEqualTo("00000101");
        assertThat(result.getStatus()).isEqualTo("충전중");
        assertThat(result.getStationName()).isEqualTo("안양 R&D 충전소");
        assertThat(result.getConnectors()).hasSize(2);
        assertThat(result.getConnectors().get(0).getConnectorId()).isEqualTo(1);
        assertThat(result.getConnectors().get(0).getStatus()).isEqualTo("충전중");
        assertThat(result.getConnectors().get(1).getConnectorId()).isEqualTo(2);
        assertThat(result.getConnectors().get(1).getStatus()).isEqualTo("충전대기");
    }

    @Test
    @DisplayName("충전기 실시간 운영 요약 통계 집계 검증")
    void testChargerSummaryKpi() {
        when(chargerRepository.findAllByOrderByChargeBoxIdAsc()).thenReturn(Collections.singletonList(mockCharger));
        when(stationRepository.findAll()).thenReturn(Collections.singletonList(mockStation));
        when(corpRepository.findAll()).thenReturn(Collections.singletonList(mockCorp));
        when(connectorStatusRepository.findAll()).thenReturn(Arrays.asList(mockConnector1, mockConnector2));

        ChargerSummaryDto summary = chargerService.getChargerSummary();

        assertThat(summary.getTotalCount()).isEqualTo(1);
        assertThat(summary.getChargingCount()).isEqualTo(1);
        assertThat(summary.getTotalPowerKw()).isEqualByComparingTo(new BigDecimal("48.50"));
        assertThat(summary.getTotalAccumulatedKwh()).isEqualByComparingTo(new BigDecimal("32.50"));
    }
}
