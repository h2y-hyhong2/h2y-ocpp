package com.lselink.elvis.connect.sync.service.impl;

import com.lselink.elvis.connect.sync.domain.AuthHist;
import com.lselink.elvis.connect.sync.dto.IdTagInfo;
import com.lselink.elvis.connect.sync.repository.AuthHistRepository;
import com.lselink.elvis.connect.sync.service.AuthAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.Executor;

/**
 * Authorize 인증 감사 이력 비동기 기록 서비스 구현체 (Sprint 20: S20-SYNC-0070-CO)
 * <p>
 * 메인 인증 응답 파이프라인에 추가 레이턴시를 유발하지 않도록(0ms 영향),
 * Virtual Thread를 통해 완전히 분리된 Fire-and-Forget 방식으로 DB에 영속화합니다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAuditLogServiceImpl implements AuthAuditLogService {

    private final Executor virtualThreadExecutor;
    private final AuthHistRepository authHistRepository;

    @Override
    public void logAuthAsync(String chargeBoxId, String messageId, String idTag, IdTagInfo idTagInfo, long elapsedNano) {
        // Java 25 가상 스레드로 완전 비동기 위임
        virtualThreadExecutor.execute(() -> {
            try {
                double latencyMs = elapsedNano / 1_000_000.0;
                String cacheTier = idTagInfo.getCacheTier() != null ? idTagInfo.getCacheTier() : "UNKNOWN";

                AuthHist hist = AuthHist.builder()
                        .chargeBoxId(chargeBoxId)
                        .messageId(messageId)
                        .idTag(idTag)
                        .authStatus(idTagInfo.getStatus())
                        .parentIdTag(idTagInfo.getParentIdTag())
                        .failReason(idTagInfo.getStatus().isAccepted() ? null : idTagInfo.getStatus().getDescription())
                        .cacheTier(cacheTier)
                        .latencyMs(BigDecimal.valueOf(latencyMs).setScale(2, RoundingMode.HALF_UP))
                        .build();

                authHistRepository.save(hist);

                log.debug("[AuthAudit] 감사 이력 저장 완료: chargeBoxId={}, idTag={}, status={}, tier={}, latency={}ms",
                        chargeBoxId, idTag, idTagInfo.getStatus(), cacheTier, latencyMs);

            } catch (Exception e) {
                log.error("[AuthAudit] 감사 이력 비동기 저장 실패 (무시): chargeBoxId={}, idTag={}, err={}",
                        chargeBoxId, idTag, e.getMessage(), e);
            }
        });
    }
}
