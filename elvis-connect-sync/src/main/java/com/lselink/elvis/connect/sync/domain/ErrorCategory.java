package com.lselink.elvis.connect.sync.domain;

/**
 * DLT 오류 분류 열거형 (Sprint 24: S24-SYNC-0100-CO)
 * <ul>
 *   <li>NON_RETRYABLE: 역직렬화 실패, JSON 문법 오류, 비표준 스키마 등 즉시 격리 대상</li>
 *   <li>TRANSIENT: DB 락 타임아웃, 커넥션 풀 일시 고갈, 네트워크 지연 등 지수 백오프 재시도 대상</li>
 *   <li>BUSINESS_LOGIC: 지원 불가 OCPP Action 또는 권한/검증 실패 등 비즈니스 예외</li>
 * </ul>
 */
public enum ErrorCategory {
    NON_RETRYABLE("즉시 격리 (역직렬화/문법 오류)"),
    TRANSIENT("지수 백오프 재시도 (인프라 일시 경합)"),
    BUSINESS_LOGIC("비즈니스 예외 (지원 불가 Action)");

    private final String description;

    ErrorCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 예외 클래스 타입에 따른 오류 분류 판정
     *
     * @param ex 발생한 예외
     * @return ErrorCategory
     */
    public static ErrorCategory classify(Exception ex) {
        if (ex == null) {
            return NON_RETRYABLE;
        }

        String exName = ex.getClass().getName();
        String exMsg = ex.getMessage();
        if (exMsg == null) {
            exMsg = "";
        }

        // 역직렬화 / JSON 문법 / 파싱 에러 -> NON_RETRYABLE
        if (exName.contains("Json") || exName.contains("Parse") || exName.contains("Deserialization")) {
            return NON_RETRYABLE;
        }

        // DB 락 / 타임아웃 / 커넥션 고갈 -> TRANSIENT
        if (exName.contains("Lock") || exName.contains("Timeout") || exName.contains("Deadlock") || exName.contains("Connection")) {
            return TRANSIENT;
        }

        // 기본적으로 비즈니스 또는 미분류 예외
        return BUSINESS_LOGIC;
    }
}
