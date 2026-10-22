package com.lselink.elvis.control.config;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import jakarta.annotation.PostConstruct;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/**
 * P6Spy 완성형 SQL 로깅 포매터
 * 
 * 기능:
 * 1. '?' 바인딩 파라미터를 실제 값으로 인라인 치환한 완전한 SQL 출력
 * 2. 다중 행 Pretty Formatting 적용
 * 3. 쿼리 실행 소요 시간(ms) 및 카테고리 정보 표기
 */
@Configuration
public class P6SpySqlFormatter implements MessageFormattingStrategy {

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

        StringBuilder sb = new StringBuilder();
        sb.append("\n-----------------------------------------------------------------------------------------\n");
        sb.append(String.format("[P6Spy SQL] Connection: #%d | Execution Time: %d ms | Category: %s\n", connectionId, elapsed, category));
        sb.append("-----------------------------------------------------------------------------------------\n");
        sb.append(formattedSql);
        sb.append("\n-----------------------------------------------------------------------------------------");
        return sb.toString();
    }

    private String formatSql(String category, String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        // Statement/Commit/Rollback 등 중 statement인 경우만 포맷팅
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
}
