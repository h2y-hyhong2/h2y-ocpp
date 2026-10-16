package com.lselink.elvis.connect.ws.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 인메모리 충전기 WebSocket 세션 스토리지 (Lock-free ConcurrentHashMap)
 * 세션 동시성 관리 및 60초 미수신 단말 소켓 자동 헬스체크 기능 제공 (REQ-008)
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

    private static final Duration SESSION_TIMEOUT = Duration.ofSeconds(60);

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
        lastActivityMap.put(chargeBoxId, Instant.now());
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
        return sessionMap.size();
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
            log.warn("[SessionStore] 60초 무응답 타임아웃 세션 정리 완료: {}건 해제됨", evicted);
        }

        int activeCount = getActiveSessionCount();
        if (activeCount > 0) {
            logActiveSessions("30초 주기 모니터링");
        }
    }

    /**
     * 60초 이상 미수신(유휴) 단말 세션 자동 감지 및 강제 해제 (Heartbeat 헬스체크)
     */
    public int cleanupIdleSessions() {
        Instant threshold = Instant.now().minus(SESSION_TIMEOUT);
        int evictedCount = 0;

        for (var entry : lastActivityMap.entrySet()) {
            String chargeBoxId = entry.getKey();
            Instant lastActive = entry.getValue();

            if (lastActive.isBefore(threshold)) {
                WebSocketSession session = sessionMap.get(chargeBoxId);
                if (session != null && session.isOpen()) {
                    log.warn("[SessionStore] 60초 헬스체크 미응답 타임아웃 단말 소켓 강제 해제: chargeBoxId={}, idleDuration={}s",
                            chargeBoxId, Duration.between(lastActive, Instant.now()).toSeconds());
                    try {
                        session.close(CloseStatus.SESSION_NOT_RELIABLE.withReason("Heartbeat timeout (60s)"));
                    } catch (IOException e) {
                        log.error("[SessionStore] 소켓 해제 실패: chargeBoxId={}", chargeBoxId, e);
                    }
                    sessionMap.remove(chargeBoxId, session);
                    lastActivityMap.remove(chargeBoxId);
                    evictedCount++;
                }
            }
        }
        return evictedCount;
    }
}
