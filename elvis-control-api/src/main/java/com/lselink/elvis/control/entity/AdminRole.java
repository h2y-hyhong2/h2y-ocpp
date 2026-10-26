package com.lselink.elvis.control.entity;

/**
 * [S33-API-0070-CO] 관리자 역할(Role) 이넘
 */
public enum AdminRole {
    ROLE_ADMIN("시스템 관리자"),
    ROLE_OPERATOR("관제 운영자"),
    ROLE_VIEWER("모니터링 조회자");

    private final String description;

    AdminRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
