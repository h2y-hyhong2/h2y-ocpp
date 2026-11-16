package com.lselink.elvis.connect.sync.repository;

import com.lselink.elvis.connect.sync.domain.SessionStatus;
import com.lselink.elvis.connect.sync.domain.TransactionSession;

import java.util.Collection;
import java.util.Optional;

/**
 * 충전 거래 세션 QueryDSL 커스텀 Repository 인터페이스 (AGENTS.md §5)
 */
public interface TransactionSessionRepositoryCustom {

    /**
     * 특정 커넥터의 최신 활성 세션 조회
     */
    Optional<TransactionSession> findLatestActiveSession(
            String chargeBoxId, int connectorId, Collection<SessionStatus> statuses);
}
