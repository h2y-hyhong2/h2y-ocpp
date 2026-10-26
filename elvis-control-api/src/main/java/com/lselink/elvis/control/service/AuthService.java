package com.lselink.elvis.control.service;

import com.lselink.elvis.control.dto.auth.AdminUserDto;
import com.lselink.elvis.control.dto.auth.AuthTokenDto;
import com.lselink.elvis.control.dto.auth.LoginRequest;
import com.lselink.elvis.control.dto.auth.TokenRefreshRequest;

/**
 * [S33-API-0070-CO] 관리자 계정 인증 및 토큰 관리 서비스 인터페이스
 */
public interface AuthService {

    /**
     * 관리자 로그인 및 JWT Access/Refresh 토큰 발급
     */
    AuthTokenDto login(LoginRequest request);

    /**
     * Refresh Token을 통한 토큰 재발급(Rotation)
     */
    AuthTokenDto refreshToken(TokenRefreshRequest request);

    /**
     * 현재 인증된 관리자 프로필 정보 조회
     */
    AdminUserDto getCurrentUser(String username);
}
