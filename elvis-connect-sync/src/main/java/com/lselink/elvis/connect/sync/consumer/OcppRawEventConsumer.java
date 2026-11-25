package com.lselink.elvis.connect.sync.consumer;

import com.lselink.elvis.connect.sync.domain.ErrorCategory;
import com.lselink.elvis.connect.sync.dto.OcppRawEventEnvelope;
import com.lselink.elvis.connect.sync.ocpp.OcppMessageParser;
import com.lselink.elvis.connect.sync.ocpp.OcppParsedMessage;
import com.lselink.elvis.connect.sync.service.AuthorizeService;
import com.lselink.elvis.connect.sync.service.ConnectorStatusService;
import com.lselink.elvis.connect.sync.service.DataTransferService;
import com.lselink.elvis.connect.sync.service.DltRecoveryService;
import com.lselink.elvis.connect.sync.service.MeterValuesService;
import com.lselink.elvis.connect.sync.service.OcppResponseSender;
import com.lselink.elvis.connect.sync.service.TransactionSessionService;
import com.lselink.elvis.connect.sync.util.LogFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Kafka ocpp-raw-events 토픽을 소비하여 chargeBoxId 파티션 순서를 보장하며 동기 처리하는 파이프라인 컨슈머
 *
 * <p>Sprint 17: OCPP 메시지 파서 연계</p>
 * <p>Sprint 18: StatusNotification / Heartbeat / BootNotification → MySQL Upsert 연계</p>
 * <p>Sprint 19: StartTransaction / StopTransaction → 세션 생성 및 마감 연계</p>
 * <p>Sprint 20: Authorize → DB/Redis 회원 유효성 체크 및 즉시 응답 하향 (S20-SYNC-0070-CO)</p>
 * <p>Sprint 24: DLT 격리 및 메시지 지수 백오프 재시도 파이프라인 연계 (S24-SYNC-0100-CO)</p>
 */
