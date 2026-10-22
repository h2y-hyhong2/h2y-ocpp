package com.lselink.elvis.connect.sync.service.impl;

import com.lselink.elvis.connect.sync.domain.DltHistory;
import com.lselink.elvis.connect.sync.domain.DltStatus;
import com.lselink.elvis.connect.sync.domain.ErrorCategory;
import com.lselink.elvis.connect.sync.dto.DeadLetterEvent;
import com.lselink.elvis.connect.sync.producer.DltProducer;
import com.lselink.elvis.connect.sync.repository.DltHistoryRepository;
import com.lselink.elvis.connect.sync.service.DltRecoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DLT 결함 격리 및 메시지 재처리 서비스 구현체 (Sprint 24: S24-SYNC-0100-CO)
 */
@Service
public class DltRecoveryServiceImpl implements DltRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(DltRecoveryServiceImpl.class);
    private static final DateTimeFormatter DLT_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final DltHistoryRepository dltHistoryRepository;
    private final DltProducer dltProducer;

    public DltRecoveryServiceImpl(DltHistoryRepository dltHistoryRepository,
                                  DltProducer dltProducer) {
        this.dltHistoryRepository = dltHistoryRepository;
        this.dltProducer = dltProducer;
    }

    @Override
    @Transactional
    public void isolateFailure(String chargeBoxId,
                               String rawPayload,
                               Exception ex,
                               String sourceTopic,
                               int sourcePartition,
                               long sourceOffset,
                               int retryCount) {
        LocalDateTime failedTime = LocalDateTime.now();
        String dltId = generateDltId(failedTime);

        // 오류 분류 판정 (Non-retryable vs Transient vs Business)
        ErrorCategory category = ErrorCategory.classify(ex);

        String exceptionClass = "UnknownException";
        String errorMessage = "알 수 없는 오류";
        if (ex != null) {
            exceptionClass = ex.getClass().getName();
            if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
                errorMessage = ex.getMessage();
            }
        }

        // 1. MySQL tbl_dlt_history 원장 영속화
        DltHistory history = DltHistory.builder()
                .dltId(dltId)
                .chargeBoxId(chargeBoxId)
                .sourceTopic(sourceTopic)
                .sourcePartition(sourcePartition)
                .sourceOffset(sourceOffset)
                .errorCategory(category)
                .exceptionClass(exceptionClass)
                .errorMessage(errorMessage)
                .rawPayload(rawPayload)
                .retryCount(retryCount)
                .failedDt(failedTime)
                .build();

        dltHistoryRepository.save(history);

        log.warn("[DltRecovery] 결함 메시지 원장 격리 저장 완료: dltId={}, boxId={}, category={}, err={}",
                dltId, chargeBoxId, category, errorMessage);

        // 2. Kafka ocpp-dlt 토픽으로 격리 이벤트 발행
        DeadLetterEvent event = new DeadLetterEvent(
                dltId,
                chargeBoxId,
                sourceTopic,
                sourcePartition,
                sourceOffset,
                rawPayload,
                category,
                exceptionClass,
                errorMessage,
                retryCount,
                failedTime,
                DltStatus.PENDING
        );

        dltProducer.sendToDlt(chargeBoxId, event);
    }

    @Override
    @Transactional
    public boolean replayMessage(String dltId) {
        Optional<DltHistory> optHistory = dltHistoryRepository.findByDltId(dltId);

        if (optHistory.isEmpty()) {
            log.warn("[DltRecovery] Replay 대상 DLT 레코드 부재: dltId={}", dltId);
            return false;
        }

        DltHistory history = optHistory.get();
        String chargeBoxId = history.getChargeBoxId();
        String rawPayload = history.getRawPayload();

        try {
            // 원본 인바운드 토픽으로 재주입
            dltProducer.replayToInboundTopic(chargeBoxId, rawPayload);

            // 상태를 REPLAYED로 갱신
            LocalDateTime replayedTime = LocalDateTime.now();
            history.markReplayed(replayedTime);
            dltHistoryRepository.save(history);

            log.info("[DltRecovery] Replay 재처리 완료: dltId={}, boxId={}", dltId, chargeBoxId);
            return true;

        } catch (Exception ex) {
            String retryErrMsg = "Replay 재주입 실패: " + ex.getMessage();
            history.markFailed(retryErrMsg);
            dltHistoryRepository.save(history);

            log.error("[DltRecovery] Replay 실행 중 오류: dltId={}, error={}", dltId, ex.getMessage(), ex);
            return false;
        }
    }

    @Override
    @Transactional
    public void discardMessage(String dltId, String reason) {
        Optional<DltHistory> optHistory = dltHistoryRepository.findByDltId(dltId);

        if (optHistory.isPresent()) {
            DltHistory history = optHistory.get();
            history.markDiscarded();
            dltHistoryRepository.save(history);

            log.info("[DltRecovery] DLT 메시지 폐기 처리 완료: dltId={}, reason={}", dltId, reason);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DltHistory> getPendingFailures() {
        return dltHistoryRepository.findAllByStatusOrderByFailedDtDesc(DltStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DltHistory> getDltDetail(String dltId) {
        return dltHistoryRepository.findByDltId(dltId);
    }

    private String generateDltId(LocalDateTime now) {
        String timePart = now.format(DLT_ID_FORMATTER);
        String uuidPart = UUID.randomUUID().toString().substring(0, 8);
        return "DLT-" + timePart + "-" + uuidPart;
    }
}
