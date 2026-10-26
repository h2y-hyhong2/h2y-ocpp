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
import java.math.BigDecimal;
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
            String line = String.format("%d,%s,%s,\"%s\",%d,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                    item.getTransactionId(),
                    escapeCsv(item.getChargeBoxId()),
                    escapeCsv(item.getStationId()),
                    escapeCsv(item.getStationName()),
                    item.getConnectorId(),
                    escapeCsv(item.getUserTag()),
                    item.getStartTime(),
                    item.getStopTime(),
                    item.getKwh(),
                    item.getUnitPrice(),
                    item.getTotalAmount(),
                    escapeCsv(item.getPaymentStatus()),
                    escapeCsv(item.getStopReason()),
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

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"");
    }

    /**
     * 기존 코드 및 테스트 호환용 레거시 DTO 정의
     */
    public static class CdrItemDto {
        private Long transactionId;
        private String userTag;
        private String chargeBoxId;
        private String stationName;
        private String startTime;
        private String stopTime;
        private BigDecimal kwh;
        private BigDecimal unitPrice;
        private BigDecimal totalAmount;
        private String paymentStatus;

        public CdrItemDto() {}

        public CdrItemDto(Long transactionId, String userTag, String chargeBoxId, String stationName,
                          String startTime, String stopTime, BigDecimal kwh, BigDecimal unitPrice,
                          BigDecimal totalAmount, String paymentStatus) {
            this.transactionId = transactionId;
            this.userTag = userTag;
            this.chargeBoxId = chargeBoxId;
            this.stationName = stationName;
            this.startTime = startTime;
            this.stopTime = stopTime;
            this.kwh = kwh;
            this.unitPrice = unitPrice;
            this.totalAmount = totalAmount;
            this.paymentStatus = paymentStatus;
        }

        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
        public String getUserTag() { return userTag; }
        public void setUserTag(String userTag) { this.userTag = userTag; }
        public String getChargeBoxId() { return chargeBoxId; }
        public void setChargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; }
        public String getStationName() { return stationName; }
        public void setStationName(String stationName) { this.stationName = stationName; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getStopTime() { return stopTime; }
        public void setStopTime(String stopTime) { this.stopTime = stopTime; }
        public BigDecimal getKwh() { return kwh; }
        public void setKwh(BigDecimal kwh) { this.kwh = kwh; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    }
}
