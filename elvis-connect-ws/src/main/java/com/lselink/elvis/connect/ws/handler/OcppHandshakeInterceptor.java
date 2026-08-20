package com.lselink.elvis.connect.ws.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket Handshake 시점에 URI 경로에서 chargeBoxId를 추출하여 세션 속성(Attributes)에 바인딩
 */
@Slf4j
@Component
public class OcppHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_CHARGE_BOX_ID = "chargeBoxId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        URI uri = request.getURI();
        String path = uri.getPath(); // 예: /ocpp/CP_1001

        String chargeBoxId = extractChargeBoxId(path);
        if (chargeBoxId == null || chargeBoxId.isBlank()) {
            log.warn("[Handshake] 유효하지 않은 URI 경로 (chargeBoxId 누락): {}", path);
            return false;
        }

        attributes.put(ATTR_CHARGE_BOX_ID, chargeBoxId);
        log.info("[Handshake] 웹소켓 핸드셰이크 요청 수락: chargeBoxId={}, remoteUri={}", chargeBoxId, uri);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        if (exception != null) {
            log.error("[Handshake] 핸드셰이크 처리 중 예외 발생: {}", exception.getMessage());
        }
    }

    private String extractChargeBoxId(String path) {
        if (path == null) {
            return null;
        }
        String[] segments = path.split("/");
        if (segments.length >= 3 && "ocpp".equalsIgnoreCase(segments[1])) {
            return segments[2];
        }
        return null;
    }
}
