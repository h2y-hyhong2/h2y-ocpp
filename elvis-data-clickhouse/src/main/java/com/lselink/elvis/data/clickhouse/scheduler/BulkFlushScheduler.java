package com.lselink.elvis.data.clickhouse.scheduler;

import com.lselink.elvis.data.clickhouse.service.MeterValueBatchService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 1초 주기 타이머 기반 플러시 스케줄러
 * 저부하/소규모 인입 시에도 1초 이내 적재를 보장하는 시간 기반 트리거
 */
@Component
public class BulkFlushScheduler {

    private static final Logger log = LoggerFactory.getLogger(BulkFlushScheduler.class);

    private final MeterValueBatchService batchService;
    private ScheduledExecutorService scheduler;

    public BulkFlushScheduler(MeterValueBatchService batchService) {
        this.batchService = batchService;
    }

    @PostConstruct
    public void start() {
        log.info("[ClickHouse] BulkFlushScheduler 1초 주기 타이머 시작...");
        // Virtual Threads 팩토리를 사용한 ScheduledExecutorService 생성
        this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("ch-flush-timer").factory());
        this.scheduler.scheduleAtFixedRate(this::tick, 500, 500, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        try {
            batchService.checkAndFlushTimeout();
        } catch (Exception ex) {
            log.error("[ClickHouse] 스케줄러 플러시 검사 실패: {}", ex.getMessage(), ex);
        }
    }

    @PreDestroy
    public void stop() {
        log.info("[ClickHouse] BulkFlushScheduler 종료...");
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
