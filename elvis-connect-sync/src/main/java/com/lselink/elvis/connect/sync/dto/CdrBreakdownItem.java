package com.lselink.elvis.connect.sync.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lselink.elvis.connect.sync.domain.DayType;
import com.lselink.elvis.connect.sync.domain.LoadType;
import com.lselink.elvis.connect.sync.domain.SeasonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * CDR 원장 내 시간대별 분할 과금 상세 내역 Value Object (JSON 저장용)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CdrBreakdownItem {

    /**
     * 슬롯 시작 일시 (문자열: yyyy-MM-dd HH:mm:ss)
     */
    private String slotStart;

    /**
     * 슬롯 종료 일시 (문자열: yyyy-MM-dd HH:mm:ss)
     */
    private String slotEnd;

    /**
     * 계절 구분
     */
    private SeasonType seasonType;

    /**
     * 요일 구분
     */
    private DayType dayType;

    /**
     * 부하 구분 (경부하, 중부하, 최대부하)
     */
    private LoadType loadType;

    /**
     * 슬롯 소요 시간 (초 단위)
     */
    private long durationSeconds;

    /**
     * 해당 구간 적용 전력량 단가 (원/kWh)
     */
    private BigDecimal unitPrice;

    /**
     * 해당 구간 배분 충전량 (kWh, 소수점 3자리)
     */
    private BigDecimal kwh;

    /**
     * 해당 구간 산출 요금 (세전, 원, 소수점 2자리)
     */
    private BigDecimal fee;
}
