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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 충전 거래 이력(CDR) 비즈니스 서비스 구현체
 */
@Service
@Transactional(readOnly = true)
public class CdrServiceImpl implements CdrService {

    private final TransactionCdrRepository transactionCdrRepository;
    private final StationRepository stationRepository;
    private final ChargerRepository chargerRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CdrServiceImpl(TransactionCdrRepository transactionCdrRepository,
                          StationRepository stationRepository,
                          ChargerRepository chargerRepository) {
        this.transactionCdrRepository = transactionCdrRepository;
        this.stationRepository = stationRepository;
        this.chargerRepository = chargerRepository;
    }

    @Override
    public PageResponse<CdrResponseDto> getPagedCdr(CdrSearchCondition condition, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = 20;
        if (size > 0) {
            safeSize = Math.min(size, 100);
        }

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<TransactionCdrEntity> cdrPage = transactionCdrRepository.searchCdr(condition, pageable);
        List<TransactionCdrEntity> content = cdrPage.getContent();

        Map<String, String> chargerToStationMap = getChargerToStationMap(content);
        Map<String, String> stationNameMap = getStationNameMap();

        List<CdrResponseDto> dtos = new ArrayList<>();
        for (TransactionCdrEntity entity : content) {
            dtos.add(mapToResponseDto(entity, chargerToStationMap, stationNameMap));
        }

        return PageResponse.of(
                dtos,
                safePage,
                safeSize,
                cdrPage.getTotalElements(),
                cdrPage.getTotalPages(),
                cdrPage.isLast()
        );
    }

    @Override
    public CdrSummaryDto getCdrSummary(CdrSearchCondition condition) {
        return transactionCdrRepository.getCdrSummary(condition);
    }

    @Override
    public List<CdrResponseDto> getAllFilteredCdr(CdrSearchCondition condition) {
        List<TransactionCdrEntity> list = transactionCdrRepository.findAllByCondition(condition);

        Map<String, String> chargerToStationMap = getChargerToStationMap(list);
        Map<String, String> stationNameMap = getStationNameMap();

        List<CdrResponseDto> dtos = new ArrayList<>();
        for (TransactionCdrEntity entity : list) {
            dtos.add(mapToResponseDto(entity, chargerToStationMap, stationNameMap));
        }

        return dtos;
    }

    private CdrResponseDto mapToResponseDto(TransactionCdrEntity entity,
                                            Map<String, String> chargerToStationMap,
                                            Map<String, String> stationNameMap) {
        String boxId = entity.getChargeBoxId();

        String stId = chargerToStationMap.get(boxId);
        if (stId == null || stId.isEmpty()) {
            if (boxId != null && boxId.length() >= 6) {
                stId = boxId.substring(0, 6);
            } else {
                stId = "000000";
            }
        }

        String stationName = stationNameMap.get(stId);
        if (stationName == null || stationName.isEmpty()) {
            stationName = "CSMS Lite 거점 충전소 (" + stId + ")";
        }

        String startTimeStr = "";
        LocalDateTime startTime = entity.getStartTime();
        if (startTime != null) {
            startTimeStr = startTime.format(DATE_TIME_FORMATTER);
        }

        String stopTimeStr = "진행중";
        Double durationMinutes = null;
        LocalDateTime stopTime = entity.getStopTime();
        if (stopTime != null) {
            stopTimeStr = stopTime.format(DATE_TIME_FORMATTER);
            if (startTime != null) {
                long seconds = Duration.between(startTime, stopTime).getSeconds();
                durationMinutes = Math.round((seconds / 60.0) * 10.0) / 10.0;
            }
        }

        BigDecimal kwh = entity.getTotalKwh() != null ? entity.getTotalKwh() : BigDecimal.ZERO;
        BigDecimal unitPrice = entity.getUnitPrice() != null ? entity.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal totalAmount = entity.getTotalAmount() != null ? entity.getTotalAmount() : BigDecimal.ZERO;

        String paymentStatus = entity.getPaymentStatus();
        if (paymentStatus == null || paymentStatus.isEmpty()) {
            paymentStatus = "UNPAID";
        }

        String stopReason = entity.getStopReason() != null ? entity.getStopReason() : "-";

        return CdrResponseDto.builder()
                .transactionId(entity.getTransactionId())
                .chargeBoxId(boxId)
                .stationId(stId)
                .stationName(stationName)
                .connectorId(entity.getConnectorId())
                .userTag(entity.getUserTag())
                .startTime(startTimeStr)
                .stopTime(stopTimeStr)
                .kwh(kwh)
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .paymentStatus(paymentStatus)
                .stopReason(stopReason)
                .durationMinutes(durationMinutes)
                .build();
    }

    private Map<String, String> getChargerToStationMap(List<TransactionCdrEntity> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> boxIds = list.stream()
                .map(TransactionCdrEntity::getChargeBoxId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<ChargerEntity> chargers = chargerRepository.findAllById(boxIds);
        Map<String, String> map = new HashMap<>();
        for (ChargerEntity c : chargers) {
            map.put(c.getChargeBoxId(), c.getStId());
        }
        return map;
    }

    private Map<String, String> getStationNameMap() {
        List<StationEntity> stations = stationRepository.findAll();
        Map<String, String> map = new HashMap<>();
        for (StationEntity s : stations) {
            map.put(s.getStId(), s.getName());
        }
        return map;
    }
}
