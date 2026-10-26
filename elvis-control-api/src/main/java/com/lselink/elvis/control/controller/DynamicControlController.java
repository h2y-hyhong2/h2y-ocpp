package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.common.ApiResponse;
import com.lselink.elvis.control.dto.dynamic.SandboxPolicyRequest;
import com.lselink.elvis.control.dto.dynamic.SandboxStatusDto;
import com.lselink.elvis.control.dto.dynamic.WireTapPolicyRequest;
import com.lselink.elvis.control.dto.dynamic.WireTapStatusDto;
import com.lselink.elvis.control.service.DynamicControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 동적 제어(Sandbox 트래픽 격리 및 WireTap 실시간 패킷 감시) REST 컨트롤러
 * (Sprint 32: S32-API-0090-CO)
 * <p>
 * 관제 UI(PAGE-SESSION-01.html) 및 데스크톱 런처로부터
 * 특정 기기의 동적 Sandbox 격리 및 WireTap 감시를 제어하는 진입점입니다.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/control")
public class DynamicControlController {

    private static final Logger log = LoggerFactory.getLogger(DynamicControlController.class);

    private final DynamicControlService dynamicControlService;

    public DynamicControlController(DynamicControlService dynamicControlService) {
        this.dynamicControlService = dynamicControlService;
    }

    // === Sandbox (트래픽 격리, REQ-021) ===

    /**
     * 특정 충전기 Sandbox 격리 정책 설정/갱신
     * POST /api/v1/control/sandbox
     */
    @PostMapping("/sandbox")
    public ApiResponse<SandboxStatusDto> setSandboxPolicy(@RequestBody SandboxPolicyRequest request) {
        log.info("📡 [DynamicControl] Sandbox 격리 정책 요청: chargeBoxId={}, enabled={}, mode={}",
                request.getChargeBoxId(), request.getEnabled(), request.getPolicyMode());

        // [거버넌스 9조] 서비스 I/O 호출 변수 분리
        SandboxStatusDto result = dynamicControlService.setSandboxPolicy(request);
        return ApiResponse.ok(result);
    }

    /**
     * 현재 활성화된 Sandbox 격리 목록 조회
     * GET /api/v1/control/sandbox
     */
    @GetMapping("/sandbox")
    public ApiResponse<List<SandboxStatusDto>> getActiveSandboxList() {
        log.info("📡 [DynamicControl] 활성 Sandbox 목록 조회 요청");

        // [거버넌스 9조] 서비스 I/O 호출 변수 분리
        List<SandboxStatusDto> list = dynamicControlService.getActiveSandboxList();
        return ApiResponse.ok(list);
    }

    /**
     * 특정 충전기 Sandbox 격리 상태 조회
     * GET /api/v1/control/sandbox/{chargeBoxId}
     */
    @GetMapping("/sandbox/{chargeBoxId}")
    public ApiResponse<SandboxStatusDto> getSandboxStatus(@PathVariable String chargeBoxId) {
        log.info("📡 [DynamicControl] 특정 기기 Sandbox 상태 조회: chargeBoxId={}", chargeBoxId);

        // [거버넌스 9조] 서비스 I/O 호출 변수 분리
        SandboxStatusDto status = dynamicControlService.getSandboxStatus(chargeBoxId);
        return ApiResponse.ok(status);
    }

    /**
     * 특정 충전기 Sandbox 격리 해제
     * DELETE /api/v1/control/sandbox/{chargeBoxId}
     */
    @DeleteMapping("/sandbox/{chargeBoxId}")
    public ApiResponse<Boolean> removeSandboxPolicy(@PathVariable String chargeBoxId) {
        log.info("📡 [DynamicControl] Sandbox 격리 해제 요청: chargeBoxId={}", chargeBoxId);

        // [거버넌스 9조] 서비스 I/O 호출 변수 분리
        boolean removed = dynamicControlService.removeSandboxPolicy(chargeBoxId);
        return ApiResponse.ok(removed);
    }

    // === WireTap (실시간 패킷 감시, REQ-022) ===

    /**
     * 특정 충전기 WireTap 감시 정책 설정/갱신
     * POST /api/v1/control/wiretap
     */
    @PostMapping("/wiretap")
    public ApiResponse<WireTapStatusDto> setWireTapPolicy(@RequestBody WireTapPolicyRequest request) {
        log.info("📡 [DynamicControl] WireTap 감시 모드 설정 요청: chargeBoxId={}, enabled={}, level={}",
                request.getChargeBoxId(), request.getEnabled(), request.getTapLevel());

        // [거버넌스 9조] 서비스 I/O 호출 변수 분리
        WireTapStatusDto result = dynamicControlService.setWireTapPolicy(request);
        return ApiResponse.ok(result);
    }

    /**
     * 현재 활성화된 WireTap 감시 목록 조회
     * GET /api/v1/control/wiretap
     */
    @GetMapping("/wiretap")
    public ApiResponse<List<WireTapStatusDto>> getActiveWireTapList() {
        log.info("📡 [DynamicControl] 활성 WireTap 목록 조회 요청");

        // [거버넌스 9조] 서비스 I/O 호출 변수 분리
        List<WireTapStatusDto> list = dynamicControlService.getActiveWireTapList();
        return ApiResponse.ok(list);
    }

    /**
     * 특정 충전기 WireTap 감시 상태 조회
     * GET /api/v1/control/wiretap/{chargeBoxId}
     */
    @GetMapping("/wiretap/{chargeBoxId}")
    public ApiResponse<WireTapStatusDto> getWireTapStatus(@PathVariable String chargeBoxId) {
        log.info("📡 [DynamicControl] 특정 기기 WireTap 상태 조회: chargeBoxId={}", chargeBoxId);

        // [거버넌스 9조] 서비스 I/O 호출 변수 분리
        WireTapStatusDto status = dynamicControlService.getWireTapStatus(chargeBoxId);
        return ApiResponse.ok(status);
    }

    /**
     * 특정 충전기 WireTap 감시 즉시 중단
     * DELETE /api/v1/control/wiretap/{chargeBoxId}
     */
    @DeleteMapping("/wiretap/{chargeBoxId}")
    public ApiResponse<Boolean> removeWireTapPolicy(@PathVariable String chargeBoxId) {
        log.info("📡 [DynamicControl] WireTap 감시 중단 요청: chargeBoxId={}", chargeBoxId);

        // [거버넌스 9조] 서비스 I/O 호출 변수 분리
        boolean removed = dynamicControlService.removeWireTapPolicy(chargeBoxId);
        return ApiResponse.ok(removed);
    }
}
