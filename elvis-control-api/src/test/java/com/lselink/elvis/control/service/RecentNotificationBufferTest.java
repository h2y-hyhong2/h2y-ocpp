package com.lselink.elvis.control.service;

import com.lselink.elvis.control.dto.UiNotificationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 최근 알림 인메모리 링 버퍼 단위 테스트 (Sprint 28: S28-API-0060-TE)
 */
class RecentNotificationBufferTest {

    private RecentNotificationBuffer buffer;

    @BeforeEach
    void setUp() {
        // 테스트용 최대 5건 버퍼 생성
        buffer = new RecentNotificationBuffer(5);
    }

    @Test
    @DisplayName("알림 적재 및 최신순 역순 조회 검증")
    void testAddAndGetRecentList() {
        // given
        UiNotificationDto event1 = createDto("EVT-01", "00000001", 1, "GroundFailure");
        UiNotificationDto event2 = createDto("EVT-02", "00000001", 2, "OverCurrentFailure");
        UiNotificationDto event3 = createDto("EVT-03", "00000002", 1, "HighTemperature");

        // when
        buffer.add(event1);
        buffer.add(event2);
        buffer.add(event3);

        // then
        assertThat(buffer.size()).isEqualTo(3);

        List<UiNotificationDto> list = buffer.getRecentList(10);
        assertThat(list).hasSize(3);
        // 최신순 (event3 -> event2 -> event1)
        assertThat(list.get(0).getEventId()).isEqualTo("EVT-03");
        assertThat(list.get(1).getEventId()).isEqualTo("EVT-02");
        assertThat(list.get(2).getEventId()).isEqualTo("EVT-01");
    }

    @Test
    @DisplayName("버퍼 용량 초과 시 FIFO 밀어내기(Eviction) 검증")
    void testRingBufferCapacityEviction() {
        // given: 용량 5인 버퍼에 7개 적재
        for (int i = 1; i <= 7; i++) {
            String eventId = String.format("EVT-%02d", i);
            buffer.add(createDto(eventId, "00000001", 1, "GroundFailure"));
        }

        // then: 최대 5개 유지 및 오래된 1, 2번 제거, 7~3번 유지
        assertThat(buffer.size()).isEqualTo(5);

        List<UiNotificationDto> list = buffer.getRecentList(5);
        assertThat(list).hasSize(5);
        assertThat(list.get(0).getEventId()).isEqualTo("EVT-07");
        assertThat(list.get(4).getEventId()).isEqualTo("EVT-03");
    }

    @Test
    @DisplayName("비정상 limit 요청 시 안전 기본값 방어 검증")
    void testSafeLimitHandling() {
        // given
        buffer.add(createDto("EVT-01", "00000001", 1, "NoError"));

        // when: limit 0 이하 요청
        List<UiNotificationDto> zeroList = buffer.getRecentList(0);
        assertThat(zeroList).hasSize(1);

        // when: limit 음수 요청
        List<UiNotificationDto> negativeList = buffer.getRecentList(-5);
        assertThat(negativeList).hasSize(1);

        // when: null 입력 방어
        buffer.add(null);
        assertThat(buffer.size()).isEqualTo(1);
    }

    private UiNotificationDto createDto(String eventId, String chargeBoxId, int connectorId, String errorCode) {
        return new UiNotificationDto(
                eventId,
                "FAULT_DETECTED",
                "CRITICAL",
                chargeBoxId,
                connectorId,
                "Faulted",
                errorCode,
                "VENDOR_ERR",
                "장애가 발생했습니다.",
                LocalDateTime.now(),
                "EMERGENCY_SHUTDOWN",
                true
        );
    }
}
