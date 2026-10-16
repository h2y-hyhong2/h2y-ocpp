package com.lselink.elvis.connect.sync.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.lselink.elvis.connect.sync.domain.AuthorizationStatus;
import com.lselink.elvis.connect.sync.domain.RfidCard;
import com.lselink.elvis.connect.sync.dto.IdTagInfo;
import com.lselink.elvis.connect.sync.repository.RfidCardRepository;
import com.lselink.elvis.connect.sync.service.MemberAuthCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * RFID 회원 인증 다단계(Tiered) 캐시 서비스 구현체 (Sprint 20: S20-SYNC-0070-CO)
 * <p>
 * L1 (Caffeine In-Memory: 30초) → L2 (Redis Distributed: 1시간) → L3 (MySQL RDBMS)
 * 3계층 초저지연 Cache-Aside 및 자동 장애 복원(Failover)을 수행합니다.
 * </p>
 */
@Service
public class MemberAuthCacheServiceImpl implements MemberAuthCacheService {

    private static final Logger log = LoggerFactory.getLogger(MemberAuthCacheServiceImpl.class);

    private static final String REDIS_KEY_PREFIX = "elvis:rfid:tag:";
    private static final long REDIS_TTL_SECONDS = 3600L;          // 정상 태그 TTL 1시간
    private static final long NEGATIVE_TTL_SECONDS = 10L;         // 미등록 태그 단기 TTL 10초

    private final Cache<String, IdTagInfo> caffeineTagCache;
    private final RedisTemplate<String, Object> redisCacheTemplate;
    private final RfidCardRepository rfidCardRepository;

    public MemberAuthCacheServiceImpl(Cache<String, IdTagInfo> caffeineTagCache,
                                     RedisTemplate<String, Object> redisCacheTemplate,
                                     RfidCardRepository rfidCardRepository) {
        this.caffeineTagCache = caffeineTagCache;
        this.redisCacheTemplate = redisCacheTemplate;
        this.rfidCardRepository = rfidCardRepository;
    }

    @Override
    public IdTagInfo validateIdTag(String rawIdTag) {
        if (rawIdTag == null || rawIdTag.isBlank()) {
            return IdTagInfo.invalid();
        }

        String idTag = rawIdTag.trim().toUpperCase();

        // ─── 1. L1 로컬 인메모리 캐시 조회 (Caffeine, 목표 < 0.5ms) ─────────────
        IdTagInfo l1Cached = caffeineTagCache.getIfPresent(idTag);
        if (l1Cached != null) {
            log.debug("[AuthCache] L1 Caffeine 캐시 적중: idTag={}, status={}", idTag, l1Cached.getStatus());
            l1Cached.setCacheTier("L1_CAFFEINE");
            return l1Cached;
        }

        // ─── 2. L2 분산 캐시 조회 (Redis, 목표 2~4ms) ─────────────────────────
        String redisKey = REDIS_KEY_PREFIX + idTag;
        try {
            Object redisObj = redisCacheTemplate.opsForValue().get(redisKey);
            if (redisObj instanceof IdTagInfo redisCached) {
                log.debug("[AuthCache] L2 Redis 캐시 적중: idTag={}, status={}", idTag, redisCached.getStatus());
                // L1 로컬 캐시 백필 (Backfill)
                caffeineTagCache.put(idTag, redisCached);
                redisCached.setCacheTier("L2_REDIS");
                return redisCached;
            }
        } catch (Exception e) {
            // Redis 장애 또는 Read Timeout 시 DB 직행 폴백 (장애 전파 방지)
            log.warn("[AuthCache] L2 Redis 캐시 조회 실패 (DB 직접 폴백 진행): idTag={}, err={}", idTag, e.getMessage());
        }

        // ─── 3. L3 마스터 RDBMS 조회 (MySQL tb_rfid_card, 목표 8~15ms) ──────────
        log.info("[AuthCache] L1/L2 Cache Miss -> L3 DB 원장 조회: idTag={}", idTag);
        Optional<RfidCard> cardOpt = rfidCardRepository.findByIdTagAndIsDeletedFalse(idTag);

        IdTagInfo result;
        if (cardOpt.isPresent()) {
            RfidCard card = cardOpt.get();
            AuthorizationStatus status = card.evaluateStatus(LocalDateTime.now());

            result = IdTagInfo.builder()
                    .status(status)
                    .expiryDate(card.getExpiryDate())
                    .parentIdTag(card.getParentIdTag())
                    .cacheTier("L3_DB")
                    .build();

            // L2 분산 캐시 적재 (TTL 1시간)
            cacheToRedis(redisKey, result, REDIS_TTL_SECONDS);

        } else {
            // 마스터에 미존재하는 미인가/미등록 태그
            result = IdTagInfo.builder()
                    .status(AuthorizationStatus.Invalid)
                    .cacheTier("L3_DB")
                    .build();

            // Negative Caching: 무차별 공격에 의한 DB 과부하 방지 (10초 단기 캐싱)
            cacheToRedis(redisKey, result, NEGATIVE_TTL_SECONDS);
        }

        // L1 로컬 캐시 백필
        caffeineTagCache.put(idTag, result);
        return result;
    }

    @Override
    public void evictCache(String rawIdTag) {
        if (rawIdTag == null || rawIdTag.isBlank()) {
            return;
        }

        String idTag = rawIdTag.trim().toUpperCase();
        log.info("[AuthCache] 캐시 무효화(Evict) 실행: idTag={}", idTag);

        // L1 로컬 캐시 무효화
        caffeineTagCache.invalidate(idTag);

        // L2 분산 캐시 삭제
        try {
            redisCacheTemplate.delete(REDIS_KEY_PREFIX + idTag);
        } catch (Exception e) {
            log.error("[AuthCache] Redis 캐시 삭제 실패: idTag={}, err={}", idTag, e.getMessage());
        }
    }

    private void cacheToRedis(String key, IdTagInfo info, long ttlSeconds) {
        try {
            redisCacheTemplate.opsForValue().set(key, info, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[AuthCache] Redis 캐시 적재 실패 (무시): key={}, err={}", key, e.getMessage());
        }
    }
}
