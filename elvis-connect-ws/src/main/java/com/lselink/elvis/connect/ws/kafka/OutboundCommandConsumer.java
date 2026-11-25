package com.lselink.elvis.connect.ws.kafka;

import com.lselink.elvis.connect.ws.dto.OcppOutboundCommand;
import com.lselink.elvis.connect.ws.session.LocalSessionStore;
import com.lselink.elvis.connect.ws.util.LogFormatUtils;
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
        if (command == null || command.getPayload() == null || command.getChargeBoxId() == null) {
            log.warn("[OutboundConsumer] 유효하지 않은 하향 명령 무시 (null command 또는 payload): {}", command);
            return;
        }

        String chargeBoxId = command.getChargeBoxId();
        String payload = command.getPayload();
        String msgId = (command.getMessageId() != null && !command.getMessageId().isBlank())
                ? command.getMessageId()
                : LogFormatUtils.extractMsgId(payload);

        try (LogFormatUtils.MdcScope ignored = LogFormatUtils.withMdc(chargeBoxId, msgId)) {
            log.debug("[REQ] OUTBOUND     | {} | charger={} msgId={}",
                    LogFormatUtils.padAction(command.getAction()), chargeBoxId, msgId);

            Optional<WebSocketSession> sessionOpt = sessionStore.getSession(chargeBoxId);
            if (sessionOpt.isPresent()) {
                WebSocketSession session = sessionOpt.get();
                synchronized (session) {
                    if (session.isOpen()) {
                        try {
                            session.sendMessage(new TextMessage(payload));
                            gatewayMetrics.recordOutboundCommand();
                            log.info("[SEND]    {} | {}", chargeBoxId, payload);
                        } catch (IOException e) {
                            log.error("[WebSocket][OCPP1.6] Send failed - charger: {}, error: {}",
                                    chargeBoxId, e.getMessage(), e);
                        }
                    } else {
                        log.warn("[WebSocket][OCPP1.6] Session closed - charger: {}", chargeBoxId);
                    }
                }
            } else {
                log.warn("[WebSocket][OCPP1.6] Active session not found (offline) - charger: {}", chargeBoxId);
            }
        }
    }
}
