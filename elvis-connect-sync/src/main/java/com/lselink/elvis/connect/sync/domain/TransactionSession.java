package com.lselink.elvis.connect.sync.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 충전 거래 세션 관리 원장 엔티티 (Sprint 19: S19-SYNC-0040-CO)
 * <p>
 * OCPP StartTransaction / StopTransaction 수신 시 충전 세션의 시작부터
 * 전력량 계측, 종료 및 과금 연계(CDR)에 이르는 생명주기를 원자적으로 관리합니다.
 * </p>
 */
@Entity
@Table(
        name = "tb_transaction_session",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_SESSION_CHARGE_BOX_TRN", columnNames = {"CHARGE_BOX_ID", "TRANSACTION_ID"})
        },
        indexes = {
                @Index(name = "IX_SESSION_CHARGER_CONN_STATUS", columnList = "CHARGE_BOX_ID, CONNECTOR_ID, STATUS"),
                @Index(name = "IX_SESSION_START_TIME", columnList = "START_TIME"),
                @Index(name = "IX_SESSION_ID_TAG", columnList = "ID_TAG")
        }
)
@DynamicUpdate
public class TransactionSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /** 충전기 식별자 (ChargeBoxIdentity) */
    @Column(name = "CHARGE_BOX_ID", nullable = false, length = 64)
    private String chargeBoxId;

    /** 충전 건/커넥터 번호 (1~N) */
    @Column(name = "CONNECTOR_ID", nullable = false)
    private int connectorId;

    /** OCPP 거래 번호 (단말 채번 TransactionId) */
    @Column(name = "TRANSACTION_ID", nullable = false)
    private Long transactionId;

    /** 회원 인증 식별자 (RFID / IdTag) */
    @Column(name = "ID_TAG", nullable = false, length = 64)
    private String idTag;

    /** 상위/법인 회원 식별자 (ParentIdTag) */
    @Column(name = "PARENT_ID_TAG", length = 64)
    private String parentIdTag;

    /** 세션 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 32)
    private SessionStatus status;

    /** 충전 시작 미터값 (Wh) */
    @Column(name = "METER_START", nullable = false)
    private Long meterStart;

    /** 충전 종료 미터값 (Wh) */
    @Column(name = "METER_STOP")
    private Long meterStop;

    /** 최종 충전 전력량 (kWh, (meterStop - meterStart) / 1000) */
    @Column(name = "TOTAL_ENERGY_KWH", nullable = false, precision = 10, scale = 3)
    private BigDecimal totalEnergyKwh;

    /** 충전 시작 일시 (단말 타임스탬프 기준) */
    @Column(name = "START_TIME", nullable = false)
    private LocalDateTime startTime;

    /** 충전 종료 일시 (단말 타임스탬프 기준) */
    @Column(name = "STOP_TIME")
    private LocalDateTime stopTime;

    /** 총 충전 경과 시간 (분 단위) */
    @Column(name = "CHARGING_MINUTES", nullable = false)
    private int chargingMinutes;

    /** OCPP 종료 사유 (EmergencyStop, EVDisconnected 등 11종) */
    @Enumerated(EnumType.STRING)
    @Column(name = "STOP_REASON", length = 64)
    private StopReason stopReason;

    /** JPA 낙관적 락(Optimistic Locking) 버전 컬럼 */
    @Version
    @Column(name = "VERSION", nullable = false)
    private int version;

    /** 최초 세션 생성 일시 */
    @CreationTimestamp
    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt;

    @UpdateTimestamp
    @Column(name = "UPDT_DT", nullable = false)
    private LocalDateTime updtDt;

    protected TransactionSession() {
    }

    public TransactionSession(Long id, String chargeBoxId, int connectorId, Long transactionId, String idTag,
                              String parentIdTag, SessionStatus status, Long meterStart, Long meterStop,
                              BigDecimal totalEnergyKwh, LocalDateTime startTime, LocalDateTime stopTime,
                              int chargingMinutes, StopReason stopReason, int version, LocalDateTime regDt,
                              LocalDateTime updtDt) {
        this.id = id;
        this.chargeBoxId = chargeBoxId;
        this.connectorId = connectorId;
        this.transactionId = transactionId;
        this.idTag = idTag;
        this.parentIdTag = parentIdTag;
        this.status = status;
        this.meterStart = meterStart;
        this.meterStop = meterStop;
        this.totalEnergyKwh = totalEnergyKwh;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.chargingMinutes = chargingMinutes;
        this.stopReason = stopReason;
        this.version = version;
        this.regDt = regDt;
        this.updtDt = updtDt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getChargeBoxId() { return chargeBoxId; }
    public void setChargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; }

    public int getConnectorId() { return connectorId; }
    public void setConnectorId(int connectorId) { this.connectorId = connectorId; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public String getIdTag() { return idTag; }
    public void setIdTag(String idTag) { this.idTag = idTag; }

    public String getParentIdTag() { return parentIdTag; }
    public void setParentIdTag(String parentIdTag) { this.parentIdTag = parentIdTag; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }

    public Long getMeterStart() { return meterStart; }
    public void setMeterStart(Long meterStart) { this.meterStart = meterStart; }

    public Long getMeterStop() { return meterStop; }
    public void setMeterStop(Long meterStop) { this.meterStop = meterStop; }

    public BigDecimal getTotalEnergyKwh() { return totalEnergyKwh; }
    public void setTotalEnergyKwh(BigDecimal totalEnergyKwh) { this.totalEnergyKwh = totalEnergyKwh; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getStopTime() { return stopTime; }
    public void setStopTime(LocalDateTime stopTime) { this.stopTime = stopTime; }

    public int getChargingMinutes() { return chargingMinutes; }
    public void setChargingMinutes(int chargingMinutes) { this.chargingMinutes = chargingMinutes; }

    public StopReason getStopReason() { return stopReason; }
    public void setStopReason(StopReason stopReason) { this.stopReason = stopReason; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public LocalDateTime getRegDt() { return regDt; }
    public void setRegDt(LocalDateTime regDt) { this.regDt = regDt; }

    public LocalDateTime getUpdtDt() { return updtDt; }
    public void setUpdtDt(LocalDateTime updtDt) { this.updtDt = updtDt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String chargeBoxId;
        private int connectorId;
        private Long transactionId;
        private String idTag;
        private String parentIdTag;
        private SessionStatus status;
        private Long meterStart;
        private Long meterStop;
        private BigDecimal totalEnergyKwh;
        private LocalDateTime startTime;
        private LocalDateTime stopTime;
        private int chargingMinutes;
        private StopReason stopReason;
        private int version;
        private LocalDateTime regDt;
        private LocalDateTime updtDt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder chargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; return this; }
        public Builder connectorId(int connectorId) { this.connectorId = connectorId; return this; }
        public Builder transactionId(Long transactionId) { this.transactionId = transactionId; return this; }
        public Builder idTag(String idTag) { this.idTag = idTag; return this; }
        public Builder parentIdTag(String parentIdTag) { this.parentIdTag = parentIdTag; return this; }
        public Builder status(SessionStatus status) { this.status = status; return this; }
        public Builder meterStart(Long meterStart) { this.meterStart = meterStart; return this; }
        public Builder meterStop(Long meterStop) { this.meterStop = meterStop; return this; }
        public Builder totalEnergyKwh(BigDecimal totalEnergyKwh) { this.totalEnergyKwh = totalEnergyKwh; return this; }
        public Builder startTime(LocalDateTime startTime) { this.startTime = startTime; return this; }
        public Builder stopTime(LocalDateTime stopTime) { this.stopTime = stopTime; return this; }
        public Builder chargingMinutes(int chargingMinutes) { this.chargingMinutes = chargingMinutes; return this; }
        public Builder stopReason(StopReason stopReason) { this.stopReason = stopReason; return this; }
        public Builder version(int version) { this.version = version; return this; }
        public Builder regDt(LocalDateTime regDt) { this.regDt = regDt; return this; }
        public Builder updtDt(LocalDateTime updtDt) { this.updtDt = updtDt; return this; }

        public TransactionSession build() {
            return new TransactionSession(id, chargeBoxId, connectorId, transactionId, idTag, parentIdTag,
                    status, meterStart, meterStop, totalEnergyKwh, startTime, stopTime, chargingMinutes,
                    stopReason, version, regDt, updtDt);
        }
    }

    /**
     * 세션 종료 비즈니스 로직
     *
     * @param meterStop 종료 미터값 (Wh)
     * @param stopTime  종료 일시
     * @param reason    종료 사유
     */
    public void stopSession(Long meterStop, LocalDateTime stopTime, StopReason reason) {
        this.meterStop = meterStop;
        this.stopTime = stopTime != null ? stopTime : LocalDateTime.now();
        this.stopReason = reason != null ? reason : StopReason.Other;
        this.status = SessionStatus.STOPPED;

        // 전력량(kWh) 산출 및 음수값 방어
        if (meterStop != null && this.meterStart != null && meterStop >= this.meterStart) {
            long diffWh = meterStop - this.meterStart;
            this.totalEnergyKwh = BigDecimal.valueOf(diffWh)
                    .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
        } else {
            this.totalEnergyKwh = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }

        // 충전 경과 시간(분) 산출
        if (this.stopTime != null && this.startTime != null) {
            long seconds = Duration.between(this.startTime, this.stopTime).getSeconds();
            this.chargingMinutes = (int) Math.max(0, seconds / 60);
        }
    }

    /**
     * 진행 중 계측값 업데이트 (MeterValues 연계 시 활용)
     *
     * @param currentMeterWh 현재 미터값 (Wh)
     */
    public void updateMeterValues(Long currentMeterWh) {
        if (currentMeterWh != null && this.meterStart != null && currentMeterWh >= this.meterStart) {
            long diffWh = currentMeterWh - this.meterStart;
            this.totalEnergyKwh = BigDecimal.valueOf(diffWh)
                    .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
            this.status = SessionStatus.CHARGING;
        }
    }
}
