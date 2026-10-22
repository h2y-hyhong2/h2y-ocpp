package com.lselink.elvis.control.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

/**
 * ClickHouse 시계열 DB의 가동 상태를 동적으로 감지하는 조건부 Condition
 * <p>
 * 설정(elvis.clickhouse.enabled)이 true이더라도, 실제 ClickHouse 서버(기본 8123 포트)가
 * 물리적으로 실행되어 있지 않다면 DataSource 풀 초기화 및 Repository 생성을 안전하게 스킵합니다.
 * 이를 통해 로컬에 ClickHouse가 설치되어 있지 않거나 미가동 상태여도 CSMS 관제 API 서버가
 * 오류 없이 100% 정상 기동(Graceful Degradation)되도록 보장합니다.
 * </p>
 */
public class ClickHouseAvailableCondition implements Condition {

    private static final Logger log = LoggerFactory.getLogger(ClickHouseAvailableCondition.class);

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String enabledStr = context.getEnvironment().getProperty("elvis.clickhouse.enabled", "false");
        if (!"true".equalsIgnoreCase(enabledStr)) {
            log.info("[ClickHouse] elvis.clickhouse.enabled=false (시계열 DB 연동 비활성화)");
            return false;
        }

        String url = context.getEnvironment().getProperty("elvis.clickhouse.url", "jdbc:ch://localhost:8123/elvis_poc");
        String host = "localhost";
        int port = 8123;

        try {
            String cleanUrl = url.replace("jdbc:ch://", "http://").replace("jdbc:clickhouse://", "http://");
            URI uri = URI.create(cleanUrl);
            String parsedHost = uri.getHost();
            if (parsedHost != null && !parsedHost.isBlank()) {
                host = parsedHost;
            }
            int parsedPort = uri.getPort();
            if (parsedPort > 0) {
                port = parsedPort;
            }
        } catch (Exception e) {
            log.warn("[ClickHouse] JDBC URL 파싱 중 예외 발생, 기본값({}:{}) 사용: {}", host, port, e.getMessage());
        }

        // 300ms 초단기 소켓 핑으로 서버 생존 여부 확인
        try (Socket socket = new Socket()) {
            InetSocketAddress endpoint = new InetSocketAddress(host, port);
            socket.connect(endpoint, 300);
            log.info("[ClickHouse] 서버({}:{}) 연결 확인 성공! 시계열 OLAP 기능을 활성화합니다.", host, port);
            return true;
        } catch (Exception e) {
            log.warn("[ClickHouse] 서버({}:{})에 연결할 수 없습니다 (미가동 상태). ClickHouse 연동을 건너뛰고 기본 모드로 기동합니다.", host, port);
            return false;
        }
    }
}
