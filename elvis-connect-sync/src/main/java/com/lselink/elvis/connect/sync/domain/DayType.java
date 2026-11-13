package com.lselink.elvis.connect.sync.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * 요일 구분 Enum (한전 TOU 단가 체계 기준)
 * [전사 코딩 표준 준수: Lombok 배제, 명시적 생성자 및 Getter 구현]
 */
public enum DayType {

    WEEKDAY("주중 (월~금)"),
    SATURDAY("토요일"),
    HOLIDAY("일요일 및 공휴일");

    private final String description;

    DayType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 일자 기준으로 요일 타입 판정
     */
    public static DayType from(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SUNDAY) {
            return HOLIDAY;
        } else if (dow == DayOfWeek.SATURDAY) {
            return SATURDAY;
        } else {
            return WEEKDAY;
        }
    }
}
