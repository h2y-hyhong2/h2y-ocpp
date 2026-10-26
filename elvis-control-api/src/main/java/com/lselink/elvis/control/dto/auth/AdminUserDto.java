package com.lselink.elvis.control.dto.auth;

import java.time.LocalDateTime;

/**
 * [S33-API-0070-CO] 현재 로그인 관리자 정보 응답 DTO
 */
public record AdminUserDto(
    String username,
    String name,
    String role,
    boolean enabled,
    LocalDateTime createdAt
) {}
