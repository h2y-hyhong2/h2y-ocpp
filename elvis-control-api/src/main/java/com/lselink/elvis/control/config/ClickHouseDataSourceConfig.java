package com.lselink.elvis.control.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * ClickHouse OLAP 전용 HikariCP 커넥션 풀 및 DataSource 구성 클래스
 */
@Configuration
@Conditional(ClickHouseAvailableCondition.class)
public class ClickHouseDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(ClickHouseDataSourceConfig.class);

    @Value("${elvis.clickhouse.url:jdbc:ch://localhost:8123/elvis_poc}")
    private String url;

    @Value("${elvis.clickhouse.user:default}")
    private String user;

    @Value("${elvis.clickhouse.password:}")
    private String password;

    @Value("${elvis.clickhouse.driver-class-name:com.clickhouse.jdbc.ClickHouseDriver}")
    private String driverClassName;

    @Value("${elvis.clickhouse.maximum-pool-size:15}")
    private int maxPoolSize;

    @Value("${elvis.clickhouse.minimum-idle:5}")
    private int minIdle;

    @Value("${elvis.clickhouse.connection-timeout-ms:5000}")
    private long connectionTimeoutMs;

    @Value("${elvis.clickhouse.socket-timeout-ms:10000}")
    private String socketTimeoutMs;

    @Bean(name = "clickHouseDataSource")
    public DataSource clickHouseDataSource() {
        log.info("[ClickHouse] HikariCP OLAP 커넥션 풀 초기화 시작 (URL={}, MaxPool={})", url, maxPoolSize);

        HikariConfig config = new HikariConfig();
        config.setPoolName("HikariPool-ClickHouse-OLAP");
        config.setDriverClassName(driverClassName);
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);

        // OLAP 집약형 커넥션 풀 사이징 및 타임아웃
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeoutMs);
        config.setIdleTimeout(600000);           // 10분
        config.setMaxLifetime(1800000);          // 30분
        config.setLeakDetectionThreshold(10000); // 10초

        // ClickHouse 미가동 상태에서도 풀 생성 시 fail-fast로 인한 전체 서버 다운 방지
        config.setInitializationFailTimeout(0);

        // ClickHouse JDBC 전용 소켓 및 고속 LZ4 압축 파라미터
        config.addDataSourceProperty("compress", "1");
        config.addDataSourceProperty("socket_timeout", socketTimeoutMs);
        config.addDataSourceProperty("connect_timeout", "3000");

        HikariDataSource dataSource = new HikariDataSource(config);
        log.info("[ClickHouse] HikariCP OLAP 커넥션 풀 초기화 완료 (PoolName={})", config.getPoolName());
        return dataSource;
    }

    @Bean(name = "clickHouseJdbcTemplate")
    public NamedParameterJdbcTemplate clickHouseJdbcTemplate(
            @Qualifier("clickHouseDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
