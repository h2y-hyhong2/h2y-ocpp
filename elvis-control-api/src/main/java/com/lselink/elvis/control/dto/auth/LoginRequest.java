package com.lselink.elvis.control.dto.auth;

/**
 * [S33-API-0070-CO] 관리자 로그인 요청 DTO
 */
public record LoginRequest(
    String username,
    String password
) {}
