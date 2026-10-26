package com.lselink.elvis.control.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [S33-API-0070-TE] JwtTokenProvider 단위 테스트
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secretKey = "test-secret-key-for-unit-testing-purposes-only-256bit!";

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        jwtTokenProvider = new JwtTokenProvider(objectMapper, secretKey, 3600, 604800);
    }

    @Test
    @DisplayName("Access Token 생성 및 클레임 추출 정상 검증")
    void testGenerateAccessTokenAndExtractClaims() {
        String username = "admin";
        String role = "ROLE_ADMIN";

        String token = jwtTokenProvider.generateAccessToken(username, role);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(username, jwtTokenProvider.extractUsername(token));
        assertEquals(role, jwtTokenProvider.extractRole(token));
        assertFalse(jwtTokenProvider.isRefreshToken(token));
    }

    @Test
    @DisplayName("Refresh Token 생성 및 타입 판별 검증")
    void testGenerateRefreshToken() {
        String username = "operator";

        String refreshToken = jwtTokenProvider.generateRefreshToken(username);

        assertNotNull(refreshToken);
        assertTrue(jwtTokenProvider.validateToken(refreshToken));
        assertEquals(username, jwtTokenProvider.extractUsername(refreshToken));
        assertTrue(jwtTokenProvider.isRefreshToken(refreshToken));
    }

    @Test
    @DisplayName("서명이 위조된 토큰은 검증에 실패해야 한다")
    void testTamperedTokenValidationFails() {
        String token = jwtTokenProvider.generateAccessToken("admin", "ROLE_ADMIN");
        String tamperedToken = token + "corrupted";

        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("null 또는 빈 문자열 토큰은 안전하게 false를 반환해야 한다")
    void testNullOrEmptyTokenReturnsFalse() {
        assertFalse(jwtTokenProvider.validateToken(null));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken("   "));
        assertFalse(jwtTokenProvider.validateToken("invalid.token"));
    }

    @Test
    @DisplayName("만료 시간이 0초인 토큰은 즉시 만료되어야 한다")
    void testExpiredTokenReturnsFalse() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 유효시간 0초 프로바이더
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(objectMapper, secretKey, -10, -10);

        String expiredToken = shortLivedProvider.generateAccessToken("admin", "ROLE_ADMIN");

        assertFalse(shortLivedProvider.validateToken(expiredToken));
    }
}
