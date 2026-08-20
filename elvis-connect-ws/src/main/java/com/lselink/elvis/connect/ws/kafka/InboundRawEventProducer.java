package com.lselink.elvis.connect.ws.kafka;

import com.lselink.elvis.connect.ws.dto.OcppRawEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 충전기로부터 수신된 OCPP Raw JSON 패킷을 Kafka ocpp-raw-events 토픽으로 비동기 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InboundRawEventProducer {

    private final KafkaTemplate<String, OcppRawEventEnvelope> kafkaTemplate;

    @Value("${elvis.gateway.kafka.inbound-topic:ocpp-raw-events}")
    private String inboundTopic;

    /**
     * 비동기 이벤트 발행 (I/O 루프 블로킹 방지)
     */
    public CompletableFuture<SendResult<String, OcppRawEventEnvelope>> sendRawEvent(OcppRawEventEnvelope envelope) {
        String partitionKey = envelope.getChargeBoxId();

        CompletableFuture<SendResult<String, OcppRawEventEnvelope>> future =
                kafkaTemplate.send(inboundTopic, partitionKey, envelope);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[InboundProducer] Kafka 발행 실패: chargeBoxId={}, topic={}, error={}",
                        partitionKey, inboundTopic, ex.getMessage(), ex);
            } else {
                log.debug("[InboundProducer] Kafka 발행 성공: chargeBoxId={}, offset={}",
                        partitionKey, result.getRecordMetadata().offset());
            }
        });

        return future;
    }
}
