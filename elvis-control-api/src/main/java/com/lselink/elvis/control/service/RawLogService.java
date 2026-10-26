package com.lselink.elvis.control.service;

import com.lselink.elvis.control.common.PageResponse;
import com.lselink.elvis.control.dto.RawLogResponseDto;
import com.lselink.elvis.control.dto.RawLogSearchCondition;

import java.util.List;

/**
 * ClickHouse 원본 통신로그 조회 서비스 인터페이스
 */
public interface RawLogService {

    /**
     * 다차원 조건 검색 및 서버사이드 페이징 조회
     *
     * @param condition 검색 조건 DTO
     * @return 페이징된 원본 통신로그 응답
     */
    PageResponse<RawLogResponseDto> searchRawLogs(RawLogSearchCondition condition);

    /**
     * 특정 충전기 최근 통신로그 N건 고속 조회
     *
     * @param chargeBoxId 충전기 결합 식별자 (8자리)
     * @param limit 조회 한도
     * @return 최근 통신로그 목록
     */
    List<RawLogResponseDto> getLatestLogs(String chargeBoxId, int limit);
}
