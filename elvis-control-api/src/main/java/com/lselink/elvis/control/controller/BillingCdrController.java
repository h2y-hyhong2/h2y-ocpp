package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.entity.StationEntity;
import com.lselink.elvis.control.entity.TransactionCdrEntity;
import com.lselink.elvis.control.repository.StationRepository;
import com.lselink.elvis.control.repository.TransactionCdrRepository;
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
@CrossOrigin(origins = "*")
public class BillingCdrController {

    private final TransactionCdrRepository transactionCdrRepository;
    private final StationRepository stationRepository;
    private final com.lselink.elvis.control.repository.ChargerRepository chargerRepository;

    public BillingCdrController(TransactionCdrRepository transactionCdrRepository,
                                StationRepository stationRepository,
                                com.lselink.elvis.control.repository.ChargerRepository chargerRepository) {
        this.transactionCdrRepository = transactionCdrRepository;
        this.stationRepository = stationRepository;
        this.chargerRepository = chargerRepository;
    }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

        public CdrItemDto() {
        }

        public CdrItemDto(Long transactionId, String userTag, String chargeBoxId, String stationName,
                          String startTime, String stopTime, BigDecimal kwh, BigDecimal unitPrice,
                          BigDecimal totalAmount, String paymentStatus) {
            this.transactionId = transactionId;
            this.userTag = userTag;
            this.chargeBoxId = chargeBoxId;
            this.stationName = stationName;
            this.startTime = startTime;
            this.stopTime = stopTime;
            this.kwh = kwh;
            this.unitPrice = unitPrice;
            this.totalAmount = totalAmount;
            this.paymentStatus = paymentStatus;
        }

        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

        public String getUserTag() { return userTag; }
        public void setUserTag(String userTag) { this.userTag = userTag; }

        public String getChargeBoxId() { return chargeBoxId; }
        public void setChargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; }

        public String getStationName() { return stationName; }
        public void setStationName(String stationName) { this.stationName = stationName; }

        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }

        public String getStopTime() { return stopTime; }
        public void setStopTime(String stopTime) { this.stopTime = stopTime; }

        public BigDecimal getKwh() { return kwh; }
        public void setKwh(BigDecimal kwh) { this.kwh = kwh; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
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

            public Builder transactionId(Long transactionId) { this.transactionId = transactionId; return this; }
            public Builder userTag(String userTag) { this.userTag = userTag; return this; }
            public Builder chargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; return this; }
            public Builder stationName(String stationName) { this.stationName = stationName; return this; }
            public Builder startTime(String startTime) { this.startTime = startTime; return this; }
            public Builder stopTime(String stopTime) { this.stopTime = stopTime; return this; }
            public Builder kwh(BigDecimal kwh) { this.kwh = kwh; return this; }
            public Builder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
            public Builder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
            public Builder paymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; return this; }

            public CdrItemDto build() {
                return new CdrItemDto(transactionId, userTag, chargeBoxId, stationName, startTime, stopTime, kwh, unitPrice, totalAmount, paymentStatus);
            }
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCdrRecords() {
        List<TransactionCdrEntity> list = transactionCdrRepository.findAllByOrderByStartTimeDesc();

        Map<String, String> stationNameMap = stationRepository.findAll().stream()
                .collect(Collectors.toMap(StationEntity::getStId, StationEntity::getName, (s1, s2) -> s1));

        Map<String, String> chargerStationMap = chargerRepository.findAll().stream()
                .collect(Collectors.toMap(com.lselink.elvis.control.entity.ChargerEntity::getChargeBoxId, com.lselink.elvis.control.entity.ChargerEntity::getStId, (c1, c2) -> c1));

        List<CdrItemDto> items = list.stream().map(c -> {
            String stId = chargerStationMap.getOrDefault(c.getChargeBoxId(), "10001");
            String stationName = stationNameMap.getOrDefault(stId, "강남 테헤란로 하이퍼차징 스테이션");

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