@Component
public class OcppRawEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OcppRawEventConsumer.class);

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 1000L;

    private final OcppMessageParser ocppMessageParser;
    private final ConnectorStatusService connectorStatusService;
    private final TransactionSessionService transactionSessionService;
    private final AuthorizeService authorizeService;
    private final MeterValuesService meterValuesService;
    private final DataTransferService dataTransferService;
    private final DltRecoveryService dltRecoveryService;
    private final OcppResponseSender ocppResponseSender;

    public OcppRawEventConsumer(OcppMessageParser ocppMessageParser,
                                ConnectorStatusService connectorStatusService,
                                TransactionSessionService transactionSessionService,
                                AuthorizeService authorizeService,
                                MeterValuesService meterValuesService,
                                DataTransferService dataTransferService,
                                DltRecoveryService dltRecoveryService,
                                OcppResponseSender ocppResponseSender) {
        this.ocppMessageParser = ocppMessageParser;
        this.connectorStatusService = connectorStatusService;
        this.transactionSessionService = transactionSessionService;
        this.authorizeService = authorizeService;
        this.meterValuesService = meterValuesService;
        this.dataTransferService = dataTransferService;
        this.dltRecoveryService = dltRecoveryService;
        this.ocppResponseSender = ocppResponseSender;
    }

    @KafkaListener(
            topics = "${elvis.sync.kafka.inbound-topic:ocpp-raw-events}",
            groupId = "${spring.kafka.consumer.group-id:elvis-connect-sync-group}"
    )
    public void consumeRawEvent(OcppRawEventEnvelope envelope) {
        if (envelope == null) {
            log.warn("[SyncConsumer] null OcppRawEventEnvelope 수신 무시");
            return;
        }
        String chargeBoxId = envelope.getChargeBoxId();
        String initialMsgId = LogFormatUtils.extractMsgId(envelope.getRawPayload());
        if (initialMsgId == null || initialMsgId.isBlank()) {
            initialMsgId = envelope.getSessionId();
        }

        try (LogFormatUtils.MdcScope ignored = LogFormatUtils.withMdc(chargeBoxId, initialMsgId)) {
            processEnvelope(envelope);
        } catch (Exception e) {
            log.error("[SyncConsumer] 패킷 처리 최종 예외: charger={} error={}",
                    chargeBoxId, e.getMessage(), e);
        }
    }

    private void processEnvelope(OcppRawEventEnvelope envelope) {
        if (envelope == null) {
            return;
        }
        String chargeBoxId = envelope.getChargeBoxId();

        // ─── 0단계: WebSocket 연결 종료/타임아웃 시스템 이벤트 처리 ───────────────────
        if (envelope.isDisconnectedEvent()) {
            log.info("[EVT] DISCONNECTED | {} | charger={} sessionId={}",
                    LogFormatUtils.padAction("Disconnected"), chargeBoxId, envelope.getSessionId());
            connectorStatusService.handleDisconnected(chargeBoxId);
            return;
        }

        String rawPayload = envelope.getRawPayload();
        int payloadLength = rawPayload != null ? rawPayload.length() : 0;

        log.debug("[SyncConsumer] 패킷 수신: charger={} len={}", chargeBoxId, payloadLength);

        // ─── 1단계: OCPP 파서 (Sprint 17) ───────────────────────────────────────────
        Optional<OcppParsedMessage> parsedOpt = ocppMessageParser.parse(chargeBoxId, rawPayload);
        if (parsedOpt.isEmpty()) {
            log.warn("[SyncConsumer] OCPP 파싱 실패 → DLT 즉시 격리: charger={}", chargeBoxId);
            dltRecoveryService.isolateFailure(
                    chargeBoxId,
                    "UNKNOWN",
                    rawPayload,
                    "PARSING_FAILED",
                    "OCPP 메시지 파싱 실패 또는 미지원 포맷",
                    ErrorCategory.NON_RETRYABLE,
                    0
            );
            return;
        }

        OcppParsedMessage msg = parsedOpt.get();
        if (msg.getUniqueId() != null && !msg.getUniqueId().isBlank()) {
            MDC.put(LogFormatUtils.MDC_MSG_ID, msg.getUniqueId());
        }

        // CALLRESULT(3) / CALLERROR(4)는 단말이 CSMS 명령에 응답한 정상 프레임이므로 DLT로 보내지 않고 정상 종료
        if (msg.isCallResultOrError()) {
            String frameType = msg.getMessageType() == 3 ? "CALLRESULT  " : "CALLERROR   ";
            log.info("[REQ] {} | {} | charger={} msgId={} payload={}",
                    frameType, LogFormatUtils.padAction(msg.getAction()),
                    chargeBoxId, msg.getUniqueId(), msg.getRaw());
            return;
        }

        // ─── 2단계: 비즈니스 디스패치 및 지수 백오프 재시도 (Sprint 24) ────────────
        dispatchActionWithRetry(msg, envelope);
    }

    /**
     * 최대 3회 지수 백오프(1s -> 2s -> 4s) 재시도 및 DLT 최종 격리
     *
     * @param msg      파싱된 OCPP 메시지
     * @param envelope 원본 수신 봉투
     */
    private void dispatchActionWithRetry(OcppParsedMessage msg, OcppRawEventEnvelope envelope) {
        String chargeBoxId = msg.getChargeBoxId();
        String action = msg.getAction();
        String rawPayload = envelope.getRawPayload();

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                dispatchAction(msg);
                return; // 정상 처리 완료 시 즉시 반환
            } catch (Exception e) {
                ErrorCategory errorCategory = ErrorCategory.classify(e);
                String errorClass = e.getClass().getSimpleName();
                String errorMessage = e.getMessage() != null ? e.getMessage() : "No detail message";

                // 1) 재시도 불가(NON_RETRYABLE) 또는 비즈니스 예외(BUSINESS_LOGIC)는 즉시 DLT 격리
                if (errorCategory != ErrorCategory.TRANSIENT) {
                    log.warn("[SyncConsumer] {} 에러 감지 → DLT 즉시 격리: chargeBoxId={}, action={}, error={}",
                            errorCategory, chargeBoxId, action, errorClass);
                    dltRecoveryService.isolateFailure(
                            chargeBoxId,
                            action,
                            rawPayload,
                            errorClass,
                            errorMessage,
                            errorCategory,
                            attempt
                    );
                    return;
                }

                // 2) 일시적 장애(DB 타임아웃, 커넥션 풀 등) 재시도 처리
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    long backoffMs = INITIAL_BACKOFF_MS * (1L << (attempt - 1)); // 1000ms -> 2000ms -> 4000ms
                    log.warn("[SyncConsumer] 일시적 장애 감지 (재시도 {}/{} 예정, 대기 {}ms): chargeBoxId={}, action={}, error={}",
                            attempt, MAX_RETRY_ATTEMPTS, backoffMs, chargeBoxId, action, errorClass);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("[SyncConsumer] 재시도 대기 인터럽트 발생: chargeBoxId={}", chargeBoxId);
                        break;
                    }
                } else {
                    // 3) 최대 재시도 초과 → DLT 최종 격리
                    log.error("[SyncConsumer] 최대 재시도({}회) 초과 → DLT 최종 격리: chargeBoxId={}, action={}, error={}",
                            MAX_RETRY_ATTEMPTS, chargeBoxId, action, errorClass, e);
                    dltRecoveryService.isolateFailure(
                            chargeBoxId,
                            action,
                            rawPayload,
                            errorClass,
                            errorMessage,
                            errorCategory,
                            MAX_RETRY_ATTEMPTS
                    );
                }
            }
        }
    }

    /**
     * 파싱된 OCPP Action 별 서비스 분기 처리
     *
     * @param msg 파싱된 OCPP 메시지
     */
    private void dispatchAction(OcppParsedMessage msg) {
        String action = msg.getAction();
        String paddedAction = LogFormatUtils.padAction(action);
        long startNanos = System.nanoTime();

        log.info("[REQ] CALL         | {} | charger={} msgId={} payload={}",
                paddedAction, msg.getChargeBoxId(), msg.getUniqueId(), msg.getRaw());

        switch (action) {
            // ─── Sprint 18: 실시간 상태 UPDATE ──────────────────────────────────
            case "StatusNotification" -> connectorStatusService.handleStatusNotification(msg);
            case "Heartbeat"          -> connectorStatusService.handleHeartbeat(msg);
            case "BootNotification"   -> connectorStatusService.handleBootNotification(msg);

            // ─── Sprint 19: 거래 세션 생성 & 종료 (S19-SYNC-0040-CO) ─────────────
            case "StartTransaction"   -> transactionSessionService.handleStartTransaction(msg);
            case "StopTransaction"    -> transactionSessionService.handleStopTransaction(msg);

            // ─── Sprint 20: Authorize 회원 인증 처리 (S20-SYNC-0070-CO) ───────────
            case "Authorize"          -> authorizeService.handleAuthorize(msg);

            // ─── 실시간 계측치(MeterValues) DB 반영 및 CALLRESULT ({}) 하향 응답 ─────
            case "MeterValues"        -> meterValuesService.handleStandardMeterValues(msg);

            // ─── LS E-Link 확장 DataTransfer 라우터 ────────────────────────────────
            case "DataTransfer"       -> dataTransferService.handleDataTransfer(msg);

            default -> {
                log.warn("[REQ] CALL         | {} | charger={} msgId={} status=NotImplemented",
                        paddedAction, msg.getChargeBoxId(), msg.getUniqueId());
                ocppResponseSender.sendNotImplementedError(msg.getChargeBoxId(), msg.getUniqueId(), action);
            }
        }

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        log.info("[RES] CALL         | {} | charger={} msgId={} elapsed={}ms",
                paddedAction, msg.getChargeBoxId(), msg.getUniqueId(), elapsedMs);
    }
}
