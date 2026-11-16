package com.lselink.elvis.control.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "charger")
public class ChargerEntity {

    @Id
    @Column(name = "charge_box_id", length = 50)
    private String chargeBoxId;

    @Column(name = "st_id", nullable = false, length = 20)
    private String stId;

    @Column(name = "cp_id", nullable = false, length = 10)
    private String cpId;

    @Column(name = "vendor", nullable = false, length = 50)
    private String vendor;

    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @Column(name = "spec", nullable = false, length = 50)
    private String spec;

    @Column(name = "protocol", length = 30)
    private String protocol;

    @Column(name = "firmware_ver", length = 50)
    private String firmwareVer;

    @Column(name = "use_yn", length = 1)
    private String useYn;

    @Column(name = "reg_dt")
    private LocalDateTime regDt;

    @Column(name = "updt_dt")
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
