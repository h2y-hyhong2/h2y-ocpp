package com.lselink.elvis.data.clickhouse.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * ClickHouse 전용 DataSource 설정 클래스
 */
@Configuration
@ConditionalOnProperty(name = "elvis.clickhouse.enabled", havingValue = "true", matchIfMissing = true)
public class ClickHouseDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(ClickHouseDataSourceConfig.class);

    private final ClickHouseProperties properties;

    public ClickHouseDataSourceConfig(ClickHouseProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "clickHouseDataSource")
    @Primary
    public DataSource clickHouseDataSource() {
        String chUrl = properties.getUrl();
        String chUser = properties.getUser();
        String chPassword = properties.getPassword();
        String chDriver = properties.getDriverClassName();

        log.info("[ClickHouse] DataSource 초기화 시작: URL={}, Driver={}", chUrl, chDriver);

        DataSource dataSource = DataSourceBuilder.create()
                .url(chUrl)
                .username(chUser)
                .password(chPassword)
                .driverClassName(chDriver)
                .build();

        log.info("[ClickHouse] DataSource 초기화 완료.");
        return dataSource;
    }
}
