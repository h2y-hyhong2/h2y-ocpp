package com.lselink.elvis.data.clickhouse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ClickHouse 연동 환경설정 프로퍼티
 */
@Component
@ConfigurationProperties(prefix = "elvis.clickhouse")
public class ClickHouseProperties {

    private boolean enabled = true;
    private String url = "jdbc:ch://localhost:8123/elvis_poc";
    private String user = "default";
    private String password = "";
    private String driverClassName = "com.clickhouse.jdbc.ClickHouseDriver";
    private BatchProperties batch = new BatchProperties();

    public static class BatchProperties {
        private int size = 1000;
        private long timeoutMs = 1000;

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public BatchProperties getBatch() {
        return batch;
    }

    public void setBatch(BatchProperties batch) {
        this.batch = batch;
    }
}
