package com.lselink.elvis.control.service.impl;

import com.lselink.elvis.control.dto.dynamic.SandboxPolicyRequest;
import com.lselink.elvis.control.dto.dynamic.SandboxStatusDto;
import com.lselink.elvis.control.dto.dynamic.WireTapPolicyRequest;
import com.lselink.elvis.control.dto.dynamic.WireTapStatusDto;
import com.lselink.elvis.control.service.DynamicControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 동적 제어(Sandbox 트래픽 격리 및 WireTap 실시간 패킷 감시) 서비스 구현체
 * (Sprint 32: S32-API-0090-CO)
 * <p>
 * ConcurrentHashMap 기반 락-프리 O(1) 인메모리 레지스트리를 통해
 * 초당 수천 건 패킷 라우팅 경로에서 논블로킹으로 동적 정책을 평가합니다.
 * 거버넌스 9조 Clean Code 및 10조 채번 표준을 엄격히 준수합니다.
 * </p>
 */
@Service
public class DynamicControlServiceImpl implements DynamicControlService {

    private static final Logger log = LoggerFactory.getLogger(DynamicControlServiceImpl.class);

    /** 특정 충전기 Sandbox 격리 레지스트리 (Key: chargeBoxId) */
    private final Map<String, SandboxStatusDto> sandboxRegistry = new ConcurrentHashMap<>();

    /** 특정 충전기 WireTap 감시 레지스트리 (Key: chargeBoxId) */
    private final Map<String, WireTapStatusDto> wireTapRegistry = new ConcurrentHashMap<>();

    // === Sandbox (트래픽 격리, REQ-021) ===

    @Override
    public SandboxStatusDto setSandboxPolicy(SandboxPolicyRequest request) {
        String chargeBoxId = sanitizeChargeBoxId(request.getChargeBoxId());

        boolean isEnabled = true;
        if (request.getEnabled() != null) {
            isEnabled = request.getEnabled();
        }

        if (!isEnabled) {
            sandboxRegistry.remove(chargeBoxId);
            log.info("[Sandbox] 격리 정책 비활성화 및 레지스트리 제거: chargeBoxId={}", chargeBoxId);
            return new SandboxStatusDto(chargeBoxId, false, "NORMAL", "격리 해제됨", Instant.now(), null);
        }

        String mode = "ISOLATED";
        String reqMode = request.getPolicyMode();
        if (reqMode != null && !reqMode.trim().isEmpty()) {
            String upperMode = reqMode.trim().toUpperCase();
            if ("DROP".equals(upperMode) || "MOCK".equals(upperMode) || "ISOLATED".equals(upperMode)) {
                mode = upperMode;
            }
        }

        String reason = "비정상 패킷 격리";
        String reqReason = request.getReason();
        if (reqReason != null && !reqReason.trim().isEmpty()) {
            reason = reqReason.trim();
        }

        long ttl = 3600L;
        Long reqTtl = request.getTtlSeconds();
        if (reqTtl != null && reqTtl >= 0) {
            ttl = reqTtl;
        }

        Instant now = Instant.now();
        Instant expiresAt = null;
        if (ttl > 0) {
            expiresAt = now.plusSeconds(ttl);
        }

        SandboxStatusDto statusDto = new SandboxStatusDto(chargeBoxId, true, mode, reason, now, expiresAt);
        sandboxRegistry.put(chargeBoxId, statusDto);

        log.info("[Sandbox] 격리 정책 등록 완료: chargeBoxId={}, mode={}, ttl={}s, reason={}",
                chargeBoxId, mode, ttl, reason);

        return statusDto;
    }

    @Override
    public SandboxStatusDto getSandboxStatus(String chargeBoxId) {
        String cleanId = sanitizeChargeBoxId(chargeBoxId);
        SandboxStatusDto status = sandboxRegistry.get(cleanId);

        if (status == null) {
            return new SandboxStatusDto(cleanId, false, "NORMAL", "정상 트래픽 (비격리)", Instant.now(), null);
        }

        if (status.isExpired()) {
            sandboxRegistry.remove(cleanId);
            log.info("[Sandbox] 격리 정책 TTL 만료로 자동 제거: chargeBoxId={}", cleanId);
            return new SandboxStatusDto(cleanId, false, "NORMAL", "TTL 만료 해제됨", Instant.now(), null);
        }

        return status;
    }

    @Override
    public List<SandboxStatusDto> getActiveSandboxList() {
        List<SandboxStatusDto> activeList = new ArrayList<>();
        Instant now = Instant.now();

        for (Map.Entry<String, SandboxStatusDto> entry : sandboxRegistry.entrySet()) {
            SandboxStatusDto dto = entry.getValue();
            if (dto.getExpiresAt() != null && dto.getExpiresAt().isBefore(now)) {
                sandboxRegistry.remove(entry.getKey());
            } else if (dto.isEnabled()) {
                activeList.add(dto);
            }
        }

        return activeList;
    }

