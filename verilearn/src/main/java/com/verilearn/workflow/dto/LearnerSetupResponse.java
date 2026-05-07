package com.verilearn.workflow.dto;

import com.verilearn.knowledge.dto.KnowledgeNodeResponse;

import java.util.List;

public class LearnerSetupResponse {

    private Long userId;
    private Long goalId;
    private String feishuOpenId;
    private String topic;
    private String targetLevel;
    private Integer dailyMinutes;
    private String goalStatus;
    private Integer initializedNodeCount;
    private Integer chapterCount;
    private List<KnowledgeNodeResponse> knowledgeNodes;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

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

    public String getGoalStatus() {
        return goalStatus;
    }

    public void setGoalStatus(String goalStatus) {
        this.goalStatus = goalStatus;
    }

    public Integer getInitializedNodeCount() {
        return initializedNodeCount;
    }

    public void setInitializedNodeCount(Integer initializedNodeCount) {
        this.initializedNodeCount = initializedNodeCount;
    }

    public Integer getChapterCount() {
        return chapterCount;
    }

    public void setChapterCount(Integer chapterCount) {
        this.chapterCount = chapterCount;
    }

    public List<KnowledgeNodeResponse> getKnowledgeNodes() {
        return knowledgeNodes;
    }

    public void setKnowledgeNodes(List<KnowledgeNodeResponse> knowledgeNodes) {
        this.knowledgeNodes = knowledgeNodes;
    }
}
