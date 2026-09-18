package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.dto.ChargerResponseDto;
import com.lselink.elvis.control.service.ChargerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chargers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChargerController {

    private final ChargerService chargerService;

    @GetMapping
    public ResponseEntity<List<ChargerResponseDto>> getChargers(
            @RequestParam(required = false, defaultValue = "ALL") String stId,
            @RequestParam(required = false, defaultValue = "ALL") String status) {
        return ResponseEntity.ok(chargerService.getChargers(stId, status));
    }

    @PostMapping("/{chargeBoxId}/remote-command")
    public ResponseEntity<Map<String, Object>> sendRemoteCommand(
            @PathVariable String chargeBoxId,
            @RequestBody Map<String, String> body) {
        String action = body.getOrDefault("action", "Reset");
        boolean success = chargerService.executeRemoteAction(chargeBoxId, action);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "chargeBoxId", chargeBoxId,
                "action", action,
                "message", "[" + chargeBoxId + "] 단말에 " + action + " 명령이 전달되었습니다."
        ));
    }
}
