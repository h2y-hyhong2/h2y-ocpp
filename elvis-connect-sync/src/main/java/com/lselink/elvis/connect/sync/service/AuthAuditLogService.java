package com.lselink.elvis.connect.sync.service;

import com.lselink.elvis.connect.sync.dto.IdTagInfo;

/**
 * Authorize 인증 감사 이력 비동기 로깅 인터페이스 (Sprint 20: S20-SYNC-0070-DE)
 * <p>
 * 메인 인증 응답 파이프라인의 레이턴시(0ms 추가 지연)에 영향을 주지 않도록
 * Fire-and-Forget 비동기 방식으로 이력을 영속화합니다.
 * </p>
 */
public interface AuthAuditLogService {

    /**
     * 인증 결과 감사 이력 비동기 기록
     *
     * @param chargeBoxId 충전기 식별자
     * @param messageId   OCPP MessageId
     * @param idTag       요청 태그 식별자
     * @param idTagInfo   인증 결과 정보
     * @param elapsedNano 총 판정 소요시간 (나노초)
     */
    void logAuthAsync(String chargeBoxId, String messageId, String idTag, IdTagInfo idTagInfo, long elapsedNano);
}
