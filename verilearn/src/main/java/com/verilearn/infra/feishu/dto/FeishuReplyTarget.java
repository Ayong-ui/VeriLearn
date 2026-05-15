package com.verilearn.infra.feishu.dto;

public class FeishuReplyTarget {

    private String receiveIdType;
    private String receiveId;

    public FeishuReplyTarget() {
    }

    public FeishuReplyTarget(String receiveIdType, String receiveId) {
        this.receiveIdType = receiveIdType;
        this.receiveId = receiveId;
    }

    public String getReceiveIdType() {
        return receiveIdType;
    }

    public void setReceiveIdType(String receiveIdType) {
        this.receiveIdType = receiveIdType;
    }

    public String getReceiveId() {
        return receiveId;
    }

    public void setReceiveId(String receiveId) {
        this.receiveId = receiveId;
    }

    public static FeishuReplyTarget openId(String openId) {
        return new FeishuReplyTarget("open_id", openId);
    }
}
