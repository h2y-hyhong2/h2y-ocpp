package com.lselink.elvis.connect.sync.repository;

import com.lselink.elvis.connect.sync.domain.ChargeDetailRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 충전 과금 원장(CDR) 리포지토리 (Sprint 21: S21-SYNC-0080-CO)
 */
@Repository
public interface ChargeDetailRecordRepository extends JpaRepository<ChargeDetailRecord, Long> {

    /**
     * 충전기 식별자와 트랜잭션 번호로 CDR 조회 (복합 고유키 매핑)
     */
    Optional<ChargeDetailRecord> findByChargeBoxIdAndTransactionId(String chargeBoxId, Long transactionId);

    /**
     * 과금 영수증 번호(CDR_NO)로 조회
     */
    Optional<ChargeDetailRecord> findByCdrNo(String cdrNo);

    /**
     * 특정 충전기 및 트랜잭션의 과금 원장 존재 여부 확인 (멱등성 가드)
     */
    boolean existsByChargeBoxIdAndTransactionId(String chargeBoxId, Long transactionId);

    /**
     * 특정 회원 태그의 충전 원장 이력 조회 (종료시간 내림차순)
     */
    List<ChargeDetailRecord> findByIdTagOrderByStopTimeDesc(String idTag);

    /**
     * 특정 기간 내 발생한 과금 원장 목록 조회
     */
    List<ChargeDetailRecord> findByStopTimeBetweenOrderByStopTimeAsc(LocalDateTime start, LocalDateTime end);
}
