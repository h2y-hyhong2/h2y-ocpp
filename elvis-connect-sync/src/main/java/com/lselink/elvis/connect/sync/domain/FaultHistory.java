package com.lselink.elvis.connect.sync.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * 충전기 장애 발생 및 복구 이력 원장 엔티티 (Sprint 23: S23-SYNC-0060-CO)
 * <p>
 * 충전기 고장(Faulted) 및 에러코드 수신 시 생성되며, 정상 복구(NoError) 시 자동 마감 처리됩니다.
 * </p>
 */
@Entity
@Table(name = "tbl_fault_history",
        indexes = {
                @Index(name = "IX_FAULT_BOX_CONN", columnList = "charge_box_id, connector_id, is_resolved"),
                @Index(name = "IX_FAULT_OCCURRED", columnList = "occurred_dt"),
                @Index(name = "IX_FAULT_SEVERITY", columnList = "severity")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Builder
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
}
