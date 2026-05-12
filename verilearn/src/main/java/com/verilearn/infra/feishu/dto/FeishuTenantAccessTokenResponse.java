package com.verilearn.infra.feishu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FeishuTenantAccessTokenResponse {

    private int code;
    private String msg;

    @JsonProperty("tenant_access_token")
    private String tenantAccessToken;

    @JsonProperty("expire")
    private Integer expireInSeconds;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getTenantAccessToken() {
        return tenantAccessToken;
    }

    public void setTenantAccessToken(String tenantAccessToken) {
        this.tenantAccessToken = tenantAccessToken;
    }

    public Integer getExpireInSeconds() {
        return expireInSeconds;
    }

    public void setExpireInSeconds(Integer expireInSeconds) {
        this.expireInSeconds = expireInSeconds;
    }
}
