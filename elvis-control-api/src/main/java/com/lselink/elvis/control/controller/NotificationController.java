package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.common.ApiResponse;
import com.lselink.elvis.control.config.StompSessionChannelInterceptor;
import com.lselink.elvis.control.dto.UiNotificationDto;
import com.lselink.elvis.control.service.NotificationPushService;
import com.lselink.elvis.control.service.RecentNotificationBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 실시간 관제 알림 조회 및 테스트 발생 REST 컨트롤러 (Sprint 28: S28-API-0060-CO)
 * <p>
 * 신규 접속 클라이언트 초기 하이드레이션(최근 알림 N건 조회) 및 세션 통계, E2E 테스트 알림 발생을 담당합니다.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final RecentNotificationBuffer ringBuffer;
    private final NotificationPushService pushService;
    private final StompSessionChannelInterceptor sessionInterceptor;

    public NotificationController(RecentNotificationBuffer ringBuffer,
                                  NotificationPushService pushService,
                                  StompSessionChannelInterceptor sessionInterceptor) {
        this.ringBuffer = ringBuffer;
        this.pushService = pushService;
        this.sessionInterceptor = sessionInterceptor;
    }

    /**
     * 최근 발생한 장애 알림 목록 조회 (초기 하이드레이션용)
     *
     * @param limit 조회할 최대 건수 (기본 50건)
     * @return 최신순 알림 DTO 목록
     */
    @GetMapping("/recent")
    public ApiResponse<List<UiNotificationDto>> getRecentNotifications(
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        List<UiNotificationDto> recentList = ringBuffer.getRecentList(limit);
        log.info("[NotificationController] 최근 알림 조회 완료: 요청건수={}, 반환건수={}", limit, recentList.size());
        return ApiResponse.ok(recentList);
    }

    /**
     * 실시간 웹소켓 세션 및 버퍼 상태 메트릭 조회
     */
    @GetMapping("/metrics")
    public ApiResponse<Map<String, Object>> getNotificationMetrics() {
        int activeSessions = sessionInterceptor.getActiveSessionCount();
        int bufferedCount = ringBuffer.size();

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("activeWebSocketSessions", activeSessions);
        metrics.put("bufferedNotificationCount", bufferedCount);
        metrics.put("serverTime", LocalDateTime.now());

        return ApiResponse.ok(metrics);
    }

    /**
     * 가상 장애 알림 발생 테스트 API (E2E 검증 및 데모 시연용)
     */
    @PostMapping("/test-fault")
    public ApiResponse<UiNotificationDto> generateTestFault(
            @RequestParam(name = "chargeBoxId", defaultValue = "00000001") String chargeBoxId,
            @RequestParam(name = "connectorId", defaultValue = "1") int connectorId,
            @RequestParam(name = "errorCode", defaultValue = "GroundFailure") String errorCode,
            @RequestParam(name = "severity", defaultValue = "CRITICAL") String severity) {

        String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String eventId = "EVT-TEST-" + randomSuffix;
        String message = String.format("[%s] %d번 커넥터 %s 고장이 감지되었습니다.", chargeBoxId, connectorId, errorCode);

        UiNotificationDto testEvent = new UiNotificationDto(
                eventId,
                "FAULT_DETECTED",
                severity,
                chargeBoxId,
                connectorId,
                "Faulted",
                errorCode,
                "TEST_ERR_999",
                message,
                LocalDateTime.now(),
                "EMERGENCY_SHUTDOWN",
                true
        );

        pushService.pushNotification(testEvent);
        log.info("[NotificationController] 가상 장애 알림 테스트 발행 완료: eventId={}", eventId);
        return ApiResponse.ok(testEvent);
    }
}
