package com.lselink.elvis.control.dto.auth;

/**
 * [S33-API-0070-CO] JWT 인증 토큰 발급 응답 DTO
 */
public record AuthTokenDto(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    String username,
    String role
) {
    public static AuthTokenDto of(String accessToken, String refreshToken, long expiresIn, String username, String role) {
        return new AuthTokenDto(accessToken, refreshToken, "Bearer", expiresIn, username, role);
    }
}
