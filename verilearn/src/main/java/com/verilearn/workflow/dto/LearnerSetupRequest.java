package com.verilearn.workflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class LearnerSetupRequest {

    @NotBlank(message = "feishuOpenId cannot be blank")
    @Size(max = 64, message = "feishuOpenId is too long")
    private String feishuOpenId;

    @NotBlank(message = "topic cannot be blank")
    @Size(max = 200, message = "topic is too long")
    private String topic;

    @Size(max = 100, message = "targetLevel is too long")
    private String targetLevel;

    @Min(value = 1, message = "dailyMinutes must be at least 1")
    @Max(value = 1440, message = "dailyMinutes must be at most 1440")
    private Integer dailyMinutes;

    @Size(max = 20, message = "nodeNames is too large")
    private List<@NotBlank(message = "nodeName cannot be blank") @Size(max = 100, message = "nodeName is too long") String> nodeNames;

    public String getFeishuOpenId() {
        return feishuOpenId;
    }

    public void setFeishuOpenId(String feishuOpenId) {
        this.feishuOpenId = feishuOpenId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(String targetLevel) {
        this.targetLevel = targetLevel;
    }

    public Integer getDailyMinutes() {
        return dailyMinutes;
    }

    public void setDailyMinutes(Integer dailyMinutes) {
        this.dailyMinutes = dailyMinutes;
    }

    public List<String> getNodeNames() {
        return nodeNames;
    }

    public void setNodeNames(List<String> nodeNames) {
        this.nodeNames = nodeNames;
    }
}
