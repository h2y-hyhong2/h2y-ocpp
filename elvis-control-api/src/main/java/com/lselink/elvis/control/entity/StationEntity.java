package com.lselink.elvis.control.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "station")
public class StationEntity {

    @Id
    @Column(name = "st_id", length = 20)
    private String stId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "corp_id", nullable = false, length = 20)
    private String corpId;

    @Column(name = "charger_count", nullable = false)
    private Integer chargerCount;

    @Column(name = "addr", length = 250)
    private String addr;

    @Column(name = "lat", precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(name = "lng", precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "reg_dt")
    private LocalDateTime regDt;

    @Column(name = "updt_dt")
    private LocalDateTime updtDt;

    public StationEntity() {
    }

    public String getStId() { return stId; }
    public void setStId(String stId) { this.stId = stId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCorpId() { return corpId; }
    public void setCorpId(String corpId) { this.corpId = corpId; }

    public Integer getChargerCount() { return chargerCount; }
    public void setChargerCount(Integer chargerCount) { this.chargerCount = chargerCount; }

    public String getAddr() { return addr; }
    public void setAddr(String addr) { this.addr = addr; }

    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }

    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getRegDt() { return regDt; }
    public void setRegDt(LocalDateTime regDt) { this.regDt = regDt; }

    public LocalDateTime getUpdtDt() { return updtDt; }
    public void setUpdtDt(LocalDateTime updtDt) { this.updtDt = updtDt; }
}
