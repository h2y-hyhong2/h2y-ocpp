package com.lselink.elvis.data.clickhouse.service;

import com.lselink.elvis.data.clickhouse.buffer.MeterValueBufferQueue;
import com.lselink.elvis.data.clickhouse.config.ClickHouseProperties;
import com.lselink.elvis.data.clickhouse.model.MeterValueEntity;
import com.lselink.elvis.data.clickhouse.repository.ClickHouseBulkRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 계측값 인메모리 버퍼링 및 이중 조건 플러시(Dual Trigger Flush) 핵심 서비스
 */
@Service
public class MeterValueBatchService {

    private static final Logger log = LoggerFactory.getLogger(MeterValueBatchService.class);

    private final MeterValueBufferQueue bufferQueue;
    private final ClickHouseBulkRepository clickHouseRepository;
    private final int batchSizeThreshold;
    private final long flushIntervalMs;

    private final AtomicLong lastFlushTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean isFlushing = new AtomicBoolean(false);

    @Autowired
    public MeterValueBatchService(
            MeterValueBufferQueue bufferQueue,
            @Autowired(required = false) ClickHouseBulkRepository clickHouseRepository,
            ClickHouseProperties properties) {
        this.bufferQueue = bufferQueue;
        this.clickHouseRepository = clickHouseRepository;
        this.batchSizeThreshold = properties.getBatch().getSize();
        this.flushIntervalMs = properties.getBatch().getTimeoutMs();

        log.info("[ClickHouse] MeterValueBatchService 초기화 완료 (임계치: {}건, 주기: {}ms)",
                this.batchSizeThreshold, this.flushIntervalMs);
    }

    /**
     * 계측값 비차단 큐잉 (인바운드 스레드는 블로킹 없이 즉시 반환)
     * 조건 ①: 1,000건 임계치 도달 시 가상 스레드로 즉시 플러시 트리거
     *
     * @param entity 계측값 엔티티
     */
    public void enqueue(MeterValueEntity entity) {
        int currentSize = bufferQueue.offer(entity);

        // 조건 ①: 임계치 도달 시 즉시 가상 스레드 플러시 발화
        if (currentSize >= batchSizeThreshold) {
            triggerVirtualThreadFlush();
        }
    }

    /**
     * 조건 ②: 1초 주기 타이머에 의한 경과 시간 검사 및 플러시 트리거
     */
    public void checkAndFlushTimeout() {
        if (bufferQueue.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastTime = lastFlushTime.get();
        long elapsedMs = now - lastTime;

        if (elapsedMs >= flushIntervalMs) {
            triggerVirtualThreadFlush();
        }
    }

    /**
     * 가상 스레드를 활용한 비동기 논블로킹 플러시 실행
     */
    private void triggerVirtualThreadFlush() {
        Thread.ofVirtual().name("ch-bulk-flush-worker").start(this::executeFlush);
    }

    /**
     * 버퍼에서 데이터를 인출하여 ClickHouse에 실제 벌크 전송
     */
    public synchronized void executeFlush() {
        if (bufferQueue.isEmpty()) {
            return;
        }

        // 플러시 중복 실행 방지
        if (!isFlushing.compareAndSet(false, true)) {
            return;
        }

        try {
            List<MeterValueEntity> batchList = bufferQueue.drainTo(batchSizeThreshold);
            if (batchList.isEmpty()) {
                return;
            }

            int batchCount = batchList.size();
            long updateTime = System.currentTimeMillis();
            lastFlushTime.set(updateTime);

            if (clickHouseRepository != null) {
                int inserted = clickHouseRepository.insertBatch(batchList);
                int remaining = bufferQueue.getSize();
                log.info("[ClickHouse] 벌크 플러시 성공: {}건 전송 완료 (잔여 큐: {}건)", inserted, remaining);
            } else {
                log.warn("[ClickHouse] Repository 비활성화 모드 (단순 드레인): {}건 소각", batchCount);
            }

        } catch (Exception ex) {
            log.error("[ClickHouse] 벌크 플러시 실행 중 예외 발생: {}", ex.getMessage(), ex);
        } finally {
            isFlushing.set(false);
        }
    }

    /**
     * 스프링 컨텍스트 종료 시 잔여 데이터 전수 플러시 (Graceful Drain)
     */
    @PreDestroy
    public void onShutdown() {
        int remainingSize = bufferQueue.getSize();
        log.info("[ClickHouse] 서버 셧다운 감지. 잔여 미터값 전수 플러시 시작 (잔여 건수: {}건)", remainingSize);

        try {
            List<MeterValueEntity> remainingList = bufferQueue.drainAll();
            if (!remainingList.isEmpty() && clickHouseRepository != null) {
                int inserted = clickHouseRepository.insertBatch(remainingList);
                log.info("[ClickHouse] 셧다운 잔여 미터값 플러시 완료: {}건 전송 성공", inserted);
            }
        } catch (Exception ex) {
            log.error("[ClickHouse] 셧다운 잔여 미터값 플러시 실패: {}", ex.getMessage(), ex);
        }

        log.info("[ClickHouse] MeterValueBatchService 정상 종료 완료.");
    }

    public int getQueueSize() {
        return bufferQueue.getSize();
    }
}
