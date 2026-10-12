package com.lselink.elvis.connect.sync.service;

import com.lselink.elvis.connect.sync.ocpp.OcppParsedMessage;

/**
 * OCPP 1.6-J Authorize 전문 처리 서비스 인터페이스 (Sprint 20: S20-SYNC-0070-DE)
 * <p>
 * Authorize.req 인입 시 초고속 회원 인증 판정 후 즉시 Outbound CALLRESULT를 하향 발행하고,
 * 감사 이력 적재는 비동기로 분리 수행합니다.
 * </p>
 */
public interface AuthorizeService {

    /**
     * Authorize 메시지 수신 및 논블로킹 응답 처리
     *
     * @param msg 파싱된 OCPP Authorize 메시지
     */
    void handleAuthorize(OcppParsedMessage msg);
}
