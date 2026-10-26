package com.lselink.elvis.control.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 프론트엔드 관제 UI 푸시용 알림 DTO (Sprint 28: S28-API-0060-CO)
 * <p>
 * Kafka {@code ocpp-ui-notifications} 수신 데이터 또는 STOMP {@code /topic/notifications} 브로드캐스트용 모델입니다.
 * </p>
 */
public class UiNotificationDto {

    private String eventId;
    private String eventType;
    private String severity;
    private String chargeBoxId;
    private int connectorId;
    private String status;
    private String errorCode;
    private String vendorErrorCode;
    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private String actionRequired;
    private boolean soundAlert;

    public UiNotificationDto() {
    }

    public UiNotificationDto(String eventId, String eventType, String severity,
                              String chargeBoxId, int connectorId, String status,
                              String errorCode, String vendorErrorCode, String message,
                              LocalDateTime timestamp, String actionRequired, boolean soundAlert) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.severity = severity;
        this.chargeBoxId = chargeBoxId;
        this.connectorId = connectorId;
        this.status = status;
        this.errorCode = errorCode;
        this.vendorErrorCode = vendorErrorCode;
        this.message = message;
        this.timestamp = timestamp;
        this.actionRequired = actionRequired;
        this.soundAlert = soundAlert;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public void setChargeBoxId(String chargeBoxId) {
        this.chargeBoxId = chargeBoxId;
    }

    public int getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(int connectorId) {
        this.connectorId = connectorId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getVendorErrorCode() {
        return vendorErrorCode;
    }

    public void setVendorErrorCode(String vendorErrorCode) {
        this.vendorErrorCode = vendorErrorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getActionRequired() {
        return actionRequired;
    }

    public void setActionRequired(String actionRequired) {
        this.actionRequired = actionRequired;
    }

    public boolean isSoundAlert() {
        return soundAlert;
    }

    public void setSoundAlert(boolean soundAlert) {
        this.soundAlert = soundAlert;
    }
}
