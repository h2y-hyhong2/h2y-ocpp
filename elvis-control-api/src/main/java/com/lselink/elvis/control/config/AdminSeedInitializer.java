package com.lselink.elvis.control.config;

import com.lselink.elvis.control.service.impl.AuthServiceImpl;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 기동 후 Spring 트랜잭션 프록시를 통해 기본 관리자 계정을 시딩하는 Runner
 */
@Component
public class AdminSeedInitializer implements ApplicationRunner {

    private final AuthServiceImpl authService;

    public AdminSeedInitializer(AuthServiceImpl authService) {
        this.authService = authService;
    }

    @Override
    public void run(ApplicationArguments args) {
        authService.initSeedAdmins();
    }
}
