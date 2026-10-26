package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.common.ApiResponse;
import com.lselink.elvis.control.config.StompSessionChannelInterceptor;
import com.lselink.elvis.control.dto.UiNotificationDto;
import com.lselink.elvis.control.service.NotificationPushService;
import com.lselink.elvis.control.service.RecentNotificationBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 실시간 알림 REST 컨트롤러 단위 테스트 (Sprint 28: S28-API-0060-TE)
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private RecentNotificationBuffer ringBuffer;

    @Mock
    private NotificationPushService pushService;

    @Mock
    private StompSessionChannelInterceptor sessionInterceptor;

    private NotificationController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationController(ringBuffer, pushService, sessionInterceptor);
    }

    @Test
    @DisplayName("최근 발생 알림 목록 조회(GET /recent) 성공 검증")
    void testGetRecentNotifications() {
        // given
        UiNotificationDto sample = new UiNotificationDto(
                "EVT-01", "FAULT_DETECTED", "CRITICAL", "00000001", 1,
                "Faulted", "GroundFailure", "E01", "접지 이상",
                LocalDateTime.now(), "EMERGENCY_SHUTDOWN", true
        );
        when(ringBuffer.getRecentList(50)).thenReturn(Collections.singletonList(sample));

        // when
        ApiResponse<List<UiNotificationDto>> response = controller.getRecentNotifications(50);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getEventId()).isEqualTo("EVT-01");
        verify(ringBuffer, times(1)).getRecentList(50);
    }

    @Test
    @DisplayName("웹소켓 세션 및 버퍼 상태 메트릭(GET /metrics) 조회 검증")
    void testGetNotificationMetrics() {
        // given
        when(sessionInterceptor.getActiveSessionCount()).thenReturn(3);
        when(ringBuffer.size()).thenReturn(12);

        // when
        ApiResponse<Map<String, Object>> response = controller.getNotificationMetrics();

        // then
        assertThat(response.isSuccess()).isTrue();
        Map<String, Object> data = response.getData();
        assertThat(data.get("activeWebSocketSessions")).isEqualTo(3);
        assertThat(data.get("bufferedNotificationCount")).isEqualTo(12);
    }

    @Test
    @DisplayName("가상 장애 알림 테스트 발생(POST /test-fault) 성공 검증")
    void testGenerateTestFault() {
        // when
        ApiResponse<UiNotificationDto> response = controller.generateTestFault(
                "00000002", 1, "OverCurrentFailure", "CRITICAL"
        );

        // then
        assertThat(response.isSuccess()).isTrue();
        UiNotificationDto event = response.getData();
        assertThat(event.getChargeBoxId()).isEqualTo("00000002");
        assertThat(event.getErrorCode()).isEqualTo("OverCurrentFailure");
        assertThat(event.getSeverity()).isEqualTo("CRITICAL");
        assertThat(event.getEventId()).startsWith("EVT-TEST-");

        verify(pushService, times(1)).pushNotification(any(UiNotificationDto.class));
    }
}
