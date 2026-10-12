package com.lselink.elvis.connect.sync.service;

import com.lselink.elvis.connect.sync.dto.IdTagInfo;

/**
 * RFID 회원 인증 다단계(Tiered) 캐시 조회 인터페이스 (Sprint 20: S20-SYNC-0070-DE)
 * <p>
 * L1(Caffeine 로컬 인메모리, 30초) → L2(Redis 분산 캐시, 1시간) → L3(MySQL tb_rfid_card 마스터)
 * 다단계 Cache-Aside 패턴을 적용하여 평균 5ms 이내(P99 30ms 이하) 초저지연 응답을 제공합니다.
 * </p>
 */
public interface MemberAuthCacheService {

    /**
     * RFID 태그 유효성을 다단계 캐시 기반으로 검증 및 상태 판정
     *
     * @param idTag 충전기로부터 인입된 RFID 식별자
     * @return 판정된 IdTagInfo (Accepted, Blocked, Expired, Invalid 등)
     */
    IdTagInfo validateIdTag(String idTag);

    /**
     * 회원/카드 정보 변경 시 캐시 무효화 (Evict)
     *
     * @param idTag 무효화 대상 RFID 식별자
     */
    void evictCache(String idTag);
}
