package com.lselink.elvis.connect.sync.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 전력 부하 구분 Enum (경부하 / 중부하 / 최대부하)
 */
@Getter
@RequiredArgsConstructor
public enum LoadType {

    OFF_PEAK("경부하 (심야 최저 단가)"),
    MID_PEAK("중부하 (주간 표준 단가)"),
    ON_PEAK("최대부하 (피크 최고 단가)");

    private final String description;
}
