package com.verilearn.infra.feishu.dto;

public class FeishuChallengeResponse {

    private String challenge;

    public FeishuChallengeResponse() {
    }

    public FeishuChallengeResponse(String challenge) {
        this.challenge = challenge;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }
}
