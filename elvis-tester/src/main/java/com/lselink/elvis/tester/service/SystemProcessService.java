package com.lselink.elvis.tester.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class SystemProcessService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(1000))
            .build();

    /**
     * 전체 컴포넌트 실시간 구동 상태 체크
     */
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("kafka", checkSocket("localhost", 9092));
        status.put("wsGateway", checkHttp("http://localhost:8080/actuator/health"));
        status.put("syncEngine", checkHttp("http://localhost:8082/actuator/health"));
        status.put("testerBackend", true);

        return status;
    }

    /**
     * Kafka 토픽 자동 생성 스크립트 실행
     */
    public Map<String, Object> runCreateTopics() {
        return executeScript("scripts/create-kafka-topics.bat", "Kafka 토픽 자동 생성");
    }

    /**
     * Kafka KRaft 브로커 기동
     */
    public Map<String, Object> startKafka() {
        return executeScript("scripts/start-kafka-kraft.bat", "Apache Kafka (KRaft)");
    }

    /**
     * elvis-connect-ws 게이트웨이 기동
     */
    public Map<String, Object> startWsGateway() {
        return executeCmd("cmd /c start \"ELVIS - WS Gateway (:8080)\" gradlew.bat :elvis-connect-ws:bootRun", "WebSocket 게이트웨이");
    }

    /**
     * elvis-connect-sync 싱크 엔진 기동
     */
    public Map<String, Object> startSyncEngine() {
        return executeCmd("cmd /c start \"ELVIS - Sync Engine (:8082)\" gradlew.bat :elvis-connect-sync:bootRun", "Sync 엔진");
    }

    private boolean checkSocket(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 800);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkHttp(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(800))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> executeScript(String relativeScriptPath, String serviceName) {
        try {
            File projectRoot = findProjectRoot();
            File scriptFile = new File(projectRoot, relativeScriptPath);

            if (!scriptFile.exists()) {
                return Map.of("success", false, "message", "스크립트 파일을 찾을 수 없습니다: " + scriptFile.getAbsolutePath());
            }

            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", serviceName, scriptFile.getAbsolutePath());
            pb.directory(projectRoot);
            pb.start();

            log.info("[SystemProcess] 스크립트 실행 성공: {}", serviceName);
            return Map.of("success", true, "message", serviceName + " 실행 명령이 전달되었습니다.");
        } catch (IOException e) {
            log.error("[SystemProcess] 스크립트 실행 실패: {}", e.getMessage(), e);
            return Map.of("success", false, "message", "실행 실패: " + e.getMessage());
        }
    }

    private Map<String, Object> executeCmd(String cmd, String serviceName) {
        try {
            File projectRoot = findProjectRoot();
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", cmd);
            pb.directory(projectRoot);
            pb.start();

            log.info("[SystemProcess] 서비스 기동 명령 실행: {}", serviceName);
            return Map.of("success", true, "message", serviceName + " 기동 명령이 전달되었습니다.");
        } catch (IOException e) {
            log.error("[SystemProcess] 기동 명령 실패: {}", e.getMessage(), e);
            return Map.of("success", false, "message", "실행 실패: " + e.getMessage());
        }
    }

    private File findProjectRoot() {
        File current = new File(".").getAbsoluteFile();
        while (current != null) {
            if (new File(current, "settings.gradle").exists()) {
                return current;
            }
            current = current.getParentFile();
        }
        return new File(".");
    }
}
