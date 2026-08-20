package com.lselink.elvis.connect.ws.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 인메모리 충전기 WebSocket 세션 스토리지 (Lock-free ConcurrentHashMap)
 */
@Slf4j
@Component
public class LocalSessionStore {

    /**
     * Key: chargeBoxId, Value: WebSocketSession
     */
    private final ConcurrentMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    /**
     * 세션 등록 및 중복 연결 축출 (Session Eviction)
     */
    public void registerSession(String chargeBoxId, WebSocketSession newSession) {
        WebSocketSession existingSession = sessionMap.put(chargeBoxId, newSession);
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
}
