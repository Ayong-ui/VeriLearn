package com.verilearn.infra.feishu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "verilearn.feishu")
public class FeishuProperties {

    private String mode;
    private String baseUrl;
    private String appId;
    private String appSecret;
    private String verificationToken;
    private String encryptKey;
    private boolean longConnectionEnabled;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public String getEncryptKey() {
        return encryptKey;
    }

    public void setEncryptKey(String encryptKey) {
        this.encryptKey = encryptKey;
    }

    public boolean isLongConnectionEnabled() {
        return longConnectionEnabled;
    }

    public void setLongConnectionEnabled(boolean longConnectionEnabled) {
        this.longConnectionEnabled = longConnectionEnabled;
    }

    public boolean isLongConnectionMode() {
        return "LONG_CONNECTION".equalsIgnoreCase(mode);
    }
}
