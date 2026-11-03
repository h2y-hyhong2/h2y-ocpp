package com.lselink.elvis.control.repository;

import com.lselink.elvis.control.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * [S33-API-0070-CO] 관리자 계정 Spring Data JPA 리포지토리
 */
@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    /**
     * 계정명(username) 기반 단건 조회
     */
    Optional<AdminUser> findByUsername(String username);

    /**
     * 계정명 존재 여부 확인
     */
    boolean existsByUsername(String username);
}

