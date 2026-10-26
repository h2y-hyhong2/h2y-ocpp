package com.lselink.elvis.control.service;

import com.lselink.elvis.control.dto.UiNotificationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 실시간 관제 알림 푸시 및 팬아웃 서비스 구현체 (Sprint 28: S28-API-0060-CO)
 * <p>
 * 수신된 장애 알림을 인메모리 링 버퍼에 적재하고,
 * Spring STOMP 브로커를 통해 전사 공통/충전소별/충전기별 3대 토픽으로 전파합니다.
 * </p>
 */
@Service
public class NotificationPushServiceImpl implements NotificationPushService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPushServiceImpl.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final RecentNotificationBuffer ringBuffer;

    public NotificationPushServiceImpl(SimpMessagingTemplate messagingTemplate,
                                       RecentNotificationBuffer ringBuffer) {
        this.messagingTemplate = messagingTemplate;
        this.ringBuffer = ringBuffer;
    }

    @Override
    public void pushNotification(UiNotificationDto notification) {
        if (notification == null) {
            log.warn("[NotificationPush] 푸시 대상 알림 객체가 null입니다.");
            return;
        }

        // 1. 거버넌스 9조: 설명 변수 추출 및 호이스팅
        String chargeBoxId = notification.getChargeBoxId();
        String eventId = notification.getEventId();
        String eventType = notification.getEventType();
        String severity = notification.getSeverity();
        String stationId = "";

        // 거버넌스 10조 채번 규칙: chargeBoxId 8자리 앞 6자리는 stationId
        if (chargeBoxId != null && chargeBoxId.length() >= 6) {
            stationId = chargeBoxId.substring(0, 6);
        }

        // 2. 인메모리 링 버퍼에 최근 알림 캐싱
        ringBuffer.add(notification);

        // 3. 목적지 토픽 분리 (외부 I/O 변수 분리)
        String commonTopic = "/topic/notifications";
        String stationTopic = "/topic/station/" + stationId;
        String chargerTopic = "/topic/charger/" + chargeBoxId;

        log.info("[NotificationPush] STOMP 알림 푸시 시작: eventId={}, type={}, severity={}, chargeBoxId={}",
                eventId, eventType, severity, chargeBoxId);

        // (1) 전사 공통 관제 토픽 브로드캐스트
        messagingTemplate.convertAndSend(commonTopic, notification);

        // (2) 충전소별 관제 화면 전용 토픽
        if (!stationId.isEmpty()) {
            messagingTemplate.convertAndSend(stationTopic, notification);
        }

        // (3) 충전기 단말 개별 토픽
        if (chargeBoxId != null && !chargeBoxId.isEmpty()) {
            messagingTemplate.convertAndSend(chargerTopic, notification);
        }

        log.info("[NotificationPush] STOMP 팬아웃 완료: topics=[{}, {}, {}]", commonTopic, stationTopic, chargerTopic);
    }
}
