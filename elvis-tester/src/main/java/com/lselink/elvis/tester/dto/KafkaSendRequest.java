package com.lselink.elvis.tester.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaSendRequest {
    private String topic;
    private String chargeBoxId;
    private String payload;
}
