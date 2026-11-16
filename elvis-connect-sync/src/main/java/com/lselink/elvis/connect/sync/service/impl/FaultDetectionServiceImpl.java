package com.lselink.elvis.connect.sync.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lselink.elvis.connect.sync.domain.FaultHistory;
import com.lselink.elvis.connect.sync.domain.FaultSeverity;
import com.lselink.elvis.connect.sync.dto.FaultNotificationEvent;
import com.lselink.elvis.connect.sync.ocpp.OcppParsedMessage;
import com.lselink.elvis.connect.sync.producer.NotificationProducer;
import com.lselink.elvis.connect.sync.repository.FaultHistoryRepository;
import com.lselink.elvis.connect.sync.service.FaultDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 충전기 실시간 장애 감지 및 자동 복구 서비스 구현체 (Sprint 23: S23-SYNC-0060-CO)
 * <p>
 * 10ms 이하 인라인 장애 판정, Bounded Caffeine 기반 Flapping 방어 L1 캐시,
 * MySQL 원장 저장(중복 미해결 이력 방지) 및 Kafka UI 알림 발행 수행.
 * </p>
 */
@Service
public class FaultDetectionServiceImpl implements FaultDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FaultDetectionServiceImpl.class);
    private static final DateTimeFormatter EVENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final long MAX_FAULT_CACHE_SIZE = 10_000L;

    private final FaultHistoryRepository faultHistoryRepository;
    private final NotificationProducer notificationProducer;
    private final long flappingWindowSeconds;

    /**
     * 충전기 커넥터별 최신 장애 상태 Bounded L1 캐시 (Flapping 억제 및 메모리 누수 방지)
     * Key: {chargeBoxId}:{connectorId}
     */
    private final Cache<String, FaultCacheState> faultStateCache;

    public FaultDetectionServiceImpl(
            FaultHistoryRepository faultHistoryRepository,
            NotificationProducer notificationProducer,
            @Value("${elvis.sync.fault.flapping-window-seconds:60}") long flappingWindowSeconds
    ) {
        this.faultHistoryRepository = faultHistoryRepository;
        this.notificationProducer = notificationProducer;
        this.flappingWindowSeconds = flappingWindowSeconds > 0 ? flappingWindowSeconds : 60L;
        long cacheTtlSeconds = Math.max(this.flappingWindowSeconds * 5, 600L);
        this.faultStateCache = Caffeine.newBuilder()
                .maximumSize(MAX_FAULT_CACHE_SIZE)
                .expireAfterWrite(cacheTtlSeconds, TimeUnit.SECONDS)
                .build();
    }

    @Override
    @Transactional
    public void processFaultEvent(OcppParsedMessage msg) {
        String chargeBoxId = msg.getChargeBoxId();
        JsonNode payload = msg.getPayload();

        int connectorId = (payload != null && payload.hasNonNull("connectorId"))
                ? payload.get("connectorId").asInt(0)
                : 0;
        String status = extractNonBlankText(payload, "status", "Unknown");
        String errorCode = extractNonBlankText(payload, "errorCode", "NoError");
        String vendorErrorCode = extractNonBlankText(payload, "vendorErrorCode", null);
        String info = extractNonBlankText(payload, "info", null);

        LocalDateTime eventTime = LocalDateTime.now();

        // 1. 장애 상태인지 여부 판정
        boolean isFault = isFaultCondition(status, errorCode);

        if (isFault) {
            // [장애 감지 케이스]
            handleFaultDetected(chargeBoxId, connectorId, status, errorCode, vendorErrorCode, info, eventTime);
        } else {
            // [정상 복구 감지 케이스: status != 'Faulted' AND (errorCode == null or errorCode == 'NoError')]
            handleFaultRecovery(chargeBoxId, connectorId);
        }
    }

    /**
     * 장애 감지 조건 평가
     * - status가 'Faulted' 이거나
     * - errorCode가 'NoError'가 아닌 경우 (단, null/빈값 제외)
     */
    @Override
    public boolean isFaultCondition(String status, String errorCode) {
        if ("Faulted".equalsIgnoreCase(status)) {
            return true;
        }

        if (errorCode != null && !errorCode.isBlank() && !errorCode.equalsIgnoreCase("null")) {
            if (!"NoError".equalsIgnoreCase(errorCode)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 동일 에러 연속 인입(Flapping) 방어 판정 (원자적 compute 기반 레이스 컨디션 제거)
     */
    @Override
    public boolean shouldTriggerAlert(String chargeBoxId, int connectorId, String errorCode) {
        String cacheKey = chargeBoxId + ":" + connectorId;
        String safeErrorCode = (errorCode != null && !errorCode.isBlank()) ? errorCode : "OtherError";
        LocalDateTime now = LocalDateTime.now();
        AtomicBoolean triggered = new AtomicBoolean(false);

        faultStateCache.asMap().compute(cacheKey, (key, previousState) -> {
            if (previousState == null) {
                triggered.set(true);
                return new FaultCacheState(safeErrorCode, now);
            }

            boolean isSameError = previousState.errorCode().equalsIgnoreCase(safeErrorCode);
            LocalDateTime windowThreshold = previousState.lastReportedAt().plusSeconds(flappingWindowSeconds);
            if (isSameError && now.isBefore(windowThreshold)) {
                log.debug("[FaultFilter] Flapping 억제 적용: chargeBoxId={}, connectorId={}, errorCode={}",
                        chargeBoxId, connectorId, safeErrorCode);
                return previousState;
            }

            triggered.set(true);
            return new FaultCacheState(safeErrorCode, now);
        });

        return triggered.get();
    }

    /**
     * 활성 장애 자동 복구 마감 처리 (미해결 장애가 존재할 때만 UPDATE 실행)
     */
    @Override
    @Transactional
    public void resolveActiveFault(String chargeBoxId, int connectorId) {
        if (!faultHistoryRepository.existsActiveFault(chargeBoxId, connectorId)) {
            return;
        }

        LocalDateTime resolvedTime = LocalDateTime.now();
        int resolvedCount = faultHistoryRepository.resolveActiveFaults(chargeBoxId, connectorId, resolvedTime);

        if (resolvedCount > 0) {
            log.info("[FaultRecovery] 활성 장애 자동 마감 완료: chargeBoxId={}, connectorId={}, count={}",
                    chargeBoxId, connectorId, resolvedCount);

            // 복구 알림 이벤트 생성 및 Kafka 발행
            String eventId = generateEventId();
            String recoveryMsg = "충전기 장애가 정상 복구되었습니다 (정상 상태 전환).";

            FaultNotificationEvent event = new FaultNotificationEvent(
                    eventId,
                    "FAULT_RESOLVED",
                    FaultSeverity.INFO,
                    chargeBoxId,
                    connectorId,
                    "Available",
                    "NoError",
                    null,
                    recoveryMsg,
                    resolvedTime,
                    "NONE"
            );

            notificationProducer.sendFaultNotification(chargeBoxId, event);
        }
    }

    /**
     * 장애 감지 처리 서브루틴
     */
    private void handleFaultDetected(String chargeBoxId,
                                     int connectorId,
                                     String status,
                                     String errorCode,
                                     String vendorErrorCode,
                                     String info,
                                     LocalDateTime eventTime) {

        // Flapping 방어 필터 검사
        boolean needAlert = shouldTriggerAlert(chargeBoxId, connectorId, errorCode);
        if (!needAlert) {
            return;
        }

        // 지속 장애 재유입 시 동일 미해결 이력 중복 INSERT 방지
        Optional<FaultHistory> existingActive = faultHistoryRepository.findLatestUnresolvedFault(chargeBoxId, connectorId);
        if (existingActive.isPresent() && errorCode.equalsIgnoreCase(existingActive.get().getErrorCode())) {
            log.debug("[FaultDetected] 기존 미해결 동일 장애 유지 중 (원장 중복 등록 생략): chargeBoxId={}, connectorId={}, errorCode={}",
                    chargeBoxId, connectorId, errorCode);
            return;
        }

        // 다른 에러코드로 전이된 경우 기존 미해결 장애 마감 후 신규 등록
        if (existingActive.isPresent()) {
            faultHistoryRepository.resolveActiveFaults(chargeBoxId, connectorId, eventTime);
        }

        // 심각도 및 권장조치 도출
        FaultSeverity severity = FaultSeverity.fromErrorCode(errorCode);
        String actionRequired = determineActionRequired(severity);
        String faultMessage = buildFaultMessage(errorCode, vendorErrorCode, info);

        // 1. MySQL fault_history 원장 INSERT
        FaultHistory faultHistory = FaultHistory.builder()
                .chargeBoxId(chargeBoxId)
                .connectorId(connectorId)
                .status(status)
                .errorCode(errorCode)
                .vendorErrorCode(vendorErrorCode)
                .severity(severity)
                .faultMsg(faultMessage)
                .actionRequired(actionRequired)
                .occurredDt(eventTime)
                .build();

        faultHistoryRepository.save(faultHistory);

        log.warn("[FaultDetected] 신규 장애 감지 및 원장 등록: chargeBoxId={}, connectorId={}, errorCode={}, severity={}",
                chargeBoxId, connectorId, errorCode, severity);

        // 2. Kafka ocpp-ui-notifications 알림 발행
        String eventId = generateEventId();
        FaultNotificationEvent event = new FaultNotificationEvent(
                eventId,
                "FAULT_DETECTED",
                severity,
                chargeBoxId,
                connectorId,
                status,
                errorCode,
                vendorErrorCode,
                faultMessage,
                eventTime,
                actionRequired
        );

        notificationProducer.sendFaultNotification(chargeBoxId, event);
    }

    /**
     * 장애 복구 처리 서브루틴
     */
    private void handleFaultRecovery(String chargeBoxId, int connectorId) {
        String cacheKey = chargeBoxId + ":" + connectorId;
        FaultCacheState previousState = faultStateCache.asMap().remove(cacheKey);

        if (previousState != null) {
            log.info("[FaultRecovery] 장애 상태 캐시 해제: chargeBoxId={}, connectorId={}, prevError={}",
                    chargeBoxId, connectorId, previousState.errorCode());
        }

        // DB 활성 장애 존재 시에만 자동 마감 및 복구 알림 발행
        resolveActiveFault(chargeBoxId, connectorId);
    }

    private String extractNonBlankText(JsonNode payload, String field, String defaultValue) {
        if (payload == null || !payload.hasNonNull(field)) {
            return defaultValue;
        }
        String text = payload.get(field).asText(null);
        if (text == null || text.isBlank() || text.equalsIgnoreCase("null")) {
            return defaultValue;
        }
        return text;
    }

    /**
     * 심각도에 따른 권장 조치 도출
     */
    private String determineActionRequired(FaultSeverity severity) {
        if (severity == FaultSeverity.CRITICAL) {
            return "EMERGENCY_SHUTDOWN";
        } else if (severity == FaultSeverity.MAJOR) {
            return "INSPECTION";
        } else {
            return "NONE";
        }
    }

    /**
     * 화면 표시용 한글 에러 설명 문구 생성
     */
    private String buildFaultMessage(String errorCode, String vendorErrorCode, String info) {
        String baseDesc = switch (errorCode) {
            case "ConnectorLockFailure" -> "커넥터 잠금 장치 체결/해제 실패";
            case "EVCommunicationError" -> "전기차량과 충전기 간 통신 오류 (CAN/PLC 통신 불안정)";
            case "GroundFailure"        -> "접지 결함 감지 (누전 위험 긴급 차단)";
            case "HighTemperature"     -> "충전 모듈 과열 경고 (온도 센서 임계치 초과)";
            case "InternalError"       -> "충전기 내부 제어기(MCU/DSP) 결함";
            case "LocalListConflict"   -> "로컬 인증 목록 충돌";
            case "NoError"             -> "정상 상태";
            case "OtherError"          -> "기타 시스템 알 수 없는 에러";
            case "OverCurrentFailure"  -> "출력 전류 과전류 감지 차단";
            case "OverVoltage"         -> "입력/출력 전압 과전압 감지 차단";
            case "PowerMeterFailure"   -> "전력량계(Meter) 통신 또는 계량 데이터 오류";
            case "PowerSwitchFailure"  -> "파워 스위치(릴레이/접촉기) 오동작 결함";
            case "ReaderFailure"       -> "RFID 카드 리더기 인식 오류";
            case "ResetFailure"        -> "충전기 리셋 실패";
            case "UnderVoltage"        -> "입력 전압 저전압 감지";
            case "WeakSignal"          -> "무선 통신 모뎀 신호 미약 (통신 지연)";
            default                    -> "상세 에러코드: " + errorCode;
        };

        StringBuilder sb = new StringBuilder(baseDesc);
        if (vendorErrorCode != null && !vendorErrorCode.isBlank()) {
            sb.append(" (제조사코드: ").append(vendorErrorCode).append(")");
        }
        if (info != null && !info.isBlank()) {
            sb.append(" [상세정보: ").append(info).append("]");
        }

        return sb.toString();
    }

    /**
     * 고유 이벤트 ID 생성
     */
    private String generateEventId() {
        String timestampPart = LocalDateTime.now().format(EVENT_TIME_FORMATTER);
        String uuidPart = UUID.randomUUID().toString().substring(0, 8);
        return "EVT-" + timestampPart + "-" + uuidPart;
    }

    /**
     * 인메모리 Flapping 방어용 캐시 상태 레코드
     */
    private record FaultCacheState(String errorCode, LocalDateTime lastReportedAt) {
    }
}
