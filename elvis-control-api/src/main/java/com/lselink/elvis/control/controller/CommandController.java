package com.lselink.elvis.control.controller;

import com.lselink.elvis.control.common.ApiResponse;
import com.lselink.elvis.control.dto.command.ChangeConfigurationCommandRequest;
import com.lselink.elvis.control.dto.command.CommandResultDto;
import com.lselink.elvis.control.dto.command.RemoteStartCommandRequest;
import com.lselink.elvis.control.dto.command.RemoteStopCommandRequest;
import com.lselink.elvis.control.dto.command.ResetCommandRequest;
import com.lselink.elvis.control.dto.command.TriggerMessageCommandRequest;
import com.lselink.elvis.control.dto.command.UnlockConnectorCommandRequest;
import com.lselink.elvis.control.service.OutboundCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 하향 원격 제어 REST 컨트롤러 (Sprint 31: S31-API-0050-CO)
 * <p>
 * 관제 UI 및 데스크톱 런처로부터 6대 원격 제어 요청을 수신하여
 * Kafka ocpp-outbound-commands 토픽으로 발행하는 진입점입니다.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/commands")
public class CommandController {

    private static final Logger log = LoggerFactory.getLogger(CommandController.class);

    private final OutboundCommandService outboundCommandService;

    public CommandController(OutboundCommandService outboundCommandService) {
        this.outboundCommandService = outboundCommandService;
    }

    /**
     * 원격 충전 시작 (RemoteStartTransaction)
     * POST /api/v1/commands/remote-start
     */
    @PostMapping("/remote-start")
    public ApiResponse<CommandResultDto> remoteStart(@RequestBody RemoteStartCommandRequest request) {
        log.info("📡 [CommandController] 원격 충전 시작 요청 - chargeBoxId={}, connectorId={}, idTag={}",
                request.getChargeBoxId(), request.getConnectorId(), request.getIdTag());

        // [거버넌스 9조] 외부 I/O 서비스 호출 변수 분리
        CommandResultDto result = outboundCommandService.remoteStart(request);
        return ApiResponse.ok(result);
    }

    /**
     * 원격 충전 강제 종료 (RemoteStopTransaction)
     * POST /api/v1/commands/remote-stop
     */
    @PostMapping("/remote-stop")
    public ApiResponse<CommandResultDto> remoteStop(@RequestBody RemoteStopCommandRequest request) {
        log.info("📡 [CommandController] 원격 충전 종료 요청 - chargeBoxId={}, transactionId={}",
                request.getChargeBoxId(), request.getTransactionId());

        // [거버넌스 9조] 외부 I/O 서비스 호출 변수 분리
        CommandResultDto result = outboundCommandService.remoteStop(request);
        return ApiResponse.ok(result);
    }

    /**
     * 충전기 원격 리셋 (Reset)
     * POST /api/v1/commands/reset
     */
    @PostMapping("/reset")
    public ApiResponse<CommandResultDto> reset(@RequestBody ResetCommandRequest request) {
        log.info("📡 [CommandController] 충전기 원격 리셋 요청 - chargeBoxId={}, type={}",
                request.getChargeBoxId(), request.getType());

        // [거버넌스 9조] 외부 I/O 서비스 호출 변수 분리
        CommandResultDto result = outboundCommandService.reset(request);
        return ApiResponse.ok(result);
    }

    /**
     * 커넥터 원격 잠금 해제 (UnlockConnector)
     * POST /api/v1/commands/unlock-connector
     */
    @PostMapping("/unlock-connector")
    public ApiResponse<CommandResultDto> unlockConnector(@RequestBody UnlockConnectorCommandRequest request) {
        log.info("📡 [CommandController] 커넥터 잠금 해제 요청 - chargeBoxId={}, connectorId={}",
                request.getChargeBoxId(), request.getConnectorId());

        // [거버넌스 9조] 외부 I/O 서비스 호출 변수 분리
        CommandResultDto result = outboundCommandService.unlockConnector(request);
        return ApiResponse.ok(result);
    }

    /**
     * 설정 파라미터 변경 (ChangeConfiguration)
     * POST /api/v1/commands/change-config
     */
    @PostMapping("/change-config")
    public ApiResponse<CommandResultDto> changeConfiguration(@RequestBody ChangeConfigurationCommandRequest request) {
        log.info("📡 [CommandController] 설정 변경 요청 - chargeBoxId={}, key={}, value={}",
                request.getChargeBoxId(), request.getKey(), request.getValue());

        // [거버넌스 9조] 외부 I/O 서비스 호출 변수 분리
        CommandResultDto result = outboundCommandService.changeConfiguration(request);
        return ApiResponse.ok(result);
    }

    /**
     * 전문 즉시 보고 트리거 (TriggerMessage)
     * POST /api/v1/commands/trigger-message
     */
    @PostMapping("/trigger-message")
    public ApiResponse<CommandResultDto> triggerMessage(@RequestBody TriggerMessageCommandRequest request) {
        log.info("📡 [CommandController] 전문 트리거 요청 - chargeBoxId={}, message={}, connectorId={}",
                request.getChargeBoxId(), request.getRequestedMessage(), request.getConnectorId());

        // [거버넌스 9조] 외부 I/O 서비스 호출 변수 분리
        CommandResultDto result = outboundCommandService.triggerMessage(request);
        return ApiResponse.ok(result);
    }
}
