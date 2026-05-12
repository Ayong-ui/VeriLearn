package com.verilearn.infra.feishu.dto;

import java.util.Map;

public class FeishuCardActionResponse {

    private String action;
    private String openId;
    private String toastText;
    private Map<String, Object> card;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getToastText() {
        return toastText;
    }

    public void setToastText(String toastText) {
        this.toastText = toastText;
    }

    public Map<String, Object> getCard() {
        return card;
    }

    public void setCard(Map<String, Object> card) {
        this.card = card;
    }
}
