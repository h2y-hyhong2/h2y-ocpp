package com.lselink.elvis.connect.sync.domain;

/**
 * 장애 심각도 열거형 (Sprint 23: S23-SYNC-0060-CO)
 * <ul>
 *   <li>CRITICAL: 화재, 감전, 안전 직결 (긴급 차단 필요)</li>
 *   <li>MAJOR: 충전 불가 하드웨어 결함 (현장 출동 점검 필요)</li>
 *   <li>MINOR: 충전 가능 경고 또는 통신 일시 지연 (원격 모니터링)</li>
 *   <li>INFO: 정상 복구 알림</li>
 * </ul>
 */
public enum FaultSeverity {
    CRITICAL("긴급 차단"),
    MAJOR("현장 점검"),
    MINOR("경고 관제"),
    INFO("정상 복구");

    private final String description;

    FaultSeverity(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * OCPP 표준 에러코드로부터 심각도를 판정하는 팩토리 메서드
     *
     * @param errorCode OCPP 1.6 ChargePointErrorCode
     * @return FaultSeverity
     */
    public static FaultSeverity fromErrorCode(String errorCode) {
        if (errorCode == null) {
            return MINOR;
        }

        switch (errorCode) {
            case "GroundFailure", "OverVoltage", "HighTemperature", "EVCommunicationError" -> {
                return CRITICAL;
            }
            case "ConnectorLockFailure", "InternalError", "PowerMeterFailure", "PowerSwitchFailure", "OverCurrentFailure" -> {
                return MAJOR;
            }
            case "NoError" -> {
                return INFO;
            }
            default -> {
                return MINOR;
            }
        }
    }
}
