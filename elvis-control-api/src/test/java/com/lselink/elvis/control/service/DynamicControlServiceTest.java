package com.lselink.elvis.control.service;

import com.lselink.elvis.control.dto.dynamic.SandboxPolicyRequest;
import com.lselink.elvis.control.dto.dynamic.SandboxStatusDto;
import com.lselink.elvis.control.dto.dynamic.WireTapPolicyRequest;
import com.lselink.elvis.control.dto.dynamic.WireTapStatusDto;
import com.lselink.elvis.control.service.impl.DynamicControlServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sandbox / WireTap 동적 제어 서비스 단위 테스트 (Sprint 32: S32-API-0090-TE)
 */
class DynamicControlServiceTest {

    private DynamicControlServiceImpl dynamicControlService;

    @BeforeEach
    void setUp() {
        dynamicControlService = new DynamicControlServiceImpl();
    }

    // === Sandbox 단위 테스트 ===

    @Test
    @DisplayName("Sandbox 격리 정책 등록 성공 및 상태 조회 검증")
    void setSandboxPolicy_enable_success() {
        // given
        SandboxPolicyRequest request = new SandboxPolicyRequest("00000001", true, "DROP", "테스트 악성 패킷 격리", 3600L);

        // when
        SandboxStatusDto status = dynamicControlService.setSandboxPolicy(request);

        // then
        assertThat(status).isNotNull();
        assertThat(status.getChargeBoxId()).isEqualTo("00000001");
        assertThat(status.isEnabled()).isTrue();
        assertThat(status.getPolicyMode()).isEqualTo("DROP");
        assertThat(status.getReason()).isEqualTo("테스트 악성 패킷 격리");
        assertThat(status.getExpiresAt()).isNotNull();
        assertThat(status.isExpired()).isFalse();

        // 인터셉터 판별 검증
        assertThat(dynamicControlService.isSandboxed("00000001")).isTrue();
        assertThat(dynamicControlService.isSandboxed("00000002")).isFalse();
    }

    @Test
    @DisplayName("Sandbox TTL 만료 시 isSandboxed 판별에서 false 반환 및 자동 제거 검증")
    void sandbox_ttl_expiration() {
        // given: 1초 TTL 설정
        SandboxPolicyRequest request = new SandboxPolicyRequest("00000001", true, "ISOLATED", "만료 테스트", 1L);
        dynamicControlService.setSandboxPolicy(request);

        // 강제로 만료 일시를 과거로 조작
        SandboxStatusDto status = dynamicControlService.getSandboxStatus("00000001");
        status.setExpiresAt(Instant.now().minusSeconds(10));

        // when & then
        assertThat(dynamicControlService.isSandboxed("00000001")).isFalse();
        assertThat(dynamicControlService.getSandboxStatus("00000001").isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Sandbox 정책 수동 해제(remove) 검증")
    void removeSandboxPolicy_success() {
        // given
        SandboxPolicyRequest request = new SandboxPolicyRequest("00000001", true, "MOCK", "수동 해제 테스트", 3600L);
        dynamicControlService.setSandboxPolicy(request);
        assertThat(dynamicControlService.isSandboxed("00000001")).isTrue();

        // when
        boolean removed = dynamicControlService.removeSandboxPolicy("00000001");

        // then
        assertThat(removed).isTrue();
        assertThat(dynamicControlService.isSandboxed("00000001")).isFalse();
    }

    // === WireTap 단위 테스트 ===

    @Test
    @DisplayName("WireTap 실시간 감시 모드 활성화 및 레벨 설정 검증")
    void setWireTapPolicy_enable_success() {
        // given
        WireTapPolicyRequest request = new WireTapPolicyRequest("00000001", true, "CALL_ONLY", 1800L);

        // when
        WireTapStatusDto status = dynamicControlService.setWireTapPolicy(request);

        // then
        assertThat(status).isNotNull();
        assertThat(status.getChargeBoxId()).isEqualTo("00000001");
        assertThat(status.isEnabled()).isTrue();
        assertThat(status.getTapLevel()).isEqualTo("CALL_ONLY");
        assertThat(status.getExpiresAt()).isNotNull();
        assertThat(status.isExpired()).isFalse();

        // 인터셉터 판별 검증
        assertThat(dynamicControlService.isWireTapped("00000001")).isTrue();
        assertThat(dynamicControlService.isWireTapped("00000002")).isFalse();
    }

    @Test
    @DisplayName("WireTap 타임아웃 만료 시 isWireTapped 판별에서 false 반환 검증")
    void wiretap_ttl_expiration() {
        // given
        WireTapPolicyRequest request = new WireTapPolicyRequest("00000001", true, "ALL", 1800L);
        dynamicControlService.setWireTapPolicy(request);

        // 강제로 과거 시각 설정
        WireTapStatusDto status = dynamicControlService.getWireTapStatus("00000001");
        status.setExpiresAt(Instant.now().minusSeconds(10));

        // when & then
        assertThat(dynamicControlService.isWireTapped("00000001")).isFalse();
        assertThat(dynamicControlService.getWireTapStatus("00000001").isEnabled()).isFalse();
    }

    @Test
    @DisplayName("WireTap 정책 수동 중단 검증")
    void removeWireTapPolicy_success() {
        // given
        WireTapPolicyRequest request = new WireTapPolicyRequest("00000001", true, "ALL", 1800L);
        dynamicControlService.setWireTapPolicy(request);
        assertThat(dynamicControlService.isWireTapped("00000001")).isTrue();

        // when
        boolean removed = dynamicControlService.removeWireTapPolicy("00000001");

        // then
        assertThat(removed).isTrue();
        assertThat(dynamicControlService.isWireTapped("00000001")).isFalse();
    }

    // === 종합 목록 및 채번 표준 검증 ===

    @Test
    @DisplayName("활성 Sandbox 및 WireTap 목록 조회 검증")
    void getActiveLists_success() {
        // given
        dynamicControlService.setSandboxPolicy(new SandboxPolicyRequest("00000001", true, "DROP", "사유1", 3600L));
        dynamicControlService.setSandboxPolicy(new SandboxPolicyRequest("00000002", true, "ISOLATED", "사유2", 3600L));

        dynamicControlService.setWireTapPolicy(new WireTapPolicyRequest("00000001", true, "ALL", 1800L));

        // when
        List<SandboxStatusDto> sandboxList = dynamicControlService.getActiveSandboxList();
        List<WireTapStatusDto> wireTapList = dynamicControlService.getActiveWireTapList();

        // then
        assertThat(sandboxList).hasSize(2);
        assertThat(wireTapList).hasSize(1);
    }

    @Test
    @DisplayName("chargeBoxId가 null 또는 공백인 경우 거버넌스 10조 표준 기본값(00000001) 정규화 검증")
    void chargeBoxId_null_normalization() {
        // given
        SandboxPolicyRequest request = new SandboxPolicyRequest(null, true, "DROP", "기본값 테스트", 3600L);

        // when
        SandboxStatusDto status = dynamicControlService.setSandboxPolicy(request);

        // then
        assertThat(status.getChargeBoxId()).isEqualTo("00000001");
        assertThat(dynamicControlService.isSandboxed("00000001")).isTrue();
    }
}
