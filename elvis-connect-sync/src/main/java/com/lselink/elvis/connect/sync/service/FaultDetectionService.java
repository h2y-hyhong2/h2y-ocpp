package com.lselink.elvis.connect.sync.service;

import com.lselink.elvis.connect.sync.ocpp.OcppParsedMessage;

/**
 * 충전기 실시간 장애 감지 및 자동 복구 서비스 (Sprint 23: S23-SYNC-0060-CO)
 * <p>
 * StatusNotification 전문을 분석하여 장애 발생 판정, Flapping 방어 필터링, 원장 기록 및 Kafka 알림 발행을 전담합니다.
 * </p>
 */
public interface FaultDetectionService {

    /**
     * StatusNotification 전문을 분석하여 장애 발생 또는 자동 복구를 감지하고 처리
     *
     * @param msg 파싱된 OCPP StatusNotification 메시지
     */
    void processFaultEvent(OcppParsedMessage msg);

    /**
     * 장애 감지 조건 여부 판정 (status == 'Faulted' OR errorCode != 'NoError')
     *
     * @param status    OCPP 상태
     * @param errorCode OCPP 에러코드
     * @return 장애 조건 충족 여부
     */
    boolean isFaultCondition(String status, String errorCode);

    /**
     * 동일 장애의 연속 인입(Flapping) 방어 필터링
     *
     * @param chargeBoxId 충전기 ID
     * @param connectorId 커넥터 번호
     * @param errorCode   에러코드
     * @return 신규 알림 발행 대상 여부 (true: 발행 필요, false: 중복 억제)
     */
    boolean shouldTriggerAlert(String chargeBoxId, int connectorId, String errorCode);

    /**
     * 활성 장애 자동 복구 마감 처리 (NoError 수신 시)
     *
     * @param chargeBoxId 충전기 ID
     * @param connectorId 커넥터 번호
     */
    void resolveActiveFault(String chargeBoxId, int connectorId);
}
