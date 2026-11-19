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
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 하향 원격 제어 REST 컨트롤러 (Sprint 31: S31-API-0050-CO)
 */
@RestController
@RequestMapping("/api/v1/commands")
public class CommandController {

    private static final Logger log = LoggerFactory.getLogger(CommandController.class);

    private final OutboundCommandService outboundCommandService;

    public CommandController(OutboundCommandService outboundCommandService) {
        this.outboundCommandService = outboundCommandService;
    }

    @PostMapping("/remote-start")
    public ApiResponse<CommandResultDto> remoteStart(@Valid @RequestBody RemoteStartCommandRequest request) {
        requireNonNullRequest(request);
        log.info("[CommandController] 원격 충전 시작 요청 - chargeBoxId={}, connectorId={}, idTag={}",
                request.getChargeBoxId(), request.getConnectorId(), request.getIdTag());

        CommandResultDto result = outboundCommandService.remoteStart(request);
        return ApiResponse.ok(result);
    }

    @PostMapping("/remote-stop")
    public ApiResponse<CommandResultDto> remoteStop(@Valid @RequestBody RemoteStopCommandRequest request) {
        requireNonNullRequest(request);
        log.info("[CommandController] 원격 충전 종료 요청 - chargeBoxId={}, transactionId={}",
                request.getChargeBoxId(), request.getTransactionId());

        CommandResultDto result = outboundCommandService.remoteStop(request);
        return ApiResponse.ok(result);
    }

    @PostMapping("/reset")
    public ApiResponse<CommandResultDto> reset(@Valid @RequestBody ResetCommandRequest request) {
        requireNonNullRequest(request);
        log.info("[CommandController] 충전기 원격 리셋 요청 - chargeBoxId={}, type={}",
                request.getChargeBoxId(), request.getType());

        CommandResultDto result = outboundCommandService.reset(request);
        return ApiResponse.ok(result);
    }

    @PostMapping("/unlock-connector")
    public ApiResponse<CommandResultDto> unlockConnector(@Valid @RequestBody UnlockConnectorCommandRequest request) {
        requireNonNullRequest(request);
        log.info("[CommandController] 커넥터 잠금 해제 요청 - chargeBoxId={}, connectorId={}",
                request.getChargeBoxId(), request.getConnectorId());

        CommandResultDto result = outboundCommandService.unlockConnector(request);
        return ApiResponse.ok(result);
    }

    @PostMapping("/change-config")
    public ApiResponse<CommandResultDto> changeConfiguration(@Valid @RequestBody ChangeConfigurationCommandRequest request) {
        requireNonNullRequest(request);
        log.info("[CommandController] 설정 변경 요청 - chargeBoxId={}, key={}, value={}",
                request.getChargeBoxId(), request.getKey(), request.getValue());

        CommandResultDto result = outboundCommandService.changeConfiguration(request);
        return ApiResponse.ok(result);
    }

    @PostMapping("/trigger-message")
    public ApiResponse<CommandResultDto> triggerMessage(@Valid @RequestBody TriggerMessageCommandRequest request) {
        requireNonNullRequest(request);
        log.info("[CommandController] 전문 트리거 요청 - chargeBoxId={}, message={}, connectorId={}",
                request.getChargeBoxId(), request.getRequestedMessage(), request.getConnectorId());

        CommandResultDto result = outboundCommandService.triggerMessage(request);
        return ApiResponse.ok(result);
    }

    private void requireNonNullRequest(Object request) {
        if (request == null) {
            throw new IllegalArgumentException("요청 본문은 필수입니다.");
        }
    }
}
