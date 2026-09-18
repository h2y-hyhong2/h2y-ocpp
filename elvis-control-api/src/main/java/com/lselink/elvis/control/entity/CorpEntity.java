package com.lselink.elvis.control.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TBL_CORP")
@Getter
@Setter
@NoArgsConstructor
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
}
