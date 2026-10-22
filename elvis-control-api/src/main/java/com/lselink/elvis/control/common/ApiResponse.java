package com.lselink.elvis.control.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * CSMS Lite 관제 어드민 REST API 표준 공통 응답 래퍼 (Sprint 25: S25-API-0010-CO)
 *
 * @param <T> 페이로드 데이터 타입
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final int code;
    private final String message;
    private final T data;
    private final Instant timestamp;

    public ApiResponse(boolean success, int code, String message, T data, Instant timestamp) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
    }

    /**
     * 성공 응답 (데이터 포함)
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 200, "성공", data, Instant.now());
    }

    /**
     * 성공 응답 (메시지 및 데이터 포함)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, 200, message, data, Instant.now());
    }

    /**
     * 성공 응답 (메시지만 포함, 데이터 없음)
     */
    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, 200, message, null, Instant.now());
    }

    /**
     * 에러 응답 (상태 코드 및 에러 메시지)
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(false, code, message, null, Instant.now());
    }

    public boolean isSuccess() {
        return success;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
