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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 하향 원격 제어 REST 컨트롤러 단위 테스트 (Sprint 31: S31-API-0050-TE)
 */
@ExtendWith(MockitoExtension.class)
class CommandControllerTest {

    @Mock
    private OutboundCommandService outboundCommandService;

    @InjectMocks
    private CommandController commandController;

    @Test
    @DisplayName("원격 충전 시작 엔드포인트 응답 래핑 검증")
    void remoteStart_endpoint() {
        // given
        RemoteStartCommandRequest request = new RemoteStartCommandRequest("00000001", 1, "RFID-1001");
        CommandResultDto mockResult = CommandResultDto.sent("cmd-001", "00000001", "RemoteStartTransaction", "정상 발송");
        when(outboundCommandService.remoteStart(any(RemoteStartCommandRequest.class))).thenReturn(mockResult);

        // when
        ApiResponse<CommandResultDto> response = commandController.remoteStart(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getAction()).isEqualTo("RemoteStartTransaction");
        assertThat(response.getData().getStatus()).isEqualTo("SENT");
    }

    @Test
    @DisplayName("원격 충전 종료 엔드포인트 응답 래핑 검증")
    void remoteStop_endpoint() {
        // given
        RemoteStopCommandRequest request = new RemoteStopCommandRequest("00000001", 1001);
        CommandResultDto mockResult = CommandResultDto.sent("cmd-002", "00000001", "RemoteStopTransaction", "정상 발송");
        when(outboundCommandService.remoteStop(any(RemoteStopCommandRequest.class))).thenReturn(mockResult);

        // when
        ApiResponse<CommandResultDto> response = commandController.remoteStop(request);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getAction()).isEqualTo("RemoteStopTransaction");
    }

    @Test
    @DisplayName("충전기 리셋 엔드포인트 응답 래핑 검증")
    void reset_endpoint() {
        // given
        ResetCommandRequest request = new ResetCommandRequest("00000001", "Soft");
        CommandResultDto mockResult = CommandResultDto.sent("cmd-003", "00000001", "Reset", "정상 발송");
        when(outboundCommandService.reset(any(ResetCommandRequest.class))).thenReturn(mockResult);

        // when
        ApiResponse<CommandResultDto> response = commandController.reset(request);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getAction()).isEqualTo("Reset");
    }

    @Test
    @DisplayName("커넥터 잠금 해제 엔드포인트 응답 래핑 검증")
    void unlockConnector_endpoint() {
        // given
        UnlockConnectorCommandRequest request = new UnlockConnectorCommandRequest("00000001", 1);
        CommandResultDto mockResult = CommandResultDto.sent("cmd-004", "00000001", "UnlockConnector", "정상 발송");
        when(outboundCommandService.unlockConnector(any(UnlockConnectorCommandRequest.class))).thenReturn(mockResult);

        // when
        ApiResponse<CommandResultDto> response = commandController.unlockConnector(request);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getAction()).isEqualTo("UnlockConnector");
    }

    @Test
    @DisplayName("설정 파라미터 변경 엔드포인트 응답 래핑 검증")
    void changeConfig_endpoint() {
        // given
        ChangeConfigurationCommandRequest request = new ChangeConfigurationCommandRequest("00000001", "Key", "Val");
        CommandResultDto mockResult = CommandResultDto.sent("cmd-005", "00000001", "ChangeConfiguration", "정상 발송");
        when(outboundCommandService.changeConfiguration(any(ChangeConfigurationCommandRequest.class))).thenReturn(mockResult);

        // when
        ApiResponse<CommandResultDto> response = commandController.changeConfiguration(request);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getAction()).isEqualTo("ChangeConfiguration");
    }

    @Test
    @DisplayName("전문 즉시 보고 트리거 엔드포인트 응답 래핑 검증")
    void triggerMessage_endpoint() {
        // given
        TriggerMessageCommandRequest request = new TriggerMessageCommandRequest("00000001", "StatusNotification", 1);
        CommandResultDto mockResult = CommandResultDto.sent("cmd-006", "00000001", "TriggerMessage", "정상 발송");
        when(outboundCommandService.triggerMessage(any(TriggerMessageCommandRequest.class))).thenReturn(mockResult);

        // when
        ApiResponse<CommandResultDto> response = commandController.triggerMessage(request);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getAction()).isEqualTo("TriggerMessage");
    }
}
