package com.lselink.elvis.connect.sync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lselink.elvis.connect.sync.domain.SessionStatus;
import com.lselink.elvis.connect.sync.domain.StopReason;
import com.lselink.elvis.connect.sync.domain.TransactionSession;
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

/**
 * 충전 거래 세션 생성 및 종료 비즈니스 서비스 (Sprint 19: S19-SYNC-0040-CO)
 * <p>
 * OCPP StartTransaction / StopTransaction 수신 시 세션 생명주기 관리,
 * 멱등성 및 원자적 상태 전이, 커넥터 실시간 상태 연계를 수행합니다.
 * </p>
 */
@Service
public class TransactionSessionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionSessionService.class);

    private final TransactionSessionRepository sessionRepository;
    private final ConnectorStatusRepository connectorStatusRepository;
    private final TouRatingService touRatingService;
    private final TransactionSequenceService sequenceService;
    private final OcppResponseSender ocppResponseSender;

    public TransactionSessionService(TransactionSessionRepository sessionRepository,
                                     ConnectorStatusRepository connectorStatusRepository,
                                     TouRatingService touRatingService,
                                     TransactionSequenceService sequenceService,
                                     OcppResponseSender ocppResponseSender) {
        this.sessionRepository = sessionRepository;
        this.connectorStatusRepository = connectorStatusRepository;
        this.touRatingService = touRatingService;
        this.sequenceService = sequenceService;
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
            String idTag = payload.path("idTag").asText("UNKNOWN");
            long meterStart = payload.path("meterStart").asLong(0L);
            LocalDateTime startTime = parseTimestamp(payload.path("timestamp").asText(null));

            // transactionId 추출 (단말 전달 또는 CSMS 전역 시퀀스 신규 채번)
            long transactionId = extractTransactionId(payload);

            log.info("[SessionSvc] StartTransaction 수신: chargeBoxId={}, connId={}, txId={}, tag={}, meterStart={}",
                    chargeBoxId, connectorId, transactionId, idTag, meterStart);

            // 1. 멱등성 검증: 동일 (chargeBoxId, transactionId) 세션 기등록 여부 확인
            if (sessionRepository.existsByChargeBoxIdAndTransactionId(chargeBoxId, transactionId)) {
                log.warn("[SessionSvc] 이미 존재하는 세션 (StartTransaction 중복 인입 무시 및 응답 재회신): chargeBoxId={}, txId={}",
                        chargeBoxId, transactionId);
                ocppResponseSender.sendStartTransactionResponse(chargeBoxId, uniqueId, transactionId, "Accepted");
                return;
            }

            // 2. 단일 TransactionSession 엔티티 생성 및 영속화
            TransactionSession session = TransactionSession.builder()
                    .chargeBoxId(chargeBoxId)
                    .connectorId(connectorId)
                    .transactionId(transactionId)
                    .idTag(idTag)
                    .status(SessionStatus.STARTED)
                    .meterStart(meterStart)
                    .totalEnergyKwh(BigDecimal.ZERO.setScale(3))
                    .startTime(startTime)
                    .chargingMinutes(0)
                    .version(0)
                    .build();

            try {
                sessionRepository.save(session);
                log.info("[SessionSvc] 신규 충전 세션 영속화 완료: id={}, chargeBoxId={}, txId={}",
                        session.getId(), chargeBoxId, transactionId);
            } catch (DataIntegrityViolationException e) {
                // 동시 인입 시 복합 유니크 제약(UK_SESSION_CHARGE_BOX_TRN)으로 중복 방어
                log.warn("[SessionSvc] DB 유니크 제약 위반으로 중복 세션 인입 안전 방어: chargeBoxId={}, txId={}",
                        chargeBoxId, transactionId);
                ocppResponseSender.sendStartTransactionResponse(chargeBoxId, uniqueId, transactionId, "Accepted");
                return;
            }

            // 3. 커넥터 실시간 상태 (tb_connector_status) Charging 동기화
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

            // 4. OCPP 1.6-J CALLRESULT 하향 응답 회신 ({idTagInfo: {status: "Accepted"}, transactionId})
            ocppResponseSender.sendStartTransactionResponse(chargeBoxId, uniqueId, transactionId, "Accepted");

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
            long transactionId = payload.path("transactionId").asLong(0L);
            long meterStop = payload.path("meterStop").asLong(0L);
            LocalDateTime stopTime = parseTimestamp(payload.path("timestamp").asText(null));
            String reasonStr = payload.path("reason").asText(null);
            StopReason reason = StopReason.fromValue(reasonStr);

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

                // 고아 데이터 방지를 위한 즉시 마감 세션 생성
                session = TransactionSession.builder()
                        .chargeBoxId(chargeBoxId)
                        .connectorId(1)
                        .transactionId(transactionId)
                        .idTag(payload.path("idTag").asText("UNKNOWN"))
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

                sessionRepository.save(session);
                
                // 고아 세션 CDR 원장 생성
                try {
                    touRatingService.calculateAndSaveCdr(session, meterStop, stopTime);
                } catch (Exception e) {
                    log.error("[SessionSvc] 고아 세션 CDR 생성 실패: txId={}, error={}", transactionId, e.getMessage());
                }

                // 고아 세션도 단말에 StopTransaction.conf 회신
                ocppResponseSender.sendStopTransactionResponse(chargeBoxId, uniqueId, "Accepted");
                return;
            }

            // 2. 멱등성 검증: 이미 종료된 세션에 대한 중복 StopTransaction 패킷 수신 시 안전 반환 및 응답 재회신
            if (session.getStatus().isTerminated()) {
                log.warn("[SessionSvc] 이미 종료된 세션 (StopTransaction 중복 인입 무시 및 응답 재회신): chargeBoxId={}, txId={}, status={}",
                        chargeBoxId, transactionId, session.getStatus());
                ocppResponseSender.sendStopTransactionResponse(chargeBoxId, uniqueId, "Accepted");
                return;
            }

            // 3. 세션 마감 비즈니스 로직 수행 (전력량 kWh 계산, 충전 경과시간 산출, 상태 전이)
            session.stopSession(meterStop, stopTime, reason);
            sessionRepository.save(session);

            log.info("[SessionSvc] 세션 종료 처리 완료: txId={}, totalKwh={}, minutes={}, status={}",
                    transactionId, session.getTotalEnergyKwh(), session.getChargingMinutes(), session.getStatus());

            // 4. 커넥터 실시간 상태 (tb_connector_status) Available 초기화
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

            // 5. TOU 시간대별 차등 요금 계산 및 독립 CDR 과금 원장(tb_charge_detail_record) 영속화 (S21-SYNC-0080-CO)
            try {
                touRatingService.calculateAndSaveCdr(session, meterStop, stopTime);
            } catch (Exception e) {
                log.error("[SessionSvc] CDR 과금 원장 생성 중 예외 발생 (세션 마감은 유지): txId={}, error={}",
                        transactionId, e.getMessage(), e);
            }

            // 6. OCPP 1.6-J CALLRESULT 하향 응답 회신 ({idTagInfo: {status: "Accepted"}})
            ocppResponseSender.sendStopTransactionResponse(chargeBoxId, uniqueId, "Accepted");

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

    /**
     * StartTransaction payload 또는 CSMS 전역 시퀀스로부터 transactionId 채번
     */
    private long extractTransactionId(JsonNode payload) {
        if (payload.hasNonNull("transactionId")) {
            long candidate = payload.get("transactionId").asLong();
            if (candidate > 0L) {
                return candidate;
            }
        }
        return sequenceService.nextTransactionId();
    }
}
