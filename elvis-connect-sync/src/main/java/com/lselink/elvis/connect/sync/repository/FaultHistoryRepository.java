package com.lselink.elvis.connect.sync.repository;

import com.lselink.elvis.connect.sync.domain.FaultHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 충전기 장애 이력 Repository (Sprint 23: S23-SYNC-0060-CO)
 */
public interface FaultHistoryRepository extends JpaRepository<FaultHistory, Long> {

    /**
     * 충전기 커넥터의 최신 활성(미해결) 장애 레코드 조회
     *
     * @param chargeBoxId 충전기 ID
     * @param connectorId 커넥터 번호
     * @param isResolved  해결 여부 ("N")
     * @return 활성 장애 Optional
     */
    Optional<FaultHistory> findFirstByChargeBoxIdAndConnectorIdAndIsResolvedOrderByFaultIdDesc(
            String chargeBoxId, int connectorId, String isResolved
    );

    /**
     * 특정 충전기 커넥터의 모든 활성 장애 목록 조회
     */
    List<FaultHistory> findAllByChargeBoxIdAndConnectorIdAndIsResolved(
            String chargeBoxId, int connectorId, String isResolved
    );

    /**
     * 활성 상태인 장애 레코드들을 원자적으로 일괄 정상 복구 마감 처리
     *
     * @param chargeBoxId 충전기 ID
     * @param connectorId 커넥터 번호
     * @param resolvedDt  복구 일시
     * @return 갱신된 행 수
     */
    @Modifying
    @Query(value = """
            UPDATE tbl_fault_history
               SET is_resolved = 'Y',
                   resolved_dt = :resolvedDt,
                   updt_dt     = NOW(3)
             WHERE charge_box_id = :chargeBoxId
               AND connector_id  = :connectorId
               AND is_resolved   = 'N'
            """, nativeQuery = true)
    int resolveActiveFaults(
            @Param("chargeBoxId") String chargeBoxId,
            @Param("connectorId") int connectorId,
            @Param("resolvedDt") LocalDateTime resolvedDt
    );
}
