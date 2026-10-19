package com.lselink.elvis.connect.sync.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 결제 및 과금 상태 Enum
 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    PENDING("결제 대기"),
    PAID("결제 완료"),
    FAILED("결제 실패"),
    CANCELLED("과금 취소");

    private final String description;
}
