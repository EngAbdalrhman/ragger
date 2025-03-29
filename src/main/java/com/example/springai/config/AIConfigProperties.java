package com.example.springai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:ai-config.properties")
@ConfigurationProperties(prefix = "ai")
public class AIConfigProperties {

    private String provider;
    private int timeout;
    private int maxRetries;
    private int retryDelay;
    private Local local;

    // Getters and Setters
    public static class Local {

        private boolean queueEnabled;
        private int queuePrefetch;
        private String queueConcurrency;

        // Getters and Setters
        public boolean isQueueEnabled() {
            return queueEnabled;
        }

        public void setQueueEnabled(boolean queueEnabled) {
            this.queueEnabled = queueEnabled;
        }

        public int getQueuePrefetch() {
            return queuePrefetch;
        }

        public void setQueuePrefetch(int queuePrefetch) {
            this.queuePrefetch = queuePrefetch;
        }

        public String getQueueConcurrency() {
            return queueConcurrency;
        }

        public void setQueueConcurrency(String queueConcurrency) {
            this.queueConcurrency = queueConcurrency;
        }
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(int retryDelay) {
        this.retryDelay = retryDelay;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }
}
