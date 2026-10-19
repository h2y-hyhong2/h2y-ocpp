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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 충전 과금 원장 (CDR: Charge Detail Record) 엔티티 (Sprint 21: S21-SYNC-0080-CO)
 * <p>
 * 단일 충전 세션의 최종 소비 전력량, 적용 단가, 시간대별 분할 상세 및 최종 결제 금액을 확정한 불변 마스터 원장입니다.
 * </p>
 */
@Entity
@Table(
        name = "tb_charge_detail_record",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_CDR_NO", columnNames = {"CDR_NO"}),
                @UniqueConstraint(name = "UK_CDR_CHARGE_BOX_TRN", columnNames = {"CHARGE_BOX_ID", "TRANSACTION_ID"})
        },
        indexes = {
                @Index(name = "IX_CDR_STOP_TIME", columnList = "STOP_TIME"),
                @Index(name = "IX_CDR_ID_TAG", columnList = "ID_TAG"),
                @Index(name = "IX_CDR_SESSION_ID", columnList = "SESSION_ID")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChargeDetailRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /**
     * 과금 영수증 번호 (고유키: CDR-YYYYMMDD-CBID-TRNID)
     */
    @Column(name = "CDR_NO", length = 48, nullable = false)
    private String cdrNo;

    /**
     * 충전기 식별자 (8자리 정규화)
     */
    @Column(name = "CHARGE_BOX_ID", length = 64, nullable = false)
    private String chargeBoxId;

    /**
     * 충전 커넥터 번호 (1: C1, 2: C2)
     */
    @Column(name = "CONNECTOR_ID", nullable = false)
    @Builder.Default
    private int connectorId = 1;

    /**
     * OCPP 거래 번호 (전역 단일 시퀀스)
     */
    @Column(name = "TRANSACTION_ID", nullable = false)
    private Long transactionId;

    /**
     * 연관 세션 일련번호 (tb_transaction_session.ID)
     */
    @Column(name = "SESSION_ID")
    private Long sessionId;

    /**
     * 인증/결제 회원 태그 식별자
     */
    @Column(name = "ID_TAG", length = 64, nullable = false)
    private String idTag;

    /**
     * 법인/그룹 상위 대표 태그 식별자
     */
    @Column(name = "PARENT_ID_TAG", length = 64)
    private String parentIdTag;

    /**
     * 충전 개시 일시
     */
    @Column(name = "START_TIME", nullable = false)
    private LocalDateTime startTime;

    /**
     * 충전 종료 일시
     */
    @Column(name = "STOP_TIME", nullable = false)
    private LocalDateTime stopTime;

    /**
     * 총 충전 소요 초 (Seconds)
     */
    @Column(name = "CHARGING_SECONDS", nullable = false)
    @Builder.Default
    private long chargingSeconds = 0L;

    /**
     * 충전 시작 미터값 (Wh)
     */
    @Column(name = "START_METER_WH", nullable = false)
    @Builder.Default
    private long startMeterWh = 0L;

    /**
     * 충전 종료 미터값 (Wh)
     */
    @Column(name = "STOP_METER_WH", nullable = false)
    @Builder.Default
    private long stopMeterWh = 0L;

    /**
     * 총 충전 전력량 (kWh, 소수점 3자리)
     */
    @Column(name = "TOTAL_ENERGY_KWH", precision = 10, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal totalEnergyKwh = BigDecimal.ZERO.setScale(3);

    /**
     * 적용된 요금 정책 식별자
     */
    @Column(name = "POLICY_ID", length = 32, nullable = false)
    @Builder.Default
    private String policyId = "DEFAULT_TOU";

    /**
     * 전력량 요금 합계 (세전, 원)
     */
    @Column(name = "ENERGY_FEE", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal energyFee = BigDecimal.ZERO.setScale(2);

    /**
     * 기본 요금 및 점유 페널티 (원)
     */
    @Column(name = "BASIC_FEE", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal basicFee = BigDecimal.ZERO.setScale(2);

    /**
     * 회원/제휴 할인 금액 (원)
     */
    @Column(name = "DISCOUNT_FEE", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal discountFee = BigDecimal.ZERO.setScale(2);

    /**
     * 부가가치세 (10%, 원단위 사사오입)
     */
    @Column(name = "VAT", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal vat = BigDecimal.ZERO.setScale(2);

    /**
     * 최종 결제 청구 금액 (ENERGY + BASIC - DISCOUNT + VAT, 원)
     */
    @Column(name = "TOTAL_AMOUNT", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);

    /**
     * 결제 상태 (PENDING, PAID, FAILED, CANCELLED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_STATUS", length = 32, nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /**
     * 시간대별(경/중/최대부하) 사용량 및 단가 분할 JSON 문자열
     */
    @Column(name = "BREAKDOWN_JSON", columnDefinition = "json")
    private String breakdownJson;

    @CreationTimestamp
    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt;

    @UpdateTimestamp
    @Column(name = "UPDT_DT", nullable = false)
    private LocalDateTime updtDt;

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
}
