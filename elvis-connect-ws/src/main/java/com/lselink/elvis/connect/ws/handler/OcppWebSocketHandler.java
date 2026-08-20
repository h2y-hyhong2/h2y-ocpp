package com.lselink.elvis.connect.ws.handler;

import com.lselink.elvis.connect.ws.dto.OcppRawEventEnvelope;
import com.lselink.elvis.connect.ws.kafka.InboundRawEventProducer;
import com.lselink.elvis.connect.ws.session.LocalSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;

/**
 * 충전기 WebSocket 통신을 처리하는 인그레스 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcppWebSocketHandler extends TextWebSocketHandler {

    private final LocalSessionStore sessionStore;
    private final InboundRawEventProducer inboundProducer;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String chargeBoxId = getChargeBoxId(session);
        log.info("[WS-Handler] 소켓 연결 수립: chargeBoxId={}, sessionId={}, remoteAddress={}",
                chargeBoxId, session.getId(), session.getRemoteAddress());

        sessionStore.registerSession(chargeBoxId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String chargeBoxId = getChargeBoxId(session);
        String payload = message.getPayload();

        log.debug("[WS-Handler] 패킷 수신: chargeBoxId={}, length={}", chargeBoxId, payload.length());

        OcppRawEventEnvelope envelope = OcppRawEventEnvelope.builder()
                .chargeBoxId(chargeBoxId)
                .sessionId(session.getId())
                .remoteAddress(session.getRemoteAddress() != null ? session.getRemoteAddress().toString() : "unknown")
                .rawPayload(payload)
                .receivedAt(Instant.now())
                .build();

        // 비동기로 Kafka 토픽에 발행하여 I/O 루프 블로킹 방지
        inboundProducer.sendRawEvent(envelope);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String chargeBoxId = getChargeBoxId(session);
        log.error("[WS-Handler] 소켓 전송 에러 발생: chargeBoxId={}, sessionId={}, error={}",
                chargeBoxId, session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String chargeBoxId = getChargeBoxId(session);
        log.info("[WS-Handler] 소켓 연결 종료: chargeBoxId={}, sessionId={}, status={}",
                chargeBoxId, session.getId(), status);

        sessionStore.removeSession(chargeBoxId, session);
    }

    private String getChargeBoxId(WebSocketSession session) {
        Object attr = session.getAttributes().get(OcppHandshakeInterceptor.ATTR_CHARGE_BOX_ID);
        return attr != null ? attr.toString() : "UNKNOWN";
    }
}
