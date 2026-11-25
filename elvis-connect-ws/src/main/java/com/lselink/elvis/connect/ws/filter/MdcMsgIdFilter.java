package com.lselink.elvis.connect.ws.filter;

import com.lselink.elvis.connect.ws.util.LogFormatUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP/핸드셰이크 요청 시 URI에서 charger를 MDC에 바인딩하고, 요청 종료 시 charger·msgId를 정리하는 필터.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcMsgIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String charger = LogFormatUtils.extractChargerFromUri(request.getRequestURI());
        if (charger != null && !charger.isBlank()) {
            MDC.put(LogFormatUtils.MDC_CHARGER, charger);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(LogFormatUtils.MDC_CHARGER);
            MDC.remove(LogFormatUtils.MDC_MSG_ID);
        }
    }
}
