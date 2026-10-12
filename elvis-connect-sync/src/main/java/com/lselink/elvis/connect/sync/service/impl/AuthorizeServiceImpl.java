package com.lselink.elvis.connect.sync.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lselink.elvis.connect.sync.dto.IdTagInfo;
import com.lselink.elvis.connect.sync.dto.OcppOutboundCommand;
import com.lselink.elvis.connect.sync.ocpp.OcppParsedMessage;
import com.lselink.elvis.connect.sync.service.AuthAuditLogService;
import com.lselink.elvis.connect.sync.service.AuthorizeService;
import com.lselink.elvis.connect.sync.service.MemberAuthCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * OCPP 1.6-J Authorize 전문 처리 서비스 구현체 (Sprint 20: S20-SYNC-0070-CO)
 * <p>
 * Authorize.req 인입 시 초고속 회원 인증 판정 후 즉시 Outbound CALLRESULT를 하향 발행하고,
 * 감사 이력 적재는 비동기로 분리 수행합니다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizeServiceImpl implements AuthorizeService {

    private static final int MSG_TYPE_CALLRESULT = 3;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private final MemberAuthCacheService memberAuthCacheService;
    private final AuthAuditLogService authAuditLogService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${elvis.sync.kafka.outbound-topic:ocpp-outbound-commands}")
    private String outboundTopic;

    @Override
    public void handleAuthorize(OcppParsedMessage msg) {
        long startNano = System.nanoTime();
        String chargeBoxId = msg.getChargeBoxId();
        String uniqueId = msg.getUniqueId();
        String rawIdTag = msg.getPayload().path("idTag").asText();

        log.info("[Authorize] 회원 인증 요청 수신: chargeBoxId={}, uniqueId={}, idTag={}",
                chargeBoxId, uniqueId, rawIdTag);

        // 1. 다단계(L1 Caffeine -> L2 Redis -> L3 MySQL) 초고속 회원 유효성 판정 (< 5ms)
        IdTagInfo idTagInfo = memberAuthCacheService.validateIdTag(rawIdTag);

        // 2. OCPP 1.6-J CALLRESULT JSON 페이로드 빌드
        String callResultJson = buildCallResultPayload(uniqueId, idTagInfo);

        // 3. Kafka 하향 명령 토픽으로 즉시 발행 (논블로킹)
        OcppOutboundCommand outboundCmd = OcppOutboundCommand.builder()
                .chargeBoxId(chargeBoxId)
                .action("Authorize")
                .commandPayload(callResultJson)
                .build();

        kafkaTemplate.send(outboundTopic, chargeBoxId, outboundCmd)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Authorize] CALLRESULT 하향 발행 실패: chargeBoxId={}, err={}",
                                chargeBoxId, ex.getMessage(), ex);
                    } else {
                        log.debug("[Authorize] CALLRESULT 하향 발행 완료: chargeBoxId={}, offset={}",
                                chargeBoxId, result.getRecordMetadata().offset());
                    }
                });

        // 4. 감사 이력 저장은 Fire-and-Forget 비동기 위임 (메인 응답 지연 0ms)
        long elapsedNano = System.nanoTime() - startNano;
        authAuditLogService.logAuthAsync(chargeBoxId, uniqueId, rawIdTag, idTagInfo, elapsedNano);

        log.info("[Authorize] 인증 처리 완료: chargeBoxId={}, status={}, latency={:.2f}ms, tier={}",
                chargeBoxId, idTagInfo.getStatus(), elapsedNano / 1_000_000.0, idTagInfo.getCacheTier());
    }

    /**
     * OCPP 1.6-J CALLRESULT [3, uniqueId, {"idTagInfo": {...}}] JSON 직렬화
     */
    private String buildCallResultPayload(String uniqueId, IdTagInfo idTagInfo) {
        try {
            ArrayNode root = objectMapper.createArrayNode();
            root.add(MSG_TYPE_CALLRESULT);
            root.add(uniqueId);

            ObjectNode payload = objectMapper.createObjectNode();
            ObjectNode tagInfoNode = payload.putObject("idTagInfo");
            tagInfoNode.put("status", idTagInfo.getStatus().name());

            if (idTagInfo.getExpiryDate() != null) {
                tagInfoNode.put("expiryDate", idTagInfo.getExpiryDate().format(ISO_FORMATTER));
            }
            if (idTagInfo.getParentIdTag() != null && !idTagInfo.getParentIdTag().isBlank()) {
                tagInfoNode.put("parentIdTag", idTagInfo.getParentIdTag());
            }

            root.add(payload);
            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            log.error("[Authorize] CALLRESULT 페이로드 직렬화 오류: uniqueId={}, err={}", uniqueId, e.getMessage(), e);
            // 에러 시 최소 규격 Invalid 응답 생성
            return String.format("[3,\"%s\",{\"idTagInfo\":{\"status\":\"Invalid\"}}]", uniqueId);
        }
    }
}
