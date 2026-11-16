package com.lselink.elvis.control.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "corp")
public class CorpEntity {

    @Id
    @Column(name = "corp_id", length = 20)
    private String corpId;

    @Column(name = "corp_name", nullable = false, length = 100)
    private String corpName;

    @Column(name = "short_name", nullable = false, length = 50)
    private String shortName;

    @Column(name = "tag", nullable = false, length = 20)
    private String tag;

    @Column(name = "color_class", length = 20)
    private String colorClass;

    @Column(name = "badge", length = 50)
    private String badge;

    @Column(name = "use_yn", length = 1)
    private String useYn;

    @Column(name = "reg_dt")
    private LocalDateTime regDt;

    @Column(name = "updt_dt")
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
