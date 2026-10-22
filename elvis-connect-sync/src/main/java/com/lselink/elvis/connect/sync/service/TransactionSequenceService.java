package com.lselink.elvis.connect.sync.service;

import com.lselink.elvis.connect.sync.repository.TransactionSessionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * OCPP 1.6 규격 트랜잭션 ID 전역 단일 시퀀스 채번 서비스 (규칙 10 표준 준수)
 * <p>
 * - 타입: 32비트 signed integer (최대값 2,147,483,647 이하)
 * - 채번 주체: CSMS 서버 (StartTransaction.conf 응답 시 발급)
 * - Redis INCR 원자적 채번 및 DB MAX(transaction_id) 기반 초기 동기화/폴백
 * </p>
 */
@Service
public class TransactionSequenceService {

    private static final Logger log = LoggerFactory.getLogger(TransactionSequenceService.class);

    public static final String REDIS_SEQ_KEY = "csms:seq:transaction_id";
    public static final long MAX_TRANSACTION_ID = 2_147_483_647L;

    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionSessionRepository sessionRepository;
    private final AtomicLong fallbackSequence = new AtomicLong(0L);

    public TransactionSequenceService(StringRedisTemplate stringRedisTemplate,
                                      TransactionSessionRepository sessionRepository) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.sessionRepository = sessionRepository;
    }

    @PostConstruct
    public void initSequence() {
        try {
            long maxDbId = sessionRepository.findMaxTransactionId();
            fallbackSequence.set(maxDbId);

            Boolean hasKey = stringRedisTemplate.hasKey(REDIS_SEQ_KEY);
            if (hasKey == null || !hasKey) {
                stringRedisTemplate.opsForValue().set(REDIS_SEQ_KEY, String.valueOf(maxDbId));
                log.info("[TxSeqSvc] Redis 트랜잭션 시퀀스 초기화 완료: key={}, initialVal={}", REDIS_SEQ_KEY, maxDbId);
            } else {
                String currentValStr = stringRedisTemplate.opsForValue().get(REDIS_SEQ_KEY);
                long currentVal = 0L;
                if (currentValStr != null && !currentValStr.isBlank()) {
                    currentVal = Long.parseLong(currentValStr);
                }
                if (currentVal < maxDbId) {
                    stringRedisTemplate.opsForValue().set(REDIS_SEQ_KEY, String.valueOf(maxDbId));
                    log.warn("[TxSeqSvc] Redis 시퀀스가 DB보다 낮아 동기화: redis={}, dbMax={}", currentVal, maxDbId);
                } else {
                    log.info("[TxSeqSvc] 기존 Redis 트랜잭션 시퀀스 유지: key={}, currentVal={}", REDIS_SEQ_KEY, currentVal);
                }
            }
        } catch (Exception e) {
            log.warn("[TxSeqSvc] Redis 시퀀스 초기화 실패 (인메모리 폴백 활성화): err={}", e.getMessage());
        }
    }

    /**
     * 전역 고유 트랜잭션 ID 발급 (1부터 순차 증가, 32비트 signed int 범위 보장)
     */
    public long nextTransactionId() {
        try {
            Long nextId = stringRedisTemplate.opsForValue().increment(REDIS_SEQ_KEY);
            if (nextId != null) {
                if (nextId > MAX_TRANSACTION_ID) {
                    log.warn("[TxSeqSvc] ⚠️ 트랜잭션 ID 최대치(21억) 도달로 롤오버(1) 처리");
                    stringRedisTemplate.opsForValue().set(REDIS_SEQ_KEY, "1");
                    return 1L;
                }
                return nextId;
            }
        } catch (Exception e) {
            log.warn("[TxSeqSvc] Redis 채번 실패, 인메모리 원자 시퀀스로 폴백: err={}", e.getMessage());
        }

        long fallbackNext = fallbackSequence.incrementAndGet();
        if (fallbackNext > MAX_TRANSACTION_ID) {
            fallbackSequence.set(1L);
            return 1L;
        }
        return fallbackNext;
    }
}
