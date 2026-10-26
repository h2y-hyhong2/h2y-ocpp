package com.lselink.elvis.control.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lselink.elvis.control.dto.OcppOutboundCommand;
import com.lselink.elvis.control.dto.command.ChangeConfigurationCommandRequest;
import com.lselink.elvis.control.dto.command.CommandResultDto;
import com.lselink.elvis.control.dto.command.RemoteStartCommandRequest;
import com.lselink.elvis.control.dto.command.RemoteStopCommandRequest;
import com.lselink.elvis.control.dto.command.ResetCommandRequest;
import com.lselink.elvis.control.dto.command.TriggerMessageCommandRequest;
import com.lselink.elvis.control.dto.command.UnlockConnectorCommandRequest;
import com.lselink.elvis.control.service.OutboundCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 하향 원격 제어 커맨드 서비스 구현체 (Sprint 31: S31-API-0050-CO)
 * <p>
 * 6대 원격 제어 커맨드를 OCPP 1.6 CALL JSON 프레임으로 직렬화한 후,
 * Kafka ocpp-outbound-commands 토픽으로 chargeBoxId 파티션 키를 부여하여 발행합니다.
 * 거버넌스 9조 Clean Code 및 10조 채번 표준을 엄격히 준수합니다.
 * </p>
 */
@Service
public class OutboundCommandServiceImpl implements OutboundCommandService {

