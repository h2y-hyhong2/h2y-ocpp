package com.lselink.elvis.control.repository;

import com.lselink.elvis.control.entity.TransactionCdrEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionCdrRepository extends JpaRepository<TransactionCdrEntity, Long> {

    List<TransactionCdrEntity> findAllByOrderByStartTimeDesc();

    @Query("SELECT COUNT(t), COALESCE(SUM(t.totalKwh), 0), COALESCE(SUM(t.totalAmount), 0) FROM TransactionCdrEntity t")
    Object[] getCdrAggregateSummary();

    /**
     * 다차원 조건 검색 및 서버사이드 페이징 (Spring Data JPA)
     */
    @Query("SELECT t FROM TransactionCdrEntity t " +
           "WHERE (:chargeBoxId IS NULL OR :chargeBoxId = '' OR t.chargeBoxId = :chargeBoxId) " +
           "  AND (:userTag IS NULL OR :userTag = '' OR LOWER(t.userTag) LIKE LOWER(CONCAT('%', :userTag, '%'))) " +
           "  AND (:paymentStatus IS NULL OR :paymentStatus = '' OR :paymentStatus = 'ALL' OR t.paymentStatus = :paymentStatus) " +
           "  AND (:startDate IS NULL OR t.startTime >= :startDate) " +
           "  AND (:endDate IS NULL OR t.startTime <= :endDate) " +
           "ORDER BY t.startTime DESC")
    Page<TransactionCdrEntity> searchCdrPaged(
            @Param("chargeBoxId") String chargeBoxId,
            @Param("userTag") String userTag,
            @Param("paymentStatus") String paymentStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * 다차원 조건 검색 단일 DB 집계 (COUNT, SUM(kwh), SUM(amount))
     */
    @Query("SELECT COUNT(t), COALESCE(SUM(t.totalKwh), 0), COALESCE(SUM(t.totalAmount), 0) FROM TransactionCdrEntity t " +
           "WHERE (:chargeBoxId IS NULL OR :chargeBoxId = '' OR t.chargeBoxId = :chargeBoxId) " +
           "  AND (:userTag IS NULL OR :userTag = '' OR LOWER(t.userTag) LIKE LOWER(CONCAT('%', :userTag, '%'))) " +
           "  AND (:paymentStatus IS NULL OR :paymentStatus = '' OR :paymentStatus = 'ALL' OR t.paymentStatus = :paymentStatus) " +
           "  AND (:startDate IS NULL OR t.startTime >= :startDate) " +
           "  AND (:endDate IS NULL OR t.startTime <= :endDate)")
    Object[] getCdrAggregateSummaryByCondition(
            @Param("chargeBoxId") String chargeBoxId,
            @Param("userTag") String userTag,
            @Param("paymentStatus") String paymentStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 다차원 조건 검색 전체 목록 조회 (CSV 내보내기용)
     */
    @Query("SELECT t FROM TransactionCdrEntity t " +
           "WHERE (:chargeBoxId IS NULL OR :chargeBoxId = '' OR t.chargeBoxId = :chargeBoxId) " +
           "  AND (:userTag IS NULL OR :userTag = '' OR LOWER(t.userTag) LIKE LOWER(CONCAT('%', :userTag, '%'))) " +
           "  AND (:paymentStatus IS NULL OR :paymentStatus = '' OR :paymentStatus = 'ALL' OR t.paymentStatus = :paymentStatus) " +
           "  AND (:startDate IS NULL OR t.startTime >= :startDate) " +
           "  AND (:endDate IS NULL OR t.startTime <= :endDate) " +
           "ORDER BY t.startTime DESC")
    List<TransactionCdrEntity> findAllFiltered(
            @Param("chargeBoxId") String chargeBoxId,
            @Param("userTag") String userTag,
            @Param("paymentStatus") String paymentStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
