package com.lselink.elvis.control.entity;

import java.time.LocalDateTime;

/**
 * [S33-API-0070-CO] 관리자 도메인 모델
 */
public class AdminUser {

    private String username;
    private String password;
    private String name;
    private AdminRole role;
    private boolean enabled;
    private LocalDateTime createdAt;

    public AdminUser() {
    }

    public AdminUser(String username, String password, String name, AdminRole role, boolean enabled, LocalDateTime createdAt) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public AdminRole getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
