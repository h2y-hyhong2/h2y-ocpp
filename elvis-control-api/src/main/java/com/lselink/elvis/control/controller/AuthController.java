package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.common.ApiResponse;
import com.lselink.elvis.control.dto.auth.AdminUserDto;
import com.lselink.elvis.control.dto.auth.AuthTokenDto;
import com.lselink.elvis.control.dto.auth.LoginRequest;
import com.lselink.elvis.control.dto.auth.TokenRefreshRequest;
import com.lselink.elvis.control.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [S33-API-0070-CO] 관리자 계정 로그인, 토큰 갱신 및 내 정보 조회 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 관리자 로그인 및 JWT 발급
     */
    @PostMapping("/login")
    public ApiResponse<AuthTokenDto> login(@RequestBody LoginRequest request) {
        log.info("[AuthController] 로그인 요청 수신: username={}", request != null ? request.username() : "null");
        AuthTokenDto tokenDto = authService.login(request);
        return ApiResponse.ok(tokenDto);
    }

    /**
     * JWT 토큰 재발급(Rotation)
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthTokenDto> refreshToken(@RequestBody TokenRefreshRequest request) {
        log.info("[AuthController] 토큰 재발급 요청 수신");
        AuthTokenDto tokenDto = authService.refreshToken(request);
        return ApiResponse.ok(tokenDto);
    }

    /**
     * 현재 로그인 관리자 프로필 정보 조회
     */
    @GetMapping("/me")
    public ApiResponse<AdminUserDto> getMyProfile(@AuthenticationPrincipal String username) {
        log.info("[AuthController] 관리자 정보 조회 요청: username={}", username);
        AdminUserDto adminUserDto = authService.getCurrentUser(username);
        return ApiResponse.ok(adminUserDto);
    }
}
