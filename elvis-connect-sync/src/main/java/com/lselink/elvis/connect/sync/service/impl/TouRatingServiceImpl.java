package com.lselink.elvis.connect.sync.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lselink.elvis.connect.sync.domain.ChargeDetailRecord;
import com.lselink.elvis.connect.sync.domain.PaymentStatus;
import com.lselink.elvis.connect.sync.domain.PricingPolicy;
import com.lselink.elvis.connect.sync.domain.PricingTouRule;
import com.lselink.elvis.connect.sync.domain.TransactionSession;
import com.lselink.elvis.connect.sync.dto.CdrBreakdownItem;
import com.lselink.elvis.connect.sync.repository.ChargeDetailRecordRepository;
import com.lselink.elvis.connect.sync.repository.PricingPolicyRepository;
import com.lselink.elvis.connect.sync.repository.PricingTouRuleRepository;
import com.lselink.elvis.connect.sync.service.TouRatingService;
import com.lselink.elvis.connect.sync.service.TouTimeSlicer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * TOU 요금 연산 & CDR 영속화 서비스 구현체 (Sprint 21: S21-SYNC-0080-CO)
 * <p>
 * Virtual Threads 환경에서 인메모리 초고속(<1ms) 요금 계산 및 독립 과금 원장 영속화를 수행합니다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TouRatingServiceImpl implements TouRatingService {

    private static final String DEFAULT_POLICY_ID = "DEFAULT_TOU";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final BigDecimal VAT_RATE = new BigDecimal("0.10");

    private final PricingPolicyRepository policyRepository;
    private final PricingTouRuleRepository ruleRepository;
    private final ChargeDetailRecordRepository cdrRepository;
    private final TouTimeSlicer timeSlicer;
    private final ObjectMapper objectMapper;

    // 인메모리 단가 규칙 캐시 (PolicyId -> RuleGroupMap)
    private final Map<String, Map<String, List<PricingTouRule>>> ruleCache = new ConcurrentHashMap<>();

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ChargeDetailRecord calculateAndSaveCdr(
            TransactionSession session,
            long meterStop,
            LocalDateTime stopTime
    ) {
        String chargeBoxId = session.getChargeBoxId();
        Long transactionId = session.getTransactionId();

        // 1. 멱등성 검증: 이미 발행된 CDR 원장이 존재할 경우 중복 연산 방지
        ChargeDetailRecord existingCdr = cdrRepository
                .findByChargeBoxIdAndTransactionId(chargeBoxId, transactionId)
                .orElse(null);
        if (existingCdr != null) {
            log.info("[TouRating] 이미 존재하는 CDR 원장 반환 (멱등성 보장): cdrNo={}, txId={}",
                    existingCdr.getCdrNo(), transactionId);
            return existingCdr;
        }

        // 2. 정책 로드 및 규칙 캐시 준비
        String policyId = DEFAULT_POLICY_ID;
        PricingPolicy policy = policyRepository.findByPolicyIdAndIsActiveTrue(policyId)
                .orElseGet(() -> PricingPolicy.builder()
                        .policyId(DEFAULT_POLICY_ID)
                        .policyName("기본 요금 정책")
                        .policyType("TOU")
                        .basicFee(BigDecimal.ZERO.setScale(2))
                        .isActive(true)
                        .build());

        Map<String, List<PricingTouRule>> policyRules = getOrLoadRules(policyId);

        // 3. 충전 전력량 및 지속 시간 산출
        LocalDateTime startTime = session.getStartTime();
        BigDecimal totalKwh = session.getTotalEnergyKwh();
        if (totalKwh == null || totalKwh.compareTo(BigDecimal.ZERO) <= 0) {
            long meterStart = session.getMeterStart();
            long diffWh = Math.max(0L, meterStop - meterStart);
            totalKwh = BigDecimal.valueOf(diffWh)
                    .divide(BigDecimal.valueOf(1000.0), 3, RoundingMode.HALF_UP);
        }

        long chargingSeconds = Duration.between(startTime, stopTime).toSeconds();
        if (chargingSeconds < 0) {
            chargingSeconds = 0L;
        }

        // 4. Time-Slicing 기반 시간대별 단가 분할 연산
        List<CdrBreakdownItem> breakdownItems = timeSlicer.sliceAndRate(
                startTime,
                stopTime,
                totalKwh,
                policyRules
        );

        // 5. 금융 표준 BigDecimal 요금 집계
        BigDecimal energyFee = breakdownItems.stream()
                .map(CdrBreakdownItem::getFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal basicFee = policy.getBasicFee() != null ? policy.getBasicFee() : BigDecimal.ZERO.setScale(2);
        BigDecimal discountFee = BigDecimal.ZERO.setScale(2);

        // 부가세: (세전 전력량 요금 + 기본료 - 할인) * 10% (원단위 사사오입)
        BigDecimal taxableAmount = energyFee.add(basicFee).subtract(discountFee).max(BigDecimal.ZERO);
        BigDecimal vat = taxableAmount.multiply(VAT_RATE).setScale(0, RoundingMode.HALF_UP).setScale(2);

        // 최종 결제 청구 금액 (원단위 사사오입)
        BigDecimal totalAmount = taxableAmount.add(vat).setScale(0, RoundingMode.HALF_UP).setScale(2);

        // 6. 고유 영수증 번호 채번: CDR-YYYYMMDD-CBID-TRNID
        String datePart = stopTime.format(DATE_FORMATTER);
        String cdrNo = String.format("CDR-%s-%s-%d", datePart, chargeBoxId, transactionId);

        // 7. 분할 상세 내역 JSON 직렬화
        String breakdownJson = null;
        try {
            breakdownJson = objectMapper.writeValueAsString(breakdownItems);
        } catch (JsonProcessingException e) {
            log.warn("[TouRating] Breakdown JSON 직렬화 실패: error={}", e.getMessage());
        }

        // 8. CDR 엔티티 빌드
        ChargeDetailRecord cdr = ChargeDetailRecord.builder()
                .cdrNo(cdrNo)
                .chargeBoxId(chargeBoxId)
                .connectorId(session.getConnectorId())
                .transactionId(transactionId)
                .sessionId(session.getId())
                .idTag(session.getIdTag())
                .parentIdTag(session.getParentIdTag())
                .startTime(startTime)
                .stopTime(stopTime)
                .chargingSeconds(chargingSeconds)
                .startMeterWh(session.getMeterStart())
                .stopMeterWh(meterStop)
                .totalEnergyKwh(totalKwh)
                .policyId(policyId)
                .energyFee(energyFee)
                .basicFee(basicFee)
                .discountFee(discountFee)
                .vat(vat)
                .totalAmount(totalAmount)
                .paymentStatus(PaymentStatus.PENDING)
                .breakdownJson(breakdownJson)
                .build();

        try {
            ChargeDetailRecord savedCdr = cdrRepository.save(cdr);
            log.info("[TouRating] CDR 과금 원장 영속화 완료: cdrNo={}, totalKwh={}, totalAmount={}원 (세전 {}원, VAT {}원), slots={}",
                    savedCdr.getCdrNo(), totalKwh, totalAmount, energyFee, vat, breakdownItems.size());
            return savedCdr;
        } catch (DataIntegrityViolationException e) {
            log.warn("[TouRating] CDR 중복 저장 시도 방어 (UK_CDR_CHARGE_BOX_TRN): chargeBoxId={}, txId={}",
                    chargeBoxId, transactionId);
            return cdrRepository.findByChargeBoxIdAndTransactionId(chargeBoxId, transactionId)
                    .orElse(cdr);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ChargeDetailRecord getCdr(String chargeBoxId, Long transactionId) {
        return cdrRepository.findByChargeBoxIdAndTransactionId(chargeBoxId, transactionId)
                .orElse(null);
    }

    /**
     * 특정 정책의 단가 규칙 목록 조회 및 인메모리 캐싱
     */
    private Map<String, List<PricingTouRule>> getOrLoadRules(String policyId) {
        return ruleCache.computeIfAbsent(policyId, id -> {
            List<PricingTouRule> rules = ruleRepository
                    .findByPolicyIdOrderBySeasonTypeAscDayTypeAscStartHourAsc(id);
            if (rules.isEmpty()) {
                log.warn("[TouRating] 등록된 TOU 단가 규칙이 없음: policyId={}", id);
                return Collections.emptyMap();
            }

            return rules.stream()
                    .collect(Collectors.groupingBy(r -> r.getSeasonType().name() + "_" + r.getDayType().name()));
        });
    }
}
