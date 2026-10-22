package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.common.ApiResponse;
import com.lselink.elvis.control.common.PageResponse;
import com.lselink.elvis.control.dto.CorpResponseDto;
import com.lselink.elvis.control.dto.StationResponseDto;
import com.lselink.elvis.control.service.StationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping("/corps")
    public ApiResponse<List<CorpResponseDto>> getCorps() {
        List<CorpResponseDto> list = stationService.getAllCorps();
        return ApiResponse.success(list);
    }

    @GetMapping("/stations")
    public ApiResponse<List<StationResponseDto>> getStations(
            @RequestParam(required = false, defaultValue = "ALL") String corpId) {
        List<StationResponseDto> list = stationService.getAllStations(corpId);
        return ApiResponse.success(list);
    }

    @GetMapping("/stations/paged")
    public ApiResponse<PageResponse<StationResponseDto>> getPagedStations(
            @RequestParam(required = false, defaultValue = "ALL") String corpId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<StationResponseDto> paged = stationService.getPagedStations(corpId, keyword, page, size);
        return ApiResponse.success(paged);
    }

    @GetMapping("/stations/{stId}")
    public ApiResponse<StationResponseDto> getStationDetail(@PathVariable String stId) {
        StationResponseDto dto = stationService.getStationById(stId);
        return ApiResponse.success(dto);
    }
}
