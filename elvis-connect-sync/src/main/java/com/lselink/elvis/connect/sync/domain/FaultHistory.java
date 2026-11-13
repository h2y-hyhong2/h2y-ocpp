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
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * 충전기 장애 발생 및 복구 이력 원장 엔티티 (Sprint 23: S23-SYNC-0060-CO)
 * <p>
 * 충전기 고장(Faulted) 및 에러코드 수신 시 생성되며, 정상 복구(NoError) 시 자동 마감 처리됩니다.
 * [전사 코딩 표준 준수: Lombok 배제, 순수 자바 엔티티 및 Builder 패턴 구현]
 * </p>
 */
@Entity
@Table(name = "tbl_fault_history",
        indexes = {
                @Index(name = "IX_FAULT_BOX_CONN",
                        columnList = "charge_box_id, connector_id, is_resolved"),
                @Index(name = "IX_FAULT_OCCURRED", columnList = "occurred_dt"),
                @Index(name = "IX_FAULT_SEVERITY", columnList = "severity")
        })
@DynamicInsert
@DynamicUpdate
public class FaultHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fault_id")
    private Long faultId;

    /** 충전기 식별자 (ChargeBoxId, 8자리) */
    @Column(name = "charge_box_id", length = 50, nullable = false)
    private String chargeBoxId;

    /** 커넥터 번호 (0=본체, 1~N=커넥터) */
    @Column(name = "connector_id", nullable = false)
    private int connectorId;

    /** 발생 당시 OCPP 충전 상태 (Faulted 등) */
    @Column(name = "status", length = 32, nullable = false)
    private String status;

    /** OCPP 1.6 표준 에러코드 (OverVoltage, GroundFailure 등) */
    @Column(name = "error_code", length = 50, nullable = false)
    private String errorCode;

    /** 제조사 상세 에러코드 */
    @Column(name = "vendor_error_code", length = 50)
    private String vendorErrorCode;

    /** 심각도 (CRITICAL, MAJOR, MINOR, INFO) */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20, nullable = false)
    private FaultSeverity severity;

    /** 상세 장애 설명 메시지 */
    @Column(name = "fault_msg", length = 500)
    private String faultMsg;

    /** 권장 조치 가이드 (EMERGENCY_SHUTDOWN, INSPECTION, NONE) */
    @Column(name = "action_required", length = 50)
    private String actionRequired;

    /** 장애 발생 일시 */
    @Column(name = "occurred_dt", nullable = false)
    private LocalDateTime occurredDt;

    /** 장애 복구 일시 */
    @Column(name = "resolved_dt")
    private LocalDateTime resolvedDt;

    /** 해결 여부 (Y: 해결완료, N: 미해결/장애중) */
    @Column(name = "is_resolved", columnDefinition = "char(1)", length = 1, nullable = false)
    private String isResolved;

    /** 등록일시 */
    @Column(name = "reg_dt", nullable = false, updatable = false)
    private LocalDateTime regDt;

    /** 수정일시 */
    @Column(name = "updt_dt", nullable = false)
    private LocalDateTime updtDt;

    protected FaultHistory() {}

    public FaultHistory(String chargeBoxId,
            int connectorId,
            String status,
            String errorCode,
            String vendorErrorCode,
            FaultSeverity severity,
            String faultMsg,
            String actionRequired,
            LocalDateTime occurredDt) {
        this.chargeBoxId = chargeBoxId;
        this.connectorId = connectorId;
        this.status = status;
        this.errorCode = errorCode;
        this.vendorErrorCode = vendorErrorCode;
        this.severity = severity;
        this.faultMsg = faultMsg;
        this.actionRequired = actionRequired;
        this.occurredDt = occurredDt;
        this.isResolved = "N";
        this.regDt = LocalDateTime.now();
        this.updtDt = LocalDateTime.now();
    }

    public Long getFaultId() {
        return faultId;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public int getConnectorId() {
        return connectorId;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getVendorErrorCode() {
        return vendorErrorCode;
    }

    public FaultSeverity getSeverity() {
        return severity;
    }

    public String getFaultMsg() {
        return faultMsg;
    }

    public String getActionRequired() {
        return actionRequired;
    }

    public LocalDateTime getOccurredDt() {
        return occurredDt;
    }

    public LocalDateTime getResolvedDt() {
        return resolvedDt;
    }

    public String getIsResolved() {
        return isResolved;
    }

    public LocalDateTime getRegDt() {
        return regDt;
    }

    public LocalDateTime getUpdtDt() {
        return updtDt;
    }

    /**
     * 장애 정상 복구 종결 처리
     *
     * @param resolvedDt 복구 일시
     */
    public void resolveFault(LocalDateTime resolvedDt) {
        this.isResolved = "Y";
        this.resolvedDt = resolvedDt;
        this.updtDt = LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String chargeBoxId;
        private int connectorId;
        private String status;
        private String errorCode;
        private String vendorErrorCode;
        private FaultSeverity severity;
        private String faultMsg;
        private String actionRequired;
        private LocalDateTime occurredDt;

        public Builder chargeBoxId(String chargeBoxId) {
            this.chargeBoxId = chargeBoxId;
            return this;
        }

        public Builder connectorId(int connectorId) {
            this.connectorId = connectorId;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder vendorErrorCode(String vendorErrorCode) {
            this.vendorErrorCode = vendorErrorCode;
            return this;
        }

        public Builder severity(FaultSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder faultMsg(String faultMsg) {
            this.faultMsg = faultMsg;
            return this;
        }

        public Builder actionRequired(String actionRequired) {
            this.actionRequired = actionRequired;
            return this;
        }

        public Builder occurredDt(LocalDateTime occurredDt) {
            this.occurredDt = occurredDt;
            return this;
        }

        public FaultHistory build() {
            return new FaultHistory(chargeBoxId, connectorId, status, errorCode,
                    vendorErrorCode, severity, faultMsg, actionRequired, occurredDt);
        }
    }
}
