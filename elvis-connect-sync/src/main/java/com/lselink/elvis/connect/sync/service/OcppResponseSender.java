package com.lselink.elvis.connect.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lselink.elvis.connect.sync.dto.OcppOutboundCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * OCPP 1.6-J 표준 CALLRESULT (MessageType = 3) 하향 회신 전담 서비스
 * <p>
 * [3, "<uniqueId>", { ...payload... }] 규격을 조립하여
 * Kafka ocpp-outbound-commands 토픽으로 즉시 발행합니다.
 * </p>
 */
@Service
public class OcppResponseSender {

    private static final Logger log = LoggerFactory.getLogger(OcppResponseSender.class);

    private static final int MSG_TYPE_CALLRESULT = 3;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${elvis.sync.kafka.outbound-topic:ocpp-outbound-commands}")
    private String outboundTopic;

    public OcppResponseSender(KafkaTemplate<String, Object> kafkaTemplate,
                              ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * BootNotification.conf 하향 응답 전송
     * <pre>
     * [3, uniqueId, { "status": "Accepted", "currentTime": "2026-10-22T08:45:00.000Z", "interval": 60 }]
     * </pre>
     */
    public void sendBootNotificationResponse(String chargeBoxId, String uniqueId, int intervalSeconds) {
        ObjectNode payload = objectMapper.createObjectNode();
        String currentUtcTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        payload.put("status", "Accepted");
        payload.put("currentTime", currentUtcTime);
        payload.put("interval", intervalSeconds);

        sendCallResult(chargeBoxId, uniqueId, "BootNotification", payload);
    }

    /**
     * Heartbeat.conf 하향 응답 전송
     * <pre>
     * [3, uniqueId, { "currentTime": "2026-10-22T08:45:00.000Z" }]
     * </pre>
     */
    public void sendHeartbeatResponse(String chargeBoxId, String uniqueId) {
        ObjectNode payload = objectMapper.createObjectNode();
        String currentUtcTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        payload.put("currentTime", currentUtcTime);

        sendCallResult(chargeBoxId, uniqueId, "Heartbeat", payload);
    }

    /**
     * StatusNotification.conf 하향 응답 전송
     * <pre>
     * [3, uniqueId, {}]
     * </pre>
     */
    public void sendStatusNotificationResponse(String chargeBoxId, String uniqueId) {
        ObjectNode payload = objectMapper.createObjectNode();
        sendCallResult(chargeBoxId, uniqueId, "StatusNotification", payload);
    }

    /**
     * StartTransaction.conf 하향 응답 전송
     * <pre>
     * [3, uniqueId, { "idTagInfo": { "status": "Accepted" }, "transactionId": 12345 }]
     * </pre>
     */
    public void sendStartTransactionResponse(String chargeBoxId, String uniqueId, long transactionId, String idTagStatus) {
        ObjectNode payload = objectMapper.createObjectNode();

        ObjectNode idTagInfo = objectMapper.createObjectNode();
        String effectiveStatus = "Accepted";
        if (idTagStatus != null && !idTagStatus.isBlank()) {
            effectiveStatus = idTagStatus;
        }
        idTagInfo.put("status", effectiveStatus);

        payload.set("idTagInfo", idTagInfo);
        payload.put("transactionId", transactionId);

        sendCallResult(chargeBoxId, uniqueId, "StartTransaction", payload);
    }

    /**
     * StopTransaction.conf 하향 응답 전송
     * <pre>
     * [3, uniqueId, { "idTagInfo": { "status": "Accepted" } }]
     * </pre>
     */
    public void sendStopTransactionResponse(String chargeBoxId, String uniqueId, String idTagStatus) {
        ObjectNode payload = objectMapper.createObjectNode();

        if (idTagStatus != null && !idTagStatus.isBlank()) {
            ObjectNode idTagInfo = objectMapper.createObjectNode();
            idTagInfo.put("status", idTagStatus);
            payload.set("idTagInfo", idTagInfo);
        }

        sendCallResult(chargeBoxId, uniqueId, "StopTransaction", payload);
    }

    /**
     * MeterValues.conf 하향 응답 전송
     * <pre>
     * [3, uniqueId, {}]
     * </pre>
     */
    public void sendMeterValuesResponse(String chargeBoxId, String uniqueId) {
        ObjectNode payload = objectMapper.createObjectNode();
        sendCallResult(chargeBoxId, uniqueId, "MeterValues", payload);
    }

    /**
     * OCPP 1.6-J CALLRESULT [3, uniqueId, payload] 공통 조립 및 Kafka 하향 발행
     */
    public void sendCallResult(String chargeBoxId, String uniqueId, String action, ObjectNode payload) {
        if (uniqueId == null || uniqueId.isBlank()) {
            log.warn("[OcppSender] uniqueId 부재로 CALLRESULT 발행 스킵: chargeBoxId={}, action={}", chargeBoxId, action);
            return;
        }

        try {
            ArrayNode root = objectMapper.createArrayNode();
            root.add(MSG_TYPE_CALLRESULT);
            root.add(uniqueId);
            root.add(payload);

            String callResultJson = objectMapper.writeValueAsString(root);

            OcppOutboundCommand outboundCmd = OcppOutboundCommand.builder()
                    .chargeBoxId(chargeBoxId)
                    .action(action)
                    .commandPayload(callResultJson)
                    .messageId(uniqueId)
                    .build();

            log.info("[OcppSender] 하향 CALLRESULT 발행: chargeBoxId={}, action={}, uniqueId={}, payload={}",
                    chargeBoxId, action, uniqueId, callResultJson);

            kafkaTemplate.send(outboundTopic, chargeBoxId, outboundCmd)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("[OcppSender] CALLRESULT 하향 전송 실패: chargeBoxId={}, action={}, error={}",
                                    chargeBoxId, action, ex.getMessage(), ex);
                        } else {
                            log.debug("[OcppSender] CALLRESULT 하향 전송 완료: chargeBoxId={}, action={}, offset={}",
                                    chargeBoxId, action, result.getRecordMetadata().offset());
                        }
                    });

        } catch (Exception e) {
            log.error("[OcppSender] CALLRESULT 직렬화 실패: chargeBoxId={}, action={}, error={}",
                    chargeBoxId, action, e.getMessage(), e);
        }
    }
}
