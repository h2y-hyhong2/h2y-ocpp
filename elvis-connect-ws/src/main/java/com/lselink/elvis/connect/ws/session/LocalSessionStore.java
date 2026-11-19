package com.lselink.elvis.connect.ws.session;

import com.lselink.elvis.connect.ws.kafka.InboundRawEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 인메모리 충전기 WebSocket 세션 스토리지 (Lock-free ConcurrentHashMap)
 * 세션 동시성 관리 및 유휴 단말 소켓 자동 헬스체크 기능 제공 (REQ-008)
 */
@Component
public class LocalSessionStore {

    private static final Logger log = LoggerFactory.getLogger(LocalSessionStore.class);

    /**
     * Key: chargeBoxId, Value: WebSocketSession
     */
    private final ConcurrentMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    /**
     * Key: chargeBoxId, Value: 마지막 활동/패킷 수신 시각
     */
    private final ConcurrentMap<String, Instant> lastActivityMap = new ConcurrentHashMap<>();

    private final long idleTimeoutSeconds;
    private final Duration sessionTimeout;
    private final Duration pingInterval;
    private final InboundRawEventProducer inboundProducer;

    public LocalSessionStore(long idleTimeoutSeconds) {
        this(idleTimeoutSeconds, 30L, null);
    }

    @Autowired
    public LocalSessionStore(@Value("${elvis.gateway.websocket.idle-timeout-seconds:360}") long idleTimeoutSeconds,
                             @Value("${elvis.gateway.websocket.ping-interval-seconds:30}") long pingIntervalSeconds,
                             InboundRawEventProducer inboundProducer) {
        this.idleTimeoutSeconds = idleTimeoutSeconds > 0 ? idleTimeoutSeconds : 360L;
        this.sessionTimeout = Duration.ofSeconds(this.idleTimeoutSeconds);
        long safePingSec = pingIntervalSeconds > 0 ? pingIntervalSeconds : 30L;
        this.pingInterval = Duration.ofSeconds(safePingSec);
        this.inboundProducer = inboundProducer;
    }

    /**
     * 세션 등록 및 중복 연결 축출 (Session Eviction)
     */
    public void registerSession(String chargeBoxId, WebSocketSession newSession) {
        WebSocketSession existingSession = sessionMap.put(chargeBoxId, newSession);
        lastActivityMap.put(chargeBoxId, Instant.now());

        if (existingSession != null && existingSession.isOpen() && !existingSession.getId().equals(newSession.getId())) {
            log.warn("[SessionStore] 중복 연결 감지: chargeBoxId={}, 기존 세션 id={} 강제 종료", chargeBoxId, existingSession.getId());
            try {
                existingSession.close(CloseStatus.POLICY_VIOLATION.withReason("Duplicate chargeBoxId connected"));
            } catch (IOException e) {
                log.error("[SessionStore] 기존 세션 종료 실패: {}", e.getMessage());
            }
        }

        // 신규 연결 등록 즉시 현재 활성 연결 세션 전체 현황 로깅
        logActiveSessions("신규 세션 등록 [" + chargeBoxId + "]");
    }

    /**
     * 패킷/Heartbeat 수신 시 세션 활동 시각 갱신
     */
    public void touchSession(String chargeBoxId) {
        if (chargeBoxId != null && sessionMap.containsKey(chargeBoxId)) {
            lastActivityMap.put(chargeBoxId, Instant.now());
        }
    }

    /**
     * 세션 조회
     */
    public Optional<WebSocketSession> getSession(String chargeBoxId) {
        return Optional.ofNullable(sessionMap.get(chargeBoxId));
    }

    /**
     * 세션 제거 (동일 세션 ID 확인 후 안전하게 제거)
     */
    public boolean removeSession(String chargeBoxId, WebSocketSession session) {
        boolean removed = sessionMap.remove(chargeBoxId, session);
        if (removed) {
            lastActivityMap.remove(chargeBoxId);
            // 세션 종료 즉시 잔여 활성 세션 현황 로깅
            logActiveSessions("세션 종료 해제 [" + chargeBoxId + "]");
        }
        return removed;
    }

    /**
     * 현재 활성 세션 수 반환
     */
    public int getActiveSessionCount() {
        return getActiveChargeBoxIds().size();
    }