    @Override
    public boolean removeSandboxPolicy(String chargeBoxId) {
        String cleanId = sanitizeChargeBoxId(chargeBoxId);
        SandboxStatusDto removed = sandboxRegistry.remove(cleanId);
        boolean result = false;
        if (removed != null) {
            result = true;
            log.info("[Sandbox] 격리 정책 수동 해제 성공: chargeBoxId={}", cleanId);
        }
        return result;
    }

    @Override
    public boolean isSandboxed(String chargeBoxId) {
        if (chargeBoxId == null || chargeBoxId.trim().isEmpty()) {
            return false;
        }

        String cleanId = chargeBoxId.trim();
        SandboxStatusDto status = sandboxRegistry.get(cleanId);
        if (status == null) {
            return false;
        }

        if (!status.isEnabled()) {
            return false;
        }

        if (status.isExpired()) {
            sandboxRegistry.remove(cleanId);
            return false;
        }

        return true;
    }

    // === WireTap (실시간 패킷 감시, REQ-022) ===

    @Override
    public WireTapStatusDto setWireTapPolicy(WireTapPolicyRequest request) {
        String chargeBoxId = sanitizeChargeBoxId(request.getChargeBoxId());

        boolean isEnabled = true;
        if (request.getEnabled() != null) {
            isEnabled = request.getEnabled();
        }

        if (!isEnabled) {
            wireTapRegistry.remove(chargeBoxId);
            log.info("[WireTap] 감시 모드 비활성화 및 레지스트리 제거: chargeBoxId={}", chargeBoxId);
            return new WireTapStatusDto(chargeBoxId, false, "OFF", Instant.now(), null);
        }

        String level = "ALL";
        String reqLevel = request.getTapLevel();
        if (reqLevel != null && !reqLevel.trim().isEmpty()) {
            String upperLevel = reqLevel.trim().toUpperCase();
            if ("ALL".equals(upperLevel) || "CALL_ONLY".equals(upperLevel) || "ERROR_ONLY".equals(upperLevel)) {
                level = upperLevel;
            }
        }

        long duration = 1800L; // 기본 30분
        Long reqDuration = request.getMaxDurationSeconds();
        if (reqDuration != null && reqDuration > 0) {
            if (reqDuration > 3600L) {
                duration = 3600L; // 최대 1시간 상한
            } else {
                duration = reqDuration;
            }
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(duration);

        WireTapStatusDto statusDto = new WireTapStatusDto(chargeBoxId, true, level, now, expiresAt);
        wireTapRegistry.put(chargeBoxId, statusDto);

        log.info("[WireTap] 실시간 감시 모드 활성화 완료: chargeBoxId={}, level={}, duration={}s",
                chargeBoxId, level, duration);

        return statusDto;
    }

    @Override
    public WireTapStatusDto getWireTapStatus(String chargeBoxId) {
        String cleanId = sanitizeChargeBoxId(chargeBoxId);
        WireTapStatusDto status = wireTapRegistry.get(cleanId);

        if (status == null) {
            return new WireTapStatusDto(cleanId, false, "OFF", Instant.now(), null);
        }

        if (status.isExpired()) {
            wireTapRegistry.remove(cleanId);
            log.info("[WireTap] 감시 타임아웃 만료로 자동 제거: chargeBoxId={}", cleanId);
            return new WireTapStatusDto(cleanId, false, "OFF", Instant.now(), null);
        }

        return status;
    }

    @Override
    public List<WireTapStatusDto> getActiveWireTapList() {
        List<WireTapStatusDto> activeList = new ArrayList<>();
        Instant now = Instant.now();

        for (Map.Entry<String, WireTapStatusDto> entry : wireTapRegistry.entrySet()) {
            WireTapStatusDto dto = entry.getValue();
            if (dto.getExpiresAt() != null && dto.getExpiresAt().isBefore(now)) {
                wireTapRegistry.remove(entry.getKey());
            } else if (dto.isEnabled()) {
                activeList.add(dto);
            }
        }

        return activeList;
    }

    @Override
    public boolean removeWireTapPolicy(String chargeBoxId) {
        String cleanId = sanitizeChargeBoxId(chargeBoxId);
        WireTapStatusDto removed = wireTapRegistry.remove(cleanId);
        boolean result = false;
        if (removed != null) {
            result = true;
            log.info("[WireTap] 실시간 감시 수동 중지 성공: chargeBoxId={}", cleanId);
        }
        return result;
    }

    @Override
    public boolean isWireTapped(String chargeBoxId) {
        if (chargeBoxId == null || chargeBoxId.trim().isEmpty()) {
            return false;
        }

        String cleanId = chargeBoxId.trim();
        WireTapStatusDto status = wireTapRegistry.get(cleanId);
        if (status == null) {
            return false;
        }

        if (!status.isEnabled()) {
            return false;
        }

        if (status.isExpired()) {
            wireTapRegistry.remove(cleanId);
            return false;
        }

        return true;
    }

    /**
     * 거버넌스 10조 채번 표준에 따른 chargeBoxId 정규화
     */
    private String sanitizeChargeBoxId(String chargeBoxId) {
        if (chargeBoxId == null || chargeBoxId.trim().isEmpty()) {
            return "00000001";
        }
        return chargeBoxId.trim();
    }
}
