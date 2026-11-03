package com.lselink.elvis.control.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * ClickHouse 원본 통신로그 다차원 조건 검색 DTO (거버넌스 10조 채번 표준 준수)
 * [전사 코딩 표준 준수: Lombok 전면 배제, 순수 자바 POJO 및 Builder 패턴 구현]
 */
public class RawLogSearchCondition {

    /** 충전기 결합 식별자 (8자리 정규화, 예: "00000001") */
    private String chargeBoxId;

    /** 커넥터 식별자 (1, 2) */
    private Integer connectorId;

    /** 트랜잭션 식별자 (전역 고유 단일 시퀀스) */
    private Long transactionId;

    /** 검색 시작 일시 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    /** 검색 종료 일시 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    /** 페이지 번호 (0부터 시작) */
    private int page = 0;

    /** 페이지 크기 (기본 20건, 최대 100건) */
    private int size = 20;

    public RawLogSearchCondition() {
    }

    public RawLogSearchCondition(String chargeBoxId, Integer connectorId, Long transactionId,
                                 LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        this.chargeBoxId = chargeBoxId;
        this.connectorId = connectorId;
        this.transactionId = transactionId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.page = page;
        this.size = size;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public void setChargeBoxId(String chargeBoxId) {
        this.chargeBoxId = chargeBoxId;
    }

    public Integer getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(Integer connectorId) {
        this.connectorId = connectorId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    /**
     * 오프셋 계산 (호이스팅 변수)
     */
    public int getOffset() {
        int safePage = 0;
        if (this.page > 0) {
            safePage = this.page;
        }
        int safeSize = 20;
        if (this.size > 0) {
            safeSize = Math.min(this.size, 100);
        }
        return safePage * safeSize;
    }

    /**
     * 안전한 페이지 크기 한도 반환
     */
    public int getLimit() {
        int safeLimit = 20;
        if (this.size > 0) {
            safeLimit = Math.min(this.size, 100);
        }
        return safeLimit;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String chargeBoxId;
        private Integer connectorId;
        private Long transactionId;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private int page = 0;
        private int size = 20;

        public Builder chargeBoxId(String chargeBoxId) {
            this.chargeBoxId = chargeBoxId;
            return this;
        }

        public Builder connectorId(Integer connectorId) {
            this.connectorId = connectorId;
            return this;
        }

        public Builder transactionId(Long transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder startDate(LocalDateTime startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(LocalDateTime endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public RawLogSearchCondition build() {
            return new RawLogSearchCondition(chargeBoxId, connectorId, transactionId, startDate, endDate, page, size);
        }
    }
}
