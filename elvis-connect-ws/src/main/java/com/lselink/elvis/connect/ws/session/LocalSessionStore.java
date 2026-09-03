package com.lselink.elvis.connect.ws.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 인메모리 충전기 WebSocket 세션 스토리지 (Lock-free ConcurrentHashMap)
 * 세션 동시성 관리 및 60초 미수신 단말 소켓 자동 헬스체크 기능 제공 (REQ-008)
 */
@Slf4j
@Component
public class LocalSessionStore {

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
        log.info("[SessionStore] 세션 등록 완료: chargeBoxId={}, sessionId={}, 총 활성 세션={}",
                chargeBoxId, newSession.getId(), sessionMap.size());
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
            log.info("[SessionStore] 세션 제거 완료: chargeBoxId={}, sessionId={}, 총 활성 세션={}",
                    chargeBoxId, session.getId(), sessionMap.size());
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
