package com.lselink.elvis.tester.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDto {
    private String id;
    private String name;
    private String action;
    private String defaultTopic;
    private String description;
    private String samplePayload;
}
