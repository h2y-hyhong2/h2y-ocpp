package com.lselink.elvis.connect.sync.ocpp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
@RequiredArgsConstructor
public class OcppMessageParser {

    private static final int MSG_TYPE_CALL = 2;

    private final ObjectMapper objectMapper;

    /**
     * rawPayload를 파싱하여 OcppParsedMessage를 반환한다.
     *
     * @param chargeBoxId 충전기 식별자
     * @param rawPayload  OCPP JSON 배열 원문
     * @return 파싱 성공 시 Optional(OcppParsedMessage), 실패/무관 시 Optional.empty()
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
            if (messageType != MSG_TYPE_CALL) {
                // CALLRESULT / CALLERROR 는 이번 Sprint 범위 제외
                return Optional.empty();
            }

            String uniqueId = root.get(1).asText();
            String action   = root.get(2).asText();
            JsonNode payload = root.size() > 3 ? root.get(3) : objectMapper.createObjectNode();

            return Optional.of(OcppParsedMessage.builder()
                    .chargeBoxId(chargeBoxId)
                    .uniqueId(uniqueId)
                    .action(action)
                    .payload(payload)
                    .build());

        } catch (Exception e) {
            log.warn("[OcppParser] rawPayload 파싱 실패: chargeBoxId={}, error={}", chargeBoxId, e.getMessage());
            return Optional.empty();
        }
    }
}
