package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.common.ApiResponse;
import com.lselink.elvis.control.dto.dynamic.SandboxPolicyRequest;
import com.lselink.elvis.control.dto.dynamic.SandboxStatusDto;
import com.lselink.elvis.control.dto.dynamic.WireTapPolicyRequest;
import com.lselink.elvis.control.dto.dynamic.WireTapStatusDto;
import com.lselink.elvis.control.service.DynamicControlService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
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
 */
@RestController
@RequestMapping("/api/v1/control")
public class DynamicControlController {

    private static final Logger log = LoggerFactory.getLogger(DynamicControlController.class);

    private final DynamicControlService dynamicControlService;

    public DynamicControlController(DynamicControlService dynamicControlService) {
        this.dynamicControlService = dynamicControlService;
    }

    @PostMapping("/sandbox")
    public ApiResponse<SandboxStatusDto> setSandboxPolicy(@Valid @RequestBody SandboxPolicyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Sandbox 정책 요청 본문은 필수입니다.");
        }
        log.info("[DynamicControl] Sandbox 격리 정책 요청: chargeBoxId={}, enabled={}, mode={}",
                request.getChargeBoxId(), request.getEnabled(), request.getPolicyMode());

        SandboxStatusDto result = dynamicControlService.setSandboxPolicy(request);
        return ApiResponse.ok(result);
    }

    @GetMapping("/sandbox")
    public ApiResponse<List<SandboxStatusDto>> getActiveSandboxList() {
        log.info("[DynamicControl] 활성 Sandbox 목록 조회 요청");

        List<SandboxStatusDto> list = dynamicControlService.getActiveSandboxList();
        return ApiResponse.ok(list);
    }

    @GetMapping("/sandbox/{chargeBoxId}")
    public ApiResponse<SandboxStatusDto> getSandboxStatus(@PathVariable String chargeBoxId) {
        requireValidChargeBoxIdPath(chargeBoxId);
        log.info("[DynamicControl] 특정 기기 Sandbox 상태 조회: chargeBoxId={}", chargeBoxId);

        SandboxStatusDto status = dynamicControlService.getSandboxStatus(chargeBoxId);
        return ApiResponse.ok(status);
    }

    @DeleteMapping("/sandbox/{chargeBoxId}")
    public ApiResponse<Boolean> removeSandboxPolicy(@PathVariable String chargeBoxId) {
        requireValidChargeBoxIdPath(chargeBoxId);
        log.info("[DynamicControl] Sandbox 격리 해제 요청: chargeBoxId={}", chargeBoxId);

        boolean removed = dynamicControlService.removeSandboxPolicy(chargeBoxId);
        return ApiResponse.ok(removed);
    }

    @PostMapping("/wiretap")
    public ApiResponse<WireTapStatusDto> setWireTapPolicy(@Valid @RequestBody WireTapPolicyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("WireTap 정책 요청 본문은 필수입니다.");
        }
        log.info("[DynamicControl] WireTap 감시 모드 설정 요청: chargeBoxId={}, enabled={}, level={}",
                request.getChargeBoxId(), request.getEnabled(), request.getTapLevel());

        WireTapStatusDto result = dynamicControlService.setWireTapPolicy(request);
        return ApiResponse.ok(result);
    }

    @GetMapping("/wiretap")
    public ApiResponse<List<WireTapStatusDto>> getActiveWireTapList() {
        log.info("[DynamicControl] 활성 WireTap 목록 조회 요청");

        List<WireTapStatusDto> list = dynamicControlService.getActiveWireTapList();
        return ApiResponse.ok(list);
    }

    @GetMapping("/wiretap/{chargeBoxId}")
    public ApiResponse<WireTapStatusDto> getWireTapStatus(@PathVariable String chargeBoxId) {
        requireValidChargeBoxIdPath(chargeBoxId);
        log.info("[DynamicControl] 특정 기기 WireTap 상태 조회: chargeBoxId={}", chargeBoxId);

        WireTapStatusDto status = dynamicControlService.getWireTapStatus(chargeBoxId);
        return ApiResponse.ok(status);
    }

    @DeleteMapping("/wiretap/{chargeBoxId}")
    public ApiResponse<Boolean> removeWireTapPolicy(@PathVariable String chargeBoxId) {
        requireValidChargeBoxIdPath(chargeBoxId);
        log.info("[DynamicControl] WireTap 감시 중단 요청: chargeBoxId={}", chargeBoxId);

        boolean removed = dynamicControlService.removeWireTapPolicy(chargeBoxId);
        return ApiResponse.ok(removed);
    }

    private void requireValidChargeBoxIdPath(String chargeBoxId) {
        if (!StringUtils.hasText(chargeBoxId)) {
            throw new IllegalArgumentException("chargeBoxId는 필수입니다.");
        }
    }
}
