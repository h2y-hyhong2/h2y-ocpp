package com.lselink.elvis.connect.sync.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lselink.elvis.connect.sync.domain.DayType;
import com.lselink.elvis.connect.sync.domain.LoadType;
import com.lselink.elvis.connect.sync.domain.SeasonType;

import java.math.BigDecimal;

/**
 * CDR 원장 내 시간대별 분할 과금 상세 내역 Value Object (JSON 저장용)
 * [전사 코딩 표준 준수: Lombok 배제, 순수 자바 POJO 및 Builder 패턴 구현]
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CdrBreakdownItem {

    /**
     * 슬롯 시작 일시 (문자열: yyyy-MM-dd HH:mm:ss)
     */
    private String slotStart;

    /**
     * 슬롯 종료 일시 (문자열: yyyy-MM-dd HH:mm:ss)
     */
    private String slotEnd;

    /**
     * 계절 구분
     */
    private SeasonType seasonType;

    /**
     * 요일 구분
     */
    private DayType dayType;

    /**
     * 부하 구분 (경부하, 중부하, 최대부하)
     */
    private LoadType loadType;

    /**
     * 슬롯 소요 시간 (초 단위)
     */
    private long durationSeconds;

    /**
     * 해당 구간 적용 전력량 단가 (원/kWh)
     */
    private BigDecimal unitPrice;

    /**
     * 해당 구간 배분 충전량 (kWh, 소수점 3자리)
     */
    private BigDecimal kwh;

    /**
     * 해당 구간 산출 요금 (세전, 원, 소수점 2자리)
     */
    private BigDecimal fee;

    public CdrBreakdownItem() {
    }

    public CdrBreakdownItem(String slotStart, String slotEnd, SeasonType seasonType, DayType dayType,
                            LoadType loadType, long durationSeconds, BigDecimal unitPrice, BigDecimal kwh, BigDecimal fee) {
        this.slotStart = slotStart;
        this.slotEnd = slotEnd;
        this.seasonType = seasonType;
        this.dayType = dayType;
        this.loadType = loadType;
        this.durationSeconds = durationSeconds;
        this.unitPrice = unitPrice;
        this.kwh = kwh;
        this.fee = fee;
    }

    public String getSlotStart() {
        return slotStart;
    }

    public void setSlotStart(String slotStart) {
        this.slotStart = slotStart;
    }

    public String getSlotEnd() {
        return slotEnd;
    }

    public void setSlotEnd(String slotEnd) {
        this.slotEnd = slotEnd;
    }

    public SeasonType getSeasonType() {
        return seasonType;
    }

    public void setSeasonType(SeasonType seasonType) {
        this.seasonType = seasonType;
    }

    public DayType getDayType() {
        return dayType;
    }

    public void setDayType(DayType dayType) {
        this.dayType = dayType;
    }

    public LoadType getLoadType() {
        return loadType;
    }

    public void setLoadType(LoadType loadType) {
        this.loadType = loadType;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getKwh() {
        return kwh;
    }

    public void setKwh(BigDecimal kwh) {
        this.kwh = kwh;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    @Override
    public String toString() {
        return "CdrBreakdownItem{" +
                "slotStart='" + slotStart + '\'' +
                ", slotEnd='" + slotEnd + '\'' +
                ", seasonType=" + seasonType +
                ", dayType=" + dayType +
                ", loadType=" + loadType +
                ", durationSeconds=" + durationSeconds +
                ", unitPrice=" + unitPrice +
                ", kwh=" + kwh +
                ", fee=" + fee +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String slotStart;
        private String slotEnd;
        private SeasonType seasonType;
        private DayType dayType;
        private LoadType loadType;
        private long durationSeconds;
        private BigDecimal unitPrice;
        private BigDecimal kwh;
        private BigDecimal fee;

        public Builder slotStart(String slotStart) {
            this.slotStart = slotStart;
            return this;
        }

        public Builder slotEnd(String slotEnd) {
            this.slotEnd = slotEnd;
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

        public Builder durationSeconds(long durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }

        public Builder unitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        public Builder kwh(BigDecimal kwh) {
            this.kwh = kwh;
            return this;
        }

        public Builder fee(BigDecimal fee) {
            this.fee = fee;
            return this;
        }

        public CdrBreakdownItem build() {
            return new CdrBreakdownItem(slotStart, slotEnd, seasonType, dayType, loadType,
                    durationSeconds, unitPrice, kwh, fee);
        }
    }
}
