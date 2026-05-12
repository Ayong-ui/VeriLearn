package com.verilearn.infra.feishu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FeishuCardActionRequest {

    @JsonProperty("open_id")
    private String openId;

    private FeishuCardOperator operator;
    private FeishuCardAction action;

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public FeishuCardOperator getOperator() {
        return operator;
    }

    public void setOperator(FeishuCardOperator operator) {
        this.operator = operator;
    }

    public FeishuCardAction getAction() {
        return action;
    }

    public void setAction(FeishuCardAction action) {
        this.action = action;
    }

    public static class FeishuCardOperator {

        @JsonProperty("open_id")
        private String openId;

        public String getOpenId() {
            return openId;
        }

        public void setOpenId(String openId) {
            this.openId = openId;
        }
    }

    public static class FeishuCardAction {

        private FeishuCardActionValue value;

        public FeishuCardActionValue getValue() {
            return value;
        }

        public void setValue(FeishuCardActionValue value) {
            this.value = value;
        }
    }

    public static class FeishuCardActionValue {

        @JsonProperty("action")
        private String actionName;

        @JsonProperty("task_id")
        private Long taskId;

        public String getActionName() {
            return actionName;
        }

        public void setActionName(String actionName) {
            this.actionName = actionName;
        }

        public Long getTaskId() {
            return taskId;
        }

        public void setTaskId(Long taskId) {
            this.taskId = taskId;
        }
    }
}
