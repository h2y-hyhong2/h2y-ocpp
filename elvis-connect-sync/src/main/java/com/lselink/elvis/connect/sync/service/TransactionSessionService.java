package com.lselink.elvis.connect.sync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lselink.elvis.connect.sync.domain.AuthorizationStatus;
import com.lselink.elvis.connect.sync.domain.SessionStatus;
import com.lselink.elvis.connect.sync.domain.StopReason;
import com.lselink.elvis.connect.sync.domain.TransactionSession;
import com.lselink.elvis.connect.sync.dto.IdTagInfo;
import com.lselink.elvis.connect.sync.ocpp.OcppParsedMessage;
import com.lselink.elvis.connect.sync.repository.ConnectorStatusRepository;
import com.lselink.elvis.connect.sync.repository.TransactionSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 충전 거래 세션 생성 및 종료 비즈니스 서비스 (Sprint 19: S19-SYNC-0040-CO)
 * <p>
 * OCPP StartTransaction / StopTransaction 수신 시 세션 생명주기 관리,
 * 서버 측 전역 시퀀스 transactionId 채번, idTag 유효성 검증, 멱등성 및 커넥터 상태 연계를 수행합니다.
 * </p>
 */
@Service
public class TransactionSessionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionSessionService.class);
    private static final List<SessionStatus> ACTIVE_STATUSES = List.of(SessionStatus.STARTED, SessionStatus.CHARGING);

    private final TransactionSessionRepository sessionRepository;
    private final ConnectorStatusRepository connectorStatusRepository;
    private final TouRatingService touRatingService;
    private final TransactionSequenceService sequenceService;
    private final MemberAuthCacheService memberAuthCacheService;
    private final OcppResponseSender ocppResponseSender;

    public TransactionSessionService(TransactionSessionRepository sessionRepository,
                                     ConnectorStatusRepository connectorStatusRepository,
                                     TouRatingService touRatingService,
                                     TransactionSequenceService sequenceService,
                                     MemberAuthCacheService memberAuthCacheService,
                                     OcppResponseSender ocppResponseSender) {
        this.sessionRepository = sessionRepository;
        this.connectorStatusRepository = connectorStatusRepository;
        this.touRatingService = touRatingService;
        this.sequenceService = sequenceService;
        this.memberAuthCacheService = memberAuthCacheService;
        this.ocppResponseSender = ocppResponseSender;
    }

    /**
     * StartTransaction 이벤트 처리
     * <pre>
     * payload 예시:
     * {
     *   "connectorId": 1,
     *   "idTag": "RFID-192840",
     *   "meterStart": 105200,
     *   "timestamp": "2026-10-06T10:00:00Z",
     *   "reservationId": 0
     * }
     * </pre>
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void handleStartTransaction(OcppParsedMessage msg) {
        String chargeBoxId = msg.getChargeBoxId();
        String uniqueId = msg.getUniqueId();
        JsonNode payload = msg.getPayload();

        try {
            int connectorId = payload.path("connectorId").asInt(1);
            String idTag = extractNonBlankText(payload, "idTag", "");
            long meterStart = payload.path("meterStart").asLong(0L);
            LocalDateTime startTime = parseTimestamp(extractNonBlankText(payload, "timestamp", null));

            // 1. 회원 태그 유효성 검증 (L1 -> L2 -> L3 캐시 조회)
            IdTagInfo idTagInfo = memberAuthCacheService.validateIdTag(idTag);
            AuthorizationStatus authStatus = idTagInfo.getStatus() != null
                    ? idTagInfo.getStatus()
                    : AuthorizationStatus.Invalid;
            String idTagStatus = authStatus.name();
            String safeIdTag = idTag.isBlank() ? "UNKNOWN" : idTag;

            // 2. 멱등성 검증: 동일 커넥터에 동일 (idTag, meterStart, startTime) 활성 세션 존재 시 기존 transactionId 재회신
            Optional<TransactionSession> existingActiveOpt = sessionRepository
                    .findLatestActiveSession(chargeBoxId, connectorId, ACTIVE_STATUSES);
            if (existingActiveOpt.isPresent()) {
                TransactionSession existing = existingActiveOpt.get();
                if (safeIdTag.equals(existing.getIdTag())
                        && meterStart == existing.getMeterStart()
                        && startTime.equals(existing.getStartTime())) {
                    log.warn("[SessionSvc] 중복 StartTransaction 인입 무시 및 기존 transactionId 재회신: chargeBoxId={}, connId={}, txId={}",
                            chargeBoxId, connectorId, existing.getTransactionId());
                    ocppResponseSender.sendStartTransactionResponse(
                            chargeBoxId, uniqueId, existing.getTransactionId(), idTagStatus);
                    return;
                }
            }

            // 3. CSMS 서버 측 전역 단일 시퀀스로 transactionId 항상 신규 채번 (AGENTS.md §4)
            long transactionId = sequenceService.nextTransactionId();

            log.info("[SessionSvc] StartTransaction 수신: chargeBoxId={}, connId={}, txId={}, tag={}, authStatus={}, meterStart={}",
                    chargeBoxId, connectorId, transactionId, safeIdTag, idTagStatus, meterStart);

            boolean accepted = authStatus.isAccepted();

            // 4. TransactionSession 엔티티 생성 및 영속화
            TransactionSession session = TransactionSession.builder()
                    .chargeBoxId(chargeBoxId)
                    .connectorId(connectorId)
                    .transactionId(transactionId)
                    .idTag(safeIdTag)
                    .parentIdTag(idTagInfo.getParentIdTag())
                    .status(accepted ? SessionStatus.STARTED : SessionStatus.ABORTED)
                    .meterStart(meterStart)
                    .meterStop(accepted ? null : meterStart)
                    .totalEnergyKwh(BigDecimal.ZERO.setScale(3))
                    .startTime(startTime)
                    .stopTime(accepted ? null : startTime)
                    .stopReason(accepted ? null : StopReason.DeAuthorized)
                    .chargingMinutes(0)
                    .version(0)
                    .build();

            try {
                sessionRepository.save(session);
                log.info("[SessionSvc] 충전 세션 영속화 완료: id={}, chargeBoxId={}, txId={}, status={}",
                        session.getId(), chargeBoxId, transactionId, session.getStatus());
            } catch (DataIntegrityViolationException e) {
                log.warn("[SessionSvc] DB 유니크 제약 위반으로 중복 세션 인입 안전 방어: chargeBoxId={}, txId={}",
                        chargeBoxId, transactionId);
                ocppResponseSender.sendStartTransactionResponse(chargeBoxId, uniqueId, transactionId, idTagStatus);
                return;
            }

            // 5. 인증 수락된 경우에만 커넥터 실시간 상태 (connector_status) Charging 동기화
            if (accepted) {
                connectorStatusRepository.upsertStatus(
                        chargeBoxId,
                        connectorId,
                        "Charging",
                        "NoError",
                        null,
                        "SessionStarted(txId=" + transactionId + ")",
                        startTime
                );
                log.info("[SessionSvc] 커넥터 상태 연동 완료: chargeBoxId={}, connId={}, status=Charging",
                        chargeBoxId, connectorId);
            } else {
                log.warn("[SessionSvc] StartTransaction 인증 거부로 Charging 전환 생략: chargeBoxId={}, connId={}, tag={}, status={}",
                        chargeBoxId, connectorId, safeIdTag, idTagStatus);
            }

            // 6. OCPP 1.6-J CALLRESULT 하향 응답 회신 ({idTagInfo: {status}, transactionId})
            ocppResponseSender.sendStartTransactionResponse(chargeBoxId, uniqueId, transactionId, idTagStatus);

        } catch (Exception e) {
            log.error("[SessionSvc] StartTransaction 처리 실패: chargeBoxId={}, error={}",
                    chargeBoxId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * StopTransaction 이벤트 처리
     * <pre>
     * payload 예시:
     * {
     *   "transactionId": 98120,
     *   "meterStop": 173600,
     *   "timestamp": "2026-10-06T10:45:00Z",
     *   "idTag": "RFID-192840",
     *   "reason": "Local"
     * }
     * </pre>
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void handleStopTransaction(OcppParsedMessage msg) {
        String chargeBoxId = msg.getChargeBoxId();
        String uniqueId = msg.getUniqueId();
        JsonNode payload = msg.getPayload();

        try {
            long transactionId = payload.hasNonNull("transactionId") ? payload.get("transactionId").asLong(0L) : 0L;
            if (transactionId <= 0L) {
                throw new IllegalArgumentException("StopTransaction missing valid transactionId: chargeBoxId=" + chargeBoxId);
            }

            long meterStop = payload.path("meterStop").asLong(0L);
            LocalDateTime stopTime = parseTimestamp(extractNonBlankText(payload, "timestamp", null));
            String reasonStr = extractNonBlankText(payload, "reason", null);
            StopReason reason = StopReason.fromValue(reasonStr);

            String idTag = extractNonBlankText(payload, "idTag", null);
            String responseIdTagStatus = "Accepted";
            if (idTag != null) {
                IdTagInfo idTagInfo = memberAuthCacheService.validateIdTag(idTag);
                if (idTagInfo.getStatus() != null) {
                    responseIdTagStatus = idTagInfo.getStatus().name();
                }
            }

            log.info("[SessionSvc] StopTransaction 수신: chargeBoxId={}, txId={}, meterStop={}, reason={}",
                    chargeBoxId, transactionId, meterStop, reason);

            // 1. 기존 세션 조회
            TransactionSession session = sessionRepository
                    .findByChargeBoxIdAndTransactionId(chargeBoxId, transactionId)
                    .orElse(null);

            // Out-of-Order 대응: StartTransaction이 유실되었거나 지연 도래한 경우
            if (session == null) {
                log.warn("[SessionSvc] 선행 세션 미발견 (Orphan StopTransaction 인입): chargeBoxId={}, txId={}",
                        chargeBoxId, transactionId);

                int fallbackConnectorId = payload.hasNonNull("connectorId")
                        ? payload.get("connectorId").asInt(1)
                        : 1;

                // 고아 데이터 방지를 위한 즉시 마감 세션 생성
                session = TransactionSession.builder()
                        .chargeBoxId(chargeBoxId)
                        .connectorId(fallbackConnectorId)
                        .transactionId(transactionId)
                        .idTag(idTag != null ? idTag : "UNKNOWN")
                        .status(SessionStatus.ABORTED)
                        .meterStart(meterStop)
                        .meterStop(meterStop)
                        .totalEnergyKwh(BigDecimal.ZERO.setScale(3))
                        .startTime(stopTime)
                        .stopTime(stopTime)
                        .stopReason(reason)
                        .chargingMinutes(0)
                        .version(0)
                        .build();

                try {
                    sessionRepository.save(session);
                } catch (DataIntegrityViolationException e) {
                    log.warn("[SessionSvc] 고아 세션 동시 저장 충돌 방어: chargeBoxId={}, txId={}", chargeBoxId, transactionId);
                    ocppResponseSender.sendStopTransactionResponse(chargeBoxId, uniqueId, responseIdTagStatus);
                    return;
                }

                // 고아 세션에서도 커넥터 실시간 상태 (connector_status) Available 복구
                connectorStatusRepository.upsertStatus(
                        chargeBoxId,
                        fallbackConnectorId,
                        "Available",
                        "NoError",
                        null,
                        "OrphanSessionStopped(txId=" + transactionId + ")",
                        stopTime
                );

                // 고아 세션 CDR 원장 생성 (독립 트랜잭션 REQUIRES_NEW)
                try {
                    touRatingService.calculateAndSaveCdr(session, meterStop, stopTime);
                } catch (Exception e) {
                    log.error("[SessionSvc] 고아 세션 CDR 생성 실패: txId={}, error={}", transactionId, e.getMessage());
                }

                // 고아 세션도 단말에 StopTransaction.conf 회신
                ocppResponseSender.sendStopTransactionResponse(chargeBoxId, uniqueId, responseIdTagStatus);
                return;
            }

            // 2. 멱등성 검증: 이미 종료된 세션에 대한 중복 StopTransaction 패킷 수신 시 안전 반환 및 응답 재회신
            if (session.getStatus().isTerminated()) {
                log.warn("[SessionSvc] 이미 종료된 세션 (StopTransaction 중복 인입 무시 및 응답 재회신): chargeBoxId={}, txId={}, status={}",
                        chargeBoxId, transactionId, session.getStatus());
                ocppResponseSender.sendStopTransactionResponse(chargeBoxId, uniqueId, responseIdTagStatus);
                return;
            }

            // 3. 세션 마감 비즈니스 로직 수행 (전력량 kWh 계산, 충전 경과시간 산출, 상태 전이)
            session.stopSession(meterStop, stopTime, reason);
            sessionRepository.save(session);

            log.info("[SessionSvc] 세션 종료 처리 완료: txId={}, totalKwh={}, minutes={}, status={}",
                    transactionId, session.getTotalEnergyKwh(), session.getChargingMinutes(), session.getStatus());

            // 4. 커넥터 실시간 상태 (connector_status) Available 초기화
            connectorStatusRepository.upsertStatus(
                    chargeBoxId,
                    session.getConnectorId(),
                    "Available",
                    "NoError",
                    null,
                    "SessionStopped(txId=" + transactionId + ")",
                    stopTime
            );
            log.info("[SessionSvc] 커넥터 상태 복구 완료: chargeBoxId={}, connId={}, status=Available",
                    chargeBoxId, session.getConnectorId());

            // 5. TOU 시간대별 차등 요금 계산 및 독립 CDR 과금 원장(charge_detail_record) 영속화 (REQUIRES_NEW)
            try {
                touRatingService.calculateAndSaveCdr(session, meterStop, stopTime);
            } catch (Exception e) {
                log.error("[SessionSvc] CDR 과금 원장 생성 중 예외 발생 (세션 마감은 유지): txId={}, error={}",
                        transactionId, e.getMessage(), e);
            }

            // 6. OCPP 1.6-J CALLRESULT 하향 응답 회신 ({idTagInfo: {status}})
            ocppResponseSender.sendStopTransactionResponse(chargeBoxId, uniqueId, responseIdTagStatus);

        } catch (Exception e) {
            log.error("[SessionSvc] StopTransaction 처리 실패: chargeBoxId={}, error={}",
                    chargeBoxId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * ISO-8601 타임스탬프 파싱 (실패 시 현재 시각 반환)
     */
    private LocalDateTime parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(timestampStr.trim()), ZoneId.systemDefault());
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(timestampStr.trim(), DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception ex) {
                log.debug("[SessionSvc] 일시 파싱 실패 (현재 시각 대체): input={}", timestampStr);
                return LocalDateTime.now();
            }
        }
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
}
