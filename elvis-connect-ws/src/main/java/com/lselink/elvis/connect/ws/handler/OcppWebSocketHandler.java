package com.lselink.elvis.connect.ws.handler;

import com.lselink.elvis.connect.ws.config.WebSocketProperties;
import com.lselink.elvis.connect.ws.dto.OcppRawEventEnvelope;
import com.lselink.elvis.connect.ws.kafka.InboundRawEventProducer;
import com.lselink.elvis.connect.ws.metrics.GatewayMetrics;
import com.lselink.elvis.connect.ws.session.LocalSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.List;

/**
 * 충전기 WebSocket 통신을 처리하는 인그레스 핸들러
 */
@Component
public class OcppWebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable {

    private static final Logger log = LoggerFactory.getLogger(OcppWebSocketHandler.class);

    private final LocalSessionStore sessionStore;
    private final InboundRawEventProducer inboundProducer;
    private final GatewayMetrics gatewayMetrics;
    private final WebSocketProperties webSocketProperties;

    public OcppWebSocketHandler(LocalSessionStore sessionStore,
                                InboundRawEventProducer inboundProducer,
                                GatewayMetrics gatewayMetrics,
                                WebSocketProperties webSocketProperties) {
        this.sessionStore = sessionStore;
        this.inboundProducer = inboundProducer;
        this.gatewayMetrics = gatewayMetrics;
        this.webSocketProperties = webSocketProperties;
    }

    @Override
    public List<String> getSubProtocols() {
        List<String> configured = webSocketProperties.getSubprotocols();
        return (configured != null && !configured.isEmpty())
                ? List.copyOf(configured)
                : List.of("ocpp1.6", "ocpp1.5");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String chargeBoxId = getChargeBoxId(session);

        // Kafka Broker 장애 Failover 시 신규 연결 차단 및 단말 로컬 버퍼링 유도 (REQ-003, REQ-009)
        if (!inboundProducer.isKafkaHealthy()) {
            log.warn("[WS-Handler] ⚠️ Kafka Broker 장애 상태로 인한 연결 거절: chargeBoxId={}", chargeBoxId);
            session.close(CloseStatus.SERVICE_RESTARTED.withReason("Kafka Broker Failover - Buffer Locally"));
            return;
        }

        log.info("[WS-Handler] 소켓 연결 수립: chargeBoxId={}, sessionId={}, remoteAddress={}",
                chargeBoxId, session.getId(), session.getRemoteAddress());

        sessionStore.registerSession(chargeBoxId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        long startNanos = System.nanoTime();
        String chargeBoxId = getChargeBoxId(session);
        String payload = message.getPayload();

        // 수신 패킷에 따른 세션 마지막 활동 시각 갱신
        sessionStore.touchSession(chargeBoxId);

        // 실시간 인바운드 TPS 모니터링 메트릭 카운팅 (REQ-004)
        gatewayMetrics.recordInboundPacket();

        log.info("[WS-RECV] [{}] 수신 전문: {}", chargeBoxId, payload);

        OcppRawEventEnvelope envelope = OcppRawEventEnvelope.builder()
                .chargeBoxId(chargeBoxId)
                .sessionId(session.getId())
                .remoteAddress(session.getRemoteAddress() != null ? session.getRemoteAddress().toString() : "unknown")
                .rawPayload(payload)
                .receivedAt(Instant.now())
                .build();

        // 비동기로 Kafka 토픽에 발행하여 I/O 루프 블로킹 방지 및 레이턴시 기록
        inboundProducer.sendRawEvent(envelope)
                .whenComplete((result, ex) ->
                        gatewayMetrics.recordPacketProcessingLatency(System.nanoTime() - startNanos));
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) {
        String chargeBoxId = getChargeBoxId(session);
        sessionStore.touchSession(chargeBoxId);
        log.debug("[WS-PONG] [{}] WebSocket Pong 수신 - 세션 활동 시각 갱신", chargeBoxId);
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

        if (sessionStore.removeSession(chargeBoxId, session)) {
            String remoteAddress = session.getRemoteAddress() != null
                    ? session.getRemoteAddress().toString()
                    : "unknown";
            inboundProducer.sendDisconnectEvent(chargeBoxId, session.getId(), remoteAddress);
        }
    }

    private String getChargeBoxId(WebSocketSession session) {
        Object attr = session.getAttributes().get(OcppHandshakeInterceptor.ATTR_CHARGE_BOX_ID);
        return attr != null ? attr.toString() : "UNKNOWN";
    }
}
