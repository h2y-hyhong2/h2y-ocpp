package com.lselink.elvis.control.dto;

public class CorpResponseDto {
    private String id;
    private String name;
    private String shortName;
    private String tag;
    private String colorClass;
    private String badge;

    public CorpResponseDto() {
    }

    public CorpResponseDto(String id, String name, String shortName, String tag, String colorClass, String badge) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.tag = tag;
        this.colorClass = colorClass;
        this.badge = badge;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public String getColorClass() { return colorClass; }
    public void setColorClass(String colorClass) { this.colorClass = colorClass; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }
}
