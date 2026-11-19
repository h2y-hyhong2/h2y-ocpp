package com.lselink.elvis.control.config;

import com.lselink.elvis.control.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * REST API 요청/응답 공통 로깅 필터
 * <p>
 * /api/** 요청의 HTTP 메서드, URI(쿼리 포함), 상태코드, 처리 시간(ms), 인증 사용자, 클라이언트 IP를 기록합니다.
 * </p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RestApiLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RestApiLoggingFilter.class);
    private static final Pattern SENSITIVE_QUERY_PATTERN =
            Pattern.compile("(?i)(password|passwd|pwd|secret|token|accessToken|refreshToken)=([^&]*)");

    private final boolean enabled;

    public RestApiLoggingFilter(
            @Value("${elvis.control.api-logging.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/")) {
            return true;
        }
        return HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long startNanos = System.nanoTime();
        String method = request.getMethod();
        String path = buildRequestPath(request);
        String clientIp = resolveClientIp(request);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            int status = response.getStatus();
            String username = resolveUsername(request);

            if (status >= 500) {
                log.error("[API] {} {} -> {} ({}ms) user={} ip={}",
                        method, path, status, elapsedMs, username, clientIp);
            } else if (status >= 400) {
                log.warn("[API] {} {} -> {} ({}ms) user={} ip={}",
                        method, path, status, elapsedMs, username, clientIp);
            } else {
                log.info("[API] {} {} -> {} ({}ms) user={} ip={}",
                        method, path, status, elapsedMs, username, clientIp);
            }
        }
    }

    private String buildRequestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return uri;
        }
        String maskedQuery = SENSITIVE_QUERY_PATTERN.matcher(query).replaceAll("$1=***");
        return uri + "?" + maskedQuery;
    }

    private String resolveUsername(HttpServletRequest request) {
        Object attr = request.getAttribute(JwtAuthenticationFilter.ATTR_AUTH_USERNAME);
        if (attr instanceof String name && !name.isBlank()) {
            return name;
        }
        return "anonymous";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int commaIdx = forwarded.indexOf(',');
            return (commaIdx > 0 ? forwarded.substring(0, commaIdx) : forwarded).trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "-";
    }
}
