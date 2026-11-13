package com.lselink.elvis.connect.sync.domain;

import java.time.LocalDate;
import java.time.Month;

/**
 * 한전/환경부 계절 구분 Enum
 * [전사 코딩 표준 준수: Lombok 배제, 명시적 생성자 및 Getter 구현]
 */
public enum SeasonType {

    SPRING_AUTUMN("봄/가을 (3~6월, 9~10월)"),
    SUMMER("여름철 (7~8월)"),
    WINTER("겨울철 (11~2월)");

    private final String description;

    SeasonType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 일자 기준으로 계절 판정
     */
    public static SeasonType from(LocalDate date) {
        Month month = date.getMonth();
        return switch (month) {
            case JULY, AUGUST -> SUMMER;
            case NOVEMBER, DECEMBER, JANUARY, FEBRUARY -> WINTER;
            default -> SPRING_AUTUMN; // 3, 4, 5, 6, 9, 10월
        };
    }
}
