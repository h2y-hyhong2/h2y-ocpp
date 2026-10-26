package com.lselink.elvis.control.repository.rawlog;

import com.lselink.elvis.control.dto.RawLogResponseDto;
import com.lselink.elvis.control.dto.RawLogSearchCondition;

import java.util.List;

/**
 * ClickHouse 원본 통신로그 조회 Repository 인터페이스
 */
public interface RawLogRepository {

    /**
     * 조건에 일치하는 전체 레코드 건수 조회 (ClickHouse count() 최적화)
     *
     * @param condition 검색 조건 DTO
     * @return 총 건수
     */
    long countByCondition(RawLogSearchCondition condition);

    /**
     * 조건에 일치하는 페이징 목록 조회 (지연 인출 Late Materialization 적용)
     *
     * @param condition 검색 조건 DTO
     * @return 페이징된 원본 통신로그 목록
     */
    List<RawLogResponseDto> findPagedByCondition(RawLogSearchCondition condition);

    /**
     * 특정 충전기 최근 통신로그 N건 고속 조회 (모달 및 실시간 뷰어용)
     *
     * @param chargeBoxId 충전기 결합 식별자 (8자리)
     * @param limit 조회 한도
     * @return 최근 통신로그 목록
     */
    List<RawLogResponseDto> findLatestByChargeBoxId(String chargeBoxId, int limit);
}
