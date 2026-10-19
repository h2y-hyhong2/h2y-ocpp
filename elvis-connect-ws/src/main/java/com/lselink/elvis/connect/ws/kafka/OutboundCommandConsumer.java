package com.lselink.elvis.connect.ws.kafka;

import com.lselink.elvis.connect.ws.dto.OcppOutboundCommand;
import com.lselink.elvis.connect.ws.session.LocalSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Optional;

/**
 * Kafka ocpp-outbound-commands 토픽을 구독하여 타겟 충전기 소켓 세션으로 하향 제어 명령 전송
 */
@Component
public class OutboundCommandConsumer {

    private static final Logger log = LoggerFactory.getLogger(OutboundCommandConsumer.class);

    private final LocalSessionStore sessionStore;
    private final com.lselink.elvis.connect.ws.metrics.GatewayMetrics gatewayMetrics;

    public OutboundCommandConsumer(LocalSessionStore sessionStore,
                                  com.lselink.elvis.connect.ws.metrics.GatewayMetrics gatewayMetrics) {
        this.sessionStore = sessionStore;
        this.gatewayMetrics = gatewayMetrics;
    }

    @KafkaListener(
            topics = "${elvis.gateway.kafka.outbound-topic:ocpp-outbound-commands}",
            containerFactory = "outboundKafkaListenerContainerFactory"
    )
    public void consumeOutboundCommand(OcppOutboundCommand command) {
        String chargeBoxId = command.getChargeBoxId();
        log.info("[OutboundConsumer] 하향 명령 수신: chargeBoxId={}, action={}, messageId={}",
                chargeBoxId, command.getAction(), command.getMessageId());

        Optional<WebSocketSession> sessionOpt = sessionStore.getSession(chargeBoxId);
        if (sessionOpt.isPresent()) {
            WebSocketSession session = sessionOpt.get();
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(command.getPayload()));
                    gatewayMetrics.recordOutboundCommand();
                    log.info("[WS-SEND] [{}] 하향 전문 전송: {}", chargeBoxId, command.getPayload());
                } catch (IOException e) {
                    log.error("[OutboundConsumer] 하향 명령 소켓 전송 실패: chargeBoxId={}, error={}",
                            chargeBoxId, e.getMessage(), e);
                }
            } else {
                log.warn("[OutboundConsumer] 충전기 소켓 닫힘 상태: chargeBoxId={}", chargeBoxId);
            }
        } else {
            log.warn("[OutboundConsumer] 활성 세션 미존재 (오프라인): chargeBoxId={}", chargeBoxId);
        }
    }
}
