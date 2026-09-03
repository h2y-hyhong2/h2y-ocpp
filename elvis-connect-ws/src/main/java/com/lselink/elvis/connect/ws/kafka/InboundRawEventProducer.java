package com.lselink.elvis.connect.ws.kafka;

import com.lselink.elvis.connect.ws.dto.OcppRawEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.lselink.elvis.connect.ws.session.LocalSessionStore;
import org.springframework.web.socket.CloseStatus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 충전기로부터 수신된 OCPP Raw JSON 패킷을 Kafka ocpp-raw-events 토픽으로 비동기 발행
 * Kafka 브로커 장애 감지 시 단말 로컬 버퍼링 유도를 위한 Failover 서킷 브레이커 제공 (REQ-003, REQ-009)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InboundRawEventProducer {

    private final KafkaTemplate<String, OcppRawEventEnvelope> kafkaTemplate;
    private final LocalSessionStore sessionStore;

    @Value("${elvis.gateway.kafka.inbound-topic:ocpp-raw-events}")
    private String inboundTopic;

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicBoolean kafkaHealthy = new AtomicBoolean(true);
    private static final int FAILURE_THRESHOLD = 5;

    /**
     * 비동기 이벤트 발행 (I/O 루프 블로킹 방지)
     */
    public CompletableFuture<SendResult<String, OcppRawEventEnvelope>> sendRawEvent(OcppRawEventEnvelope envelope) {
        String partitionKey = envelope.getChargeBoxId();

        CompletableFuture<SendResult<String, OcppRawEventEnvelope>> future =
                kafkaTemplate.send(inboundTopic, partitionKey, envelope);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                int failures = consecutiveFailures.incrementAndGet();
                log.error("[InboundProducer] Kafka 발행 실패 (연속 {}회): chargeBoxId={}, topic={}, error={}",
                        failures, partitionKey, inboundTopic, ex.getMessage(), ex);

                if (failures >= FAILURE_THRESHOLD && kafkaHealthy.compareAndSet(true, false)) {
                    log.error("[InboundProducer] ⚠️ Kafka Broker 장애 감지! Failover 서킷 오픈 - 단말 소켓 차단 및 로컬 버퍼링 유도");
                }
            } else {
                consecutiveFailures.set(0);
                if (kafkaHealthy.compareAndSet(false, true)) {
                    log.info("[InboundProducer] 🟢 Kafka Broker 정상 복구 감지 - 서비스 정상화");
                }
                log.debug("[InboundProducer] Kafka 발행 성공: chargeBoxId={}, offset={}",
                        partitionKey, result.getRecordMetadata().offset());
            }
        });

        return future;
    }

    public boolean isKafkaHealthy() {
        return kafkaHealthy.get();
    }
}
