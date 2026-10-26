package com.lselink.elvis.control.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * [S33-API-0070-CO] RFC 7519 HMAC-SHA256 기반 JWT 토큰 생성 및 검증 프로바이더
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String JWT_HEADER_BASE64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final ObjectMapper objectMapper;
    private final byte[] secretKeyBytes;
    private final long accessTokenValiditySeconds;
    private final long refreshTokenValiditySeconds;

    public JwtTokenProvider(
            ObjectMapper objectMapper,
            @Value("${elvis.control.jwt.secret:c2VjdXJlLWVsdmlzLWNzbXMtbGl0ZS1qd3Qta2V5LTIwMjYtMjU2Yml0LXNlY3JldA==}") String secretKey,
            @Value("${elvis.control.jwt.access-token-validity-seconds:3600}") long accessTokenValiditySeconds,
            @Value("${elvis.control.jwt.refresh-token-validity-seconds:604800}") long refreshTokenValiditySeconds) {
        this.objectMapper = objectMapper;
        this.secretKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }

    /**
     * Access Token 생성 (기본 유효시간 1시간)
     */
    public String generateAccessToken(String username, String role) {
        long nowSeconds = Instant.now().getEpochSecond();
        long expSeconds = nowSeconds + accessTokenValiditySeconds;

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", username);
        claims.put("role", role);
        claims.put("type", "access");
        claims.put("iat", nowSeconds);
        claims.put("exp", expSeconds);

        return createSignedToken(claims);
    }

    /**
     * Refresh Token 생성 (기본 유효시간 7일)
     */
    public String generateRefreshToken(String username) {
        long nowSeconds = Instant.now().getEpochSecond();
        long expSeconds = nowSeconds + refreshTokenValiditySeconds;

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", username);
        claims.put("type", "refresh");
        claims.put("iat", nowSeconds);
        claims.put("exp", expSeconds);

        return createSignedToken(claims);
    }

    /**
     * 토큰 서명 및 만료 유효성 검증
     */
    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        try {
            String headerAndPayload = parts[0] + "." + parts[1];
            String expectedSignature = sign(headerAndPayload);
            String actualSignature = parts[2];

            if (!expectedSignature.equals(actualSignature)) {
                log.warn("[JwtTokenProvider] JWT 서명 검증 불일치");
                return false;
            }

            Map<String, Object> claims = parseClaims(parts[1]);
            Object expObj = claims.get("exp");
            if (expObj instanceof Number expNumber) {
                long expSeconds = expNumber.longValue();
                long currentSeconds = Instant.now().getEpochSecond();
                if (currentSeconds >= expSeconds) {
                    log.warn("[JwtTokenProvider] JWT 토큰 만료 (exp: {}, now: {})", expSeconds, currentSeconds);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.warn("[JwtTokenProvider] 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰에서 사용자 아이디(username) 추출
     */
    public String extractUsername(String token) {
        Map<String, Object> claims = getClaimsFromToken(token);
        Object sub = claims.get("sub");
        if (sub != null) {
            return sub.toString();
        }
        return null;
    }

    /**
     * 토큰에서 사용자 역할(role) 추출
     */
    public String extractRole(String token) {
        Map<String, Object> claims = getClaimsFromToken(token);
        Object role = claims.get("role");
        if (role != null) {
            return role.toString();
        }
        return null;
    }

    /**
     * 토큰 타입이 Refresh Token인지 확인
     */
    public boolean isRefreshToken(String token) {
        Map<String, Object> claims = getClaimsFromToken(token);
        Object type = claims.get("type");
        if (type != null) {
            return "refresh".equalsIgnoreCase(type.toString());
        }
        return false;
    }

    public long getAccessTokenValiditySeconds() {
        return accessTokenValiditySeconds;
    }

    private String createSignedToken(Map<String, Object> claims) {
        try {
            byte[] payloadBytes = objectMapper.writeValueAsBytes(claims);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes);
            String headerAndPayload = JWT_HEADER_BASE64 + "." + encodedPayload;
            String signature = sign(headerAndPayload);

            return headerAndPayload + "." + signature;
        } catch (Exception e) {
            log.error("[JwtTokenProvider] 토큰 생성 실패: {}", e.getMessage(), e);
            throw new IllegalStateException("JWT 토큰 생성 중 오류가 발생했습니다.", e);
        }
    }

    private Map<String, Object> getClaimsFromToken(String token) {
        if (token == null) {
            return Map.of();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Map.of();
        }
        try {
            return parseClaims(parts[1]);
        } catch (Exception e) {
            log.warn("[JwtTokenProvider] Claims 파싱 실패: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<String, Object> parseClaims(String base64Payload) throws Exception {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(base64Payload);
        return objectMapper.readValue(decodedBytes, new TypeReference<Map<String, Object>>() {});
    }

    private String sign(String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, HMAC_SHA256);
        mac.init(secretKeySpec);

        byte[] signedBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signedBytes);
    }
}
