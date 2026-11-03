package com.lselink.elvis.control.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * ClickHouse 원본 통신로그 응답 DTO
 * [전사 코딩 표준 준수: Lombok 전면 배제, 순수 자바 POJO 및 Builder 패턴 구현]
 */
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

    public RawLogResponseDto() {
    }

    public RawLogResponseDto(LocalDateTime eventTime, String chargeBoxId, Integer connectorId, Long transactionId,
                             Double voltage, Double currentImport, Double powerActive, Double energyActive,
                             Double soc, String rawJson) {
        this.eventTime = eventTime;
        this.chargeBoxId = chargeBoxId;
        this.connectorId = connectorId;
        this.transactionId = transactionId;
        this.voltage = voltage;
        this.currentImport = currentImport;
        this.powerActive = powerActive;
        this.energyActive = energyActive;
        this.soc = soc;
        this.rawJson = rawJson;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getChargeBoxId() {
        return chargeBoxId;
    }

    public void setChargeBoxId(String chargeBoxId) {
        this.chargeBoxId = chargeBoxId;
    }

    public Integer getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(Integer connectorId) {
        this.connectorId = connectorId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Double getVoltage() {
        return voltage;
    }

    public void setVoltage(Double voltage) {
        this.voltage = voltage;
    }

    public Double getCurrentImport() {
        return currentImport;
    }

    public void setCurrentImport(Double currentImport) {
        this.currentImport = currentImport;
    }

    public Double getPowerActive() {
        return powerActive;
    }

    public void setPowerActive(Double powerActive) {
        this.powerActive = powerActive;
    }

    public Double getEnergyActive() {
        return energyActive;
    }

    public void setEnergyActive(Double energyActive) {
        this.energyActive = energyActive;
    }

    public Double getSoc() {
        return soc;
    }

    public void setSoc(Double soc) {
        this.soc = soc;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDateTime eventTime;
        private String chargeBoxId;
        private Integer connectorId;
        private Long transactionId;
        private Double voltage;
        private Double currentImport;
        private Double powerActive;
        private Double energyActive;
        private Double soc;
        private String rawJson;

        public Builder eventTime(LocalDateTime eventTime) {
            this.eventTime = eventTime;
            return this;
        }

        public Builder chargeBoxId(String chargeBoxId) {
            this.chargeBoxId = chargeBoxId;
            return this;
        }

        public Builder connectorId(Integer connectorId) {
            this.connectorId = connectorId;
            return this;
        }

        public Builder transactionId(Long transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder voltage(Double voltage) {
            this.voltage = voltage;
            return this;
        }

        public Builder currentImport(Double currentImport) {
            this.currentImport = currentImport;
            return this;
        }

        public Builder powerActive(Double powerActive) {
            this.powerActive = powerActive;
            return this;
        }

        public Builder energyActive(Double energyActive) {
            this.energyActive = energyActive;
            return this;
        }

        public Builder soc(Double soc) {
            this.soc = soc;
            return this;
        }

        public Builder rawJson(String rawJson) {
            this.rawJson = rawJson;
            return this;
        }

        public RawLogResponseDto build() {
            return new RawLogResponseDto(eventTime, chargeBoxId, connectorId, transactionId,
                    voltage, currentImport, powerActive, energyActive, soc, rawJson);
        }
    }
}
