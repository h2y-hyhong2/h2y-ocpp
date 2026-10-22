package com.lselink.elvis.connect.sync.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lselink.elvis.connect.sync.domain.DltStatus;
import com.lselink.elvis.connect.sync.domain.ErrorCategory;

import java.time.LocalDateTime;

/**
 * Dead Letter Topic (`ocpp-dlt`) 전송용 표준 격리 메시지 봉투 DTO (Sprint 24: S24-SYNC-0100-CO)
 *
 * @param dltId           DLT 고유 식별자 (예: DLT-20261022-xxxx)
 * @param chargeBoxId     충전기 식별자
 * @param sourceTopic     원본 토픽명
 * @param sourcePartition 원본 파티션 번호
 * @param sourceOffset    원본 오프셋 번호
 * @param rawPayload      수신 원본 전문
 * @param errorCategory   오류 분류 (NON_RETRYABLE, TRANSIENT, BUSINESS_LOGIC)
 * @param exceptionClass  발생 예외 클래스명
 * @param errorMessage    오류 메시지 요약
 * @param retryCount      재시도 횟수
 * @param failedAt        실패 발생 일시
 * @param status          상태 (PENDING 등)
 */
public record DeadLetterEvent(
        String dltId,
        String chargeBoxId,
        String sourceTopic,
        int sourcePartition,
        long sourceOffset,
        String rawPayload,
        ErrorCategory errorCategory,
        String exceptionClass,
        String errorMessage,
        int retryCount,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime failedAt,
        DltStatus status
) {
}
