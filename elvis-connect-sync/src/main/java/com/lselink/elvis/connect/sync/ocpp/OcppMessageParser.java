package com.lselink.elvis.connect.sync.ocpp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * OCPP 1.6 raw JSON 전문 파서
 * <p>
 * Kafka ocpp-raw-events 토픽에서 수신된 rawPayload(OCPP 배열 JSON)를
 * {@link OcppParsedMessage}로 디코딩한다.
 * </p>
 *
 * <pre>
 * OCPP 1.6 메시지 포맷:
 *   CALL        : [2, "uniqueId", "Action", {payload}]
 *   CALLRESULT  : [3, "uniqueId", {payload}]
 *   CALLERROR   : [4, "uniqueId", "errorCode", "errorDescription", {details}]
 * </pre>
 */
@Component
public class OcppMessageParser {

    private static final Logger log = LoggerFactory.getLogger(OcppMessageParser.class);
    private static final int MSG_TYPE_CALL = 2;
    private static final int MSG_TYPE_CALLRESULT = 3;
    private static final int MSG_TYPE_CALLERROR = 4;

    private final ObjectMapper objectMapper;

    public OcppMessageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * rawPayload를 파싱하여 OcppParsedMessage를 반환한다.
     * CALL(2), CALLRESULT(3), CALLERROR(4) 모두 유효한 OCPP 프레임으로 파싱하여
     * CALLRESULT/CALLERROR가 파싱 실패(DLT)로 오인되지 않도록 한다.
     *
     * @param chargeBoxId 충전기 식별자
     * @param rawPayload  OCPP JSON 배열 원문
     * @return 파싱 성공 시 Optional(OcppParsedMessage), 파싱 실패 시 Optional.empty()
     */
    public Optional<OcppParsedMessage> parse(String chargeBoxId, String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            if (!root.isArray() || root.size() < 3) {
                return Optional.empty();
            }

            int messageType = root.get(0).asInt(-1);
            String uniqueId = root.get(1).asText(null);
            if (uniqueId == null || uniqueId.isBlank()) {
                return Optional.empty();
            }

            if (messageType == MSG_TYPE_CALL) {
                String action = root.get(2).asText(null);
                if (action == null || action.isBlank()) {
                    return Optional.empty();
                }
                JsonNode payload = root.size() > 3 && root.get(3) != null && !root.get(3).isNull()
                        ? root.get(3)
                        : objectMapper.createObjectNode();

                return Optional.of(OcppParsedMessage.builder()
                        .messageType(MSG_TYPE_CALL)
                        .chargeBoxId(chargeBoxId)
                        .uniqueId(uniqueId)
                        .action(action)
                        .payload(payload)
                        .raw(rawPayload)
                        .build());
            }

            if (messageType == MSG_TYPE_CALLRESULT) {
                JsonNode payload = root.get(2) != null && !root.get(2).isNull()
                        ? root.get(2)
                        : objectMapper.createObjectNode();
                return Optional.of(OcppParsedMessage.builder()
                        .messageType(MSG_TYPE_CALLRESULT)
                        .chargeBoxId(chargeBoxId)
                        .uniqueId(uniqueId)
                        .action("CALLRESULT")
                        .payload(payload)
                        .raw(rawPayload)
                        .build());
            }

            if (messageType == MSG_TYPE_CALLERROR) {
                JsonNode details = root.size() > 4 && root.get(4) != null && !root.get(4).isNull()
                        ? root.get(4)
                        : objectMapper.createObjectNode();
                return Optional.of(OcppParsedMessage.builder()
                        .messageType(MSG_TYPE_CALLERROR)
                        .chargeBoxId(chargeBoxId)
                        .uniqueId(uniqueId)
                        .action("CALLERROR")
                        .payload(details)
                        .raw(rawPayload)
                        .build());
            }

            return Optional.empty();

        } catch (Exception e) {
            log.warn("[OcppParser] rawPayload 파싱 실패: chargeBoxId={}, error={}", chargeBoxId, e.getMessage());
            return Optional.empty();
        }
    }
}
