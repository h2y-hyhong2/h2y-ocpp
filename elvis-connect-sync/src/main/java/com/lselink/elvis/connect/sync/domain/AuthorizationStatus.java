package com.lselink.elvis.connect.sync.domain;

/**
 * OCPP 1.6-J Authorize.conf 응답 상태 열거형 (idTagInfo.status)
 */
public enum AuthorizationStatus {
    Accepted("정상 인증 승인"),
    Blocked("차단 / 정지 / 분실 카드"),
    Expired("유효기간 만료"),
    Invalid("미등록 또는 삭제된 카드"),
    ConcurrentTx("동시 다중 충전 세션 불가");

    private final String description;

    AuthorizationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAccepted() {
        return this == Accepted;
    }
}
