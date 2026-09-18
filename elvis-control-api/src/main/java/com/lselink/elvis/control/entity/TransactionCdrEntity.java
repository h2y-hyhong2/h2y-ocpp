package com.lselink.elvis.control.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TBL_TRANSACTION_CDR")
@Getter
@Setter
@NoArgsConstructor
public class TransactionCdrEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_ID")
    private Long transactionId;

    @Column(name = "CHARGE_BOX_ID", nullable = false, length = 50)
    private String chargeBoxId;

    @Column(name = "CONNECTOR_ID", nullable = false)
    private Integer connectorId;

    @Column(name = "USER_TAG", nullable = false, length = 50)
    private String userTag;

    @Column(name = "START_TIME", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "STOP_TIME")
    private LocalDateTime stopTime;

    @Column(name = "METER_START_WH")
    private Long meterStartWh;

    @Column(name = "METER_STOP_WH")
    private Long meterStopWh;

    @Column(name = "TOTAL_KWH", precision = 10, scale = 2)
    private BigDecimal totalKwh;

    @Column(name = "UNIT_PRICE", precision = 8, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "TOTAL_AMOUNT", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "PAYMENT_STATUS", length = 20)
    private String paymentStatus;

    @Column(name = "STOP_REASON", length = 50)
    private String stopReason;

    @Column(name = "REG_DT")
    private LocalDateTime regDt;
}
