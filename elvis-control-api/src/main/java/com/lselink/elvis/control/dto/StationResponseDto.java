package com.lselink.elvis.control.dto;

public class StationResponseDto {
    private String id;
    private String name;
    private String corpId;
    private String corpName;
    private String corpShortName;
    private Integer chargerCount;
    private Integer startIdx;

    public StationResponseDto() {
    }

    public StationResponseDto(String id, String name, String corpId, String corpName, String corpShortName, Integer chargerCount, Integer startIdx) {
        this.id = id;
        this.name = name;
        this.corpId = corpId;
        this.corpName = corpName;
        this.corpShortName = corpShortName;
        this.chargerCount = chargerCount;
        this.startIdx = startIdx;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCorpId() { return corpId; }
    public void setCorpId(String corpId) { this.corpId = corpId; }

    public String getCorpName() { return corpName; }
    public void setCorpName(String corpName) { this.corpName = corpName; }

    public String getCorpShortName() { return corpShortName; }
    public void setCorpShortName(String corpShortName) { this.corpShortName = corpShortName; }

    public Integer getChargerCount() { return chargerCount; }
    public void setChargerCount(Integer chargerCount) { this.chargerCount = chargerCount; }

    public Integer getStartIdx() { return startIdx; }
    public void setStartIdx(Integer startIdx) { this.startIdx = startIdx; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String corpId;
        private String corpName;
        private String corpShortName;
        private Integer chargerCount;
        private Integer startIdx;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder corpId(String corpId) { this.corpId = corpId; return this; }
        public Builder corpName(String corpName) { this.corpName = corpName; return this; }
        public Builder corpShortName(String corpShortName) { this.corpShortName = corpShortName; return this; }
        public Builder chargerCount(Integer chargerCount) { this.chargerCount = chargerCount; return this; }
        public Builder startIdx(Integer startIdx) { this.startIdx = startIdx; return this; }

        public StationResponseDto build() {
            return new StationResponseDto(id, name, corpId, corpName, corpShortName, chargerCount, startIdx);
        }
    }
}
