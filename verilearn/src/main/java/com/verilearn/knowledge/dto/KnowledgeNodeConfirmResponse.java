package com.verilearn.knowledge.dto;

public class KnowledgeNodeConfirmResponse {

    private Long goalId;
    private int initializedCount;
    private String status;

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public int getInitializedCount() {
        return initializedCount;
    }

    public void setInitializedCount(int initializedCount) {
        this.initializedCount = initializedCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
