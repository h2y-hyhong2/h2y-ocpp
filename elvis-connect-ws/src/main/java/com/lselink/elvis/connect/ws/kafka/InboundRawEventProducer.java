package com.lselink.elvis.connect.ws.kafka;

import com.lselink.elvis.connect.ws.dto.OcppRawEventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 충전기로부터 수신된 OCPP Raw JSON 패킷을 Kafka ocpp-raw-events 토픽으로 비동기 발행
 * Kafka 브로커 장애 감지 시 단말 로컬 버퍼링 유도를 위한 Failover 서킷 브레이커 제공 (REQ-003, REQ-009)
 */
@Component
public class InboundRawEventProducer {

    private static final Logger log = LoggerFactory.getLogger(InboundRawEventProducer.class);

    private static final int FAILURE_THRESHOLD = 5;
    private static final Duration CIRCUIT_OPEN_DURATION = Duration.ofSeconds(30);

    private final KafkaTemplate<String, OcppRawEventEnvelope> kafkaTemplate;

    public InboundRawEventProducer(KafkaTemplate<String, OcppRawEventEnvelope> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Value("${elvis.gateway.kafka.inbound-topic:ocpp-raw-events}")
    private String inboundTopic;

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicBoolean kafkaHealthy = new AtomicBoolean(true);
    private final AtomicReference<Instant> circuitOpenedAt = new AtomicReference<>();

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

                if (failures >= FAILURE_THRESHOLD) {
                    circuitOpenedAt.set(Instant.now());
                    if (kafkaHealthy.compareAndSet(true, false)) {
                        log.error("[InboundProducer] ⚠️ Kafka Broker 장애 감지! Failover 서킷 오픈 ({}초 후 Half-Open 재시도) - 단말 소켓 차단 및 로컬 버퍼링 유도",
                                CIRCUIT_OPEN_DURATION.toSeconds());
                    }
                }
            } else {
                consecutiveFailures.set(0);
                circuitOpenedAt.set(null);
                if (kafkaHealthy.compareAndSet(false, true)) {
                    log.info("[InboundProducer] 🟢 Kafka Broker 정상 복구 감지 - 서비스 정상화");
                }
                log.debug("[InboundProducer] Kafka 발행 성공: chargeBoxId={}, offset={}",
                        partitionKey, result.getRecordMetadata().offset());
            }
        });

        return future;
    }

    /**
     * 충전기 WebSocket 연결 종료/타임아웃 이벤트 발행 (동일 chargeBoxId 파티션 키로 순서 보장)
     */
    public CompletableFuture<SendResult<String, OcppRawEventEnvelope>> sendDisconnectEvent(
            String chargeBoxId, String sessionId, String remoteAddress) {
        OcppRawEventEnvelope envelope = OcppRawEventEnvelope.builder()
                .eventType(OcppRawEventEnvelope.EVENT_TYPE_DISCONNECTED)
                .chargeBoxId(chargeBoxId)
                .sessionId(sessionId)
                .remoteAddress(remoteAddress != null ? remoteAddress : "unknown")
                .rawPayload(null)
                .receivedAt(Instant.now())
                .build();
        log.info("[InboundProducer] 단말 연결 종료(DISCONNECTED) 이벤트 발행: chargeBoxId={}, sessionId={}",
                chargeBoxId, sessionId);
        return sendRawEvent(envelope);
    }

    /**
     * Kafka 브로커 헬스 상태 반환 (서킷 오픈 후 쿨다운 경과 시 Half-Open 프로브 허용으로 영구 잠금 방지)
     */
    public boolean isKafkaHealthy() {
        if (kafkaHealthy.get()) {
            return true;
        }
        Instant openedAt = circuitOpenedAt.get();
        if (openedAt != null && Duration.between(openedAt, Instant.now()).compareTo(CIRCUIT_OPEN_DURATION) >= 0) {
            if (kafkaHealthy.compareAndSet(false, true)) {
                consecutiveFailures.set(FAILURE_THRESHOLD - 1);
                log.info("[InboundProducer] 🟡 Kafka Failover 서킷 Half-Open 전환 - 복구 프로브 연결 허용");
            }
            return true;
        }
        return false;
    }
}
