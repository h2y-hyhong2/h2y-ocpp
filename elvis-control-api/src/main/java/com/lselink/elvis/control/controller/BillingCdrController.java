package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.common.ApiResponse;
import com.lselink.elvis.control.common.PageResponse;
import com.lselink.elvis.control.dto.CdrResponseDto;
import com.lselink.elvis.control.dto.CdrSearchCondition;
import com.lselink.elvis.control.dto.CdrSummaryDto;
import com.lselink.elvis.control.service.CdrService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 충전 거래 이력(CDR) 관제 및 정산 REST API 컨트롤러
 * 거버넌스 9조 Clean Code 및 관심사 분리(SoC) 준수
 */
@RestController
@RequestMapping("/api/v1/cdr")
public class BillingCdrController {

    private final CdrService cdrService;

    public BillingCdrController(CdrService cdrService) {
        this.cdrService = cdrService;
    }

    /**
     * 1) 다차원 조건 검색 및 서버사이드 페이징 조회
     */
    @GetMapping("/paged")
    public ApiResponse<PageResponse<CdrResponseDto>> getCdrPaged(
            @ModelAttribute CdrSearchCondition condition,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PageResponse<CdrResponseDto> response = cdrService.getPagedCdr(condition, page, size);
        return ApiResponse.success(response);
    }

    /**
     * 2) 조건 검색 4대 KPI 요약 통계 조회 (단일 쿼리)
     */
    @GetMapping("/summary")
    public ApiResponse<CdrSummaryDto> getCdrSummary(
            @ModelAttribute CdrSearchCondition condition) {
        CdrSummaryDto summary = cdrService.getCdrSummary(condition);
        return ApiResponse.success(summary);
    }

    /**
     * 3) 대용량 이력 CSV 스트리밍 다운로드 (UTF-8 with BOM)
     */
    @GetMapping("/export")
    public void exportCdrCsv(
            @ModelAttribute CdrSearchCondition condition,
            HttpServletResponse response) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "cdr_records_" + timestamp + ".csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        List<CdrResponseDto> items = cdrService.getAllFilteredCdr(condition);

        PrintWriter writer = response.getWriter();
        // Excel 한글 깨짐 방지를 위한 UTF-8 BOM 삽입
        writer.write('\uFEFF');

        // CSV 헤더
        writer.println("트랜잭션ID,충전기ID,충전소ID,충전소명,커넥터ID,회원태그,시작시각,종료시각,충전량(kWh),단가(원),결제금액(원),결제상태,종료사유,충전시간(분)");

        for (CdrResponseDto item : items) {
            String line = String.format("%d,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                    item.getTransactionId(),
                    formatCsvCell(item.getChargeBoxId()),
                    formatCsvCell(item.getStationId()),
                    formatCsvCell(item.getStationName()),
                    item.getConnectorId(),
                    formatCsvCell(item.getUserTag()),
                    formatCsvCell(item.getStartTime()),
                    formatCsvCell(item.getStopTime()),
                    item.getKwh(),
                    item.getUnitPrice(),
                    item.getTotalAmount(),
                    formatCsvCell(item.getPaymentStatus()),
                    formatCsvCell(item.getStopReason()),
                    item.getDurationMinutes() != null ? String.valueOf(item.getDurationMinutes()) : "-"
            );
            writer.println(line);
        }

        writer.flush();
    }

    /**
     * 4) 기존 레거시 프로토타입 하위 호환 엔드포인트
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getCdrRecords() {
        CdrSearchCondition condition = new CdrSearchCondition();
        PageResponse<CdrResponseDto> paged = cdrService.getPagedCdr(condition, 0, 100);
        CdrSummaryDto summary = cdrService.getCdrSummary(condition);

        Map<String, Object> summaryMap = new LinkedHashMap<>();
        summaryMap.put("totalCount", summary.getTotalCount());
        summaryMap.put("totalKwh", summary.getTotalKwh());
        summaryMap.put("totalAmount", summary.getTotalAmount());
        summaryMap.put("avgMinutes", summary.getAvgMinutes());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", paged.getContent());
        result.put("summary", summaryMap);

        return ApiResponse.success(result);
    }

    private String formatCsvCell(String value) {
        if (value == null) {
            return "\"\"";
        }
        String sanitized = value;
        if (!sanitized.isEmpty()) {
            char first = sanitized.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
                sanitized = "'" + sanitized;
            }
        }
        return "\"" + sanitized.replace("\"", "\"\"") + "\"";
    }
}
