package com.lselink.elvis.control.service;

import com.lselink.elvis.control.common.PageResponse;
import com.lselink.elvis.control.dto.ChargerResponseDto;
import com.lselink.elvis.control.dto.ChargerSummaryDto;
import com.lselink.elvis.control.dto.ConnectorDetailDto;
import com.lselink.elvis.control.entity.ChargerEntity;
import com.lselink.elvis.control.entity.ConnectorStatusEntity;
import com.lselink.elvis.control.entity.CorpEntity;
import com.lselink.elvis.control.entity.StationEntity;
import com.lselink.elvis.control.repository.ChargerRepository;
import com.lselink.elvis.control.repository.ConnectorStatusRepository;
import com.lselink.elvis.control.repository.CorpRepository;
import com.lselink.elvis.control.repository.StationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ChargerService {

    private final ChargerRepository chargerRepository;
    private final ConnectorStatusRepository connectorStatusRepository;
    private final StationRepository stationRepository;
    private final CorpRepository corpRepository;

    public ChargerService(ChargerRepository chargerRepository,
                          ConnectorStatusRepository connectorStatusRepository,
                          StationRepository stationRepository,
                          CorpRepository corpRepository) {
        this.chargerRepository = chargerRepository;
        this.connectorStatusRepository = connectorStatusRepository;
        this.stationRepository = stationRepository;
        this.corpRepository = corpRepository;
    }

    public List<ChargerResponseDto> getChargers(String stId, String status) {
        List<ChargerEntity> chargers;
        if (stId != null && !stId.equalsIgnoreCase("ALL")) {
            chargers = chargerRepository.findByStIdOrderByCpIdAsc(stId);
        } else {
            chargers = chargerRepository.findAllByOrderByChargeBoxIdAsc();
        }

        List<StationEntity> stationList = stationRepository.findAll();
        Map<String, StationEntity> stationMap = stationList.stream()
                .collect(Collectors.toMap(StationEntity::getStId, s -> s, (s1, s2) -> s1));

        List<CorpEntity> corpList = corpRepository.findAll();
        Map<String, CorpEntity> corpMap = corpList.stream()
                .collect(Collectors.toMap(CorpEntity::getCorpId, c -> c, (c1, c2) -> c1));

        List<ConnectorStatusEntity> connectorList = connectorStatusRepository.findAll();
        Map<String, List<ConnectorStatusEntity>> connectorGroupByBox = connectorList.stream()
                .collect(Collectors.groupingBy(ConnectorStatusEntity::getChargeBoxId));

        List<ChargerResponseDto> resultList = new ArrayList<>();

        for (ChargerEntity chg : chargers) {
            String boxId = chg.getChargeBoxId();
            StationEntity st = stationMap.get(chg.getStId());

            String stationName = "";
            String corpId = "";
            String corpName = "";
            String corpShortName = "";
            if (st != null) {
                stationName = st.getName();
                corpId = st.getCorpId();
                CorpEntity corp = corpMap.get(corpId);
                if (corp != null) {
                    corpName = corp.getCorpName();
                    corpShortName = corp.getShortName();
                }
            }

            List<ConnectorStatusEntity> conns = connectorGroupByBox.getOrDefault(boxId, Collections.emptyList());

            // 커넥터 상세 목록 변환 (듀얼 커넥터 지원)
            List<ConnectorDetailDto> connectorDtos = new ArrayList<>();
            for (ConnectorStatusEntity c : conns) {
                String cHeartbeat = "방금 전 (실시간 DB)";
                if (c.getLastHeartbeat() != null) {
                    cHeartbeat = c.getLastHeartbeat().toString();
                }

                ConnectorDetailDto cDto = ConnectorDetailDto.builder()
                        .connectorId(c.getConnectorId())
                        .status(c.getStatus())
                        .powerKw(c.getPowerKw() != null ? c.getPowerKw() : BigDecimal.ZERO)
                        .voltageV(c.getVoltageV() != null ? c.getVoltageV() : BigDecimal.ZERO)
                        .currentA(c.getCurrentA() != null ? c.getCurrentA() : BigDecimal.ZERO)
                        .socPercent(c.getSocPercent() != null ? c.getSocPercent() : 0)
                        .batteryTempC(c.getBatteryTempC() != null ? c.getBatteryTempC() : 25)
                        .carModel(c.getCarModel())
                        .userTag(c.getUserTag())
                        .accumulatedKwh(c.getAccumulatedKwh() != null ? c.getAccumulatedKwh() : BigDecimal.ZERO)
                        .chargingMinutes(c.getChargingMinutes() != null ? c.getChargingMinutes() : 0)
                        .lastHeartbeat(cHeartbeat)
                        .build();
                connectorDtos.add(cDto);
            }

            // 대표 커넥터(1번 또는 첫번째) 상태 추출
            ConnectorStatusEntity conn = null;
            if (!conns.isEmpty()) {
                conn = conns.get(0);
            }

            String curStatus = "연결안됨";
            BigDecimal powerKw = BigDecimal.ZERO;
            BigDecimal voltageV = BigDecimal.ZERO;
            BigDecimal currentA = BigDecimal.ZERO;
            int socPercent = 0;
            int batteryTempC = 25;
            String carModel = null;
            String userTag = null;
            BigDecimal accumulatedKwh = BigDecimal.ZERO;
            int chargingMinutes = 0;

            if (conn != null) {
                curStatus = conn.getStatus();
                if (conn.getPowerKw() != null) {
                    powerKw = conn.getPowerKw();
                }
                if (conn.getVoltageV() != null) {
                    voltageV = conn.getVoltageV();
                }
                if (conn.getCurrentA() != null) {
                    currentA = conn.getCurrentA();
                }
                if (conn.getSocPercent() != null) {
                    socPercent = conn.getSocPercent();
                }
                if (conn.getBatteryTempC() != null) {
                    batteryTempC = conn.getBatteryTempC();
                }
                carModel = conn.getCarModel();
                userTag = conn.getUserTag();
                if (conn.getAccumulatedKwh() != null) {
                    accumulatedKwh = conn.getAccumulatedKwh();
                }
                if (conn.getChargingMinutes() != null) {
                    chargingMinutes = conn.getChargingMinutes();
                }
            }

            // 상태 필터링
            boolean statusMatches = false;
            if (status == null || status.equalsIgnoreCase("ALL")) {
                statusMatches = true;
            } else if (status.equals(curStatus)) {
                statusMatches = true;
            }

            if (statusMatches) {
                ChargerResponseDto dto = ChargerResponseDto.builder()
                        .id(boxId + "-1")
                        .chargeBoxId(boxId)
                        .stId(chg.getStId())
                        .stationName(stationName)
                        .corpId(corpId)
                        .corpName(corpName)
                        .corpShortName(corpShortName)
                        .cpId(chg.getCpId())
                        .connectorId(1)
                        .status(curStatus)
                        .spec(chg.getSpec())
                        .powerKw(powerKw)
                        .voltageV(voltageV)
                        .currentA(currentA)
                        .socPercent(socPercent)
                        .batteryTempC(batteryTempC)
                        .vendor(chg.getVendor())
                        .model(chg.getModel())
                        .carModel(carModel)
                        .userTag(userTag)
                        .protocol(chg.getProtocol())
                        .lastHeartbeat("방금 전 (실시간 DB)")
                        .accumulatedKwh(accumulatedKwh)
                        .chargingMinutes(chargingMinutes)
                        .connectors(connectorDtos)
                        .build();
                resultList.add(dto);
            }
        }

        return resultList;
    }

    public ChargerResponseDto getChargerById(String chargeBoxId) {
        ChargerEntity chg = chargerRepository.findById(chargeBoxId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 충전기입니다: " + chargeBoxId));

        StationEntity st = stationRepository.findById(chg.getStId()).orElse(null);
        String stationName = "";
        String corpId = "";
        String corpName = "";
        String corpShortName = "";
        if (st != null) {
            stationName = st.getName();
            corpId = st.getCorpId();
            CorpEntity corp = corpRepository.findById(corpId).orElse(null);
            if (corp != null) {
                corpName = corp.getCorpName();
                corpShortName = corp.getShortName();
            }
        }

        List<ConnectorStatusEntity> conns = connectorStatusRepository.findByChargeBoxId(chargeBoxId);
        List<ConnectorDetailDto> connectorDtos = new ArrayList<>();
        for (ConnectorStatusEntity c : conns) {
            String cHeartbeat = "방금 전 (실시간 DB)";
            if (c.getLastHeartbeat() != null) {
                cHeartbeat = c.getLastHeartbeat().toString();
            }

            ConnectorDetailDto cDto = ConnectorDetailDto.builder()
                    .connectorId(c.getConnectorId())
                    .status(c.getStatus())
                    .powerKw(c.getPowerKw() != null ? c.getPowerKw() : BigDecimal.ZERO)
                    .voltageV(c.getVoltageV() != null ? c.getVoltageV() : BigDecimal.ZERO)
                    .currentA(c.getCurrentA() != null ? c.getCurrentA() : BigDecimal.ZERO)
                    .socPercent(c.getSocPercent() != null ? c.getSocPercent() : 0)
                    .batteryTempC(c.getBatteryTempC() != null ? c.getBatteryTempC() : 25)
                    .carModel(c.getCarModel())
                    .userTag(c.getUserTag())
                    .accumulatedKwh(c.getAccumulatedKwh() != null ? c.getAccumulatedKwh() : BigDecimal.ZERO)
                    .chargingMinutes(c.getChargingMinutes() != null ? c.getChargingMinutes() : 0)
                    .lastHeartbeat(cHeartbeat)
                    .build();
            connectorDtos.add(cDto);
        }

        ConnectorStatusEntity conn = null;
        if (!conns.isEmpty()) {
            conn = conns.get(0);
        }

        String curStatus = "연결안됨";
        BigDecimal powerKw = BigDecimal.ZERO;
        BigDecimal voltageV = BigDecimal.ZERO;
        BigDecimal currentA = BigDecimal.ZERO;
        int socPercent = 0;
        int batteryTempC = 25;
        String carModel = null;
        String userTag = null;
        BigDecimal accumulatedKwh = BigDecimal.ZERO;
        int chargingMinutes = 0;

        if (conn != null) {
            curStatus = conn.getStatus();
            if (conn.getPowerKw() != null) {
                powerKw = conn.getPowerKw();
            }
            if (conn.getVoltageV() != null) {
                voltageV = conn.getVoltageV();
            }
            if (conn.getCurrentA() != null) {
                currentA = conn.getCurrentA();
            }
            if (conn.getSocPercent() != null) {
                socPercent = conn.getSocPercent();
            }
            if (conn.getBatteryTempC() != null) {
                batteryTempC = conn.getBatteryTempC();
            }
            carModel = conn.getCarModel();
            userTag = conn.getUserTag();
            if (conn.getAccumulatedKwh() != null) {
                accumulatedKwh = conn.getAccumulatedKwh();
            }
            if (conn.getChargingMinutes() != null) {
                chargingMinutes = conn.getChargingMinutes();
            }
        }

        return ChargerResponseDto.builder()
                .id(chargeBoxId + "-1")
                .chargeBoxId(chargeBoxId)
                .stId(chg.getStId())
                .stationName(stationName)
                .corpId(corpId)
                .corpName(corpName)
                .corpShortName(corpShortName)
                .cpId(chg.getCpId())
                .connectorId(1)
                .status(curStatus)
                .spec(chg.getSpec())
                .powerKw(powerKw)
                .voltageV(voltageV)
                .currentA(currentA)
                .socPercent(socPercent)
                .batteryTempC(batteryTempC)
                .vendor(chg.getVendor())
                .model(chg.getModel())
                .carModel(carModel)
                .userTag(userTag)
                .protocol(chg.getProtocol())
                .lastHeartbeat("방금 전 (실시간 DB)")
                .accumulatedKwh(accumulatedKwh)
                .chargingMinutes(chargingMinutes)
                .connectors(connectorDtos)
                .build();
    }

    public PageResponse<ChargerResponseDto> getPagedChargers(String stId, String status, String keyword, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? 10 : size;

        List<ChargerResponseDto> all = getChargers(stId, status);

        List<ChargerResponseDto> filtered = new ArrayList<>();
        String trimmedKeyword = (keyword != null) ? keyword.trim().toLowerCase() : "";

        for (ChargerResponseDto c : all) {
            if (trimmedKeyword.isEmpty()) {
                filtered.add(c);
            } else {
                boolean matchBox = c.getChargeBoxId() != null && c.getChargeBoxId().toLowerCase().contains(trimmedKeyword);
                boolean matchStName = c.getStationName() != null && c.getStationName().toLowerCase().contains(trimmedKeyword);
                boolean matchCorp = c.getCorpName() != null && c.getCorpName().toLowerCase().contains(trimmedKeyword);
                boolean matchVendor = c.getVendor() != null && c.getVendor().toLowerCase().contains(trimmedKeyword);
                boolean matchModel = c.getModel() != null && c.getModel().toLowerCase().contains(trimmedKeyword);

                if (matchBox || matchStName || matchCorp || matchVendor || matchModel) {
                    filtered.add(c);
                }
            }
        }

        long totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);
        if (totalPages == 0) {
            totalPages = 1;
        }

        int fromIndex = safePage * safeSize;
        List<ChargerResponseDto> pageContent;
        if (fromIndex >= totalElements) {
            pageContent = new ArrayList<>();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, (int) totalElements);
            pageContent = new ArrayList<>(filtered.subList(fromIndex, toIndex));
        }

        boolean isLast = (safePage >= totalPages - 1);
        return PageResponse.of(pageContent, safePage, safeSize, totalElements, totalPages, isLast);
    }

    public ChargerSummaryDto getChargerSummary() {
        List<ChargerResponseDto> chargers = getChargers("ALL", "ALL");

        long total = chargers.size();
        long available = 0;
        long charging = 0;
        long completed = 0;
        long faulted = 0;
        long disconnected = 0;

        BigDecimal totalPowerKw = BigDecimal.ZERO;
        BigDecimal totalAccumulatedKwh = BigDecimal.ZERO;

        for (ChargerResponseDto c : chargers) {
            String st = c.getStatus();
            if (st == null) {
                disconnected++;
            } else if (st.equals("충전대기") || st.equalsIgnoreCase("Available")) {
                available++;
            } else if (st.equals("충전중") || st.equalsIgnoreCase("Charging")) {
                charging++;
            } else if (st.equals("충전완료") || st.equalsIgnoreCase("Finishing") || st.startsWith("Suspended")) {
                completed++;
            } else if (st.equals("고장") || st.equalsIgnoreCase("Faulted") || st.equals("점검중") || st.equalsIgnoreCase("Unavailable")) {
                faulted++;
            } else {
                disconnected++;
            }

            if (c.getPowerKw() != null) {
                totalPowerKw = totalPowerKw.add(c.getPowerKw());
            }
            if (c.getAccumulatedKwh() != null) {
                totalAccumulatedKwh = totalAccumulatedKwh.add(c.getAccumulatedKwh());
            }
        }

        return ChargerSummaryDto.builder()
                .totalCount(total)
                .availableCount(available)
                .chargingCount(charging)
                .completedCount(completed)
                .faultedCount(faulted)
                .disconnectedCount(disconnected)
                .totalPowerKw(totalPowerKw)
                .totalAccumulatedKwh(totalAccumulatedKwh)
                .build();
    }

    @Transactional
    public boolean executeRemoteAction(String chargeBoxId, String action) {
        List<ConnectorStatusEntity> conns = connectorStatusRepository.findByChargeBoxId(chargeBoxId);
        if (!conns.isEmpty()) {
            for (ConnectorStatusEntity conn : conns) {
                if ("RemoteStartTransaction".equalsIgnoreCase(action)) {
                    conn.setStatus("충전중");
                } else if ("RemoteStopTransaction".equalsIgnoreCase(action)) {
                    conn.setStatus("충전완료");
                } else if ("Reset".equalsIgnoreCase(action) || "UnlockConnector".equalsIgnoreCase(action)) {
                    conn.setStatus("충전대기");
                }
                connectorStatusRepository.save(conn);
            }
        }
        return true;
    }
}
