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
 * Dead Letter Topic 실패 메시지 격리 및 재처리 원장 엔티티 (Sprint 24: S24-SYNC-0100-CO)
 * <p>
 * 결함 패킷(Poison Pill) 또는 최종 재시도 실패 메시지를 보존하고, 운영자 Replay 라이프사이클을 추적합니다.
 * </p>
 */
@Entity
@Table(name = "tbl_dlt_history",
        indexes = {
                @Index(name = "IX_DLT_BOX", columnList = "charge_box_id"),
                @Index(name = "IX_DLT_STATUS", columnList = "status"),
                @Index(name = "IX_DLT_FAILED", columnList = "failed_dt")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
@DynamicUpdate
public class DltHistory {

    /** DLT 고유 식별자 (PK - 예: DLT-20261022-xxxx) */
    @Id
    @Column(name = "dlt_id", length = 64, nullable = false)
    private String dltId;

    /** 충전기 식별자 (파싱 실패 시 null 가능) */
    @Column(name = "charge_box_id", length = 50)
    private String chargeBoxId;

    /** 원본 인바운드 토픽명 (예: ocpp-raw-events) */
    @Column(name = "source_topic", length = 100, nullable = false)
    private String sourceTopic;

    /** 원본 토픽 파티션 번호 */
    @Column(name = "source_partition", nullable = false)
    private int sourcePartition;

    /** 원본 토픽 오프셋 번호 */
    @Column(name = "source_offset", nullable = false)
    private long sourceOffset;

    /** 오류 분류 (NON_RETRYABLE, TRANSIENT, BUSINESS_LOGIC) */
    @Enumerated(EnumType.STRING)
    @Column(name = "error_category", length = 30, nullable = false)
    private ErrorCategory errorCategory;

    /** 발생 예외 클래스명 */
    @Column(name = "exception_class", length = 200)
    private String exceptionClass;

    /** 오류 메시지 요약 */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /** 원본 수신 전문 (OCPP JSON 전체 문자열) */
    @Lob
    @Column(name = "raw_payload", columnDefinition = "LONGTEXT", nullable = false)
    private String rawPayload;

    /** 재시도 횟수 */
    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    /** 재처리 상태 (PENDING, REPLAYED, DISCARDED, FAILED) */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private DltStatus status;

    /** 최초 실패 일시 */
    @Column(name = "failed_dt", nullable = false)
    private LocalDateTime failedDt;

    /** 재처리 완료 일시 */
    @Column(name = "replayed_dt")
    private LocalDateTime replayedDt;

    /** 등록 일시 */
    @Column(name = "reg_dt", nullable = false, updatable = false)
    private LocalDateTime regDt;

    /** 수정 일시 */
    @Column(name = "updt_dt", nullable = false)
    private LocalDateTime updtDt;

    @Builder
    public DltHistory(String dltId,
                      String chargeBoxId,
                      String sourceTopic,
                      int sourcePartition,
                      long sourceOffset,
                      ErrorCategory errorCategory,
                      String exceptionClass,
                      String errorMessage,
                      String rawPayload,
                      int retryCount,
                      LocalDateTime failedDt) {
        this.dltId = dltId;
        this.chargeBoxId = chargeBoxId;
        this.sourceTopic = sourceTopic;
        this.sourcePartition = sourcePartition;
        this.sourceOffset = sourceOffset;
        this.errorCategory = errorCategory;
        this.exceptionClass = exceptionClass;
        this.errorMessage = errorMessage;
        this.rawPayload = rawPayload;
        this.retryCount = retryCount;
        this.status = DltStatus.PENDING;
        this.failedDt = failedDt;
        this.regDt = LocalDateTime.now();
        this.updtDt = LocalDateTime.now();
    }

    /**
     * 재처리 완료 상태로 전이
     *
     * @param replayedDt 재처리 완료 시각
     */
    public void markReplayed(LocalDateTime replayedDt) {
        this.status = DltStatus.REPLAYED;
        this.replayedDt = replayedDt;
        this.updtDt = LocalDateTime.now();
    }

    /**
     * 영구 폐기 처리
     */
    public void markDiscarded() {
        this.status = DltStatus.DISCARDED;
        this.updtDt = LocalDateTime.now();
    }

    /**
     * 재처리 실패 처리
     */
    public void markFailed(String newErrorMessage) {
        this.status = DltStatus.FAILED;
        this.errorMessage = newErrorMessage;
        this.retryCount = this.retryCount + 1;
        this.updtDt = LocalDateTime.now();
    }
}
