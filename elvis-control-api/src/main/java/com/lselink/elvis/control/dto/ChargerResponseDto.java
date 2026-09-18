package com.lselink.elvis.control.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargerResponseDto {
    private String id;
    private String chargeBoxId;
    private String stId;
    private String stationName;
    private String corpId;
    private String corpName;
    private String corpShortName;
    private String cpId;
    private Integer connectorId;
    private String status;
    private String spec;
    private BigDecimal powerKw;
    private BigDecimal voltageV;
    private BigDecimal currentA;
    private Integer socPercent;
    private Integer batteryTempC;
    private String vendor;
    private String model;
    private String carModel;
    private String userTag;
    private String protocol;
    private String lastHeartbeat;
    private BigDecimal accumulatedKwh;
    private Integer chargingMinutes;
}
