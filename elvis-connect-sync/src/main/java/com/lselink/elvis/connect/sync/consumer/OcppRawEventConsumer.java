package com.lselink.elvis.connect.sync.consumer;

import com.lselink.elvis.connect.sync.dto.OcppRawEventEnvelope;
import com.lselink.elvis.connect.sync.ocpp.OcppMessageParser;
import com.lselink.elvis.connect.sync.ocpp.OcppParsedMessage;
import com.lselink.elvis.connect.sync.service.AuthorizeService;
import com.lselink.elvis.connect.sync.service.ConnectorStatusService;
import com.lselink.elvis.connect.sync.service.TransactionSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * Kafka ocpp-raw-events 토픽을 소비하여 Virtual Threads로 비동기 파싱 및 전처리 파이프라인 수행
 *
 * <p>Sprint 17: OCPP 메시지 파서 연계</p>
 * <p>Sprint 18: StatusNotification / Heartbeat / BootNotification → MySQL Upsert 연계</p>
 * <p>Sprint 19: StartTransaction / StopTransaction → 세션 생성 및 마감 연계</p>
 * <p>Sprint 20: Authorize → DB/Redis 회원 유효성 체크 및 즉시 응답 하향 (S20-SYNC-0070-CO)</p>
 */
@Component
public class OcppRawEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OcppRawEventConsumer.class);

    private final Executor virtualThreadExecutor;
    private final OcppMessageParser ocppMessageParser;
    private final ConnectorStatusService connectorStatusService;
    private final TransactionSessionService transactionSessionService;
    private final AuthorizeService authorizeService;

    public OcppRawEventConsumer(Executor virtualThreadExecutor,
                               OcppMessageParser ocppMessageParser,
                               ConnectorStatusService connectorStatusService,
                               TransactionSessionService transactionSessionService,
                               AuthorizeService authorizeService) {
        this.virtualThreadExecutor = virtualThreadExecutor;
        this.ocppMessageParser = ocppMessageParser;
        this.connectorStatusService = connectorStatusService;
        this.transactionSessionService = transactionSessionService;
        this.authorizeService = authorizeService;
    }

    @KafkaListener(
            topics = "${elvis.sync.kafka.inbound-topic:ocpp-raw-events}",
            groupId = "${spring.kafka.consumer.group-id:elvis-connect-sync-group}"
    )
    public void consumeRawEvent(OcppRawEventEnvelope envelope) {
        // Java 25 가상 스레드로 작업 비동기 위임 (I/O 병목 및 블로킹 분리)
        virtualThreadExecutor.execute(() -> {
            try {
                processEnvelope(envelope);
            } catch (Exception e) {
                log.error("[SyncConsumer] 패킷 처리 실패: chargeBoxId={}, error={}",
                        envelope.getChargeBoxId(), e.getMessage(), e);
            }
        });
    }

    private void processEnvelope(OcppRawEventEnvelope envelope) {
        log.debug("[SyncConsumer] 가상스레드[{}] 패킷 수신: chargeBoxId={}, len={}",
                Thread.currentThread().getName(), envelope.getChargeBoxId(),
                envelope.getRawPayload() != null ? envelope.getRawPayload().length() : 0);

        // ─── OCPP 파서 (Sprint 17) ───────────────────────────────────────────
        ocppMessageParser.parse(envelope.getChargeBoxId(), envelope.getRawPayload())
                .ifPresent(this::dispatchAction);
    }

    /**
     * 파싱된 OCPP Action 별 서비스 분기 처리
     *
     * @param msg 파싱된 OCPP 메시지
     */
    private void dispatchAction(OcppParsedMessage msg) {
        String action = msg.getAction();

        log.info("[SyncConsumer] OCPP Action 수신: chargeBoxId={}, action={}", msg.getChargeBoxId(), action);

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

            // ─── Sprint 21~22 이후 연계 예정 ─────────────────────────────────────
            case "MeterValues" ->
                log.debug("[SyncConsumer] 미구현 Action 수신 (향후 Sprint 처리 예정): action={}", action);

            default ->
                log.trace("[SyncConsumer] 무관 Action 무시: chargeBoxId={}, action={}", msg.getChargeBoxId(), action);
        }
    }
}
