package com.lselink.elvis.control.repository;

import com.lselink.elvis.control.dto.CdrSearchCondition;
import com.lselink.elvis.control.dto.CdrSummaryDto;
import com.lselink.elvis.control.entity.TransactionCdrEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 충전 거래 이력(CDR) QueryDSL 커스텀 리포지토리 인터페이스
 */
public interface TransactionCdrRepositoryCustom {

    Page<TransactionCdrEntity> searchCdr(CdrSearchCondition condition, Pageable pageable);

    CdrSummaryDto getCdrSummary(CdrSearchCondition condition);

    List<TransactionCdrEntity> findAllByCondition(CdrSearchCondition condition);
}
