package com.lselink.elvis.connect.sync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lselink.elvis.connect.sync.ocpp.OcppParsedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * OCPP 1.6J DataTransfer (LS E-Link 확장 규격) 수신 디스패처
 * <p>
 * vendorId / messageId 기반으로 핸들러를 라우팅하며,
 * 미지원 messageId에 대해서는 표준 규격에 따라 UnknownMessageId를 즉시 회신해 단말 타임아웃을 방지합니다.
 * </p>
 */
@Service
public class DataTransferService {

    private static final Logger log = LoggerFactory.getLogger(DataTransferService.class);

    private final ObjectMapper objectMapper;
    private final MeterValuesService meterValuesService;
    private final OcppResponseSender ocppResponseSender;

    public DataTransferService(ObjectMapper objectMapper,
                               MeterValuesService meterValuesService,
                               OcppResponseSender ocppResponseSender) {
        this.objectMapper = objectMapper;
        this.meterValuesService = meterValuesService;
        this.ocppResponseSender = ocppResponseSender;
    }

    public void handleDataTransfer(OcppParsedMessage msg) {
        String chargeBoxId = msg.getChargeBoxId();
        String uniqueId = msg.getUniqueId();
        JsonNode payload = msg.getPayload();

        String vendorId = trimToNull(payload.path("vendorId").asText(null));
        String messageId = trimToNull(payload.path("messageId").asText(null));

        if (messageId == null) {
            log.warn("[DataTransferSvc] messageId 누락 → Rejected 회신: chargeBoxId={}, vendorId={}",
                    chargeBoxId, vendorId);
            ocppResponseSender.sendDataTransferResponse(chargeBoxId, uniqueId, "Rejected", null);
            return;
        }

        JsonNode dataNode;
        try {
            dataNode = parseDataNode(payload.path("data"));
        } catch (Exception e) {
            log.warn("[DataTransferSvc] data JSON 파싱 실패 → Rejected 회신: chargeBoxId={}, messageId={}, error={}",
                    chargeBoxId, messageId, e.getMessage());
            ocppResponseSender.sendDataTransferResponse(chargeBoxId, uniqueId, "Rejected", null);
            return;
        }

        log.info("[DataTransferSvc] 수신 라우팅: chargeBoxId={}, vendorId={}, messageId={}",
                chargeBoxId, vendorId, messageId);

        switch (messageId) {
            case "MeterValues" ->
                    meterValuesService.handleDataTransferMeterValues(chargeBoxId, uniqueId, dataNode);

            // Phase 2 확장 대상: unitprice.req, statusnoti, chargingAlarm, Authorize, vehicleInfo, payment, fullrechgsoc.req, userSetSoc, batteryInfo
            default -> {
                log.warn("[DataTransferSvc] 미구현 DataTransfer messageId → UnknownMessageId 회신: chargeBoxId={}, vendorId={}, messageId={}",
                        chargeBoxId, vendorId, messageId);
                ocppResponseSender.sendDataTransferResponse(chargeBoxId, uniqueId, "UnknownMessageId", null);
            }
        }
    }

    private JsonNode parseDataNode(JsonNode rawDataNode) throws Exception {
        if (rawDataNode == null || rawDataNode.isMissingNode() || rawDataNode.isNull()) {
            return null;
        }
        if (rawDataNode.isTextual()) {
            String text = rawDataNode.asText().trim();
            if (text.isEmpty()) {
                return null;
            }
            return objectMapper.readTree(text);
        }
        return rawDataNode;
    }

    private String trimToNull(String value) {
        return (value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim())) ? null : value.trim();
    }
}
