package com.lselink.elvis.connect.sync.service;

import com.lselink.elvis.connect.sync.domain.ChargeDetailRecord;
import com.lselink.elvis.connect.sync.domain.TransactionSession;

import java.time.LocalDateTime;

/**
 * TOU 요금 계산 및 과금 원장(CDR) 관리 서비스 인터페이스 (Sprint 21: S21-SYNC-0080-CO)
 */
public interface TouRatingService {

    /**
     * StopTransaction 수신 시 세션 데이터 기반 실시간 TOU 요금 연산 및 CDR 원장 영속화
     *
     * @param session   종료 대상 트랜잭션 세션
     * @param meterStop 충전 종료 시점 미터값 (Wh)
     * @param stopTime  충전 종료 일시
     * @return 생성 및 영속화된 ChargeDetailRecord 엔티티
     */
    ChargeDetailRecord calculateAndSaveCdr(TransactionSession session, long meterStop, LocalDateTime stopTime);

    /**
     * 충전기 ID와 트랜잭션 ID로 CDR 과금 원장 조회
     */
    ChargeDetailRecord getCdr(String chargeBoxId, Long transactionId);
}
