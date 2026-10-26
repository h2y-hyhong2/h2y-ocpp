package com.lselink.elvis.control.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lselink.elvis.control.dto.UiNotificationDto;
import com.lselink.elvis.control.service.NotificationPushService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka ocpp-ui-notifications 토픽 수신 컨슈머 (Sprint 28: S28-API-0060-CO)
 * <p>
 * elvis-connect-sync가 발행한 충전기 장애 감지 및 복구 이벤트를 수신하여
 * 관제 UI 브로드캐스트 서비스(NotificationPushService)로 위임합니다.
 * </p>
 */
@Component
public class KafkaNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaNotificationConsumer.class);

    private final NotificationPushService pushService;
    private final ObjectMapper objectMapper;

    public KafkaNotificationConsumer(NotificationPushService pushService, ObjectMapper objectMapper) {
        this.pushService = pushService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${elvis.control.kafka.notification-topic:ocpp-ui-notifications}",
            groupId = "${spring.kafka.consumer.group-id:elvis-control-group}"
    )
    public void consumeNotification(ConsumerRecord<String, String> record) {
        String key = record.key();
        String payload = record.value();
        String topic = record.topic();
        int partition = record.partition();
        long offset = record.offset();

        log.info("[KafkaConsumer] 장애 알림 수신: topic={}, partition={}, offset={}, key={}",
                topic, partition, offset, key);

        if (payload == null || payload.trim().isEmpty()) {
            log.warn("[KafkaConsumer] 빈 페이로드가 수신되었습니다: key={}", key);
            return;
        }

        try {
            UiNotificationDto notification = objectMapper.readValue(payload, UiNotificationDto.class);
            pushService.pushNotification(notification);
        } catch (Exception ex) {
            log.error("[KafkaConsumer] 알림 역직렬화 또는 푸시 처리 실패: key={}, payload={}, err={}",
                    key, payload, ex.getMessage(), ex);
        }
    }
}
