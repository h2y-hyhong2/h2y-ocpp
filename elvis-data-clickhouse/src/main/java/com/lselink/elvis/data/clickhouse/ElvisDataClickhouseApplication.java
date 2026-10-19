package com.lselink.elvis.data.clickhouse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CSMS Lite - ClickHouse 시계열 대용량 적재 엔진 (elvis-data-clickhouse)
 * 포트: 18083 (04-아키텍처 표준)
 */
@SpringBootApplication
public class ElvisDataClickhouseApplication {

    private static final Logger log = LoggerFactory.getLogger(ElvisDataClickhouseApplication.class);

    public static void main(String[] args) {
        log.info("[ClickHouse] ElvisDataClickhouseApplication 기동 시작...");
        SpringApplication.run(ElvisDataClickhouseApplication.class, args);
        log.info("[ClickHouse] ElvisDataClickhouseApplication 기동 완료 (포트 18083).");
    }
}
