package com.lselink.elvis.control.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 관제 UI 브로드캐스트용 WebSocket STOMP 메시지 브로커 설정 (Sprint 28: S28-API-0060-CO)
 * <p>
 * 엔드포인트: {@code /ws-push} (SockJS Fallback 지원)
 * 브로커 토픽 프리픽스: {@code /topic} (Heartbeat 10초)
 * 클라이언트 인바운드 프리픽스: {@code /app}
 * </p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompSessionChannelInterceptor sessionChannelInterceptor;

    public WebSocketConfig(StompSessionChannelInterceptor sessionChannelInterceptor) {
        this.sessionChannelInterceptor = sessionChannelInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Heartbeat 스케줄러 풀 구성 (송수신 10초 주기)
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("ws-heartbeat-");
        taskScheduler.initialize();

        // 클라이언트가 구독할 Simple Broker 토픽 목적지 설정
        registry.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(taskScheduler);

        // 클라이언트에서 서버로 메시지 발행 시의 엔드포인트 프리픽스
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 순수 WebSocket 엔드포인트 (Tauri 및 최신 브라우저용)
        registry.addEndpoint("/ws-push")
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "tauri://localhost",
                        "https://tauri.localhost"
                );

        // SockJS Fallback 지원 엔드포인트 (프록시/방화벽 통과용)
        registry.addEndpoint("/ws-push")
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "tauri://localhost",
                        "https://tauri.localhost"
                )
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(sessionChannelInterceptor);
    }
}
