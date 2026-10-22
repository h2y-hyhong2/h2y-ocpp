package com.lselink.elvis.connect.sync.producer;

import com.lselink.elvis.connect.sync.dto.DeadLetterEvent;
import com.lselink.elvis.connect.sync.dto.OcppRawEventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Dead Letter Topic (`ocpp-dlt`) 발행 및 Replay 전송 Producer (Sprint 24: S24-SYNC-0100-CO)
 */
@Component
public class DltProducer {

    private static final Logger log = LoggerFactory.getLogger(DltProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String dltTopic;
    private final String inboundTopic;

    public DltProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${elvis.sync.kafka.dlt-topic:ocpp-dlt}") String dltTopic,
            @Value("${elvis.sync.kafka.inbound-topic:ocpp-raw-events}") String inboundTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.dltTopic = dltTopic;
        this.inboundTopic = inboundTopic;
    }

    /**
     * 결함 메시지를 Dead Letter Topic (`ocpp-dlt`)으로 발행
     *
     * @param chargeBoxId 충전기 식별자 (파티션 키)
     * @param event       DLT 이벤트 객체
     */
    public void sendToDlt(String chargeBoxId, DeadLetterEvent event) {
        String topicName = this.dltTopic;
        String dltId = event.dltId();
        String partitionKey = chargeBoxId;
        if (partitionKey == null || partitionKey.isBlank()) {
            partitionKey = "UNKNOWN";
        }

        log.warn("[DltProducer] DLT 격리 발행 시작: topic={}, key={}, dltId={}, errorCategory={}",
                topicName, partitionKey, dltId, event.errorCategory());

        kafkaTemplate.send(topicName, partitionKey, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[DltProducer] DLT 발행 실패: key={}, dltId={}, error={}",
                        partitionKey, dltId, ex.getMessage(), ex);
            } else {
                long offset = result.getRecordMetadata().offset();
                int partition = result.getRecordMetadata().partition();
                log.info("[DltProducer] DLT 발행 완료: key={}, dltId={}, partition={}, offset={}",
                        partitionKey, dltId, partition, offset);
            }
        });
    }

    /**
     * 운영자 수동 재처리(Replay): 원본 토픽(`ocpp-raw-events`)으로 메시지 재주입
     *
     * @param chargeBoxId 충전기 ID
     * @param rawPayload  원본 OCPP JSON 전문
     */
    public void replayToInboundTopic(String chargeBoxId, String rawPayload) {
        String topicName = this.inboundTopic;
        String partitionKey = chargeBoxId;
        if (partitionKey == null || partitionKey.isBlank()) {
            partitionKey = "REPLAY";
        }

        OcppRawEventEnvelope envelope = new OcppRawEventEnvelope(chargeBoxId, rawPayload);

        log.info("[DltProducer] Replay 원본 토픽 재주입: topic={}, key={}", topicName, partitionKey);

        kafkaTemplate.send(topicName, partitionKey, envelope).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[DltProducer] Replay 재주입 실패: key={}, error={}",
                        partitionKey, ex.getMessage(), ex);
            } else {
                long offset = result.getRecordMetadata().offset();
                int partition = result.getRecordMetadata().partition();
                log.info("[DltProducer] Replay 재주입 성공: key={}, partition={}, offset={}",
                        partitionKey, partition, offset);
            }
        });
    }
}
