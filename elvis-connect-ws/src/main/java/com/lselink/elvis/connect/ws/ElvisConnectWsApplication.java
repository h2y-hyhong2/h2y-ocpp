package com.lselink.elvis.connect.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CSMS Lite - ELVIS-CONNECT WebSocket Gateway Application
 */
@EnableScheduling
@EnableKafka
@SpringBootApplication
public class ElvisConnectWsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElvisConnectWsApplication.class, args);
    }
}
