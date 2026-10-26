package com.lselink.elvis.control.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 충전 거래 이력(CDR) 다차원 검색 조건 DTO
 */
public class CdrSearchCondition {

    private String startDate;
    private String endDate;
    private String stId;
    private String chargeBoxId;
    private String userTag;
    private String paymentStatus;
    private String keyword;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public CdrSearchCondition() {
    }

    public CdrSearchCondition(String startDate, String endDate, String stId, String chargeBoxId,
                              String userTag, String paymentStatus, String keyword) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.stId = stId;
        this.chargeBoxId = chargeBoxId;
        this.userTag = userTag;
        this.paymentStatus = paymentStatus;
        this.keyword = keyword;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getStId() {
        return stId;
    }

    public void setStId(String stId) {
        this.stId = stId;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public void setChargeBoxId(String chargeBoxId) {
        this.chargeBoxId = chargeBoxId;
    }

    public String getUserTag() {
        return userTag;
    }

    public void setUserTag(String userTag) {
        this.userTag = userTag;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * 시작 일시 파싱 (문자열 -> LocalDateTime)
     * 거버넌스 9조: 명시적 if-else 및 널 안전성 확보
     */
    public LocalDateTime getParsedStartDate() {
        if (startDate == null) {
            return null;
        }
        String trimmed = startDate.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            if (trimmed.length() == 10) {
                LocalDate date = LocalDate.parse(trimmed, DATE_FORMATTER);
                return date.atStartOfDay();
            } else {
                return LocalDateTime.parse(trimmed, DATE_TIME_FORMATTER);
            }
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 종료 일시 파싱 (문자열 -> LocalDateTime, 날짜만 입력 시 당일 23:59:59.999999999)
     * 거버넌스 9조: 명시적 if-else 및 널 안전성 확보
     */
    public LocalDateTime getParsedEndDate() {
        if (endDate == null) {
            return null;
        }
        String trimmed = endDate.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            if (trimmed.length() == 10) {
                LocalDate date = LocalDate.parse(trimmed, DATE_FORMATTER);
                return date.atTime(LocalTime.MAX);
            } else {
                return LocalDateTime.parse(trimmed, DATE_TIME_FORMATTER);
            }
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "CdrSearchCondition{" +
                "startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", stId='" + stId + '\'' +
                ", chargeBoxId='" + chargeBoxId + '\'' +
                ", userTag='" + userTag + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", keyword='" + keyword + '\'' +
                '}';
    }
}
