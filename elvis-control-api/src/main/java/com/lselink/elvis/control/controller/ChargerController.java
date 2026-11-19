package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.common.ApiResponse;
import com.lselink.elvis.control.common.PageResponse;
import com.lselink.elvis.control.dto.ChargerResponseDto;
import com.lselink.elvis.control.dto.ChargerSummaryDto;
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

    @GetMapping("/summary")
    public ApiResponse<ChargerSummaryDto> getChargerSummary() {
        ChargerSummaryDto summary = chargerService.getChargerSummary();
        return ApiResponse.success(summary);
    }

    @GetMapping("/paged")
    public ApiResponse<PageResponse<ChargerResponseDto>> getPagedChargers(
            @RequestParam(required = false, defaultValue = "ALL") String stId,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<ChargerResponseDto> paged = chargerService.getPagedChargers(stId, status, keyword, page, size);
        return ApiResponse.success(paged);
    }

    @GetMapping("/{chargeBoxId}")
    public ApiResponse<ChargerResponseDto> getChargerDetail(@PathVariable String chargeBoxId) {
        ChargerResponseDto dto = chargerService.getChargerById(chargeBoxId);
        return ApiResponse.success(dto);
    }

    @PostMapping("/{chargeBoxId}/remote-command")
    public ApiResponse<Map<String, Object>> sendRemoteCommand(
            @PathVariable String chargeBoxId,
            @RequestBody(required = false) Map<String, Object> body) {
        String action = "Reset";
        Integer connectorId = null;
        if (body != null) {
            Object rawAction = body.get("action");
            if (rawAction != null && !rawAction.toString().isBlank()) {
                action = rawAction.toString().trim();
            }
            Object rawConnId = body.get("connectorId");
            if (rawConnId instanceof Number num) {
                connectorId = num.intValue();
            } else if (rawConnId instanceof String str && !str.isBlank()) {
                try {
                    connectorId = Integer.parseInt(str.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        boolean success = chargerService.executeRemoteAction(chargeBoxId, action, connectorId);

        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("success", success);
        resultMap.put("chargeBoxId", chargeBoxId);
        resultMap.put("action", action);
        if (connectorId != null) {
            resultMap.put("connectorId", connectorId);
        }

        String targetLabel = (connectorId != null && connectorId > 0)
                ? "[" + chargeBoxId + " #" + connectorId + "]"
                : "[" + chargeBoxId + "]";
        String message = targetLabel + " 단말에 " + action + " 명령이 전달되었습니다.";
        return ApiResponse.success(message, resultMap);
    }
}
