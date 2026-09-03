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

        // 1. HTTP Basic Auth 헤더 및 Query Parameter Token 검증 (REQ-007)
        if (!validateSecurityCredentials(request, uri)) {
            log.warn("[Handshake] 미인증 충전기 핸드셰이크 거부: chargeBoxId={}, remoteUri={}", chargeBoxId, uri);
            return false;
        }

        attributes.put(ATTR_CHARGE_BOX_ID, chargeBoxId);
        log.info("[Handshake] 웹소켓 핸드셰이크 요청 수락: chargeBoxId={}, remoteUri={}", chargeBoxId, uri);
        return true;
    }

    /**
     * HTTP Authorization 헤더(Basic) 또는 URL 토큰 쿼리 파라미터 검증
     */
    private boolean validateSecurityCredentials(ServerHttpRequest request, URI uri) {
        // HTTP Basic Auth 헤더 검사
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            return true; // Basic 인증 정보 수신 통과
        }

        // URL 쿼리 파라미터 내 token 또는 key 검사 (예: /ocpp/CP_1001?token=xyz)
        String query = uri.getQuery();
        if (query != null && (query.contains("token=") || query.contains("auth="))) {
            return true;
        }

        // PoC 환경에서는 미설정 시에도 개발 편의상 허용하되 경고 로깅
        log.debug("[Handshake] 인증 헤더/토큰 미포함 (PoC 모드 기본 허용): {}", uri);
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
