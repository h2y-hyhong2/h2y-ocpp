package com.lselink.elvis.tester.controller;

import com.lselink.elvis.tester.service.SystemProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tester/system")
@RequiredArgsConstructor
public class SystemProcessController {

    private final SystemProcessService processService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(processService.getSystemStatus());
    }

    @PostMapping("/start-kafka")
    public ResponseEntity<Map<String, Object>> startKafka() {
        return ResponseEntity.ok(processService.startKafka());
    }

    @PostMapping("/create-topics")
    public ResponseEntity<Map<String, Object>> createTopics() {
        return ResponseEntity.ok(processService.runCreateTopics());
    }

    @PostMapping("/start-ws")
    public ResponseEntity<Map<String, Object>> startWsGateway() {
        return ResponseEntity.ok(processService.startWsGateway());
    }

    @PostMapping("/start-sync")
    public ResponseEntity<Map<String, Object>> startSyncEngine() {
        return ResponseEntity.ok(processService.startSyncEngine());
    }
}
