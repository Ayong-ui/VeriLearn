package com.verilearn.infra.feishu.dto;

import java.util.Map;

public class FeishuCardPreviewResponse {

    private String cardType;
    private String openId;
    private Map<String, Object> card;

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public Map<String, Object> getCard() {
        return card;
    }

    public void setCard(Map<String, Object> card) {
        this.card = card;
    }
}
