package com.lselink.elvis.control.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_corp")
public class CorpEntity {

    @Id
    @Column(name = "CORP_ID", length = 20)
    private String corpId;

    @Column(name = "CORP_NAME", nullable = false, length = 100)
    private String corpName;

    @Column(name = "SHORT_NAME", nullable = false, length = 50)
    private String shortName;

    @Column(name = "TAG", nullable = false, length = 20)
    private String tag;

    @Column(name = "COLOR_CLASS", length = 20)
    private String colorClass;

    @Column(name = "BADGE", length = 50)
    private String badge;

    @Column(name = "USE_YN", length = 1)
    private String useYn;

    @Column(name = "REG_DT")
    private LocalDateTime regDt;

    @Column(name = "UPDT_DT")
    private LocalDateTime updtDt;

    public CorpEntity() {
    }

    public String getCorpId() { return corpId; }
    public void setCorpId(String corpId) { this.corpId = corpId; }

    public String getCorpName() { return corpName; }
    public void setCorpName(String corpName) { this.corpName = corpName; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public String getColorClass() { return colorClass; }
    public void setColorClass(String colorClass) { this.colorClass = colorClass; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }

    public LocalDateTime getRegDt() { return regDt; }
    public void setRegDt(LocalDateTime regDt) { this.regDt = regDt; }

    public LocalDateTime getUpdtDt() { return updtDt; }
    public void setUpdtDt(LocalDateTime updtDt) { this.updtDt = updtDt; }
}
