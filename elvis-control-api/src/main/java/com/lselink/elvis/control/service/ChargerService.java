package com.lselink.elvis.control.service;

import com.lselink.elvis.control.dto.ChargerResponseDto;
import com.lselink.elvis.control.entity.*;
import com.lselink.elvis.control.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChargerService {

    private final ChargerRepository chargerRepository;
    private final ConnectorStatusRepository connectorStatusRepository;
    private final StationRepository stationRepository;
    private final CorpRepository corpRepository;

    public List<ChargerResponseDto> getChargers(String stId, String status) {
        List<ChargerEntity> chargers = (stId != null && !stId.equalsIgnoreCase("ALL"))
                ? chargerRepository.findByStIdOrderByCpIdAsc(stId)
                : chargerRepository.findAllByOrderByChargeBoxIdAsc();

        Map<String, StationEntity> stationMap = stationRepository.findAll().stream()
                .collect(Collectors.toMap(StationEntity::getStId, s -> s, (s1, s2) -> s1));

        Map<String, CorpEntity> corpMap = corpRepository.findAll().stream()
                .collect(Collectors.toMap(CorpEntity::getCorpId, c -> c, (c1, c2) -> c1));

        Map<String, ConnectorStatusEntity> statusMap = connectorStatusRepository.findAll().stream()
                .collect(Collectors.toMap(cs -> cs.getId().getChargeBoxId(), cs -> cs, (c1, c2) -> c1));

        return chargers.stream()
                .map(chg -> {
                    StationEntity st = stationMap.get(chg.getStId());
                    CorpEntity corp = (st != null) ? corpMap.get(st.getCorpId()) : null;
                    ConnectorStatusEntity conn = statusMap.get(chg.getChargeBoxId());

                    String curStatus = (conn != null) ? conn.getStatus() : "충전대기";

                    return ChargerResponseDto.builder()
                            .id(chg.getChargeBoxId() + "-1")
                            .chargeBoxId(chg.getChargeBoxId())
                            .stId(chg.getStId())
                            .stationName(st != null ? st.getName() : "")
                            .corpId(st != null ? st.getCorpId() : "")
                            .corpName(corp != null ? corp.getCorpName() : "")
                            .corpShortName(corp != null ? corp.getShortName() : "")
                            .cpId(chg.getCpId())
                            .connectorId(1)
                            .status(curStatus)
                            .spec(chg.getSpec())
                            .powerKw(conn != null && conn.getPowerKw() != null ? conn.getPowerKw() : BigDecimal.ZERO)
                            .voltageV(conn != null && conn.getVoltageV() != null ? conn.getVoltageV() : BigDecimal.ZERO)
                            .currentA(conn != null && conn.getCurrentA() != null ? conn.getCurrentA() : BigDecimal.ZERO)
                            .socPercent(conn != null && conn.getSocPercent() != null ? conn.getSocPercent() : 0)
                            .batteryTempC(conn != null && conn.getBatteryTempC() != null ? conn.getBatteryTempC() : 25)
                            .vendor(chg.getVendor())
                            .model(chg.getModel())
                            .carModel(conn != null ? conn.getCarModel() : null)
                            .userTag(conn != null ? conn.getUserTag() : null)
                            .protocol(chg.getProtocol())
                            .lastHeartbeat("방금 전 (실시간 DB)")
                            .accumulatedKwh(conn != null && conn.getAccumulatedKwh() != null ? conn.getAccumulatedKwh() : BigDecimal.ZERO)
                            .chargingMinutes(conn != null && conn.getChargingMinutes() != null ? conn.getChargingMinutes() : 0)
                            .build();
                })
                .filter(c -> status == null || status.equalsIgnoreCase("ALL") || c.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean executeRemoteAction(String chargeBoxId, String action) {
        // 실제 하향 제어 또는 커넥터 상태 갱신
        List<ConnectorStatusEntity> conns = connectorStatusRepository.findByIdChargeBoxId(chargeBoxId);
        if (!conns.isEmpty()) {
            ConnectorStatusEntity conn = conns.get(0);
            if ("RemoteStartTransaction".equalsIgnoreCase(action)) {
                conn.setStatus("충전중");
            } else if ("RemoteStopTransaction".equalsIgnoreCase(action)) {
                conn.setStatus("충전완료");
            } else if ("Reset".equalsIgnoreCase(action)) {
                conn.setStatus("충전대기");
            }
            connectorStatusRepository.save(conn);
        }
        return true;
    }
}
