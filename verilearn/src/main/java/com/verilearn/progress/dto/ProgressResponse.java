package com.verilearn.progress.dto;

import java.util.List;

public class ProgressResponse {

    private Long userId;
    private Long goalId;
    private String topic;
    private String targetLevel;
    private Integer dailyMinutes;
    private String goalStatus;
    private int totalNodes;
    private int notStartedNodes;
    private int inProgressNodes;
    private int passedNodes;
    private int needsRetryNodes;
    private int totalChapters;
    private int completedChapters;
    private int inProgressChapters;
    private int pendingReviewChapters;
    private List<RecentTaskResponse> recentTasks;

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

    public int getTotalNodes() {
        return totalNodes;
    }

    public void setTotalNodes(int totalNodes) {
        this.totalNodes = totalNodes;
    }

    public int getNotStartedNodes() {
        return notStartedNodes;
    }

    public void setNotStartedNodes(int notStartedNodes) {
        this.notStartedNodes = notStartedNodes;
    }

    public int getInProgressNodes() {
        return inProgressNodes;
    }

    public void setInProgressNodes(int inProgressNodes) {
        this.inProgressNodes = inProgressNodes;
    }

    public int getPassedNodes() {
        return passedNodes;
    }

    public void setPassedNodes(int passedNodes) {
        this.passedNodes = passedNodes;
    }

    public int getNeedsRetryNodes() {
        return needsRetryNodes;
    }

    public void setNeedsRetryNodes(int needsRetryNodes) {
        this.needsRetryNodes = needsRetryNodes;
    }

    public int getTotalChapters() {
        return totalChapters;
    }

    public void setTotalChapters(int totalChapters) {
        this.totalChapters = totalChapters;
    }

    public int getCompletedChapters() {
        return completedChapters;
    }

    public void setCompletedChapters(int completedChapters) {
        this.completedChapters = completedChapters;
    }

    public int getInProgressChapters() {
        return inProgressChapters;
    }

    public void setInProgressChapters(int inProgressChapters) {
        this.inProgressChapters = inProgressChapters;
    }

    public int getPendingReviewChapters() {
        return pendingReviewChapters;
    }

    public void setPendingReviewChapters(int pendingReviewChapters) {
        this.pendingReviewChapters = pendingReviewChapters;
    }

    public List<RecentTaskResponse> getRecentTasks() {
        return recentTasks;
    }

    public void setRecentTasks(List<RecentTaskResponse> recentTasks) {
        this.recentTasks = recentTasks;
    }
}
