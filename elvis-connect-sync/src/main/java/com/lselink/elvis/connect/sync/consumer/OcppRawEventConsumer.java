package com.lselink.elvis.connect.sync.consumer;

import com.lselink.elvis.connect.sync.dto.OcppRawEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * Kafka ocpp-raw-events 토픽을 소비하여 Virtual Threads로 비동기 파싱 및 전처리 파이프라인 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcppRawEventConsumer {

    private final Executor virtualThreadExecutor;

    @KafkaListener(
            topics = "${elvis.sync.kafka.inbound-topic:ocpp-raw-events}",
            groupId = "${spring.kafka.consumer.group-id:elvis-connect-sync-group}"
    )
    public void consumeRawEvent(OcppRawEventEnvelope envelope) {
        // Java 25 가상 스레드로 작업 비동기 위임 (I/O 병목 및 블로킹 분리)
        virtualThreadExecutor.execute(() -> {
            try {
                processEnvelope(envelope);
            } catch (Exception e) {
                log.error("[SyncConsumer] 패킷 처리 실패: chargeBoxId={}, error={}",
                        envelope.getChargeBoxId(), e.getMessage(), e);
            }
        });
    }

    private void processEnvelope(OcppRawEventEnvelope envelope) {
        log.info("[SyncConsumer] 가상스레드[{}] 패킷 수신 및 전처리 시작: chargeBoxId={}, len={}",
                Thread.currentThread().getName(), envelope.getChargeBoxId(),
                envelope.getRawPayload() != null ? envelope.getRawPayload().length() : 0);

        // 향후 Sprint 17~21 OCPP 파서 및 TOU/CDR 연산 로직 연계 지점
    }
}
