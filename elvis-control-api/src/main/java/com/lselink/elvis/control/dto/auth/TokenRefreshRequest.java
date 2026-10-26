package com.lselink.elvis.control.dto.auth;

/**
 * [S33-API-0070-CO] JWT 토큰 재발급(Refresh) 요청 DTO
 */
public record TokenRefreshRequest(
    String refreshToken
) {}
