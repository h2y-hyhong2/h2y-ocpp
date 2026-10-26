package com.lselink.elvis.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lselink.elvis.control.dto.auth.AdminUserDto;
import com.lselink.elvis.control.dto.auth.AuthTokenDto;
import com.lselink.elvis.control.dto.auth.LoginRequest;
import com.lselink.elvis.control.dto.auth.TokenRefreshRequest;
import com.lselink.elvis.control.security.JwtTokenProvider;
import com.lselink.elvis.control.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [S33-API-0070-TE] AuthService 단위 테스트
 */
class AuthServiceTest {

    private AuthServiceImpl authService;
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        jwtTokenProvider = new JwtTokenProvider(
                objectMapper,
                "auth-service-test-secret-key-for-unit-test-256bit!",
                3600,
                604800
        );
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthServiceImpl(jwtTokenProvider, passwordEncoder);
        authService.initSeedAdmins();
    }

    @Test
    @DisplayName("기본 관리자(admin) 로그인 성공 및 토큰 반환 검증")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("admin", "admin123!");

        AuthTokenDto tokenDto = authService.login(request);

        assertNotNull(tokenDto);
        assertEquals("admin", tokenDto.username());
        assertEquals("ROLE_ADMIN", tokenDto.role());
        assertEquals("Bearer", tokenDto.tokenType());
        assertNotNull(tokenDto.accessToken());
        assertNotNull(tokenDto.refreshToken());
        assertTrue(jwtTokenProvider.validateToken(tokenDto.accessToken()));
        assertTrue(jwtTokenProvider.validateToken(tokenDto.refreshToken()));
    }

    @Test
    @DisplayName("존재하지 않는 계정으로 로그인 시 예외 발생 검증")
    void testLoginWithNonExistentUserFails() {
        LoginRequest request = new LoginRequest("unknownUser", "password123!");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertTrue(exception.getMessage().contains("아이디 또는 비밀번호가 올바르지 않습니다"));
    }

    @Test
    @DisplayName("비밀번호 불일치 시 로그인 실패 검증")
    void testLoginWithWrongPasswordFails() {
        LoginRequest request = new LoginRequest("admin", "wrongPassword!");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertTrue(exception.getMessage().contains("아이디 또는 비밀번호가 올바르지 않습니다"));
    }

    @Test
    @DisplayName("유효한 Refresh Token으로 토큰 재발급(Rotation) 성공 검증")
    void testRefreshTokenSuccess() {
        LoginRequest loginRequest = new LoginRequest("operator", "operator123!");
        AuthTokenDto loginToken = authService.login(loginRequest);

        TokenRefreshRequest refreshRequest = new TokenRefreshRequest(loginToken.refreshToken());
        AuthTokenDto refreshedToken = authService.refreshToken(refreshRequest);

        assertNotNull(refreshedToken);
        assertEquals("operator", refreshedToken.username());
        assertEquals("ROLE_OPERATOR", refreshedToken.role());
        assertTrue(jwtTokenProvider.validateToken(refreshedToken.accessToken()));
        assertTrue(jwtTokenProvider.validateToken(refreshedToken.refreshToken()));
    }

    @Test
    @DisplayName("위조된 Refresh Token으로 갱신 시도 시 예외 발생 검증")
    void testRefreshTokenWithInvalidTokenFails() {
        TokenRefreshRequest invalidRequest = new TokenRefreshRequest("invalid-tampered-token");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.refreshToken(invalidRequest)
        );

        assertTrue(exception.getMessage().contains("유효하지 않거나 만료된 Refresh Token"));
    }

    @Test
    @DisplayName("관리자 프로필 정보 조회(getCurrentUser) 성공 검증")
    void testGetCurrentUserSuccess() {
        AdminUserDto adminDto = authService.getCurrentUser("admin");

        assertNotNull(adminDto);
        assertEquals("admin", adminDto.username());
        assertEquals("시스템 최고관리자", adminDto.name());
        assertEquals("시스템 관리자", adminDto.role());
        assertTrue(adminDto.enabled());
    }
}
