package com.lselink.elvis.connect.sync.repository;

import com.lselink.elvis.connect.sync.domain.RfidCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * RFID 카드 마스터 리포지토리 (Sprint 20: S20-SYNC-0070-DE)
 */
@Repository
public interface RfidCardRepository extends JpaRepository<RfidCard, Long> {

    /**
     * 태그 식별자로 활성 카드 단건 조회
     *
     * @param idTag RFID 태그 식별자
     * @return 카드 엔티티 Optional
     */
    Optional<RfidCard> findByIdTagAndIsDeletedFalse(String idTag);
}
