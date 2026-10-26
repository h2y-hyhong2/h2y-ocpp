package com.lselink.elvis.control.service;

import com.lselink.elvis.control.common.PageResponse;
import com.lselink.elvis.control.dto.CdrResponseDto;
import com.lselink.elvis.control.dto.CdrSearchCondition;
import com.lselink.elvis.control.dto.CdrSummaryDto;

import java.util.List;

/**
 * 충전 거래 이력(CDR) 비즈니스 서비스 인터페이스
 */
public interface CdrService {

    /**
     * 다차원 조건 검색 및 서버사이드 페이징 조회
     */
    PageResponse<CdrResponseDto> getPagedCdr(CdrSearchCondition condition, int page, int size);

    /**
     * 다차원 조건 검색 단일 쿼리 요약 통계(KPI) 조회
     */
    CdrSummaryDto getCdrSummary(CdrSearchCondition condition);

    /**
     * 조건 검색 필터링된 전체 목록 조회 (CSV 내보내기용)
     */
    List<CdrResponseDto> getAllFilteredCdr(CdrSearchCondition condition);
}
