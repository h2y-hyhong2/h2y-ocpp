package com.lselink.elvis.connect.ws.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket 게이트웨이 설정 프로퍼티 (elvis.gateway.websocket.*)
 */
@Component
@ConfigurationProperties(prefix = "elvis.gateway.websocket")
public class WebSocketProperties {

    private String path = "/ocpp/{chargeBoxId}";
    private List<String> subprotocols = new ArrayList<>(List.of("ocpp1.6", "ocpp1.5"));
    private int bufferSizeBytes = 65536;
    private long idleTimeoutSeconds = 360;
    private long pingIntervalSeconds = 30;
    private boolean requireAuth = false;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getSubprotocols() {
        return subprotocols;
    }

    public void setSubprotocols(List<String> subprotocols) {
        if (subprotocols != null && !subprotocols.isEmpty()) {
            this.subprotocols = new ArrayList<>(subprotocols);
        }
    }

    public int getBufferSizeBytes() {
        return bufferSizeBytes;
    }

    public void setBufferSizeBytes(int bufferSizeBytes) {
        this.bufferSizeBytes = bufferSizeBytes;
    }

    public long getIdleTimeoutSeconds() {
        return idleTimeoutSeconds;
    }

    public void setIdleTimeoutSeconds(long idleTimeoutSeconds) {
        this.idleTimeoutSeconds = idleTimeoutSeconds;
    }

    public long getPingIntervalSeconds() {
        return pingIntervalSeconds;
    }

    public void setPingIntervalSeconds(long pingIntervalSeconds) {
        this.pingIntervalSeconds = pingIntervalSeconds;
    }

    public boolean isRequireAuth() {
        return requireAuth;
    }

    public void setRequireAuth(boolean requireAuth) {
        this.requireAuth = requireAuth;
    }
}
