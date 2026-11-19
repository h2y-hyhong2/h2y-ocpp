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
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final String STATUS_DISCONNECTED = "연결안됨";

    private final ChargerRepository chargerRepository;
    private final ConnectorStatusRepository connectorStatusRepository;
    private final StationRepository stationRepository;
    private final CorpRepository corpRepository;
    private final WsSessionClient wsSessionClient;

    public ChargerService(ChargerRepository chargerRepository,
                          ConnectorStatusRepository connectorStatusRepository,
                          StationRepository stationRepository,
                          CorpRepository corpRepository) {
        this(chargerRepository, connectorStatusRepository, stationRepository, corpRepository, null);
    }

    @Autowired
    public ChargerService(ChargerRepository chargerRepository,
                          ConnectorStatusRepository connectorStatusRepository,
                          StationRepository stationRepository,
                          CorpRepository corpRepository,
                          WsSessionClient wsSessionClient) {
        this.chargerRepository = chargerRepository;
        this.connectorStatusRepository = connectorStatusRepository;
        this.stationRepository = stationRepository;
        this.corpRepository = corpRepository;
        this.wsSessionClient = wsSessionClient;
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
        Set<String> activeSessionIds = resolveActiveSessionIds();

        List<ChargerResponseDto> resultList = new ArrayList<>();
        for (ChargerEntity chg : chargers) {
            StationEntity st = stationMap.get(chg.getStId());
            CorpEntity corp = (st != null) ? corpMap.get(st.getCorpId()) : null;
            List<ConnectorStatusEntity> conns = connectorGroupByBox.getOrDefault(chg.getChargeBoxId(), Collections.emptyList());

            ChargerResponseDto dto = toChargerResponseDto(chg, st, corp, conns, activeSessionIds);
            if (matchesStatusFilter(dto, status)) {
                resultList.add(dto);
            }
        }
        return resultList;
    }

    private boolean matchesStatusFilter(ChargerResponseDto dto, String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            return true;
        }
        if (status.equals(dto.getStatus())) {
            return true;
        }
        if (dto.getConnectors() != null) {
            return dto.getConnectors().stream().anyMatch(c -> status.equals(c.getStatus()));
        }
        return false;
    }

    public ChargerResponseDto getChargerById(String chargeBoxId) {
        ChargerEntity chg = chargerRepository.findById(chargeBoxId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 충전기입니다: " + chargeBoxId));

        StationEntity st = stationRepository.findById(chg.getStId()).orElse(null);
        CorpEntity corp = (st != null) ? corpRepository.findById(st.getCorpId()).orElse(null) : null;
        List<ConnectorStatusEntity> conns = connectorStatusRepository.findByChargeBoxId(chargeBoxId);

        return toChargerResponseDto(chg, st, corp, conns, resolveActiveSessionIds());
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
        Set<String> activeSessionIds = resolveActiveSessionIds();

        List<ChargerResponseDto> dtos = new ArrayList<>(content.size());
        for (ChargerEntity chg : content) {
            StationEntity st = stationMap.get(chg.getStId());
            CorpEntity corp = (st != null) ? corpMap.get(st.getCorpId()) : null;
            List<ConnectorStatusEntity> conns = connectorGroupByBox.getOrDefault(chg.getChargeBoxId(), Collections.emptyList());
            dtos.add(toChargerResponseDto(chg, st, corp, conns, activeSessionIds));
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
            } else if (st.equals("충전완료") || st.equalsIgnoreCase("Finishing") || st.startsWith("Suspended") || st.equalsIgnoreCase("Pause")) {
                completed++;
            } else if (st.equals("고장") || st.equalsIgnoreCase("Faulted") || st.equals("점검중") || st.equalsIgnoreCase("Unavailable") || st.equalsIgnoreCase("Maintenance")) {
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
        return executeRemoteAction(chargeBoxId, action, null);
    }

    @Transactional
    public boolean executeRemoteAction(String chargeBoxId, String action, Integer connectorId) {
        if (chargeBoxId == null || chargeBoxId.isBlank() || !chargerRepository.existsById(chargeBoxId)) {
            return false;
        }

        List<ConnectorStatusEntity> conns = connectorStatusRepository.findByChargeBoxId(chargeBoxId);
        if (conns.isEmpty()) {
            return false;
        }

        boolean isResetAction = "Reset".equalsIgnoreCase(action) || "RemoteReset".equalsIgnoreCase(action);
        String targetStatus;
        if ("RemoteStartTransaction".equalsIgnoreCase(action)) {
            targetStatus = "충전중";
        } else if ("RemoteStopTransaction".equalsIgnoreCase(action)) {
            targetStatus = "충전완료";
        } else if (isResetAction || "UnlockConnector".equalsIgnoreCase(action)) {
            targetStatus = "충전대기";
        } else {
            return false;
        }

        List<ConnectorStatusEntity> targets;
        if (!isResetAction && connectorId != null && connectorId > 0) {
            targets = conns.stream()
                    .filter(c -> connectorId.equals(c.getConnectorId()))
                    .toList();
            if (targets.isEmpty()) {
                return false;
            }
        } else {
            targets = conns;
        }

        for (ConnectorStatusEntity conn : targets) {
            conn.setStatus(targetStatus);
            connectorStatusRepository.save(conn);
        }
        return true;
    }

    private Set<String> resolveActiveSessionIds() {
        return wsSessionClient != null ? wsSessionClient.getActiveChargeBoxIds() : null;
    }

    private ChargerResponseDto toChargerResponseDto(ChargerEntity chg,
                                                    StationEntity st,
                                                    CorpEntity corp,
                                                    List<ConnectorStatusEntity> rawConns,
                                                    Set<String> activeSessionIds) {
        String boxId = chg.getChargeBoxId();
        boolean enforceOffline = (activeSessionIds != null && !activeSessionIds.contains(boxId));
        String stationName = (st != null && st.getName() != null) ? st.getName() : "";
        String corpId = (st != null && st.getCorpId() != null) ? st.getCorpId() : "";
        String corpName = (corp != null && corp.getCorpName() != null) ? corp.getCorpName() : "";
        String corpShortName = (corp != null && corp.getShortName() != null) ? corp.getShortName() : "";

        List<ConnectorStatusEntity> sortedConns = (rawConns == null || rawConns.isEmpty())
                ? Collections.emptyList()
                : rawConns.stream()
                        .sorted(Comparator.comparing(ConnectorStatusEntity::getConnectorId, Comparator.nullsLast(Integer::compareTo)))
                        .toList();

        // connectorId=0은 충전기 본체(Boot/Heartbeat) 레코드이므로 실제 충전건(>=1)이 있으면 분리
        List<ConnectorStatusEntity> channelConns = sortedConns.stream()
                .filter(c -> c.getConnectorId() != null && c.getConnectorId() > 0)
                .toList();
        List<ConnectorStatusEntity> displayConns = channelConns.isEmpty() ? sortedConns : channelConns;

        List<ConnectorDetailDto> connectorDtos = new ArrayList<>(displayConns.size());
        for (ConnectorStatusEntity c : displayConns) {
            String effectiveConnStatus = enforceOffline
                    ? STATUS_DISCONNECTED
                    : (c.getStatus() != null ? c.getStatus() : STATUS_DISCONNECTED);
            connectorDtos.add(ConnectorDetailDto.builder()
                    .connectorId(c.getConnectorId())
                    .status(effectiveConnStatus)
                    .powerKw(enforceOffline ? BigDecimal.ZERO : (c.getPowerKw() != null ? c.getPowerKw() : BigDecimal.ZERO))
                    .voltageV(enforceOffline ? BigDecimal.ZERO : (c.getVoltageV() != null ? c.getVoltageV() : BigDecimal.ZERO))
                    .currentA(enforceOffline ? BigDecimal.ZERO : (c.getCurrentA() != null ? c.getCurrentA() : BigDecimal.ZERO))
                    .socPercent(enforceOffline ? 0 : (c.getSocPercent() != null ? c.getSocPercent() : 0))
                    .batteryTempC(c.getBatteryTempC() != null ? c.getBatteryTempC() : 25)
                    .carModel(c.getCarModel())
                    .userTag(c.getUserTag())
                    .accumulatedKwh(c.getAccumulatedKwh() != null ? c.getAccumulatedKwh() : BigDecimal.ZERO)
                    .chargingMinutes(enforceOffline ? 0 : (c.getChargingMinutes() != null ? c.getChargingMinutes() : 0))
                    .lastHeartbeat(formatHeartbeat(c.getLastHeartbeat()))
                    .build());
        }

        ConnectorStatusEntity repConn = selectRepresentativeConnector(displayConns);
        int repConnectorId = (repConn != null && repConn.getConnectorId() != null) ? repConn.getConnectorId() : 1;
        String curStatus = enforceOffline
                ? STATUS_DISCONNECTED
                : ((repConn != null && repConn.getStatus() != null) ? repConn.getStatus() : STATUS_DISCONNECTED);
        boolean isConnected = (activeSessionIds != null)
                ? !enforceOffline
                : !STATUS_DISCONNECTED.equals(curStatus);
        BigDecimal powerKw = enforceOffline ? BigDecimal.ZERO : ((repConn != null && repConn.getPowerKw() != null) ? repConn.getPowerKw() : BigDecimal.ZERO);
        BigDecimal voltageV = enforceOffline ? BigDecimal.ZERO : ((repConn != null && repConn.getVoltageV() != null) ? repConn.getVoltageV() : BigDecimal.ZERO);
        BigDecimal currentA = enforceOffline ? BigDecimal.ZERO : ((repConn != null && repConn.getCurrentA() != null) ? repConn.getCurrentA() : BigDecimal.ZERO);
        int socPercent = enforceOffline ? 0 : ((repConn != null && repConn.getSocPercent() != null) ? repConn.getSocPercent() : 0);
        int batteryTempC = (repConn != null && repConn.getBatteryTempC() != null) ? repConn.getBatteryTempC() : 25;
        String carModel = (repConn != null) ? repConn.getCarModel() : null;
        String userTag = (repConn != null) ? repConn.getUserTag() : null;
        BigDecimal accumulatedKwh = (repConn != null && repConn.getAccumulatedKwh() != null) ? repConn.getAccumulatedKwh() : BigDecimal.ZERO;
        int chargingMinutes = enforceOffline ? 0 : ((repConn != null && repConn.getChargingMinutes() != null) ? repConn.getChargingMinutes() : 0);

        LocalDateTime latestHeartbeat = sortedConns.stream()
                .map(ConnectorStatusEntity::getLastHeartbeat)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        String repHeartbeat = formatHeartbeat(latestHeartbeat);

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
                .connected(isConnected)
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

    private ConnectorStatusEntity selectRepresentativeConnector(List<ConnectorStatusEntity> conns) {
        if (conns == null || conns.isEmpty()) {
            return null;
        }
        return conns.stream()
                .min(Comparator.comparingInt((ConnectorStatusEntity c) -> statusPriority(c.getStatus()))
                        .thenComparing(ConnectorStatusEntity::getConnectorId, Comparator.nullsLast(Integer::compareTo)))
                .orElse(conns.get(0));
    }

    private int statusPriority(String status) {
        if (status == null) {
            return 7;
        }
        return switch (status) {
            case "고장", "Faulted", "통신이상" -> 1;
            case "충전중", "Charging" -> 2;
            case "충전준비중", "Preparing", "일시중지", "SuspendedEV", "SuspendedEVSE", "Pause" -> 3;
            case "충전완료", "Finishing", "예약중", "Reserved" -> 4;
            case "점검중", "Unavailable", "Maintenance", "운영중지" -> 5;
            case "충전대기", "Available" -> 6;
            default -> 7;
        };
    }

    private String formatHeartbeat(LocalDateTime lastHeartbeat) {
        return lastHeartbeat != null ? lastHeartbeat.format(HEARTBEAT_FORMATTER) : "-";
    }
}
