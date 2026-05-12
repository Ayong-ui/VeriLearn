package com.verilearn.workflow.dto;

import com.verilearn.chapter.dto.ChapterDetailResponse;
import com.verilearn.chapter.dto.ChapterSummaryResponse;
import com.verilearn.progress.dto.ProgressResponse;
import com.verilearn.task.dto.TaskResponse;

import java.util.List;

public class LearnerDashboardResponse {

    private Long userId;
    private Long goalId;
    private String feishuOpenId;
    private String topic;
    private String targetLevel;
    private Integer dailyMinutes;
    private String goalStatus;
    private TaskResponse todayTask;
    private ProgressResponse progress;
    private int chapterCount;
    private int pendingReviewCount;
    private ChapterDetailResponse currentChapter;
    private List<LearnerMaterialReference> currentMaterials;
    private List<ChapterSummaryResponse> chapters;
    private List<ChapterSummaryResponse> pendingReviews;

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

    public TaskResponse getTodayTask() {
        return todayTask;
    }

    public void setTodayTask(TaskResponse todayTask) {
        this.todayTask = todayTask;
    }

    public ProgressResponse getProgress() {
        return progress;
    }

    public void setProgress(ProgressResponse progress) {
        this.progress = progress;
    }

    public int getChapterCount() {
        return chapterCount;
    }

    public void setChapterCount(int chapterCount) {
        this.chapterCount = chapterCount;
    }

    public int getPendingReviewCount() {
        return pendingReviewCount;
    }

    public void setPendingReviewCount(int pendingReviewCount) {
        this.pendingReviewCount = pendingReviewCount;
    }

    public ChapterDetailResponse getCurrentChapter() {
        return currentChapter;
    }

    public void setCurrentChapter(ChapterDetailResponse currentChapter) {
        this.currentChapter = currentChapter;
    }

    public List<LearnerMaterialReference> getCurrentMaterials() {
        return currentMaterials;
    }

    public void setCurrentMaterials(List<LearnerMaterialReference> currentMaterials) {
        this.currentMaterials = currentMaterials;
    }

    public List<ChapterSummaryResponse> getChapters() {
        return chapters;
    }

    public void setChapters(List<ChapterSummaryResponse> chapters) {
        this.chapters = chapters;
    }

    public List<ChapterSummaryResponse> getPendingReviews() {
        return pendingReviews;
    }

    public void setPendingReviews(List<ChapterSummaryResponse> pendingReviews) {
        this.pendingReviews = pendingReviews;
    }
}
