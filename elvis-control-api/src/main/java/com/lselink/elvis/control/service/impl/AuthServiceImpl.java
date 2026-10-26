package com.lselink.elvis.control.service.impl;

import com.lselink.elvis.control.dto.auth.AdminUserDto;
import com.lselink.elvis.control.dto.auth.AuthTokenDto;
import com.lselink.elvis.control.dto.auth.LoginRequest;
import com.lselink.elvis.control.dto.auth.TokenRefreshRequest;
import com.lselink.elvis.control.entity.AdminRole;
import com.lselink.elvis.control.entity.AdminUser;
import com.lselink.elvis.control.security.JwtTokenProvider;
import com.lselink.elvis.control.service.AuthService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [S33-API-0070-CO] 관리자 계정 인증 및 JWT 토큰 관리 서비스 구현체
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, AdminUser> userRegistry = new ConcurrentHashMap<>();

    public AuthServiceImpl(JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initSeedAdmins() {
        // PoC 기본 관리자 및 운영자 계정 시딩
        String adminEncodedPw = passwordEncoder.encode("admin123!");
        AdminUser admin = new AdminUser(
                "admin",
                adminEncodedPw,
                "시스템 최고관리자",
                AdminRole.ROLE_ADMIN,
                true,
                LocalDateTime.now()
        );
        userRegistry.put("admin", admin);

        String operatorEncodedPw = passwordEncoder.encode("operator123!");
        AdminUser operator = new AdminUser(
                "operator",
                operatorEncodedPw,
                "충전 관제 운영자",
                AdminRole.ROLE_OPERATOR,
                true,
                LocalDateTime.now()
        );
        userRegistry.put("operator", operator);

        String viewerEncodedPw = passwordEncoder.encode("viewer123!");
        AdminUser viewer = new AdminUser(
                "viewer",
                viewerEncodedPw,
                "관제 대시보드 조회자",
                AdminRole.ROLE_VIEWER,
                true,
                LocalDateTime.now()
        );
        userRegistry.put("viewer", viewer);

        log.info("[AuthService] PoC 기본 관리자 계정 3종(admin, operator, viewer) 시딩 완료");
    }

    @Override
    public AuthTokenDto login(LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.username()) || !StringUtils.hasText(request.password())) {
            throw new IllegalArgumentException("아이디와 비밀번호를 모두 입력해야 합니다.");
        }

        String username = request.username().trim();
        String rawPassword = request.password();

        AdminUser user = userRegistry.get(username);
        if (user == null) {
            log.warn("[AuthService] 로그인 실패 - 계정 없음: {}", username);
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        boolean passwordMatches = passwordEncoder.matches(rawPassword, user.getPassword());
        if (!passwordMatches) {
            log.warn("[AuthService] 로그인 실패 - 비밀번호 불일치: {}", username);
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!user.isEnabled()) {
            log.warn("[AuthService] 로그인 실패 - 비활성 계정: {}", username);
            throw new IllegalStateException("비활성화된 계정입니다. 관리자에게 문의하세요.");
        }

        String roleName = user.getRole().name();
        String accessToken = jwtTokenProvider.generateAccessToken(username, roleName);
        String refreshToken = jwtTokenProvider.generateRefreshToken(username);
        long validitySeconds = jwtTokenProvider.getAccessTokenValiditySeconds();

        log.info("[AuthService] 로그인 성공: username={}, role={}", username, roleName);
        return AuthTokenDto.of(accessToken, refreshToken, validitySeconds, username, roleName);
    }

    @Override
    public AuthTokenDto refreshToken(TokenRefreshRequest request) {
        if (request == null || !StringUtils.hasText(request.refreshToken())) {
            throw new IllegalArgumentException("Refresh Token은 필수입니다.");
        }

        String refreshToken = request.refreshToken().trim();
        boolean isValid = jwtTokenProvider.validateToken(refreshToken);
        boolean isRefreshType = jwtTokenProvider.isRefreshToken(refreshToken);

        if (!isValid || !isRefreshType) {
            log.warn("[AuthService] 토큰 갱신 실패 - 유효하지 않거나 만료된 Refresh Token");
            throw new IllegalArgumentException("유효하지 않거나 만료된 Refresh Token입니다.");
        }

        String username = jwtTokenProvider.extractUsername(refreshToken);
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("토큰에서 사용자 정보를 식별할 수 없습니다.");
        }

        AdminUser user = userRegistry.get(username);
        if (user == null || !user.isEnabled()) {
            throw new IllegalArgumentException("존재하지 않거나 비활성화된 계정입니다.");
        }

        String roleName = user.getRole().name();
        String newAccessToken = jwtTokenProvider.generateAccessToken(username, roleName);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);
        long validitySeconds = jwtTokenProvider.getAccessTokenValiditySeconds();

        log.info("[AuthService] 토큰 재발급(Rotation) 완료: username={}", username);
        return AuthTokenDto.of(newAccessToken, newRefreshToken, validitySeconds, username, roleName);
    }

    @Override
    public AdminUserDto getCurrentUser(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("사용자 아이디가 누락되었습니다.");
        }

        AdminUser user = userRegistry.get(username.trim());
        if (user == null) {
            throw new IllegalArgumentException("해당 사용자를 찾을 수 없습니다: " + username);
        }

        String roleDescription = user.getRole().getDescription();
        return new AdminUserDto(
                user.getUsername(),
                user.getName(),
                roleDescription,
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
