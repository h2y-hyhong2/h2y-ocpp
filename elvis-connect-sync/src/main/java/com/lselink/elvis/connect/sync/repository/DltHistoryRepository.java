package com.lselink.elvis.connect.sync.repository;

import com.lselink.elvis.connect.sync.domain.DltHistory;
import com.lselink.elvis.connect.sync.domain.DltStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DLT 실패 메시지 격리 원장 Repository (Sprint 24: S24-SYNC-0100-CO)
 */
public interface DltHistoryRepository extends JpaRepository<DltHistory, String> {

    /**
     * DLT 식별자로 단건 조회
     */
    Optional<DltHistory> findByDltId(String dltId);

    /**
     * 상태별 DLT 목록 조회 (최신 실패순)
     */
    List<DltHistory> findAllByStatusOrderByFailedDtDesc(DltStatus status);

    /**
     * 특정 충전기의 DLT 목록 조회 (최신순)
     */
    List<DltHistory> findAllByChargeBoxIdOrderByFailedDtDesc(String chargeBoxId);

    /**
     * 상태별 건수 카운트 (미처리 PENDING 건수 모니터링용)
     */
    long countByStatus(DltStatus status);

    /**
     * Replay 성공 시 상태 마감 처리
     */
    @Modifying
    @Query(value = """
            UPDATE tbl_dlt_history
               SET status      = 'REPLAYED',
                   replayed_dt = :replayedDt,
                   updt_dt     = NOW(3)
             WHERE dlt_id      = :dltId
            """, nativeQuery = true)
    int markReplayedNative(
            @Param("dltId") String dltId,
            @Param("replayedDt") LocalDateTime replayedDt
    );
}
