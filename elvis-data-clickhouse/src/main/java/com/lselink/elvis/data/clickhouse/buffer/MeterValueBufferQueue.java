package com.lselink.elvis.data.clickhouse.buffer;

import com.lselink.elvis.data.clickhouse.model.MeterValueEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 락-프리(Lock-Free) 인메모리 버퍼 큐
 * ConcurrentLinkedQueue + AtomicInteger 기반으로 O(1) 크기 추적 및 논블로킹 인큐/디큐 제공
 */
@Component
public class MeterValueBufferQueue {

    private final ConcurrentLinkedQueue<MeterValueEntity> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger currentSize = new AtomicInteger(0);

    /**
     * 계측 엔티티 인큐 (논블로킹)
     *
     * @param entity 계측 엔티티
     * @return 현재 버퍼 내 레코드 수
     */
    public int offer(MeterValueEntity entity) {
        if (entity == null) {
            return currentSize.get();
        }
        queue.offer(entity);
        return currentSize.incrementAndGet();
    }

    /**
     * 지정된 최대 크기만큼 버퍼에서 일괄 추출
     *
     * @param maxElements 최대 추출 건수
     * @return 추출된 엔티티 리스트
     */
    public List<MeterValueEntity> drainTo(int maxElements) {
        List<MeterValueEntity> batch = new ArrayList<>(Math.min(maxElements, Math.max(currentSize.get(), 16)));
        MeterValueEntity item;

        while (batch.size() < maxElements && (item = queue.poll()) != null) {
            batch.add(item);
            currentSize.decrementAndGet();
        }

        return batch;
    }

    /**
     * 버퍼 내 모든 엔티티 일괄 추출 (Graceful Shutdown 시 활용)
     *
     * @return 전체 잔여 엔티티 리스트
     */
    public List<MeterValueEntity> drainAll() {
        List<MeterValueEntity> all = new ArrayList<>(Math.max(currentSize.get(), 16));
        MeterValueEntity item;

        while ((item = queue.poll()) != null) {
            all.add(item);
            currentSize.decrementAndGet();
        }

        return all;
    }

    /**
     * 현재 버퍼 내 대기 건수 반환 (O(1))
     */
    public int getSize() {
        return currentSize.get();
    }

    /**
     * 버퍼가 비어있는지 여부
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
