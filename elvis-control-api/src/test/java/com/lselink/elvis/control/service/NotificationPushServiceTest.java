package com.lselink.elvis.control.service;

import com.lselink.elvis.control.dto.UiNotificationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 실시간 알림 푸시 및 3단 팬아웃 서비스 단위 테스트 (Sprint 28: S28-API-0060-TE)
 */
@ExtendWith(MockitoExtension.class)
class NotificationPushServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private RecentNotificationBuffer ringBuffer;
    private NotificationPushServiceImpl pushService;

    @BeforeEach
    void setUp() {
        ringBuffer = new RecentNotificationBuffer(50);
        pushService = new NotificationPushServiceImpl(messagingTemplate, ringBuffer);
    }

    @Test
    @DisplayName("3단 계층 토픽(공통/충전소/충전기) 팬아웃 브로드캐스트 검증")
    void testMultiTopicFanout() {
        // given: 거버넌스 10조 표준 채번 (충전소: 000000, 충전기: 01 -> chargeBoxId: 00000001)
        UiNotificationDto dto = new UiNotificationDto(
                "EVT-20261026-0001",
                "FAULT_DETECTED",
                "CRITICAL",
                "00000001",
                1,
                "Faulted",
                "GroundFailure",
                "ERR_SHORT",
                "접지 장애 감지",
                LocalDateTime.now(),
                "EMERGENCY_SHUTDOWN",
                true
        );

        // when
        pushService.pushNotification(dto);

        // then: 링 버퍼에 적재 확인
        assertThat(ringBuffer.size()).isEqualTo(1);
        assertThat(ringBuffer.getRecentList(1).get(0).getEventId()).isEqualTo("EVT-20261026-0001");

        // then: 3개 토픽으로 전파되었는지 SimpMessagingTemplate 캡처 검증
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        verify(messagingTemplate, times(3)).convertAndSend(destinationCaptor.capture(), payloadCaptor.capture());

        List<String> capturedDestinations = destinationCaptor.getAllValues();
        assertThat(capturedDestinations).containsExactly(
                "/topic/notifications",
                "/topic/station/000000",
                "/topic/charger/00000001"
        );
    }

    @Test
    @DisplayName("null 입력 시 예외 없이 방어 처리 검증")
    void testNullNotificationHandling() {
        // when
        pushService.pushNotification(null);

        // then
        assertThat(ringBuffer.size()).isEqualTo(0);
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}
