package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.common.ApiResponse;
import com.lselink.elvis.control.dto.dynamic.SandboxPolicyRequest;
import com.lselink.elvis.control.dto.dynamic.SandboxStatusDto;
import com.lselink.elvis.control.dto.dynamic.WireTapPolicyRequest;
import com.lselink.elvis.control.dto.dynamic.WireTapStatusDto;
import com.lselink.elvis.control.service.DynamicControlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Sandbox / WireTap 동적 제어 REST 컨트롤러 단위 테스트 (Sprint 32: S32-API-0090-TE)
 */
@ExtendWith(MockitoExtension.class)
class DynamicControlControllerTest {

    @Mock
    private DynamicControlService dynamicControlService;

    @InjectMocks
    private DynamicControlController dynamicControlController;

    @Test
    @DisplayName("Sandbox 정책 등록 엔드포인트 응답 래핑 검증")
    void setSandboxPolicy_endpoint() {
        // given
        SandboxPolicyRequest request = new SandboxPolicyRequest("00000001", true, "DROP", "테스트", 3600L);
        SandboxStatusDto mockDto = new SandboxStatusDto("00000001", true, "DROP", "테스트", Instant.now(), null);
        when(dynamicControlService.setSandboxPolicy(any(SandboxPolicyRequest.class))).thenReturn(mockDto);

        // when
        ApiResponse<SandboxStatusDto> response = dynamicControlController.setSandboxPolicy(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getChargeBoxId()).isEqualTo("00000001");
        assertThat(response.getData().getPolicyMode()).isEqualTo("DROP");
    }

    @Test
    @DisplayName("활성 Sandbox 목록 조회 엔드포인트 응답 검증")
    void getActiveSandboxList_endpoint() {
        // given
        SandboxStatusDto mockDto = new SandboxStatusDto("00000001", true, "ISOLATED", "사유", Instant.now(), null);
        when(dynamicControlService.getActiveSandboxList()).thenReturn(Collections.singletonList(mockDto));

        // when
        ApiResponse<List<SandboxStatusDto>> response = dynamicControlController.getActiveSandboxList();

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
    }

    @Test
    @DisplayName("특정 기기 Sandbox 상태 조회 엔드포인트 검증")
    void getSandboxStatus_endpoint() {
        // given
        SandboxStatusDto mockDto = new SandboxStatusDto("00000001", true, "MOCK", "사유", Instant.now(), null);
        when(dynamicControlService.getSandboxStatus(eq("00000001"))).thenReturn(mockDto);

        // when
        ApiResponse<SandboxStatusDto> response = dynamicControlController.getSandboxStatus("00000001");

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getPolicyMode()).isEqualTo("MOCK");
    }

    @Test
    @DisplayName("Sandbox 정책 해제 엔드포인트 응답 검증")
    void removeSandboxPolicy_endpoint() {
        // given
        when(dynamicControlService.removeSandboxPolicy(eq("00000001"))).thenReturn(true);

        // when
        ApiResponse<Boolean> response = dynamicControlController.removeSandboxPolicy("00000001");

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isTrue();
    }

    @Test
    @DisplayName("WireTap 정책 등록 엔드포인트 응답 래핑 검증")
    void setWireTapPolicy_endpoint() {
        // given
        WireTapPolicyRequest request = new WireTapPolicyRequest("00000001", true, "ALL", 1800L);
        WireTapStatusDto mockDto = new WireTapStatusDto("00000001", true, "ALL", Instant.now(), null);
        when(dynamicControlService.setWireTapPolicy(any(WireTapPolicyRequest.class))).thenReturn(mockDto);

        // when
        ApiResponse<WireTapStatusDto> response = dynamicControlController.setWireTapPolicy(request);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getTapLevel()).isEqualTo("ALL");
    }

    @Test
    @DisplayName("WireTap 감시 해제 엔드포인트 응답 검증")
    void removeWireTapPolicy_endpoint() {
        // given
        when(dynamicControlService.removeWireTapPolicy(eq("00000001"))).thenReturn(true);

        // when
        ApiResponse<Boolean> response = dynamicControlController.removeWireTapPolicy("00000001");

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isTrue();
    }
}
