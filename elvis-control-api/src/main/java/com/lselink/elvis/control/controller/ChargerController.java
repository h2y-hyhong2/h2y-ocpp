package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.common.ApiResponse;
import com.lselink.elvis.control.dto.ChargerResponseDto;
import com.lselink.elvis.control.service.ChargerService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chargers")
public class ChargerController {

    private final ChargerService chargerService;

    public ChargerController(ChargerService chargerService) {
        this.chargerService = chargerService;
    }

    @GetMapping
    public ApiResponse<List<ChargerResponseDto>> getChargers(
            @RequestParam(required = false, defaultValue = "ALL") String stId,
            @RequestParam(required = false, defaultValue = "ALL") String status) {
        List<ChargerResponseDto> list = chargerService.getChargers(stId, status);
        return ApiResponse.success(list);
    }

    @PostMapping("/{chargeBoxId}/remote-command")
    public ApiResponse<Map<String, Object>> sendRemoteCommand(
            @PathVariable String chargeBoxId,
            @RequestBody Map<String, String> body) {
        String action = body.getOrDefault("action", "Reset");
        boolean success = chargerService.executeRemoteAction(chargeBoxId, action);

        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("success", success);
        resultMap.put("chargeBoxId", chargeBoxId);
        resultMap.put("action", action);

        String message = "[" + chargeBoxId + "] 단말에 " + action + " 명령이 전달되었습니다.";
        return ApiResponse.success(message, resultMap);
    }
}

