package com.verilearn.infra.feishu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FeishuSendMessageResponse {

    private int code;
    private String msg;
    private FeishuMessageData data;

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

    public FeishuMessageData getData() {
        return data;
    }

    public void setData(FeishuMessageData data) {
        this.data = data;
    }

    public static class FeishuMessageData {

        @JsonProperty("message_id")
        private String messageId;

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }
    }
}
