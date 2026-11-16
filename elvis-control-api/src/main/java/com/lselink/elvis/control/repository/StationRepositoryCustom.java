package com.lselink.elvis.control.repository;

import com.lselink.elvis.control.entity.StationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 충전소 마스터 QueryDSL 커스텀 리포지토리 인터페이스
 */
public interface StationRepositoryCustom {

    Page<StationEntity> searchStations(String corpId, String keyword, Pageable pageable);
}
