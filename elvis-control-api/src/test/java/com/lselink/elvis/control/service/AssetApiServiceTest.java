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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

        LocalDateTime heartbeatTime = LocalDateTime.of(2026, 11, 16, 10, 30, 0);

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
        mockConnector1.setLastHeartbeat(heartbeatTime);

        mockConnector2 = new ConnectorStatusEntity();
        mockConnector2.setChargeBoxId("00000101");
        mockConnector2.setConnectorId(2);
        mockConnector2.setStatus("충전대기");
        mockConnector2.setPowerKw(new BigDecimal("10.00"));
        mockConnector2.setAccumulatedKwh(new BigDecimal("5.00"));
        mockConnector2.setLastHeartbeat(heartbeatTime);
    }

    @Test
    @DisplayName("충전소 QueryDSL 페이징 및 키워드 필터링 조회 검증")
    void testStationPagedSearch() {
        Page<StationEntity> page = new PageImpl<>(Collections.singletonList(mockStation), PageRequest.of(0, 10), 1L);
        when(stationRepository.searchStations(eq("ALL"), eq("안양"), any(Pageable.class))).thenReturn(page);
        when(corpRepository.findAllById(any())).thenReturn(Collections.singletonList(mockCorp));

        PageResponse<StationResponseDto> result = stationService.getPagedStations("ALL", "안양", 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("안양 R&D 충전소");
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("충전기 QueryDSL 페이징 조회 및 빈 결과 totalPages=0 정합성 검증")
    void testChargerPagedSearchAndEmptyPage() {
        Page<ChargerEntity> page = new PageImpl<>(Collections.singletonList(mockCharger), PageRequest.of(0, 10), 1L);
        when(chargerRepository.searchChargers(eq("ALL"), eq("ALL"), eq("00000101"), any(Pageable.class)))
                .thenReturn(page);
        when(stationRepository.findAllById(any())).thenReturn(Collections.singletonList(mockStation));
        when(corpRepository.findAllById(any())).thenReturn(Collections.singletonList(mockCorp));
        when(connectorStatusRepository.findByChargeBoxIdIn(any()))
                .thenReturn(Arrays.asList(mockConnector2, mockConnector1));

        PageResponse<ChargerResponseDto> result = chargerService.getPagedChargers("ALL", "ALL", "00000101", 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getConnectorId()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);

        when(chargerRepository.searchChargers(eq("ALL"), eq("ALL"), eq("NOT_FOUND"), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 10)));
        PageResponse<ChargerResponseDto> emptyResult = chargerService.getPagedChargers("ALL", "ALL", "NOT_FOUND", 0, 10);
        assertThat(emptyResult.getContent()).isEmpty();
        assertThat(emptyResult.getTotalPages()).isEqualTo(0);
        assertThat(emptyResult.isLast()).isTrue();
    }

    @Test
    @DisplayName("충전기 단건 상세 조회 시 커넥터 ID 오름차순 정렬 및 실제 하트비트 타임스탬프 매핑 검증")
    void testChargerDetailWithDualConnectors() {
        when(chargerRepository.findById("00000101")).thenReturn(Optional.of(mockCharger));
        when(stationRepository.findById("000001")).thenReturn(Optional.of(mockStation));
        when(corpRepository.findById("CORP01")).thenReturn(Optional.of(mockCorp));
        // 의도적으로 역순 반환하여 정렬 검증
        when(connectorStatusRepository.findByChargeBoxId("00000101"))
                .thenReturn(Arrays.asList(mockConnector2, mockConnector1));

        ChargerResponseDto result = chargerService.getChargerById("00000101");

        assertThat(result.getChargeBoxId()).isEqualTo("00000101");
        assertThat(result.getConnectorId()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo("충전중");
        assertThat(result.getLastHeartbeat()).isEqualTo("2026-11-16 10:30:00");
        assertThat(result.getStationName()).isEqualTo("안양 R&D 충전소");
        assertThat(result.getConnectors()).hasSize(2);
        assertThat(result.getConnectors().get(0).getConnectorId()).isEqualTo(1);
        assertThat(result.getConnectors().get(0).getStatus()).isEqualTo("충전중");
        assertThat(result.getConnectors().get(1).getConnectorId()).isEqualTo(2);
        assertThat(result.getConnectors().get(1).getStatus()).isEqualTo("충전대기");
    }

    @Test
    @DisplayName("충전기 실시간 운영 요약 통계 집계 시 모든 커넥터 전력/충전량 합산 검증")
    void testChargerSummaryKpi() {
        when(chargerRepository.findAllByOrderByChargeBoxIdAsc()).thenReturn(Collections.singletonList(mockCharger));
        when(stationRepository.findAll()).thenReturn(Collections.singletonList(mockStation));
        when(corpRepository.findAll()).thenReturn(Collections.singletonList(mockCorp));
        when(connectorStatusRepository.findAll()).thenReturn(Arrays.asList(mockConnector1, mockConnector2));

        ChargerSummaryDto summary = chargerService.getChargerSummary();

        assertThat(summary.getTotalCount()).isEqualTo(1);
        assertThat(summary.getChargingCount()).isEqualTo(1);
        assertThat(summary.getTotalPowerKw()).isEqualByComparingTo(new BigDecimal("58.50"));
        assertThat(summary.getTotalAccumulatedKwh()).isEqualByComparingTo(new BigDecimal("37.50"));
    }

    @Test
    @DisplayName("원격 제어 액션 실행 시 RemoteReset 지원 및 미존재 충전기 false 반환 검증")
    void testExecuteRemoteAction() {
        when(chargerRepository.existsById("00000101")).thenReturn(true);
        when(chargerRepository.existsById("99999999")).thenReturn(false);
        when(connectorStatusRepository.findByChargeBoxId("00000101"))
                .thenReturn(Arrays.asList(mockConnector1, mockConnector2));

        boolean resetResult = chargerService.executeRemoteAction("00000101", "RemoteReset");
        boolean missingResult = chargerService.executeRemoteAction("99999999", "Reset");

        assertThat(resetResult).isTrue();
        assertThat(mockConnector1.getStatus()).isEqualTo("충전대기");
        assertThat(mockConnector2.getStatus()).isEqualTo("충전대기");
        assertThat(missingResult).isFalse();
        verify(connectorStatusRepository, org.mockito.Mockito.times(2)).save(any(ConnectorStatusEntity.class));
    }

    @Test
    @DisplayName("0번 본체 레코드 분리 및 2번 커넥터 충전중 우선순위 대표 상태 반영 검증")
    void testConnectorPriorityAndBodySeparation() {
        ConnectorStatusEntity bodyConn = new ConnectorStatusEntity();
        bodyConn.setChargeBoxId("00000101");
        bodyConn.setConnectorId(0);
        bodyConn.setStatus("Available");

        mockConnector1.setStatus("Available");
        mockConnector2.setStatus("Charging");

        when(chargerRepository.findById("00000101")).thenReturn(Optional.of(mockCharger));
        when(stationRepository.findById("000001")).thenReturn(Optional.of(mockStation));
        when(corpRepository.findById("CORP01")).thenReturn(Optional.of(mockCorp));
        when(connectorStatusRepository.findByChargeBoxId("00000101"))
                .thenReturn(Arrays.asList(bodyConn, mockConnector1, mockConnector2));

        ChargerResponseDto result = chargerService.getChargerById("00000101");

        assertThat(result.getConnectors()).hasSize(2);
        assertThat(result.getConnectors()).extracting("connectorId").containsExactly(1, 2);
        assertThat(result.getConnectorId()).isEqualTo(2);
        assertThat(result.getStatus()).isEqualTo("Charging");
    }

    @Test
    @DisplayName("특정 커넥터 ID 지정 원격 제어 시 해당 커넥터 상태만 변경 검증")
    void testExecuteRemoteActionForSpecificConnector() {
        mockConnector1.setStatus("충전대기");
        mockConnector2.setStatus("충전대기");

        when(chargerRepository.existsById("00000101")).thenReturn(true);
        when(connectorStatusRepository.findByChargeBoxId("00000101"))
                .thenReturn(Arrays.asList(mockConnector1, mockConnector2));

        boolean result = chargerService.executeRemoteAction("00000101", "RemoteStartTransaction", 2);

        assertThat(result).isTrue();
        assertThat(mockConnector1.getStatus()).isEqualTo("충전대기");
        assertThat(mockConnector2.getStatus()).isEqualTo("충전중");
        verify(connectorStatusRepository, org.mockito.Mockito.times(1)).save(mockConnector2);
    }

    @Test
    @DisplayName("WS 활성 세션이 없는 충전기는 DB 상태가 Available이어도 연결안됨 및 출력 0으로 반환 검증")
    void testOfflineChargerMarkedDisconnectedWhenNoActiveWsSession() {
        WsSessionClient mockWsSessionClient = org.mockito.Mockito.mock(WsSessionClient.class);
        when(mockWsSessionClient.getActiveChargeBoxIds()).thenReturn(Collections.emptySet());

        ChargerService wsAwareService = new ChargerService(
                chargerRepository, connectorStatusRepository, stationRepository, corpRepository, mockWsSessionClient);

        mockConnector1.setStatus("Available");
        mockConnector2.setStatus("Available");

        when(chargerRepository.findById("00000101")).thenReturn(Optional.of(mockCharger));
        when(stationRepository.findById("000001")).thenReturn(Optional.of(mockStation));
        when(corpRepository.findById("CORP01")).thenReturn(Optional.of(mockCorp));
        when(connectorStatusRepository.findByChargeBoxId("00000101"))
                .thenReturn(Arrays.asList(mockConnector1, mockConnector2));

        ChargerResponseDto result = wsAwareService.getChargerById("00000101");

        assertThat(result.getConnected()).isFalse();
        assertThat(result.getStatus()).isEqualTo("연결안됨");
        assertThat(result.getPowerKw()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getConnectors()).hasSize(2);
        assertThat(result.getConnectors()).allSatisfy(conn -> {
            assertThat(conn.getStatus()).isEqualTo("연결안됨");
            assertThat(conn.getPowerKw()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }
}
