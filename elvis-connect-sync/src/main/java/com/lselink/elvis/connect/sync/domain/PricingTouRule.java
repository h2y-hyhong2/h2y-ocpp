package com.lselink.elvis.connect.sync.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TOU 계절/요일/시간대별 단가 상세 규칙 엔티티 (Sprint 21: S21-SYNC-0080-CO)
 */
@Entity
@Table(
        name = "tb_pricing_tou_rule",
        indexes = {
                @Index(name = "IX_TOU_RULE_LOOKUP", columnList = "POLICY_ID, SEASON_TYPE, DAY_TYPE, START_HOUR, END_HOUR")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PricingTouRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /**
     * 연관 요금 정책 식별자
     */
    @Column(name = "POLICY_ID", length = 32, nullable = false)
    private String policyId;

    /**
     * 계절 구분 (SPRING_AUTUMN, SUMMER, WINTER)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "SEASON_TYPE", length = 16, nullable = false)
    private SeasonType seasonType;

    /**
     * 요일 구분 (WEEKDAY, SATURDAY, HOLIDAY)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "DAY_TYPE", length = 16, nullable = false)
    private DayType dayType;

    /**
     * 부하 구분 (OFF_PEAK, MID_PEAK, ON_PEAK)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "LOAD_TYPE", length = 16, nullable = false)
    private LoadType loadType;

    /**
     * 적용 시작 시각 (0~23)
     */
    @Column(name = "START_HOUR", nullable = false)
    private int startHour;

    /**
     * 적용 종료 시각 (1~24, 24는 익일 00시)
     */
    @Column(name = "END_HOUR", nullable = false)
    private int endHour;

    /**
     * 전력량 단가 (원/kWh)
     */
    @Column(name = "UNIT_PRICE", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @CreationTimestamp
    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt;

    /**
     * 특정 시간(hour)이 본 규칙의 적용 시간 범위에 속하는지 여부
     */
    public boolean matchesHour(int hour) {
        return hour >= startHour && hour < endHour;
    }
}
