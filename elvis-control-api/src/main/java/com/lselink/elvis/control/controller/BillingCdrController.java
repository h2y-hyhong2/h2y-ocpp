package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.entity.StationEntity;
import com.lselink.elvis.control.entity.TransactionCdrEntity;
import com.lselink.elvis.control.repository.StationRepository;
import com.lselink.elvis.control.repository.TransactionCdrRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/cdr")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BillingCdrController {

    private final TransactionCdrRepository transactionCdrRepository;
    private final StationRepository stationRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Data
    @Builder
    public static class CdrItemDto {
        private Long transactionId;
        private String userTag;
        private String chargeBoxId;
        private String stationName;
        private String startTime;
        private String stopTime;
        private BigDecimal kwh;
        private BigDecimal unitPrice;
        private BigDecimal totalAmount;
        private String paymentStatus;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCdrRecords() {
        List<TransactionCdrEntity> list = transactionCdrRepository.findAllByOrderByStartTimeDesc();

        Map<String, String> stationNameMap = stationRepository.findAll().stream()
                .collect(Collectors.toMap(StationEntity::getStId, StationEntity::getName, (s1, s2) -> s1));

        List<CdrItemDto> items = list.stream().map(c -> {
            String stId = c.getChargeBoxId().length() >= 6 ? "10" + c.getChargeBoxId().substring(3, 6) : "10001";
            String stationName = stationNameMap.getOrDefault(stId, "서울 테헤란로 하이퍼차징 스테이션");

            return CdrItemDto.builder()
                    .transactionId(c.getTransactionId())
                    .userTag(c.getUserTag())
                    .chargeBoxId(c.getChargeBoxId())
                    .stationName(stationName)
                    .startTime(c.getStartTime().format(FMT))
                    .stopTime(c.getStopTime() != null ? c.getStopTime().format(FMT) : "진행중 (실시간)")
                    .kwh(c.getTotalKwh())
                    .unitPrice(c.getUnitPrice())
                    .totalAmount(c.getTotalAmount())
                    .paymentStatus(c.getPaymentStatus())
                    .build();
        }).collect(Collectors.toList());

        long count = list.size();
        BigDecimal totalKwh = list.stream().map(TransactionCdrEntity::getTotalKwh).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmt = list.stream().map(TransactionCdrEntity::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new HashMap<>();
        result.put("records", items);
        result.put("summary", Map.of(
                "totalCount", count,
                "totalKwh", totalKwh,
                "totalAmount", totalAmt,
                "avgMinutes", 38.6
        ));

        return ResponseEntity.ok(result);
    }
}
