package com.lselink.elvis.connect.sync.repository;

import com.lselink.elvis.connect.sync.domain.FaultHistory;

import java.util.Optional;

/**
 * 충전기 장애 이력 QueryDSL 커스텀 Repository 인터페이스 (AGENTS.md §5)
 */
public interface FaultHistoryRepositoryCustom {

    /**
     * 충전기 커넥터의 최신 미해결 활성 장애 조회
     */
    Optional<FaultHistory> findLatestUnresolvedFault(String chargeBoxId, int connectorId);

    /**
     * 충전기 커넥터의 활성(미해결) 장애 존재 여부 확인
     */
    boolean existsActiveFault(String chargeBoxId, int connectorId);
}
