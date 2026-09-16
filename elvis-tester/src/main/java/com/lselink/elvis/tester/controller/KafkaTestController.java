package com.lselink.elvis.tester.controller;

import com.lselink.elvis.tester.dto.KafkaSendRequest;
import com.lselink.elvis.tester.dto.LoadTestRequest;
import com.lselink.elvis.tester.dto.TemplateDto;
import com.lselink.elvis.tester.dto.TestLogEntry;
import com.lselink.elvis.tester.service.KafkaTestService;
import com.lselink.elvis.tester.service.TemplateRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tester")
@RequiredArgsConstructor
public class KafkaTestController {

    private final KafkaTestService testService;
    private final TemplateRegistry templateRegistry;

    /**
     * 사전 정의된 OCPP 메시지 템플릿 목록 조회
     */
    @GetMapping("/templates")
    public ResponseEntity<List<TemplateDto>> getTemplates() {
        return ResponseEntity.ok(templateRegistry.getAllTemplates());
    }

    /**
     * 카프카 토픽 단건 메시지 즉시 발송
     */
    @PostMapping("/send")
    public ResponseEntity<TestLogEntry> sendMessage(@RequestBody KafkaSendRequest request) {
        TestLogEntry entry = testService.sendMessage(request);
        return ResponseEntity.ok(entry);
    }

    /**
     * 부하 테스트 시작
     */
    @PostMapping("/load/start")
    public ResponseEntity<Map<String, Object>> startLoad(@RequestBody LoadTestRequest request) {
        return ResponseEntity.ok(testService.startLoadTest(request));
    }

    /**
     * 부하 테스트 중지
     */
    @PostMapping("/load/stop")
    public ResponseEntity<Map<String, Object>> stopLoad() {
        return ResponseEntity.ok(testService.stopLoadTest());
    }

    /**
     * 부하 테스트 현재 상태 및 카운터 조회
     */
    @GetMapping("/load/status")
    public ResponseEntity<Map<String, Object>> getLoadStatus() {
        return ResponseEntity.ok(testService.getLoadStatus());
    }

    /**
     * 최근 송수신/모니터링 로그 목록 조회
     */
    @GetMapping("/logs")
    public ResponseEntity<List<TestLogEntry>> getRecentLogs() {
        return ResponseEntity.ok(testService.getRecentLogs());
    }

    /**
     * 실시간 SSE 이벤트 스트림 구독
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeStream() {
        return testService.subscribeStream();
    }
}
