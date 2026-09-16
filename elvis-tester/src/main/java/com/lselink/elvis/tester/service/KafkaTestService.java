package com.lselink.elvis.tester.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lselink.elvis.tester.dto.KafkaSendRequest;
import com.lselink.elvis.tester.dto.LoadTestRequest;
import com.lselink.elvis.tester.dto.TestLogEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaTestService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 최근 로그 버퍼 (최대 300건 보관)
    private final Deque<TestLogEntry> recentLogs = new ConcurrentLinkedDeque<>();
    private static final int MAX_LOG_SIZE = 300;

    // 실시간 SSE 연결 관리
    private final List<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();

    // 부하 테스트 실행 상태 관리
    private final AtomicBoolean loadRunning = new AtomicBoolean(false);
    private final AtomicLong totalSentCount = new AtomicLong(0);
    private final AtomicLong totalFailCount = new AtomicLong(0);
    private ExecutorService loadExecutor;

    public TestLogEntry sendMessage(KafkaSendRequest request) {
        String topic = request.getTopic();
        String chargeBoxId = request.getChargeBoxId();
        String payload = request.getPayload();
        String logId = UUID.randomUUID().toString();

        TestLogEntry entry = TestLogEntry.builder()
                .id(logId)
                .timestamp(Instant.now())
                .direction(topic.contains("outbound") ? "OUTBOUND" : "INBOUND")
                .topic(topic)
                .chargeBoxId(chargeBoxId)
                .action(extractAction(payload))
                .payload(payload)
                .status("SUCCESS")
                .message("발송 성공")
                .build();

        try {
            // KafkaTemplate 전송 (Key: chargeBoxId)
            kafkaTemplate.send(topic, chargeBoxId, payload).whenComplete((res, ex) -> {
                if (ex != null) {
                    entry.setStatus("FAILED");
                    entry.setMessage(ex.getMessage());
                    totalFailCount.incrementAndGet();
                    log.error("[KafkaTester] 발송 실패: topic={}, key={}, error={}", topic, chargeBoxId, ex.getMessage());
                } else {
                    totalSentCount.incrementAndGet();
                    log.debug("[KafkaTester] 발송 완료: topic={}, offset={}", topic, res.getRecordMetadata().offset());
                }
                broadcastLog(entry);
            });

            addLog(entry);
        } catch (Exception e) {
            entry.setStatus("FAILED");
            entry.setMessage(e.getMessage());
            totalFailCount.incrementAndGet();
            addLog(entry);
            broadcastLog(entry);
        }

        return entry;
    }

    public synchronized Map<String, Object> startLoadTest(LoadTestRequest request) {
        if (loadRunning.get()) {
            return Map.of("success", false, "message", "부하 테스트가 이미 실행 중입니다.");
        }

        loadRunning.set(true);
        totalSentCount.set(0);
        totalFailCount.set(0);

        // Java 25 Virtual Threads 기반 실행기
        loadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        loadExecutor.submit(() -> {
            log.info("[KafkaTester] 🚀 부하 테스트 시작: TPS={}, Devices={}, Duration={}s",
                    request.getTps(), request.getDeviceCount(), request.getDurationSeconds());

            long intervalNanos = 1_000_000_000L / Math.max(1, request.getTps());
            long endTime = System.currentTimeMillis() + (request.getDurationSeconds() * 1000L);
            Random random = new Random();

            while (loadRunning.get() && System.currentTimeMillis() < endTime) {
                long cycleStart = System.nanoTime();

                int deviceIdx = random.nextInt(request.getDeviceCount()) + 1;
                String chargeBoxId = String.format("%s%04d", request.getChargeBoxPrefix(), deviceIdx);
                String payload = generateLoadPayload(chargeBoxId, request.getTemplateName());

                sendMessage(KafkaSendRequest.builder()
                        .topic(request.getTopic())
                        .chargeBoxId(chargeBoxId)
                        .payload(payload)
                        .build());

                long elapsedNanos = System.nanoTime() - cycleStart;
                long sleepNanos = intervalNanos - elapsedNanos;
                if (sleepNanos > 0) {
                    try {
                        TimeUnit.NANOSECONDS.sleep(sleepNanos);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            loadRunning.set(false);
            log.info("[KafkaTester] 🏁 부하 테스트 종료. 총 발송: {}, 실패: {}",
                    totalSentCount.get(), totalFailCount.get());
        });

        return Map.of("success", true, "message", "부하 테스트가 시작되었습니다.");
    }

    public synchronized Map<String, Object> stopLoadTest() {
        if (!loadRunning.get()) {
            return Map.of("success", false, "message", "실행 중인 부하 테스트가 없습니다.");
        }
        loadRunning.set(false);
        if (loadExecutor != null) {
            loadExecutor.shutdownNow();
        }
        return Map.of("success", true, "message", "부하 테스트가 중지되었습니다.");
    }

    public Map<String, Object> getLoadStatus() {
        return Map.of(
                "running", loadRunning.get(),
                "totalSent", totalSentCount.get(),
                "totalFail", totalFailCount.get()
        );
    }

    public List<TestLogEntry> getRecentLogs() {
        return new ArrayList<>(recentLogs);
    }

    public SseEmitter subscribeStream() {
        SseEmitter emitter = new SseEmitter(180_000L); // 3분 타임아웃
        sseEmitters.add(emitter);

        emitter.onCompletion(() -> sseEmitters.remove(emitter));
        emitter.onTimeout(() -> sseEmitters.remove(emitter));
        emitter.onError(e -> sseEmitters.remove(emitter));

        return emitter;
    }

    public void addLog(TestLogEntry entry) {
        recentLogs.addFirst(entry);
        while (recentLogs.size() > MAX_LOG_SIZE) {
            recentLogs.removeLast();
        }
    }

    public void broadcastLog(TestLogEntry entry) {
        for (SseEmitter emitter : sseEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("kafka-log")
                        .data(entry));
            } catch (IOException e) {
                sseEmitters.remove(emitter);
            }
        }
    }

    private String extractAction(String payload) {
        if (payload == null) return "UNKNOWN";
        try {
            if (payload.trim().startsWith("[")) {
                List<?> list = objectMapper.readValue(payload, List.class);
                if (list.size() > 2 && list.get(2) instanceof String action) {
                    return action;
                }
            } else if (payload.trim().startsWith("{")) {
                Map<?, ?> map = objectMapper.readValue(payload, Map.class);
                if (map.containsKey("action")) {
                    return String.valueOf(map.get("action"));
                }
            }
        } catch (Exception ignored) {
        }
        return "UNKNOWN";
    }

    private String generateLoadPayload(String chargeBoxId, String templateName) {
        String uuid = UUID.randomUUID().toString();
        String now = Instant.now().toString();

        return switch (templateName) {
            case "BootNotification" -> """
                    [2, "%s", "BootNotification", {
                      "chargePointVendor": "LSELINK",
                      "chargePointModel": "ELVIS-TEST",
                      "chargePointSerialNumber": "%s"
                    }]
                    """.formatted(uuid, chargeBoxId);
            case "StatusNotification" -> """
                    [2, "%s", "StatusNotification", {
                      "connectorId": 1,
                      "status": "Occupied",
                      "errorCode": "NoError",
                      "timestamp": "%s"
                    }]
                    """.formatted(uuid, now);
            default -> """
                    [2, "%s", "MeterValues", {
                      "connectorId": 1,
                      "transactionId": 10001,
                      "meterValue": [{
                        "timestamp": "%s",
                        "sampledValue": [
                          { "value": "15000", "unit": "Wh" },
                          { "value": "220.0", "unit": "V" }
                        ]
                      }]
                    }]
                    """.formatted(uuid, now);
        };
    }
}
