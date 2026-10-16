package com.lselink.elvis.control.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_charger")
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

    public ChargerEntity() {
    }

    public String getChargeBoxId() { return chargeBoxId; }
    public void setChargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; }

    public String getStId() { return stId; }
    public void setStId(String stId) { this.stId = stId; }

    public String getCpId() { return cpId; }
    public void setCpId(String cpId) { this.cpId = cpId; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getFirmwareVer() { return firmwareVer; }
    public void setFirmwareVer(String firmwareVer) { this.firmwareVer = firmwareVer; }

    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }

    public LocalDateTime getRegDt() { return regDt; }
    public void setRegDt(LocalDateTime regDt) { this.regDt = regDt; }

    public LocalDateTime getUpdtDt() { return updtDt; }
    public void setUpdtDt(LocalDateTime updtDt) { this.updtDt = updtDt; }
}
