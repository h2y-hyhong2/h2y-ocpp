package com.lselink.elvis.control.config;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;

/**
 * P6Spy 완성형 SQL 로깅 포매터
 * 
 * 기능:
 * 1. '?' 바인딩 파라미터를 실제 값으로 인라인 치환한 완전한 SQL 출력
 * 2. 다중 행 Pretty Formatting 적용
 * 3. 쿼리 실행 소요 시간(ms) 및 카테고리 정보 표기
 * 4. 쿼리를 호출한 HTTP 요청 엔드포인트 및 소스 코드 위치(클래스.메소드:라인) 자동 추적
 */
@Configuration
public class P6SpySqlFormatter implements MessageFormattingStrategy {

    private static final String APP_PACKAGE = "com.lselink.elvis.control";

    @PostConstruct
    public void setLogMessageFormat() {
        P6SpyOptions.getActiveInstance().setLogMessageFormat(this.getClass().getName());
    }

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        String formattedSql = formatSql(category, sql);
        if (formattedSql == null || formattedSql.trim().isEmpty()) {
            return "";
        }

        String callSite = getCallSite();
        String httpEndpoint = getHttpEndpoint();

        StringBuilder sb = new StringBuilder();
        sb.append("\n-----------------------------------------------------------------------------------------\n");
        sb.append(String.format("[P6Spy SQL] Connection: #%d | Execution Time: %d ms | Category: %s\n", connectionId, elapsed, category));
        if (!httpEndpoint.isEmpty()) {
            sb.append(String.format("[HTTP API ] %s\n", httpEndpoint));
        }
        sb.append(String.format("[Call Site] %s\n", callSite));
        sb.append("-----------------------------------------------------------------------------------------\n");
        sb.append(formattedSql);
        sb.append("\n-----------------------------------------------------------------------------------------");
        return sb.toString();
    }

    private String formatSql(String category, String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        if (Category.STATEMENT.getName().equals(category)) {
            String trimmedSql = sql.trim().toLowerCase(Locale.ROOT);
            if (trimmedSql.startsWith("create") || trimmedSql.startsWith("alter") || trimmedSql.startsWith("drop") || trimmedSql.startsWith("comment")) {
                return FormatStyle.DDL.getFormatter().format(sql);
            } else {
                return FormatStyle.BASIC.getFormatter().format(sql);
            }
        }

        return sql;
    }

    /**
     * 쿼리를 실행한 애플리케이션 내부 소스 코드 위치(클래스, 메소드, 파일, 라인 번호) 추적
     */
    private String getCallSite() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.startsWith(APP_PACKAGE) && !className.contains("P6SpySqlFormatter")) {
                return String.format("%s.%s(%s:%d)", 
                        className, 
                        element.getMethodName(), 
                        element.getFileName(), 
                        element.getLineNumber());
            }
        }
        return "Unknown Source";
    }

    /**
     * 현재 스레드에 바인딩된 HTTP 요청 정보(메소드 및 URI) 추출
     */
    private String getHttpEndpoint() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                if (req != null) {
                    String query = req.getQueryString();
                    if (query != null && !query.isEmpty()) {
                        return req.getMethod() + " " + req.getRequestURI() + "?" + query;
                    }
                    return req.getMethod() + " " + req.getRequestURI();
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}
