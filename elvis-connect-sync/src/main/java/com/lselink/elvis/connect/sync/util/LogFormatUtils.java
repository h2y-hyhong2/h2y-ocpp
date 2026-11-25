package com.lselink.elvis.connect.sync.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCPP 로그 컬럼 정렬 및 MDC 추출 유틸리티 (evcs_ocpp_rest 표준 준수).
 */
public final class LogFormatUtils {

    /** MDC charger(8자리 충전기 결합 식별자) 키 상수. */
    public static final String MDC_CHARGER = "charger";

    /** MDC msgId 키 상수. */
    public static final String MDC_MSG_ID = "msgId";

    /** OCPP 1.6 최장 Action명(DiagnosticsStatusNotification, 29자) 기준 고정 폭. */
    public static final int ACTION_WIDTH = 29;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Pattern REST_CHARGER_PATTERN =
            Pattern.compile("(?i)/rechgstId/([^/]+)/rechgrId/([^/]+)");

    private LogFormatUtils() {
    }

    /**
     * Action명을 좌측 정렬 고정 폭 문자열로 반환한다.
     *
     * @param action Action 객체 또는 문자열
     * @return 고정 폭으로 패딩된 Action 문자열
     */
    public static String padAction(Object action) {
        String safeAction = action != null ? String.valueOf(action) : "";
        int padLen = ACTION_WIDTH - safeAction.length();
        if (padLen <= 0) {
            return safeAction;
        }
        StringBuilder sb = new StringBuilder(ACTION_WIDTH);
        sb.append(safeAction);
        for (int i = 0; i < padLen; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * OCPP WebSocket JSON 배열 프레임([type, msgId, ...])에서 msgId를 안전하게 추출한다.
     *
     * @param payload 수신된 원문 JSON 문자열
     * @return 추출된 msgId (파싱 불가 시 null)
     */
    public static String extractMsgId(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(payload);
            if (root != null && root.isArray() && root.size() > 1 && root.get(1).isValueNode()) {
                String id = root.get(1).asText(null);
                return (id != null && !id.isBlank()) ? id : null;
            }
        } catch (Exception ignore) {
            // 비정상 JSON은 후속 파서에서 처리
        }
        return null;
    }

    /**
     * 요청 URI에서 충전기 결합 식별자({rechgstId}{rechgrId})를 추출한다.
     *
     * @param uri HTTP 요청 URI
     * @return 추출된 충전기 식별자 (미일치 시 null)
     */
    public static String extractChargerFromUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return null;
        }
        Matcher matcher = REST_CHARGER_PATTERN.matcher(uri);
        if (matcher.find()) {
            return matcher.group(1) + matcher.group(2);
        }
        return null;
    }

    /**
     * try-with-resources로 charger/msgId MDC를 바인딩하고 종료 시 이전 값으로 안전 복원한다.
     */
    public static MdcScope withMdc(String charger, String msgId) {
        return new MdcScope(charger, msgId);
    }

    public static final class MdcScope implements AutoCloseable {
        private final String previousCharger;
        private final String previousMsgId;

        private MdcScope(String charger, String msgId) {
            this.previousCharger = MDC.get(MDC_CHARGER);
            this.previousMsgId = MDC.get(MDC_MSG_ID);
            if (charger != null && !charger.isBlank()) {
                MDC.put(MDC_CHARGER, charger);
            }
            if (msgId != null && !msgId.isBlank()) {
                MDC.put(MDC_MSG_ID, msgId);
            }
        }

        @Override
        public void close() {
            if (previousCharger != null) {
                MDC.put(MDC_CHARGER, previousCharger);
            } else {
                MDC.remove(MDC_CHARGER);
            }
            if (previousMsgId != null) {
                MDC.put(MDC_MSG_ID, previousMsgId);
            } else {
                MDC.remove(MDC_MSG_ID);
            }
        }
    }
}
