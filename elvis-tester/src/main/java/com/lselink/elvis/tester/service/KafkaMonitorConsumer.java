package com.lselink.elvis.tester.service;

import com.lselink.elvis.tester.dto.TestLogEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * 카프카 토픽의 메시지를 실시간 모니터링하여 테스트 UI에 스트리밍 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMonitorConsumer {

    private final KafkaTestService testService;

    @KafkaListener(
            topics = {
                    "${elvis.tester.topics.inbound:ocpp-raw-events}",
                    "${elvis.tester.topics.outbound:ocpp-outbound-commands}",
                    "${elvis.tester.topics.ui-notifications:ocpp-ui-notifications}",
                    "${elvis.tester.topics.dlt:ocpp-raw-events.DLT}"
            },
            groupId = "${spring.kafka.consumer.group-id:elvis-tester-group}"
    )
    public void monitorTopic(ConsumerRecord<String, String> record) {
        String topic = record.topic();
        String key = record.key() != null ? record.key() : "NO_KEY";
        String value = record.value();

        TestLogEntry entry = TestLogEntry.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .direction("MONITOR")
                .topic(topic)
                .chargeBoxId(key)
                .action("TOPIC_EVENT")
                .payload(value)
                .status("SUCCESS")
                .message("파티션: " + record.partition() + ", 오프셋: " + record.offset())
                .build();

        testService.addLog(entry);
        testService.broadcastLog(entry);

        log.debug("[KafkaMonitor] 토픽 수신: topic={}, partition={}, offset={}, key={}",
                topic, record.partition(), record.offset(), key);
    }
}
