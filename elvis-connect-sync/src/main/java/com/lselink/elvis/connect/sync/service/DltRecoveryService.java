package com.lselink.elvis.connect.sync.service;

import com.lselink.elvis.connect.sync.domain.DltHistory;

import java.util.List;
import java.util.Optional;

/**
 * DLT 결함 격리 및 메시지 재처리(Replay) 서비스 인터페이스 (Sprint 24: S24-SYNC-0100-CO)
 */
public interface DltRecoveryService {

    /**
     * 실패한 메시지를 DLT 토픽으로 발행하고 MySQL 원장에 기록 (격리)
     *
     * @param chargeBoxId     충전기 ID (null 가능)
     * @param rawPayload      수신 원본 전문
     * @param ex              발생한 예외
     * @param sourceTopic     원본 토픽
     * @param sourcePartition 원본 파티션 번호
     * @param sourceOffset    원본 오프셋 번호
     * @param retryCount      재시도 횟수
     */
    void isolateFailure(String chargeBoxId,
                        String rawPayload,
                        Exception ex,
                        String sourceTopic,
                        int sourcePartition,
                        long sourceOffset,
                        int retryCount);

    /**
     * 운영자 수동 재처리: DLT 원장 메시지를 원본 인바운드 토픽으로 재주입
     *
     * @param dltId DLT 식별자
     * @return 재처리 성공 여부
     */
    boolean replayMessage(String dltId);

    /**
     * DLT 메시지 영구 폐기 처리 (악성 패킷/스팸)
     *
     * @param dltId  DLT 식별자
     * @param reason 폐기 사유
     */
    void discardMessage(String dltId, String reason);

    /**
     * 대기 중인 DLT 목록 조회
     */
    List<DltHistory> getPendingFailures();

    /**
     * 단건 DLT 상세 조회
     */
    Optional<DltHistory> getDltDetail(String dltId);
}
