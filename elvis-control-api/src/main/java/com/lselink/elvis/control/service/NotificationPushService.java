package com.lselink.elvis.control.service;

import com.lselink.elvis.control.dto.UiNotificationDto;

/**
 * 실시간 관제 알림 푸시 및 팬아웃(Fan-out) 서비스 인터페이스 (Sprint 28: S28-API-0060-CO)
 */
public interface NotificationPushService {

    /**
     * 알림을 인메모리 버퍼에 캐싱하고 WebSocket STOMP 토픽으로 팬아웃 전송
     *
     * @param notification UI 알림 DTO
     */
    void pushNotification(UiNotificationDto notification);
}
