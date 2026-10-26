package com.lselink.elvis.control.service;

import com.lselink.elvis.control.common.PageResponse;
import com.lselink.elvis.control.dto.RawLogResponseDto;
import com.lselink.elvis.control.dto.RawLogSearchCondition;
import com.lselink.elvis.control.repository.rawlog.RawLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * ClickHouse 원본 통신로그 조회 서비스 구현체 (Graceful Degradation & Clean Code 표준 준수)
 */
@Slf4j
@Service
public class RawLogServiceImpl implements RawLogService {

    private final RawLogRepository rawLogRepository;

    public RawLogServiceImpl(
            @Autowired(required = false) RawLogRepository rawLogRepository) {
        this.rawLogRepository = rawLogRepository;
    }

    @Override
    public PageResponse<RawLogResponseDto> searchRawLogs(RawLogSearchCondition condition) {
        // [거버넌스 9조] 설명 변수 호이스팅
        int pageNumber = 0;
        int pageSize = 20;
        if (condition != null) {
            pageNumber = condition.getPage();
            pageSize = condition.getSize();
        }

        // [Graceful Degradation] ClickHouse 저장소 미탑재 또는 비활성화 시 안전한 빈 페이징 반환
        if (rawLogRepository == null) {
            log.warn("⚠️ [RawLogService] ClickHouse 저장소가 비활성화되어 빈 페이징 응답을 반환합니다.");
            return PageResponse.of(Collections.emptyList(), pageNumber, pageSize, 0L);
        }

        try {
            // [거버넌스 9조] 외부 I/O 변수 선제 분리
            long totalCount = rawLogRepository.countByCondition(condition);

            // [거버넌스 9조] 3항 연산자 지양 및 명시적 if-else문 적용
            List<RawLogResponseDto> logList = Collections.emptyList();
            if (totalCount > 0) {
                logList = rawLogRepository.findPagedByCondition(condition);
            }

            return PageResponse.of(logList, pageNumber, pageSize, totalCount);
        } catch (DataAccessException ex) {
            // [Graceful Degradation] ClickHouse 단절 또는 네트워크 장애 시 크래시 방지 및 빈 페이징 폴백
            log.warn("⚠️ [RawLogService] ClickHouse 원본 통신로그 조회 실패 (Graceful Degradation 작동) - 사유: {}", ex.getMessage());
            return PageResponse.of(Collections.emptyList(), pageNumber, pageSize, 0L);
        }
    }

    @Override
    public List<RawLogResponseDto> getLatestLogs(String chargeBoxId, int limit) {
        int safeLimit = 20;
        if (limit > 0 && limit <= 100) {
            safeLimit = limit;
        }

        if (rawLogRepository == null) {
            log.warn("⚠️ [RawLogService] ClickHouse 저장소가 비활성화되어 빈 최근 통신로그 목록을 반환합니다.");
            return Collections.emptyList();
        }

        try {
            // [거버넌스 9조] 외부 I/O 변수 분리
            List<RawLogResponseDto> latestList = rawLogRepository.findLatestByChargeBoxId(chargeBoxId, safeLimit);
            if (latestList == null) {
                return Collections.emptyList();
            }
            return latestList;
        } catch (DataAccessException ex) {
            log.warn("⚠️ [RawLogService] 최근 통신로그 조회 실패 (Graceful Degradation 작동) - chargeBoxId: {}, 사유: {}",
                     chargeBoxId, ex.getMessage());
            return Collections.emptyList();
        }
    }
}
