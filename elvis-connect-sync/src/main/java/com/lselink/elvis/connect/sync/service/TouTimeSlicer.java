package com.lselink.elvis.connect.sync.service;

import com.lselink.elvis.connect.sync.domain.DayType;
import com.lselink.elvis.connect.sync.domain.LoadType;
import com.lselink.elvis.connect.sync.domain.PricingTouRule;
import com.lselink.elvis.connect.sync.domain.SeasonType;
import com.lselink.elvis.connect.sync.dto.CdrBreakdownItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 정밀 시간대 분할 과금(Time-Slicing Rating) 슬라이서 컴포넌트 (Sprint 21: S21-SYNC-0080-CO)
 * <p>
 * 충전 시작~종료 구간을 정시(Hour Boundary) 기준으로 슬라이스 분할하여,
 * 각 시간대별(경/중/최대부하) 전력량을 가중 배분하고 누적 오차를 제로화(Zero-Sum)합니다.
 * </p>
 */
@Slf4j
@Component
public class TouTimeSlicer {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final BigDecimal DEFAULT_UNIT_PRICE = new BigDecimal("255.70"); // 기본 폴백 단가 (원/kWh)

    /**
     * 타임 슬라이스 분할 및 시간대별 전력량/요금 계산
     *
     * @param startTime       충전 시작 일시
     * @param stopTime        충전 종료 일시
     * @param totalEnergyKwh  총 충전 전력량 (kWh, 소수점 3자리)
     * @param ruleCache       (SeasonType + "_" + DayType) -> List<PricingTouRule> 룰 캐시 맵
     * @return 시간대별 분할 상세 내역 리스트
     */
    public List<CdrBreakdownItem> sliceAndRate(
            LocalDateTime startTime,
            LocalDateTime stopTime,
            BigDecimal totalEnergyKwh,
            Map<String, List<PricingTouRule>> ruleCache
    ) {
        List<CdrBreakdownItem> breakdownItems = new ArrayList<>();

        if (startTime == null || stopTime == null || !stopTime.isAfter(startTime)) {
            // 비정상 시간(시작>=종료)의 경우 단일 슬롯 0원 처리
            LocalDateTime safeStart = (startTime != null) ? startTime : LocalDateTime.now();
            LocalDateTime safeStop = (stopTime != null && stopTime.isAfter(safeStart)) ? stopTime : safeStart;
            long dur = Math.max(1L, Duration.between(safeStart, safeStop).toSeconds());

            PricingTouRule rule = findRule(safeStart, ruleCache);
            BigDecimal unitPrice = (rule != null) ? rule.getUnitPrice() : DEFAULT_UNIT_PRICE;
            LoadType loadType = (rule != null) ? rule.getLoadType() : LoadType.MID_PEAK;
            BigDecimal fee = totalEnergyKwh.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

            breakdownItems.add(CdrBreakdownItem.builder()
                    .slotStart(safeStart.format(FORMATTER))
                    .slotEnd(safeStop.format(FORMATTER))
                    .seasonType(SeasonType.from(safeStart.toLocalDate()))
                    .dayType(DayType.from(safeStart.toLocalDate()))
                    .loadType(loadType)
                    .durationSeconds(dur)
                    .unitPrice(unitPrice)
                    .kwh(totalEnergyKwh)
                    .fee(fee)
                    .build());
            return breakdownItems;
        }

        long totalSeconds = Duration.between(startTime, stopTime).toSeconds();
        if (totalSeconds <= 0) {
            totalSeconds = 1L;
        }

        // 1. 시간대 구간 분할점(Hour Boundary) 계산
        List<LocalDateTime> boundaries = getHourBoundaries(startTime, stopTime);

        BigDecimal accumulatedKwh = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);

