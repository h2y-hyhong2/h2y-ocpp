package com.lselink.elvis.connect.sync.repository;

import com.lselink.elvis.connect.sync.domain.AuthHist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Authorize 인증 감사 이력 리포지토리 (Sprint 20: S20-SYNC-0070-DE)
 */
@Repository
public interface AuthHistRepository extends JpaRepository<AuthHist, Long> {
}
