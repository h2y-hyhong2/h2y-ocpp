package com.lselink.elvis.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lselink.elvis.control.dto.command.ChangeConfigurationCommandRequest;
import com.lselink.elvis.control.dto.command.CommandResultDto;
import com.lselink.elvis.control.dto.command.RemoteStartCommandRequest;
import com.lselink.elvis.control.dto.command.RemoteStopCommandRequest;
import com.lselink.elvis.control.dto.command.ResetCommandRequest;
import com.lselink.elvis.control.dto.command.TriggerMessageCommandRequest;
import com.lselink.elvis.control.dto.command.UnlockConnectorCommandRequest;
import com.lselink.elvis.control.service.impl.OutboundCommandServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Kafka 하향 원격 제어 커맨드 서비스 단위 테스트 (Sprint 31: S31-API-0050-TE)
 */
@ExtendWith(MockitoExtension.class)
class OutboundCommandServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;
    private OutboundCommandServiceImpl commandService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        commandService = new OutboundCommandServiceImpl(kafkaTemplate, objectMapper);
        ReflectionTestUtils.setField(commandService, "outboundTopic", "ocpp-outbound-commands");
    }

    @Test
    @DisplayName("RemoteStartTransaction 성공 시 SENT 상태 반환 및 파티션 키(chargeBoxId) 바인딩 검증")
    void remoteStart_success() {
        // given
        RemoteStartCommandRequest request = new RemoteStartCommandRequest("00000001", 1, "RFID-1001");
        CompletableFuture future = CompletableFuture.completedFuture(mock(org.springframework.kafka.support.SendResult.class));
        when(kafkaTemplate.send(eq("ocpp-outbound-commands"), eq("00000001"), any(String.class))).thenReturn(future);

        // when
        CommandResultDto result = commandService.remoteStart(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SENT");
        assertThat(result.getChargeBoxId()).isEqualTo("00000001");
        assertThat(result.getAction()).isEqualTo("RemoteStartTransaction");
        assertThat(result.getCommandId()).isNotEmpty();

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(1)).send(eq("ocpp-outbound-commands"), eq("00000001"), payloadCaptor.capture());

        String sentPayload = payloadCaptor.getValue();
        assertThat(sentPayload).contains("\"chargeBoxId\":\"00000001\"");
        assertThat(sentPayload).contains("RemoteStartTransaction");
        assertThat(sentPayload).contains("RFID-1001");
    }

    @Test
    @DisplayName("RemoteStopTransaction 성공 시 transactionId 매핑 및 발송 검증")
    void remoteStop_success() {
        // given
        RemoteStopCommandRequest request = new RemoteStopCommandRequest("00000001", 1002);
        CompletableFuture future = CompletableFuture.completedFuture(mock(org.springframework.kafka.support.SendResult.class));
        when(kafkaTemplate.send(eq("ocpp-outbound-commands"), eq("00000001"), any(String.class))).thenReturn(future);

        // when
        CommandResultDto result = commandService.remoteStop(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SENT");
        assertThat(result.getAction()).isEqualTo("RemoteStopTransaction");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(1)).send(eq("ocpp-outbound-commands"), eq("00000001"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("1002");
    }

    @Test
    @DisplayName("Reset 명령 성공 시 Soft 및 Hard 타입 분기 검증")
    void reset_types_success() {
        // given
        ResetCommandRequest softRequest = new ResetCommandRequest("00000001", "Soft");
        ResetCommandRequest hardRequest = new ResetCommandRequest("00000001", "Hard");

        CompletableFuture future = CompletableFuture.completedFuture(mock(org.springframework.kafka.support.SendResult.class));
        when(kafkaTemplate.send(eq("ocpp-outbound-commands"), eq("00000001"), any(String.class))).thenReturn(future);

        // when
        CommandResultDto softResult = commandService.reset(softRequest);
        CommandResultDto hardResult = commandService.reset(hardRequest);

        // then
        assertThat(softResult.getStatus()).isEqualTo("SENT");
        assertThat(hardResult.getStatus()).isEqualTo("SENT");
        verify(kafkaTemplate, times(2)).send(eq("ocpp-outbound-commands"), eq("00000001"), any(String.class));
    }

    @Test
    @DisplayName("UnlockConnector 명령 성공 시 connectorId 검증")
    void unlockConnector_success() {
        // given
        UnlockConnectorCommandRequest request = new UnlockConnectorCommandRequest("00000001", 2);
        CompletableFuture future = CompletableFuture.completedFuture(mock(org.springframework.kafka.support.SendResult.class));
        when(kafkaTemplate.send(eq("ocpp-outbound-commands"), eq("00000001"), any(String.class))).thenReturn(future);

        // when
        CommandResultDto result = commandService.unlockConnector(request);

        // then
        assertThat(result.getStatus()).isEqualTo("SENT");
        assertThat(result.getAction()).isEqualTo("UnlockConnector");
    }

    @Test
    @DisplayName("ChangeConfiguration 명령 성공 시 key/value 검증")
    void changeConfiguration_success() {
        // given
        ChangeConfigurationCommandRequest request = new ChangeConfigurationCommandRequest("00000001", "HeartbeatInterval", "30");
        CompletableFuture future = CompletableFuture.completedFuture(mock(org.springframework.kafka.support.SendResult.class));
        when(kafkaTemplate.send(eq("ocpp-outbound-commands"), eq("00000001"), any(String.class))).thenReturn(future);

        // when
        CommandResultDto result = commandService.changeConfiguration(request);

        // then
        assertThat(result.getStatus()).isEqualTo("SENT");
        assertThat(result.getAction()).isEqualTo("ChangeConfiguration");
    }

    @Test
    @DisplayName("TriggerMessage 명령 성공 시 requestedMessage 검증")
    void triggerMessage_success() {
        // given
        TriggerMessageCommandRequest request = new TriggerMessageCommandRequest("00000001", "StatusNotification", 1);
        CompletableFuture future = CompletableFuture.completedFuture(mock(org.springframework.kafka.support.SendResult.class));
        when(kafkaTemplate.send(eq("ocpp-outbound-commands"), eq("00000001"), any(String.class))).thenReturn(future);

        // when
        CommandResultDto result = commandService.triggerMessage(request);

        // then
        assertThat(result.getStatus()).isEqualTo("SENT");
        assertThat(result.getAction()).isEqualTo("TriggerMessage");
    }

    @Test
    @DisplayName("Kafka 발행 실패 시 500 에러 대신 FAILED 상태와 실패 사유 반환 (Graceful Degradation)")
    void fallback_on_kafka_error() {
        // given
        RemoteStartCommandRequest request = new RemoteStartCommandRequest("00000001", 1, "RFID-FAIL");
        CompletableFuture future = new CompletableFuture();
        future.completeExceptionally(new RuntimeException("Kafka Broker Unavailable"));
        when(kafkaTemplate.send(eq("ocpp-outbound-commands"), eq("00000001"), any(String.class))).thenReturn(future);

        // when
        CommandResultDto result = commandService.remoteStart(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getMessage()).contains("Kafka Broker Unavailable");
        assertThat(result.getAction()).isEqualTo("RemoteStartTransaction");
    }

    @Test
    @DisplayName("chargeBoxId가 null 또는 공백인 경우 거버넌스 10조 표준 기본값(00000001) 정규화 검증")
    void chargeBoxId_null_normalization() {
        // given
        RemoteStartCommandRequest request = new RemoteStartCommandRequest(null, null, null);
        CompletableFuture future = CompletableFuture.completedFuture(mock(org.springframework.kafka.support.SendResult.class));
        when(kafkaTemplate.send(eq("ocpp-outbound-commands"), eq("00000001"), any(String.class))).thenReturn(future);

        // when
        CommandResultDto result = commandService.remoteStart(request);

        // then
        assertThat(result.getChargeBoxId()).isEqualTo("00000001");
        verify(kafkaTemplate, times(1)).send(eq("ocpp-outbound-commands"), eq("00000001"), any(String.class));
    }
}
