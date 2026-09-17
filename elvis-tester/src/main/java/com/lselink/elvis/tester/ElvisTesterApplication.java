package com.lselink.elvis.tester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ElvisTesterApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElvisTesterApplication.class, args);
    }
}