    private static final Logger log = LoggerFactory.getLogger(OutboundCommandServiceImpl.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${elvis.control.kafka.outbound-topic:ocpp-outbound-commands}")
    private String outboundTopic;

    public OutboundCommandServiceImpl(KafkaTemplate<String, String> kafkaTemplate,
                                     ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public CommandResultDto remoteStart(RemoteStartCommandRequest request) {
        String chargeBoxId = sanitizeChargeBoxId(request.getChargeBoxId());
        String action = "RemoteStartTransaction";
        String messageId = UUID.randomUUID().toString();

        Map<String, Object> payloadMap = new LinkedHashMap<>();
        Integer connectorId = request.getConnectorId();
        if (connectorId != null && connectorId > 0) {
            payloadMap.put("connectorId", connectorId);
        } else {
            payloadMap.put("connectorId", 1);
        }

        String idTag = request.getIdTag();
        if (idTag != null && !idTag.trim().isEmpty()) {
            payloadMap.put("idTag", idTag.trim());
        } else {
            payloadMap.put("idTag", "RFID-UNKNOWN");
        }

        return sendOutboundCommand(chargeBoxId, action, messageId, payloadMap);
    }

    @Override
    public CommandResultDto remoteStop(RemoteStopCommandRequest request) {
        String chargeBoxId = sanitizeChargeBoxId(request.getChargeBoxId());
        String action = "RemoteStopTransaction";
        String messageId = UUID.randomUUID().toString();

        Map<String, Object> payloadMap = new LinkedHashMap<>();
        Integer transactionId = request.getTransactionId();
        if (transactionId != null && transactionId > 0) {
            payloadMap.put("transactionId", transactionId);
        } else {
            payloadMap.put("transactionId", 0);
        }

        return sendOutboundCommand(chargeBoxId, action, messageId, payloadMap);
    }

    @Override
    public CommandResultDto reset(ResetCommandRequest request) {
        String chargeBoxId = sanitizeChargeBoxId(request.getChargeBoxId());
        String action = "Reset";
        String messageId = UUID.randomUUID().toString();

        Map<String, Object> payloadMap = new LinkedHashMap<>();
        String resetType = request.getType();
        if (resetType != null && "Hard".equalsIgnoreCase(resetType)) {
            payloadMap.put("type", "Hard");
        } else {
            payloadMap.put("type", "Soft");
        }

        return sendOutboundCommand(chargeBoxId, action, messageId, payloadMap);
    }

    @Override
    public CommandResultDto unlockConnector(UnlockConnectorCommandRequest request) {
        String chargeBoxId = sanitizeChargeBoxId(request.getChargeBoxId());
        String action = "UnlockConnector";
        String messageId = UUID.randomUUID().toString();

        Map<String, Object> payloadMap = new LinkedHashMap<>();
        Integer connectorId = request.getConnectorId();
        if (connectorId != null && connectorId > 0) {
            payloadMap.put("connectorId", connectorId);
        } else {
            payloadMap.put("connectorId", 1);
        }

        return sendOutboundCommand(chargeBoxId, action, messageId, payloadMap);
    }

    @Override
    public CommandResultDto changeConfiguration(ChangeConfigurationCommandRequest request) {
        String chargeBoxId = sanitizeChargeBoxId(request.getChargeBoxId());
        String action = "ChangeConfiguration";
        String messageId = UUID.randomUUID().toString();

        Map<String, Object> payloadMap = new LinkedHashMap<>();
        String key = request.getKey();
        if (key != null) {
            payloadMap.put("key", key.trim());
        } else {
            payloadMap.put("key", "");
        }

        String value = request.getValue();
        if (value != null) {
            payloadMap.put("value", value.trim());
        } else {
            payloadMap.put("value", "");
        }

        return sendOutboundCommand(chargeBoxId, action, messageId, payloadMap);
    }

    @Override
    public CommandResultDto triggerMessage(TriggerMessageCommandRequest request) {
        String chargeBoxId = sanitizeChargeBoxId(request.getChargeBoxId());
        String action = "TriggerMessage";
        String messageId = UUID.randomUUID().toString();

        Map<String, Object> payloadMap = new LinkedHashMap<>();
        String requestedMessage = request.getRequestedMessage();
        if (requestedMessage != null && !requestedMessage.trim().isEmpty()) {
            payloadMap.put("requestedMessage", requestedMessage.trim());
        } else {
            payloadMap.put("requestedMessage", "StatusNotification");
        }

        Integer connectorId = request.getConnectorId();
        if (connectorId != null && connectorId > 0) {
            payloadMap.put("connectorId", connectorId);
        }

        return sendOutboundCommand(chargeBoxId, action, messageId, payloadMap);
    }

    /**
     * OCPP 1.6 CALL JSON 4-Tuple 배열 조립 및 Kafka 토픽 발행 (Graceful Degradation 포함)
     */
    private CommandResultDto sendOutboundCommand(String chargeBoxId,
                                                String action,
                                                String messageId,
                                                Map<String, Object> payloadMap) {
        try {
            // 1. OCPP 1.6 CALL 4-Tuple 배열: [2, "<messageId>", "<Action>", { <payload> }]
            List<Object> callFrame = new ArrayList<>();
            callFrame.add(2);
            callFrame.add(messageId);
            callFrame.add(action);
            callFrame.add(payloadMap);

            String callJson = objectMapper.writeValueAsString(callFrame);

            // 2. 게이트웨이 전송 래퍼 DTO 조립
            OcppOutboundCommand outboundCommand = OcppOutboundCommand.builder()
                    .chargeBoxId(chargeBoxId)
                    .commandPayload(callJson)
                    .action(action)
                    .messageId(messageId)
                    .createdAt(Instant.now())
                    .build();

            String outboundCommandJson = objectMapper.writeValueAsString(outboundCommand);

            // 3. 외부 I/O 변수 분리 후 Kafka 발행 (파티션 키: chargeBoxId)
            String targetTopic = this.outboundTopic;
            String partitionKey = chargeBoxId;

            log.info("[OutboundService] 하향 제어 커맨드 발행 시도: topic={}, key={}, action={}, messageId={}",
                    targetTopic, partitionKey, action, messageId);

            kafkaTemplate.send(targetTopic, partitionKey, outboundCommandJson)
                    .get(3, TimeUnit.SECONDS);

            log.info("[OutboundService] 하향 제어 커맨드 발행 성공: chargeBoxId={}, action={}, messageId={}",
                    chargeBoxId, action, messageId);

            String successMessage = "하향 제어 명령(" + action + ")이 게이트웨이로 정상 발송되었습니다.";
            return CommandResultDto.sent(messageId, chargeBoxId, action, successMessage);

        } catch (Exception ex) {
            log.error("[OutboundService] Kafka 하향 제어 커맨드 발행 실패: chargeBoxId={}, action={}, error={}",
                    chargeBoxId, action, ex.getMessage(), ex);

            String failMessage = "하향 제어 명령(" + action + ") 발송 실패 (사유: " + ex.getMessage() + ")";
            return CommandResultDto.failed(messageId, chargeBoxId, action, failMessage);
        }
    }

    /**
     * 거버넌스 10조 채번 표준에 따른 chargeBoxId 정규화
     */
    private String sanitizeChargeBoxId(String chargeBoxId) {
        if (chargeBoxId == null || chargeBoxId.trim().isEmpty()) {
            return "00000001";
        }
        return chargeBoxId.trim();
    }
}
