package com.lselink.elvis.control.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TBL_CHARGER")
@Getter
@Setter
@NoArgsConstructor
public class ChargerEntity {

    @Id
    @Column(name = "CHARGE_BOX_ID", length = 50)
    private String chargeBoxId;

    @Column(name = "ST_ID", nullable = false, length = 20)
    private String stId;

    @Column(name = "CP_ID", nullable = false, length = 10)
    private String cpId;

    @Column(name = "VENDOR", nullable = false, length = 50)
    private String vendor;

    @Column(name = "MODEL", nullable = false, length = 50)
    private String model;

    @Column(name = "SPEC", nullable = false, length = 50)
    private String spec;

    @Column(name = "PROTOCOL", length = 30)
    private String protocol;

    @Column(name = "FIRMWARE_VER", length = 50)
    private String firmwareVer;

    @Column(name = "USE_YN", length = 1)
    private String useYn;

    @Column(name = "REG_DT")
    private LocalDateTime regDt;

    @Column(name = "UPDT_DT")
    private LocalDateTime updtDt;
}
