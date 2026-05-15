package com.verilearn.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiProviderConfigUpsertRequest {

    @NotBlank(message = "providerType cannot be blank")
    @Size(max = 40, message = "providerType is too long")
    private String providerType;

    @NotBlank(message = "baseUrl cannot be blank")
    @Size(max = 255, message = "baseUrl is too long")
    private String baseUrl;

    @NotBlank(message = "modelName cannot be blank")
    @Size(max = 100, message = "modelName is too long")
    private String modelName;

    @NotBlank(message = "apiKey cannot be blank")
    @Size(max = 512, message = "apiKey is too long")
    private String apiKey;

    private Boolean activateNow;

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Boolean getActivateNow() {
        return activateNow;
    }

    public void setActivateNow(Boolean activateNow) {
        this.activateNow = activateNow;
    }
}