    /**
     * 현재 실제 연결(isOpen)되어 있는 충전기 chargeBoxId 목록 반환
     */
    public Set<String> getActiveChargeBoxIds() {
        return sessionMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().isOpen())
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * 현재 메모리에 유지 중인 활성 WebSocket 세션 상세 정보를 로그에 출력
     *
     * @param triggerReason 로그 발생 트리거/원인 (신규 등록, 연결 해제, 주기적 모니터링 등)
     */
    public void logActiveSessions(String triggerReason) {
        int totalSessions = sessionMap.size();
        if (totalSessions == 0) {
            log.info("[SessionStore] ℹ️ [{}] 현재 활성 연결 세션: 0건 (연결된 충전기 없음)", triggerReason);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[SessionStore] 🔌 [%s] 현재 활성 연결 세션 현황 (총 %d건):%n", triggerReason, totalSessions));

        Instant now = Instant.now();
        for (var entry : sessionMap.entrySet()) {
            String chargeBoxId = entry.getKey();
            WebSocketSession session = entry.getValue();

            String sessionId = session.getId();
            InetSocketAddress remoteAddr = session.getRemoteAddress();
            String remoteStr = remoteAddr != null ? remoteAddr.toString() : "unknown";

            Instant lastActive = lastActivityMap.getOrDefault(chargeBoxId, now);
            long idleSec = Duration.between(lastActive, now).toSeconds();
            boolean isOpen = session.isOpen();

            sb.append(String.format("  • [%s] sessionId=%s, remote=%s, idle=%ds, open=%b%n",
                    chargeBoxId, sessionId, remoteStr, idleSec, isOpen));
        }

        String summaryLog = sb.toString().trim();
        log.info("{}", summaryLog);
    }

    /**
     * 30초 주기 세션 헬스체크 및 활성 세션 브리핑 로그 스케줄러
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 15000)
    public void monitorAndLogActiveSessions() {
        int evicted = cleanupIdleSessions();
        if (evicted > 0) {
            log.warn("[SessionStore] {}초 무응답/종료 세션 정리 완료: {}건 해제됨", idleTimeoutSeconds, evicted);
        }

        int activeCount = getActiveSessionCount();
        if (activeCount > 0) {
            logActiveSessions("30초 주기 모니터링");
        }
    }

    /**
     * 유휴 타임아웃 초과 및 이미 닫힌 단말 세션 자동 감지·정리 + WebSocket Ping 헬스체크
     * <p>
     * 60분 Heartbeat 주기를 사용하는 현장 단말도 RFC 6455 Ping/Pong으로 생존을 유지하되,
     * 네트워크 단절로 Ping 전송 실패 또는 Pong/패킷 무응답이 지속되면 즉시 정리합니다.
     * </p>
     */
    public int cleanupIdleSessions() {
        Instant now = Instant.now();
        Instant idleThreshold = now.minus(sessionTimeout);
        Instant pingThreshold = now.minus(pingInterval);
        int evictedCount = 0;

        for (var entry : sessionMap.entrySet()) {
            String chargeBoxId = entry.getKey();
            WebSocketSession session = entry.getValue();

            if (!session.isOpen()) {
                if (sessionMap.remove(chargeBoxId, session)) {
                    lastActivityMap.remove(chargeBoxId);
                    publishDisconnect(chargeBoxId, session);
                    evictedCount++;
                }
                continue;
            }

            Instant lastActive = lastActivityMap.get(chargeBoxId);
            if (lastActive != null && lastActive.isBefore(idleThreshold)) {
                log.warn("[SessionStore] {}초 무응답(패킷/Pong 미수신) 타임아웃 단말 소켓 강제 해제: chargeBoxId={}, idleDuration={}s",
                        idleTimeoutSeconds, chargeBoxId, Duration.between(lastActive, now).toSeconds());
                boolean removed = sessionMap.remove(chargeBoxId, session);
                if (removed) {
                    lastActivityMap.remove(chargeBoxId);
                    publishDisconnect(chargeBoxId, session);
                    evictedCount++;
                }
                try {
                    session.close(CloseStatus.SESSION_NOT_RELIABLE.withReason(
                            "Idle timeout (" + idleTimeoutSeconds + "s)"));
                } catch (IOException e) {
                    log.error("[SessionStore] 소켓 해제 실패: chargeBoxId={}", chargeBoxId, e);
                }
                continue;
            }

            // 무통신이 pingInterval 이상 경과한 세션에 WebSocket 제어 프레임 Ping 전송
            if (lastActive != null && lastActive.isBefore(pingThreshold)) {
                if (!sendPingFrame(chargeBoxId, session)) {
                    boolean removed = sessionMap.remove(chargeBoxId, session);
                    if (removed) {
                        lastActivityMap.remove(chargeBoxId);
                        publishDisconnect(chargeBoxId, session);
                        evictedCount++;
                    }
                    try {
                        if (session.isOpen()) {
                            session.close(CloseStatus.GOING_AWAY.withReason("WebSocket Ping failed"));
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        return evictedCount;
    }

    private boolean sendPingFrame(String chargeBoxId, WebSocketSession session) {
        synchronized (session) {
            if (!session.isOpen()) {
                return false;
            }
            try {
                ByteBuffer payload = ByteBuffer.wrap("elvis-ping".getBytes(StandardCharsets.UTF_8));
                session.sendMessage(new PingMessage(payload));
                log.debug("[SessionStore] WebSocket Ping 전송: chargeBoxId={}", chargeBoxId);
                return true;
            } catch (IOException | IllegalStateException e) {
                log.warn("[SessionStore] WebSocket Ping 전송 실패 → 연결 단절 판정: chargeBoxId={}, error={}",
                        chargeBoxId, e.getMessage());
                return false;
            }
        }
    }

    private void publishDisconnect(String chargeBoxId, WebSocketSession session) {
        if (inboundProducer == null || chargeBoxId == null || chargeBoxId.isBlank()) {
            return;
        }
        String sessionId = session != null ? session.getId() : null;
        String remoteAddr = (session != null && session.getRemoteAddress() != null)
                ? session.getRemoteAddress().toString()
                : "unknown";
        inboundProducer.sendDisconnectEvent(chargeBoxId, sessionId, remoteAddr);
    }
}
