package com.lselink.elvis.connect.sync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Java 25 가상 스레드(Virtual Threads) 기반 고성능 이벤트 동기화 및 전처리 엔진
 * Kafka ocpp-raw-events 토픽 소비 후 파싱, 검증 및 도메인 파이프라인 중계
 */
@EnableAsync
@SpringBootApplication
public class ElvisConnectSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElvisConnectSyncApplication.class, args);
    }
}
