package com.lselink.elvis.control.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * STOMP 세션 수명 주기 및 Heartbeat 감시 인터셉터 (Sprint 28: S28-API-0060-CO)
 * <p>
 * 클라이언트의 CONNECT, SUBSCRIBE, DISCONNECT 프레임을 가로채 접속자 수를 실시간 집계하고 세션 상태를 추적합니다.
 * </p>
 */
@Component
public class StompSessionChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompSessionChannelInterceptor.class);

    private final AtomicInteger activeSessionCount = new AtomicInteger(0);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == null) {
            return message;
        }

        String sessionId = accessor.getSessionId();

        if (StompCommand.CONNECT.equals(command)) {
            int currentCount = activeSessionCount.incrementAndGet();
            log.info("[STOMP] 클라이언트 연결 수립: sessionId={}, 현재 활성 세션수={}", sessionId, currentCount);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination();
            log.info("[STOMP] 토픽 구독 등록: sessionId={}, destination={}", sessionId, destination);
        } else if (StompCommand.DISCONNECT.equals(command)) {
            int remainingCount = activeSessionCount.decrementAndGet();
            if (remainingCount < 0) {
                activeSessionCount.set(0);
                remainingCount = 0;
            }
            log.info("[STOMP] 클라이언트 연결 해제: sessionId={}, 잔여 활성 세션수={}", sessionId, remainingCount);
        }

        return message;
    }

    public int getActiveSessionCount() {
        return activeSessionCount.get();
    }
}
