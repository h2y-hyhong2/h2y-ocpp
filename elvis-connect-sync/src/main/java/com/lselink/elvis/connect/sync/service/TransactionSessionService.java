package com.lselink.elvis.connect.sync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lselink.elvis.connect.sync.domain.SessionStatus;
import com.lselink.elvis.connect.sync.domain.StopReason;
import com.lselink.elvis.connect.sync.domain.TransactionSession;
import com.lselink.elvis.connect.sync.ocpp.OcppParsedMessage;
import com.lselink.elvis.connect.sync.repository.ConnectorStatusRepository;
import com.lselink.elvis.connect.sync.repository.TransactionSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionSessionService {

    private final TransactionSessionRepository sessionRepository;
    private final ConnectorStatusRepository connectorStatusRepository;

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
        JsonNode payload = msg.getPayload();

        try {
            int connectorId = payload.path("connectorId").asInt(1);
            String idTag = payload.path("idTag").asText("UNKNOWN");
            long meterStart = payload.path("meterStart").asLong(0L);
            LocalDateTime startTime = parseTimestamp(payload.path("timestamp").asText(null));

            // transactionId 추출 (단말이 전달하거나 응답용 사전채번 ID 매핑)
            long transactionId = extractTransactionId(payload, msg.getUniqueId());

            log.info("[SessionSvc] StartTransaction 수신: chargeBoxId={}, connId={}, txId={}, tag={}, meterStart={}",
                    chargeBoxId, connectorId, transactionId, idTag, meterStart);

            // 1. 멱등성 검증: 동일 (chargeBoxId, transactionId) 세션 기등록 여부 확인
            if (sessionRepository.existsByChargeBoxIdAndTransactionId(chargeBoxId, transactionId)) {
                log.warn("[SessionSvc] 이미 존재하는 세션 (StartTransaction 중복 인입 무시): chargeBoxId={}, txId={}",
                        chargeBoxId, transactionId);
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
                return;
            }

            // 2. 멱등성 검증: 이미 종료된 세션에 대한 중복 StopTransaction 패킷 수신 시 안전 반환
            if (session.getStatus().isTerminated()) {
                log.warn("[SessionSvc] 이미 종료된 세션 (StopTransaction 중복 인입 무시): chargeBoxId={}, txId={}, status={}",
                        chargeBoxId, transactionId, session.getStatus());
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
     * StartTransaction payload 또는 메시지 컨텍스트로부터 transactionId 채번
     */
    private long extractTransactionId(JsonNode payload, String uniqueId) {
        if (payload.hasNonNull("transactionId")) {
            return payload.get("transactionId").asLong();
        }
        // payload에 없을 경우 고유 해시 기반 양의 정수 채번
        if (uniqueId != null && !uniqueId.isBlank()) {
            return Math.abs((long) uniqueId.hashCode());
        }
        return System.currentTimeMillis();
    }
}
