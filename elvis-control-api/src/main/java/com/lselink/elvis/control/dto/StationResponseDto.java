package com.lselink.elvis.control.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationResponseDto {
    private String id;
    private String name;
    private String corpId;
    private String corpName;
    private String corpShortName;
    private Integer chargerCount;
    private Integer startIdx;
}
