package com.lselink.elvis.control.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_station")
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
