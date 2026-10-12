package com.lselink.elvis.connect.sync.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lselink.elvis.connect.sync.dto.IdTagInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.concurrent.TimeUnit;

/**
 * L1 (Caffeine In-Memory) 및 L2 (Redis Distributed) 캐시 인프라 설정
 * (Sprint 20: S20-SYNC-0070-CO)
 */
@Configuration
public class CacheConfig {

    /**
     * L1 로컬 인메모리 Caffeine Cache 빈 (유효시간: 30초, 최대 용량: 5,000건)
     */
    @Bean
    public Cache<String, IdTagInfo> caffeineTagCache() {
        return Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .recordStats()
                .build();
    }

    /**
     * L2 Redis 분산 캐시 직렬화 템플릿 (Key: String, Value: JSON Object)
     */
    @Bean
    public RedisTemplate<String, Object> redisCacheTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
