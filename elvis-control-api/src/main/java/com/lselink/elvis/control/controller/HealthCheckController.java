package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CSMS Lite 어드민 API 시스템 헬스체크 컨트롤러 (Sprint 25: S25-API-0010-CO)
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthCheckController {

    @GetMapping
    public ApiResponse<Map<String, Object>> checkHealth() {
        Map<String, Object> statusMap = new LinkedHashMap<>();
        statusMap.put("status", "UP");
        statusMap.put("service", "elvis-control-api");
        statusMap.put("port", 18082);
        statusMap.put("version", "1.0.0-poc");
        statusMap.put("systemTime", Instant.now().toString());

        return ApiResponse.success("CSMS Lite 어드민 API 서버가 정상 가동 중입니다.", statusMap);
    }
}
