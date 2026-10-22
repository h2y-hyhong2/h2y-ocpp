package com.lselink.elvis.connect.sync.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lselink.elvis.connect.sync.domain.FaultSeverity;

import java.time.LocalDateTime;

/**
 * 관제 UI 및 데스크톱 런처 브로드캐스트용 장애 알림 이벤트 DTO (Sprint 23: S23-SYNC-0060-CO)
 * <p>
 * Kafka `ocpp-ui-notifications` 토픽으로 전송되는 불변 이벤트 레코드입니다.
 * </p>
 *
 * @param eventId         고유 이벤트 ID (예: EVT-20261022-xxxx)
 * @param eventType       이벤트 유형 ("FAULT_DETECTED" | "FAULT_RESOLVED")
 * @param severity        심각도 (CRITICAL, MAJOR, MINOR, INFO)
 * @param chargeBoxId     8자리 충전기 ID (예: 00000001)
 * @param connectorId     커넥터 번호 (0=본체, 1~N=개별커넥터)
 * @param status          OCPP 상태 ("Faulted", "Available" 등)
 * @param errorCode       OCPP 표준 에러코드 ("GroundFailure", "NoError" 등)
 * @param vendorErrorCode 제조사 상세 에러코드
 * @param message         화면 표시용 한국어 알림 문구
 * @param timestamp       이벤트 발생 일시
 * @param actionRequired  권장 조치 가이드 ("EMERGENCY_SHUTDOWN", "INSPECTION", "NONE")
 */
public record FaultNotificationEvent(
        String eventId,
        String eventType,
        FaultSeverity severity,
        String chargeBoxId,
        int connectorId,
        String status,
        String errorCode,
        String vendorErrorCode,
        String message,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        String actionRequired
) {
}
