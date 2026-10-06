package com.lselink.elvis.connect.sync.domain;

import lombok.Getter;

/**
 * OCPP 1.6-J 표준 충전 종료 사유 (Reason 11종) (Sprint 19: S19-SYNC-0040-CO)
 */
@Getter
public enum StopReason {

    EmergencyStop("비상 정지 버튼 동작"),
    EVDisconnected("차량 커넥터 분리"),
    HardReset("단말 하드웨어 리셋"),
    Local("충전기 로컬 조작 종료"),
    Other("기타 사유"),
    PowerLoss("전원 차단/정전"),
    Reboot("충전기 재부팅"),
    Remote("CSMS 원격 중지 명령"),
    SoftReset("단말 소프트웨어 리셋"),
    UnlockCommand("커넥터 잠금 해제 명령"),
    DeAuthorized("인증 거절 / 회원 무효");

    private final String description;

    StopReason(String description) {
        this.description = description;
    }

    /**
     * 문자열 값으로부터 안전하게 StopReason 매핑 (대소문자 무관, null/미일치 시 Other)
     */
    public static StopReason fromValue(String value) {
        if (value == null || value.isBlank()) {
            return Other;
        }
        for (StopReason reason : values()) {
            if (reason.name().equalsIgnoreCase(value.trim())) {
                return reason;
            }
        }
        return Other;
    }
}
