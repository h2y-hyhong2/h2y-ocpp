package com.lselink.elvis.control.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * elvis-connect-ws 게이트웨이의 실시간 활성 WebSocket 세션 목록을 조회하는 내부 클라이언트
 * <p>
 * 3초 주기 UI 폴링 시 /chargers 및 /chargers/summary 동시 요청의 중복 HTTP 호출을 줄이기 위해
 * 짧은 TTL 캐시(기본 1초)를 유지하며, 게이트웨이 미기동/연결 불가 시 활성 세션 0건(빈 Set)으로 판단합니다.
 * </p>
 */
@Component
public class WsSessionClient {

    private static final Logger log = LoggerFactory.getLogger(WsSessionClient.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String sessionsEndpoint;
    private final Duration readTimeout;
    private final long cacheTtlMs;

    private final Object cacheLock = new Object();
    private volatile Set<String> cachedActiveIds = Collections.emptySet();
    private volatile long cacheExpiresAtMs = 0L;
    private final AtomicBoolean gatewayUnreachableLogged = new AtomicBoolean(false);

    public WsSessionClient(
            ObjectMapper objectMapper,
            @Value("${elvis.control.ws-gateway.enabled:true}") boolean enabled,
            @Value("${elvis.control.ws-gateway.base-url:http://localhost:9080}") String baseUrl,
            @Value("${elvis.control.ws-gateway.connect-timeout-ms:500}") long connectTimeoutMs,
            @Value("${elvis.control.ws-gateway.read-timeout-ms:800}") long readTimeoutMs,
            @Value("${elvis.control.ws-gateway.cache-ttl-ms:1000}") long cacheTtlMs) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        String normalizedBase = (baseUrl != null ? baseUrl.trim() : "http://localhost:9080").replaceAll("/+$", "");
        this.sessionsEndpoint = normalizedBase + "/internal/sessions";
        long safeConnectMs = connectTimeoutMs > 0 ? connectTimeoutMs : 500L;
        long safeReadMs = readTimeoutMs > 0 ? readTimeoutMs : 800L;
        this.readTimeout = Duration.ofMillis(safeReadMs);
        this.cacheTtlMs = Math.max(0L, cacheTtlMs);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(safeConnectMs))
                .build();
    }

    /**
     * 현재 WebSocket 게이트웨이에 연결되어 있는 활성 chargeBoxId 목록 반환
     *
     * @return 활성 chargeBoxId Set (비활성화 설정 시 null, 게이트웨이 미응답 시 빈 Set)
     */
    public Set<String> getActiveChargeBoxIds() {
        if (!enabled) {
            return null;
        }

        long now = System.currentTimeMillis();
        if (now < cacheExpiresAtMs) {
            return cachedActiveIds;
        }

        synchronized (cacheLock) {
            now = System.currentTimeMillis();
            if (now < cacheExpiresAtMs) {
                return cachedActiveIds;
            }

            Set<String> fetched = fetchActiveChargeBoxIds();
            cachedActiveIds = fetched;
            cacheExpiresAtMs = System.currentTimeMillis() + cacheTtlMs;
            return fetched;
        }
    }

    private Set<String> fetchActiveChargeBoxIds() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(sessionsEndpoint))
                    .timeout(readTimeout)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode idsNode = root.path("chargeBoxIds");
                Set<String> ids = new HashSet<>();
                if (idsNode.isArray()) {
                    for (JsonNode node : idsNode) {
                        String id = node.asText(null);
                        if (id != null && !id.isBlank()) {
                            ids.add(id);
                        }
                    }
                }
                if (gatewayUnreachableLogged.compareAndSet(true, false)) {
                    log.info("[WsSessionClient] WS 게이트웨이({}) 세션 조회 정상 복구: activeCount={}",
                            sessionsEndpoint, ids.size());
                }
                return Collections.unmodifiableSet(ids);
            }

            logUnreachable("HTTP " + response.statusCode());
            return Collections.emptySet();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logUnreachable("Interrupted");
            return Collections.emptySet();
        } catch (Exception ex) {
            logUnreachable(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return Collections.emptySet();
        }
    }

    private void logUnreachable(String reason) {
        if (gatewayUnreachableLogged.compareAndSet(false, true)) {
            log.warn("[WsSessionClient] WS 게이트웨이({}) 응답 불가({}) -> 활성 연결 0건으로 처리합니다.",
                    sessionsEndpoint, reason);
        } else {
            log.debug("[WsSessionClient] WS 게이트웨이({}) 응답 불가({}), 활성 연결 0건 유지",
                    sessionsEndpoint, reason);
        }
    }
}
