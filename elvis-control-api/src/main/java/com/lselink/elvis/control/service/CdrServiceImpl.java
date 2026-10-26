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
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 충전 거래 이력(CDR) 비즈니스 서비스 구현체
 * 거버넌스 9조(Clean Code & 변수 추출) 및 10조(채번 표준) 완벽 준수
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

        // 설명 변수 호이스팅 및 파라미터 추출
        String chargeBoxId = null;
        String userTag = null;
        String paymentStatus = null;
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (condition != null) {
            chargeBoxId = condition.getChargeBoxId();
            userTag = condition.getUserTag();
            paymentStatus = condition.getPaymentStatus();
            startDate = condition.getParsedStartDate();
            endDate = condition.getParsedEndDate();
        }

        // RDBMS 서버사이드 페이징 쿼리 실행
        Page<TransactionCdrEntity> cdrPage = transactionCdrRepository.searchCdrPaged(
                chargeBoxId, userTag, paymentStatus, startDate, endDate, pageable);

        List<TransactionCdrEntity> content = cdrPage.getContent();

        // 현재 페이지 레코드에 해당하는 충전소/충전기 마스터 캐시 맵 구성 (N+1 방지)
        Map<String, String> chargerToStationMap = getChargerToStationMap(content);
        Map<String, String> stationNameMap = getStationNameMap();

        List<CdrResponseDto> dtos = new ArrayList<>();
        for (TransactionCdrEntity entity : content) {
            CdrResponseDto dto = mapToResponseDto(entity, chargerToStationMap, stationNameMap);
            dtos.add(dto);
        }

        long totalElements = cdrPage.getTotalElements();
        int totalPages = cdrPage.getTotalPages();
        boolean isLast = cdrPage.isLast();

        return PageResponse.of(dtos, safePage, safeSize, totalElements, totalPages, isLast);
    }

    @Override
    public CdrSummaryDto getCdrSummary(CdrSearchCondition condition) {
        String chargeBoxId = null;
        String userTag = null;
        String paymentStatus = null;
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (condition != null) {
            chargeBoxId = condition.getChargeBoxId();
            userTag = condition.getUserTag();
            paymentStatus = condition.getPaymentStatus();
            startDate = condition.getParsedStartDate();
            endDate = condition.getParsedEndDate();
        }

        // RDBMS 단일 집계 쿼리 실행
        Object[] rawSummary = transactionCdrRepository.getCdrAggregateSummaryByCondition(
                chargeBoxId, userTag, paymentStatus, startDate, endDate);

        long totalCount = 0L;
        BigDecimal totalKwh = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (rawSummary != null && rawSummary.length > 0) {
            Object[] row;
            if (rawSummary[0] instanceof Object[]) {
                row = (Object[]) rawSummary[0];
            } else {
                row = rawSummary;
            }

            if (row.length >= 3) {
                if (row[0] instanceof Number) {
                    totalCount = ((Number) row[0]).longValue();
                }
                if (row[1] instanceof BigDecimal) {
                    totalKwh = (BigDecimal) row[1];
                } else if (row[1] instanceof Number) {
                    totalKwh = BigDecimal.valueOf(((Number) row[1]).doubleValue());
                }
                if (row[2] instanceof BigDecimal) {
                    totalAmount = (BigDecimal) row[2];
                } else if (row[2] instanceof Number) {
                    totalAmount = BigDecimal.valueOf(((Number) row[2]).doubleValue());
                }
            }
        }

        // 평균 충전 시간 산출 (조건 검색 결과 기준 계산)
        double avgMinutes = calculateAverageDurationMinutes(chargeBoxId, userTag, paymentStatus, startDate, endDate);

        return CdrSummaryDto.builder()
                .totalCount(totalCount)
                .totalKwh(totalKwh.setScale(2, RoundingMode.HALF_UP))
                .totalAmount(totalAmount.setScale(0, RoundingMode.HALF_UP))
                .avgMinutes(avgMinutes)
                .build();
    }

    @Override
    public List<CdrResponseDto> getAllFilteredCdr(CdrSearchCondition condition) {
        String chargeBoxId = null;
        String userTag = null;
        String paymentStatus = null;
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (condition != null) {
            chargeBoxId = condition.getChargeBoxId();
            userTag = condition.getUserTag();
            paymentStatus = condition.getPaymentStatus();
            startDate = condition.getParsedStartDate();
            endDate = condition.getParsedEndDate();
        }

        List<TransactionCdrEntity> list = transactionCdrRepository.findAllFiltered(
                chargeBoxId, userTag, paymentStatus, startDate, endDate);

        Map<String, String> chargerToStationMap = getChargerToStationMap(list);
        Map<String, String> stationNameMap = getStationNameMap();

        List<CdrResponseDto> dtos = new ArrayList<>();
        for (TransactionCdrEntity entity : list) {
            CdrResponseDto dto = mapToResponseDto(entity, chargerToStationMap, stationNameMap);
            dtos.add(dto);
        }

        return dtos;
    }

    /**
     * Entity -> CdrResponseDto 매핑 (거버넌스 9조, 10조 준수)
     */
    private CdrResponseDto mapToResponseDto(TransactionCdrEntity entity,
                                           Map<String, String> chargerToStationMap,
                                           Map<String, String> stationNameMap) {
        String boxId = entity.getChargeBoxId();

        // 충전소 식별자 도출 (거버넌스 10조: chargeBoxId 앞 6자리)
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

        BigDecimal kwh = entity.getTotalKwh();
        if (kwh == null) {
            kwh = BigDecimal.ZERO;
        }

        BigDecimal unitPrice = entity.getUnitPrice();
        if (unitPrice == null) {
            unitPrice = BigDecimal.ZERO;
        }

        BigDecimal totalAmount = entity.getTotalAmount();
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }

        String paymentStatus = entity.getPaymentStatus();
        if (paymentStatus == null || paymentStatus.isEmpty()) {
            paymentStatus = "UNPAID";
        }

        String stopReason = entity.getStopReason();
        if (stopReason == null) {
            stopReason = "-";
        }

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

    private double calculateAverageDurationMinutes(String chargeBoxId, String userTag, String paymentStatus,
                                                  LocalDateTime startDate, LocalDateTime endDate) {
        List<TransactionCdrEntity> list = transactionCdrRepository.findAllFiltered(
                chargeBoxId, userTag, paymentStatus, startDate, endDate);

        long totalSeconds = 0L;
        long completedCount = 0L;

        for (TransactionCdrEntity c : list) {
            LocalDateTime st = c.getStartTime();
            LocalDateTime sp = c.getStopTime();
            if (st != null && sp != null) {
                long sec = Duration.between(st, sp).getSeconds();
                if (sec >= 0) {
                    totalSeconds += sec;
                    completedCount++;
                }
            }
        }

        if (completedCount == 0L) {
            return 0.0;
        }

        double avgSec = (double) totalSeconds / completedCount;
        return Math.round((avgSec / 60.0) * 10.0) / 10.0;
    }
}
