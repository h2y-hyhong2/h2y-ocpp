package com.lselink.elvis.connect.sync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lselink.elvis.connect.sync.ocpp.OcppParsedMessage;
import com.lselink.elvis.connect.sync.repository.ConnectorStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 실시간 충전 계측치(MeterValues) 처리 전담 서비스
 * <p>
 * OCPP 1.6J 표준 MeterValues와 LS E-Link 확장 DataTransfer(MeterValues)를 모두 파싱하여
 * MySQL connector_status 테이블에 단일 Upsert로 반영합니다.
 * </p>
 */
@Service
public class MeterValuesService {

    private static final Logger log = LoggerFactory.getLogger(MeterValuesService.class);

    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final BigDecimal KELVIN_OFFSET = new BigDecimal("273.15");

    private final ConnectorStatusRepository connectorStatusRepository;
    private final OcppResponseSender ocppResponseSender;

    public MeterValuesService(ConnectorStatusRepository connectorStatusRepository,
                              OcppResponseSender ocppResponseSender) {
        this.connectorStatusRepository = connectorStatusRepository;
        this.ocppResponseSender = ocppResponseSender;
    }

    /**
     * OCPP 1.6J 표준 MeterValues.req 처리 및 {} 응답 회신
     */
    @Transactional
    public void handleStandardMeterValues(OcppParsedMessage msg) {
        String chargeBoxId = msg.getChargeBoxId();
        String uniqueId = msg.getUniqueId();
        JsonNode payload = msg.getPayload();

        int connectorId = Math.max(1, payload.path("connectorId").asInt(1));

        BigDecimal powerKw = null;
        BigDecimal voltageV = null;
        BigDecimal currentA = null;
        Integer socPercent = null;
        Integer batteryTempC = null;
        BigDecimal accumulatedKwh = null;

        JsonNode meterValues = payload.path("meterValue");
        if (meterValues.isArray()) {
            for (JsonNode mv : meterValues) {
                JsonNode sampledValues = mv.path("sampledValue");
                if (!sampledValues.isArray()) {
                    continue;
                }
                for (JsonNode sv : sampledValues) {
                    BigDecimal rawValue = parseDecimal(sv.path("value"));
                    if (rawValue == null) {
                        continue;
                    }
                    String measurand = sv.path("measurand").asText("Energy.Active.Import.Register").trim();
                    String unit = sv.path("unit").asText("").trim();

                    switch (measurand) {
                        case "Power.Active.Import", "Power.Offered" ->
                                powerKw = toKilowatt(rawValue, unit);
                        case "Voltage" ->
                                voltageV = scale2(rawValue);
                        case "Current.Import", "Current.Offered" ->
                                currentA = scale2(rawValue);
                        case "SoC" ->
                                socPercent = clampPercent(rawValue.setScale(0, RoundingMode.HALF_UP).intValue());
                        case "Temperature" ->
                                batteryTempC = toCelsius(rawValue, unit);
                        case "Energy.Active.Import.Register", "Energy.Active.Import.Interval" ->
                                accumulatedKwh = toKilowattHour(rawValue, unit);
                        default -> {
                            // 기타 계측 항목은 현재 관제 컬럼 외 영역이므로 무시
                        }
                    }
                }
            }
        }

        if (powerKw == null && voltageV != null && currentA != null) {
            powerKw = voltageV.multiply(currentA).divide(THOUSAND, 2, RoundingMode.HALF_UP);
        }

        connectorStatusRepository.upsertTelemetry(
                chargeBoxId,
                connectorId,
                powerKw,
                voltageV,
                currentA,
                socPercent,
                batteryTempC,
                null,
                accumulatedKwh,
                null
        );

        log.info("[MeterSvc] 표준 MeterValues 반영 완료: chargeBoxId={}, connId={}, powerKw={}, voltageV={}, currentA={}, soc={}%, accKwh={}",
                chargeBoxId, connectorId, powerKw, voltageV, currentA, socPercent, accumulatedKwh);

        ocppResponseSender.sendMeterValuesResponse(chargeBoxId, uniqueId);
    }

