package com.kjr.rpf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.download")
public class DownloadConfig {
    private int limit = 100;
    private boolean limitEnabled = false;

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public boolean isLimitEnabled() {
        return limitEnabled;
    }

    public void setLimitEnabled(boolean limitEnabled) {
        this.limitEnabled = limitEnabled;
    }
}
