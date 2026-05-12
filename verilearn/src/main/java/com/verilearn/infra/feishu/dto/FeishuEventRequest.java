package com.verilearn.infra.feishu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FeishuEventRequest {

    private String type;
    private String challenge;
    private String token;
    private FeishuEventHeader header;
    private FeishuMessageEvent event;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public FeishuEventHeader getHeader() {
        return header;
    }

    public void setHeader(FeishuEventHeader header) {
        this.header = header;
    }

    public FeishuMessageEvent getEvent() {
        return event;
    }

    public void setEvent(FeishuMessageEvent event) {
        this.event = event;
    }

    public static class FeishuEventHeader {

        @JsonProperty("event_type")
        private String eventType;

        private String token;

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public static class FeishuMessageEvent {

        private FeishuSender sender;
        private FeishuMessage message;

        public FeishuSender getSender() {
            return sender;
        }

        public void setSender(FeishuSender sender) {
            this.sender = sender;
        }

        public FeishuMessage getMessage() {
            return message;
        }

        public void setMessage(FeishuMessage message) {
            this.message = message;
        }
    }

    public static class FeishuSender {

        @JsonProperty("sender_id")
        private FeishuSenderId senderId;

        public FeishuSenderId getSenderId() {
            return senderId;
        }

        public void setSenderId(FeishuSenderId senderId) {
            this.senderId = senderId;
        }
    }

    public static class FeishuSenderId {

        @JsonProperty("open_id")
        private String openId;

        public String getOpenId() {
            return openId;
        }

        public void setOpenId(String openId) {
            this.openId = openId;
        }
    }

    public static class FeishuMessage {

        @JsonProperty("message_type")
        private String messageType;

        private String content;

        public String getMessageType() {
            return messageType;
        }

        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
