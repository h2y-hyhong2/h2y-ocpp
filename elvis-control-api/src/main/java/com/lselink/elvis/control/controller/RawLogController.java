package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.common.ApiResponse;
import com.lselink.elvis.control.common.PageResponse;
import com.lselink.elvis.control.dto.RawLogResponseDto;
import com.lselink.elvis.control.dto.RawLogSearchCondition;
import com.lselink.elvis.control.service.RawLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ClickHouse 원본 통신로그 조회 REST 컨트롤러 (Sprint 30: S30-DATA-0030-CO)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/logs/raw")
@RequiredArgsConstructor
public class RawLogController {

    private final RawLogService rawLogService;

    /**
     * 원본 통신로그 다차원 조건 검색 및 서버사이드 페이징
     * GET /api/v1/logs/raw/paged
     *
     * @param condition 검색 조건 DTO (chargeBoxId, connectorId, transactionId, 기간 등)
     * @return 표준 페이징 응답 객체
     */
    @GetMapping("/paged")
    public ApiResponse<PageResponse<RawLogResponseDto>> searchRawLogsPaged(RawLogSearchCondition condition) {
        log.info("📡 [RawLogController] 원본 통신로그 페이징 검색 요청 - chargeBoxId: {}, connectorId: {}, page: {}, size: {}",
                condition.getChargeBoxId(), condition.getConnectorId(), condition.getPage(), condition.getSize());

        // [거버넌스 9조] 외부 서비스 I/O 호출 변수 분리
        PageResponse<RawLogResponseDto> pageResult = rawLogService.searchRawLogs(condition);
        return ApiResponse.ok(pageResult);
    }

    /**
     * 특정 충전기 최근 원본 통신로그 N건 고속 조회 (모달 및 실시간 뷰어용)
     * GET /api/v1/logs/raw/latest?chargeBoxId=00000001&limit=20
     *
     * @param chargeBoxId 충전기 결합 식별자 (8자리 정규화)
     * @param limit 조회 한도 (기본 20건, 최대 100건)
     * @return 최근 통신로그 목록
     */
    @GetMapping("/latest")
    public ApiResponse<List<RawLogResponseDto>> getLatestLogs(
            @RequestParam(name = "chargeBoxId") String chargeBoxId,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        log.info("📡 [RawLogController] 최근 원본 통신로그 조회 요청 - chargeBoxId: {}, limit: {}", chargeBoxId, limit);

        // [거버넌스 9조] 외부 서비스 I/O 호출 변수 분리
        List<RawLogResponseDto> latestList = rawLogService.getLatestLogs(chargeBoxId, limit);
        return ApiResponse.ok(latestList);
    }
}
