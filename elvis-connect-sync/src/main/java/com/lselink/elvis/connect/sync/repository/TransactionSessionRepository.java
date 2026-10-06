package com.lselink.elvis.connect.sync.repository;

import com.lselink.elvis.connect.sync.domain.SessionStatus;
import com.lselink.elvis.connect.sync.domain.TransactionSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 충전 거래 세션 관리 Repository (Sprint 19: S19-SYNC-0040-CO)
 */
@Repository
public interface TransactionSessionRepository extends JpaRepository<TransactionSession, Long> {

    /**
     * 충전기 ID와 단말 트랜잭션 ID로 세션 단건 조회 (멱등성 검증 및 종료 처리용)
     */
    Optional<TransactionSession> findByChargeBoxIdAndTransactionId(String chargeBoxId, Long transactionId);

    /**
     * 충전기 ID와 단말 트랜잭션 ID의 존재 여부 확인 (빠른 멱등성 검증용)
     */
    boolean existsByChargeBoxIdAndTransactionId(String chargeBoxId, Long transactionId);

    /**
     * 특정 커넥터의 최신 활성 세션 조회
     */
    Optional<TransactionSession> findFirstByChargeBoxIdAndConnectorIdAndStatusInOrderByStartTimeDesc(
            String chargeBoxId, int connectorId, Collection<SessionStatus> statuses);

    /**
     * 충전기의 활성 상태 세션 목록 조회 (Reboot 등으로 인한 일괄 마감 처리 시 활용)
     */
    List<TransactionSession> findByChargeBoxIdAndStatusIn(String chargeBoxId, Collection<SessionStatus> statuses);
}
