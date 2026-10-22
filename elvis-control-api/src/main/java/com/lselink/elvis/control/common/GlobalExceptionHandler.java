package com.lselink.elvis.control.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

/**
 * 관제 어드민 REST API 전역 예외 처리기 (Sprint 25: S25-API-0010-CO)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 잘못된 요청 인자 (400 Bad Request)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "잘못된 요청 인자입니다.";
        }
        log.warn("[GlobalException] 400 Bad Request: {}", message);
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 리소스 미존재 (404 Not Found)
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoSuchElementException(NoSuchElementException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "요청한 리소스를 찾을 수 없습니다.";
        }
        log.warn("[GlobalException] 404 Not Found: {}", message);
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.NOT_FOUND.value(), message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * DTO 유효성 검증 실패 (400 Bad Request)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        FieldError fieldError = bindingResult.getFieldError();
        String defaultMessage = "요청 값 유효성 검증에 실패하였습니다.";
        if (fieldError != null) {
            String fieldName = fieldError.getField();
            String errorMsg = fieldError.getDefaultMessage();
            defaultMessage = "[" + fieldName + "] " + errorMsg;
        }

        log.warn("[GlobalException] 400 Validation Error: {}", defaultMessage);
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.BAD_REQUEST.value(), defaultMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 서버 내부 알 수 없는 오류 (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        String errorMessage = ex.getMessage();
        log.error("[GlobalException] 500 Internal Server Error: {}", errorMessage, ex);

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "서버 내부 처리 중 오류가 발생했습니다. 관리자에게 문의하세요."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
