package com.verilearn.infra.feishu.dto;

import java.util.ArrayList;
import java.util.List;

public class FeishuLocalSetupResponse {

    private String callbackPath;
    private String callbackUrlTemplate;
    private String aiConfigPageTemplate;
    private String mode;
    private boolean appIdConfigured;
    private boolean appSecretConfigured;
    private boolean verificationTokenConfigured;
    private boolean sendMessageEnabled;
    private boolean inboundVerificationEnabled;
    private boolean longConnectionEnabled;
    private boolean longConnectionReady;
    private boolean singleChatOnly;
    private boolean singleChatReady;
    private List<String> nextSteps = new ArrayList<>();

    public String getCallbackPath() {
        return callbackPath;
    }

    public void setCallbackPath(String callbackPath) {
        this.callbackPath = callbackPath;
    }

    public String getCallbackUrlTemplate() {
        return callbackUrlTemplate;
    }

    public void setCallbackUrlTemplate(String callbackUrlTemplate) {
        this.callbackUrlTemplate = callbackUrlTemplate;
    }

    public String getAiConfigPageTemplate() {
        return aiConfigPageTemplate;
    }

    public void setAiConfigPageTemplate(String aiConfigPageTemplate) {
        this.aiConfigPageTemplate = aiConfigPageTemplate;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isAppIdConfigured() {
        return appIdConfigured;
    }

    public void setAppIdConfigured(boolean appIdConfigured) {
        this.appIdConfigured = appIdConfigured;
    }

    public boolean isAppSecretConfigured() {
        return appSecretConfigured;
    }

    public void setAppSecretConfigured(boolean appSecretConfigured) {
        this.appSecretConfigured = appSecretConfigured;
    }

    public boolean isVerificationTokenConfigured() {
        return verificationTokenConfigured;
    }

    public void setVerificationTokenConfigured(boolean verificationTokenConfigured) {
        this.verificationTokenConfigured = verificationTokenConfigured;
    }

    public boolean isSendMessageEnabled() {
        return sendMessageEnabled;
    }

    public void setSendMessageEnabled(boolean sendMessageEnabled) {
        this.sendMessageEnabled = sendMessageEnabled;
    }

    public boolean isInboundVerificationEnabled() {
        return inboundVerificationEnabled;
    }

    public void setInboundVerificationEnabled(boolean inboundVerificationEnabled) {
        this.inboundVerificationEnabled = inboundVerificationEnabled;
    }

    public boolean isLongConnectionEnabled() {
        return longConnectionEnabled;
    }

    public void setLongConnectionEnabled(boolean longConnectionEnabled) {
        this.longConnectionEnabled = longConnectionEnabled;
    }

    public boolean isLongConnectionReady() {
        return longConnectionReady;
    }

    public void setLongConnectionReady(boolean longConnectionReady) {
        this.longConnectionReady = longConnectionReady;
    }

    public boolean isSingleChatOnly() {
        return singleChatOnly;
    }

    public void setSingleChatOnly(boolean singleChatOnly) {
        this.singleChatOnly = singleChatOnly;
    }

    public boolean isSingleChatReady() {
        return singleChatReady;
    }

    public void setSingleChatReady(boolean singleChatReady) {
        this.singleChatReady = singleChatReady;
    }

    public List<String> getNextSteps() {
        return nextSteps;
    }

    public void setNextSteps(List<String> nextSteps) {
        this.nextSteps = nextSteps;
    }
}
