package com.lselink.elvis.connect.ws.controller;

import com.lselink.elvis.connect.ws.session.LocalSessionStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 관제 API(elvis-control-api)에서 현재 WebSocket 활성 연결 충전기 목록을 조회하기 위한 내부 엔드포인트
 */
@RestController
@RequestMapping("/internal/sessions")
public class InternalSessionController {

    private final LocalSessionStore sessionStore;

    public InternalSessionController(LocalSessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @GetMapping
    public Map<String, Object> getActiveSessions() {
        Set<String> activeIds = sessionStore.getActiveChargeBoxIds();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("activeCount", activeIds.size());
        response.put("chargeBoxIds", activeIds);
        return response;
    }
}
