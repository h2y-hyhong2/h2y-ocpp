package com.lselink.elvis.connect.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * CSMS Lite - ELVIS-CONNECT WebSocket Gateway Application
 */
@EnableKafka
@SpringBootApplication
public class ElvisConnectWsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElvisConnectWsApplication.class, args);
    }
}
