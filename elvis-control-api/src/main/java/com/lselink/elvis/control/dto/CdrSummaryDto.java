package com.lselink.elvis.control.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 충전 거래 이력(CDR) 4대 KPI 요약 통계 DTO
 */
public class CdrSummaryDto {

    private long totalCount;
    private BigDecimal totalKwh;
    private BigDecimal totalAmount;
    private double avgMinutes;

    public CdrSummaryDto() {
        this.totalCount = 0;
        this.totalKwh = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
        this.avgMinutes = 0.0;
    }

    public CdrSummaryDto(long totalCount, BigDecimal totalKwh, BigDecimal totalAmount, double avgMinutes) {
        this.totalCount = totalCount;
        this.totalKwh = (totalKwh != null) ? totalKwh.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.totalAmount = (totalAmount != null) ? totalAmount.setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.avgMinutes = Math.round(avgMinutes * 10.0) / 10.0;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public BigDecimal getTotalKwh() {
        return totalKwh;
    }

    public void setTotalKwh(BigDecimal totalKwh) {
        this.totalKwh = (totalKwh != null) ? totalKwh.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = (totalAmount != null) ? totalAmount.setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public double getAvgMinutes() {
        return avgMinutes;
    }

    public void setAvgMinutes(double avgMinutes) {
        this.avgMinutes = Math.round(avgMinutes * 10.0) / 10.0;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long totalCount;
        private BigDecimal totalKwh = BigDecimal.ZERO;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private double avgMinutes = 0.0;

        public Builder totalCount(long totalCount) { this.totalCount = totalCount; return this; }
        public Builder totalKwh(BigDecimal totalKwh) { this.totalKwh = totalKwh; return this; }
        public Builder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public Builder avgMinutes(double avgMinutes) { this.avgMinutes = avgMinutes; return this; }

        public CdrSummaryDto build() {
            return new CdrSummaryDto(totalCount, totalKwh, totalAmount, avgMinutes);
        }
    }
}
