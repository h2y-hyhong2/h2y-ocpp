package com.lselink.elvis.control.service;

import com.lselink.elvis.control.dto.dynamic.SandboxPolicyRequest;
import com.lselink.elvis.control.dto.dynamic.SandboxStatusDto;
import com.lselink.elvis.control.dto.dynamic.WireTapPolicyRequest;
import com.lselink.elvis.control.dto.dynamic.WireTapStatusDto;

import java.util.List;

/**
 * 동적 제어(Sandbox 트래픽 격리 및 WireTap 실시간 패킷 감시) 서비스 인터페이스
 * (Sprint 32: S32-API-0090-CO)
 */
public interface DynamicControlService {

    // === Sandbox (트래픽 격리, REQ-021) ===

    /**
     * 특정 충전기 Sandbox 격리 정책 설정/갱신
     */
    SandboxStatusDto setSandboxPolicy(SandboxPolicyRequest request);

    /**
     * 특정 충전기 Sandbox 격리 상태 조회
     */
    SandboxStatusDto getSandboxStatus(String chargeBoxId);

    /**
     * 현재 활성화된 Sandbox 격리 목록 조회
     */
    List<SandboxStatusDto> getActiveSandboxList();

    /**
     * 특정 충전기 Sandbox 격리 해제
     */
    boolean removeSandboxPolicy(String chargeBoxId);

    /**
     * 패킷 인터셉터용: 특정 충전기가 현재 Sandbox 격리 대상인지 고속 판별 (O(1))
     */
    boolean isSandboxed(String chargeBoxId);

    // === WireTap (실시간 패킷 감시, REQ-022) ===

    /**
     * 특정 충전기 WireTap 감시 정책 설정/갱신
     */
    WireTapStatusDto setWireTapPolicy(WireTapPolicyRequest request);

    /**
     * 특정 충전기 WireTap 감시 상태 조회
     */
    WireTapStatusDto getWireTapStatus(String chargeBoxId);

    /**
     * 현재 활성화된 WireTap 감시 목록 조회
     */
    List<WireTapStatusDto> getActiveWireTapList();

    /**
     * 특정 충전기 WireTap 감시 즉시 중단
     */
    boolean removeWireTapPolicy(String chargeBoxId);

    /**
     * 패킷 인터셉터용: 특정 충전기가 현재 WireTap 감시 대상인지 고속 판별 (O(1))
     */
    boolean isWireTapped(String chargeBoxId);
}
