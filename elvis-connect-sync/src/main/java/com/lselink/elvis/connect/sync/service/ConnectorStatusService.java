package com.lselink.elvis.connect.sync.service;

import com.lselink.elvis.connect.sync.ocpp.OcppParsedMessage;
import com.lselink.elvis.connect.sync.repository.ConnectorStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 충전기 실시간 상태 UPDATE 서비스 (Sprint 18: S18-SYNC-0030-CO)
 * <p>
 * OCPP StatusNotification / Heartbeat 수신 시 MySQL tb_connector_status 테이블을
 * ON DUPLICATE KEY UPDATE 방식으로 단일 문장 Upsert 처리.
 * </p>
 *
 * <ul>
 *   <li>StatusNotification → 상태(status), 에러코드(error_code) 갱신</li>
 *   <li>Heartbeat → heartbeat_at 갱신 (커넥터 0번 레코드)</li>
 * </ul>
 */
@Service
public class ConnectorStatusService {

    private static final Logger log = LoggerFactory.getLogger(ConnectorStatusService.class);

    private final ConnectorStatusRepository repository;
    private final FaultDetectionService faultDetectionService;

    public ConnectorStatusService(ConnectorStatusRepository repository,
                                  FaultDetectionService faultDetectionService) {
        this.repository = repository;
        this.faultDetectionService = faultDetectionService;
    }

    /**
     * OCPP StatusNotification 처리
     * <pre>
     * payload 예시:
     * {
     *   "connectorId": 1,
     *   "errorCode": "NoError",
     *   "status": "Available",
     *   "timestamp": "2026-10-01T10:00:00Z",
     *   "vendorId": null,
     *   "info": null
     * }
     * </pre>
     */
    @Transactional
    public void handleStatusNotification(OcppParsedMessage msg) {
        String chargeBoxId = msg.getChargeBoxId();

        try {
            int    connectorId = msg.getPayload().path("connectorId").asInt(0);
            String status      = msg.getPayload().path("status").asText("Unknown");
            String errorCode   = msg.getPayload().path("errorCode").asText(null);
            String vendorId    = nullIfEmpty(msg.getPayload().path("vendorId").asText(null));
            String info        = nullIfEmpty(msg.getPayload().path("info").asText(null));

            // 1. 실시간 상태 테이블 Upsert (Sprint 18)
            repository.upsertStatus(chargeBoxId, connectorId, status, errorCode, vendorId, info, LocalDateTime.now());

            log.info("[StatusSvc] StatusNotification Upsert 완료: chargeBoxId={}, connectorId={}, status={}, errorCode={}",
                    chargeBoxId, connectorId, status, errorCode);

            // 2. 장애 감지 및 Kafka UI 알림 발행 연계 (Sprint 23: S23-SYNC-0060-CO)
            faultDetectionService.processFaultEvent(msg);

        } catch (Exception e) {
            log.error("[StatusSvc] StatusNotification 처리 실패: chargeBoxId={}, error={}", chargeBoxId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * OCPP Heartbeat 처리 - heartbeat_at 갱신
     * <pre>payload: {}</pre>
     */
    @Transactional
    public void handleHeartbeat(OcppParsedMessage msg) {
        String chargeBoxId = msg.getChargeBoxId();

        try {
            repository.upsertHeartbeat(chargeBoxId, LocalDateTime.now());

            log.info("[StatusSvc] Heartbeat Upsert 완료: chargeBoxId={}, heartbeatAt={}", chargeBoxId, LocalDateTime.now());

        } catch (Exception e) {
            log.error("[StatusSvc] Heartbeat 처리 실패: chargeBoxId={}, error={}", chargeBoxId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * BootNotification 처리 - 기기 재기동 시 상태 초기화
     * <pre>
     * payload 예시:
     * {
     *   "chargePointModel": "EVStation",
     *   "chargePointVendor": "LSELINK",
     *   "firmwareVersion": "1.0.0"
     * }
     * </pre>
     */
    @Transactional
    public void handleBootNotification(OcppParsedMessage msg) {
        String chargeBoxId = msg.getChargeBoxId();

        try {
            // BootNotification: 커넥터 0번 상태를 Available로 초기화
            repository.upsertStatus(chargeBoxId, 0, "Available", "NoError", null, "Booted", LocalDateTime.now());

            log.info("[StatusSvc] BootNotification 초기화 완료: chargeBoxId={}", chargeBoxId);

        } catch (Exception e) {
            log.error("[StatusSvc] BootNotification 처리 실패: chargeBoxId={}, error={}", chargeBoxId, e.getMessage(), e);
            throw e;
        }
    }

    private String nullIfEmpty(String value) {
        return (value == null || value.isBlank() || value.equalsIgnoreCase("null")) ? null : value;
    }
}