    /**
     * LS E-Link 확장 DataTransfer(messageId="MeterValues") 처리 및 Accepted 응답 회신
     * <pre>
     * data 예시:
     * {
     *   "connectorId": 1, "transactionId": 1, "idTag": "C005413",
     *   "power": 8441.0 (W), "eps": 367 (V), "ecu": 23.0 (A),
     *   "accWh": 5119.042 (kWh), "rechgHr": 1141 (sec), "btrRm": 91 (%)
     * }
     * </pre>
     */
    @Transactional
    public void handleDataTransferMeterValues(String chargeBoxId, String uniqueId, JsonNode dataNode) {
        if (dataNode == null || !dataNode.isObject()) {
            log.warn("[MeterSvc] DataTransfer(MeterValues) data 누락 또는 형식 오류: chargeBoxId={}", chargeBoxId);
            ocppResponseSender.sendDataTransferResponse(chargeBoxId, uniqueId, "Rejected", null);
            return;
        }

        int connectorId = Math.max(1, dataNode.path("connectorId").asInt(1));
        String idTag = nullIfEmpty(dataNode.path("idTag").asText(null));

        BigDecimal rawPowerW = parseDecimal(dataNode.path("power"));
        BigDecimal voltageV = scale2(parseDecimal(dataNode.path("eps")));
        BigDecimal currentA = scale2(parseDecimal(dataNode.path("ecu")));

        BigDecimal powerKw = null;
        if (rawPowerW != null) {
            // 규약서 6.2장: power = eps(V) * ecu(A) (W 단위) → kW 변환
            powerKw = rawPowerW.divide(THOUSAND, 2, RoundingMode.HALF_UP);
        } else if (voltageV != null && currentA != null) {
            powerKw = voltageV.multiply(currentA).divide(THOUSAND, 2, RoundingMode.HALF_UP);
        }

        // 규약서 6.2장: accWh는 kWh 단위(소수점 3자리)
        BigDecimal accumulatedKwh = scale2(parseDecimal(dataNode.path("accWh")));

        // 규약서 6.2장: btrRm은 차량 배터리 잔량 SoC (%)
        BigDecimal rawSoc = parseDecimal(dataNode.path("btrRm"));
        Integer socPercent = rawSoc != null
                ? clampPercent(rawSoc.setScale(0, RoundingMode.HALF_UP).intValue())
                : null;

        // 규약서 6.2장: rechgHr는 충전 경과 시간(초) → 분 변환
        BigDecimal rawRechgSec = parseDecimal(dataNode.path("rechgHr"));
        Integer chargingMinutes = rawRechgSec != null
                ? Math.max(0, rawRechgSec.intValue() / 60)
                : null;

        connectorStatusRepository.upsertTelemetry(
                chargeBoxId,
                connectorId,
                powerKw,
                voltageV,
                currentA,
                socPercent,
                null,
                idTag,
                accumulatedKwh,
                chargingMinutes
        );

        log.info("[MeterSvc] LS DataTransfer(MeterValues) 반영 완료: chargeBoxId={}, connId={}, powerKw={}, voltageV={}, currentA={}, soc={}%, accKwh={}, min={}",
                chargeBoxId, connectorId, powerKw, voltageV, currentA, socPercent, accumulatedKwh, chargingMinutes);

        ocppResponseSender.sendDataTransferResponse(chargeBoxId, uniqueId, "Accepted", null);
    }

    private BigDecimal toKilowatt(BigDecimal value, String unit) {
        if ("kW".equalsIgnoreCase(unit)) {
            return scale2(value);
        }
        return value.divide(THOUSAND, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal toKilowattHour(BigDecimal value, String unit) {
        if ("kWh".equalsIgnoreCase(unit)) {
            return scale2(value);
        }
        return value.divide(THOUSAND, 2, RoundingMode.HALF_UP);
    }

    private Integer toCelsius(BigDecimal value, String unit) {
        BigDecimal celsius = "K".equalsIgnoreCase(unit) || "Kelvin".equalsIgnoreCase(unit)
                ? value.subtract(KELVIN_OFFSET)
                : value;
        return celsius.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private BigDecimal parseDecimal(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        String text = node.asText(null);
        if (text == null || text.isBlank() || "null".equalsIgnoreCase(text.trim())) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal scale2(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : null;
    }

    private int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String nullIfEmpty(String value) {
        return (value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim())) ? null : value.trim();
    }
}
