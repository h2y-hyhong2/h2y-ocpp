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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ChargerService {

    private static final DateTimeFormatter HEARTBEAT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

        Map<String, StationEntity> stationMap = stationRepository.findAll().stream()
                .collect(Collectors.toMap(StationEntity::getStId, s -> s, (s1, s2) -> s1));
        Map<String, CorpEntity> corpMap = corpRepository.findAll().stream()
                .collect(Collectors.toMap(CorpEntity::getCorpId, c -> c, (c1, c2) -> c1));
        Map<String, List<ConnectorStatusEntity>> connectorGroupByBox = connectorStatusRepository.findAll().stream()
                .collect(Collectors.groupingBy(ConnectorStatusEntity::getChargeBoxId));

        List<ChargerResponseDto> resultList = new ArrayList<>();
        for (ChargerEntity chg : chargers) {
            StationEntity st = stationMap.get(chg.getStId());
            CorpEntity corp = (st != null) ? corpMap.get(st.getCorpId()) : null;
            List<ConnectorStatusEntity> conns = connectorGroupByBox.getOrDefault(chg.getChargeBoxId(), Collections.emptyList());

            ChargerResponseDto dto = toChargerResponseDto(chg, st, corp, conns);
            if (status == null || status.equalsIgnoreCase("ALL") || status.equals(dto.getStatus())) {
                resultList.add(dto);
            }
        }
        return resultList;
    }

    public ChargerResponseDto getChargerById(String chargeBoxId) {
        ChargerEntity chg = chargerRepository.findById(chargeBoxId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 충전기입니다: " + chargeBoxId));

        StationEntity st = stationRepository.findById(chg.getStId()).orElse(null);
        CorpEntity corp = (st != null) ? corpRepository.findById(st.getCorpId()).orElse(null) : null;
        List<ConnectorStatusEntity> conns = connectorStatusRepository.findByChargeBoxId(chargeBoxId);

        return toChargerResponseDto(chg, st, corp, conns);
    }

    public PageResponse<ChargerResponseDto> getPagedChargers(String stId, String status, String keyword, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? 10 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<ChargerEntity> chargerPage = chargerRepository.searchChargers(stId, status, keyword, pageable);
        List<ChargerEntity> content = chargerPage.getContent();

        if (content.isEmpty()) {
            return PageResponse.of(Collections.emptyList(), safePage, safeSize,
                    chargerPage.getTotalElements(), chargerPage.getTotalPages(), chargerPage.isLast());
        }

        Set<String> stIds = content.stream()
                .map(ChargerEntity::getStId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, StationEntity> stationMap = stationRepository.findAllById(stIds).stream()
                .collect(Collectors.toMap(StationEntity::getStId, s -> s, (s1, s2) -> s1));

        Set<String> corpIds = stationMap.values().stream()
                .map(StationEntity::getCorpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, CorpEntity> corpMap = corpRepository.findAllById(corpIds).stream()
                .collect(Collectors.toMap(CorpEntity::getCorpId, c -> c, (c1, c2) -> c1));

        Set<String> boxIds = content.stream()
                .map(ChargerEntity::getChargeBoxId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, List<ConnectorStatusEntity>> connectorGroupByBox = connectorStatusRepository.findByChargeBoxIdIn(boxIds).stream()
                .collect(Collectors.groupingBy(ConnectorStatusEntity::getChargeBoxId));

        List<ChargerResponseDto> dtos = new ArrayList<>(content.size());
        for (ChargerEntity chg : content) {
            StationEntity st = stationMap.get(chg.getStId());
            CorpEntity corp = (st != null) ? corpMap.get(st.getCorpId()) : null;
            List<ConnectorStatusEntity> conns = connectorGroupByBox.getOrDefault(chg.getChargeBoxId(), Collections.emptyList());
            dtos.add(toChargerResponseDto(chg, st, corp, conns));
        }

        return PageResponse.of(dtos, safePage, safeSize,
                chargerPage.getTotalElements(), chargerPage.getTotalPages(), chargerPage.isLast());
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

            List<ConnectorDetailDto> connectors = c.getConnectors();
            if (connectors != null && !connectors.isEmpty()) {
                for (ConnectorDetailDto conn : connectors) {
                    if (conn.getPowerKw() != null) {
                        totalPowerKw = totalPowerKw.add(conn.getPowerKw());
                    }
                    if (conn.getAccumulatedKwh() != null) {
                        totalAccumulatedKwh = totalAccumulatedKwh.add(conn.getAccumulatedKwh());
                    }
                }
            } else {
                if (c.getPowerKw() != null) {
                    totalPowerKw = totalPowerKw.add(c.getPowerKw());
                }
                if (c.getAccumulatedKwh() != null) {
                    totalAccumulatedKwh = totalAccumulatedKwh.add(c.getAccumulatedKwh());
                }
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
        if (chargeBoxId == null || chargeBoxId.isBlank() || !chargerRepository.existsById(chargeBoxId)) {
            return false;
        }

        List<ConnectorStatusEntity> conns = connectorStatusRepository.findByChargeBoxId(chargeBoxId);
        if (conns.isEmpty()) {
            return false;
        }

        String targetStatus;
        if ("RemoteStartTransaction".equalsIgnoreCase(action)) {
            targetStatus = "충전중";
        } else if ("RemoteStopTransaction".equalsIgnoreCase(action)) {
            targetStatus = "충전완료";
        } else if ("Reset".equalsIgnoreCase(action)
                || "RemoteReset".equalsIgnoreCase(action)
                || "UnlockConnector".equalsIgnoreCase(action)) {
            targetStatus = "충전대기";
        } else {
            return false;
        }

        for (ConnectorStatusEntity conn : conns) {
            conn.setStatus(targetStatus);
            connectorStatusRepository.save(conn);
        }
        return true;
    }

    private ChargerResponseDto toChargerResponseDto(ChargerEntity chg,
                                                    StationEntity st,
                                                    CorpEntity corp,
                                                    List<ConnectorStatusEntity> rawConns) {
        String boxId = chg.getChargeBoxId();
        String stationName = (st != null && st.getName() != null) ? st.getName() : "";
        String corpId = (st != null && st.getCorpId() != null) ? st.getCorpId() : "";
        String corpName = (corp != null && corp.getCorpName() != null) ? corp.getCorpName() : "";
        String corpShortName = (corp != null && corp.getShortName() != null) ? corp.getShortName() : "";

        List<ConnectorStatusEntity> sortedConns = (rawConns == null || rawConns.isEmpty())
                ? Collections.emptyList()
                : rawConns.stream()
                        .sorted(Comparator.comparing(ConnectorStatusEntity::getConnectorId, Comparator.nullsLast(Integer::compareTo)))
                        .toList();

        List<ConnectorDetailDto> connectorDtos = new ArrayList<>(sortedConns.size());
        for (ConnectorStatusEntity c : sortedConns) {
            connectorDtos.add(ConnectorDetailDto.builder()
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
                    .lastHeartbeat(formatHeartbeat(c.getLastHeartbeat()))
                    .build());
        }

        ConnectorStatusEntity repConn = sortedConns.isEmpty() ? null : sortedConns.get(0);
        int repConnectorId = (repConn != null && repConn.getConnectorId() != null) ? repConn.getConnectorId() : 1;
        String curStatus = (repConn != null && repConn.getStatus() != null) ? repConn.getStatus() : "연결안됨";
        BigDecimal powerKw = (repConn != null && repConn.getPowerKw() != null) ? repConn.getPowerKw() : BigDecimal.ZERO;
        BigDecimal voltageV = (repConn != null && repConn.getVoltageV() != null) ? repConn.getVoltageV() : BigDecimal.ZERO;
        BigDecimal currentA = (repConn != null && repConn.getCurrentA() != null) ? repConn.getCurrentA() : BigDecimal.ZERO;
        int socPercent = (repConn != null && repConn.getSocPercent() != null) ? repConn.getSocPercent() : 0;
        int batteryTempC = (repConn != null && repConn.getBatteryTempC() != null) ? repConn.getBatteryTempC() : 25;
        String carModel = (repConn != null) ? repConn.getCarModel() : null;
        String userTag = (repConn != null) ? repConn.getUserTag() : null;
        BigDecimal accumulatedKwh = (repConn != null && repConn.getAccumulatedKwh() != null) ? repConn.getAccumulatedKwh() : BigDecimal.ZERO;
        int chargingMinutes = (repConn != null && repConn.getChargingMinutes() != null) ? repConn.getChargingMinutes() : 0;
        String repHeartbeat = (repConn != null) ? formatHeartbeat(repConn.getLastHeartbeat()) : "-";

        return ChargerResponseDto.builder()
                .id(boxId + "-" + repConnectorId)
                .chargeBoxId(boxId)
                .stId(chg.getStId())
                .stationName(stationName)
                .corpId(corpId)
                .corpName(corpName)
                .corpShortName(corpShortName)
                .cpId(chg.getCpId())
                .connectorId(repConnectorId)
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
                .lastHeartbeat(repHeartbeat)
                .accumulatedKwh(accumulatedKwh)
                .chargingMinutes(chargingMinutes)
                .connectors(connectorDtos)
                .build();
    }

    private String formatHeartbeat(LocalDateTime lastHeartbeat) {
        return lastHeartbeat != null ? lastHeartbeat.format(HEARTBEAT_FORMATTER) : "-";
    }
}
