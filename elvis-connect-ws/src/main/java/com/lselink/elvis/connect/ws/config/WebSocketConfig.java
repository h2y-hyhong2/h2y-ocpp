package com.lselink.elvis.connect.ws.config;

import com.lselink.elvis.connect.ws.handler.OcppHandshakeInterceptor;
import com.lselink.elvis.connect.ws.handler.OcppWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket 핸들러 매핑 및 버퍼/타임아웃 컨테이너 설정
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final OcppWebSocketHandler ocppWebSocketHandler;
    private final OcppHandshakeInterceptor ocppHandshakeInterceptor;

    @Value("${elvis.gateway.websocket.path:/ocpp/{chargeBoxId}}")
    private String websocketPath;

    @Value("${elvis.gateway.websocket.buffer-size-bytes:16384}")
    private int bufferSizeBytes;

    @Value("${elvis.gateway.websocket.idle-timeout-seconds:90}")
    private long idleTimeoutSeconds;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(ocppWebSocketHandler, websocketPath, "/ocpp/*")
                .addInterceptors(ocppHandshakeInterceptor)
                .setAllowedOrigins("*");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(bufferSizeBytes);
        container.setMaxBinaryMessageBufferSize(bufferSizeBytes);
        container.setMaxSessionIdleTimeout(idleTimeoutSeconds * 1000L);
        container.setAsyncSendTimeout(5000L);
        return container;
    }
}
