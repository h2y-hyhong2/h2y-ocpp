package com.lselink.elvis.control.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * ClickHouse 원본 통신로그 다차원 조건 검색 DTO (거버넌스 10조 채번 표준 준수)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    @Builder.Default
    private int page = 0;

    /** 페이지 크기 (기본 20건, 최대 100건) */
    @Builder.Default
    private int size = 20;

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
}
