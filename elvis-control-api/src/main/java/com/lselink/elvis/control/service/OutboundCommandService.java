package com.lselink.elvis.control.service;

import com.lselink.elvis.control.dto.command.ChangeConfigurationCommandRequest;
import com.lselink.elvis.control.dto.command.CommandResultDto;
import com.lselink.elvis.control.dto.command.RemoteStartCommandRequest;
import com.lselink.elvis.control.dto.command.RemoteStopCommandRequest;
import com.lselink.elvis.control.dto.command.ResetCommandRequest;
import com.lselink.elvis.control.dto.command.TriggerMessageCommandRequest;
import com.lselink.elvis.control.dto.command.UnlockConnectorCommandRequest;

/**
 * 하향 원격 제어 커맨드 서비스 인터페이스 (Sprint 31: S31-API-0050-CO)
 */
public interface OutboundCommandService {

    /**
     * 원격 충전 시작 (RemoteStartTransaction)
     */
    CommandResultDto remoteStart(RemoteStartCommandRequest request);

    /**
     * 원격 충전 강제 종료 (RemoteStopTransaction)
     */
    CommandResultDto remoteStop(RemoteStopCommandRequest request);

    /**
     * 충전기 원격 리셋 (Reset)
     */
    CommandResultDto reset(ResetCommandRequest request);

    /**
     * 커넥터 원격 잠금 해제 (UnlockConnector)
     */
    CommandResultDto unlockConnector(UnlockConnectorCommandRequest request);

    /**
     * 설정 파라미터 변경 (ChangeConfiguration)
     */
    CommandResultDto changeConfiguration(ChangeConfigurationCommandRequest request);

    /**
     * 특정 전문 즉시 보고 트리거 (TriggerMessage)
     */
    CommandResultDto triggerMessage(TriggerMessageCommandRequest request);
}
