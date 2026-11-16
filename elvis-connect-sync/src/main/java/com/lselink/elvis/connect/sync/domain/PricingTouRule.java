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
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TOU 계절/요일/시간대별 단가 상세 규칙 엔티티 (Sprint 21: S21-SYNC-0080-CO)
 * [전사 코딩 표준 준수: Lombok 배제, 순수 자바 엔티티 및 Builder 패턴 구현]
 */
@Entity
@Table(
        name = "pricing_tou_rule",
        indexes = {
                @Index(name = "ix_tou_rule_lookup", columnList = "policy_id, season_type, day_type, start_hour, end_hour")
        }
)
public class PricingTouRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 연관 요금 정책 식별자
     */
    @Column(name = "policy_id", length = 32, nullable = false)
    private String policyId;

    /**
     * 계절 구분 (SPRING_AUTUMN, SUMMER, WINTER)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "season_type", length = 16, nullable = false)
    private SeasonType seasonType;

    /**
     * 요일 구분 (WEEKDAY, SATURDAY, HOLIDAY)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", length = 16, nullable = false)
    private DayType dayType;

    /**
     * 부하 구분 (OFF_PEAK, MID_PEAK, ON_PEAK)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "load_type", length = 16, nullable = false)
    private LoadType loadType;

    /**
     * 적용 시작 시각 (0~23)
     */
    @Column(name = "start_hour", nullable = false)
    private int startHour;

    /**
     * 적용 종료 시각 (1~24, 24는 익일 00시)
     */
    @Column(name = "end_hour", nullable = false)
    private int endHour;

    /**
     * 전력량 단가 (원/kWh)
     */
    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @CreationTimestamp
    @Column(name = "reg_dt", nullable = false, updatable = false)
    private LocalDateTime regDt;

    protected PricingTouRule() {
    }

    public PricingTouRule(Long id, String policyId, SeasonType seasonType, DayType dayType,
                          LoadType loadType, int startHour, int endHour, BigDecimal unitPrice, LocalDateTime regDt) {
        this.id = id;
        this.policyId = policyId;
        this.seasonType = seasonType;
        this.dayType = dayType;
        this.loadType = loadType;
        this.startHour = startHour;
        this.endHour = endHour;
        this.unitPrice = unitPrice;
        this.regDt = regDt;
    }

    public Long getId() {
        return id;
    }

    public String getPolicyId() {
        return policyId;
    }

    public SeasonType getSeasonType() {
        return seasonType;
    }

    public DayType getDayType() {
        return dayType;
    }

    public LoadType getLoadType() {
        return loadType;
    }

    public int getStartHour() {
        return startHour;
    }

    public int getEndHour() {
        return endHour;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public LocalDateTime getRegDt() {
        return regDt;
    }

    /**
     * 특정 시간(hour)이 본 규칙의 적용 시간 범위에 속하는지 여부
     */
    public boolean matchesHour(int hour) {
        return hour >= startHour && hour < endHour;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String policyId;
        private SeasonType seasonType;
        private DayType dayType;
        private LoadType loadType;
        private int startHour;
        private int endHour;
        private BigDecimal unitPrice;
        private LocalDateTime regDt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder policyId(String policyId) {
            this.policyId = policyId;
            return this;
        }

        public Builder seasonType(SeasonType seasonType) {
            this.seasonType = seasonType;
            return this;
        }

        public Builder dayType(DayType dayType) {
            this.dayType = dayType;
            return this;
        }

        public Builder loadType(LoadType loadType) {
            this.loadType = loadType;
            return this;
        }

        public Builder startHour(int startHour) {
            this.startHour = startHour;
            return this;
        }

        public Builder endHour(int endHour) {
            this.endHour = endHour;
            return this;
        }

        public Builder unitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        public Builder regDt(LocalDateTime regDt) {
            this.regDt = regDt;
            return this;
        }

        public PricingTouRule build() {
            return new PricingTouRule(id, policyId, seasonType, dayType, loadType, startHour, endHour, unitPrice, regDt);
        }
    }
}
