package com.lselink.elvis.connect.sync.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Java 25 가상 스레드(Virtual Threads) 실행기 설정
 * 블로킹 I/O(DB, 외부 API) 발생 시 OS 커널 스레드를 블로킹하지 않고 수만 개의 동시 작업을 경량 처리
 */
@Configuration
public class VirtualThreadExecutorConfig {

    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return new TaskExecutorAdapter(
                Executors.newThreadPerTaskExecutor(
                        Thread.ofVirtual().name("vt-sync-", 1).factory()
                )
        );
    }
}
