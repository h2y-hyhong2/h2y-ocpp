package com.lselink.elvis.connect.sync.domain;

/**
 * DLT 메시지 처리 생명주기 상태 열거형 (Sprint 24: S24-SYNC-0100-CO)
 * <ul>
 *   <li>PENDING: 격리 후 운영자 확인 및 재처리 대기 상태</li>
 *   <li>REPLAYED: 원본 인바운드 토픽으로 재주입 성공 완료 상태</li>
 *   <li>DISCARDED: 운영자에 의해 영구 폐기 처리된 상태 (악성 패킷)</li>
 *   <li>FAILED: 재처리 시도 중 다시 오류가 발생하여 실패한 상태</li>
 * </ul>
 */
public enum DltStatus {
    PENDING("재처리 대기"),
    REPLAYED("재처리 완료"),
    DISCARDED("영구 폐기"),
    FAILED("재처리 실패");

    private final String description;

    DltStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
