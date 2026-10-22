package com.lselink.elvis.control.dto;

import java.math.BigDecimal;

public class ChargerSummaryDto {
    private long totalCount;
    private long availableCount;
    private long chargingCount;
    private long completedCount;
    private long faultedCount;
    private long disconnectedCount;
    private BigDecimal totalPowerKw;
    private BigDecimal totalAccumulatedKwh;

    public ChargerSummaryDto() {
    }

    public ChargerSummaryDto(long totalCount, long availableCount, long chargingCount,
                             long completedCount, long faultedCount, long disconnectedCount,
                             BigDecimal totalPowerKw, BigDecimal totalAccumulatedKwh) {
        this.totalCount = totalCount;
        this.availableCount = availableCount;
        this.chargingCount = chargingCount;
        this.completedCount = completedCount;
        this.faultedCount = faultedCount;
        this.disconnectedCount = disconnectedCount;
        this.totalPowerKw = totalPowerKw;
        this.totalAccumulatedKwh = totalAccumulatedKwh;
    }

    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }

    public long getAvailableCount() { return availableCount; }
    public void setAvailableCount(long availableCount) { this.availableCount = availableCount; }

    public long getChargingCount() { return chargingCount; }
    public void setChargingCount(long chargingCount) { this.chargingCount = chargingCount; }

    public long getCompletedCount() { return completedCount; }
    public void setCompletedCount(long completedCount) { this.completedCount = completedCount; }

    public long getFaultedCount() { return faultedCount; }
    public void setFaultedCount(long faultedCount) { this.faultedCount = faultedCount; }

    public long getDisconnectedCount() { return disconnectedCount; }
    public void setDisconnectedCount(long disconnectedCount) { this.disconnectedCount = disconnectedCount; }

    public BigDecimal getTotalPowerKw() { return totalPowerKw; }
    public void setTotalPowerKw(BigDecimal totalPowerKw) { this.totalPowerKw = totalPowerKw; }

    public BigDecimal getTotalAccumulatedKwh() { return totalAccumulatedKwh; }
    public void setTotalAccumulatedKwh(BigDecimal totalAccumulatedKwh) { this.totalAccumulatedKwh = totalAccumulatedKwh; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long totalCount;
        private long availableCount;
        private long chargingCount;
        private long completedCount;
        private long faultedCount;
        private long disconnectedCount;
        private BigDecimal totalPowerKw;
        private BigDecimal totalAccumulatedKwh;

        public Builder totalCount(long totalCount) { this.totalCount = totalCount; return this; }
        public Builder availableCount(long availableCount) { this.availableCount = availableCount; return this; }
        public Builder chargingCount(long chargingCount) { this.chargingCount = chargingCount; return this; }
        public Builder completedCount(long completedCount) { this.completedCount = completedCount; return this; }
        public Builder faultedCount(long faultedCount) { this.faultedCount = faultedCount; return this; }
        public Builder disconnectedCount(long disconnectedCount) { this.disconnectedCount = disconnectedCount; return this; }
        public Builder totalPowerKw(BigDecimal totalPowerKw) { this.totalPowerKw = totalPowerKw; return this; }
        public Builder totalAccumulatedKwh(BigDecimal totalAccumulatedKwh) { this.totalAccumulatedKwh = totalAccumulatedKwh; return this; }

        public ChargerSummaryDto build() {
            return new ChargerSummaryDto(totalCount, availableCount, chargingCount,
                    completedCount, faultedCount, disconnectedCount, totalPowerKw, totalAccumulatedKwh);
        }
    }
}
