package com.lselink.elvis.control.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * [S33-API-0070-CO] CSMS 관제 시스템 관리자 계정 엔티티
 */
@Entity
@Table(name = "admin_user")
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private AdminRole role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AdminUser() {}

    public AdminUser(String username, String password, String name, AdminRole role, boolean enabled,
            LocalDateTime createdAt) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getAdminId() {
        return adminId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public void setName(String name) {
        this.name = name;
    }

    public AdminRole getRole() {
        return role;
    }

    public void setRole(AdminRole role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
