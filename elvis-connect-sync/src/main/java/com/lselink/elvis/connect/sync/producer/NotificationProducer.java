package com.lselink.elvis.connect.sync.producer;

import com.lselink.elvis.connect.sync.dto.FaultNotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 관제 UI 브로드캐스트용 Kafka 알림 Producer (Sprint 23: S23-SYNC-0060-CO)
 * <p>
 * 장애 감지 및 복구 이벤트를 {@code ocpp-ui-notifications} 토픽으로 전송합니다.
 * </p>
 */
@Component
public class NotificationProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String notificationTopic;

    public NotificationProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${elvis.sync.kafka.notification-topic:ocpp-ui-notifications}") String notificationTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.notificationTopic = notificationTopic;
    }

    /**
     * 장애/복구 알림 이벤트를 Kafka 토픽으로 발행
     *
     * @param chargeBoxId 파티션 키로 사용될 충전기 식별자 (8자리)
     * @param event       알림 이벤트 DTO
     */
    public void sendFaultNotification(String chargeBoxId, FaultNotificationEvent event) {
        String topicName = this.notificationTopic;
        String eventId = event.eventId();
        String eventType = event.eventType();

        log.info("[NotificationProducer] 알림 발행 시작: topic={}, key={}, eventId={}, type={}",
                topicName, chargeBoxId, eventId, eventType);

        kafkaTemplate.send(topicName, chargeBoxId, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[NotificationProducer] 알림 발행 실패: key={}, eventId={}, error={}",
                        chargeBoxId, eventId, ex.getMessage(), ex);
            } else {
                long offset = result.getRecordMetadata().offset();
                int partition = result.getRecordMetadata().partition();
                log.info("[NotificationProducer] 알림 발행 성공: key={}, eventId={}, partition={}, offset={}",
                        chargeBoxId, eventId, partition, offset);
            }
        });
    }
}
