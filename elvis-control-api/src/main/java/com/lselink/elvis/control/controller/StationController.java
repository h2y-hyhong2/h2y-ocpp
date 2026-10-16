package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.dto.StationResponseDto;
import com.lselink.elvis.control.entity.CorpEntity;
import com.lselink.elvis.control.service.StationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping("/corps")
    public ResponseEntity<List<com.lselink.elvis.control.dto.CorpResponseDto>> getCorps() {
        return ResponseEntity.ok(stationService.getAllCorps());
    }

    @GetMapping("/stations")
    public ResponseEntity<List<StationResponseDto>> getStations(@RequestParam(required = false, defaultValue = "ALL") String corpId) {
        return ResponseEntity.ok(stationService.getAllStations(corpId));
    }
}
