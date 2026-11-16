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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 충전 과금 원장 (CDR: Charge Detail Record) 엔티티 (Sprint 21: S21-SYNC-0080-CO)
 * <p>
 * 단일 충전 세션의 최종 소비 전력량, 적용 단가, 시간대별 분할 상세 및 최종 결제 금액을 확정한 불변 마스터 원장입니다.
 * [전사 코딩 표준 준수: Lombok 배제, 순수 자바 엔티티 및 Builder 패턴 구현]
 * </p>
 */
@Entity
@Table(
        name = "charge_detail_record",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cdr_no", columnNames = {"cdr_no"}),
                @UniqueConstraint(name = "uk_cdr_charge_box_trn", columnNames = {"charge_box_id", "transaction_id"})
        },
        indexes = {
                @Index(name = "ix_cdr_stop_time", columnList = "stop_time"),
                @Index(name = "ix_cdr_id_tag", columnList = "id_tag"),
                @Index(name = "ix_cdr_session_id", columnList = "session_id")
        }
)
public class ChargeDetailRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 과금 영수증 번호 (고유키: CDR-YYYYMMDD-CBID-TRNID)
     */
    @Column(name = "cdr_no", length = 64, nullable = false)
    private String cdrNo;

    /**
     * 충전기 식별자 (8자리 정규화)
     */
    @Column(name = "charge_box_id", length = 50, nullable = false)
    private String chargeBoxId;

    /**
     * 충전 커넥터 번호 (1: C1, 2: C2)
     */
    @Column(name = "connector_id", nullable = false)
    private int connectorId = 1;

    /**
     * OCPP 거래 번호 (전역 단일 시퀀스)
     */
    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    /**
     * 연관 세션 일련번호 (transaction_session.id)
     */
    @Column(name = "session_id")
    private Long sessionId;

    /**
     * 인증/결제 회원 태그 식별자
     */
    @Column(name = "id_tag", length = 64, nullable = false)
    private String idTag;

    /**
     * 법인/그룹 상위 대표 태그 식별자
     */
    @Column(name = "parent_id_tag", length = 64)
    private String parentIdTag;

    /**
     * 충전 개시 일시
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * 충전 종료 일시
     */
    @Column(name = "stop_time", nullable = false)
    private LocalDateTime stopTime;

    /**
     * 총 충전 소요 초 (Seconds)
     */
    @Column(name = "charging_seconds", nullable = false)
    private long chargingSeconds = 0L;

    /**
     * 충전 시작 미터값 (Wh)
     */
    @Column(name = "start_meter_wh", nullable = false)
    private long startMeterWh = 0L;

    /**
     * 충전 종료 미터값 (Wh)
     */
    @Column(name = "stop_meter_wh", nullable = false)
    private long stopMeterWh = 0L;

    /**
     * 총 충전 전력량 (kWh, 소수점 3자리)
     */
    @Column(name = "total_energy_kwh", precision = 10, scale = 3, nullable = false)
    private BigDecimal totalEnergyKwh = BigDecimal.ZERO.setScale(3);

    /**
     * 적용된 요금 정책 식별자
     */
    @Column(name = "policy_id", length = 32, nullable = false)
    private String policyId = "DEFAULT_TOU";

    /**
     * 전력량 요금 합계 (세전, 원)
     */
    @Column(name = "energy_fee", precision = 12, scale = 2, nullable = false)
    private BigDecimal energyFee = BigDecimal.ZERO.setScale(2);

    /**
     * 기본 요금 및 점유 페널티 (원)
     */
    @Column(name = "basic_fee", precision = 12, scale = 2, nullable = false)
    private BigDecimal basicFee = BigDecimal.ZERO.setScale(2);

    /**
     * 회원/제휴 할인 금액 (원)
     */
    @Column(name = "discount_fee", precision = 12, scale = 2, nullable = false)
    private BigDecimal discountFee = BigDecimal.ZERO.setScale(2);

    /**
     * 부가가치세 (10%, 원단위 사사오입)
     */
    @Column(name = "vat", precision = 12, scale = 2, nullable = false)
    private BigDecimal vat = BigDecimal.ZERO.setScale(2);

    /**
     * 최종 결제 청구 금액 (ENERGY + BASIC - DISCOUNT + VAT, 원)
     */
    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);

    /**
     * 결제 상태 (PENDING, PAID, FAILED, CANCELLED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 32, nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /**
     * 시간대별(경/중/최대부하) 사용량 및 단가 분할 JSON 문자열
     */
    @Column(name = "breakdown_json", columnDefinition = "json")
    private String breakdownJson;

    @CreationTimestamp
    @Column(name = "reg_dt", nullable = false, updatable = false)
    private LocalDateTime regDt;

    @UpdateTimestamp
    @Column(name = "updt_dt", nullable = false)
    private LocalDateTime updtDt;

    protected ChargeDetailRecord() {
    }

    public ChargeDetailRecord(Long id, String cdrNo, String chargeBoxId, int connectorId,
                              Long transactionId, Long sessionId, String idTag, String parentIdTag,
                              LocalDateTime startTime, LocalDateTime stopTime, long chargingSeconds,
                              long startMeterWh, long stopMeterWh, BigDecimal totalEnergyKwh,
                              String policyId, BigDecimal energyFee, BigDecimal basicFee,
                              BigDecimal discountFee, BigDecimal vat, BigDecimal totalAmount,
                              PaymentStatus paymentStatus, String breakdownJson,
                              LocalDateTime regDt, LocalDateTime updtDt) {
        this.id = id;
        this.cdrNo = cdrNo;
        this.chargeBoxId = chargeBoxId;
        this.connectorId = connectorId;
        this.transactionId = transactionId;
        this.sessionId = sessionId;
        this.idTag = idTag;
        this.parentIdTag = parentIdTag;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.chargingSeconds = chargingSeconds;
        this.startMeterWh = startMeterWh;
        this.stopMeterWh = stopMeterWh;
        this.totalEnergyKwh = totalEnergyKwh != null ? totalEnergyKwh : BigDecimal.ZERO.setScale(3);
        this.policyId = policyId != null ? policyId : "DEFAULT_TOU";
        this.energyFee = energyFee != null ? energyFee : BigDecimal.ZERO.setScale(2);
        this.basicFee = basicFee != null ? basicFee : BigDecimal.ZERO.setScale(2);
        this.discountFee = discountFee != null ? discountFee : BigDecimal.ZERO.setScale(2);
        this.vat = vat != null ? vat : BigDecimal.ZERO.setScale(2);
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO.setScale(2);
        this.paymentStatus = paymentStatus != null ? paymentStatus : PaymentStatus.PENDING;
        this.breakdownJson = breakdownJson;
        this.regDt = regDt;
        this.updtDt = updtDt;
    }

    public Long getId() {
        return id;
    }

    public String getCdrNo() {
        return cdrNo;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public int getConnectorId() {
        return connectorId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public String getIdTag() {
        return idTag;
    }

    public String getParentIdTag() {
        return parentIdTag;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getStopTime() {
        return stopTime;
    }

    public long getChargingSeconds() {
        return chargingSeconds;
    }

    public long getStartMeterWh() {
        return startMeterWh;
    }

    public long getStopMeterWh() {
        return stopMeterWh;
    }

    public BigDecimal getTotalEnergyKwh() {
        return totalEnergyKwh;
    }

    public String getPolicyId() {
        return policyId;
    }

    public BigDecimal getEnergyFee() {
        return energyFee;
    }

    public BigDecimal getBasicFee() {
        return basicFee;
    }

    public BigDecimal getDiscountFee() {
        return discountFee;
    }

    public BigDecimal getVat() {
        return vat;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public String getBreakdownJson() {
        return breakdownJson;
    }

    public LocalDateTime getRegDt() {
        return regDt;
    }

    public LocalDateTime getUpdtDt() {
        return updtDt;
    }

    /**
     * 결제 완료 처리
     */
    public void markAsPaid() {
        this.paymentStatus = PaymentStatus.PAID;
    }

    /**
     * 결제 실패 처리
     */
    public void markAsFailed() {
        this.paymentStatus = PaymentStatus.FAILED;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String cdrNo;
        private String chargeBoxId;
        private int connectorId = 1;
        private Long transactionId;
        private Long sessionId;
        private String idTag;
        private String parentIdTag;
        private LocalDateTime startTime;
        private LocalDateTime stopTime;
        private long chargingSeconds = 0L;
        private long startMeterWh = 0L;
        private long stopMeterWh = 0L;
        private BigDecimal totalEnergyKwh = BigDecimal.ZERO.setScale(3);
        private String policyId = "DEFAULT_TOU";
        private BigDecimal energyFee = BigDecimal.ZERO.setScale(2);
        private BigDecimal basicFee = BigDecimal.ZERO.setScale(2);
        private BigDecimal discountFee = BigDecimal.ZERO.setScale(2);
        private BigDecimal vat = BigDecimal.ZERO.setScale(2);
        private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
        private PaymentStatus paymentStatus = PaymentStatus.PENDING;
        private String breakdownJson;
        private LocalDateTime regDt;
        private LocalDateTime updtDt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder cdrNo(String cdrNo) {
            this.cdrNo = cdrNo;
            return this;
        }

        public Builder chargeBoxId(String chargeBoxId) {
            this.chargeBoxId = chargeBoxId;
            return this;
        }

        public Builder connectorId(int connectorId) {
            this.connectorId = connectorId;
            return this;
        }

        public Builder transactionId(Long transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder sessionId(Long sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder idTag(String idTag) {
            this.idTag = idTag;
            return this;
        }

        public Builder parentIdTag(String parentIdTag) {
            this.parentIdTag = parentIdTag;
            return this;
        }

        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder stopTime(LocalDateTime stopTime) {
            this.stopTime = stopTime;
            return this;
        }

        public Builder chargingSeconds(long chargingSeconds) {
            this.chargingSeconds = chargingSeconds;
            return this;
        }

        public Builder startMeterWh(long startMeterWh) {
            this.startMeterWh = startMeterWh;
            return this;
        }

        public Builder stopMeterWh(long stopMeterWh) {
            this.stopMeterWh = stopMeterWh;
            return this;
        }

        public Builder totalEnergyKwh(BigDecimal totalEnergyKwh) {
            this.totalEnergyKwh = totalEnergyKwh;
            return this;
        }

        public Builder policyId(String policyId) {
            this.policyId = policyId;
            return this;
        }

        public Builder energyFee(BigDecimal energyFee) {
            this.energyFee = energyFee;
            return this;
        }

        public Builder basicFee(BigDecimal basicFee) {
            this.basicFee = basicFee;
            return this;
        }

        public Builder discountFee(BigDecimal discountFee) {
            this.discountFee = discountFee;
            return this;
        }

        public Builder vat(BigDecimal vat) {
            this.vat = vat;
            return this;
        }

        public Builder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder paymentStatus(PaymentStatus paymentStatus) {
            this.paymentStatus = paymentStatus;
            return this;
        }

        public Builder breakdownJson(String breakdownJson) {
            this.breakdownJson = breakdownJson;
            return this;
        }

        public Builder regDt(LocalDateTime regDt) {
            this.regDt = regDt;
            return this;
        }

        public Builder updtDt(LocalDateTime updtDt) {
            this.updtDt = updtDt;
            return this;
        }

        public ChargeDetailRecord build() {
            return new ChargeDetailRecord(id, cdrNo, chargeBoxId, connectorId, transactionId,
                    sessionId, idTag, parentIdTag, startTime, stopTime, chargingSeconds,
                    startMeterWh, stopMeterWh, totalEnergyKwh, policyId, energyFee, basicFee,
                    discountFee, vat, totalAmount, paymentStatus, breakdownJson, regDt, updtDt);
        }
    }
}