        // 2. 각 슬롯별 전력량 및 요금 계산
        for (int i = 0; i < boundaries.size() - 1; i++) {
            LocalDateTime slotStart = boundaries.get(i);
            LocalDateTime slotEnd = boundaries.get(i + 1);
            long slotSeconds = Duration.between(slotStart, slotEnd).toSeconds();

            if (slotSeconds <= 0) {
                continue;
            }

            SeasonType seasonType = SeasonType.from(slotStart.toLocalDate());
            DayType dayType = DayType.from(slotStart.toLocalDate());
            PricingTouRule rule = findRule(slotStart, ruleCache);

            BigDecimal unitPrice = (rule != null) ? rule.getUnitPrice() : DEFAULT_UNIT_PRICE;
            LoadType loadType = (rule != null) ? rule.getLoadType() : LoadType.MID_PEAK;

            BigDecimal slotKwh;
            boolean isLastSlot = (i == boundaries.size() - 2);

            if (isLastSlot) {
                // 마지막 슬롯: 소수점 오차 보정을 위해 (전체 전력량 - 누적 전력량) 적용
                slotKwh = totalEnergyKwh.subtract(accumulatedKwh).max(BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP);
            } else {
                // 가중 비율 계산: (slotSeconds / totalSeconds) * totalEnergyKwh
                slotKwh = totalEnergyKwh.multiply(BigDecimal.valueOf(slotSeconds))
                        .divide(BigDecimal.valueOf(totalSeconds), 3, RoundingMode.HALF_UP);
                accumulatedKwh = accumulatedKwh.add(slotKwh);
            }

            BigDecimal slotFee = slotKwh.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

            breakdownItems.add(CdrBreakdownItem.builder()
                    .slotStart(slotStart.format(FORMATTER))
                    .slotEnd(slotEnd.format(FORMATTER))
                    .seasonType(seasonType)
                    .dayType(dayType)
                    .loadType(loadType)
                    .durationSeconds(slotSeconds)
                    .unitPrice(unitPrice)
                    .kwh(slotKwh)
                    .fee(slotFee)
                    .build());
        }

        return breakdownItems;
    }

    /**
     * 충전 시작~종료 시각 사이의 모든 정시(Hour Boundary) 경계 시각 목록 도출
     */
    private List<LocalDateTime> getHourBoundaries(LocalDateTime start, LocalDateTime end) {
        List<LocalDateTime> boundaries = new ArrayList<>();
        boundaries.add(start);

        // 다음 첫 번째 정시(HH:00:00) 산출
        LocalDateTime nextBoundary = start.truncatedTo(ChronoUnit.HOURS).plusHours(1);

        while (nextBoundary.isBefore(end)) {
            boundaries.add(nextBoundary);
            nextBoundary = nextBoundary.plusHours(1);
        }

        boundaries.add(end);
        return boundaries;
    }

    /**
     * 특정 시각(시작 시각 기준)에 매칭되는 TOU 단가 규칙 검색
     */
    private PricingTouRule findRule(LocalDateTime time, Map<String, List<PricingTouRule>> ruleCache) {
        if (ruleCache == null || ruleCache.isEmpty()) {
            return null;
        }

        SeasonType season = SeasonType.from(time.toLocalDate());
        DayType day = DayType.from(time.toLocalDate());
        int hour = time.getHour();

        String key = season.name() + "_" + day.name();
        List<PricingTouRule> rules = ruleCache.get(key);

        if (rules != null) {
            for (PricingTouRule rule : rules) {
                if (rule.matchesHour(hour)) {
                    return rule;
                }
            }
        }

        // 주말/공휴일 특화 규칙이 없을 경우 주중(WEEKDAY) 규칙으로 폴백
        if (day != DayType.WEEKDAY) {
            String fallbackKey = season.name() + "_" + DayType.WEEKDAY.name();
            List<PricingTouRule> fallbackRules = ruleCache.get(fallbackKey);
            if (fallbackRules != null) {
                for (PricingTouRule rule : fallbackRules) {
                    if (rule.matchesHour(hour)) {
                        return rule;
                    }
                }
            }
        }

        return null;
    }
}
