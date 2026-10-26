package com.lselink.elvis.control.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ClickHouse 원본 통신로그 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawLogResponseDto {

    /** 이벤트 수신 일시 */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventTime;

    /** 충전기 결합 식별자 (8자리 정규화, 예: "00000001") */
    private String chargeBoxId;

    /** 커넥터 번호 (1, 2) */
    private Integer connectorId;

    /** 트랜잭션 ID (전역 고유 시퀀스) */
    private Long transactionId;

    /** 계측 전압 (V) */
    private Double voltage;

    /** 계측 전류 (A) */
    private Double currentImport;

    /** 활성 전력 (kW) */
    private Double powerActive;

    /** 누적 유효 전력량 (kWh) */
    private Double energyActive;

    /** 배터리 충전율 SOC (%) */
    private Double soc;

    /** 원본 OCPP JSON 전문 (WebSocket Raw Payload) */
    private String rawJson;
}
