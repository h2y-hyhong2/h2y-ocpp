package com.lselink.elvis.connect.sync.domain;

/**
 * 결제 및 과금 상태 Enum
 * [전사 코딩 표준 준수: Lombok 배제, 명시적 생성자 및 Getter 구현]
 */
public enum PaymentStatus {

    PENDING("결제 대기"),
    PAID("결제 완료"),
    FAILED("결제 실패"),
    CANCELLED("과금 취소");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
