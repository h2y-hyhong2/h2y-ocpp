package com.lselink.elvis.control.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TBL_STATION")
@Getter
@Setter
@NoArgsConstructor
public class StationEntity {

    @Id
    @Column(name = "ST_ID", length = 20)
    private String stId;

    @Column(name = "NAME", nullable = false, length = 150)
    private String name;

    @Column(name = "CORP_ID", nullable = false, length = 20)
    private String corpId;

    @Column(name = "CHARGER_COUNT", nullable = false)
    private Integer chargerCount;

    @Column(name = "ADDR", length = 250)
    private String addr;

    @Column(name = "LAT", precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(name = "LNG", precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "REG_DT")
    private LocalDateTime regDt;

    @Column(name = "UPDT_DT")
    private LocalDateTime updtDt;
}
