package com.lselink.elvis.connect.sync.domain;

import lombok.Getter;

/**
 * 충전 거래 세션 생명주기 상태 열거형 (Sprint 19: S19-SYNC-0040-CO)
 */
@Getter
public enum SessionStatus {

    STARTED("충전 시작 수신"),
    CHARGING("충전 진행 중 (계측값 수신)"),
    STOPPED("충전 종료 (StopTransaction 수신)"),
    COMPLETED("정산 및 과금 CDR 생성 완료"),
    ABORTED("비정상 중단 / 즉시 취소"),
    FORCE_CLOSED("타임아웃 강제 마감");

    private final String description;

    SessionStatus(String description) {
        this.description = description;
    }

    /**
     * 현재 활성화(진행 중) 세션 여부 판정
     */
    public boolean isActive() {
        return this == STARTED || this == CHARGING;
    }

    /**
     * 종결(마감) 상태 여부 판정
     */
    public boolean isTerminated() {
        return this == STOPPED || this == COMPLETED || this == ABORTED || this == FORCE_CLOSED;
    }
}
